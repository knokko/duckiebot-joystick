package simulator;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class TestSimulatorLatency {

    @Test
    public void testZeroLatency() {
        var testLatency = new SimulatorLatency<>(0.0, 1);
        assertEquals(1, (int) testLatency.get(1));
        testLatency.insert(1.1, 5);
        assertEquals(5, (int) testLatency.get(1.1));
        assertEquals(5, (int) testLatency.get(1.11));

        testLatency.insert(1.2, -3);
        assertEquals(-3, (int) testLatency.get(1.2));
    }

    @Test
    public void testWithLatency() {
        var testLatency = new SimulatorLatency<>(0.5, 3);
        assertEquals(3, (int) testLatency.get(1.0));
        testLatency.insert(1.1, 4);
        assertEquals(3, (int) testLatency.get(1.1));
        testLatency.insert(1.3, 8);
        assertEquals(3, (int) testLatency.get(1.4));
        assertEquals(4, (int) testLatency.get(1.61));
        assertEquals(4, (int) testLatency.get(1.71));
        assertEquals(8, (int) testLatency.get(1.81));
        assertEquals(8, (int) testLatency.get(2.81));
    }
}
