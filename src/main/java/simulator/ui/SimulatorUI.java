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

    public static void main(String[] args) throws InterruptedException {
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
                    Terrain.IDEAL, 0.08, 0.08, 0.0, 0.0
            );
            estimations = simulator.estimations;
            controls = simulator.controls;
            trackedState = simulator.trackedState;
            updateFunction = simulator;
        }

        double maxAcceleration = 5.7;

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

        var routeController = new BezierController(route, desiredVelocity, estimations, controls, maxAcceleration);
        //var routeController = new StepController(route, desiredVelocity, estimations, controls, maxAcceleration);
        var differentialDriver = new DifferentialDriver(desiredVelocity, desiredWheelSpeed, estimations, controls);
        var directSpeedController = new DirectSpeedPIDController(desiredVelocity, desiredWheelSpeed, estimations);

        var leftSpeedEstimator = new SpeedEstimator(
                () -> trackedState.leftWheelEncoder, newSpeed -> estimations.leftSpeed = newSpeed, estimations.leftSpeedFunction
        );
        var rightSpeedEstimator = new SpeedEstimator(
                () -> trackedState.rightWheelEncoder, newSpeed -> estimations.rightSpeed = newSpeed, estimations.rightSpeedFunction
        );

        var updater = new ControllerUpdater();

        desiredVelocity.speed = 0.01;
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
        monitorFrame.add(new MonitorBoard(trackedState, controls, estimations));
        monitorFrame.setVisible(true);

        var simulatorFrame = new JFrame();
        simulatorFrame.setSize(1200, 800);
        simulatorFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        simulatorFrame.add(new SimulatorBoard(estimations, desiredVelocity, route));
        simulatorFrame.setVisible(true);

        Thread updateThread = new Thread(updater::start);
        updateThread.setDaemon(true);
        updateThread.start();

        boolean[] drawing = {true};
        Thread repaintThread = new Thread(() -> {
            try {
                while (drawing[0]) {
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

        Thread.sleep(30000);
        route.clear();
        drawing[0] = false;
        controls.velRight = 0.0;
        controls.velLeft = 0.0;
        desiredVelocity.speed = 0.0;
        Thread.sleep(100);
        monitorFrame.dispose();
    }
}
