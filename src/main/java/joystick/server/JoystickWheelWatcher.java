package joystick.server;

import java.io.IOException;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.DoubleConsumer;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

public class JoystickWheelWatcher {

    private static final Pattern TICKS_PATTERN = Pattern.compile("secs:\\s*(\\d+)\\n.*nsecs:\\s*(\\d*)\\n.*\\ndata:\\s(\\d+)");

    private final BiConsumer<Long, Integer> leftEncoderConsumer, rightEncoderConsumer;
    private final DoubleConsumer leftMotorConsumer, rightMotorConsumer;
    private final DoubleConsumer tofConsumer;

    public JoystickWheelWatcher(
            BiConsumer<Long, Integer> leftEncoderConsumer, DoubleConsumer leftMotorConsumer,
            BiConsumer<Long, Integer> rightEncoderConsumer, DoubleConsumer rightMotorConsumer,
            DoubleConsumer tofConsumer
    ) {
        this.leftEncoderConsumer = leftEncoderConsumer;
        this.leftMotorConsumer = leftMotorConsumer;
        this.rightEncoderConsumer = rightEncoderConsumer;
        this.rightMotorConsumer = rightMotorConsumer;
        this.tofConsumer = tofConsumer;
    }

    public void start() {
        start("rostopic echo /db4/left_wheel_encoder_node/tick", message -> {
            var matcher = TICKS_PATTERN.matcher(message);
            if (!matcher.find()) System.out.println("Uh ooh, message was \"" + message + "\"");

            String rawSeconds = matcher.group(1);
            String rawNanoSeconds = matcher.group(2);
            String rawData = matcher.group(3);
            leftEncoderConsumer.accept(1_000_000_000L * parseLong(rawSeconds) + parseLong(rawNanoSeconds), parseInt(rawData));
        }, true);

        start("rostopic echo /db4/right_wheel_encoder_node/tick", message -> {
            var matcher = TICKS_PATTERN.matcher(message);
            if (!matcher.find()) System.out.println("Uh ooh, message was \"" + message + "\"");

            String rawSeconds = matcher.group(1);
            String rawNanoSeconds = matcher.group(2);
            String rawData = matcher.group(3);
            rightEncoderConsumer.accept(1_000_000_000L * parseLong(rawSeconds) + parseLong(rawNanoSeconds), parseInt(rawData));
        }, true);

        start("rostopic echo /db4/wheels_driver_node/wheels_cmd_executed/vel_left", line -> leftMotorConsumer.accept(parseDouble(line)), false);
        start("rostopic echo /db4/wheels_driver_node/wheels_cmd_executed/vel_right", line -> rightMotorConsumer.accept(parseDouble(line)), false);
        start("rostopic echo /db4/front_center_tof_driver_node/range/range", line -> tofConsumer.accept(parseDouble(line)), false);
    }

    private void start(String command, DataConsumer consumer, boolean multiLine) {
        var thread = new Thread(() -> {
            try {
                var process = Runtime.getRuntime().exec(command);
                var scanner = new Scanner(process.getInputStream());
                var lines = new StringBuilder();
                while(scanner.hasNextLine()) {

                    String line = scanner.nextLine();

                    if (line.equals("---")) {
                        if (multiLine) {
                            try {
                                consumer.accept(lines.toString());
                                lines = new StringBuilder();
                            } catch (NumberFormatException invalid) {
                                System.out.println("Got unexpected line: " + lines + " (" + invalid.getMessage() + ")");
                            }
                        }
                    } else {
                        if (multiLine) {
                            lines.append(line);
                            lines.append('\n');
                        } else {
                            try {
                                consumer.accept(line);
                            } catch (NumberFormatException invalid) {
                                System.out.println("Got unexpected line: " + lines + " (" + invalid.getMessage() + ")");
                            }
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
