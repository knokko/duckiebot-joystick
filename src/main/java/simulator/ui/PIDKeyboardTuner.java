package simulator.ui;

import controller.parameters.PIDParameters;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static java.awt.event.KeyEvent.*;

public class PIDKeyboardTuner implements KeyListener {

    private final PIDParameters pid;

    private char coefficient = '?';

    public PIDKeyboardTuner(PIDParameters pid) {
        this.pid = pid;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent event) {
        int code = event.getKeyCode();
        if (code == VK_P) coefficient = 'p';
        if (code == VK_I) coefficient = 'i';
        if (code == VK_D) coefficient = 'd';

        if (coefficient != '?' && (code == VK_LEFT || code == VK_RIGHT || code == VK_UP || code == VK_DOWN)) {
            double value;
            if (coefficient == 'p') value = pid.Kp;
            else if (coefficient == 'i') value = pid.Ki;
            else if (coefficient == 'd') value = pid.Kd;
            else throw new IllegalStateException("Unexpected coefficient: " + coefficient);

            if (code == VK_LEFT) value += 0.1;
            if (code == VK_RIGHT) value -= 0.1;
            if (code == VK_UP) value += 0.001;
            if (code == VK_DOWN) value -= 0.001;

            if (coefficient == 'p') pid.Kp = value;
            if (coefficient == 'i') pid.Ki = value;
            if (coefficient == 'd') pid.Kd = value;

            System.out.printf("New PID: Kp = %.3f, Ki=%.4f, Kd=%.5f\n", pid.Kp, pid.Ki, pid.Kd);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}
