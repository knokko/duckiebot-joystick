package joystick.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static java.lang.Math.abs;

public class JoystickBoard extends JPanel implements KeyListener {

    private int lastLeftEncoder, lastRightEncoder, currentLeftEncoder, currentRightEncoder;
    private double leftMotor, rightMotor;

    private double joystickX, joystickY;

    private double getCommand(double unboundedValue) {
        if (unboundedValue < -1.0) return -1.0;
        return Math.min(unboundedValue, 1.0);
    }

    public double getLeftCommand() {
        return getCommand(joystickY - joystickX);
    }

    public double getRightCommand() {
        return getCommand(joystickY + joystickX);
    }

    public void processLeftEncoder(int data) {
        SwingUtilities.invokeLater(() -> {
            this.lastLeftEncoder = currentLeftEncoder;
            this.currentLeftEncoder = data;
        });
    }

    public void processLeftMotor(double data) {
        SwingUtilities.invokeLater(() -> this.leftMotor = data);
    }

    public void processRightEncoder(int data) {
        SwingUtilities.invokeLater(() -> {
            this.lastRightEncoder = currentRightEncoder;
            this.currentRightEncoder = data;
        });
    }

    public void processRightMotor(double data) {
        SwingUtilities.invokeLater(() -> this.rightMotor = data);
    }

    @Override
    public void paint(Graphics graphics) {
        int deltaLeft = 50 * (currentLeftEncoder - lastLeftEncoder);
        int deltaRight = 50 * (currentRightEncoder - lastRightEncoder);
        int deltaLeftMotor = (int) (300f * leftMotor);
        int deltaRightMotor = (int) (300f * rightMotor);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, this.getWidth(), this.getHeight());

        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("TimesRoman", Font.BOLD, 20));
        graphics.drawString("Wheel encoders", 10, 30);
        graphics.drawString("Motor commands", 200, 30);
        graphics.drawString("Joystick", 550, 30);

        graphics.setColor(new Color(0, 50, 200));
        int baseY = 350;
        if (deltaLeft > 0) {
            graphics.fillRect(20, baseY - deltaLeft, 80, deltaLeft);
        } else {
            graphics.fillRect(20, baseY, 80, -deltaLeft);
        }
        graphics.setColor(new Color(250, 200, 0));
        if (deltaLeftMotor > 0) {
            graphics.fillRect(220, baseY - deltaLeftMotor, 80, deltaLeftMotor);
        } else {
            graphics.fillRect(220, baseY, 80, -deltaLeftMotor);
        }

        graphics.setColor(Color.BLUE);
        if (deltaRight > 0) {
            graphics.fillRect(100, baseY - deltaRight, 80, deltaRight);
        } else {
            graphics.fillRect(100, baseY, 80, -deltaRight);
        }
        graphics.setColor(new Color(200, 150, 50));
        if (deltaRightMotor > 0) {
            graphics.fillRect(300, baseY - deltaRightMotor, 80, deltaRightMotor);
        } else {
            graphics.fillRect(300, baseY, 80, -deltaRightMotor);
        }

        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(400, 50, 400, 400);
        graphics.setColor(Color.BLACK);
        graphics.drawRect(400, 50, 400, 400);
        graphics.drawLine(600, 50, 600, 450);
        graphics.drawLine(400, 250, 800, 250);
        int originRadius = 20;
        graphics.drawOval(600 - originRadius, 250 - originRadius, 2 * originRadius, 2 * originRadius);
        int joystickRadius = 40;
        graphics.setColor(Color.RED);
        graphics.drawOval(
                600 + (int) (200 * joystickX) - joystickRadius,
                250 - (int) (200 * joystickY) - joystickRadius,
                2 * joystickRadius, 2 * joystickRadius
        );
        joystickRadius = 5;
        graphics.fillOval(
                600 + (int) (200 * joystickX) - joystickRadius,
                250 - (int) (200 * joystickY) - joystickRadius,
                2 * joystickRadius, 2 * joystickRadius
        );

        new Thread(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            this.repaint();
        }).start();
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {}

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        double factor = keyEvent.isShiftDown() ? 0.1 : 1.0;
        if (keyEvent.getKeyCode() == KeyEvent.VK_LEFT) joystickX -= 0.1 * factor;
        if (keyEvent.getKeyCode() == KeyEvent.VK_RIGHT) joystickX += 0.1 * factor;
        if (keyEvent.getKeyCode() == KeyEvent.VK_DOWN) joystickY -= 0.1 * factor;
        if (keyEvent.getKeyCode() == KeyEvent.VK_UP) joystickY += 0.1 * factor;
        if (keyEvent.getKeyCode() == KeyEvent.VK_SPACE) {
            joystickX = 0.0;
            joystickY = 0.0;
        }

        joystickX = Math.max(joystickX, -1.0);
        joystickX = Math.min(joystickX, 1.0);
        joystickY = Math.max(joystickY, -1.0);
        joystickY = Math.min(joystickY, 1.0);

        if (abs(joystickX) < 0.001) joystickX = 0.0;
        if (abs(joystickY) < 0.001) joystickY = 0.0;
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {}
}
