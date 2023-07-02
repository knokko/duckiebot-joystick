package simulator.bezier;

import javax.swing.*;

public class BezierSimulator {

    public static void main(String[] args) {
        var frame = new JFrame();
        var board = new BezierBoard(frame::getInsets);
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.add(board);
        frame.addMouseListener(board);
        frame.addMouseMotionListener(board);
        frame.setVisible(true);
    }
}
