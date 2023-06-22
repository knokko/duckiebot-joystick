package controller;
import state.DuckieControls;


import controller.desired.DesiredVelocity;
import controller.desired.DesiredWheelSpeed;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;

import static controller.util.DuckieWheels.DISTANCE_BETWEEN_WHEELS;

public class DifferentialDriver implements ControllerFunction {

    private final DesiredVelocity desiredVelocity;
    private final DesiredWheelSpeed desiredWheelSpeed;
    private final DuckieEstimations estimations;
    private final DuckieControls controls;

    // PID variables
    private double errorP = 0;
    private double errorI = 0;
    private double errorD = 0;

    // Ramping parameters
    private double setPoint = 0;
    private double rampingSpeed = 0.1;


    public DifferentialDriver(DesiredVelocity desiredVelocity, DesiredWheelSpeed desiredWheelSpeed, DuckieEstimations estimations, DuckieControls controls) {
        this.desiredVelocity = desiredVelocity;
        this.desiredWheelSpeed = desiredWheelSpeed;
        this.estimations = estimations;
        this.controls = controls;
    }

    @Override
    public void update(double deltaTime) {
        // Setpoint ramping
        setPoint += Math.min(desiredVelocity.angle - setPoint, rampingSpeed * deltaTime);
        double errorAngle = setPoint - estimations.angle;

        // If the error is larger than 0.5 turns, we should rotate the other way instead
        if (errorAngle > 0.5) errorAngle -= 1;
        if (errorAngle < -0.5) errorAngle += 1;

        // Calculate PID
        errorP = errorAngle;
        errorI += errorAngle * deltaTime;
        errorD = (errorAngle - errorP) / deltaTime;

        double Kp = 0.5;
        double Ki = 0.3;
        double Kd = 0.05;

        double angleCorrection = Kp * errorP + Ki * errorI + Kd * errorD;

        // TODO Ensure that this stays in range [-maxSpeed, maxSpeed]
        desiredWheelSpeed.leftSpeed -= angleCorrection;
        desiredWheelSpeed.rightSpeed += angleCorrection;

        if(desiredWheelSpeed.leftSpeed > 0 && desiredWheelSpeed.rightSpeed > 0){
            controls.velLeft = desiredWheelSpeed.leftSpeed;
            controls.velRight = desiredWheelSpeed.rightSpeed;
        }
    }
}
