package controller.updater;

import java.util.ArrayList;
import java.util.List;

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
            //noinspection InfiniteLoopStatement
            while (true) {

                while (true) {
                    long currentTime = System.currentTimeMillis();
                    if (updateCounter < (currentTime - startTime)) break;
                    //noinspection BusyWait
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
