package controller.estimation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class LatencyEstimator {

    static List<IndexedEntry> findPeaks(List<IndexedEntry> entries, double window) {
        List<IndexedEntry> peakMap = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            IndexedEntry currentPeak = entries.get(index);
            if (index > 0) {
                IndexedEntry oldPeak = entries.get(index - 1);
            }
            peakMap.add(currentPeak);
        }

        // TODO Maybe finish this
        return null;
    }

    private final List<Entry> entries = new ArrayList<>();
    private final DoubleSupplier getCurrentValue;
    private final DoubleConsumer setEstimation;

    private double globalTime;

    public LatencyEstimator(DoubleSupplier getCurrentValue, DoubleConsumer setEstimation) {
        this.getCurrentValue = getCurrentValue;
        this.setEstimation = setEstimation;
    }

    public void update(double deltaTime) {
        globalTime += deltaTime;

        entries.add(new Entry(globalTime, getCurrentValue.getAsDouble()));
        entries.removeIf(entry -> entry.timestamp < globalTime - 5000);

        List<IndexedEntry> indexedEntries = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            indexedEntries.add(new IndexedEntry(index, entry.timestamp, entry.value));
        }

        findPeaks(indexedEntries, 0.1);
    }
    private record Entry(double timestamp, double value) {}
    private record IndexedEntry(int index, double timestamp, double value) {}
}
