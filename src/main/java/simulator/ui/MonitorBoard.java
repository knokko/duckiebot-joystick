package simulator.ui;

import controller.desired.DesiredWheelSpeed;
import controller.estimation.DuckieEstimations;
import state.DuckieControls;
import state.DuckieState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.DoubleSupplier;

import static controller.util.DuckieWheels.WHEEL_RADIUS;
import static controller.util.DuckieWheels.WHEEL_TICKS_PER_TURN;

public class MonitorBoard extends JPanel {

    private final Collection<GraphSequence> graphSequences = new ArrayList<>();

    private Integer initialLeftTicks, initialRightTicks;

    public MonitorBoard(DuckieState trackedState, DuckieControls controls, DuckieEstimations estimations, DesiredWheelSpeed desiredWheelSpeed) {
        graphSequences.add(new GraphSequence("Left control input", new Color(200, 150, 0), () -> controls.velLeft));
        graphSequences.add(new GraphSequence("Right control input", new Color(250, 190, 20), () -> controls.velRight));
        graphSequences.add(new GraphSequence("Left speed", new Color(0, 50, 180), () -> estimations.leftSpeed));
        graphSequences.add(new GraphSequence("Right speed", new Color(20, 70, 250), () -> estimations.rightSpeed));
        graphSequences.add(new GraphSequence("Left control output", new Color(200, 0, 0), () -> trackedState.leftWheelControl));
        graphSequences.add(new GraphSequence("Right control output", new Color(250, 0, 20), () -> trackedState.rightWheelControl));
        graphSequences.add(new GraphSequence("Left wheel ticks", new Color(0, 200, 10), () -> {
            Integer leftTicks = trackedState.leftWheelEncoder;
            if (leftTicks != null) {
                if (initialLeftTicks == null) initialLeftTicks = leftTicks;
                else return (leftTicks - initialLeftTicks) * 0.002;
            }
            return 0.0;
        }));
        graphSequences.add(new GraphSequence("Right wheel ticks", new Color(0, 250, 30), () -> {
            Integer rightTicks = trackedState.rightWheelEncoder;
            if (rightTicks != null) {
                if (initialRightTicks == null) initialRightTicks = rightTicks;
                else return (rightTicks - initialRightTicks) * 0.002;
            }
            return 0.0;
        }));
        //graphSequences.add(new GraphSequence("Desired left speed", Color.MAGENTA, () -> desiredWheelSpeed.leftSpeed));

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
//        double pidScalar = 100.0;
//        double pidOffset = -0.5;
//        graphSequences.add(new GraphSequence("Left P", new Color(150, 0, 250), () -> estimations.leftPID.p * pidScalar + pidOffset));
//        graphSequences.add(new GraphSequence("Left I", new Color(250, 0, 150), () -> estimations.leftPID.i * pidScalar + pidOffset));
//        graphSequences.add(new GraphSequence("Left D", new Color(0, 250, 250), () -> estimations.leftPID.d * pidScalar + pidOffset));
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

        Toolkit.getDefaultToolkit().sync();
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
