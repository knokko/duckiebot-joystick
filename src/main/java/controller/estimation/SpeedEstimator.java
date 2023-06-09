package controller.estimation;

import java.util.LinkedList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

import static controller.util.DuckieWheels.WHEEL_RADIUS;
import static controller.util.DuckieWheels.WHEEL_TICKS_PER_TURN;

public class SpeedEstimator {

    private final IntSupplier wheelTicks;
    private final DoubleConsumer speedEstimation;
    private final DoubleSupplier getQuickestSpeedChangeInterval;
    private final DoubleConsumer setQuickestSpeedChangeInterval;

    private double globalTimer;
    private final List<PositionEntry> lastEntries = new LinkedList<>();

    public SpeedEstimator(
            IntSupplier wheelTicks, DoubleConsumer speedEstimation,
            DoubleSupplier getQuickestSpeedChangeInterval, DoubleConsumer setQuickestSpeedChangeInterval
    ) {
        this.wheelTicks = wheelTicks;
        this.speedEstimation = speedEstimation;
        this.getQuickestSpeedChangeInterval = getQuickestSpeedChangeInterval;
        this.setQuickestSpeedChangeInterval = setQuickestSpeedChangeInterval;
    }

    public void update(double deltaTime) {
        globalTimer += deltaTime;
        int currentTicks = wheelTicks.getAsInt();
        double quickestSpeedChangeInterval = getQuickestSpeedChangeInterval.getAsDouble();
        if (Double.isNaN(quickestSpeedChangeInterval)) {
            speedEstimation.accept(0);
        } else {
            var lastEntry = lastEntries.get(lastEntries.size() - 1);
            double distance = (currentTicks - lastEntry.wheelTicks) * 2 * Math.PI * WHEEL_RADIUS / WHEEL_TICKS_PER_TURN;
            speedEstimation.accept(distance / (globalTimer - lastEntry.timeStamp));
        }

        for (var entry : lastEntries) {
            if (entry.wheelTicks != currentTicks) {
                double speedChangeInterval = globalTimer - entry.timeStamp;
                if (Double.isNaN(quickestSpeedChangeInterval) || speedChangeInterval < quickestSpeedChangeInterval) {
                    quickestSpeedChangeInterval = speedChangeInterval;
                    setQuickestSpeedChangeInterval.accept(speedChangeInterval);
                    break;
                }
            }
        }
        if (!Double.isNaN(quickestSpeedChangeInterval)) {
            lastEntries.removeIf(entry -> entry.timeStamp < globalTimer - 2 * getQuickestSpeedChangeInterval.getAsDouble());
        }
        lastEntries.add(0, new PositionEntry(currentTicks, globalTimer));
    }

    private record PositionEntry(int wheelTicks, double timeStamp) {
    }
}
