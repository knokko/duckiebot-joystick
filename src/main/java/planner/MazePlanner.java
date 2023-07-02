package planner;

import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;

import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import static controller.util.DuckieBot.GRID_SIZE;

public class MazePlanner implements ControllerFunction  {
    private final BlockingQueue<GridPosition> highLevelRoute;
    private final DuckieEstimations estimations;
    private int[][] tremauxMap;
    private byte currentX = 0;
    private byte currentY = 0;
    private Cell currentCell;

    class Cell{
        enum WallFlag{
            Up,
            Down,
            Left,
            Right;

            public static final EnumSet<WallFlag> ALL_OPTS = EnumSet.allOf(WallFlag.class);
            public static final EnumSet<WallFlag> NO_OPTS = EnumSet.noneOf(WallFlag.class);
        }

        public int x;
        public int y;
        EnumSet<WallFlag> walls;

        public Cell(int x, int y, int wallValue){
            this.x = x;
            this.y = y;
            this.walls = WallFlag.NO_OPTS;
        }
    }

    enum Mode {
        Explore,
        Race,
      }

    private Mode mode = Mode.Explore;
      
    public MazePlanner(BlockingQueue<GridPosition> highLevelRoute, DuckieEstimations estimations) {
        this.highLevelRoute = highLevelRoute;
        this.estimations = estimations;

        tremauxMap = new int[8][8];
    }

    @Override
    public void update(double deltaTime) {
        var previousX = currentX;
        var previousY = currentY;
        currentX = (byte)Math.floor(estimations.x / GRID_SIZE);
        currentY = (byte)Math.floor(estimations.y / GRID_SIZE);

        if(currentX != previousX || currentY != previousY){
            updateCell();
        }
        switch(mode) {
            case Explore:
                explore();
                break;
            case Race:
            default:
                break;
        }
    }

    void updateCell(){
        currentCell = new Cell(currentX, currentY, 0);

        var walls = estimations.walls.copyWalls();
        for(var wall : walls)
        {
            // Look at the walls of the current grid for bottom and ???
            if(wall.gridX() == currentX && wall.gridY() == currentY)
            {
                switch(wall.axis()){
                    case Y:
                        if(wall.gridX() > currentX)
                        currentCell.walls.add(Cell.WallFlag.Left);
                         System.out.println("Left Wall at " + currentX + ", " + currentY);
                        break;
                    case X:
                        currentCell.walls.add(Cell.WallFlag.Down);
                        System.out.println("Bottom Wall at " + currentX + ", " + currentY);
                        break;
                }
            }
            // Look at the tile above to find the top wall
            else if(wall.gridX() == currentX && wall.gridY() == currentY + 1)
            {
                currentCell.walls.add(Cell.WallFlag.Up);
                System.out.println("Up Wall at " + currentX + ", " + currentY);
            }
            // Look at the tile to the right to find the right wall
            else if(wall.gridX() == currentX + 1 && wall.gridY() == currentY)
            {
                currentCell.walls.add(Cell.WallFlag.Right);
                System.out.println("Right Wall at " + currentX + ", " + currentY);
            }
        }

    }

    public void explore(){
        var walls = estimations.walls.copyWalls();
        for(var wall : walls)
        {
            wall.gridX();
        }


        byte newPointX = (byte)(currentX + 3);
        byte newPointY = currentY;
        highLevelRoute.add(new GridPosition(newPointX, newPointY));

        mode = Mode.Race;
    }
}
