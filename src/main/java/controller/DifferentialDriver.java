package controller;
import state.DuckieControls;


import controller.desired.DesiredVelocity;
import controller.desired.DesiredWheelSpeed;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;

import java.util.LinkedList;

import static java.lang.Math.abs;

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

    private LinkedList<Double> errorList = new LinkedList<>();


    public DifferentialDriver(DesiredVelocity desiredVelocity, DesiredWheelSpeed desiredWheelSpeed, DuckieEstimations estimations, DuckieControls controls) {
        this.desiredVelocity = desiredVelocity;
        this.desiredWheelSpeed = desiredWheelSpeed;
        this.estimations = estimations;
        this.controls = controls;
    }

    private double smartAngle(double angle) {
        if (angle > 0.5) return angle - 1;
        if (angle < -0.5) return angle + 1;
        return angle;
    }

    @Override
    public void update(double deltaTime) {
        double rawAngleToGoal = desiredVelocity.angle - estimations.angle;
        double angleToGoal = smartAngle(rawAngleToGoal);

        // Setpoint ramping
        //double rampingSpeed = 0.9 - 0.85 * (abs(estimations.leftSpeed) + abs(estimations.rightSpeed)) * 0.5;
        double rampingSpeed = 0.2;

        setPoint += Math.signum(angleToGoal) * Math.min(abs(smartAngle(desiredVelocity.angle - setPoint)), rampingSpeed * deltaTime);

        // Window the error list
        if(errorList.size() > 10000){
            errorList.removeFirst();
        }

        // Calculate PID
        var error = smartAngle(setPoint - estimations.angle);

        errorP = error;
        errorList.add(error * deltaTime);
        errorI = errorList.stream().mapToDouble(Double::doubleValue).sum();
        errorD = error / deltaTime;

        double Kp = 1.0;
        double Ki = 0.00;
        double Kd = 0.00000;


        double angleCorrection = Kp * errorP + Ki * errorI + Kd * errorD;
        System.out.printf("P: %.2f, I: %.2f, D: %.2f errD: %.2f\n", Kp*errorP, Ki*errorI, Kd*errorD, errorD);

        // Print PID values


        // Calculate desired wheel speeds

        double finalLeftSpeed = desiredWheelSpeed.leftSpeed;
        double finalRightSpeed = desiredWheelSpeed.rightSpeed;
        if(finalLeftSpeed != 0 && finalRightSpeed != 0){
            finalLeftSpeed -= angleCorrection;
            finalRightSpeed += angleCorrection;
        }

        controls.velLeft = finalLeftSpeed;
        controls.velRight = finalRightSpeed;
    }
}
