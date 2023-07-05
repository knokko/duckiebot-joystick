package state;

import camera.CameraWalls;
import camera.RelativeWall;

public class DuckieState {

    public volatile WheelEncoderEntry leftWheelEncoder, rightWheelEncoder;
    public volatile double leftWheelControl, rightWheelControl;
    public volatile double tof;
    public volatile CameraWalls cameraWalls;
    public volatile DuckiePosition duckie;

    public record WheelEncoderEntry(long timestamp, int value) {}
    public record DuckiePosition(long timestamp, RelativeWall position) {}
}
