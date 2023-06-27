package controller.estimation;

import controller.updater.ControllerFunction;
import controller.util.DuckieWheels;
import controller.util.Polynomial;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

public class SpeedPredictor implements ControllerFunction {

    private final DoubleSupplier getWheelTicks;
    private final Consumer<Polynomial> setPolynomial;

    private final List<PositionEntry> pastPositions = new LinkedList<>();

    private double globalTime;

    public SpeedPredictor(DoubleSupplier getWheelTicks, Consumer<Polynomial> setPolynomial) {
        this.getWheelTicks = getWheelTicks;
        this.setPolynomial = setPolynomial;
    }

    @Override
    public void update(double deltaTime) {
        globalTime += deltaTime;
        double averageTicks = getWheelTicks.getAsDouble();
        if (!Double.isNaN(averageTicks)) {
            double averageDistance = averageTicks * DuckieWheels.WHEEL_CIRCUMFERENCE / DuckieWheels.WHEEL_TICKS_PER_TURN;
            pastPositions.add(new PositionEntry(globalTime, averageDistance));
        }

        pastPositions.removeIf(entry -> entry.time < globalTime - 0.8);

        int degree = 2;
        Polynomial polynomial = Polynomial.fit(pastPositions, degree);
        if (polynomial != null) setPolynomial.accept(polynomial);
    }

    private record PositionEntry(double time, double averageDistance) implements Polynomial.Entry {

        @Override
        public String toString() {
            return String.format("Position(time=%.3f, value=%.3f)", time, averageDistance);
        }

        @Override
        public double getT() {
            return time;
        }

        @Override
        public double getY() {
            return averageDistance;
        }
    }
}
