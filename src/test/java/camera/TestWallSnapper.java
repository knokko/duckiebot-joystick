package camera;

import org.junit.Test;
import planner.GridWall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static controller.util.DuckieWheels.GRID_SIZE;
import static junit.framework.TestCase.assertEquals;

public class TestWallSnapper {

    private static void testTransformWall(RelativeWall input, double x, double y, double angle, AbsoluteWall output) {
        var actualOutput = WallSnapper.transformWall(input, x, y, angle);
        assertEquals(output.x(), actualOutput.x(), 0.01);
        assertEquals(output.y(), actualOutput.y(), 0.01);
    }

    @Test
    public void testTransformWall() {
        // Simple case: wall at 0 distance
        testTransformWall(new RelativeWall(0, 0.9), 10, 20, 0.4, new AbsoluteWall(10, 20));

        // Simple case: relative angle is 0
        testTransformWall(new RelativeWall(10, 0), 50, 30, 0.5, new AbsoluteWall(40, 30));

        // Simple case: own angle is 0
        testTransformWall(new RelativeWall(20, 0.25), -40, 20, 0, new AbsoluteWall(-40, 40));

        // Simple case: own position is the origin
        testTransformWall(new RelativeWall(30, 0.4), 0, 0, 0.1, new AbsoluteWall(-30, 0));
    }

    @SafeVarargs
    private <T> Collection<T> walls(T... walls) {
        var result = new ArrayList<T>(walls.length);
        Collections.addAll(result, walls);
        return result;
    }

    @Test
    public void testSnapSimpleTranslations() {

        var snapToLeft = new WallSnapper(walls(
                new RelativeWall(1.7 * GRID_SIZE, 0), new RelativeWall(3.7 * GRID_SIZE, 0)
        ), new WallSnapper.FixedPose(0, 0, 0));

        assertEquals(0.0, snapToLeft.computeError(0.0, -0.2 * GRID_SIZE, 0.0), 0.001);

        var leftResult = snapToLeft.snap(
                0.1, 11, 0.4 * GRID_SIZE, 91
        );
        assertEquals(0.0, leftResult.correctedPose().angle(), 0.01);
        assertEquals(-0.2 * GRID_SIZE, leftResult.correctedPose().x(), 0.01);
        assertEquals(0.0, leftResult.correctedPose().y(), 0.01);
        assertEquals(0.0, leftResult.error(), 0.001);
        assertEquals(walls(
                new GridWall(1, 0, GridWall.Axis.X),
                new GridWall(3, 0, GridWall.Axis.X)
        ), leftResult.walls());

        var snapUp = new WallSnapper(walls(
                RelativeWall.cartesian(1.8 * GRID_SIZE, 2.0 * GRID_SIZE),
                RelativeWall.cartesian(-4.7 * GRID_SIZE, -0.5 * GRID_SIZE)
        ), new WallSnapper.FixedPose(0.2 * GRID_SIZE, 0.4 * GRID_SIZE, 0));

        var upResult = snapUp.snap(
                0.1, 33, GRID_SIZE * 0.3, 66
        );
        assertEquals(0.0, upResult.correctedPose().angle(), 0.01);
        assertEquals(0.2 * GRID_SIZE, upResult.correctedPose().x(), 0.01);
        assertEquals(0.5 * GRID_SIZE, upResult.correctedPose().y(), 0.01);
        assertEquals(0.0, upResult.error(), 0.01);
        assertEquals(walls(
                new GridWall(2, 2, GridWall.Axis.Y),
                new GridWall(-5, 0, GridWall.Axis.X)
        ), upResult.walls());
    }

    // TODO Test with angles
}
