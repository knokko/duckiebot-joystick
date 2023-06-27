package controller;

import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import static java.lang.Math.abs;

public class SpeedPolyController implements ControllerFunction {

    private final DuckieEstimations estimations;
    private final DoubleSupplier desiredSpeed;
    private final DoubleConsumer setThrottle;

    private double globalTime;

    private double currentThrottle = 0.0;
    private double currentStepSize = 0.0;

    public SpeedPolyController(DuckieEstimations estimations, DoubleSupplier desiredSpeed, DoubleConsumer setThrottle) {
        this.estimations = estimations;
        this.desiredSpeed = desiredSpeed;
        this.setThrottle = setThrottle;
    }

    private Motion predictMotion() {
        return null;
    }

    private void controlThrottle(double deltaTime) {
        globalTime += deltaTime;

        currentThrottle += currentStepSize * deltaTime;

        double targetSpeed = desiredSpeed.getAsDouble();

        double stepFactor = 0.1;
        var distancePoly = estimations.distancePolynomial;
        if (distancePoly != null) {
            var speedPoly = distancePoly.getDerivative();
            double referenceTime = globalTime + 0.3 + 0.4 * abs(targetSpeed - speedPoly.get(globalTime));
            double expectedSpeed = speedPoly.get(referenceTime);
            if (expectedSpeed > targetSpeed) stepFactor = -stepFactor;
        }

        if (stepFactor > 0.0 && currentStepSize < 0.0) currentStepSize = 0.0;
        else if (stepFactor < 0.0 && currentStepSize > 0.0) currentStepSize = 0.0;
        else currentStepSize += stepFactor * deltaTime;

        if (targetSpeed == 0.0 && abs(currentThrottle) < 0.2) {
            currentThrottle = 0.0;
            currentStepSize = 0.0;
        }

        if (currentThrottle > 1.0) {
            currentThrottle = 1.0;
            currentStepSize = 0.0;
        }

        setThrottle.accept(currentThrottle);
    }

    @Override
    public void update(double deltaTime) {
        controlThrottle(deltaTime);
    }


    private record Motion(double speed, double acceleration) {}
}