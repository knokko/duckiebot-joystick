package controller.desired;

import java.util.concurrent.atomic.AtomicInteger;

public class DesiredPose {

    public static final int STATUS_UNREAD = 0;
    public static final int STATUS_READ = 1;
    public static final int STATUS_CANCELLED = 2;

    public final double x, y;
    public final double angle;
    public final boolean backward;
    public final AtomicInteger status;

    public DesiredPose(double x, double y, double angle, boolean backward) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.backward = backward;
        this.status = new AtomicInteger(STATUS_UNREAD);
    }

    @Override
    public String toString() {
        return String.format("DP(%.2f, %.2f, backward=%b)", x, y, backward);
    }
}
