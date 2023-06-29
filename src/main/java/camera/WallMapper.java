package camera;

import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import state.DuckieState;

import static controller.util.DuckieWheels.GRID_SIZE;

public class WallMapper implements ControllerFunction {

    private final DuckieEstimations estimations;
    private final DuckieState trackedState;
    private final double maxMapError;
    private final double maxCorrectionError;
    private long lastTimestamp;

    public WallMapper(
            DuckieEstimations estimations, DuckieState trackedState,
            double maxMapError, double maxCorrectionError
    ) {
        this.estimations = estimations;
        this.trackedState = trackedState;
        this.maxMapError = maxMapError;
        this.maxCorrectionError = maxCorrectionError;
    }

    @Override
    public void update(double deltaTime) {
        var relativeWalls = trackedState.cameraWalls;
        if (relativeWalls != null && lastTimestamp != relativeWalls.timestamp()) {
            lastTimestamp = relativeWalls.timestamp();

            var snapper = new WallSnapper(relativeWalls.walls(), new WallSnapper.FixedPose(estimations.x, estimations.y, estimations.angle));
            var snapResult = snapper.snap(0.1, 33, 0.01 * GRID_SIZE, 33);

            if (snapResult.error() <= maxMapError && snapResult.walls().size() > 2) {
                for (var wall : snapResult.walls()) estimations.walls.add(wall);
            }

            if (snapResult.error() <= maxCorrectionError) {
                estimations.x = snapResult.correctedPose().x();
                estimations.y = snapResult.correctedPose().y();
                estimations.angle = snapResult.correctedPose().angle();
            }
        }
    }
}
