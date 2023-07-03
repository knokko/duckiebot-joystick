package state;

import camera.CameraWalls;

public class DuckieState {

    public volatile WheelEncoderEntry leftWheelEncoder, rightWheelEncoder;
    public volatile double leftWheelControl, rightWheelControl;
    public volatile double tof;
    public volatile CameraWalls cameraWalls;

    public record WheelEncoderEntry(long timestamp, int value) {}
}
