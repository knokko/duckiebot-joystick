package camera;

import org.junit.Test;
import planner.GridWall;

import static controller.util.DuckieWheels.GRID_SIZE;
import static junit.framework.TestCase.assertEquals;

public class TestRelativeWall {

    private void testFromGrid(GridWall gridWall, WallSnapper.FixedPose cameraPose, RelativeWall expected) {
        var actual = RelativeWall.fromGrid(gridWall, cameraPose);
        assertEquals(expected.distance(), actual.distance(), 0.01);
        assertEquals(expected.angle(), actual.angle(), 0.01);
    }

    @Test
    public void testFromGrid() {
        // Simple case with camera at the origin
        testFromGrid(
                new GridWall(2, 3, GridWall.Axis.Y),
                new WallSnapper.FixedPose(0, 0, 0),
                new RelativeWall(4.03 * GRID_SIZE, 0.167)
        );

        // The same case, but everything shifted by (3, 1)
        testFromGrid(
                new GridWall(5, 4, GridWall.Axis.Y),
                new WallSnapper.FixedPose(3 * GRID_SIZE, GRID_SIZE, 0),
                new RelativeWall(4.03 * GRID_SIZE, 0.167)
        );

        // Now with a camera angle of 90 degrees
        testFromGrid(
                new GridWall(5, 4, GridWall.Axis.Y),
                new WallSnapper.FixedPose(3 * GRID_SIZE, GRID_SIZE, 0.25),
                new RelativeWall(4.03 * GRID_SIZE, -0.083)
        );
    }
}
