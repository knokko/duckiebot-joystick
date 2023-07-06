package simulator.ui;

import camera.WallMapper;
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
import controller.util.DuckieBot;
import joystick.client.JoystickClientConnection;
import planner.GridPosition;
import planner.KeyboardPlanner;
import planner.RoutePlanner;
import simulator.Simulator;
import simulator.Terrain;
import simulator.WallGrid;
import state.DuckieControls;
import state.DuckiePose;
import state.DuckieState;

import javax.swing.*;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import static controller.util.DuckieBot.GRID_SIZE;
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
        WallGrid realWalls = null;
        DuckiePose realPose = null;

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
                    Terrain.VERY_NOISY_SLOW, 0.0, 0.0, 0.0, 0.0,
                    100, 0.00, 0.00, 0.05
            );
            estimations = simulator.estimations;
            controls = simulator.controls;
            trackedState = simulator.trackedState;
            updateFunction = simulator;
            realWalls = simulator.walls;
            realPose = simulator.realPose;
        }

        //var lowLevelRoute = new ConcurrentLinkedQueue<DesiredPose>();
        var lowLevelRoute = new LinkedList<DesiredPose>();
        var highLevelRoute = new LinkedBlockingQueue<GridPosition>();
        lowLevelRoute.add(new DesiredPose(0.5, 0.5 * GRID_SIZE, 0, false));
        lowLevelRoute.add(new DesiredPose(0.5, 5 * GRID_SIZE, 0.25, false));
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

        //var routeController = new BezierController(lowLevelRoute, desiredVelocity, estimations);
        var routeController = new StepController(lowLevelRoute, desiredVelocity, estimations, controls, 5.0);
        //var routeController = new KeyboardController(desiredVelocity, estimations, controls);
        var differentialDriver = new DifferentialDriver(
                desiredVelocity, desiredWheelSpeed, estimations, controls, parameters.anglePID
        );
        var directSpeedController = new DirectSpeedPIDController(
                desiredVelocity, desiredWheelSpeed, estimations, parameters.speedPID
        );
//        var directSpeedController = new SpeedPolyController(estimations, () -> desiredVelocity.speed, throttle -> {
//            desiredWheelSpeed.leftSpeed = throttle;
//            desiredWheelSpeed.rightSpeed = throttle;
//        });

        var leftSpeedEstimator = new SpeedEstimator(
                () -> trackedState.leftWheelEncoder, newSpeed -> estimations.leftSpeed = newSpeed
        );
        var rightSpeedEstimator = new SpeedEstimator(
                () -> trackedState.rightWheelEncoder, newSpeed -> estimations.rightSpeed = newSpeed
        );

//        var averageSpeedEstimator = new SpeedPredictor(() -> {
//            var leftTicks = trackedState.leftWheelEncoder;
//            var rightTicks = trackedState.rightWheelEncoder;
//            if (leftTicks != null && rightTicks != null) return (leftTicks.value() + rightTicks.value()) * 0.5;
//            else return Double.NaN;
//        }, newPoly -> estimations.distancePolynomial = newPoly);

        var updater = new ControllerUpdater();

        updater.addController(updateFunction, 1);
        updater.addController(routeController, 1);
        updater.addController(directSpeedController, 1);
        updater.addController(differentialDriver, 1);
        updater.addController(leftSpeedEstimator, 1);
        updater.addController(rightSpeedEstimator, 1);
        //updater.addController(averageSpeedEstimator, 1);

        var wallUpdater = new ControllerUpdater();
        wallUpdater.addController(new WallMapper(estimations, trackedState, 0.02, 0.0), 1);

        var positioner = new ControllerUpdater();
        positioner.addController(new Positionator(desiredVelocity, trackedState, estimations, controls), 10);

        var monitorFrame = new JFrame();
        monitorFrame.setSize(800, 500);
        monitorFrame.setAutoRequestFocus(false);
        monitorFrame.setLocation(1200, 200);
        monitorFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var pid = parameters.anglePID;
        monitorFrame.add(new MonitorBoard(trackedState, controls, estimations, desiredVelocity, pid));
        monitorFrame.addKeyListener(new PIDKeyboardTuner(pid));
        monitorFrame.setVisible(true);

        var simulatorFrame = new JFrame();
        simulatorFrame.setSize(1200, 800);
        simulatorFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        simulatorFrame.add(new SimulatorBoard(estimations, desiredVelocity, lowLevelRoute, realWalls, realPose, trackedState));
        //simulatorFrame.addKeyListener(routeController);
        if (useManualRouteControl) {
            simulatorFrame.addKeyListener(new KeyboardPlanner(highLevelRoute, lowLevelRoute));
        }
        simulatorFrame.setVisible(true);

        Thread updateThread = new Thread(updater::start);
        updateThread.setDaemon(true);
        updateThread.start();

        Thread wallThread = new Thread(wallUpdater::start);
        wallThread.setDaemon(true);
        wallThread.start();

        Thread positionerThread = new Thread(positioner::start);
        positionerThread.setDaemon(true);
        positionerThread.start();

        Thread poseThread = new Thread(() -> {
            while (true) {
                poseEstimator.update(0.0);
            }
        });
        poseThread.setDaemon(true);
        poseThread.start();

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
