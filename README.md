# Controller & server slave node
This repository contains the source code of our duckiebot controller and the server slave node.

## Dependencies
It is currently a plain Java project without any dependency management. 
The only dependency is currently la4j, which can be found in the `lib` folder.
Due to the small amount of dependencies and the limited time, we didn't bother using a package manager, so you need some IDE to run the project.

## Running the simulator
To run the simulator, you should use the main class `simulator.ui.SimulatorUI` and give the **program argument** "manual" (to the main method).
I recommend running this class from your IDE.

## Running on the duckiebot
To run on the duckiebot, you should use the main class `simulator.ui.SimulatorUI` and give the **program argument** "duckie-manual" (to the main method).
Note that the *server slave node* needs to be running. I recommend running this class from your IDE.

## Running the server slave node
First, you need to build the server slave node into a JAR file, with main class `joystick.server.JoystickServer` (no program arguments needed).
Then, you need to place it at the path `packages/joystick/joystick-server.jar` in the https://github.com/knokko/maze-ros-program repository.
Finally, follow the instructions of `maze-ros-program` to build & run it.
