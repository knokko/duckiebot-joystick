package planner;

import controller.desired.DesiredPose;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import static controller.desired.DesiredPose.STATUS_CANCELLED;
import static controller.desired.DesiredPose.STATUS_UNREAD;

public class RoutePlanner {

    public static int simpleSin(double angle) {
        if (angle == 0.0 || angle == 0.5) return 0;
        if (angle == 0.25) return 1;
        if (angle == 0.75) return -1;
        throw new IllegalArgumentException("Unexpected angle: " + angle);
    }

    public static int simpleCos(double angle) {
        if (angle == 0.0) return 1;
        if (angle == 0.25 || angle == 0.75) return 0;
        if (angle == 0.5) return -1;
        throw new IllegalArgumentException("Unexpected angle: " + angle);
    }

    private static double centerX(GridPose pose) {
        return 0.2 * pose.x + 0.1;
    }

    private static double centerY(GridPose pose) {
        return 0.2 * pose.y + 0.1;
    }

    private final BlockingQueue<GridPosition> highLevelRoute;
    private final Queue<DesiredPose> lowLevelRoute;

    private GridPose currentGridPose = new GridPose(0, 0, 0.0), previousGridPose;
    private DesiredPose currentPose = new DesiredPose(0.16, 0.1, 0.0, false), previousPose;

    public RoutePlanner(BlockingQueue<GridPosition> highLevelRoute, Queue<DesiredPose> lowLevelRoute) {
        this.highLevelRoute = highLevelRoute;
        this.lowLevelRoute = lowLevelRoute;
    }

    private void addLowLevel(DesiredPose newPose) {
        this.previousPose = currentPose;
        this.currentPose = newPose;
        this.lowLevelRoute.add(newPose);
    }

    private void addLowLevel(int x, int y, double angle, boolean backward) {
        if (backward) {
            angle += 0.5;
            if (angle >= 1.0) angle -= 1.0;
        }
        double offset = backward ? -0.01 : 0.06;
        addLowLevel(new DesiredPose(
                (0.2 * x + 0.1) + offset * simpleCos(angle),
                (0.2 * y + 0.1) + offset * simpleSin(angle),
                angle,
                backward
        ));
    }

    private void processSingleGridStep(double desiredAngle) {
        double deltaAngle = desiredAngle - currentGridPose.angle;
        if (deltaAngle < 0.0) deltaAngle += 1.0;

        GridPose newPose;

        if (deltaAngle == 0.0) {
            newPose = new GridPose(
                    currentGridPose.x + simpleCos(currentGridPose.angle),
                    currentGridPose.y + simpleSin(currentGridPose.angle),
                    currentGridPose.angle
            );
            addLowLevel(newPose.x, newPose.y, currentGridPose.angle, false);
        } else if (deltaAngle == 0.25 || deltaAngle == 0.75) {
            if (previousPose == null) {
                System.err.println("You can't make a turn from the starting pose");
                return;
            }

            if (!currentPose.status.compareAndSet(STATUS_UNREAD, STATUS_CANCELLED)) {
                // If this code is reached, it's too late to turn, so the duckiebot should drive back first
                double backX = centerX(previousGridPose);
                double backY = centerY(previousGridPose);

                if (currentPose.backward) {
                    backX += 0.03 * simpleCos(currentGridPose.angle);
                    backY += 0.03 * simpleSin(currentGridPose.angle);
                } else {
                    backX += 0.06 * simpleCos(currentGridPose.angle);
                    backY += 0.06 * simpleSin(currentGridPose.angle);
                }

                lowLevelRoute.add(new DesiredPose(backX, backY, currentPose.angle, !currentPose.backward));
            }

            double frontX = centerX(previousGridPose);
            double frontY = centerY(previousGridPose);
            if (currentPose.backward) {
                frontX -= 0.1 * simpleCos(currentGridPose.angle);
                frontY -= 0.1 * simpleSin(currentGridPose.angle);
            } else {
                frontX += 0.1 * simpleCos(currentGridPose.angle);
                frontY += 0.1 * simpleSin(currentGridPose.angle);
            }

            lowLevelRoute.add(new DesiredPose(frontX, frontY, currentPose.angle, currentPose.backward));

            double finalAngle = desiredAngle;
            if (currentPose.backward) {
                finalAngle += 0.5;
                if (finalAngle >= 1.0) finalAngle -= 1.0;
            }

            newPose = new GridPose(
                    currentGridPose.x + simpleCos(desiredAngle),
                    currentGridPose.y + simpleSin(desiredAngle),
                    finalAngle
            );

            lowLevelRoute.add(new DesiredPose(
                    centerX(newPose) - 0.1 * simpleCos(desiredAngle),
                    centerY(newPose) - 0.1 * simpleSin(desiredAngle),
                    finalAngle, currentPose.backward
            ));
            addLowLevel(newPose.x, newPose.y, desiredAngle, currentPose.backward);
        } else if (deltaAngle == 0.5) {
            newPose = new GridPose(
                    currentGridPose.x - simpleCos(currentGridPose.angle),
                    currentGridPose.y - simpleSin(currentGridPose.angle),
                    currentGridPose.angle
            );
            addLowLevel(new DesiredPose(
                    centerX(newPose) - 0.01 * simpleCos(currentGridPose.angle),
                    centerY(newPose) - 0.01 * simpleSin(currentGridPose.angle),
                    currentGridPose.angle,
                    true
            ));
        } else throw new IllegalStateException("Unexpected deltaAngle: " + deltaAngle);

        previousGridPose = currentGridPose;
        currentGridPose = newPose;
    }

    public void start() {
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                var nextPosition = highLevelRoute.take();
                int dx = nextPosition.x() - currentGridPose.x;
                int dy = nextPosition.y() - currentGridPose.y;
                if (dx == 0 && dy == 0) System.err.println("Useless command");
                if (dx != 0 && dy != 0) System.err.println("Ambiguous command");
                while (dx > 0) {
                    processSingleGridStep(0.0);
                    dx--;
                }
                while (dx < 0) {
                    processSingleGridStep(0.5);
                    dx++;
                }
                while (dy > 0) {
                    processSingleGridStep(0.25);
                    dy--;
                }
                while (dy < 0) {
                    processSingleGridStep(0.75);
                    dy++;
                }
            }
        } catch (InterruptedException shouldNotHappen) {
            throw new RuntimeException(shouldNotHappen);
        }
    }

    private record GridPose(int x, int y, double angle) {

        @Override
        public String toString() {
            return String.format("GridPose(%d, %d, %.2f)", x, y, angle);
        }
    }
}
