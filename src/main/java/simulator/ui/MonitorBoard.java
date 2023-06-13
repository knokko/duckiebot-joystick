package simulator.ui;

import controller.estimation.DuckieEstimations;
import state.DuckieControls;
import state.DuckieState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.DoubleSupplier;

public class MonitorBoard extends JPanel {

    private final Collection<GraphSequence> graphSequences = new ArrayList<>();

    public MonitorBoard(DuckieState trackedState, DuckieControls controls, DuckieEstimations estimations) {
        graphSequences.add(new GraphSequence("Left control input", new Color(200, 150, 0), () -> controls.velLeft));
        graphSequences.add(new GraphSequence("Right control input", new Color(180, 170, 20), () -> controls.velRight));
        graphSequences.add(new GraphSequence("Left speed", new Color(0, 50, 200), () -> estimations.leftSpeed));
        graphSequences.add(new GraphSequence("Right speed", new Color(20, 70, 180), () -> estimations.rightSpeed));
        graphSequences.add(new GraphSequence("Left control output", new Color(250, 0, 0), () -> trackedState.leftWheelControl));
        graphSequences.add(new GraphSequence("Right control output", new Color(230, 0, 20), () -> trackedState.rightWheelControl));
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
