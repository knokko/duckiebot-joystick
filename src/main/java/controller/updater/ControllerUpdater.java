package controller.updater;

import controller.AccelerationLimiter;
import controller.BezierController;
import controller.SpeedPIDController;
import controller.VelocityController;
import controller.estimation.PoseEstimator;
import controller.estimation.SpeedEstimator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

import static java.lang.Thread.sleep;

public class ControllerUpdater {

    private final List<ControllerEntry> controllers = new ArrayList<>();

    public void addController(ControllerFunction controller, int period) {
        controllers.add(new ControllerEntry(controller, period));
    }

    public void start() {
        try {
            long updateCounter = 0;

            long startTime = System.currentTimeMillis();
            while (true) {

                while (true) {
                    long currentTime = System.currentTimeMillis();
                    if (updateCounter < (currentTime - startTime)) break;
                    sleep(1);
                }

                updateCounter += 1;

                long currentTime = System.nanoTime();

                for (var entry : controllers) {
                    if (updateCounter % entry.period == 0) {
                        double deltaTime = (currentTime - entry.lastUpdateTime) / 1_000_000_000.0;
                        entry.controller.update(deltaTime);
                        entry.lastUpdateTime = currentTime;
                    }
                }
            }
        } catch (InterruptedException shouldNotHappen) {
            throw new Error(shouldNotHappen);
        }
    }
}
