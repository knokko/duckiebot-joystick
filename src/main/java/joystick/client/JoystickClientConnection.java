package joystick.client;

import camera.CameraWalls;
import camera.RelativeWall;
import state.DuckieState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
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
                socket.setTcpNoDelay(true);
                socket.connect(new InetSocketAddress(server, 21002));
                var input = new DataInputStream(socket.getInputStream());
                var output = new DataOutputStream(socket.getOutputStream());

                double lastCommandLeft = Double.NaN;
                double lastCommandRight = Double.NaN;

                while (true) {
                    boolean didSomething = false;
                    while (input.available() > 0) {
                        byte type = input.readByte();
                        if (type == 1) duckieState.leftWheelEncoder = new DuckieState.WheelEncoderEntry(input.readLong(), input.readInt());
                        else if (type == 2) leftMotorConsumer.accept(input.readFloat());
                        else if (type == 3) duckieState.rightWheelEncoder = new DuckieState.WheelEncoderEntry(input.readLong(), input.readInt());
                        else if (type == 4) rightMotorConsumer.accept(input.readFloat());
                        else if (type == 5) {
                            int numWalls = input.readInt();
                            var walls = new ArrayList<RelativeWall>(numWalls);
                            for (int counter = 0; counter < numWalls; counter++) {
                                walls.add(new RelativeWall(input.readFloat(), 1f - input.readFloat()));
                            }
                            duckieState.cameraWalls = new CameraWalls(System.nanoTime(), walls);
                        } else if (type == 6) duckieState.duckie = new DuckieState.DuckiePosition(
                                System.nanoTime(),
                                new RelativeWall(input.readFloat(), input.readFloat())
                        );
                        else duckieState.tof = input.readFloat();
                        didSomething = true;
                    }

                    double commandLeft = leftMotorControl.getAsDouble();
                    double commandRight = rightMotorControl.getAsDouble();

                    if (lastCommandLeft != commandLeft || lastCommandRight != commandRight) {
                        output.writeFloat((float) commandLeft);
                        output.writeFloat((float) commandRight * 1.3f);
                        output.flush();
                        lastCommandLeft = commandLeft;
                        lastCommandRight = commandRight;
                        didSomething = true;
                    }

                    if (!didSomething) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(1);
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
