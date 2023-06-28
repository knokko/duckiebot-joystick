package camera;

import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import state.DuckieState;

import static controller.util.DuckieWheels.GRID_SIZE;

public class WallMapper implements ControllerFunction {

    private final DuckieEstimations estimations;
    private final DuckieState trackedState;
    private long lastTimestamp;

    public WallMapper(DuckieEstimations estimations, DuckieState trackedState) {
        this.estimations = estimations;
        this.trackedState = trackedState;
    }

    @Override
    public void update(double deltaTime) {
        var relativeWalls = trackedState.cameraWalls;
        if (relativeWalls != null && lastTimestamp != relativeWalls.timestamp()) {
            lastTimestamp = relativeWalls.timestamp();

            long startTime = System.currentTimeMillis();
            var snapper = new WallSnapper(relativeWalls.walls(), new WallSnapper.FixedPose(estimations.x, estimations.y, estimations.angle));
            var snapResult = snapper.snap(0.1, 33, 0.1 * GRID_SIZE, 33);

            for (var wall : snapResult.walls()) estimations.walls.add(wall);
        }
    }
}
