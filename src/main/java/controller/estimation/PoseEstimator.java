package controller.estimation;

import controller.updater.ControllerFunction;
import state.DuckieState;

import static controller.util.DuckieBot.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class PoseEstimator implements ControllerFunction {

    private final DuckieState trackedState;
    private final DuckieEstimations estimations;

    private DuckieState.WheelEncoderEntry lastLeftWheelTick, lastRightWheelTick;

    public PoseEstimator(DuckieState trackedState, DuckieEstimations estimations) {
        this.trackedState = trackedState;
        this.estimations = estimations;

        this.lastLeftWheelTick = trackedState.leftWheelEncoder;
        this.lastRightWheelTick = trackedState.rightWheelEncoder;
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    @Override
    public void update(double deltaTime) {
        if (this.lastLeftWheelTick == null || this.lastRightWheelTick == null) {
            this.lastLeftWheelTick = trackedState.leftWheelEncoder;
            this.lastRightWheelTick = trackedState.rightWheelEncoder;
        }
        var currentLeftTicks = trackedState.leftWheelEncoder;
        var currentRightTicks = trackedState.rightWheelEncoder;

        if (currentLeftTicks == null || currentRightTicks == null || lastLeftWheelTick == null || lastRightWheelTick == null) return;
        if (currentLeftTicks == this.lastLeftWheelTick && currentRightTicks == this.lastRightWheelTick) return;

        double leftDistance = (currentLeftTicks.value() - lastLeftWheelTick.value()) * WHEEL_RADIUS * 2 * Math.PI / WHEEL_TICKS_PER_TURN;
        double rightDistance = (currentRightTicks.value() - lastRightWheelTick.value()) * WHEEL_RADIUS * 2 * Math.PI / WHEEL_TICKS_PER_TURN;

        double averageDistance = (leftDistance + rightDistance) * 0.5;
        double angleRadians = estimations.angle * 2 * Math.PI;
        estimations.x += averageDistance * cos(angleRadians);
        estimations.y += averageDistance * sin(angleRadians);

        double deltaAngle = (rightDistance - leftDistance) / (2 * Math.PI * DISTANCE_BETWEEN_WHEELS);
        double newAngle = estimations.angle + deltaAngle;
        if (newAngle < 0) newAngle += 1;
        if (newAngle > 1) newAngle -= 1;

        estimations.angle = newAngle;

        lastLeftWheelTick = currentLeftTicks;
        lastRightWheelTick = currentRightTicks;
    }
}
