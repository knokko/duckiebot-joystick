package camera;

import java.util.Collection;

public record CameraWalls(long timestamp, Collection<RelativeWall> walls) {
}
