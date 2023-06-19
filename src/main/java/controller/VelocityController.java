package controller;

import controller.desired.DesiredVelocity;
import controller.desired.DesiredWheelSpeed;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;

import static controller.util.DuckieWheels.DISTANCE_BETWEEN_WHEELS;

public class VelocityController implements ControllerFunction {

    private final DesiredVelocity desiredVelocity;
    private final DesiredWheelSpeed desiredWheelSpeed;
    private final DuckieEstimations estimations;

    public VelocityController(
            DesiredVelocity desiredVelocity, DesiredWheelSpeed desiredWheelSpeed, DuckieEstimations estimations
    ) {
        this.desiredVelocity = desiredVelocity;
        this.desiredWheelSpeed = desiredWheelSpeed;
        this.estimations = estimations;
    }

    @Override
    public void update(double deltaTime) {
        double errorAngle = desiredVelocity.angle - estimations.angle;

        // If the error is larger than 0.5 turns, we should rotate the other way instead
        if (errorAngle > 0.5) errorAngle -= 1;
        if (errorAngle < -0.5) errorAngle += 1;

        double turnTime = desiredVelocity.turnTime;
        double extraDistanceRight = Math.PI * DISTANCE_BETWEEN_WHEELS * errorAngle;
        double angleCorrection = extraDistanceRight / turnTime; // Should be PID controlled

        double desiredSpeed = desiredVelocity.speed;

        // TODO Ensure that this stays in range [-maxSpeed, maxSpeed]
        desiredWheelSpeed.leftSpeed = desiredSpeed - angleCorrection;
        desiredWheelSpeed.rightSpeed = desiredSpeed + angleCorrection;
    }
}
