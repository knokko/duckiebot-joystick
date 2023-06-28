package controller.estimation;

import controller.util.Polynomial;
import simulator.WallGrid;

import static controller.util.DuckieWheels.GRID_SIZE;

public class DuckieEstimations {
    /**
     * The (x, y) coordinates relative to the origin (starting point), in meters
     */
    public volatile double x = 0.5 * GRID_SIZE + 0.03, y = 0.5 * GRID_SIZE;
    /**
     * The estimated speed of the left and right wheel, in meters per second
     */
    public volatile double leftSpeed, rightSpeed;
    /**
     * The estimated angle of the duckiebot, in turns
     */
    public volatile double angle;
    /**
     * The estimated time between giving motor commands and changing the speed, in seconds
     */
    public volatile double leftControlLatency = 0.03, rightControlLatency = 0.03; // TODO Don't hardcode this

    public volatile Polynomial distancePolynomial;

    public final WallGrid walls = new WallGrid();
}
