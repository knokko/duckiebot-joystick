package controller.desired;

public class DesiredVelocity {

    /**
     * The desired speed, in meters per second
     */
    public volatile double speed;

    /**
     * The desired angle, in turns
     */
    public volatile double angle;

    public volatile double turnTime = 1.0;

    public boolean needReposition = false;
}
