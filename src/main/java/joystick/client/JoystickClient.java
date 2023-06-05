package joystick.client;

import javax.swing.*;

public class JoystickClient {

    public static void main(String[] args) {
        boolean dummy = args.length > 0 && args[0].equals("dummy");

        JFrame frame = new JFrame();
        var joystick = new JoystickBoard(frame::getInsets);
        frame.setSize(810, 700);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(joystick);
        frame.addKeyListener(joystick);
        frame.addMouseMotionListener(joystick);
        frame.setVisible(true);

        var connection = new JoystickClientConnection(
                dummy ? "localhost" : "db4.local",
                joystick::processLeftEncoder, joystick::processLeftMotor,
                joystick::processRightEncoder, joystick::processRightMotor,
                joystick::getLeftCommand, joystick::getRightCommand
        );
        connection.start();
    }
}
