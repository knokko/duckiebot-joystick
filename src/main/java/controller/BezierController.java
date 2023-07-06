package controller;

import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import controller.util.BezierCurve;

import java.util.Queue;

import static controller.desired.DesiredPose.*;
import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.lang.Math.*;

public class BezierController implements ControllerFunction {

    private final Queue<DesiredPose> route;
    private final DesiredVelocity desiredVelocity;
    private final DuckieEstimations estimations;

    private BezierCurve curve;

    public BezierController(
            Queue<DesiredPose> route, DesiredVelocity desiredVelocity,
            DuckieEstimations estimations
    ) {
        this.route = route;
        this.desiredVelocity = desiredVelocity;
        this.estimations = estimations;
    }

    public BezierCurve getCurve() {
        return curve;
    }

    @Override
    public void update(double deltaTime) {
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

        if (isNaN(destinationPose.x)) {
            desiredVelocity.speed = NaN;
            desiredVelocity.angle = destinationPose.angle;

            double difference = desiredVelocity.angle - estimations.angle;
            if (difference < -0.5) difference += 1;
            if (difference > 0.5) difference -= 1;
            if (abs(difference) < 0.01) route.remove();
            return;
        }

        double speed = 0.18;
        if (destinationPose.backward) speed = -speed;

        var dx = destinationPose.x - estimations.x;
        var dy = destinationPose.y - estimations.y;
        var distance = sqrt(dx * dx + dy * dy);

        double x1 = estimations.x;
        double y1 = estimations.y;

        double ownAngleRad = estimations.angle * 2 * Math.PI;
        double x2 = x1 + 0.3 * distance * Math.signum(speed) * cos(ownAngleRad);
        double y2 = y1 + 0.3 * distance * Math.signum(speed) * sin(ownAngleRad);

        double x4 = destinationPose.x;
        double y4 = destinationPose.y;

        double destAngleRad = destinationPose.angle * 2 * Math.PI;
        double x3 = x4 - 0.5 * distance * Math.signum(speed) * cos(destAngleRad);
        double y3 = y4 - 0.5 * distance * Math.signum(speed) * sin(destAngleRad);

        this.curve = new BezierCurve(x1, y1, x2, y2, x3, y3, x4, y4);

        double destinationTime = distance / abs(speed);
        double timeStep = 0.35;
        if (route.size() == 1) timeStep = 0.55;
        double t = timeStep / destinationTime;
        if (t >= 1) {
            t = 1;
            route.poll();
        }

        double desiredDx = curve.getX(t) - estimations.x;
        double desiredDy = curve.getY(t) - estimations.y;

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
