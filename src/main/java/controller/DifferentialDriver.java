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

    @Override
    public void update(double deltaTime) {
        // Setpoint ramping
        double errorAngle = desiredVelocity.angle - estimations.angle;

        // If the error is larger than 0.5 turns, we should rotate the other way instead
        if (errorAngle > 0.5) errorAngle -= 1;
        if (errorAngle < -0.5) errorAngle += 1;

        double rampingSpeed = 0.9 - 0.75 * (abs(estimations.leftSpeed) + abs(estimations.rightSpeed)) * 0.5;

        setPoint += Math.signum(errorAngle - setPoint)*Math.min(abs(errorAngle - setPoint), rampingSpeed * deltaTime);

        // Window the error list
        if(errorList.size() > 10000){
            errorList.removeFirst();
        }

        // Calculate PID
        var error = setPoint - estimations.angle;
        if (error > 0.5) error -= 1.0;
        if (error < -0.5) error += 1.0;

        errorP = error;
        errorList.add(error * deltaTime);
        errorI = errorList.stream().mapToDouble(Double::doubleValue).sum();
        errorD = error / deltaTime;

        double Kp = 1.1;
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
