package controller.estimation;

import state.DuckieControls;

public class SpeedFunctionEstimator {

    private final DuckieControls controls;
    private final DuckieEstimations estimations;

    public SpeedFunctionEstimator(DuckieControls controls, DuckieEstimations estimations) {
        this.controls = controls;
        this.estimations = estimations;
    }

    public void update() {

    }
}
