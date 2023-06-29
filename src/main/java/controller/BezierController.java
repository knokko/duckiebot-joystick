package controller;

import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;

import java.util.Queue;

import static controller.desired.DesiredPose.*;
import static java.lang.Math.*;

public class BezierController implements ControllerFunction {

    private final Queue<DesiredPose> route;
    private final DesiredVelocity desiredVelocity;
    private final DuckieEstimations estimations;

    private double nextPoseTimer;

    public BezierController(
            Queue<DesiredPose> route, DesiredVelocity desiredVelocity,
            DuckieEstimations estimations
    ) {
        this.route = route;
        this.desiredVelocity = desiredVelocity;
        this.estimations = estimations;
    }

    @Override
    public void update(double deltaTime) {
        if (nextPoseTimer > 0) {
            nextPoseTimer -= deltaTime;
            if (nextPoseTimer <= 0) {
                nextPoseTimer = 0;
                route.poll();
            }
        }

        var destinationPose = route.peek();

        // If the pose is cancelled, we should pick the next pose instead
        while (destinationPose != null) {
            int oldStatus = destinationPose.status.compareAndExchange(STATUS_UNREAD, STATUS_READ);
            if (oldStatus == STATUS_CANCELLED) {
                route.remove();
                destinationPose = route.peek();
            }
            else break;
        }

        if (destinationPose == null) {
            desiredVelocity.speed = 0.0;
            desiredVelocity.angle = estimations.angle;
            return;
        }

        double speed = 0.3;
        if (destinationPose.backward) speed = -speed;

        var dx = destinationPose.x - estimations.x;
        var dy = destinationPose.y - estimations.y;
        var distance = sqrt(dx * dx + dy * dy);

        if (distance < 0.08 && nextPoseTimer == 0.0) {
            nextPoseTimer = abs(distance * 0.8 / speed);
        }

        double x1 = estimations.x;
        double y1 = estimations.y;

        double ownAngleRad = estimations.angle * 2 * Math.PI;
        double x2 = x1 + 0.5 * distance * Math.signum(speed) * cos(ownAngleRad);
        double y2 = y1 + 0.5 * distance * Math.signum(speed) * sin(ownAngleRad);

        double x4 = destinationPose.x;
        double y4 = destinationPose.y;

        double destAngleRad = destinationPose.angle * 2 * Math.PI;
        double x3 = x4 - 0.5 * distance * Math.signum(speed) * cos(destAngleRad);
        double y3 = y4 - 0.5 * distance * Math.signum(speed) * sin(destAngleRad);

        double destinationTime = distance / abs(speed);
        double timeStep = 0.15;
        double t = timeStep / destinationTime;

        double desiredDx = 3 * (1.0 - t) * (1.0 - t) * (x2 - x1) + 6 * (1.0 - t) * t * (x3 - x2) + 3 * t * t * (x4 - x3);
        double desiredDy = 3 * (1.0 - t) * (1.0 - t) * (y2 - y1) + 6 * (1.0 - t) * t * (y3 - y2) + 3 * t * t * (y4 - y3);

        double desiredAngle;
        if (speed >= 0.0) {
            desiredAngle = atan2(desiredDy, desiredDx) / (2 * Math.PI);
        } else {
            desiredAngle = atan2(-desiredDy, -desiredDx) / (2 * Math.PI);
        }

        desiredVelocity.angle = desiredAngle;
        desiredVelocity.speed = speed;
        desiredVelocity.turnTime = timeStep;
    }
}
