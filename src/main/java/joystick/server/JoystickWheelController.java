package joystick.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Arrays;

public class JoystickWheelController implements WebSocket.Listener {

    private final boolean dummy;
    private WebSocket webSocket;

    private int sequenceNumber;


    public JoystickWheelController(boolean dummy) {
        this.dummy = dummy;
    }

    public void controlWheels(float left, float right) {
        long time = System.currentTimeMillis();
        long timeSeconds = time / 1000;
        String[] commandArray = {
                "rostopic", "pub", "--once", "/db4/wheels_driver_node/wheels_cmd", "duckietown_msgs/WheelsCmdStamped",
                "{header: {seq: " + sequenceNumber + ", stamp: " + timeSeconds + ", frame_id: joystick" + sequenceNumber +
                        "}, vel_left: " + left + ", vel_right: " + right + "}"
        };
        sequenceNumber += 1;
        try {
            if (dummy) System.out.println(Arrays.toString(commandArray));
            else {
                if (webSocket == null) {
                    webSocket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
                            new URI("ws://localhost:9001"), this
                    ).join();
                }
                String jsonCommand = "{\"op\": \"publish\", \"topic\": \"/db4/wheels_driver_node/wheels_cmd\", \"msg\": {" +
                        "\"header\":{\"seq\": " + sequenceNumber + ", \"stamp\": 1685969712, \"frame_id\": \"joystick1\"}," +
                        "\"vel_left\": " + left + ", \"vel_right\": " + right + "}}";
                webSocket.sendText(jsonCommand, true);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
