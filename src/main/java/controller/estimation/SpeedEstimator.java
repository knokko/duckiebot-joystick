package controller.estimation;

import controller.updater.ControllerFunction;
import controller.util.Polynomial;
import state.DuckieState;

import java.util.LinkedList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import static controller.util.DuckieBot.*;

public class SpeedEstimator implements ControllerFunction {

    private final Supplier<DuckieState.WheelEncoderEntry> wheelTicks;
    private final DoubleConsumer speedEstimation;

    private final List<PositionEntry> lastEntries = new LinkedList<>();
    private long startEncoderTime, startJavaTime;

    public SpeedEstimator(Supplier<DuckieState.WheelEncoderEntry> wheelTicks, DoubleConsumer speedEstimation) {
        this.wheelTicks = wheelTicks;
        this.speedEstimation = speedEstimation;
    }

    @Override
    public void update(double deltaTime) {
        var currentTicks = wheelTicks.get();
        if (currentTicks == null) return;

        if (startEncoderTime == 0) {
            startEncoderTime = currentTicks.timestamp();
            startJavaTime = System.nanoTime();
        }

        double maxTimeDifference = 0.15; // was 0.05

        var nextEntry = new PositionEntry(currentTicks.value(), (currentTicks.timestamp() - startEncoderTime) / 1_000_000_000.0);
        double currentTime = (System.nanoTime() - startJavaTime) / 1_000_000_000.0;
        lastEntries.removeIf(entry -> entry.timeStamp < currentTime - maxTimeDifference);
        lastEntries.add(0, nextEntry);

        var poly = Polynomial.fit(lastEntries, 1);
        if (poly != null) {
            speedEstimation.accept(poly.getDerivative().get(currentTime));
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
