package joystick.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;

public class JoystickClientConnection {

    private final String server;
    private final IntConsumer leftEncoderConsumer, rightEncoderConsumer;
    private final DoubleConsumer leftMotorConsumer, rightMotorConsumer;
    private final DoubleSupplier leftMotorControl, rightMotorControl;

    public JoystickClientConnection(
            String server,
            IntConsumer leftEncoderConsumer, DoubleConsumer leftMotorConsumer,
            IntConsumer rightEncoderConsumer, DoubleConsumer rightMotorConsumer,
            DoubleSupplier leftMotorControl, DoubleSupplier rightMotorControl
    ) {
        this.server = server;
        this.leftEncoderConsumer = leftEncoderConsumer;
        this.leftMotorConsumer = leftMotorConsumer;
        this.rightEncoderConsumer = rightEncoderConsumer;
        this.rightMotorConsumer = rightMotorConsumer;
        this.leftMotorControl = leftMotorControl;
        this.rightMotorControl = rightMotorControl;
    }

    public void start() {
        var thread = new Thread(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(server, 21002));
                var input = new DataInputStream(socket.getInputStream());
                var output = new DataOutputStream(socket.getOutputStream());

                double lastCommandLeft = leftMotorControl.getAsDouble();
                double lastCommandRight = rightMotorControl.getAsDouble();

                while (true) {
                    boolean didSomething = false;
                    while (input.available() > 0) {
                        byte type = input.readByte();
                        if (type == 1) leftEncoderConsumer.accept(input.readInt());
                        else if (type == 2) leftMotorConsumer.accept(input.readFloat());
                        else if (type == 3) rightEncoderConsumer.accept(input.readInt());
                        else rightMotorConsumer.accept(input.readFloat());
                        didSomething = true;
                    }

                    double commandLeft = leftMotorControl.getAsDouble();
                    double commandRight = rightMotorControl.getAsDouble();

                    if (lastCommandLeft != commandLeft || lastCommandRight != commandRight) {
                        output.writeFloat((float) commandLeft);
                        output.writeFloat((float) commandRight);
                        lastCommandLeft = commandLeft;
                        lastCommandRight = commandRight;
                        didSomething = true;
                    }

                    if (!didSomething) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException shouldNotHappen) {
                            throw new Error(shouldNotHappen);
                        }
                    }
                }
            } catch (IOException failed) {
                failed.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
