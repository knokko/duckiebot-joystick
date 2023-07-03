package camera;

import planner.GridWall;

import static controller.util.DuckieBot.GRID_SIZE;
import static java.lang.Math.*;

public record RelativeWall(double distance, double angle) {

    public static RelativeWall cartesian(double x, double y) {
        return new RelativeWall(sqrt(x * x + y * y), atan2(y, x) / (2 * PI));
    }

    public static RelativeWall fromGrid(GridWall wall, WallSnapper.FixedPose camera) {
        double absoluteX = wall.gridX() * GRID_SIZE;
        double absoluteY = wall.gridY() * GRID_SIZE;
        if (wall.axis() == GridWall.Axis.X) absoluteX += 0.5 * GRID_SIZE;
        else absoluteY += 0.5 * GRID_SIZE;

        double dx = absoluteX - camera.x();
        double dy = absoluteY - camera.y();

        double distance = sqrt(dx * dx + dy * dy);
        double angle = atan2(dy, dx) / (2 * PI) - camera.angle();
        return new RelativeWall(distance, angle);
    }

    public static RelativeWall noisyFromGrid(GridWall wall, WallSnapper.FixedPose camera, double maxNoise) {
        RelativeWall exact = fromGrid(wall, camera);
        double noisyDistance = exact.distance() * (1 - maxNoise + 2 * maxNoise * Math.random());
        double maxAngleNoise = noisyDistance * 0.28 * maxNoise;
        double noisyAngle = exact.angle * (1 - maxAngleNoise + 2 * maxAngleNoise * Math.random());
        if (noisyAngle < -0.5) noisyAngle += 1;
        if (noisyAngle > 0.5) noisyAngle -= 1;
        return new RelativeWall(noisyDistance, noisyAngle);
    }
}
