package joystick.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class JoystickConnectionManager {

    private final List<Socket> sockets = new ArrayList<>();

    private final BiConsumer<Float, Float> controlMotor;

    public JoystickConnectionManager(BiConsumer<Float, Float> controlMotor) {
        this.controlMotor = controlMotor;
    }

    public void broadcastLeftWheelEncoder(int data) {
        broadcast((byte) 1, output -> output.writeInt(data));
    }

    public void broadcastLeftWheelMotor(double data) {
        broadcast((byte) 2, output -> output.writeFloat((float) data));
    }

    public void broadcastRightWheelEncoder(int data) {
        broadcast((byte) 3, output -> output.writeInt(data));
    }

    public void broadcastRightWheelMotor(double data) {
        broadcast((byte) 4, output -> output.writeFloat((float) data));
    }

    public void broadcastTof(double data) {
        broadcast((byte) 5, output -> output.writeFloat((float) data));
    }

    private void broadcast(byte type, DataSource writeData) {
        synchronized (this) {
            sockets.removeIf(socket -> {
                try {
                    var output = new DataOutputStream(socket.getOutputStream());
                    output.writeByte(type);
                    writeData.write(output);
                    output.flush();
                } catch (IOException e) {
                    System.out.println("failed to notify a socket");
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        return true;
                    }
                }
                return socket.isClosed();
            });
        }
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket()) {
                serverSocket.bind(new InetSocketAddress(21002));

                while (true) {
                    Socket newSocket = serverSocket.accept();
                    synchronized (this) {
                        sockets.add(newSocket);
                    }
                    new Thread(() -> manageClient(newSocket)).start();
                    System.out.println("found client");
                }
            } catch (IOException failed) {
                failed.printStackTrace();
            }
        }).start();
    }

    private void manageClient(Socket socket) {
        try {
            var input = new DataInputStream(socket.getInputStream());
            while (true) {
                controlMotor.accept(input.readFloat(), input.readFloat());
            }
        } catch (IOException failed) {
            try {
                socket.close();
            } catch (IOException failedToClose) {
                // In this case, the connection was already broken, and there is nothing more we can/need to do
            }
        }
        System.out.println("A client disconnected");
    }

    @FunctionalInterface
    private interface DataSource {
        void write(DataOutputStream output) throws IOException;
    }
}
