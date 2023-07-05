package simulator;

import planner.GridWall;

public class SimulatorMaze {

    public static WallGrid createTestingWallGrid8x8() {
        var grid = new WallGrid();

        // Y = (-)4
        for (int x = -4; x < 4; x++) {
            grid.add(new GridWall(x, -4, GridWall.Axis.X));
            grid.add(new GridWall(x, 4, GridWall.Axis.X));
        }

        // X = (-)4
        for (int y = -4; y < 4; y++) {
            grid.add(new GridWall(-4, y, GridWall.Axis.Y));
            grid.add(new GridWall(4, y, GridWall.Axis.Y));
        }

        // Y = 3.5
        grid.add(new GridWall(0, 3, GridWall.Axis.Y));
        grid.add(new GridWall(4, 3, GridWall.Axis.Y));

        // Y = 3
        grid.add(new GridWall(-3, 3, GridWall.Axis.X));
        grid.add(new GridWall(-2, 3, GridWall.Axis.X));
        grid.add(new GridWall(1, 3, GridWall.Axis.X));
        grid.add(new GridWall(2, 3, GridWall.Axis.X));

        // Y = 2.5
        grid.add(new GridWall(-1, 2, GridWall.Axis.Y));
        grid.add(new GridWall(3, 2, GridWall.Axis.Y));

        // Y = 2
        grid.add(new GridWall(-4, 2, GridWall.Axis.X));
        grid.add(new GridWall(-3, 2, GridWall.Axis.X));
        grid.add(new GridWall(-1, 2, GridWall.Axis.X));
        grid.add(new GridWall(0, 2, GridWall.Axis.X));
        grid.add(new GridWall(1, 2, GridWall.Axis.X));

        // Y = 1.5
        grid.add(new GridWall(-2, 1, GridWall.Axis.Y));
        grid.add(new GridWall(2, 1, GridWall.Axis.Y));
        grid.add(new GridWall(3, 1, GridWall.Axis.Y));

        // Y = 1
        grid.add(new GridWall(-2, 1, GridWall.Axis.X));
        grid.add(new GridWall(0, 1, GridWall.Axis.X));
        grid.add(new GridWall(2, 1, GridWall.Axis.X));

        // Y = 0.5
        grid.add(new GridWall(-3, 0, GridWall.Axis.Y));
        grid.add(new GridWall(-1, 0, GridWall.Axis.Y));

        // Y = 0
        grid.add(new GridWall(-2, 0, GridWall.Axis.X));
        grid.add(new GridWall(0, 0, GridWall.Axis.X));
        grid.add(new GridWall(1, 0, GridWall.Axis.X));

        // Y = -0.5
        grid.add(new GridWall(-2, -1, GridWall.Axis.Y));
        grid.add(new GridWall(1, -1, GridWall.Axis.Y));
        grid.add(new GridWall(3, -1, GridWall.Axis.Y));

        // Y = -1
        grid.add(new GridWall(-1, -1, GridWall.Axis.X));
        grid.add(new GridWall(0, -1, GridWall.Axis.X));

        // Y = -1.5
        for (int x = -3; x < 0; x++) grid.add(new GridWall(x, -2, GridWall.Axis.Y));
        grid.add(new GridWall(2, -2, GridWall.Axis.Y));
        grid.add(new GridWall(3, -2, GridWall.Axis.Y));

        // Y = -2
        grid.add(new GridWall(0, -2, GridWall.Axis.X));
        grid.add(new GridWall(1, -2, GridWall.Axis.X));

        // Y = -2.5
        for (int x = -3; x <= 0; x++) grid.add(new GridWall(x, -3, GridWall.Axis.Y));

        // Y = -3
        grid.add(new GridWall(-3, -3, GridWall.Axis.X));
        grid.add(new GridWall(2, -3, GridWall.Axis.X));
        grid.add(new GridWall(3, -3, GridWall.Axis.X));

        // Y = -3.5
        grid.add(new GridWall(1, -4, GridWall.Axis.Y));

        return grid;
    }

    public static WallGrid createTestingWallGrid5x5() {
        var grid = new WallGrid();

        // Y = (-)3
        for (int x = -2; x < 3; x++) {
            grid.add(new GridWall(x, -2, GridWall.Axis.X));
            grid.add(new GridWall(x, 3, GridWall.Axis.X));
        }

        // X = (-)3
        for (int y = -2; y < 3; y++) {
            grid.add(new GridWall(-2, y, GridWall.Axis.Y));
            grid.add(new GridWall(3, y, GridWall.Axis.Y));
        }

        // Y = 2.5
        grid.add(new GridWall(1, 2, GridWall.Axis.Y));

        // Y = 2
        grid.add(new GridWall(-1, 2, GridWall.Axis.X));
        grid.add(new GridWall(0, 2, GridWall.Axis.X));

        // Y = 1.5
        grid.add(new GridWall(2, 1, GridWall.Axis.Y));

        // Y = 1
        grid.add(new GridWall(-2, 1, GridWall.Axis.X));
        grid.add(new GridWall(0, 1, GridWall.Axis.X));
        grid.add(new GridWall(1, 1, GridWall.Axis.X));

        // Y = 0.5
        grid.add(new GridWall(-1, 0, GridWall.Axis.Y));

        // Y = 0
        grid.add(new GridWall(0, 0, GridWall.Axis.X));

        // Y = -0.5
        grid.add(new GridWall(0, -1, GridWall.Axis.Y));
        grid.add(new GridWall(2, -1, GridWall.Axis.Y));

        // Y = -1
        grid.add(new GridWall(-1, -1, GridWall.Axis.X));
        grid.add(new GridWall(1, -1, GridWall.Axis.X));

        return grid;
    }
}
