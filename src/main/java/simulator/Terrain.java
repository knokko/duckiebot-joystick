package simulator;

import java.util.function.DoubleFunction;

import static java.lang.Math.max;

public class Terrain {

    /**
     * An unrealistic terrain where the angular velocity of the wheels is exactly equal to the motor signal. This is
     * convenient for testing the rest of the system.
     */
    public static final Terrain IDEAL = new Terrain(motorSignal -> motorSignal);

    public static final Terrain SIMPLE_VERY_SLOW = new Terrain(motorSignal -> 0.3 * motorSignal);
    public static final Terrain SIMPLE_SLOW = new Terrain(motorThrottle -> 0.6 * motorThrottle);
    public static final Terrain SIMPLE_FAST = new Terrain(motorSignal -> 1.6 * motorSignal);

    public static final Terrain SLOPED_SLOW = new Terrain(motorSignal -> max(0.0, 0.6 * motorSignal - 0.2));
    public static final Terrain SLOPED_FAST = new Terrain(motorSignal -> max(0.0, 1.8 * motorSignal - 0.1));

    public static final Terrain NOISY_SLOW = new Terrain(motorSignal -> max(0, (0.3 * Math.random() + 0.6) * motorSignal - 0.1));
    public static final Terrain VERY_NOISY_SLOW = new Terrain(motorSignal -> max(0, (0.4 * Math.random() + 0.4) * motorSignal - 0.1));
    public static final Terrain NOISY_FAST = new Terrain(motorSignal -> (0.4 * Math.random() + 1.2) * motorSignal);
    public static final Terrain VERY_NOISY_FAST = new Terrain(motorSignal -> (0.8 * Math.random() + 0.8) * motorSignal);

    public static final Terrain SKEWED_SLOW = new Terrain(
            leftSignal -> max(0.6 * leftSignal - 0.15, 0.0),
            rightSignal -> max(0.5 * rightSignal - 0.2, 0.0)
    );

    public static final Terrain SKEWED_FAST = new Terrain(
            leftSignal -> max(1.2 * leftSignal - 0.05, 0.0),
            rightSignal -> max(0.9 * rightSignal - 0.1, 0.0)
    );

    /**
     * <p>
     * The speed functions map the motor signal (in range -1 to 1) to an angular velocity in turns per second. For
     * instance, if leftSpeedFunction(0.5) returns 0.8, it means that giving the left wheel motor a vel_left of 0.5
     * will cause the left wheel to start turning 0.8 times per second.
     * </p>
     * <p>
     *     The purpose of this function is to model various types of terrain on which the duckiebot can drive.
     *     Experience has learned us that the 'speed function' on the table is not the same as the speed function on
     *     the floor.
     * </p>
     */
    public final DoubleFunction<Double> leftSpeedFunction, rightSpeedFunction;

    public Terrain(DoubleFunction<Double> leftSpeedFunction, DoubleFunction<Double> rightSpeedFunction) {
        this.leftSpeedFunction = leftSpeedFunction;
        this.rightSpeedFunction = rightSpeedFunction;
    }

    public Terrain(DoubleFunction<Double> speedFunction) {
        this(speedFunction, speedFunction);
    }
}
