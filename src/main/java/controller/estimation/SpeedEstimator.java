package controller.estimation;

import controller.updater.ControllerFunction;
import controller.util.Polynomial;

import java.util.LinkedList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import static controller.util.DuckieWheels.*;

public class SpeedEstimator implements ControllerFunction {

    private final Supplier<Integer> wheelTicks;
    private final DoubleConsumer speedEstimation;

    private double globalTimer;
    private final List<PositionEntry> lastEntries = new LinkedList<>();

    public SpeedEstimator(Supplier<Integer> wheelTicks, DoubleConsumer speedEstimation) {
        this.wheelTicks = wheelTicks;
        this.speedEstimation = speedEstimation;
    }

    @Override
    public void update(double deltaTime) {
        globalTimer += deltaTime;
        Integer currentTicks = wheelTicks.get();

        double maxTimeDifference = 0.35; // was 0.05
        lastEntries.removeIf(entry -> entry.timeStamp < globalTimer - maxTimeDifference);

        if (currentTicks != null) lastEntries.add(0, new PositionEntry(currentTicks, globalTimer));

        var poly = Polynomial.fit(lastEntries, 2);
        if (poly != null) {
            speedEstimation.accept(poly.getDerivative().get(globalTimer));
        } else {
            speedEstimation.accept(0.0);
        }
    }

    private record PositionEntry(int wheelTicks, double timeStamp) implements Polynomial.Entry {

        @Override
        public double getT() {
            return timeStamp;
        }

        @Override
        public double getY() {
            return wheelTicks * WHEEL_CIRCUMFERENCE / WHEEL_TICKS_PER_TURN;
        }
    }
}
