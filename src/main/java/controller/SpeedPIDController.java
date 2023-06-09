package controller;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class SpeedPIDController {

    private final double pGain, iGain, dGain; // 10, 0.9, 5
    private final DoubleSupplier estimatedSpeed, getDesiredSpeed, getControlInput;
    private final DoubleConsumer setControlInput;

    private double lastSpeed;
    private double lastError = Double.NaN;
    private double errorSum;

    public SpeedPIDController(
            double pGain, double iGain, double dGain,
            DoubleSupplier estimatedSpeed, DoubleSupplier getDesiredSpeed,
            DoubleConsumer setControlInput, DoubleSupplier getControlInput
    ) {
        this.pGain = pGain;
        this.iGain = iGain;
        this.dGain = dGain;
        this.estimatedSpeed = estimatedSpeed;
        this.getDesiredSpeed = getDesiredSpeed;
        this.setControlInput = setControlInput;
        this.getControlInput = getControlInput;
    }

    public void update(double deltaTime) {
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
            double dValue = dGain * derivativeError * deltaTime;

            setControlInput.accept(getControlInput.getAsDouble() + pValue + iValue + dValue);
        }

        lastError = error;
        errorSum += error * deltaTime;
        lastSpeed = currentSpeed;
    }
}
