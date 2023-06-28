package simulator;

import camera.WallSnapper;
import org.junit.Test;
import planner.GridWall;

import static controller.util.DuckieWheels.GRID_SIZE;
import static junit.framework.TestCase.assertEquals;
import static simulator.WallGrid.getWallView;

public class TestWallGrid {

    private void testGetWallView(GridWall wall, WallSnapper.FixedPose camera, WallGrid.WallView expected) {
        var actual = getWallView(wall, camera);
        assertEquals(expected.distance(), actual.distance(), 0.01);
        assertEquals(expected.minAngle(), actual.minAngle(), 0.01);
        assertEquals(expected.maxAngle(), actual.maxAngle(), 0.01);
    }

    @Test
    public void testGetWallView() {
        // Simple origin tests
        var origin = new WallSnapper.FixedPose(0, 0, 0);
        testGetWallView(
                new GridWall(2, 0, GridWall.Axis.Y), origin,
                new WallGrid.WallView(2.06 * GRID_SIZE, 0, 0.083)
        );
        testGetWallView(
                new GridWall(0, 2, GridWall.Axis.X), origin,
                new WallGrid.WallView(2.06 * GRID_SIZE, 0.25 - 0.083, 0.25)
        );
        testGetWallView(
                new GridWall(0, -2, GridWall.Axis.X), origin,
                new WallGrid.WallView(2.06 * GRID_SIZE, -0.25, 0.083 - 0.25)
        );

        // A more complicated pose
        var pose = new WallSnapper.FixedPose(-0.5 * GRID_SIZE, -2.5 * GRID_SIZE, 0.375);
        testGetWallView(
                new GridWall(0, 0, GridWall.Axis.X), pose,
                new WallGrid.WallView(2.69 * GRID_SIZE, 0.164 - 0.375, 0.22 - 0.375)
        );

        // Edge cases
        pose = new WallSnapper.FixedPose(0, 0, 0.9);
        testGetWallView(
                new GridWall(-1, 0, GridWall.Axis.Y), pose,
                new WallGrid.WallView(1.12 * GRID_SIZE, -0.525, -0.4)
        );
        testGetWallView(
                new GridWall(-1, 1, GridWall.Axis.Y), pose,
                new WallGrid.WallView(1.8 * GRID_SIZE, 0.43, 0.475)
        );
        testGetWallView(
                new GridWall(10, -1, GridWall.Axis.X), origin,
                new WallGrid.WallView(10.55 * GRID_SIZE, -0.016, -0.014)
        );
    }
}
