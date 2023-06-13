package controller;

import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import state.DuckieControls;

import java.util.List;

import static java.lang.Math.*;

public class BezierController implements ControllerFunction {

    private final List<DesiredPose> route;
    private final DesiredVelocity desiredVelocity;
    private final DuckieEstimations estimations;
    private final DuckieControls controls;
    private final double maxAcceleration;

    public BezierController(
            List<DesiredPose> route, DesiredVelocity desiredVelocity,
            DuckieEstimations estimations, DuckieControls controls, double maxAcceleration
    ) {
        this.route = route;
        this.desiredVelocity = desiredVelocity;
        this.estimations = estimations;
        this.controls = controls;
        this.maxAcceleration = maxAcceleration;
    }

    @Override
    public void update(double deltaTime) {
        if (route.isEmpty()) {
            desiredVelocity.speed = 0.0;
            desiredVelocity.angle = estimations.angle;
            return;
        }

        double speed = 0.2; // 0.2 worked
        var destinationPose = route.get(0);
        var dx = destinationPose.x - estimations.x;
        var dy = destinationPose.y - estimations.y;
        var distance = sqrt(dx * dx + dy * dy);

        if (distance * 0.5 / speed < 0.3) {
            route.remove(0);
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
                route.remove(0);
                return;
            }
        }

        double x1 = estimations.x;
        double y1 = estimations.y;

        double ownAngleRad = estimations.angle * 2 * Math.PI;
        double x2 = x1 + 0.5 * distance * cos(ownAngleRad);
        double y2 = y1 + 0.5 * distance * sin(ownAngleRad);

        double x4 = destinationPose.x;
        double y4 = destinationPose.y;

        double destAngleRad = destinationPose.angle * 2 * Math.PI;
        double x3 = x4 - 0.5 * distance * cos(destAngleRad);
        double y3 = y4 - 0.5 * distance * sin(destAngleRad);

        double destinationTime = distance / speed;
        double timeStep = 0.5;
        double t = timeStep / destinationTime;

        double desiredDx = 3 * (1.0 - t) * (1.0 - t) * (x2 - x1) + 6 * (1.0 - t) * t * (x3 - x2) + 3 * t * t * (x4 - x3);
        double desiredDy = 3 * (1.0 - t) * (1.0 - t) * (y2 - y1) + 6 * (1.0 - t) * t * (y3 - y2) + 3 * t * t * (y4 - y3);

        desiredVelocity.angle = atan2(desiredDy, desiredDx) / (2 * Math.PI);
        desiredVelocity.speed = speed;
        desiredVelocity.turnTime = timeStep;
    }
}
