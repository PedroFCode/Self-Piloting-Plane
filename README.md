# Neural Network for Predicting Player Movement in a Grid Game
## Overview
This project implements a neural network designed to predict player movement within a 30x20 game grid environment. The model uses a simple feedforward architecture to balance complexity and performance, aiming to accurately forecast the player's next move based on the visible game area and recent movement history.

## Network Design
### Architecture

- Input Layer: 12 neurons representing the visible 3x4 grid area around the player's position.
- Hidden Layer: 1 hidden layer with 9 neurons utilizing the hyperbolic tangent (tanh) activation function for non-linear transformations.
- Output Layer: 1 neuron with a tanh activation function, producing a value between -1 and 1. This output represents the predicted movement direction:
  -1: Move up
  0: No movement
  1: Move down
  
- Activation Functions
tanh (Hyperbolic Tangent): Chosen for its ability to produce outputs in the range of -1 to 1, which aligns with the movement prediction requirements. The function also helps in managing gradients effectively during training, reducing the chances of vanishing or exploding gradients.

- Training Approach
Loss Function: Sum of Squared Errors (SSE) is used to measure the discrepancy between predicted and actual movement.
Optimizer: The model is trained using backpropagation with Stochastic Gradient Descent (SGD), a robust technique for minimizing the SSE loss.

## Feature Engineering
### Game State Representation
- Grid Dimensions: The game environment is a 30x20 grid. Each cell can be either:
1: Solid wall
0: Open space

### Input Data
- Visible Area: A 3x4 grid area centered around the player's current position is used as input. This reduces dimensionality and focuses on the relevant surroundings.
- Normalization: Inputs are binary (0 or 1), directly representing open spaces or walls without further normalization.
Output Data
- Movement Direction: The model outputs a value between -1 and 1 using the tanh activation function to indicate movement direction:
-1: Move up
0: No movement
1: Move down

- Player Movement Tracking
Historical Context: The last movement direction of the player (up, down, or no movement) is tracked to provide context for predictions, helping the model understand recent trends in player movement.

## Rationale
### Network Architecture
A simple feedforward neural network architecture is chosen to maintain a balance between model complexity and performance. This approach is suitable for tasks where the input-output mapping is relatively straightforward and does not require deep, hierarchical feature extraction.

Activation Function
The tanh activation function is utilized for both the hidden and output layers due to its output range of -1 to 1, which naturally aligns with the movement direction predictions. Its smooth gradient also aids in the convergence of the training process.

Feature Selection
Features are meticulously selected based on their relevance to predicting player movement:

Visible Game Area: Provides immediate spatial context for decision-making.
Player Movement History: Adds temporal context, helping the model make more informed predictions.

## Compiled JAR File

This project includes a compiled JAR file. This JAR file contains the compiled version of the project and can be executed directly.

### To run the JAR file, you need to have [Java](https://www.java.com/en/download/) installed on your system. You can execute the JAR file using the following command when in the jar files directory: 

```bash
java -jar ai.jar
