package simulator.bezier;

import controller.BezierController;
import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.estimation.DuckieEstimations;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.LinkedList;
import java.util.function.Supplier;

import static java.lang.Math.*;

public class BezierBoard extends JPanel implements MouseListener, MouseMotionListener {

    private final Supplier<Insets> insets;
    private final DuckieEstimations estimations = new DuckieEstimations();

    public BezierBoard(Supplier<Insets> insets) {
        this.insets = insets;
        estimations.x = 0.15;
        estimations.y = 0.75;
        estimations.angle = 0.0;
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        graphics.setColor(Color.BLACK);
        for (double d = 0.3; d < 0.91; d += 0.3) {
            graphics.drawLine(transformX(d), 0, transformX(d), getHeight());
            graphics.drawLine(0, transformY(d), getWidth(), transformY(d));
        }
        {
            graphics.setColor(Color.YELLOW);
            int radius = 7;
            graphics.fillOval(transformX(estimations.x) - radius, transformY(estimations.y) - radius, 2 * radius, 2 * radius);

            double angleLength = 0.04;
            graphics.setColor(Color.BLACK);
            graphics.drawLine(
                    transformX(estimations.x), transformY(estimations.y),
                    transformX(estimations.x + angleLength * cos(estimations.angle * 2 * PI)),
                    transformY(estimations.y + angleLength * sin(estimations.angle * 2 * PI))
            );
        }

        var testRoute = new LinkedList<DesiredPose>();
        if (estimations.x < 0.32) testRoute.add(new DesiredPose(0.3, 0.75, 0.0, false));
        if (estimations.x < 0.62) testRoute.add(new DesiredPose(0.6, 0.75, 0.0, false));
        if (estimations.y > 0.58) testRoute.add(new DesiredPose(0.75, 0.6, 0.75, false));
        testRoute.add(new DesiredPose(0.75, 0.3, 0.75, false));

        var desiredVelocity = new DesiredVelocity();
        var controller = new BezierController(testRoute, desiredVelocity, estimations);

        int oldSize = -1;
        while (testRoute.size() != oldSize) {
            oldSize = testRoute.size();
            controller.update(0.001);
        }

        {
            graphics.setColor(new Color(0, 200, 0));
            double angleLength = 0.04;
            graphics.drawLine(
                    transformX(estimations.x), transformY(estimations.y),
                    transformX(estimations.x + angleLength * cos(desiredVelocity.angle * 2 * PI)),
                    transformY(estimations.y + angleLength * sin(desiredVelocity.angle * 2 * PI))
            );
        }

        for (var route : testRoute) {
            graphics.setColor(new Color(0, 0, 200));
            int radius = 5;
            graphics.fillOval(transformX(route.x) - radius, transformY(route.y) - radius, 2 * radius, 2 * radius);

            double angleLength = 0.03;
            graphics.setColor(Color.BLACK);
            graphics.drawLine(
                    transformX(route.x), transformY(route.y),
                    transformX(route.x + angleLength * cos(2 * PI * route.angle)),
                    transformY(route.y + angleLength * sin(2 * PI * route.angle))
            );
        }

        var curve = controller.getCurve();
        if (curve != null) {
            graphics.setColor(Color.RED);
            int radius = 1;
            for (double t = 0.0; t <= 1.0; t += 0.01) {
                graphics.fillOval(
                        transformX(curve.getX(t)) - radius, transformY(curve.getY(t)) - radius,
                        2 * radius, 2 * radius
                );
            }

            graphics.setColor(Color.MAGENTA);
            radius = 5;
            graphics.fillOval(
                    transformX(curve.x2()) - radius, transformY(curve.y2()) - radius,
                    2 * radius, 2 * radius
            );
            graphics.fillOval(
                    transformX(curve.x3()) - radius, transformY(curve.y3()) - radius,
                    2 * radius, 2 * radius
            );
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private int transformX(double realX) {
        return (int) (getWidth() * realX);
    }

    private int transformY(double realY) {
        return (int) (getHeight() * (1 - realY));
    }

    private double transformReverseX(int screenX) {
        return (double) screenX / getWidth();
    }

    private double transformReverseY(int screenY) {
        return 1 - (double) screenY / getHeight();
    }

    private void handleClick(int x, int y, int button) {
        double realX = transformReverseX(x - insets.get().left);
        double realY = transformReverseY(y - insets.get().top);
        if (button == 1) {
            estimations.x = realX;
            estimations.y = realY;
        } else {
            double dx = realX - estimations.x;
            double dy = realY - estimations.y;
            if (abs(dx) + abs(dy) > 0.01) estimations.angle = atan2(dy, dx) / (2 * PI);
        }
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        handleClick(event.getX(), event.getY(), event.getButton());
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent event) {
        handleClick(event.getX(), event.getY(), event.getModifiersEx() == InputEvent.BUTTON1_DOWN_MASK ? 1 : 0);
    }

    @Override
    public void mouseMoved(MouseEvent e) {}
}
