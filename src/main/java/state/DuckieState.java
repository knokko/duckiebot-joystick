package state;

import camera.CameraWalls;

public class DuckieState {

    public volatile Integer leftWheelEncoder, rightWheelEncoder;
    public volatile double leftWheelControl, rightWheelControl;
    public volatile double tof;
    public volatile CameraWalls cameraWalls;
}
