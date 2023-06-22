package controller;

import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import state.DuckieControls;

import java.util.List;

import static java.lang.Math.*;

public class StepController implements ControllerFunction {

    private final List<DesiredPose> route;
    private final DesiredVelocity desiredVelocity;
    private final DuckieEstimations estimations;
    private final DuckieControls controls;
    private final double maxAcceleration;

    public StepController(
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

        double speed = 0.5; // 0.2 worked
        
        // Distance to next²
        var destinationPose     = route.get(0);
        var nextDestinationPose = destinationPose;
        if(route.size() > 1)
        {
            nextDestinationPose = route.get(1);
        }
        var dx = nextDestinationPose.x - estimations.x;
        var dy = nextDestinationPose.y - estimations.y;
        var distanceToNext2 = sqrt(dx * dx + dy * dy);

        dx = nextDestinationPose.x - destinationPose.x;
        dy = nextDestinationPose.y - destinationPose.y;
        var distanceBetweenNext = sqrt(dx * dx + dy * dy);
        
        // Distance to next
        dx = destinationPose.x - estimations.x;
        dy = destinationPose.y - estimations.y;
        var distanceToNext = sqrt(dx * dx + dy * dy);

        // If the next² point is closer than the next point, remove the next point
        if ((distanceToNext2 < distanceBetweenNext && distanceToNext < 0.05) || distanceToNext < 0.02) {
            route.remove(0);
            return;
        }

        // If this is the last entry, we should stop the duckiebot at the finish
        if (route.size() == 1) {
            double currentSpeed = (estimations.leftSpeed + estimations.rightSpeed) * 0.5;
            double breakTime = deltaTime + max(
                    abs(controls.velLeft) / maxAcceleration + estimations.leftControlLatency,
                    abs(controls.velRight) / maxAcceleration + estimations.rightControlLatency
            );
            double breakDistance = 0.5 * currentSpeed * breakTime;
            if (breakDistance > distanceToNext) {
                desiredVelocity.speed = 0;
                route.remove(0);
                return;
            }
        }

        desiredVelocity.angle = atan2(dy, dx) / (2 * Math.PI);
        desiredVelocity.speed = speed;
        desiredVelocity.turnTime = 0;
    }
}
