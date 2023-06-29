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

    // Ramping parameters
    private double setPoint = 0;
    private int derivativeBackPropagator = 5;

    private LinkedList<ErrorSample> errorList = new LinkedList<>();
    record ErrorSample(double timestamp, double error) {}

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
        // PID variables
        double errorP = 0;
        double errorI = 0;
        double errorD = 0;

        double rawAngleToGoal = desiredVelocity.angle - estimations.angle;
        double angleToGoal = smartAngle(rawAngleToGoal);

        // Setpoint ramping
        //double rampingSpeed = 0.9 - 0.85 * (abs(estimations.leftSpeed) + abs(estimations.rightSpeed)) * 0.5;
        double rampingSpeed = 0.6;

        setPoint += Math.signum(angleToGoal) * Math.min(abs(smartAngle(desiredVelocity.angle - setPoint)), rampingSpeed * deltaTime);
        if (setPoint < 0) setPoint += 1;
        if (setPoint > 1) setPoint -= 1;

        // Window the error list
        if(errorList.size() > 10000){
            errorList.removeFirst();
        }

        // Calculate PID
        var error = smartAngle(setPoint - estimations.angle);

        // Calculate Last error
        double lastError = 0;
        int lastErrorIndex = Math.max(errorList.size()-derivativeBackPropagator, 0);
        if (!errorList.isEmpty()) {
            lastError = errorList.get(lastErrorIndex).error;
        }

        // Propotional
        errorP = error;

        // Intergral
        for (int i = 0; i < errorList.size(); i++) {
            errorI += errorList.get(i).error * errorList.get(i).timestamp;
        }
        errorList.add(new ErrorSample(deltaTime, error));

        // Derivative
        double timeDiff = 0;
        for (int i = lastErrorIndex; i < errorList.size() ; i++) {
            timeDiff += errorList.get(i).timestamp;
        }

        if(timeDiff > 0){
            errorD = (error - lastError) / timeDiff;
        }else{
            errorD = 0;
        }

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
        } else errorList.clear();

        controls.velLeft = finalLeftSpeed;
        controls.velRight = finalRightSpeed;
    }
}
