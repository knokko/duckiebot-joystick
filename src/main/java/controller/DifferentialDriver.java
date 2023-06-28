package controller;

import controller.parameters.PIDParameters;
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
    private final PIDParameters pid;

    // PID variables
    private double errorP = 0;
    private double errorI = 0;
    private double errorD = 0;

    // Ramping parameters
    private double setPoint = 0;

    private LinkedList<Double> errorList = new LinkedList<>();


    public DifferentialDriver(
            DesiredVelocity desiredVelocity, DesiredWheelSpeed desiredWheelSpeed, DuckieEstimations estimations,
            DuckieControls controls, PIDParameters pid
    ) {
        this.desiredVelocity = desiredVelocity;
        this.desiredWheelSpeed = desiredWheelSpeed;
        this.estimations = estimations;
        this.controls = controls;
        this.pid = pid;
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
        if (setPoint < 0) setPoint += 1;
        if (setPoint > 1) setPoint -= 1;

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

        double correctionP = pid.Kp * errorP;
        double correctionI = pid.Ki * errorI;
        double correctionD = pid.Kd * errorD;
        pid.correctionP = correctionP;
        pid.correctionI = correctionI;
        pid.correctionD = correctionD;
        double angleCorrection = correctionP + correctionI + correctionD;

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
