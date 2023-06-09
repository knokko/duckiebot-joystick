package simulator;

import controller.estimation.DuckieEstimations;
import state.DuckieControls;
import state.DuckiePose;
import state.DuckieState;

import static controller.util.DuckieWheels.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class Simulator {

    private final Terrain terrain;
    public final DuckiePose realPose;
    public final DuckieEstimations estimations;
    private double exactLeftWheelTicks, exactRightWheelTicks;
    public final DuckieControls controls; // TODO Add control latency
    public final DuckieState trackedState;

    public Simulator(Terrain terrain) {
        this.terrain = terrain;
        this.realPose = new DuckiePose();
        this.estimations = new DuckieEstimations();
        this.controls = new DuckieControls();
        this.trackedState = new DuckieState();
    }

    /**
     * This should be called 1000 times per second
     */
    public synchronized void update(double deltaTime) {
        realPose.x += deltaTime * realPose.velocityX;
        realPose.y += deltaTime * realPose.velocityY;

        // TODO Add control latency
        double leftVelocity = terrain.leftSpeedFunction.apply(controls.velLeft);
        double rightVelocity = terrain.rightSpeedFunction.apply(controls.velRight);
        double averageVelocity = 0.5 * (leftVelocity + rightVelocity);
        double angleRadians = realPose.angle * 2 * Math.PI; // Convert turns to radians
        realPose.velocityX = averageVelocity * cos(angleRadians);
        realPose.velocityY = averageVelocity * sin(angleRadians);

        realPose.angle += deltaTime * (rightVelocity - leftVelocity) / (2 * Math.PI * DISTANCE_BETWEEN_WHEELS);
        if (realPose.angle >= 1) realPose.angle -= 1;
        if (realPose.angle < 0) realPose.angle += 1;

        // Add feedback latency
        this.exactLeftWheelTicks += WHEEL_TICKS_PER_TURN * deltaTime * leftVelocity / (2 * Math.PI * WHEEL_RADIUS);
        this.exactRightWheelTicks += WHEEL_TICKS_PER_TURN * deltaTime * rightVelocity / (2 * Math.PI * WHEEL_RADIUS);
        trackedState.leftWheelEncoder = (int) exactLeftWheelTicks;
        trackedState.rightWheelEncoder = (int) exactRightWheelTicks;
    }
}
