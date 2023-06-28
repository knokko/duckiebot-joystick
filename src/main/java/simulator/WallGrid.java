package simulator;

import camera.WallSnapper;
import planner.GridWall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static controller.util.DuckieWheels.GRID_SIZE;
import static java.lang.Math.*;

public class WallGrid {

    // TODO Test this
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
        double relativeAngle1 = absoluteAngle1 - camera.angle();
        double relativeAngle2 = absoluteAngle2 - camera.angle();

        if (relativeAngle1 < -0.5 && relativeAngle2 < -0.5) {
            relativeAngle1 += 1;
            relativeAngle2 += 1;
        }

        return new WallView(distance, min(relativeAngle1, relativeAngle2), max(relativeAngle1, relativeAngle2));
    }

    private final Set<GridWall> allWalls = new HashSet<>();

    // TODO Test this
    public Collection<GridWall> findVisibleWalls(WallSnapper.FixedPose camera) {
        var visibleWalls = new ArrayList<GridWall>();

        double fov = 0.125; // Assume camera field of view to be 45 degrees
        for (var candidateWall : allWalls) {
            var candidateView = getWallView(candidateWall, camera);
            if (candidateView.maxAngle > -fov && candidateView.minAngle < fov) {

                boolean isHidden = false;
                for (var otherWall : allWalls) {
                    var otherView = getWallView(otherWall, camera);
                    if (otherView.distance < candidateView.distance
                            && otherView.maxAngle > candidateView.maxAngle && otherView.minAngle < candidateView.minAngle
                    ) {
                        isHidden = true;
                        break;
                    }
                }

                if (!isHidden) {
                    visibleWalls.add(candidateWall);
                }
            }
        }

        return visibleWalls;
    }

    record WallView(double distance, double minAngle, double maxAngle) {}
}
