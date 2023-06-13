package controller.updater;

class ControllerEntry {

    final ControllerFunction controller;
    final int period;
    long lastUpdateTime = System.nanoTime();

    ControllerEntry(ControllerFunction controller, int period) {
        this.controller = controller;
        this.period = period;
    }
}
