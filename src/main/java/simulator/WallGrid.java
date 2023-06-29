package simulator;

import camera.WallSnapper;
import planner.GridWall;

import java.util.*;

import static controller.util.DuckieBot.GRID_SIZE;
import static java.lang.Math.*;

public class WallGrid {

    static WallView getWallView(GridWall wall, WallSnapper.FixedPose camera) {
        double startX = wall.gridX() * GRID_SIZE;
        double startY = wall.gridY() * GRID_SIZE;
        double endX = startX;
        double endY = startY;
        if (wall.axis() == GridWall.Axis.X) endX += GRID_SIZE;
        if (wall.axis() == GridWall.Axis.Y) endY += GRID_SIZE;

        double midX = (startX + endX) * 0.5;
        double midY = (startY + endY) * 0.5;

        double distance = sqrt((camera.x() - midX) * (camera.x() - midX) + (camera.y() - midY) * (camera.y() - midY));
        double absoluteAngle1 = atan2(startY - camera.y(), startX - camera.x()) / (2 * PI);
        double absoluteAngle2 = atan2(endY - camera.y(), endX - camera.x()) / (2 * PI);
        double angleDifference = absoluteAngle2 - absoluteAngle1;
        if (angleDifference > 0.5) angleDifference -= 1;
        if (angleDifference < -0.5) angleDifference += 1;

        double relativeAngle1 = absoluteAngle1 - camera.angle();
        if (relativeAngle1 < -0.5) relativeAngle1 += 1;
        double relativeAngle2 = relativeAngle1 + angleDifference;

        double minAngle = min(relativeAngle1, relativeAngle2);
        double maxAngle = max(relativeAngle1, relativeAngle2);
        return new WallView(distance, minAngle, maxAngle);
    }

    private final Set<GridWall> allWalls = new HashSet<>();

    public synchronized void add(GridWall wall) {
        allWalls.add(wall);
    }

    public synchronized Set<GridWall> findVisibleWalls(WallSnapper.FixedPose camera) {
        var visibleWalls = new HashSet<GridWall>();

        double fov = 0.125; // Assume camera field of view to be 45 degrees
        for (var candidateWall : allWalls) {
            var candidateView = getWallView(candidateWall, camera);
            Collection<Double> candidateAngles = new LinkedList<>();
            for (double candidateAngle = candidateView.minAngle; candidateAngle <= candidateView.maxAngle; candidateAngle += 0.001) {
                candidateAngles.add(candidateAngle);
            }
            int originalSize = candidateAngles.size();

            if (candidateView.maxAngle > -fov && candidateView.minAngle < fov) {

                for (var otherWall : allWalls) {
                    var otherView = getWallView(otherWall, camera);
                    if (otherView.distance < candidateView.distance) {
                        candidateAngles.removeIf(
                                candidateAngle -> candidateAngle > otherView.minAngle && candidateAngle < otherView.maxAngle
                        );
                    }
                }

                double coveredAngle = (candidateView.maxAngle - candidateView.minAngle) * candidateAngles.size() / originalSize;
                if (coveredAngle > 0.02) {
                    visibleWalls.add(candidateWall);
                }
            }
        }

        return visibleWalls;
    }

    public synchronized Set<GridWall> copyWalls() {
        return new HashSet<>(allWalls);
    }

    record WallView(double distance, double minAngle, double maxAngle) {}
}
