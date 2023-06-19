package controller.estimation;

import controller.updater.ControllerFunction;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.DoubleSupplier;

import static java.lang.Math.abs;

public class TransferFunctionEstimator implements ControllerFunction {

    private final DoubleSupplier throttle;
    private final DoubleSupplier speed;
    private final DuckieEstimations.TransferFunction transferFunction;
    private final Buckets buckets = new Buckets(0.025, 95);
    private final BufferedImage plot = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
    private final Graphics2D graphics = plot.createGraphics();

    public TransferFunctionEstimator(
            DoubleSupplier throttle, DoubleSupplier speed,
            DuckieEstimations.TransferFunction transferFunction
    ) {
        this.throttle = throttle;
        this.speed = speed;
        this.transferFunction = transferFunction;

        this.graphics.setColor(Color.RED);
        this.graphics.drawLine(0, plot.getHeight() / 2, plot.getWidth(), plot.getHeight() / 2);
        this.graphics.drawLine(plot.getWidth() / 2, 0, plot.getWidth() / 2, plot.getHeight());
        this.graphics.setColor(Color.BLUE);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ImageIO.write(plot, "PNG", new File("plot-" + System.currentTimeMillis() + ".png"));
                Thread.sleep(100);
            } catch (IOException | InterruptedException failed) {
                throw new RuntimeException(failed);
            }
        }));
    }

    public void update(double deltaTime) {
        double currentThrottle = throttle.getAsDouble();
        double currentSpeed = speed.getAsDouble();
        buckets.insert(currentThrottle, currentSpeed);

        Double expectedSpeed = buckets.getSpeed(currentThrottle);

        int dotRadius = 2;
        graphics.setColor(Color.BLUE);
        this.graphics.fillOval(
                (int) ((0.5 + 0.5 * currentThrottle) * plot.getWidth()) - dotRadius,
                (int) ((0.5 - 0.5 * currentSpeed) * plot.getHeight()) - dotRadius,
                2 * dotRadius, 2 * dotRadius
        );
        if (expectedSpeed != null) {
            double roundedThrottle = buckets.getRoundedThrottle(currentThrottle);
            graphics.setColor(Color.GREEN);
            this.graphics.fillOval(
                    (int) ((0.5 + 0.5 * roundedThrottle) * plot.getWidth()) - dotRadius,
                    (int) ((0.5 - 0.5 * expectedSpeed) * plot.getHeight()) - dotRadius,
                    2 * dotRadius, 2 * dotRadius
            );
            if (abs(expectedSpeed) > 0.01 && roundedThrottle >= transferFunction.throttle) {
                transferFunction.throttle = roundedThrottle;
                transferFunction.slope = expectedSpeed / roundedThrottle;
            }
        }
    }

    static class Bucket {

        private final int size;
        private final java.util.List<Double> values = new ArrayList<>();

        Bucket(int size) {
            this.size = size;
            if (size % 2 == 0) throw new IllegalArgumentException("Size must be odd");
        }

        void insert(double value) {
            int insertionIndex;
            for (insertionIndex = 0; insertionIndex < values.size() && value < values.get(insertionIndex); insertionIndex++) ;
            values.add(insertionIndex, value);
            if (values.size() > size) {
                if (insertionIndex > size / 2) values.remove(0);
                else values.remove(values.size() - 1);
            }
        }

        Double getMedian() {
            if (values.size() == size) return values.get(size / 2);
            else return null;
        }
    }

    static class Buckets {

        private final double bucketWidth;
        private final int bucketSize;

        private final Map<Integer, Bucket> bucketMap = new HashMap<>();

        Buckets(double bucketWidth, int bucketSize) {
            this.bucketWidth = bucketWidth;
            this.bucketSize = bucketSize;
        }

        private int getBucketIndex(double throttle) {
            return (int) Math.floor(throttle / bucketWidth);
        }

        private Bucket getBucket(double throttle) {
            return bucketMap.computeIfAbsent(getBucketIndex(throttle), index -> new Bucket(bucketSize));
        }

        void insert(double throttle, double speed) {
            getBucket(throttle).insert(speed);
        }

        Double getSpeed(double throttle) {
            return getBucket(throttle).getMedian();
        }

        double getRoundedThrottle(double throttle) {
            return bucketWidth * getBucketIndex(throttle) + bucketWidth * 0.5f;
        }
    }
}
