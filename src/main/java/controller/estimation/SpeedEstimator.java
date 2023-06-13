package controller.estimation;

import controller.updater.ControllerFunction;

import java.util.LinkedList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntSupplier;

import static controller.util.DuckieWheels.WHEEL_RADIUS;
import static controller.util.DuckieWheels.WHEEL_TICKS_PER_TURN;

public class SpeedEstimator implements ControllerFunction {

    private final IntSupplier wheelTicks;
    private final DoubleConsumer speedEstimation;

    private double globalTimer;
    private double quickestSpeedChangeInterval = Double.NaN;
    private final List<PositionEntry> lastEntries = new LinkedList<>();

    public SpeedEstimator(
            IntSupplier wheelTicks, DoubleConsumer speedEstimation
    ) {
        this.wheelTicks = wheelTicks;
        this.speedEstimation = speedEstimation;
    }

    @Override
    public void update(double deltaTime) {
        globalTimer += deltaTime;
        int currentTicks = wheelTicks.getAsInt();

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
                    break;
                }
            }
        }

        // Delete entries older than 3 * quickestSpeedChangeInterval
        double maxTimeDifference = 3 * quickestSpeedChangeInterval;
        lastEntries.removeIf(entry -> entry.timeStamp < globalTimer - maxTimeDifference);

        lastEntries.add(0, new PositionEntry(currentTicks, globalTimer));
    }

    private record PositionEntry(int wheelTicks, double timeStamp) {
    }
}
