package simulator.ui;

import controller.desired.DesiredVelocity;
import controller.estimation.DuckieEstimations;
import controller.parameters.PIDParameters;
import state.DuckieControls;
import state.DuckieState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.DoubleSupplier;

public class MonitorBoard extends JPanel {

    private final Collection<GraphSequence> graphSequences = new ArrayList<>();
    private final DuckieEstimations estimations;

    private DuckieState.WheelEncoderEntry initialLeftTicks, initialRightTicks;

    private final long startTime = System.currentTimeMillis();

    public MonitorBoard(
            DuckieState trackedState, DuckieControls controls, DuckieEstimations estimations,
            DesiredVelocity desiredVelocity, PIDParameters pid
    ) {
        this.estimations = estimations;
        graphSequences.add(new GraphSequence("Left control input", new Color(200, 150, 0), () -> controls.velLeft));
        graphSequences.add(new GraphSequence("Right control input", new Color(250, 190, 20), () -> controls.velRight));
        graphSequences.add(new GraphSequence("Left speed", new Color(0, 50, 180), () -> estimations.leftSpeed));
        graphSequences.add(new GraphSequence("Right speed", new Color(20, 70, 250), () -> estimations.rightSpeed));
        graphSequences.add(new GraphSequence("Left control output", new Color(200, 0, 0), () -> trackedState.leftWheelControl));
        graphSequences.add(new GraphSequence("Right control output", new Color(250, 0, 20), () -> trackedState.rightWheelControl));
        graphSequences.add(new GraphSequence("Left wheel ticks", new Color(0, 200, 10), () -> {
            var leftTicks = trackedState.leftWheelEncoder;
            if (leftTicks != null) {
                if (initialLeftTicks == null) initialLeftTicks = leftTicks;
                else return (leftTicks.value() - initialLeftTicks.value()) * 0.002;
            }
            return 0.0;
        }));
        graphSequences.add(new GraphSequence("Right wheel ticks", new Color(0, 250, 30), () -> {
            var rightTicks = trackedState.rightWheelEncoder;
            if (rightTicks != null) {
                if (initialRightTicks == null) initialRightTicks = rightTicks;
                else return (rightTicks.value() - initialRightTicks.value()) * 0.002;
            }
            return 0.0;
        }));
        graphSequences.add(new GraphSequence("Average speed", new Color(100, 50, 180), () -> {
            var poly = estimations.distancePolynomial;
            if (poly != null) {
                return poly.getDerivative().get((System.currentTimeMillis() - startTime) * 0.001);
            } else return 0.0;
        }));
        graphSequences.add(new GraphSequence("Desired average speed", Color.MAGENTA, () -> desiredVelocity.speed));

//        graphSequences.add(new GraphSequence("Left wheel estimator", new Color(200, 0, 200), () -> {
//            if (initialLeftTicks != null) {
//                var speedFunction = estimations.leftSpeedFunction;
//                return (speedFunction.positionOffset + speedFunction.positionLinear * speedFunction.currentTime + speedFunction.positionQuadratic * speedFunction.currentTime * speedFunction.currentTime - initialLeftTicks) * 0.002;
//            } else return 0.0;
//        }));
//        graphSequences.add(new GraphSequence("Left wheel derivative", new Color(0, 200, 200), () -> {
//            if (initialLeftTicks != null) {
//                var speedFunction = estimations.leftSpeedFunction;
//                return (speedFunction.speedOffset + speedFunction.currentTime * speedFunction.speedLinear) * WHEEL_RADIUS * 2 * Math.PI / WHEEL_TICKS_PER_TURN;
//            } else return 0.0;
//        }));
//        graphSequences.add(new GraphSequence("Left transfer slope", new Color(12, 67, 120), () -> estimations.leftTransfer.slope * 0.6));
//        graphSequences.add(new GraphSequence("Left transfer slope", new Color(22, 77, 140), () -> estimations.rightTransfer.slope * 0.6));
//
        double pidScalar = 1.0;
        double pidOffset = -0.5;
        graphSequences.add(new GraphSequence(
                "Angle correction P", new Color(150, 0, 250),
                () -> pid.correctionP * pidScalar + pidOffset
        ));
        graphSequences.add(new GraphSequence(
                "Angle correction I", new Color(250, 0, 150),
                () -> pid.correctionI * pidScalar + pidOffset
        ));
        graphSequences.add(new GraphSequence(
                "Angle correction D", new Color(0, 250, 250),
                () -> pid.correctionD * pidScalar + pidOffset
        ));
    }

    private static final int GRAPH_WIDTH = 500;
    private static final int GRAPH_HEIGHT = 400;

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);

        long currentTime = System.currentTimeMillis();
        for (var sequence : graphSequences) {
            sequence.update();
        }

        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(0, 0, GRAPH_WIDTH, GRAPH_HEIGHT);
        graphics.setColor(Color.BLACK);
        graphics.drawLine(0, GRAPH_HEIGHT / 2, GRAPH_WIDTH, GRAPH_HEIGHT / 2);

        int labelY = 30;
        for (var sequence : graphSequences) {
            graphics.setColor(sequence.color);
            graphics.drawString(sequence.label, GRAPH_WIDTH + 10, labelY);
            labelY += 30;
            GraphPoint lastPoint = null;
            for (var point : sequence.points) {
                if (lastPoint != null) {
                    // Graph width is 500 pixels and 1 pixel is 8 milliseconds, so 4 seconds in total
                    int x1 = GRAPH_WIDTH - (int) (currentTime - lastPoint.time) / 8;
                    int y1 = (int) ((1.0 - lastPoint.value) * GRAPH_HEIGHT / 2);
                    int x2 = GRAPH_WIDTH - (int) (currentTime - point.time) / 8;
                    int y2 = (int) ((1.0 - point.value) * GRAPH_HEIGHT / 2);
                    graphics.drawLine(x1, y1, x2, y2);
                }
                lastPoint = point;
            }
        }

        graphics.setColor(Color.CYAN);
        graphics.drawString("Speed predictor", GRAPH_WIDTH + 10, labelY);
        for (long time = currentTime - 3500; time < currentTime + 500; time += 50) {
            double polyTime1 = (time - startTime) * 0.001;
            double polyTime2 = (0.05 + time - startTime) * 0.001;
            var poly = estimations.distancePolynomial;
            if (poly != null) {
                poly = poly.getDerivative();
                graphics.drawLine(
                        timeToX(currentTime, time), valueToY(poly.get(polyTime1)),
                        timeToX(currentTime, time + 50), valueToY(poly.get(polyTime2))
                );
            }
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private int timeToX(long currentTime, long measurementTime) {
        return GRAPH_WIDTH - (int) (500 + currentTime - measurementTime) / 8;
    }

    private int valueToY(double value) {
        return (int) ((1.0 - value) * GRAPH_HEIGHT / 2);
    }

    private static class GraphSequence {

        private final java.util.List<GraphPoint> points = new ArrayList<>();
        private final String label;
        private final Color color;
        private final DoubleSupplier getValue;

        GraphSequence(String label, Color color, DoubleSupplier getValue) {
            this.label = label;
            this.color = color;
            this.getValue = getValue;
        }

        void update() {
            long time = System.currentTimeMillis();
            points.add(new GraphPoint(time, getValue.getAsDouble()));
            points.removeIf(point -> time - point.time > 5000);
        }
    }

    private record GraphPoint(long time, double value) {
        @Override
        public String toString() {
            return String.format("GraphPoint(%d, %.2f)", time, value);
        }
    }
}
