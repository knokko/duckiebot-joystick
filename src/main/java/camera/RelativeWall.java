package camera;

import static java.lang.Math.*;

public record RelativeWall(double distance, double angle) {

    public static RelativeWall cartesian(double x, double y) {
        return new RelativeWall(sqrt(x * x + y * y), atan2(y, x) / (2 * PI));
    }
}
