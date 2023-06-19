package controller.estimation;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class TestTransferFunctionBucket {

    @Test
    public void simpleTest() {
        var bucket = new TransferFunctionEstimator.Bucket(5);
        assertNull(bucket.getMedian());

        bucket.insert(0.4);
        bucket.insert(0.35);
        bucket.insert(0.7);
        bucket.insert(0.5);

        assertNull(bucket.getMedian());

        bucket.insert(0.6);
        assertEquals(0.5, bucket.getMedian()); // 0.35, 0.4, 0.5, 0.6, 0.7

        bucket.insert(0.49);
        assertEquals(0.49, bucket.getMedian()); // 0.35, 0.4, 0.49, 0.5, 0.6

        bucket.insert(0.8);
        assertEquals(0.5, bucket.getMedian());
    }

    @Test
    public void testSingleSize() {
        var bucket = new TransferFunctionEstimator.Bucket(1);
        assertNull(bucket.getMedian());

        bucket.insert(0.5);
        assertEquals(0.5, bucket.getMedian());

        bucket.insert(-0.4);
        assertEquals(-0.4, bucket.getMedian());
    }
}
