package controller.util;

import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.Vector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

public class Polynomial {

    public static <T extends Entry> Polynomial fit(Collection<T> entries, int degree) {
        if (entries.isEmpty()) return null;

        double referenceY = entries.stream().min(Comparator.comparingDouble(Entry::getY)).get().getY();
        double referenceT = entries.iterator().next().getT();

        Matrix timeMatrix = Matrix.zero(entries.size(), degree + 1);
        Vector distanceVector = Vector.zero(entries.size());
        int row = 0;
        for (var entry : entries) {
            double timeFactor = 1.0;
            for (int column = 0; column <= degree; column++) {
                timeMatrix.set(row, column, timeFactor);
                timeFactor *= (entry.getT() - referenceT);
            }
            distanceVector.set(row, entry.getY() - referenceY);
            row += 1;
        }

        Matrix transposedTimes = timeMatrix.transpose();
        try {
            Vector coefficients = transposedTimes.multiply(timeMatrix).withInverter(LinearAlgebra.InverterFactory.GAUSS_JORDAN)
                    .inverse().multiply(transposedTimes).multiply(distanceVector);
            return new Polynomial(coefficients).withReferenceT(referenceT);
        } catch (IllegalArgumentException notInvertible) {
            return null;
        }
    }

    private final double[] coefficients;
    private double referenceT;

    public Polynomial(double... coefficients) {
        this.coefficients = coefficients.length == 0 ? new double[1] : coefficients;
    }

    public Polynomial(Vector coefficients) {
        this.coefficients = new double[coefficients.length()];
        for (int index = 0; index < coefficients.length(); index++) {
            this.coefficients[index] = coefficients.get(index);
        }
    }

    public Polynomial withReferenceT(double referenceT) {
        this.referenceT = referenceT;
        return this;
    }

    @Override
    public String toString() {
        var result = new StringBuilder("y = " + coefficients[0]);
        if (coefficients.length > 1) result.append(" + ").append(coefficients[1]).append("t");
        for (int index = 2; index < coefficients.length; index++) {
            result.append(" + ").append(coefficients[index]).append("t^").append(index);
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Polynomial poly) {
            return Arrays.equals(this.coefficients, poly.coefficients);
        } else return false;
    }

    public double get(double t) {
        double factor = 1.0;
        double result = 0.0;
        for (double coefficient : coefficients) {
            result += factor * coefficient;
            factor *= (t - referenceT);
        }
        return result;
    }

    public Polynomial getDerivative() {
        if (coefficients.length == 1) return new Polynomial(new double[1]);
        double[] derivative = new double[coefficients.length - 1];
        for (int index = 0; index < derivative.length; index++) {
            derivative[index] = (index + 1) * coefficients[index + 1];
        }
        return new Polynomial(derivative).withReferenceT(referenceT);
    }

    public interface Entry {

        double getT();

        double getY();
    }
}