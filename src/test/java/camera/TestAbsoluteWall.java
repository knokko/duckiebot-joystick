package camera;

import org.junit.Test;
import planner.GridWall;

import static controller.util.DuckieBot.GRID_SIZE;
import static junit.framework.TestCase.assertEquals;

public class TestAbsoluteWall {

    private void testSnap(AbsoluteWall wall, double expectedError, GridWall expectedWall) {
        var actual = wall.snap();
        assertEquals(expectedError, actual.error(), 0.001);
        if (expectedWall != null) assertEquals(expectedWall, actual.wall());
    }

    @Test
    public void testComputeError() {

        // Simple cases where the expected error is 0 and the wall is in grid (0, 0)
        testSnap(new AbsoluteWall(0.5 * GRID_SIZE, 0), 0, new GridWall(0, 0, GridWall.Axis.X));
        testSnap(new AbsoluteWall(0, 0.5 * GRID_SIZE), 0, new GridWall(0, 0, GridWall.Axis.Y));
        testSnap(new AbsoluteWall(0.5 * GRID_SIZE, 1 * GRID_SIZE), 0, new GridWall(0, 1, GridWall.Axis.X));
        testSnap(new AbsoluteWall(1 * GRID_SIZE, 0.5 * GRID_SIZE), 0, new GridWall(1, 0, GridWall.Axis.Y));

        // Cases where the wall is in the +X -Y quadrant and the error is 0
        testSnap(new AbsoluteWall(7.5 * GRID_SIZE, -2 * GRID_SIZE), 0, new GridWall(7, -2, GridWall.Axis.X));
        testSnap(new AbsoluteWall(2.0 * GRID_SIZE, -4.5 * GRID_SIZE), 0, new GridWall(2, -5, GridWall.Axis.Y));
        testSnap(new AbsoluteWall(0.5 * GRID_SIZE, -30 * GRID_SIZE), 0, new GridWall(0, -30, GridWall.Axis.X));
        testSnap(new AbsoluteWall(1.5 * GRID_SIZE, 0 * GRID_SIZE), 0, new GridWall(1, 0, GridWall.Axis.X));

        // The wall is in the center of a grid (which is awful)
        testSnap(new AbsoluteWall(-3.5 * GRID_SIZE, 0.5 * GRID_SIZE), 0.25, null);

        // The wall is in a corner (which is also bad)
        testSnap(new AbsoluteWall(0, 0), 0.25, null);
        testSnap(new AbsoluteWall(2 * GRID_SIZE, -5 * GRID_SIZE), 0.25, null);
        testSnap(new AbsoluteWall(6 * GRID_SIZE, 7 * GRID_SIZE), 0.25, null);

        // Closest to the up-wall
        testSnap(new AbsoluteWall(8.7 * GRID_SIZE, 6.9 * GRID_SIZE), 0.05, new GridWall(8, 7, GridWall.Axis.X));
        testSnap(new AbsoluteWall(8.3 * GRID_SIZE, 6.9 * GRID_SIZE), 0.05, new GridWall(8, 7, GridWall.Axis.X));

        // Closest to the right-wall
        testSnap(new AbsoluteWall(-3.1 * GRID_SIZE, 4.7 * GRID_SIZE), 0.05, new GridWall(-3, 4, GridWall.Axis.Y));
        testSnap(new AbsoluteWall(-3.1 * GRID_SIZE, 4.3 * GRID_SIZE), 0.05, new GridWall(-3, 4, GridWall.Axis.Y));

        // Closest to the down-wall
        testSnap(new AbsoluteWall(8.4 * GRID_SIZE, 6.1 * GRID_SIZE), 0.02, new GridWall(8, 6, GridWall.Axis.X));
        testSnap(new AbsoluteWall(8.6 * GRID_SIZE, 6.1 * GRID_SIZE), 0.02, new GridWall(8, 6, GridWall.Axis.X));

        // Closest to the left-wall
        testSnap(new AbsoluteWall(-1.9 * GRID_SIZE, -3.5 * GRID_SIZE), 0.01, new GridWall(-2, -4, GridWall.Axis.Y));
        testSnap(new AbsoluteWall(-1.8 * GRID_SIZE, -3.5 * GRID_SIZE), 0.04, new GridWall(-2, -4, GridWall.Axis.Y));
    }
}
