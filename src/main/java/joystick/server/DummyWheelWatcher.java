package joystick.server;

import java.util.Random;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public class DummyWheelWatcher {

    private final IntConsumer leftEncoderConsumer, rightEncoderConsumer;
    private final DoubleConsumer leftMotorConsumer, rightMotorConsumer;

    public DummyWheelWatcher(
            IntConsumer leftEncoderConsumer, DoubleConsumer leftMotorConsumer,
            IntConsumer rightEncoderConsumer, DoubleConsumer rightMotorConsumer
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
                leftEncoderConsumer.accept(rng.nextBoolean() ? 2 : 3);
                rightEncoderConsumer.accept(rng.nextBoolean() ? 2 : 3);
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
