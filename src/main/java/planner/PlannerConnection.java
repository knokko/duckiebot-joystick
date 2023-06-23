package planner;

import java.io.IOException;
import java.net.Socket;
import java.util.Queue;

public class PlannerConnection {

    static final int PORT = 21311;

    private final String server;
    private final Queue<GridPosition> highLevelRoute;

    public PlannerConnection(String server, Queue<GridPosition> highLevelRoute) {
        this.server = server;
        this.highLevelRoute = highLevelRoute;
    }

    public void start() {
        try (Socket socket = new Socket(server, PORT)) {
            var input = socket.getInputStream();
            while (true) {
                int rawX = input.read();
                if (rawX == -1) {
                    System.out.println("The high-level route is finished");
                    return;
                }
                highLevelRoute.add(new GridPosition((byte) rawX, (byte) input.read()));
            }
        } catch (IOException connectionFailed) {
            connectionFailed.printStackTrace();
        }
    }
}
