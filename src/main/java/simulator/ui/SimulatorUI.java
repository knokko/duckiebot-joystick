package simulator.ui;

import controller.*;
import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.desired.DesiredWheelSpeed;
import controller.estimation.DuckieEstimations;
import controller.estimation.PoseEstimator;
import controller.estimation.SpeedEstimator;
import controller.updater.ControllerFunction;
import controller.updater.ControllerUpdater;
import joystick.client.JoystickClientConnection;
import simulator.Simulator;
import simulator.Terrain;
import state.DuckieControls;
import state.DuckieState;

import javax.swing.*;

import java.util.LinkedList;

import static java.lang.Thread.sleep;

public class SimulatorUI {

    public static void main(String[] args) {
        boolean useDuckiebot = args.length > 0 && args[0].equals("duckie");
        DuckieEstimations estimations;
        DuckieControls controls;
        DuckieState trackedState;
        ControllerFunction updateFunction;

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
                    Terrain.SIMPLE_SLOW, 0.0, 0.0, 0.1, 0.1
            );
            estimations = simulator.estimations;
            controls = simulator.controls;
            trackedState = simulator.trackedState;
            updateFunction = simulator;
        }

        double maxAcceleration = 1.0;

        var route = new LinkedList<DesiredPose>();
        route.add(new DesiredPose(0.4, 0.1, 0));
        route.add(new DesiredPose(0.6, 0.1, 0));
        route.add(new DesiredPose(0.7, 0.2, 0.25));
        route.add(new DesiredPose(0.7, 0.4, 0.25));
        route.add(new DesiredPose(0.7, 0.6, 0.25));
        route.add(new DesiredPose(0.6, 0.7, 0.5));
        route.add(new DesiredPose(0.4, 0.7, 0.5));
        route.add(new DesiredPose(0.2, 0.7, 0.5));
        route.add(new DesiredPose(0.1, 0.6, 0.75));
        route.add(new DesiredPose(0.1, 0.4, 0.75));
        route.add(new DesiredPose(0.1, 0.2, 0.75));
        route.add(new DesiredPose(0.1, 0.0, 0.75));

        var desiredVelocity = new DesiredVelocity();
        var desiredWheelSpeed = new DesiredWheelSpeed();

        var poseEstimator = new PoseEstimator(trackedState, estimations);

        //var routeController = new RouteController(route, desiredVelocity, estimations, controls, maxAcceleration);
        var routeController = new BezierController(route, desiredVelocity, estimations, controls, maxAcceleration);
        var velocityController = new VelocityController(desiredVelocity, desiredWheelSpeed, estimations);

        var leftAccelerationLimiter = new AccelerationLimiter(maxAcceleration, signal -> controls.velLeft = signal);
        var rightAccelerationLimiter = new AccelerationLimiter(maxAcceleration, signal -> controls.velRight = signal);

        var leftPidController = new SpeedPIDController(
                0.9, 0.00, 0.3, () -> estimations.leftSpeed,
                () -> desiredWheelSpeed.leftSpeed, leftAccelerationLimiter::setControlInput,
                () -> controls.velLeft
        );
        var rightPidController = new SpeedPIDController(
                0.9, 0.00, 0.3, () -> estimations.rightSpeed,
                () -> desiredWheelSpeed.rightSpeed, rightAccelerationLimiter::setControlInput,
                () -> controls.velRight
        );
        var leftSpeedEstimator = new SpeedEstimator(
                () -> trackedState.leftWheelEncoder, newSpeed -> estimations.leftSpeed = newSpeed
        );
        var rightSpeedEstimator = new SpeedEstimator(
                () -> trackedState.rightWheelEncoder, newSpeed -> estimations.rightSpeed = newSpeed
        );

        var updater = new ControllerUpdater();

        updater.addController(updateFunction, 1);
        updater.addController(routeController, 50);
        updater.addController(leftPidController, 70);
        updater.addController(rightPidController, 70);
        updater.addController(leftAccelerationLimiter, 10);
        updater.addController(rightAccelerationLimiter, 10);
        updater.addController(velocityController, 91);
        updater.addController(leftSpeedEstimator, 30);
        updater.addController(rightSpeedEstimator, 30);
        updater.addController(poseEstimator, 30);

        var monitorFrame = new JFrame();
        monitorFrame.setSize(800, 500);
        monitorFrame.setAutoRequestFocus(false);
        monitorFrame.setLocation(1200, 200);
        monitorFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        monitorFrame.add(new MonitorBoard(trackedState, controls, estimations));
        monitorFrame.setVisible(true);

        var simulatorFrame = new JFrame();
        simulatorFrame.setSize(1200, 800);
        simulatorFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        simulatorFrame.add(new SimulatorBoard(estimations, route));
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
