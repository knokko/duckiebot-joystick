package controller.util;

public record BezierCurve(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {

    public double getX(double t) {
        return (1 - t) * (1 - t) * (1 - t) * x1 + 3 * (1 - t) * (1 - t) * t * x2 + 3 * (1 - t) * t * t * x3 + t * t * t * x4;
    }

    public double getY(double t) {
        return (1 - t) * (1 - t) * (1 - t) * y1 + 3 * (1 - t) * (1 - t) * t * y2 + 3 * (1 - t) * t * t * y3 + t * t * t * y4;
    }

    public double getDerivativeX(double t) {
        return 3 * (1.0 - t) * (1.0 - t) * (x2 - x1) + 6 * (1.0 - t) * t * (x3 - x2) + 3 * t * t * (x4 - x3);
    }

    public double getDerivativeY(double t) {
        return 3 * (1.0 - t) * (1.0 - t) * (y2 - y1) + 6 * (1.0 - t) * t * (y3 - y2) + 3 * t * t * (y4 - y3);
    }
}
