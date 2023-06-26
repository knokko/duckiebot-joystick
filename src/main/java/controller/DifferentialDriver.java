package controller;
import state.DuckieControls;


import controller.desired.DesiredVelocity;
import controller.desired.DesiredWheelSpeed;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;

import java.util.LinkedList;

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
    private double rampingSpeed = 0.5;

    private LinkedList<Double> errorList = new LinkedList<>();


    public DifferentialDriver(DesiredVelocity desiredVelocity, DesiredWheelSpeed desiredWheelSpeed, DuckieEstimations estimations, DuckieControls controls) {
        this.desiredVelocity = desiredVelocity;
        this.desiredWheelSpeed = desiredWheelSpeed;
        this.estimations = estimations;
        this.controls = controls;
    }

    @Override
    public void update(double deltaTime) {
        // Setpoint ramping
        double errorAngle = desiredVelocity.angle - estimations.angle;

        // If the error is larger than 0.5 turns, we should rotate the other way instead
        if (errorAngle > 0.5) errorAngle -= 1;
        if (errorAngle < -0.5) errorAngle += 1;

        setPoint += Math.signum(errorAngle - setPoint)*Math.min(Math.abs(errorAngle - setPoint), rampingSpeed * deltaTime);

        // Window the error list
        if(errorList.size() > 10000){
            errorList.removeFirst();
        }

        // Calculate PID
        errorP = setPoint;
        errorList.add(setPoint * deltaTime);
        errorI = errorList.stream().mapToDouble(Double::doubleValue).sum();
        errorD = (setPoint - errorP) / deltaTime;

        double Kp = 1.9;
        double Ki = 0.05;
        double Kd = 0.275;

        double angleCorrection = Kp * errorP + Ki * errorI + Kd * errorD;

        // Print PID values

        // Calculate desired wheel speeds
        if(desiredWheelSpeed.leftSpeed != 0 && desiredWheelSpeed.rightSpeed != 0){
            desiredWheelSpeed.leftSpeed -= angleCorrection;
            desiredWheelSpeed.rightSpeed += angleCorrection;
        }

        controls.velLeft = desiredWheelSpeed.leftSpeed;
        controls.velRight = desiredWheelSpeed.rightSpeed;
    }
}
