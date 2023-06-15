package joystick.server;

import java.io.IOException;
import java.util.Scanner;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

public class JoystickWheelWatcher {

    private final IntConsumer leftEncoderConsumer, rightEncoderConsumer;
    private final DoubleConsumer leftMotorConsumer, rightMotorConsumer;
    private final DoubleConsumer tofConsumer;

    public JoystickWheelWatcher(
            IntConsumer leftEncoderConsumer, DoubleConsumer leftMotorConsumer,
            IntConsumer rightEncoderConsumer, DoubleConsumer rightMotorConsumer,
            DoubleConsumer tofConsumer
    ) {
        this.leftEncoderConsumer = leftEncoderConsumer;
        this.leftMotorConsumer = leftMotorConsumer;
        this.rightEncoderConsumer = rightEncoderConsumer;
        this.rightMotorConsumer = rightMotorConsumer;
        this.tofConsumer = tofConsumer;
    }

    public void start() {
        start("rostopic echo /db4/left_wheel_encoder_node/tick/data", line -> leftEncoderConsumer.accept(parseInt(line)));
        start("rostopic echo /db4/right_wheel_encoder_node/tick/data", line -> rightEncoderConsumer.accept(parseInt(line)));
        start("rostopic echo /db4/wheels_driver_node/wheels_cmd_executed/vel_left", line -> leftMotorConsumer.accept(parseDouble(line)));
        start("rostopic echo /db4/wheels_driver_node/wheels_cmd_executed/vel_right", line -> rightMotorConsumer.accept(parseDouble(line)));
        start("rostopic echo /db4/front_center_tof_driver_node/range/range", line -> tofConsumer.accept(parseDouble(line)));
    }

    private void start(String command, DataConsumer consumer) {
        var thread = new Thread(() -> {
            try {
                var process = Runtime.getRuntime().exec(command);
                var scanner = new Scanner(process.getInputStream());
                while(scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (!line.equals("---")) {
                        try {
                            consumer.accept(line);
                        } catch (NumberFormatException invalid) {
                            System.out.println("Got unexpected line: " + line);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FunctionalInterface
    private interface DataConsumer {
        void accept(String line) throws NumberFormatException;
    }
}
