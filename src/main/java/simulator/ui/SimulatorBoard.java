package simulator.ui;

import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.estimation.DuckieEstimations;
import planner.GridWall;
import simulator.Simulator;
import simulator.WallGrid;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import static controller.desired.DesiredPose.*;
import static controller.util.DuckieWheels.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static planner.RoutePlanner.simpleCos;
import static planner.RoutePlanner.simpleSin;

public class SimulatorBoard extends JPanel {

    private static final int SCALE = 3;

    private final DuckieEstimations estimations;
    private final DesiredVelocity desiredVelocity;
    private final Queue<DesiredPose> route;
    private final WallGrid realWalls;

    public SimulatorBoard(DuckieEstimations estimations, DesiredVelocity desiredVelocity, Queue<DesiredPose> route, WallGrid realWalls) {
        this.estimations = estimations;
        this.desiredVelocity = desiredVelocity;
        //this.route = new LinkedList<>(route);
        this.route = route;
        this.realWalls = realWalls;
    }

    private final java.util.List<Point2D.Double> visitedPoints = new ArrayList<>();

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        int offsetX = getWidth() / 2;
        int offsetY = getHeight() / 2;

        Color backgroundColor = Color.LIGHT_GRAY;
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, getWidth(), getHeight());

        Color gridColor = Color.GRAY;
        graphics.setColor(gridColor);
        for (int rawX = -20; rawX <= 20; rawX++) {
            int x = (int) Math.round(offsetX + rawX * 100 * GRID_SIZE * SCALE);
            graphics.drawLine(x, 0, x, getHeight());
        }
        for (int rawY = -20; rawY <= 20; rawY++) {
            int y = (int) Math.round(offsetY - rawY * 100 * GRID_SIZE * SCALE);
            graphics.drawLine(0, y, getWidth(), y);
        }

        Color axesColor = Color.BLACK;
        graphics.setColor(axesColor);
        graphics.drawLine(0, offsetY, getWidth(), offsetY);
        graphics.drawLine(offsetX, 0, offsetX, getHeight());

        synchronized (estimations) {
            double x = estimations.x;
            double y = estimations.y;
            double angle = estimations.angle * 2 * Math.PI;

            double cosAngle = cos(angle);
            double sinAngle = sin(angle);

            double xLeftFront = x + 0.07 * cosAngle - DISTANCE_BETWEEN_WHEELS * 0.5 * sinAngle;
            double yLeftFront = y + 0.07 * sinAngle + DISTANCE_BETWEEN_WHEELS * 0.5 * cosAngle;
            double xRightFront = x + 0.07 * cosAngle + DISTANCE_BETWEEN_WHEELS * 0.5 * sinAngle;
            double yRightFront = y + 0.07 * sinAngle - DISTANCE_BETWEEN_WHEELS * 0.5 * cosAngle;
            double xLeftBack = x - 0.12 * cosAngle - DISTANCE_BETWEEN_WHEELS * 0.5 * sinAngle;
            double yLeftBack = y - 0.12 * sinAngle + DISTANCE_BETWEEN_WHEELS * 0.5 * cosAngle;
            double xRightBack = x - 0.12 * cosAngle + DISTANCE_BETWEEN_WHEELS * 0.5 * sinAngle;
            double yRightBack = y - 0.12 * sinAngle - DISTANCE_BETWEEN_WHEELS * 0.5 * cosAngle;
            double[] xCoordinates = { xLeftFront, xRightFront, xRightBack, xLeftBack };
            double[] yCoordinates = { yLeftFront, yRightFront, yRightBack, yLeftBack };
            int[] polygonXCoordinates = new int[xCoordinates.length];
            int[] polygonYCoordinates = new int[yCoordinates.length];
            for (int index = 0; index < xCoordinates.length; index++) {
                polygonXCoordinates[index] = transformRealX(xCoordinates[index]);
                polygonYCoordinates[index] = transformRealY(yCoordinates[index]);
            }

            graphics.setColor(Color.BLUE);
            graphics.drawPolygon(polygonXCoordinates, polygonYCoordinates, 4);

            double xLeftWheel = x - DISTANCE_BETWEEN_WHEELS * 0.5 * sinAngle;
            double yLeftWheel = y + DISTANCE_BETWEEN_WHEELS * 0.5 * cosAngle;
            double xRightWheel = x + DISTANCE_BETWEEN_WHEELS * 0.5 * sinAngle;
            double yRightWheel = y - DISTANCE_BETWEEN_WHEELS * 0.5 * cosAngle;
            graphics.setColor(Color.YELLOW);
            int radius = (transformRealX(xLeftWheel + WHEEL_RADIUS) - transformRealX(xLeftWheel - WHEEL_RADIUS)) / 2;
            graphics.fillOval(transformRealX(xLeftWheel) - radius, transformRealY(yLeftWheel) - radius, 2 * radius, 2 * radius);
            graphics.fillOval(transformRealX(xRightWheel) - radius, transformRealY(yRightWheel) - radius, 2 * radius, 2 * radius);

            graphics.setColor(Color.YELLOW);
            graphics.drawLine(
                    transformRealX(estimations.x),
                    transformRealY(estimations.y),
                    transformRealX(estimations.x + 0.1 * cos(desiredVelocity.angle * 2.0 * Math.PI)),
                    transformRealY(estimations.y + 0.1 * sin(desiredVelocity.angle * 2.0 * Math.PI))
            );
        }

        graphics.setColor(Color.RED);
        int radius = 1;
        for (var point : visitedPoints) {
            graphics.fillOval(transformRealX(point.x) - radius, transformRealY(point.y) - radius, 2 * radius, 2 * radius);
        }
        var newPoint = new Point2D.Double(estimations.x, estimations.y);
        if (!visitedPoints.contains(newPoint)) visitedPoints.add(newPoint);

        radius = 5;
        for (var point : route) {
            int status = point.status.get();
            if (status == STATUS_UNREAD) {
                if (point.backward) graphics.setColor(new Color(0, 0, 100));
                else graphics.setColor(new Color(0, 0, 250));
            }
            if (status == STATUS_READ) {
                if (point.backward) graphics.setColor(new Color(0, 100, 0));
                else graphics.setColor(new Color(0, 250, 0));
            }
            if (status == STATUS_CANCELLED) graphics.setColor(Color.RED);
            graphics.fillOval(transformRealX(point.x) - radius, transformRealY(point.y) - radius, 2 * radius, 2 * radius);
            graphics.setColor(Color.RED);

            graphics.drawLine(
                    transformRealX(point.x), transformRealY(point.y),
                    transformRealX(point.x + 0.03 * simpleCos(point.angle)), transformRealY(point.y + 0.03 * simpleSin(point.angle))
            );
        }

        record WallToDraw(GridWall wall, Color color) {}
        var wallsToDraw = new ArrayList<WallToDraw>();

        if (this.realWalls != null) {
            var realWalls = this.realWalls.copyWalls();
            var estimatedWalls = estimations.walls.copyWalls();
            for (var wall : realWalls) {
                if (estimatedWalls.contains(wall)) wallsToDraw.add(new WallToDraw(wall, Color.GREEN));
                else wallsToDraw.add(new WallToDraw(wall, Color.BLACK));
            }
            for (var wall : estimatedWalls) {
                if (!realWalls.contains(wall)) wallsToDraw.add(new WallToDraw(wall, Color.RED));
            }
        } else {
            for (var wall : estimations.walls.copyWalls()) {
                wallsToDraw.add(new WallToDraw(wall, Color.BLACK));
            }
        }

        for (var wall : wallsToDraw) {
            int x = transformRealX(GRID_SIZE * wall.wall.gridX());
            int y = transformRealY(GRID_SIZE * wall.wall.gridY());
            int length = transformRealX(GRID_SIZE * (wall.wall.gridX() + 1)) - x;
            graphics.setColor(wall.color);
            if (wall.wall.axis() == GridWall.Axis.X) {
                graphics.fillRect(x, y - 5, length, 10);
            } else {
                graphics.fillRect(x - 5, y - length, 10, length);
            }
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private int transformRealX(double realX) {
        return (int) (100 * SCALE * realX + getWidth() / 2);
    }

    private int transformRealY(double realY) {
        return getHeight() / 2 - (int) (100 * SCALE * realY);
    }
}
