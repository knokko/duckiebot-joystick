package state;

import static controller.util.DuckieBot.GRID_SIZE;

public class DuckiePose {

    /**
     * x-coordinate relative to the origin (starting point), in meters
     */
    public volatile double x = 0.5 * GRID_SIZE + 0.03;
    /**
     * y-coordinate relative to the origin (starting point), in meters
     */
    public volatile double y = 0.5 * GRID_SIZE;
    /**
     * velocity in the x-direction, in meters per second
     */
    public volatile double velocityX;
    /**
     * velocity in the y-direction, in meters per second
     */
    public volatile double velocityY;
    /**
     * angle relative to the starting angle, in turns
     */
    public volatile double angle;

    @Override
    public String toString() {
        return String.format("Pose(%.3f, %.3f, angle=%.1f)", x, y, angle);
    }
}
