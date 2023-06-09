package state;

public class DuckiePose {

    /**
     * x-coordinate relative to the origin (starting point), in meters
     */
    public volatile double x = 0.16;
    /**
     * y-coordinate relative to the origin (starting point), in meters
     */
    public volatile double y = 0.1;
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
