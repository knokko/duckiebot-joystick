package joystick.server;

import java.io.IOException;

public class JoystickWheelController {

    private final boolean dummy;

    private int sequenceNumber;

    public JoystickWheelController(boolean dummy) {
        this.dummy = dummy;
    }

    public void controlWheels(float left, float right) {
        long time = System.currentTimeMillis();
        long timeSeconds = time / 1000;
        long timeNanoSeconds = 1_000_000L * (time - timeSeconds * 1000);
        String command = "rostopic pub /db4/wheels_driver_node/wheels_cmd duckietown_msgs/WheelsCmdStamped '{" +
                "seq: " + sequenceNumber + ", stamp: {sec: " + timeSeconds + ", nsec: " + timeNanoSeconds + "}, " +
                "frame_id: joystick" + sequenceNumber + ", vel_left: " + left + ", vel_right: " + right + "}'";
        sequenceNumber += 1;
        try {
            if (dummy) System.out.println(command);
            else Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            throw new RuntimeException("Failed to control wheels", e);
        }
    }
}
