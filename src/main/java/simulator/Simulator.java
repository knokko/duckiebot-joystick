package simulator;

import camera.CameraWalls;
import camera.RelativeWall;
import camera.WallSnapper;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import state.DuckieControls;
import state.DuckiePose;
import state.DuckieState;

import java.util.stream.Collectors;

import static controller.util.DuckieBot.*;
import static java.lang.Math.*;

public class Simulator implements ControllerFunction {

    private final Terrain terrain;
    public final DuckiePose realPose;
    public final DuckieEstimations estimations;
    private double exactLeftWheelTicks, exactRightWheelTicks;
    public final DuckieControls controls;
    public final DuckieState trackedState;
    public final WallGrid walls = SimulatorMaze.createTestingWallGrid();

    private final SimulatorLatency<Double> leftControl, rightControl;
    private final SimulatorLatency<Integer> leftTicks, rightTicks;
    private final int cameraInterval;
    private final double leftSlipChance, rightSlipChance;
    private final double maxCameraNoise;

    private double currentTime = 0.0;

    public Simulator(Terrain terrain) {
        this(terrain, 0.0, 0.0, 0.0, 0.0,
                100, 0, 0, 0
        );
    }

    public Simulator(
            Terrain terrain,
            double leftControlLatency, double rightControlLatency,
            double leftTickLatency, double rightTickLatency,
            int cameraInterval, double leftSlipChance, double rightSlipChance, double maxCameraNoise
    ) {
        this.terrain = terrain;
        this.realPose = new DuckiePose();
        this.estimations = new DuckieEstimations();
        this.controls = new DuckieControls();
        this.trackedState = new DuckieState();
        this.trackedState.leftWheelEncoder = new DuckieState.WheelEncoderEntry(System.nanoTime(), 0);
        this.trackedState.rightWheelEncoder = new DuckieState.WheelEncoderEntry(System.nanoTime(), 0);

        this.leftControl = new SimulatorLatency<>(leftControlLatency, 0.0);
        this.rightControl = new SimulatorLatency<>(rightControlLatency, 0.0);
        this.leftTicks = new SimulatorLatency<>(leftTickLatency, 0);
        this.rightTicks = new SimulatorLatency<>(rightTickLatency, 0);

        this.cameraInterval = cameraInterval;
        this.leftSlipChance = leftSlipChance;
        this.rightSlipChance = rightSlipChance;
        this.maxCameraNoise = maxCameraNoise;
    }

    private double clampThrottle(double input) {
        if (input > 1) return 1;
        if (input < -1) return -1;
        return input;
    }

    /**
     * This should be called 1000 times per second
     */
    @Override
    public synchronized void update(double deltaTime) {
        currentTime += deltaTime;

        leftControl.insert(currentTime, clampThrottle(controls.velLeft));
        rightControl.insert(currentTime, clampThrottle(controls.velRight));

        realPose.x += deltaTime * realPose.velocityX;
        realPose.y += deltaTime * realPose.velocityY;

        double leftVelocity = terrain.leftSpeedFunction.apply(leftControl.get(currentTime));
        double rightVelocity = terrain.rightSpeedFunction.apply(rightControl.get(currentTime));
        double averageVelocity = 0.5 * (leftVelocity + rightVelocity);
        double angleRadians = realPose.angle * 2 * PI; // Convert turns to radians
        realPose.velocityX = averageVelocity * cos(angleRadians);
        realPose.velocityY = averageVelocity * sin(angleRadians);

        realPose.angle += deltaTime * (rightVelocity - leftVelocity) / (2 * PI * DISTANCE_BETWEEN_WHEELS);
        if (realPose.angle >= 1) realPose.angle -= 1;
        if (realPose.angle < 0) realPose.angle += 1;

        if (Math.random() >= leftSlipChance) {
            this.exactLeftWheelTicks += WHEEL_TICKS_PER_TURN * deltaTime * leftVelocity / (2 * PI * WHEEL_RADIUS);
        }
        if (Math.random() >= rightSlipChance) {
            this.exactRightWheelTicks += WHEEL_TICKS_PER_TURN * deltaTime * rightVelocity / (2 * PI * WHEEL_RADIUS);
        }
        leftTicks.insert(currentTime, (int) exactLeftWheelTicks);
        rightTicks.insert(currentTime, (int) exactRightWheelTicks);
        trackedState.leftWheelEncoder = new DuckieState.WheelEncoderEntry(System.nanoTime(), leftTicks.get(currentTime));
        trackedState.rightWheelEncoder = new DuckieState.WheelEncoderEntry(System.nanoTime(), rightTicks.get(currentTime));
        trackedState.leftWheelControl = leftControl.get(currentTime);
        trackedState.rightWheelControl = rightControl.get(currentTime);

        var oldWalls = trackedState.cameraWalls;
        long currentTime = System.currentTimeMillis();
        if (oldWalls == null || (currentTime - oldWalls.timestamp() > cameraInterval)) {
            double realAngleRad = realPose.angle * 2 * PI;
            var cameraPose = new WallSnapper.FixedPose(
                    realPose.x + CAMERA_OFFSET * cos(realAngleRad),
                    realPose.y + CAMERA_OFFSET * sin(realAngleRad),
                    realPose.angle
            );
            var visibleWalls = walls.findVisibleWalls(cameraPose);
            var relativeWalls = visibleWalls.stream().map(
                    wall -> RelativeWall.noisyFromGrid(wall, cameraPose, maxCameraNoise)
            ).collect(Collectors.toSet());
            trackedState.cameraWalls = new CameraWalls(currentTime, relativeWalls);
        }
    }
}
