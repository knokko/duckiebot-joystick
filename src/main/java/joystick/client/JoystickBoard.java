package joystick.client;

import state.DuckieState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Supplier;

import static java.lang.Math.abs;

public class JoystickBoard extends JPanel implements KeyListener, MouseListener, MouseMotionListener {

    private final Supplier<Insets> rootInsets;
    private final DuckieState duckieState;

    private double joystickX, joystickY;

    private Integer lastLeftEncoder, lastRightEncoder;
    private double leftMotor, rightMotor;

    public JoystickBoard(Supplier<Insets> rootInsets, DuckieState duckieState) {
        this.rootInsets = rootInsets;
        this.duckieState = duckieState;
    }

    private double getCommand(double unboundedValue) {
        if (unboundedValue < -1.0) return -1.0;
        return Math.min(unboundedValue, 1.0);
    }

    public double getLeftCommand() {
        return getCommand(joystickY + joystickX);
    }

    public double getRightCommand() {
        return getCommand(joystickY - joystickX);
    }

    public void processLeftMotor(double data) {
        SwingUtilities.invokeLater(() -> this.leftMotor = data);
    }

    public void processRightMotor(double data) {
        SwingUtilities.invokeLater(() -> this.rightMotor = data);
    }

    @Override
    public void paint(Graphics graphics) {
        var leftWheelEncoder = duckieState.leftWheelEncoder;
        var rightWheelEncoder = duckieState.rightWheelEncoder;
        int deltaLeft = 0;
        int deltaRight = 0;
        if (leftWheelEncoder != null && rightWheelEncoder != null && lastLeftEncoder != null && lastRightEncoder != null) {
            deltaLeft = 50 * (leftWheelEncoder.value() - lastLeftEncoder);
            deltaRight = 50 * (rightWheelEncoder.value() - lastRightEncoder);
        }
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

        graphics.setColor(Color.GREEN);
        int tofLength = (int) (400 * duckieState.tof);
        graphics.fillRect(820, 50, 50, tofLength);

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

        if (leftWheelEncoder != null && rightWheelEncoder != null) {
            lastLeftEncoder = leftWheelEncoder.value();
            lastRightEncoder = rightWheelEncoder.value();
        }

        Toolkit.getDefaultToolkit().sync();

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

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        int x = mouseEvent.getX();
        int y = mouseEvent.getY() - rootInsets.get().top;

        if (x > 400 && y > 50 && x < 800 && y < 450) {
            double jx = (x - 600.0) / 200.0;
            double jy = (250.0 - y) / 200.0;
            if (mouseEvent.isShiftDown()) {
                if (abs(jx) > abs(jy)) jy = 0;
                else jx = 0;
            }

            joystickX = jx;
            joystickY = jy;
        }
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {}

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {}

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        mouseDragged(mouseEvent);
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        joystickX = 0;
        joystickY = 0;
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {}

    @Override
    public void mouseExited(MouseEvent mouseEvent) {}
}
