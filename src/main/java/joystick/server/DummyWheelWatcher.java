package joystick.server;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public class DummyWheelWatcher {

    private final BiConsumer<Long, Integer> leftEncoderConsumer, rightEncoderConsumer;
    private final DoubleConsumer leftMotorConsumer, rightMotorConsumer;

    public DummyWheelWatcher(
            BiConsumer<Long, Integer> leftEncoderConsumer, DoubleConsumer leftMotorConsumer,
            BiConsumer<Long, Integer> rightEncoderConsumer, DoubleConsumer rightMotorConsumer
    ) {
        this.leftEncoderConsumer = leftEncoderConsumer;
        this.rightEncoderConsumer = rightEncoderConsumer;
        this.leftMotorConsumer = leftMotorConsumer;
        this.rightMotorConsumer = rightMotorConsumer;
    }

    public void start() {
        Thread thread = new Thread(() -> {
            Random rng = new Random();
            while (true) {
                leftEncoderConsumer.accept(System.nanoTime(), rng.nextBoolean() ? 2 : 3);
                rightEncoderConsumer.accept(System.nanoTime(), rng.nextBoolean() ? 2 : 3);
                leftMotorConsumer.accept(0.9);
                rightMotorConsumer.accept(1.0);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
