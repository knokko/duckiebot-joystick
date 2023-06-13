package controller;

import controller.desired.DesiredVelocity;
import controller.desired.DesiredWheelSpeed;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;

import static controller.util.DuckieWheels.DISTANCE_BETWEEN_WHEELS;
import static java.lang.Math.max;

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
        double speedChangeInterval;
        if (Double.isNaN(estimations.leftSpeedChangeInterval) || Double.isNaN(estimations.rightSpeedChangeInterval)) speedChangeInterval = 0.3;
        else speedChangeInterval = max(estimations.leftSpeedChangeInterval, estimations.rightSpeedChangeInterval);

        double errorAngle = desiredVelocity.angle - estimations.angle;

        // If the error is larger than 0.5 turns, we should rotate the other way instead
        if (errorAngle > 0.5) errorAngle -= 1;
        if (errorAngle < -0.5) errorAngle += 1;

        //double turnTime = max(2 * (speedChangeInterval + deltaTime), 3.0 * errorAngle);
        double turnTime = desiredVelocity.turnTime;
        double extraDistanceRight = Math.PI * DISTANCE_BETWEEN_WHEELS * errorAngle;
        double angleCorrection = extraDistanceRight / turnTime;

        double desiredSpeed = desiredVelocity.speed;

        // TODO Ensure that this stays in range [-maxSpeed, maxSpeed]
        desiredWheelSpeed.leftSpeed = desiredSpeed - angleCorrection;
        desiredWheelSpeed.rightSpeed = desiredSpeed + angleCorrection;
        //System.out.printf("desiredSpeedLeft is %.3f and desiredSpeedRight is %.3f and turnTime is %.3f and angleCorrection is %.3f and errorAngle is %.3f and speedChangeInterval is %.3f \n", desiredSpeedLeft, desiredSpeedRight, turnTime, angleCorrection, errorAngle, speedChangeInterval);
    }
}
