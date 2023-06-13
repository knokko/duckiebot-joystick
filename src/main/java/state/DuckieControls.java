package state;

public class DuckieControls {

    /**
     * The velocity commands that should be sent to the motor drivers
     */
    public volatile double velLeft, velRight;

    @Override
    public String toString() {
        return String.format("DuckieControls(left=%.2f, right=%.2f)", velLeft, velRight);
    }
}
