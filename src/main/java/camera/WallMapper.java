package camera;

import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import state.DuckieState;

import static controller.util.DuckieBot.CAMERA_OFFSET;
import static controller.util.DuckieBot.GRID_SIZE;
import static java.lang.Math.*;

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

            double angleRad = estimations.angle * 2 * PI;
            var snapper = new WallSnapper(relativeWalls.walls(), new WallSnapper.FixedPose(
                    estimations.x + CAMERA_OFFSET * cos(angleRad),
                    estimations.y + CAMERA_OFFSET * sin(angleRad),
                    estimations.angle
            ));
            var snapResult = snapper.snap(0.01, 33, 0.01 * GRID_SIZE, 33);

            if (snapResult.error() <= maxMapError && snapResult.walls().size() > 2) {
                for (var wall : snapResult.walls()) estimations.walls.add(wall);
            }

            if (snapResult.error() <= maxCorrectionError) {
                estimations.x = snapResult.correctedPose().x() - CAMERA_OFFSET * cos(angleRad);
                estimations.y = snapResult.correctedPose().y() - CAMERA_OFFSET * sin(angleRad);
                estimations.angle = snapResult.correctedPose().angle();
            }
        }
    }
}
