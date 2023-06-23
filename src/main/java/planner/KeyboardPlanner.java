package planner;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.awt.event.KeyEvent.*;
import static planner.RoutePlanner.simpleCos;
import static planner.RoutePlanner.simpleSin;

public class KeyboardPlanner implements KeyListener {

    private final BlockingQueue<Double> commandQueue = new LinkedBlockingQueue<>();

    private byte currentX, currentY;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PlannerConnection.PORT)) {
            Socket socket = serverSocket.accept();
            var output = socket.getOutputStream();

            while (true) {
                double nextAngle = commandQueue.take();
                if (nextAngle == -1.0) {
                    System.out.println("Stopping KeyboardPlanner");
                    socket.close();
                    return;
                }
                currentX += simpleCos(nextAngle);
                currentY += simpleSin(nextAngle);
                output.write(new byte[] { currentX, currentY });
                output.flush();
            }
        } catch (IOException bindFailed) {
            System.out.println("KeyboardPlanner stopped or failed: " + bindFailed.getMessage());
        } catch (InterruptedException shouldNotHappen) {
            throw new RuntimeException(shouldNotHappen);
        }
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {}

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == VK_LEFT) commandQueue.add(0.5);
        if (keyEvent.getKeyCode() == VK_RIGHT) commandQueue.add(0.0);
        if (keyEvent.getKeyCode() == VK_UP) commandQueue.add(0.25);
        if (keyEvent.getKeyCode() == VK_DOWN) commandQueue.add(0.75);
        if (keyEvent.getKeyCode() == VK_ESCAPE) stop();
    }

    public void stop() {
        commandQueue.add(-1.0);
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {}
}
