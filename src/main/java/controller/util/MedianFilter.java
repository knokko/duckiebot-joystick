package controller.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MedianFilter {

    private final int maxSize;
    private final List<Double> values = new LinkedList<>();

    public MedianFilter(int maxSize) {
        this.maxSize = maxSize;
    }

    public double get() {
        double[] sortedValues = new double[values.size()];
        int index = 0;
        for (double value : values) {
            sortedValues[index] = value;
            index += 1;
        }
        Arrays.sort(sortedValues);
        return sortedValues[sortedValues.length / 2];
    }

    public void insert(double value) {
        values.add(value);
        if (values.size() > maxSize) values.remove(0);
    }
}
