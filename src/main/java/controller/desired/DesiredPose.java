package controller.desired;

public class DesiredPose {

    public final double x, y;
    public final double angle;

    public DesiredPose(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    @Override
    public String toString() {
        return String.format("DP(%.2f, %.2f)", x, y);
    }
}
