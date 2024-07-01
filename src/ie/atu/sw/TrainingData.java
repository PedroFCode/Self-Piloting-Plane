package ie.atu.sw;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TrainingData {
	private double[][] inputData; 
    private double[][] outputData;
	public TrainingData(String filename){
		 
		    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
		        List<double[]> dataList = new ArrayList<>(); // Use a list to dynamically store the data

		        String line;
		        while ((line = reader.readLine()) != null) {
		            String[] values = line.split(",");
		            double[] rowData = new double[values.length];
		            for (int i = 0; i < values.length; i++) {
		                rowData[i] = Double.parseDouble(values[i]);
		            }
		            dataList.add(rowData);
		        }

		        // Separate input and output data
		        double[][] inputData = new double[dataList.size()][12];
		        double[][] outputData = new double[dataList.size()][1];
		        for (int i = 0; i < dataList.size(); i++) {
		            double[] row = dataList.get(i);
		            for (int j = 0; j < 12; j++) {
		                inputData[i][j] = row[j];
		            }
		            outputData[i][0] = row[row.length - 1]; 
		        }
		      
		       this.outputData = outputData;
		       this.inputData = inputData;
		        
		    } catch (IOException e) {
		        e.printStackTrace();
		        
		    }
		

	}
	public double[][] getInputData() {
		return inputData;
	}
	
	public double[][] getOutputData() {
		return outputData;
	}
	
}
