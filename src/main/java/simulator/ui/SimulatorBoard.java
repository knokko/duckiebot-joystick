package simulator.ui;

import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.estimation.DuckieEstimations;
import planner.GridWall;
import simulator.WallGrid;
import state.DuckiePose;
import state.DuckieState;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Queue;

import static controller.desired.DesiredPose.*;
import static controller.util.DuckieBot.*;
import static java.lang.Math.*;
import static planner.RoutePlanner.simpleCos;
import static planner.RoutePlanner.simpleSin;

public class SimulatorBoard extends JPanel {

    private static final int SCALE = 3;

    private final DuckieEstimations estimations;
    private final DesiredVelocity desiredVelocity;
    private final Queue<DesiredPose> route;
    private final WallGrid realWalls;
    private final DuckiePose realPose;
    private final DuckieState trackedState;

    public SimulatorBoard(
            DuckieEstimations estimations, DesiredVelocity desiredVelocity, Queue<DesiredPose> route,
            WallGrid realWalls, DuckiePose realPose, DuckieState trackedState
    ) {
        this.estimations = estimations;
        this.desiredVelocity = desiredVelocity;
        //this.route = new LinkedList<>(route);
        this.route = route;
        this.realWalls = realWalls;
        this.realPose = realPose;
        this.trackedState = trackedState;
    }

    private final java.util.List<Point2D.Double> estimatedVisitedPoints = new ArrayList<>();
    private final java.util.List<Point2D.Double> realVisitedPoints = new ArrayList<>();

    private int offsetX() {
        return getWidth() / 2;
    }

    private int offsetY() {
        return getHeight() / 2;
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);

        Color backgroundColor = Color.LIGHT_GRAY;
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, getWidth(), getHeight());

        Color gridColor = Color.GRAY;
        graphics.setColor(gridColor);
        for (int rawX = -20; rawX <= 20; rawX++) {
            int x = (int) Math.round(offsetX() + rawX * 100 * GRID_SIZE * SCALE);
            graphics.drawLine(x, 0, x, getHeight());
        }
        for (int rawY = -20; rawY <= 20; rawY++) {
            int y = (int) Math.round(offsetY() - rawY * 100 * GRID_SIZE * SCALE);
            graphics.drawLine(0, y, getWidth(), y);
        }

        Color axesColor = Color.BLACK;
        graphics.setColor(axesColor);
        graphics.drawLine(0, offsetY(), getWidth(), offsetY());
        graphics.drawLine(offsetX(), 0, offsetX(), getHeight());

        synchronized (estimations) {
            double x = estimations.x;
            double y = estimations.y;
            double angle = estimations.angle * 2 * PI;

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
                    transformRealX(estimations.x + 0.1 * cos(desiredVelocity.angle * 2.0 * PI)),
                    transformRealY(estimations.y + 0.1 * sin(desiredVelocity.angle * 2.0 * PI))
            );

            var duckie = estimations.duckie;
            if (duckie != null) {
                int duckieRadius = 10;
                double duckieX = (duckie.gridX() + 0.5) * GRID_SIZE;
                double duckieY = (duckie.gridY() + 0.5) * GRID_SIZE;
                graphics.fillOval(
                        transformRealX(duckieX) - duckieRadius,
                        transformRealY(duckieY) - duckieRadius,
                        2 * duckieRadius,
                        2 * duckieRadius
                );
            }

            graphics.setColor(Color.MAGENTA);
            var cameraWalls = trackedState.cameraWalls;
            if (cameraWalls != null) {
                for (var wall : cameraWalls.walls()) {
                    double cameraAngleRad = estimations.angle * 2 * PI;
                    double wallAngleRad = (estimations.angle + wall.angle()) * 2 * PI;
                    double wallX = estimations.x + CAMERA_OFFSET * cos(cameraAngleRad) + wall.distance() * cos(wallAngleRad);
                    double wallY = estimations.y + CAMERA_OFFSET * sin(cameraAngleRad) + wall.distance() * sin(wallAngleRad);
                    int wallRadius = 7;
                    graphics.fillOval(
                            transformRealX(wallX) - wallRadius,
                            transformRealY(wallY) - wallRadius,
                            2 * wallRadius, 2 * wallRadius
                    );
                }
            }
        }

        int radius = 1;
        if (realPose != null) {
            graphics.setColor(Color.MAGENTA);
            for (var point : realVisitedPoints) {
                graphics.fillOval(transformRealX(point.x) - radius, transformRealY(point.y) - radius, 2 * radius, 2 * radius);
            }
            var newPoint = new Point2D.Double(realPose.x, realPose.y);
            if (!realVisitedPoints.contains(newPoint)) realVisitedPoints.add(newPoint);
        }

        graphics.setColor(Color.RED);
        for (var point : estimatedVisitedPoints) {
            graphics.fillOval(transformRealX(point.x) - radius, transformRealY(point.y) - radius, 2 * radius, 2 * radius);
        }
        var newPoint = new Point2D.Double(estimations.x, estimations.y);
        if (!estimatedVisitedPoints.contains(newPoint)) estimatedVisitedPoints.add(newPoint);

        radius = 5;
        for (var point : new ArrayList<>(route)) {
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
        
        // Draw mapper shizzle
        for(var cellRow : estimations.cells){
            for(var cell : cellRow){
                int x = transformRealX(GRID_SIZE * cell.gridX() + GRID_SIZE * 0.5);
                int y = transformRealY(GRID_SIZE * cell.gridY() + GRID_SIZE * 0.5);

                // Draw a dot for if the visit count is 1
                if(cell.visitCount == 1){
                    graphics.setColor(Color.pink);
                    graphics.fillOval(x-5, y-5, 10, 10);
                }
                
                // Draw a cross for if the visit count is 2
                if(cell.visitCount > 1){
                    graphics.setColor(Color.red);
                    var blockSize = 35;
                    graphics.drawLine(x - blockSize, y - blockSize, x + blockSize, y + blockSize);
                    graphics.drawLine(x - blockSize, y + blockSize, x + blockSize, y - blockSize);
                }
            }
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private int transformRealX(double realX) {
        return (int) (100 * SCALE * realX + offsetX());
    }

    private int transformRealY(double realY) {
        return offsetY() - (int) (100 * SCALE * realY);
    }
}
