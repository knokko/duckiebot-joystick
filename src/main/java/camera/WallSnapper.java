package camera;

import planner.GridWall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static controller.util.DuckieBot.GRID_SIZE;
import static java.lang.Math.*;

public class WallSnapper {

    static AbsoluteWall transformWall(RelativeWall wall, double x, double y, double angle) {
        double transformingAngle = (angle + wall.angle()) * 2.0 * PI;
        double absoluteX = x + wall.distance() * cos(transformingAngle);
        double absoluteY = y + wall.distance() * sin(transformingAngle);
        return new AbsoluteWall(absoluteX, absoluteY);
    }

    private final List<RelativeWall> originalWalls;
    private final FixedPose estimatedPose;

    public WallSnapper(Collection<RelativeWall> originalWalls, FixedPose estimatedPose) {
        this.originalWalls = new ArrayList<>(originalWalls);
        this.originalWalls.sort((a, b) -> {
            if (a.distance() > b.distance()) return 1;
            if (a.distance() < b.distance()) return -1;
            return Double.compare(a.angle(), b.angle());
        });
        this.estimatedPose = estimatedPose;
    }

    double computeError(double deltaAngle, double deltaX, double deltaY) {
        double testAngle = estimatedPose.angle + deltaAngle;
        double testX = estimatedPose.x + deltaX;
        double testY = estimatedPose.y + deltaY;

        double error = 0.0;

        for (var wall : originalWalls) {
            var transformedWall = transformWall(wall, testX, testY, testAngle);
            error += transformedWall.snap().error();
        }

        return error;
    }

    public SnapResult snap(
            double maxAngleCorrection, int numAnglesToTry,
            double maxOffsetCorrection, int numOffsetsToTry,
            RelativeWall duckie
    ) {
        double minError = Double.MAX_VALUE;
        double bestAngleCorrection = 0;
        double bestCorrectionX = 0;
        double bestCorrectionY = 0;

        for (int angleCounter = 0; angleCounter < numAnglesToTry; angleCounter++) {
            double angleToTry = 2 * maxAngleCorrection * angleCounter / (numAnglesToTry - 1.0) - maxAngleCorrection;

            for (int xCounter = 0; xCounter < numOffsetsToTry; xCounter++) {
                double xToTry = 2 * maxOffsetCorrection * xCounter / (numOffsetsToTry - 1.0) - maxOffsetCorrection;

                for (int yCounter = 0; yCounter < numOffsetsToTry; yCounter++) {
                    double yToTry = 2 * maxOffsetCorrection * yCounter / (numOffsetsToTry - 1.0) - maxOffsetCorrection;

                    double newError = computeError(angleToTry, xToTry, yToTry);
                    if (newError < minError) {
                        minError = newError;
                        bestAngleCorrection = angleToTry;
                        bestCorrectionX = xToTry;
                        bestCorrectionY = yToTry;
                    }
                }
            }
        }

        double finalCorrectionX = bestCorrectionX;
        double finalCorrectionY = bestCorrectionY;
        double finalAngleCorrection = bestAngleCorrection;

        var walls = originalWalls.stream().map(wall -> {
            var transformedWall = transformWall(
                    wall, estimatedPose.x + finalCorrectionX,
                    estimatedPose.y + finalCorrectionY,
                    estimatedPose.angle + finalAngleCorrection
            );
            return transformedWall.snap().wall();
        }).collect(Collectors.toList());

        GridWall snappedDuckie = null;
        if (duckie != null) {
            var transformedDuckie = transformWall(
                    duckie, estimatedPose.x + finalCorrectionX,
                    estimatedPose.y + finalCorrectionY,
                    estimatedPose.angle + finalAngleCorrection
            );
            snappedDuckie = new GridWall(
                    (int) Math.floor(transformedDuckie.x() / GRID_SIZE),
                    (int) Math.floor(transformedDuckie.y() / GRID_SIZE),
                    GridWall.Axis.DUCKIE
            );
        }

        return new SnapResult(walls, snappedDuckie, new FixedPose(
                estimatedPose.x + bestCorrectionX,
                estimatedPose.y + bestCorrectionY,
                estimatedPose.angle + bestAngleCorrection
        ), minError);
    }

    public record FixedPose(double x, double y, double angle) {}

    public record SnapResult(Collection<GridWall> walls, GridWall duckie, FixedPose correctedPose, double error) {}
}
