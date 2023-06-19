package controller.estimation;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class TestTransferFunctionBuckets {

    @Test
    public void testWithSingleSizeBuckets() {
        var buckets = new TransferFunctionEstimator.Buckets(0.5, 1);
        buckets.insert(0.3, 0.7);
        buckets.insert(-0.4, 0.2);
        buckets.insert(1.8, 2.0);

        assertNull(buckets.getSpeed(-0.51));
        assertEquals(0.2, buckets.getSpeed(-0.49));
        assertEquals(0.2, buckets.getSpeed(-0.1));
        assertEquals(0.7, buckets.getSpeed(0.01));
        assertEquals(0.7, buckets.getSpeed(0.49));
        assertNull(buckets.getSpeed(0.51));
        assertNull(buckets.getSpeed(1.3));
        assertEquals(2.0, buckets.getSpeed(1.8));
        assertEquals(2.0, buckets.getSpeed(1.99));
    }

    @Test
    public void testWithSingleBucket() {
        var buckets = new TransferFunctionEstimator.Buckets(0.8, 3);
        buckets.insert(0.4, 0.6);
        buckets.insert(0.5, 0.7);
        assertNull(buckets.getSpeed(0.1));
        buckets.insert(0.6, 0.2);

        assertEquals(0.6, buckets.getSpeed(0.75));
    }
}
