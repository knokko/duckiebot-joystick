package simulator.ui;

import controller.*;
import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.desired.DesiredWheelSpeed;
import controller.estimation.DuckieEstimations;
import controller.estimation.PoseEstimator;
import controller.estimation.SpeedEstimator;
import controller.parameters.DuckieParameters;
import controller.updater.ControllerFunction;
import controller.updater.ControllerUpdater;
import joystick.client.JoystickClientConnection;
import planner.GridPosition;
import planner.KeyboardPlanner;
import planner.RoutePlanner;
import simulator.Simulator;
import simulator.Terrain;
import state.DuckieControls;
import state.DuckieState;

import javax.swing.*;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

public class SimulatorUI {

    public static void main(String[] args) {
        boolean useDuckiebot = args.length > 0 && args[0].contains("duckie");
        boolean useManualRouteControl = args.length > 0 && args[0].contains("manual");

        DuckieEstimations estimations;
        DuckieControls controls;
        DuckieState trackedState;
        ControllerFunction updateFunction;
        var parameters = new DuckieParameters();

        if (useDuckiebot) {
            estimations = new DuckieEstimations();
            controls = new DuckieControls();
            trackedState = new DuckieState();
            updateFunction = deltaTime -> {};

            var connection = new JoystickClientConnection(
                    "db4.local", trackedState,
                    leftMotor -> trackedState.leftWheelControl = leftMotor,
                    rightMotor -> trackedState.rightWheelControl = rightMotor,
                    () -> controls.velLeft, () -> controls.velRight
            );
            connection.start();
        } else {
            var simulator = new Simulator(
                    Terrain.IDEAL, 0.0, 0.0, 0.0, 0.0
            );
            estimations = simulator.estimations;
            controls = simulator.controls;
            trackedState = simulator.trackedState;
            updateFunction = simulator;
        }

        //var lowLevelRoute = new ConcurrentLinkedQueue<DesiredPose>();
        var lowLevelRoute = new LinkedList<DesiredPose>();
        var highLevelRoute = new LinkedBlockingQueue<GridPosition>();
//        lowLevelRoute.add(new DesiredPose(0.4, 0.5 * GRID_SIZE, 0, false));
//        lowLevelRoute.add(new DesiredPose(0.95, 5 * GRID_SIZE, 0.25, false));
//        lowLevelRoute.add(new DesiredPose(0.5, 0.5, 0.25, false));
//        lowLevelRoute.add(new DesiredPose(0.5, 0.9, 0.25, false));
//
//        lowLevelRoute.add(new DesiredPose(0.4, 0.1, 0, true));
//        lowLevelRoute.add(new DesiredPose(-0.2, 0.1, 0, true));
//        lowLevelRoute.add(new DesiredPose(-0.3, 0.2, 0.75, true));
//        lowLevelRoute.add(new DesiredPose(-0.3, 0.4, 0.75, true));
//        lowLevelRoute.add(new DesiredPose(0.7, 0.2, 0.25));
//        lowLevelRoute.add(new DesiredPose(0.7, 0.4, 0.25));
//        lowLevelRoute.add(new DesiredPose(0.7, 0.6, 0.25));
//        lowLevelRoute.add(new DesiredPose(0.6, 0.7, 0.5));
//        lowLevelRoute.add(new DesiredPose(0.4, 0.7, 0.5));
//        lowLevelRoute.add(new DesiredPose(0.2, 0.7, 0.5));
//        lowLevelRoute.add(new DesiredPose(0.1, 0.6, 0.75));
//        lowLevelRoute.add(new DesiredPose(0.1, 0.4, 0.75));
//        lowLevelRoute.add(new DesiredPose(0.1, 0.2, 0.75));
//        lowLevelRoute.add(new DesiredPose(0.1, 0.0, 0.75));
        
        var desiredVelocity = new DesiredVelocity();
        var desiredWheelSpeed = new DesiredWheelSpeed();

        var routePlanner = new RoutePlanner(highLevelRoute, lowLevelRoute);

        Thread routePlannerThread = new Thread(routePlanner::start);
        routePlannerThread.setDaemon(true);
        routePlannerThread.start();

        var poseEstimator = new PoseEstimator(trackedState, estimations);

        var routeController = new BezierController(lowLevelRoute, desiredVelocity, estimations);
        //var routeController = new StepController(lowLevelRoute, desiredVelocity, estimations, controls, 5.0);
        var differentialDriver = new DifferentialDriver(
                desiredVelocity, desiredWheelSpeed, estimations, controls, parameters.anglePID
        );
        var directSpeedController = new DirectSpeedPIDController(
                desiredVelocity, desiredWheelSpeed, estimations, parameters.speedPID
        );

        var leftSpeedEstimator = new SpeedEstimator(
                () -> trackedState.leftWheelEncoder, newSpeed -> estimations.leftSpeed = newSpeed, estimations.leftSpeedFunction
        );
        var rightSpeedEstimator = new SpeedEstimator(
                () -> trackedState.rightWheelEncoder, newSpeed -> estimations.rightSpeed = newSpeed, estimations.rightSpeedFunction
        );

        var updater = new ControllerUpdater();

        updater.addController(updateFunction, 1);
        updater.addController(routeController, 5);
        updater.addController(directSpeedController, 1);
        updater.addController(differentialDriver, 1);
        updater.addController(leftSpeedEstimator, 5);
        updater.addController(rightSpeedEstimator, 5);
        updater.addController(poseEstimator, 3);

        var monitorFrame = new JFrame();
        monitorFrame.setSize(800, 500);
        monitorFrame.setAutoRequestFocus(false);
        monitorFrame.setLocation(1200, 200);
        monitorFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        monitorFrame.add(new MonitorBoard(trackedState, controls, estimations, desiredWheelSpeed, parameters.anglePID));
        monitorFrame.addKeyListener(new PIDKeyboardTuner(parameters.anglePID));
        monitorFrame.setVisible(true);

        var simulatorFrame = new JFrame();
        simulatorFrame.setSize(1200, 800);
        simulatorFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        simulatorFrame.add(new SimulatorBoard(estimations, desiredVelocity, lowLevelRoute));
        if (useManualRouteControl) {
            simulatorFrame.addKeyListener(new KeyboardPlanner(highLevelRoute));
        }
        simulatorFrame.setVisible(true);

        Thread updateThread = new Thread(updater::start);
        updateThread.setDaemon(true);
        updateThread.start();

        Thread repaintThread = new Thread(() -> {
            try {
                while (true) {
                    //noinspection BusyWait
                    sleep(20);
                    SwingUtilities.invokeLater(simulatorFrame::repaint);
                    SwingUtilities.invokeLater(monitorFrame::repaint);
                }
            } catch (InterruptedException shouldNotHappen) {
                throw new Error(shouldNotHappen);
            }
        });
        repaintThread.setDaemon(true);
        repaintThread.start();
    }
}
