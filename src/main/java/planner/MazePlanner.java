package planner;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import planner.MazePlanner.Cell.WallFlag;

import static controller.util.DuckieBot.GRID_SIZE;
import static planner.GridWall.Axis.*;

public class MazePlanner implements ControllerFunction  {
    private final BlockingQueue<GridPosition> highLevelRoute;
    private final DuckieEstimations estimations;
    private final int MAX_X = 100;
    private final int MAX_Y = 100;
    private final int X_OFFSET = 50;
    private final int Y_OFFSET = 50;
    private Cell[][] cellMap = new Cell[MAX_X][MAX_Y];
    private Cell currentCell;
    private int previousX = 0;
    private int previousY = 0;
    private int currentX = 0;
    private int currentY = 0;
    private int goalX = 0;
    private int goalY = 0;
    private Mode mode = Mode.Start;

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
        public EnumSet<WallFlag> walls;

        public int visitCount = 0;

        public Cell(int x, int y, int wallValue){
            this.x = x;
            this.y = y;
            this.walls = EnumSet.noneOf(WallFlag.class);
        }

        public boolean isJunction(){
        // A junction is a cell with at most one wall
            return walls.size() < 2;
         }
    }

    enum Mode {
        Idle,
        Start,
        Explore,
        Race,
      }
      
    public MazePlanner(BlockingQueue<GridPosition> highLevelRoute, DuckieEstimations estimations) {
        this.highLevelRoute = highLevelRoute;
        this.estimations = estimations;
        this.currentCell = new Cell(X_OFFSET, Y_OFFSET, 0);

        // initialize the cell map
        for(int x = 0; x < MAX_X; x++){
            for(int y = 0; y < MAX_Y; y++){
                cellMap[x][y] = new Cell(x, y, 0);
            }
        }
    }

    @Override
    public void update(double deltaTime) {
        previousX = currentX;
        previousY = currentY;
        currentX = (int)Math.floor(estimations.x / GRID_SIZE) + X_OFFSET;
        currentY = (int)Math.floor(estimations.y / GRID_SIZE) + Y_OFFSET;

        boolean newCell = (currentX != previousX) || (currentY != previousY);
        updateCell();

        switch(mode) {
            case Start:
                // Start by going forward
                goalX = currentX + 1;
                goalY = currentY;
                highLevelRoute.add(createGridPosition(goalX, goalY));

                mode = Mode.Explore;
                break;
            case Explore:
                if(goalX == currentX && goalY == currentY && highLevelRoute.isEmpty()){
                    explore();
                }
                break;
            case Race:
            case Idle:
            default:
                break;
        }
    }

    void updateCell(){
        // Local coordinates
        var localX = currentX - X_OFFSET;
        var localY = currentY - Y_OFFSET;

        var walls = estimations.walls.copyWalls();
        for(var wall : walls)
        {
            // Look at the walls of the current grid for bottom and ???
            if(wall.gridX() == localX && wall.gridY() == localY)
            {
                switch(wall.axis()){
                    case Y:
                        if(!cellMap[currentX][currentY].walls.contains(Cell.WallFlag.Left)){
                            System.out.println("Left Wall at " + localX + ", " + localY);
                            cellMap[currentX][currentY].walls.add(Cell.WallFlag.Left);
                        }
                        break;
                    case X:
                        if(!cellMap[currentX][currentY].walls.contains(Cell.WallFlag.Down)){
                            System.out.println("Bottom Wall at " + localX + ", " + localY);
                            cellMap[currentX][currentY].walls.add(Cell.WallFlag.Down);
                        }
                        break;
                }
            }
            // Look at the tile above to find the top wall
            else if(wall.gridX() == localX && wall.gridY() == localY + 1 && wall.axis() == X)
            {
                if(!cellMap[currentX][currentY].walls.contains(Cell.WallFlag.Up)){
                    System.out.println("Up Wall at " + localX + ", " + localY);
                    cellMap[currentX][currentY].walls.add(Cell.WallFlag.Up);
                }
            }
            // Look at the tile to the right to find the right wall
            else if(wall.gridX() == localX + 1 && wall.gridY() == localY && wall.axis() == Y)
            {
                if(!cellMap[currentX][currentY].walls.contains(Cell.WallFlag.Right)){
                    System.out.println("Right Wall at " + localX + ", " + localY);
                    cellMap[currentX][currentY].walls.add(Cell.WallFlag.Right);
                }
            }
        }

        currentCell = cellMap[currentX][currentY];
    }

    public void explore(){      
        if(currentCell.isJunction()){
            // If the current cell is a junction, mark where we came from
            cellMap[previousX][previousY].visitCount++;
            /////////////////////////////////////////////////////////////////////////////////////////////////////
            //If only the entrance you just came from is marked, pick an arbitrary unmarked entrance, if any.  //
            /////////////////////////////////////////////////////////////////////////////////////////////////////

            // Check the possible directions
            List<Cell.WallFlag> posibleDirections = new ArrayList<Cell.WallFlag>();
            if(!currentCell.walls.contains(Cell.WallFlag.Up) && cellMap[currentX][currentY + 1].visitCount == 0){
                posibleDirections.add(Cell.WallFlag.Up);
            }
            if(!currentCell.walls.contains(Cell.WallFlag.Down) && cellMap[currentX][currentY - 1].visitCount == 0){
                posibleDirections.add(Cell.WallFlag.Down);
            }
            if(!currentCell.walls.contains(Cell.WallFlag.Left) && cellMap[currentX - 1][currentY].visitCount == 0){
                posibleDirections.add(Cell.WallFlag.Left);
            }
            if(!currentCell.walls.contains(Cell.WallFlag.Right) && cellMap[currentX + 1][currentY].visitCount == 0){
                posibleDirections.add(Cell.WallFlag.Right);
            }

            // Pick a random valid one
            Cell.WallFlag  randomDirection = WallFlag.Up; // Up is just a placeholder
            if(posibleDirections.size() == 0){
                // We are stuck, we need to backtrack
                System.out.println("We are stuck, we need to backtrack");
            // Perfer to go straight
            } else if (previousX < currentX && posibleDirections.contains(Cell.WallFlag.Right)){
                randomDirection = Cell.WallFlag.Right;
            } else if (previousX > currentX && posibleDirections.contains(Cell.WallFlag.Left)){
                randomDirection = Cell.WallFlag.Left;
            } else if (previousY < currentY && posibleDirections.contains(Cell.WallFlag.Up)){
                randomDirection = Cell.WallFlag.Up;
            } else if (previousY > currentY && posibleDirections.contains(Cell.WallFlag.Down)){
                randomDirection = Cell.WallFlag.Down;
            } else {
                // Pick a random direction
                Random rand = new Random();
                randomDirection = posibleDirections.get(rand.nextInt(posibleDirections.size()));
            }

            // Calculate the new position
            byte newX = (byte)currentX;
            byte newY = (byte)currentY;
            switch(randomDirection){
                case Up:
                    newY++;
                    break;
                case Down:
                    newY--;
                    break;
                case Left:
                    newX--;
                    break;
                case Right:
                    newX++;
                    break;
            }

            // Mark where we are going to
            cellMap[newX][newY].visitCount++;
            
            // Add  the new route
            goalX = newX;
            goalY = newY;
            highLevelRoute.add(createGridPosition(newX, newY));
        }
    }
    private GridPosition createGridPosition(int x, int y){
        return new GridPosition((byte)(x-X_OFFSET), (byte)(y-Y_OFFSET));
    }
}
