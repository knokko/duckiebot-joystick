package simulator;

import camera.WallSnapper;
import org.junit.Test;
import planner.GridWall;

import java.util.HashSet;

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

        testGetWallView(
                new GridWall(-10, 0, GridWall.Axis.Y), new WallSnapper.FixedPose(0, 0.5 * GRID_SIZE, 0),
                new WallGrid.WallView(10 * GRID_SIZE, -0.5 - 0.008, -0.5 + 0.008)
        );
    }

    @Test
    public void testGetVisibleWallsVerySimple() {
        var wall1 = new GridWall(1, 0, GridWall.Axis.Y);
        var wall21 = new GridWall(2, -1, GridWall.Axis.Y);
        var wall22 = new GridWall(2, 0, GridWall.Axis.Y);
        var wall23 = new GridWall(2, 1, GridWall.Axis.Y);
        var wall31 = new GridWall(3, -1, GridWall.Axis.Y);
        var wall32 = new GridWall(3, 0, GridWall.Axis.Y);
        var wall33 = new GridWall(3, 1, GridWall.Axis.Y);

        var wallBehind = new GridWall(-5, 0, GridWall.Axis.Y);

        var grid = new WallGrid();
        grid.add(wall1);
        grid.add(wall21);
        grid.add(wall22);
        grid.add(wall23);
        grid.add(wall31);
        grid.add(wall32);
        grid.add(wall33);
        grid.add(wallBehind);


        var actualWalls = grid.findVisibleWalls(new WallSnapper.FixedPose(-GRID_SIZE, 0.5 * GRID_SIZE, 0));

        var expectedWalls = new HashSet<GridWall>();
        expectedWalls.add(wall1);
        expectedWalls.add(wall21);
        expectedWalls.add(wall23);

        assertEquals(expectedWalls, actualWalls);
    }

    @Test
    public void testGetVisibleWallsComplex() {
        var obstacleFront = new GridWall(1, -2, GridWall.Axis.X);
        var obstacleLeft = new GridWall(1, -3, GridWall.Axis.Y);
        var obstacleRight = new GridWall(2, -3, GridWall.Axis.Y);
        var behind = new GridWall(1, -5, GridWall.Axis.X);

        var hiddenFront1 = new GridWall(1, -1, GridWall.Axis.X);
        var hiddenFront2 = new GridWall(1, 3, GridWall.Axis.X);
        var hiddenLeft1 = new GridWall(0, -3, GridWall.Axis.Y);
        var hiddenLeft2 = new GridWall(-5, -3, GridWall.Axis.Y);
        var hiddenRight1 = new GridWall(3, -3, GridWall.Axis.Y);
        var hiddenRight2 = new GridWall(8, -3, GridWall.Axis.Y);

        var hiddenFrontLeft = new GridWall(0, -1, GridWall.Axis.X);
        var hiddenFrontRight = new GridWall(3, -1, GridWall.Axis.Y);
        var leftBehind = new GridWall(0, -5, GridWall.Axis.Y);
        var rightBehind = new GridWall(3, -5, GridWall.Axis.X);

        var grid = new WallGrid();
        grid.add(obstacleFront);
        grid.add(obstacleLeft);
        grid.add(obstacleRight);
        grid.add(behind);
        grid.add(hiddenFront1);
        grid.add(hiddenFront2);
        grid.add(hiddenLeft1);
        grid.add(hiddenLeft2);
        grid.add(hiddenRight1);
        grid.add(hiddenRight2);
        grid.add(hiddenFrontLeft);
        grid.add(hiddenFrontRight);
        grid.add(leftBehind);
        grid.add(rightBehind);

        var actualWalls = grid.findVisibleWalls(new WallSnapper.FixedPose(1.5 * GRID_SIZE, -2.8 * GRID_SIZE, 0.25));
        var expectedWalls = new HashSet<GridWall>();
        expectedWalls.add(obstacleFront);
        expectedWalls.add(obstacleLeft);
        expectedWalls.add(obstacleRight);

        assertEquals(expectedWalls, actualWalls);
    }
}
