package ie.atu.sw;

import static java.lang.Math.max;

import static java.lang.Math.min;
import static java.util.concurrent.ThreadLocalRandom.current;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

import jhealy.aicme4j.NetworkBuilderFactory;
import jhealy.aicme4j.net.Activation;
import jhealy.aicme4j.net.Loss;
import jhealy.aicme4j.net.NeuralNetwork;
import jhealy.aicme4j.net.Output;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.DoubleAdder;

public class GameView extends JPanel implements ActionListener{
	//Some constants
	private static final long serialVersionUID	= 1L;
	private static final int MODEL_WIDTH 		= 30;
	private static final int MODEL_HEIGHT 		= 20;
	private static final int SCALING_FACTOR 	= 30;
	
	private static final int MIN_TOP 			= 2;
	private static final int MIN_BOTTOM 		= 18;
	private static final int PLAYER_COLUMN 		= 15;
	private static final int TIMER_INTERVAL 	= 100;
	
	private static final byte ONE_SET 			=  1;
	private static final byte ZERO_SET 			=  0;

	/*
	 * The 30x20 game grid is implemented using a linked list of 
	 * 30 elements, where each element contains a byte[] of size 20. 
	 */
	private LinkedList<byte[]> model = new LinkedList<>();

	//These two variables are used by the cavern generator. 
	private int prevTop = MIN_TOP;
	private int prevBot = MIN_BOTTOM;
	
	//Once the timer stops, the game is over
	private Timer timer;
	private long time;
	
	private int playerRow = 11;
	private int index = MODEL_WIDTH - 1; //Start generating at the end
	private Dimension dim;
	
	//Some fonts for the UI display
	private Font font = new Font ("Dialog", Font.BOLD, 50);
	private Font over = new Font ("Dialog", Font.BOLD, 100);

	//The player and a sprite for an exploding plane
	private Sprite sprite;
	private Sprite dyingSprite;
	
	private boolean auto;
	
	private int lastMovement = 0; // Variable to track the last movement (up, down, or none)
	
	private NeuralNetwork net;
	
	
	
	private final ExecutorService executor = Executors.newSingleThreadExecutor();


	public GameView(boolean auto) throws Exception{
		this.auto = auto; //Use the autopilot
		setBackground(Color.LIGHT_GRAY);
		setDoubleBuffered(true);
		
		//Creates a viewing area of 900 x 600 pixels
		dim = new Dimension(MODEL_WIDTH * SCALING_FACTOR, MODEL_HEIGHT * SCALING_FACTOR);
    	super.setPreferredSize(dim);
    	super.setMinimumSize(dim);
    	super.setMaximumSize(dim);
		
    	initModel();
    	
		timer = new Timer(TIMER_INTERVAL, this); //Timer calls actionPerformed() every second
		timer.start();
		
		
		trainNeuralNetwork();
	}
	
	//Build our game grid
	private void initModel() {
		for (int i = 0; i < MODEL_WIDTH; i++) {
			model.add(new byte[MODEL_HEIGHT]);
		}
	}
	
	public void setSprite(Sprite s) {
		this.sprite = s;
	}
	
	public void setDyingSprite(Sprite s) {
		this.dyingSprite = s;
	}
	
	//Called every second by actionPerformed(). Paint methods are usually ugly.
	public void paintComponent(Graphics g) {
        super.paintComponent(g);
        var g2 = (Graphics2D)g;
        
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, dim.width, dim.height);
        
        int x1 = 0, y1 = 0;
        for (int x = 0; x < MODEL_WIDTH; x++) {
        	for (int y = 0; y < MODEL_HEIGHT; y++){  
    			x1 = x * SCALING_FACTOR;
        		y1 = y * SCALING_FACTOR;

        		if (model.get(x)[y] != 0) {
            		if (y == playerRow && x == PLAYER_COLUMN) {
            			timer.stop(); //Crash...
            		}
            		g2.setColor(Color.BLACK);
            		g2.fillRect(x1, y1, SCALING_FACTOR, SCALING_FACTOR);
        		}
        		
        		if (x == PLAYER_COLUMN && y == playerRow) {
        			if (timer.isRunning()) {
            			g2.drawImage(sprite.getNext(), x1, y1, null);
        			}else {
            			g2.drawImage(dyingSprite.getNext(), x1, y1, null);
        			}
        			
        		}
        	}
        }
        
        /*
         * Not pretty, but good enough for this project... The compiler will
         * tidy up and optimise all of the arithmetics with constants below.
         */
        g2.setFont(font);
        g2.setColor(Color.RED);
        g2.fillRect(1 * SCALING_FACTOR, 15 * SCALING_FACTOR, 400, 3 * SCALING_FACTOR);
        g2.setColor(Color.WHITE);
        g2.drawString("Time: " + (int)(time * (TIMER_INTERVAL/1000.0d)) + "s", 1 * SCALING_FACTOR + 10, (15 * SCALING_FACTOR) + (2 * SCALING_FACTOR));
        
        if (!timer.isRunning()) {
			g2.setFont(over);
			g2.setColor(Color.RED);
			g2.drawString("Game Over!", MODEL_WIDTH / 5 * SCALING_FACTOR, MODEL_HEIGHT / 2* SCALING_FACTOR);
        }
	}

	//Move the plane up or down
	public void move(int step) {
		playerRow += step;
		lastMovement = step;
	}
	
	
	/*
	 * ----------
	 * AUTOPILOT!
	 * ----------
	 * The following implementation randomly picks a -1, 0, 1 to control the plane. You 
	 * should plug the trained neural network in here. This method is called by the timer
	 * every TIMER_INTERVAL units of time from actionPerformed(). There are other ways of
	 * wiring your neural network into the application, but this way might be the easiest. 
	 *  
	 */
	private void autoMove() {
		//move(current().nextInt(-1, 2)); //Move -1 (up), 0 (nowhere), 1 (down)
		double[] inputData = new double[12];
	    // Prepare the input data
	    double[] data = sample();
	    for(int i = 0; i < 12; i++) {
	    	inputData[i] = data[i];
	    }
	    System.out.println("Data: " + Arrays.toString(inputData));

	    // Use the neural network to predict the movement
	    int predictedMovement = predictMovement(inputData);
	    System.out.println("Move: " + predictedMovement);

	    // Implement logic based on the prediction
	    move(predictedMovement); 
	}

	// Method to predict movement based on current game state
	public int predictMovement(double[] data) {
	    try {
	        // Process the input data through the neural network to get the prediction
	        double prediction = net.process(data, Output.NUMERIC);
	        System.out.println("Prediction: " + prediction);

	        // Convert the prediction to an integer value representing the movement direction
	        if (prediction <= -0.5) {
	        	System.out.println("Moving Up");
	            return -1; // Move up
	        } else if (prediction >= 0.5) {
	        	System.out.println("Moving Down");
	            return 1; // Move down
	        } else {
	        	System.out.println("Not Moving");
	            return 0; // Don't move 
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return 0; // Default to not moving in case of an error
	    }
	    
	    
	}

	
//	private void startTraining() {
//        executor.submit(() -> {
//            while (true) {
//                trainNeuralNetwork();
//                try {
//                    Thread.sleep(1000); // Wait for 1 second before next training iteration
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }


	
	//Called every second by the timer 
	public void actionPerformed(ActionEvent e) {
		time++; //Update our timer
		this.repaint(); //Repaint the cavern
		
		//Update the next index to generate
		index++;
		index = (index == MODEL_WIDTH) ? 0 : index;
		
		generateNext(); //Generate the next part of the cave
		if (auto) autoMove();
		
		/*
		 * Use something like the following to extract training data.
		 * It might be a good idea to submit the double[] returned by
		 * the sample() method to an executor and then write it out 
		 * to file. You'll need to label the data too and perhaps add
		 * some more features... Finally, you do not have to sample 
		 * the data every TIMER_INTERVAL units of time. Use some modular
		 * arithmetic as shown below. Alternatively, add a key stroke 
		 * to fire an event that starts the sampling.
		 */
		
		
		
		
		if (time % 2.5 == 0) {
			
			
			
			saveTrainingData();
			
			lastMovement = 0;
		}
		
		
	}
	
	
	
	
	/*
	 * Generate the next layer of the cavern. Use the linked list to
	 * move the current head element to the tail and then randomly
	 * decide whether to increase or decrease the cavern. 
	 */
	private void generateNext() {
		var next = model.pollFirst(); 
		model.addLast(next); //Move the head to the tail
		Arrays.fill(next, ONE_SET); //Fill everything in
		
		
		//Flip a coin to determine if we could grow or shrink the cave
		var minspace = 4; //Smaller values will create a cave with smaller spaces
		prevTop += current().nextBoolean() ? 1 : -1; 
		prevBot += current().nextBoolean() ? 1 : -1;
		prevTop = max(MIN_TOP, min(prevTop, prevBot - minspace)); 		
		prevBot = min(MIN_BOTTOM, max(prevBot, prevTop + minspace));

		//Fill in the array with the carved area
		Arrays.fill(next, prevTop, prevBot, ZERO_SET);
	}
	
	
	/*
	 * Use this method to get a snapshot of the 30x20 matrix of values
	 * that make up the game grid. The grid is flatmapped into a single
	 * dimension double array... (somewhat) ready to be used by a neural 
	 * net. You can experiment around with how much of this you actually
	 * will need. The plane is always somehere in column PLAYER_COLUMN
	 * and you probably do not need any of the columns behind this. You
	 * can consider all of the columns ahead of PLAYER_COLUMN as your
	 * horizon and this value can be reduced to save space and time if
	 * needed, e.g. just look 1, 2 or 3 columns ahead. 
	 * 
	 * You may also want to track the last player movement, i.e.
	 * up, down or no change. Depending on how you design your neural
	 * network, you may also want to label the data as either okay or 
	 * dead. Alternatively, the label might be the movement (up, down
	 * or straight). 
	 *  
	 */
//	public double[] sample() {
//		var vector = new double[MODEL_WIDTH * MODEL_HEIGHT];
//		var index = 0;
//		
//		for (byte[] bm : model) {
//			for (byte b : bm) {
//				vector[index] = b;
//				index++;
//			}
//		}
//		return vector;
//	}
	
	// Method to gather training data
	public double[] sample() {
	    int visibleWidth = 3; 
	    int visibleHeight = 4; 

	    var vector = new double[(visibleWidth * visibleHeight) + 1]; // Flat map grid into 1D array (+1 for movement tracking)
	    var index = 0;

	    // Iterate over the visible area of the game grid
	    for (int y = Math.max(playerRow - visibleHeight + 1, 0); y <= Math.min(playerRow, MODEL_HEIGHT - 1); y++) {
	        for (int x = PLAYER_COLUMN; x < PLAYER_COLUMN + visibleWidth; x++) {
	            if (x >= 0 && x < MODEL_WIDTH) { // Check if x is within grid bounds
	                if (y < 0 || y >= MODEL_HEIGHT) { // If y is outside grid bounds
	                    vector[index] = 1; // Treat as solid wall
	                } else {
	                    vector[index] = model.get(x)[y]; // Copy value from game grid
	                }
	            } else {
	                vector[index] = 1; // Treat as solid wall if x is outside grid bounds
	            }
	          //  System.out.println(index);
	            index++;
	        }
	    }
	    // Add movement tracking data to the end of the vector
	    vector[index] = lastMovement;
	    return vector;
	}



	// Method to gather and save training data
	public void saveTrainingData() {
	    // Gather training data
	    double[] trainingRow = sample();

	    // Print training data 
//	    System.out.println("Training data: " + Arrays.toString(trainingRow));

	    if(!auto) {
	    saveToFile(trainingRow);
	}
	    }
	    
	
	public void saveToFile(double[] data) {
		
        String filename = "training_data.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            // Write the training data elements separated by commas
            for (int i = 0; i < data.length; i++) {
                writer.write(String.valueOf(data[i]));
                // Add comma if it's not the last element
                if (i < data.length - 1) {
                    writer.write(",");
                }
            }
            writer.newLine(); // Add newline to separate entries
        } catch (IOException e) {
            System.err.println("Error writing training data to file: " + e.getMessage());
        }
    }
	
	
	
	public void trainNeuralNetwork() throws Exception {
        if(auto) {
		TrainingData td = new TrainingData("./training_data.txt"); 
		
        System.out.println(td.getInputData());
        System.out.println(td.getOutputData());
        
        // Train the neural network
        this.net = NetworkBuilderFactory.getInstance().newNetworkBuilder()
                .inputLayer("Input", 12) 
                .hiddenLayer("Hidden1", Activation.TANH, 9)
                .outputLayer("Output", Activation.TANH, 1)
                .train(td.getInputData(), td.getOutputData(), 0.005, 0.95, 100000, 0.000000000001, Loss.SSE)
                .save("./plane.data")
                .build();
        
        System.out.println(net);
        }
//        for (int i = 0; i < inputData.length; i++) {
//			var predicted = (int) net.process(inputData[i], Output.NUMERIC);
//			var actual = Aicme4jUtils.getMaxIndex(expectedOutput[i]);
////			System.out.print(predicted + "==" + actual + "\t");
////			System.out.println(actual == predicted ? "[OK]" : "[Error]");
//		}
    }
	
	
//	private double[][] loadTrainingData(String filename) {
//	    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
//	        List<double[]> dataList = new ArrayList<>(); // Use a list to dynamically store the data
//
//	        String line;
//	        while ((line = reader.readLine()) != null) {
//	            String[] values = line.split(",");
//	            double[] rowData = new double[values.length];
//	            for (int i = 0; i < values.length; i++) {
//	                rowData[i] = Double.parseDouble(values[i]);
//	            }
//	            dataList.add(rowData);
//	        }
//
//	        // Separate input and output data
//	        double[][] inputData = new double[dataList.size()][12];
//	        double[][] outputData = new double[dataList.size()][1];
//	        for (int i = 0; i < dataList.size(); i++) {
//	            double[] row = dataList.get(i);
//	            for (int j = 0; j < 12; j++) {
//	                inputData[i][j] = row[j];
//	            }
//	            outputData[i][0] = row[row.length - 1]; // Assuming the output is always the last element
//	        }
//
//	        return inputData;
//	        
//	    } catch (IOException e) {
//	        e.printStackTrace();
//	        return null;
//	    }
//	}

	
	
	/*
	 * Resets and restarts the game when the "S" key is pressed
	 */
	public void reset() {
		model.stream() 		//Zero out the grid
		     .forEach(n -> Arrays.fill(n, 0, n.length, ZERO_SET));
		playerRow = 11;		//Centre the plane
		time = 0; 			//Reset the clock
		timer.restart();	//Start the animation
	}
}