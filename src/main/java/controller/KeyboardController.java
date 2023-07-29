package controller;

import controller.desired.DesiredVelocity;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import state.DuckieControls;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyboardController implements ControllerFunction, KeyListener {

    private final DesiredVelocity desiredVelocity;
    private final DuckieEstimations estimations;
    private final double startTime = 2;
    private double timeSum = 0;
    private double angleOfsset = 0;
    private double adjustmentAngle = 0.005;
    private double speed = 0.35;
    private DesiredVelocity keyboardPose = new DesiredVelocity();

    public KeyboardController(DesiredVelocity desiredVelocity,
            DuckieEstimations estimations, DuckieControls controls)
    {
        this.desiredVelocity = desiredVelocity;
        this.estimations = estimations;
        keyboardPose.speed = speed;
    }
    

    @Override
    public void update(double deltaTime) {
        if(timeSum < startTime)
        {
            timeSum += deltaTime;
            desiredVelocity.angle = estimations.angle; //atan2(dy, dx) / (2 * Math.PI);
            desiredVelocity.speed = 0;
            return;
        }
        desiredVelocity.angle = keyboardPose.angle;
        desiredVelocity.speed = keyboardPose.speed;
        desiredVelocity.turnTime = 0;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                 keyboardPose.angle = angleOfsset + 0.25;
                 keyboardPose.speed = speed;
                 break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                 keyboardPose.angle = angleOfsset + 0.5;
                 keyboardPose.speed = speed;
                 break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                 keyboardPose.angle = angleOfsset + 0.75;
                 keyboardPose.speed = speed;
                 break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                 keyboardPose.angle = angleOfsset + 0.0;
                 keyboardPose.speed = speed;
                break;
            case KeyEvent.VK_SPACE:
                 keyboardPose.speed = 0.0;
                 keyboardPose.angle -= angleOfsset;
                 angleOfsset = 0;
                 break;
            case KeyEvent.VK_E:
                angleOfsset += adjustmentAngle;
                keyboardPose.angle += adjustmentAngle;
                break;
            case KeyEvent.VK_Q:
                angleOfsset -= adjustmentAngle;
                keyboardPose.angle -=adjustmentAngle;
                break;
            default:
            break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
