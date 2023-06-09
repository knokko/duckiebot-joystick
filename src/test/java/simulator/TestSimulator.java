package simulator;

import org.junit.Test;

import static controller.util.DuckieWheels.*;
import static junit.framework.TestCase.assertEquals;

public class TestSimulator {

    @Test
    public void testDriveForwardOnIdealTerrain() {
        var simulator = new Simulator(Terrain.IDEAL);

        simulator.controls.velLeft = 1.0;
        simulator.controls.velRight = 1.0;

        // Simulate 0.5 seconds
        for (int counter = 0; counter < 500; counter++) {
            simulator.update(0.001);
            assertEquals(1.0, simulator.realPose.velocityX, 0.001);
            assertEquals(0.0, simulator.realPose.velocityY, 0.001);
        }

        // It should have driven 0.5 meters in the positive X direction
        assertEquals(0.5, simulator.realPose.x, 0.01);
        assertEquals(0.0, simulator.realPose.y, 0.01);

        assertEquals(WHEEL_TICKS_PER_TURN * 0.5 / (2 * Math.PI * WHEEL_RADIUS), simulator.trackedState.leftWheelEncoder, 1.1);
        assertEquals(simulator.trackedState.leftWheelEncoder, (double) simulator.trackedState.rightWheelEncoder, 1.1);
    }

    @Test
    public void testDriveRightWheelOnIdealTerrain() {
        var simulator = new Simulator(Terrain.IDEAL);

        simulator.controls.velLeft = 0.0;
        simulator.controls.velRight = 1.0;

        double rotateTime = 250 * (2 * Math.PI * DISTANCE_BETWEEN_WHEELS);
        for (int counter = 0; counter < rotateTime; counter++) {
            simulator.update(0.001);
        }

        // It should have rotated 90 degrees, which is 0.25 turns
        assertEquals(0.25, simulator.realPose.angle, 0.01);

        // This rotation should have caused a minor movement
        assertEquals(0.05, simulator.realPose.x, 0.01);
        assertEquals(0.05, simulator.realPose.y, 0.01);

        // After rotating, it should have half of the velocity of 1.0 to the positive Y direction
        assertEquals(0.0, simulator.realPose.velocityX, 0.01);
        assertEquals(0.5, simulator.realPose.velocityY, 0.01);

        assertEquals(0.0, simulator.trackedState.leftWheelEncoder, 1.1);
        assertEquals(WHEEL_TICKS_PER_TURN * 0.25 * DISTANCE_BETWEEN_WHEELS / WHEEL_RADIUS, simulator.trackedState.rightWheelEncoder, 1.1);
    }

    @Test
    public void testRotateRightOnIdealTerrain() {
        var simulator = new Simulator(Terrain.IDEAL);
        simulator.controls.velLeft = 1.0;
        simulator.controls.velRight = -1.0;

        double rotateTime = 250 * (2 * Math.PI * DISTANCE_BETWEEN_WHEELS);
        for (int counter = 0; counter < rotateTime; counter++) {
            simulator.update(0.001);
        }

        // It should have rotated 180 degrees, which is 0.5 turns
        assertEquals(0.5, simulator.realPose.angle, 0.01);

        // This rotation should not cause the duckiebot to move, nor to get any velocity
        assertEquals(0.0, simulator.realPose.x, 0.01);
        assertEquals(0.0, simulator.realPose.y, 0.01);
        assertEquals(0.0, simulator.realPose.velocityX, 0.01);
        assertEquals(0.0, simulator.realPose.velocityY, 0.01);
    }
}
