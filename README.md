# Controller & server slave node
This repository contains the source code of our duckiebot controller and the server slave node.

## Dependencies
It is currently a plain Java project without any dependency management. 
The only dependency is currently la4j, which can be found in the `lib` folder.
Due to the small amount of dependencies and the limited time, we didn't bother using a package manager, so you need some IDE to run the project.

## Running on the duckiebot
To run on the duckiebot, you should use the main class `simulator.ui.SimulatorUI` 
and include "duckie" in the **program arguments** (to the main method).
Note that the *server slave node* needs to be running. 
I recommend running this class from your IDE.

## Running the simulator
To run the simulator, you should also use the main class `simulator.ui.SimulatorUI`,
but **without** including "duckie" in the **program arguments**.
I recommend running this class from your IDE.

## Modes
You can run this project using 4 different modes, each with their own use cases.
Every mode can be used on both the simulator and duckiebot.

### KeyPlanner
*KeyPlanner* is the default mode. You can create a path by pressing the arrow
keys (each key press will append 1 grid to the path), and the duckiebot should
follow the path smoothly. Use this mode to manually configure a path through the maze.

### KeyController
*KeyController* can be chosen by including "key-controller" in the
**program arguments**. The duckiebot will start driving after 2 seconds,
and you can choose the direction by pressing the arrow keys or WASD. This mode is
useful to test how the PID controller responds to rapid changes in desired angle.

### StepController
*StepController* can be chosen by including "step-controller" in the
**program arguments**. The duckiebot will start driving after 2 seconds,
and it will take a sharp turn after driving a hardcoded distance. We used this
to test the PID controller. TODO Do we still need this?

### AutomaticPlanner
*AutomaticPlanner* can be chosen by including "automatic-planner" in the
**program arguments**. The duckiebot will use the pathfinding algorithm
to automatically map the maze. This mode should be used during the demo if
you manage to finish this project.

## Running the server slave node
First, you need to build the server slave node into a JAR file, with main class `joystick.server.JoystickServer` (no program arguments needed).
Then, you need to place it at the path `packages/joystick/joystick-server.jar` in the https://github.com/knokko/maze-ros-program repository.
Finally, follow the instructions of `maze-ros-program` to build & run it.

## Other main classes
This project has a few more main classes (runnable classes) that we haven't
mentioned yet:

### JoystickClient
This program will open a window that has a joystick, and shows the wheel
commands and encoders. The joystick can send fine-grained motor commands to the
duckiebot: you can e.g. test how the duckiebot responds to very small or large
motor commands. You can control the joystick with a keyboard or mouse. Check
the `JoystickBoard` class to see the exact controls.

### BezierSimulator
This program will open a window where you can drag a 'dummy duckie' and
see how the `BezierController` responds to its location. The window
contains a hardcoded route, and shows the Bézier curve and control points
when you place the 'dummy duckie' somewhere. We used this program to debug
the `BezierController` and generate one of the images for our report.

### CameraCalibrator
Because we experienced a lot of issues with the built-in camera calibrator
of the duckiebot, we made our own tool to help with this. We took a
camera frame from the duckiebot containing a k'nex grid on the floor, 
and saved it in this repository. Since we know where we placed the duckiebot, 
and we know that the length of 1 k'nex grid is exactly 30 centimeters, we
could use it to create a mapping from pixel locations on the image to
relative coordinates. To add a mapping, first click on a point on the right
image, and then click on the corresponding position on the left image.
Upon quitting, it will save the points and print them to `data.txt`, which
can be used for calibration.
