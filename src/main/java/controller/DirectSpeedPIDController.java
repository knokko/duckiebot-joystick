package controller;

import controller.desired.DesiredVelocity;
import controller.desired.DesiredWheelSpeed;
import controller.estimation.DuckieEstimations;
import controller.parameters.PIDParameters;
import controller.updater.ControllerFunction;

import java.util.LinkedList;

import static java.lang.Math.abs;

public class DirectSpeedPIDController implements ControllerFunction {

    private final DesiredVelocity desiredVelocity;
    private final DesiredWheelSpeed desiredWheelSpeed;
    private final DuckieEstimations estimations;
    private final PIDParameters pid;

    // Ramping parameters
    private double setPoint = 0;
    private double rampingSpeed = 1;
    private double speedInput = 0;
    private int derivativeBackPropagator = 5;

    private LinkedList<ErrorSample> errorList = new LinkedList<>();
    record ErrorSample(double timestamp, double error) {}
    
    public DirectSpeedPIDController(
            DesiredVelocity desiredVelocity, DesiredWheelSpeed desiredWheelSpeed,
            DuckieEstimations estimations, PIDParameters pid
    ) {
        this.desiredVelocity = desiredVelocity;
        this.desiredWheelSpeed = desiredWheelSpeed;
        this.estimations = estimations;
        this.pid = pid;
    }

    @Override
    public void update(double deltaTime) {
        // PID variables
        double errorP = 0;
        double errorI = 0;
        double errorD = 0;

        // Setpoint ramping
        double speed = (estimations.leftSpeed + estimations.rightSpeed)/2;

        if(abs(desiredVelocity.speed) < 0.01 && abs(speed) < 0.05){
            desiredWheelSpeed.rightSpeed = 0;
            desiredWheelSpeed.leftSpeed = 0;
            errorList.clear();
            setPoint = 0.0;
            return;
        }

        if (desiredVelocity.speed != 0 && abs(speed) > 0.1 && Math.signum(desiredVelocity.speed) != Math.signum(speed)) {
            errorList.clear();
            setPoint = 0.0;
        }

        //setPoint +=                sign                      *                      magnitude
        setPoint += Math.signum(desiredVelocity.speed - speed) * Math.min(abs((desiredVelocity.speed - speed)), rampingSpeed * deltaTime);
        //double errorSpeed = setPoint - speed;
        double errorSpeed = desiredVelocity.speed - speed;

        // Window the error list
        if(errorList.size() > 10000){
            errorList.removeFirst();
        }

        // Calculate Last error
        double lastError = 0;
        int lastErrorIndex = Math.max(errorList.size()-derivativeBackPropagator, 0);
        if (!errorList.isEmpty()) {
            lastError = errorList.get(lastErrorIndex).error;
        }

        // Propotional
        errorP = errorSpeed;

        // Intergral
        for (int i = 0; i < errorList.size(); i++) {
            errorI += errorList.get(i).error * errorList.get(i).timestamp;
        }
        errorList.add(new ErrorSample(deltaTime, errorSpeed));

        // Derivative
        double timeDiff = 0;
        for (int i = lastErrorIndex; i < errorList.size() ; i++) {
            timeDiff += errorList.get(i).timestamp;
        }

        if(timeDiff > 0){
            errorD = (errorSpeed - lastError) / timeDiff;
        }else{
            errorD = 0;
        }

        double correctionP = pid.Kp * errorP;
        double correctionI = pid.Ki * errorI;
        double correctionD = pid.Kd * errorD;
        pid.correctionP = correctionP;
        pid.correctionI = correctionI;
        pid.correctionD = correctionD;

        speedInput += correctionP*deltaTime + correctionI + correctionD*deltaTime*deltaTime;

        // TODO Ensure that this stays in range [-maxSpeed, maxSpeed]
        if(Math.signum(speedInput) != Math.signum(desiredVelocity.speed)){
            speedInput = 0;
        }
        desiredWheelSpeed.leftSpeed = speedInput;
        desiredWheelSpeed.rightSpeed = speedInput;
    }
}
