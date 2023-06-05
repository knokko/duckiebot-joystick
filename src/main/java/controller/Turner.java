package controller;

import java.util.function.BiConsumer;
import java.util.function.DoubleSupplier;

import static java.lang.Math.abs;

public class Turner {

    private final BiConsumer<Double, Double> controlWheels;
    private final DoubleSupplier getControlX, getControlY;

    public Turner(BiConsumer<Double, Double> controlWheels, DoubleSupplier getControlX, DoubleSupplier getControlY) {
        this.controlWheels = controlWheels;
        this.getControlX = getControlX;
        this.getControlY = getControlY;
    }

    private void control(double x, double y) {
        // left = y + x
        // right = y - x
        controlWheels.accept(x * 0.5, y);
    }

    private void accelerateTo(double targetX, double targetY, double delta) throws InterruptedException {
        double currentX = getControlX.getAsDouble();
        double currentY = getControlY.getAsDouble();

        double deltaX = targetX - currentX;
        double deltaY = targetY - currentY;
        double distanceX = abs(deltaX);
        double distanceY = abs(deltaY);

        int numSteps = (int) Math.ceil(Math.max(distanceX, distanceY) / delta);
        double stepSizeX = deltaX / numSteps;
        double stepSizeY = deltaY / numSteps;

        for (int step = 0; step < numSteps; step++) {
            currentX += stepSizeX;
            currentY += stepSizeY;
            controlWheels.accept(currentX, currentY);
            Thread.sleep(50);
        }
    }

    private void accelerateTo(double targetX, double targetY) throws InterruptedException {
        accelerateTo(targetX, targetY, 0.1);
    }

    private void turn(double factorX) {
        new Thread(() -> {
            try {
                accelerateTo(0.33 * factorX, 0.85);
                accelerateTo(0, 0);
            } catch (InterruptedException shouldNotHappen) {
                throw new Error(shouldNotHappen);
            }
        }).start();
    }

    public void turnLeft() {
        turn(-1);
    }

    public void turnRight() {
        turn(1);
    }
}
