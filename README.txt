Network Design:
Input Layer: The input layer consists of 12 neurons, representing the grid around the plane.
Hidden Layer: One hidden layer is used with 9 neurons and the hyperbolic tangent (tanh) 
activation function.
Output Layer: The output layer consists of 1 neuron with the tanh activation function, 
representing the predicted movement direction (-1 for up, 0 for no movement, 1 for down).

Feature Engineering:
Game State Representation: The game state is represented as a 30x20 grid, with each cell 
being either a solid wall '1' or open space '0'.
Visible Area: The visible area of the game grid considered for input is 3 columns wide and 
4 rows high, centered around the players position.
Player Movement Tracking: The last movement of the player (up, down, or no movement) is tracked 
for predictions made by the training model.

Normalization:
Input Data: Values are binary '0' or '1' representing open space or solid wall.
Output Data: The output is normalized using the tahn activation function, which outputs values 
between -1 and 1, representing movement directions.

Rationale:
Network Architecture: A simple feedfoawrd neural network architecture is used for its suitability 
for this task which balances complexity and performance.
Activation Function: The hyperbolic tangent (tanh) activation function is used for its 
ability to output values between -1 and 1, suitable for representing movement directions.
Training Approach: The network is trained using backprop with stochastic gradient 
descent. Sum of Squared Errors (SSE) loss is minimized during training.
Feature Selection: Features are selected based on their relevance to the task of 
predicting player movement. The visible game area and player movement history are for 
making accurate predictions.