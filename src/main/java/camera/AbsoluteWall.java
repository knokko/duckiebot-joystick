package camera;

import planner.GridWall;

import static controller.util.DuckieWheels.GRID_SIZE;
import static java.lang.Math.floor;
import static java.lang.Math.min;

public record AbsoluteWall(double x, double y) {

    public SnappedError snap() {
        double gridX = x / GRID_SIZE;
        double gridY = y / GRID_SIZE;

        double rx = gridX - floor(gridX);
        double ry = gridY - floor(gridY);

        int ix = (int) floor(gridX);
        int iy = (int) floor(gridY);

        double errorRight = (1 - rx) * (1 - rx) + (0.5 - ry) * (0.5 - ry);
        double errorLeft = rx * rx + (0.5 - ry) * (0.5 - ry);
        double errorUp = (0.5 - rx) * (0.5 - rx) + (1 - ry) * (1 - ry);
        double errorDown = (0.5 - rx) * (0.5 - rx) + ry * ry;

        double minError = min(min(errorLeft, errorRight), min(errorDown, errorUp));

        var snapped = new GridWall(ix + 1, iy, GridWall.Axis.Y);
        if (errorLeft == minError) snapped = new GridWall(ix, iy, GridWall.Axis.Y);
        if (errorUp == minError) snapped = new GridWall(ix, iy + 1, GridWall.Axis.X);
        if (errorDown == minError) snapped = new GridWall(ix, iy, GridWall.Axis.X);

        return new SnappedError(snapped, minError);
    }

    public record SnappedError(GridWall wall, double error) {}
}
