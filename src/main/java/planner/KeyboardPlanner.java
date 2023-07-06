package planner;

import controller.desired.DesiredPose;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static java.awt.event.KeyEvent.*;
import static planner.RoutePlanner.simpleCos;
import static planner.RoutePlanner.simpleSin;

public class KeyboardPlanner implements KeyListener {

    private final BlockingQueue<GridPosition> highLevelRoute;
    private final List<DesiredPose> lowLevelRoute;

    private byte currentX, currentY;

    public KeyboardPlanner(BlockingQueue<GridPosition> highLevelRoute, List<DesiredPose> lowLevelRoute) {
        this.highLevelRoute = highLevelRoute;
        this.lowLevelRoute = lowLevelRoute;
    }

    private void processAngle(double angle) {
        currentX += simpleCos(angle);
        currentY += simpleSin(angle);
        highLevelRoute.add(new GridPosition(currentX, currentY));
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {}

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == VK_LEFT) processAngle(0.5);
        if (keyEvent.getKeyCode() == VK_RIGHT) processAngle(0.0);
        if (keyEvent.getKeyCode() == VK_UP) processAngle(0.25);
        if (keyEvent.getKeyCode() == VK_DOWN) processAngle(0.75);
        if (keyEvent.getKeyCode() == VK_SPACE) {
            highLevelRoute.clear();
            lowLevelRoute.clear();
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {}
}
