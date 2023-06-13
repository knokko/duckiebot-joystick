package simulator;

import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import state.DuckieControls;
import state.DuckiePose;
import state.DuckieState;

import static controller.util.DuckieWheels.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class Simulator implements ControllerFunction {

    private final Terrain terrain;
    public final DuckiePose realPose;
    public final DuckieEstimations estimations;
    private double exactLeftWheelTicks, exactRightWheelTicks;
    public final DuckieControls controls;
    public final DuckieState trackedState;

    private final SimulatorLatency<Double> leftControl, rightControl;
    private final SimulatorLatency<Integer> leftTicks, rightTicks;

    private double currentTime = 0.0;

    public Simulator(Terrain terrain) {
        this(terrain, 0.0, 0.0, 0.0, 0.0);
    }

    public Simulator(
            Terrain terrain,
            double leftControlLatency, double rightControlLatency,
            double leftTickLatency, double rightTickLatency
    ) {
        this.terrain = terrain;
        this.realPose = new DuckiePose();
        this.estimations = new DuckieEstimations();
        this.controls = new DuckieControls();
        this.trackedState = new DuckieState();

        this.leftControl = new SimulatorLatency<>(leftControlLatency, 0.0);
        this.rightControl = new SimulatorLatency<>(rightControlLatency, 0.0);
        this.leftTicks = new SimulatorLatency<>(leftTickLatency, 0);
        this.rightTicks = new SimulatorLatency<>(rightTickLatency, 0);
    }

    /**
     * This should be called 1000 times per second
     */
    @Override
    public synchronized void update(double deltaTime) {
        currentTime += deltaTime;

        leftControl.insert(currentTime, controls.velLeft);
        rightControl.insert(currentTime, controls.velRight);

        realPose.x += deltaTime * realPose.velocityX;
        realPose.y += deltaTime * realPose.velocityY;

        double leftVelocity = terrain.leftSpeedFunction.apply(leftControl.get(currentTime));
        double rightVelocity = terrain.rightSpeedFunction.apply(rightControl.get(currentTime));
        double averageVelocity = 0.5 * (leftVelocity + rightVelocity);
        double angleRadians = realPose.angle * 2 * Math.PI; // Convert turns to radians
        realPose.velocityX = averageVelocity * cos(angleRadians);
        realPose.velocityY = averageVelocity * sin(angleRadians);

        realPose.angle += deltaTime * (rightVelocity - leftVelocity) / (2 * Math.PI * DISTANCE_BETWEEN_WHEELS);
        if (realPose.angle >= 1) realPose.angle -= 1;
        if (realPose.angle < 0) realPose.angle += 1;

        this.exactLeftWheelTicks += WHEEL_TICKS_PER_TURN * deltaTime * leftVelocity / (2 * Math.PI * WHEEL_RADIUS);
        this.exactRightWheelTicks += WHEEL_TICKS_PER_TURN * deltaTime * rightVelocity / (2 * Math.PI * WHEEL_RADIUS);
        leftTicks.insert(currentTime, (int) exactLeftWheelTicks);
        rightTicks.insert(currentTime, (int) exactRightWheelTicks);
        trackedState.leftWheelEncoder = leftTicks.get(currentTime);
        trackedState.rightWheelEncoder = rightTicks.get(currentTime);
        trackedState.leftWheelControl = leftControl.get(currentTime);
        trackedState.rightWheelControl = rightControl.get(currentTime);
    }
}
