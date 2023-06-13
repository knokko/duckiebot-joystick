package controller;

import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import state.DuckieControls;

import java.util.Queue;

import static java.lang.Math.*;

public class RouteController implements ControllerFunction {

    private final Queue<DesiredPose> route;
    private final DesiredVelocity desiredVelocity;
    private final DuckieEstimations estimations;
    private final DuckieControls controls;
    private final double maxAcceleration;

    public RouteController(
            Queue<DesiredPose> route, DesiredVelocity desiredVelocity, DuckieEstimations estimations,
            DuckieControls controls, double maxAcceleration
    ) {
        this.route = route;
        this.desiredVelocity = desiredVelocity;
        this.estimations = estimations;
        this.controls = controls;
        this.maxAcceleration = maxAcceleration;
    }

    @Override
    public void update(double deltaTime) {
        // 0.7 on carpet and 0.8 on table?
        double speed = 0.2;

        var destinationPose = route.peek();
        if (destinationPose == null) {
            desiredVelocity.speed = 0.0;
            desiredVelocity.angle = estimations.angle;
            return;
        }

        var dx = destinationPose.x - estimations.x;
        var dy = destinationPose.y - estimations.y;
        var distance = sqrt(dx * dx + dy * dy);

        if (distance * 0.5 / speed < 0.3) { // TODO Do better than this
            route.remove();
            return;
        }

        // If this is the last entry, we should stop the duckiebot at the finish
        if (route.size() == 1) {
            double currentSpeed = (estimations.leftSpeed + estimations.rightSpeed) * 0.5;
            double breakTime = deltaTime + max(
                    abs(controls.velLeft) / maxAcceleration + estimations.leftSpeedChangeInterval,
                    abs(controls.velRight) / maxAcceleration + estimations.rightSpeedChangeInterval
            );
            double breakDistance = 0.5 * currentSpeed * breakTime;
            if (breakDistance > distance) {
                desiredVelocity.speed = 0;
                route.remove();
                return;
            }
        }

        desiredVelocity.angle = atan2(dy, dx) / (2 * Math.PI);
        desiredVelocity.speed = speed;
        desiredVelocity.turnTime = (distance * 0.4) / desiredVelocity.speed;
    }
}
