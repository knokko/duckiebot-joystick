package joystick.client;

import state.DuckieState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class JoystickClientConnection {

    private final String server;
    private final DuckieState duckieState;
    private final DoubleConsumer leftMotorConsumer, rightMotorConsumer;
    private final DoubleSupplier leftMotorControl, rightMotorControl;

    public JoystickClientConnection(
            String server, DuckieState duckieState,
            DoubleConsumer leftMotorConsumer, DoubleConsumer rightMotorConsumer,
            DoubleSupplier leftMotorControl, DoubleSupplier rightMotorControl
    ) {
        this.server = server;
        this.duckieState = duckieState;
        this.leftMotorConsumer = leftMotorConsumer;
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

                double lastCommandLeft = Double.NaN;
                double lastCommandRight = Double.NaN;

                while (true) {
                    boolean didSomething = false;
                    while (input.available() > 0) {
                        byte type = input.readByte();
                        if (type == 1) duckieState.leftWheelEncoder = input.readInt();
                        else if (type == 2) leftMotorConsumer.accept(input.readFloat());
                        else if (type == 3) duckieState.rightWheelEncoder = input.readInt();
                        else if (type == 4) rightMotorConsumer.accept(input.readFloat());
                        else duckieState.tof = input.readFloat();
                        didSomething = true;
                    }

                    double commandLeft = leftMotorControl.getAsDouble();
                    double commandRight = rightMotorControl.getAsDouble();

                    if (lastCommandLeft != commandLeft || lastCommandRight != commandRight) {
                        output.writeFloat((float) commandLeft);
                        output.writeFloat((float) commandRight);
                        output.flush();
                        //System.out.printf("Sent (%.2f, %.2f)\n", commandLeft, commandRight);
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
