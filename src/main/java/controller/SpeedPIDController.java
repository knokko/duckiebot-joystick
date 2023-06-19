package controller;

import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import static java.lang.Math.abs;

public class SpeedPIDController implements ControllerFunction {

    private final double pGain, iGain, dGain;
    private final DuckieEstimations.PIDValues pidValues;
    private final DoubleSupplier estimatedSpeed, getDesiredSpeed, getControlInput;
    private final DoubleConsumer setControlInput;

    private final List<ErrorEntry> lastErrors = new ArrayList<>();
    private double globalTime = 0.0;
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
        globalTime += deltaTime;
        double currentSpeed = estimatedSpeed.getAsDouble(); // meters per second

        // Don't update faster than the speed estimator
        if (currentSpeed != 0 && currentSpeed == lastSpeed) return;

        double desiredSpeed = getDesiredSpeed.getAsDouble();
        double error = desiredSpeed - currentSpeed; // meters per second
        if (Double.isNaN(lastError)) lastError = error;

        if (desiredSpeed == 0.0) {
            setControlInput.accept(0.0);
        } else {
            double derivativeError;
            if (lastErrors.size() > 3) {
                Matrix timeMatrix = Matrix.zero(lastErrors.size(), 3);
                Vector errorVector = Vector.zero(lastErrors.size());
                int index = 0;
                for (var entry : lastErrors) {
                    timeMatrix.set(index, 0, 1.0);
                    timeMatrix.set(index, 1, entry.timestamp);
                    timeMatrix.set(index, 2, entry.timestamp * entry.timestamp);
                    errorVector.set(index, entry.error);
                    index += 1;
                }

                Matrix transposedTimes = timeMatrix.transpose();
                Vector coefficients = transposedTimes.multiply(timeMatrix).withInverter(LinearAlgebra.InverterFactory.GAUSS_JORDAN)
                        .inverse().multiply(transposedTimes).multiply(errorVector);
                derivativeError = coefficients.get(1) + 2 * globalTime * coefficients.get(2);
            } else {
                //System.out.printf("error is %.3f and lastError is %.3f and deltaTime is %.3f\n", error, lastError, deltaTime);
                //derivativeError = (error - lastError) / deltaTime; // meters per second^2
                derivativeError = 0.0;
            }

            double pValue = pGain * error;
            double iValue = iGain * errorSum;
            double dValue = dGain * derivativeError;

            setControlInput.accept(getControlInput.getAsDouble() + pValue + iValue + dValue);
            pidValues.p = pValue;
            pidValues.i = iValue;
            pidValues.d = dValue;
            System.out.printf("error is %.3f and derivativeError is %.3f\n", error, derivativeError);
        }

        lastError = error;
        errorSum += error * deltaTime;
        lastSpeed = currentSpeed;

        lastErrors.add(new ErrorEntry(globalTime, error, desiredSpeed));
        lastErrors.removeIf(entry -> (entry.timestamp < globalTime - 0.1) || (abs(desiredSpeed - entry.desiredSpeed) > 0.8 * abs(entry.error)));
    }

    private record ErrorEntry(double timestamp, double error, double desiredSpeed) {}
}
