package controller;

import controller.desired.DesiredVelocity;
import controller.desired.DesiredWheelSpeed;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;

import java.util.LinkedList;

public class DirectSpeedPIDController implements ControllerFunction {

    private final DesiredVelocity desiredVelocity;
    private final DesiredWheelSpeed desiredWheelSpeed;
    private final DuckieEstimations estimations;

    // PID variables
    private double errorP = 0;
    private double errorI = 0;
    private double errorD = 0;

    // Ramping parameters
    private double setPoint = 0;
    private double rampingSpeed = 1;

    private LinkedList<Double> errorList = new LinkedList<>();

    public DirectSpeedPIDController(DesiredVelocity desiredVelocity, DesiredWheelSpeed desiredWheelSpeed, DuckieEstimations estimations) {
        this.desiredVelocity = desiredVelocity;
        this.desiredWheelSpeed = desiredWheelSpeed;
        this.estimations = estimations;
    }

    @Override
    public void update(double deltaTime) {
        // Setpoint ramping
        double speed = (estimations.leftSpeed + estimations.rightSpeed)/2;

        if(Math.abs(desiredVelocity.speed) < 0.01 && Math.abs(speed) < 0.2){
            desiredWheelSpeed.rightSpeed = 0;
            desiredWheelSpeed.leftSpeed = 0;
            errorList.clear();
            setPoint = 0.0;
            return;
        }

        if (desiredVelocity.speed != 0 && speed != 0 && Math.signum(desiredVelocity.speed) != Math.signum(speed)) {
            errorList.clear();
            setPoint = 0.0;
        }

        setPoint += Math.min(desiredVelocity.speed - setPoint, rampingSpeed * deltaTime);
        double errorSpeed = setPoint - speed;

        // Window the error list
        if(errorList.size() > 1000){
            errorList.removeFirst();
        }

        // Calculate PID
        errorP = errorSpeed;
        errorList.add(errorSpeed * deltaTime);
        errorI = errorList.stream().mapToDouble(Double::doubleValue).sum();
        errorD = (errorSpeed - errorP) / deltaTime;

        double Kp = 0.75;
        double Ki = 0.5;
        double Kd = 0.2;

        double speedInput = Kp * errorP + Ki * errorI + Kd * errorD;

        // TODO Ensure that this stays in range [-maxSpeed, maxSpeed]
        desiredWheelSpeed.leftSpeed = speedInput;
        desiredWheelSpeed.rightSpeed = speedInput;
    }
}
