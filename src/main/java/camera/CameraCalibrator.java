package camera;

import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.Vector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import static controller.util.DuckieBot.GRID_SIZE;
import static java.lang.Math.*;

public class CameraCalibrator extends JPanel implements KeyListener, MouseListener, WindowListener {

    private static final File dataFile = new File("calibration.bin");

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        var calibrator = new CameraCalibrator(frame::getInsets, Toolkit.getDefaultToolkit().getImage("camera.jpeg"));

        try (var input = new DataInputStream(Files.newInputStream(dataFile.toPath()))) {
            int numGridPoints = input.readInt();
            for (int counter = 0; counter < numGridPoints; counter++) {
                calibrator.gridPoints.add(new Point(input.readInt(), input.readInt()));
            }
            int numCameraPoints = input.readInt();
            for (int counter = 0; counter < numCameraPoints; counter++) {
                calibrator.cameraPoints.add(new Point(input.readInt(), input.readInt()));
            }
        } catch (IOException noData) {
            System.out.println("Can't find data file");
        }

        frame.setSize(1280, 510);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addMouseListener(calibrator);
        frame.addWindowListener(calibrator);
        frame.addKeyListener(calibrator);
        frame.add(calibrator);

        frame.setVisible(true);
    }

    private final Supplier<Insets> getInsets;
    private final Image cameraImage;
    private final int gridSize = 80;

    private final java.util.List<Point> gridPoints = new ArrayList<>(), cameraPoints = new ArrayList<>();

    private BiPoly distancePoly, anglePoly;
    private double estimatedX, estimatedY;

    CameraCalibrator(Supplier<Insets> getInsets, Image cameraImage) {
        this.getInsets = getInsets;
        this.cameraImage = cameraImage;
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);

        boolean shouldRetry = !graphics.drawImage(cameraImage, 0, 0, 640, 480, null);

        graphics.setColor(Color.WHITE);
        graphics.fillRect(640, 0, 640, 480);
        graphics.setColor(Color.BLACK);
        for (int y = 0; y < 480; y += gridSize) {
            graphics.drawLine(640, y, 1280, y);
        }
        for (int x = 960 - gridSize / 2; x >= 640; x -= gridSize) {
            graphics.drawLine(x, 0, x, 480);
        }
        for (int x = 960 + gridSize / 2; x < 1280; x += gridSize) {
            graphics.drawLine(x, 0, x, 480);
        }
        graphics.setColor(Color.YELLOW);
        graphics.fillRect(960 - gridSize / 2, 480 - gridSize, gridSize, gridSize);

        int radius = 3;
        graphics.setColor(Color.RED);
        for (Point point : cameraPoints) {
            graphics.fillOval(point.x - radius, point.y - radius, 2 * radius, 2 * radius);
        }
        graphics.setColor(Color.BLUE);
        for (Point point : gridPoints) {
            graphics.fillOval(point.x + 640 - radius, point.y - radius, 2 * radius, 2 * radius);
        }
        if (estimatedX != 0 || estimatedY != 0) {
            graphics.setColor(Color.GREEN);
            int x = 960 + (int) (-estimatedY * gridSize / GRID_SIZE);
            int y = 480 - gridSize - (int) (estimatedX * gridSize / GRID_SIZE);
            graphics.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);
        }

        Toolkit.getDefaultToolkit().sync();
        if (shouldRetry) {
            System.out.println("retry");
            this.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == 1) {
            var insets = getInsets.get();
            int x = mouseEvent.getX() - insets.left;
            int y = mouseEvent.getY() - insets.top;

            if (distancePoly != null && anglePoly != null) {
                if (x < 640) {
                    double distance = distancePoly.get(x, y);
                    double angle = anglePoly.get(x, y);
                    estimatedX = distance * cos(toRadians(angle));
                    estimatedY = distance * sin(toRadians(angle));
                }
                this.repaint();
                return;
            }

            if (gridPoints.size() > cameraPoints.size()) {
                if (x < 640) {
                    cameraPoints.add(new Point(x, y));
                }
            } else if (x > 640) {
                gridPoints.add(new Point(x - 640, y));
            }
        } else if (!gridPoints.isEmpty()){
            if (gridPoints.size() > cameraPoints.size()) gridPoints.remove(gridPoints.size() - 1);
            else cameraPoints.remove(cameraPoints.size() - 1);
        }
        this.repaint();
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {}

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {}

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {}

    @Override
    public void mouseExited(MouseEvent mouseEvent) {}

    @Override
    public void windowOpened(WindowEvent windowEvent) {}

    @Override
    public void windowClosing(WindowEvent windowEvent) {}

    @Override
    public void windowClosed(WindowEvent windowEvent) {
        try (var output = new DataOutputStream(Files.newOutputStream(dataFile.toPath()))) {
            output.writeInt(gridPoints.size());
            for (Point point : gridPoints) {
                output.writeInt(point.x);
                output.writeInt(point.y);
            }
            output.writeInt(cameraPoints.size());
            for (Point point : cameraPoints) {
                output.writeInt(point.x);
                output.writeInt(point.y);
            }
        } catch (IOException uhOoh) {
            uhOoh.printStackTrace();
        }

        try (var textOutput = new PrintWriter(Files.newOutputStream(new File("data.txt").toPath()))) {
            for (int index = 0; index < cameraPoints.size(); index++) {
                textOutput.printf("(%d, %d) -> (%.3f, %.3f)\n", cameraPoints.get(index).x, cameraPoints.get(index).y, getRelativeY(gridPoints.get(index).y), getRelativeX(gridPoints.get(index).x));
            }
        } catch (IOException failedToSaveText) {
            failedToSaveText.printStackTrace();
        }
    }

    @Override
    public void windowIconified(WindowEvent windowEvent) {}

    @Override
    public void windowDeiconified(WindowEvent windowEvent) {}

    @Override
    public void windowActivated(WindowEvent windowEvent) {}

    @Override
    public void windowDeactivated(WindowEvent windowEvent) {}

    @Override
    public void keyTyped(KeyEvent keyEvent) {}

    private double getRelativeX(int x) {
        return GRID_SIZE * (x - 320) / gridSize;
    }

    private double getRelativeY(int y) {
        return GRID_SIZE * (480 - gridSize - y) / gridSize;
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.VK_T) {
            var distanceEntries = new ArrayList<BiPolyEntry>();
            var angleEntries = new ArrayList<BiPolyEntry>();
            for (int index = 0; index < cameraPoints.size(); index++) {
                var gridPoint = gridPoints.get(index);
                double relativeX = getRelativeX(gridPoint.x);
                double relativeY = getRelativeY(gridPoint.y);
                if (gridPoint.y > 380) System.out.println("relativeX is " + relativeX + " and relativeY is " + relativeY);
                var cameraPoint = cameraPoints.get(index);

                distanceEntries.add(new BiPolyEntry(cameraPoint.x, cameraPoint.y, sqrt(relativeX * relativeX + relativeY * relativeY)));
                double angle = toDegrees(atan2(relativeY, relativeX)) - 90;
                if (angle < -180) angle += 360;
                if (angle > 180) angle -= 360;
                angleEntries.add(new BiPolyEntry(cameraPoint.x, cameraPoint.y, angle));
            }

            distancePoly = BiPoly.fit(distanceEntries, 4);
            anglePoly = BiPoly.fit(angleEntries, 2);
            if (distancePoly != null && anglePoly != null) {
            } else {
                System.out.println("Not enough points");
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {}

    record BiPoly(double offset, double[] coefficientsX, double[] coefficientsY) {

        static BiPoly fit(Collection<BiPolyEntry> entries, int degree) {
            Matrix inputMatrix = Matrix.zero(entries.size(), 2 * degree + 1);
            Vector outputVector = Vector.zero(entries.size());
            int row = 0;
            for (var entry : entries) {
                double factorX = 1.0;
                double factorY = 1.0;
                for (int column = 0; column <= degree; column++) {
                    inputMatrix.set(row, column, factorX);
                    if (column > 0) inputMatrix.set(row, column + degree, factorY);
                    factorX *= transformX(entry.x);
                    factorY *= transformY(entry.y);
                }
                outputVector.set(row, entry.value);
                row += 1;
            }

            Matrix transposedInput = inputMatrix.transpose();
            try {
                Vector coefficients = transposedInput.multiply(inputMatrix).withInverter(LinearAlgebra.InverterFactory.NO_PIVOT_GAUSS)
                        .inverse().multiply(transposedInput).multiply(outputVector);
                double offset = coefficients.get(0);
                double[] coefficientsX = new double[degree];
                double[] coefficientsY = new double[degree];
                for (int index = 0; index < degree; index++) {
                    coefficientsX[index] = coefficients.get(1 + index);
                    coefficientsY[index] = coefficients.get(1 + index + degree);
                }
                return new BiPoly(offset, coefficientsX, coefficientsY);
            } catch (IllegalArgumentException notInvertible) {
                System.out.println("not invertible:");
                System.out.println(inputMatrix);
                System.out.println("with vectors");
                System.out.println(outputVector);
                return null;
            }
        }

        double get(int x, int y) {
            double result = offset;
            double factor = transformX(x);
            for (double coefficient : coefficientsX) {
                result += coefficient * factor;
                factor *= transformX(x);
            }
            factor = transformY(y);
            for (double coefficient : coefficientsY) {
                result += coefficient * factor;
                factor *= transformY(y);
            }
            return result;
        }

        @Override
        public String toString() {
            var result = new StringBuilder(String.format("R = %.3f", offset));
            for (int index = 0; index < coefficientsX.length; index++) {
                double coefficient = coefficientsX[index];
                result.append(String.format(" + %.3fX^%d", coefficient, index + 1));
            }
            for (int index = 0; index < coefficientsY.length; index++) {
                double coefficient = coefficientsY[index];
                result.append(String.format(" + %.3fY^%d", coefficient, index + 1));
            }
            return result.toString();
        }
    }

    record BiPolyEntry(int x, int y, double value) {}

    private static double transformX(int x) {
        return x / 640.0;
    }

    private static double transformY(int y) {
        return y / 480.0;
    }
}
