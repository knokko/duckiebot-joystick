package simulator.ui;

import controller.AccelerationLimiter;
import controller.RouteController;
import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.SpeedPIDController;
import controller.VelocityController;
import controller.desired.DesiredWheelSpeed;
import controller.estimation.PoseEstimator;
import controller.estimation.SpeedEstimator;
import simulator.Simulator;
import simulator.Terrain;

import javax.swing.*;

import java.util.LinkedList;

import static java.lang.Math.*;
import static java.lang.Thread.sleep;

public class SimulatorUI {

    private static void addLeftTurn(LinkedList<DesiredPose> route, int startX, int startY, double currentAngle) {
        double angle = 2 * Math.PI * currentAngle;
        double radius = 0.1;
        double centerX = 0.2 * (max(0, cos(angle) - sin(angle)) + startX);
        double centerY = 0.2 * (max(0, cos(angle) + sin(angle)) + startY);

        for (int part = 1; part <= 4; part++) {
            double partAngle = angle - Math.toRadians(90) + 0.5 * Math.PI * part / 4.0;
            route.add(new DesiredPose(centerX + radius * cos(partAngle), centerY + radius * sin(partAngle), 0));
        }
        var lastPose = route.getLast();
        route.add(new DesiredPose(lastPose.x - 0.16 * sin(angle), lastPose.y + 0.16 * cos(angle), 0));
    }

    public static void main(String[] args) {
        var simulator = new Simulator(Terrain.IDEAL);

        double maxAcceleration = 1.0;

        var route = new LinkedList<DesiredPose>();
        route.add(new DesiredPose(0.36, 0.1, 0));
        route.add(new DesiredPose(0.56, 0.1, 0));
        addLeftTurn(route, 2, 0, 0);
        route.add(new DesiredPose(0.7, 0.56, 0));
        addLeftTurn(route, 3, 2, 0.25);
        route.add(new DesiredPose(0.24, 0.7, 0));
        addLeftTurn(route, 1, 3, 0.5);
        route.add(new DesiredPose(0.1, 0.04, 0));
        System.out.println("route is " + route);

        var board = new SimulatorBoard(simulator, route);

        var desiredVelocity = new DesiredVelocity();
        desiredVelocity.angle = 0.125;
        desiredVelocity.speed = 0.2;
        var desiredWheelSpeed = new DesiredWheelSpeed();

        var poseEstimator = new PoseEstimator(simulator.trackedState, simulator.estimations);

        var routeController = new RouteController(route, desiredVelocity, simulator.estimations, simulator.controls, maxAcceleration);
        var velocityController = new VelocityController(desiredVelocity, desiredWheelSpeed, simulator.estimations);

        var leftAccelerationLimiter = new AccelerationLimiter(maxAcceleration, signal -> simulator.controls.velLeft = signal);
        var rightAccelerationLimiter = new AccelerationLimiter(maxAcceleration, signal -> simulator.controls.velRight = signal);

        var leftPidController = new SpeedPIDController(
                0.9, 0.00, 0.3, () -> simulator.estimations.leftSpeed,
                () -> desiredWheelSpeed.leftSpeed, leftAccelerationLimiter::setControlInput,
                () -> simulator.controls.velLeft
        );
        var rightPidController = new SpeedPIDController(
                0.9, 0.00, 0.3, () -> simulator.estimations.rightSpeed,
                () -> desiredWheelSpeed.rightSpeed, rightAccelerationLimiter::setControlInput,
                () -> simulator.controls.velRight
        );
        var leftSpeedEstimator = new SpeedEstimator(
                () -> simulator.trackedState.leftWheelEncoder, newSpeed -> simulator.estimations.leftSpeed = newSpeed,
                () -> simulator.estimations.leftSpeedChangeInterval, newInterval -> simulator.estimations.leftSpeedChangeInterval = newInterval
        );
        var rightSpeedEstimator = new SpeedEstimator(
                () -> simulator.trackedState.rightWheelEncoder, newSpeed -> simulator.estimations.rightSpeed = newSpeed,
                () -> simulator.estimations.rightSpeedChangeInterval, newInterval -> simulator.estimations.rightSpeedChangeInterval = newInterval
        );

        var frame = new JFrame();
        frame.setSize(1200, 800);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(board);
        frame.setVisible(true);

        Thread updateThread = new Thread(() -> {
            try {
                long updateCounter = 0;

                long lastUpdateTime = System.nanoTime();
                long lastPidTime = System.nanoTime();
                long lastVelocityTime = System.nanoTime();
                long lastLimitTime = System.nanoTime();
                long lastSpeedTime = System.nanoTime();
                long lastRouteTime = System.nanoTime();

                while (true) {
                    sleep(1);
                    long currentTime = System.nanoTime();
                    double deltaUpdateTime = (currentTime - lastUpdateTime) / 1_000_000_000.0;
                    simulator.update(deltaUpdateTime);
                    lastUpdateTime = currentTime;

                    updateCounter += 1;
                    if (updateCounter == 5000) {
                        desiredVelocity.angle = 0.5;
                    }
                    if (updateCounter % 50 == 0) {
                        double deltaRouteTime = (currentTime - lastRouteTime) / 1_000_000_000.0;
                        routeController.update(deltaRouteTime);
                        System.out.printf("(x, y) = (%.2f, %.2f)\n", simulator.estimations.x, simulator.estimations.y);
                        lastRouteTime = currentTime;
                    }
                    if (updateCounter % 70 == 0) {
                        double deltaPidTime = (currentTime - lastPidTime) / 1_000_000_000.0;
                        leftPidController.update(deltaPidTime);
                        rightPidController.update(deltaPidTime);
                        lastPidTime = currentTime;
                    }
                    if (updateCounter % 10 == 0) {
                        double deltaLimitTime = (currentTime - lastLimitTime) / 1_000_000_000.0;
                        leftAccelerationLimiter.update(deltaLimitTime);
                        rightAccelerationLimiter.update(deltaLimitTime);
                        lastLimitTime = currentTime;
                    }
                    if (updateCounter % 91 == 0) {
                        double deltaVelocityTime = (currentTime - lastVelocityTime) / 1_000_000_000.0;
                        velocityController.update(deltaVelocityTime);
                        lastVelocityTime = currentTime;
                    }
                    if (updateCounter % 30 == 0) {
                        double deltaSpeedTime = (currentTime - lastSpeedTime) / 1_000_000_000.0;
                        leftSpeedEstimator.update(deltaSpeedTime);
                        rightSpeedEstimator.update(deltaSpeedTime);
                        poseEstimator.update();
                        //System.out.printf("Estimated left speed is %.3f and desired left speed is %.3f \n", simulator.estimations.leftSpeed, velocityController.desiredSpeedLeft);
                        lastSpeedTime = currentTime;
                    }
                }
            } catch (InterruptedException shouldNotHappen) {
                throw new Error(shouldNotHappen);
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();

        Thread repaintThread = new Thread(() -> {
            try {
                while (true) {
                    sleep(20);
                    SwingUtilities.invokeLater(frame::repaint);
                }
            } catch (InterruptedException shouldNotHappen) {
                throw new Error(shouldNotHappen);
            }
        });
        repaintThread.setDaemon(true);
        repaintThread.start();
    }
}
