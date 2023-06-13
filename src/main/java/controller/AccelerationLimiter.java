package controller;

import controller.updater.ControllerFunction;

import java.util.function.DoubleConsumer;

public class AccelerationLimiter implements ControllerFunction {

    /**
     * The maximum acceleration, in units per second
     */
    private final double maxAcceleration;
    private final DoubleConsumer updateControlInput;
    private volatile double currentControlInput;
    private double lastControlInput;

    public AccelerationLimiter(double maxAcceleration, DoubleConsumer updateControlInput) {
        this.maxAcceleration = maxAcceleration;
        this.updateControlInput = updateControlInput;
    }

    public void setControlInput(double value) {
        this.currentControlInput = value;
    }

    @Override
    public void update(double deltaTime) {
        double controlInput = this.currentControlInput;
        double controlDelta = controlInput - lastControlInput;

        double maxControlDelta = maxAcceleration * deltaTime;
        if (controlDelta > maxControlDelta) controlDelta = maxControlDelta;
        if (controlDelta < -maxControlDelta) controlDelta = -maxControlDelta;

        double newControlInput = lastControlInput + controlDelta;
        if (newControlInput > 1) newControlInput = 1;
        if (newControlInput < -1) newControlInput = -1;
        updateControlInput.accept(newControlInput);
        this.lastControlInput = newControlInput;
    }
}
