package controller;

import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class SpeedPIDController implements ControllerFunction {

    private final double pGain, iGain, dGain;
    private final DuckieEstimations.PIDValues pidValues;
    private final DoubleSupplier estimatedSpeed, getDesiredSpeed, getControlInput;
    private final DoubleConsumer setControlInput;

    private double lastSpeed;
    private double lastError = Double.NaN;
    private double errorSum;

    public SpeedPIDController(
            double pGain, double iGain, double dGain, DuckieEstimations.PIDValues pidValues,
            DoubleSupplier estimatedSpeed, DoubleSupplier getDesiredSpeed,
            DoubleConsumer setControlInput, DoubleSupplier getControlInput
    ) {
        this.pGain = pGain;
        this.iGain = iGain;
        this.dGain = dGain;
        this.pidValues = pidValues;
        this.estimatedSpeed = estimatedSpeed;
        this.getDesiredSpeed = getDesiredSpeed;
        this.setControlInput = setControlInput;
        this.getControlInput = getControlInput;
    }

    @Override
    public void update(double deltaTime) {
        System.out.printf("deltaTime is %.4f\n", deltaTime);
        double currentSpeed = estimatedSpeed.getAsDouble(); // meters per second

        // Don't update faster than the speed estimator
        if (currentSpeed != 0 && currentSpeed == lastSpeed) return;

        double desiredSpeed = getDesiredSpeed.getAsDouble();
        double error = desiredSpeed - currentSpeed; // meters per second
        if (Double.isNaN(lastError)) lastError = error;

        if (desiredSpeed == 0.0) {
            setControlInput.accept(0.0);
        } else {
            double derivativeError = (error - lastError) / deltaTime; // meters per second^2

            double pValue = pGain * error;
            double iValue = iGain * errorSum;
            double dValue = dGain * derivativeError;

            setControlInput.accept(pValue + iValue + dValue);
            pidValues.p = pValue;
            pidValues.i = iValue;
            pidValues.d = dValue;
        }

        lastError = error;
        errorSum += error * deltaTime;
        lastSpeed = currentSpeed;
    }
}
