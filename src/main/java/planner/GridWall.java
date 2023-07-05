package planner;

public record GridWall(int gridX, int gridY, Axis axis) {

    public enum Axis {
        X, Y, DUCKIE
    }
}
