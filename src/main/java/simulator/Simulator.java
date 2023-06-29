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

import static controller.util.DuckieWheels.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

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

    private double currentTime = 0.0;

    public Simulator(Terrain terrain) {
        this(terrain, 0.0, 0.0, 0.0, 0.0, 100, 0, 0);
    }

    public Simulator(
            Terrain terrain,
            double leftControlLatency, double rightControlLatency,
            double leftTickLatency, double rightTickLatency,
            int cameraInterval, double leftSlipChance, double rightSlipChance
    ) {
        this.terrain = terrain;
        this.realPose = new DuckiePose();
        this.estimations = new DuckieEstimations();
        this.controls = new DuckieControls();
        this.trackedState = new DuckieState();
        this.trackedState.leftWheelEncoder = 0;
        this.trackedState.rightWheelEncoder = 0;

        this.leftControl = new SimulatorLatency<>(leftControlLatency, 0.0);
        this.rightControl = new SimulatorLatency<>(rightControlLatency, 0.0);
        this.leftTicks = new SimulatorLatency<>(leftTickLatency, 0);
        this.rightTicks = new SimulatorLatency<>(rightTickLatency, 0);

        this.cameraInterval = cameraInterval;
        this.leftSlipChance = leftSlipChance;
        this.rightSlipChance = rightSlipChance;
    }

    /**
     * This should be called 1000 times per second
     */
    @Override
    public synchronized void update(double deltaTime) {
        currentTime += deltaTime;

        leftControl.insert(currentTime, controls.velLeft);
        rightControl.insert(currentTime, controls.velRight);

        realPose.x += deltaTime * realPose.velocityX;
        realPose.y += deltaTime * realPose.velocityY;

        double leftVelocity = terrain.leftSpeedFunction.apply(leftControl.get(currentTime));
        double rightVelocity = terrain.rightSpeedFunction.apply(rightControl.get(currentTime));
        double averageVelocity = 0.5 * (leftVelocity + rightVelocity);
        double angleRadians = realPose.angle * 2 * Math.PI; // Convert turns to radians
        realPose.velocityX = averageVelocity * cos(angleRadians);
        realPose.velocityY = averageVelocity * sin(angleRadians);

        realPose.angle += deltaTime * (rightVelocity - leftVelocity) / (2 * Math.PI * DISTANCE_BETWEEN_WHEELS);
        if (realPose.angle >= 1) realPose.angle -= 1;
        if (realPose.angle < 0) realPose.angle += 1;

        if (Math.random() >= leftSlipChance) {
            this.exactLeftWheelTicks += WHEEL_TICKS_PER_TURN * deltaTime * leftVelocity / (2 * Math.PI * WHEEL_RADIUS);
        }
        if (Math.random() >= rightSlipChance) {
            this.exactRightWheelTicks += WHEEL_TICKS_PER_TURN * deltaTime * rightVelocity / (2 * Math.PI * WHEEL_RADIUS);
        }
        leftTicks.insert(currentTime, (int) exactLeftWheelTicks);
        rightTicks.insert(currentTime, (int) exactRightWheelTicks);
        trackedState.leftWheelEncoder = leftTicks.get(currentTime);
        trackedState.rightWheelEncoder = rightTicks.get(currentTime);
        trackedState.leftWheelControl = leftControl.get(currentTime);
        trackedState.rightWheelControl = rightControl.get(currentTime);

        var oldWalls = trackedState.cameraWalls;
        long currentTime = System.currentTimeMillis();
        if (oldWalls == null || (currentTime - oldWalls.timestamp() > cameraInterval)) {
            var cameraPose = new WallSnapper.FixedPose(realPose.x, realPose.y, realPose.angle);
            var visibleWalls = walls.findVisibleWalls(cameraPose);
            var relativeWalls = visibleWalls.stream().map(
                    wall -> RelativeWall.fromGrid(wall, cameraPose)
            ).collect(Collectors.toSet());
            trackedState.cameraWalls = new CameraWalls(currentTime, relativeWalls);
        }
    }
}
