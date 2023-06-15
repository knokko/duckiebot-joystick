package controller.estimation;

import controller.updater.ControllerFunction;
import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.Vector;

import java.util.LinkedList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import static controller.util.DuckieWheels.WHEEL_RADIUS;
import static controller.util.DuckieWheels.WHEEL_TICKS_PER_TURN;

public class SpeedEstimator implements ControllerFunction {

    private final Supplier<Integer> wheelTicks;
    private final DoubleConsumer speedEstimation;
    private final DuckieEstimations.SpeedFunction speedFunction;

    private double globalTimer;
    private double quickestSpeedChangeInterval = Double.NaN;
    private final List<PositionEntry> lastEntries = new LinkedList<>();

    public SpeedEstimator(
            Supplier<Integer> wheelTicks, DoubleConsumer speedEstimation, DuckieEstimations.SpeedFunction speedFunction
    ) {
        this.wheelTicks = wheelTicks;
        this.speedEstimation = speedEstimation;
        this.speedFunction = speedFunction;
    }

    @Override
    public void update(double deltaTime) {
        globalTimer += deltaTime;
        Integer currentTicks = wheelTicks.get();

        if (Double.isNaN(quickestSpeedChangeInterval) || currentTicks == null) {
            //speedEstimation.accept(0);
        } else {
            var lastEntry = lastEntries.get(lastEntries.size() - 1);
            double distance = (currentTicks - lastEntry.wheelTicks) * 2 * Math.PI * WHEEL_RADIUS / WHEEL_TICKS_PER_TURN;
            //speedEstimation.accept(distance / (globalTimer - lastEntry.timeStamp));
        }

        if (currentTicks != null) {
            for (var entry : lastEntries) {
                if (entry.wheelTicks != currentTicks) {
                    double speedChangeInterval = globalTimer - entry.timeStamp;
                    if (Double.isNaN(quickestSpeedChangeInterval) || speedChangeInterval < quickestSpeedChangeInterval) {
                        quickestSpeedChangeInterval = speedChangeInterval;
                        break;
                    }
                }
            }
        }

        // Delete entries older than 3 * quickestSpeedChangeInterval
        //double maxTimeDifference = 3 * quickestSpeedChangeInterval;
        double maxTimeDifference = 0.35; // was 0.05
        lastEntries.removeIf(entry -> entry.timeStamp < globalTimer - maxTimeDifference);

        if (currentTicks != null) lastEntries.add(0, new PositionEntry(currentTicks, globalTimer));

        updateSpeedFunction();
    }

    private void updateSpeedFunction() {
        if (lastEntries.size() > 2) {
            long startTime = System.nanoTime();
            Matrix timeMatrix = Matrix.zero(lastEntries.size(), 3);
            Vector positionVector = Vector.zero(lastEntries.size());
            for (int index = 0; index < lastEntries.size(); index++) {
                var entry = lastEntries.get(index);
                timeMatrix.set(index, 0, 1.0);
                timeMatrix.set(index, 1, entry.timeStamp);
                timeMatrix.set(index, 2, entry.timeStamp * entry.timeStamp);
                positionVector.set(index, entry.wheelTicks);
            }

            Matrix transposedTimes = timeMatrix.transpose();
            Vector coefficients = transposedTimes.multiply(timeMatrix).withInverter(LinearAlgebra.InverterFactory.GAUSS_JORDAN)
                    .inverse().multiply(transposedTimes).multiply(positionVector);
            speedFunction.positionOffset = coefficients.get(0);
            speedFunction.positionLinear = coefficients.get(1);
            speedFunction.positionQuadratic = coefficients.get(2);

            // position = offset + linear * time + quad * time * time ->
            // d_position / dt = linear + 2 * quad * time
            speedFunction.speedOffset = speedFunction.positionLinear;
            speedFunction.speedLinear = 2 * speedFunction.positionQuadratic;
            speedFunction.currentTime = globalTimer;
            long endTime = System.nanoTime();
            speedEstimation.accept((speedFunction.positionLinear + 2 * globalTimer * speedFunction.positionQuadratic) * 2 * Math.PI * WHEEL_RADIUS / WHEEL_TICKS_PER_TURN);
        } else speedEstimation.accept(0.0);
    }

    private record PositionEntry(int wheelTicks, double timeStamp) {
    }
}
