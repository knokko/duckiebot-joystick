package simulator;

import java.util.ArrayList;
import java.util.List;

public class SimulatorLatency<T> {

    private final List<Entry<T>> entries = new ArrayList<>();
    private final double latency;
    private final T defaultValue;

    public SimulatorLatency(double latency, T defaultValue) {
        this.latency = latency;
        this.defaultValue = defaultValue;
    }

    public void insert(double currentTime, T value) {
        entries.add(new Entry<>(currentTime, value));
    }

    public T get(double currentTime) {
        Entry<T> latestVisibleEntry = null;
        for (var entry : entries) {
            if (entry.timestamp + latency <= currentTime) latestVisibleEntry = entry;
            else break;
        }

        if (latestVisibleEntry != null) {
            var resultEntry = latestVisibleEntry;
            entries.removeIf(entry -> entry.timestamp < resultEntry.timestamp);
            return resultEntry.value;
        } else return defaultValue;
    }

    private record Entry<T>(double timestamp, T value) {}
}
