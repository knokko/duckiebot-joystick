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

    // The real grid position
    private int realX = 0;
    private int realY = 0;

    // The previous real grid position
    private int prevRealX = 0;
    private int prevRealY = 0;

    // The current grid position, used for planning. (Current as in, plan from this position)
    private int currentX = 0;   
    private int currentY = 0; 
    
    //  The previous grid position, used for planning. (Previous as in, we planned previously from this position)
    private int previousX = 0;  
    private int previousY = 0;

    // The last grid position we crossed a T-junction (used for U-turning)
    private Cell lastTCrossing;

    // Indiana Jones mode
    private boolean backTracking = false;

    // The goal grid position
    private int goalX = 0;
    private int goalY = 0;

    // The ratio we need to be in the new cell to consider it visited
    private double visitedCellRatio = 0.25;

    // Indicates if the are in a new cell
    private boolean newCell = false;

    private Mode mode = Mode.Start;
    private boolean planAhead = false; // Plan ahead flag, used when approaching a wall straight ahead.


    private WallFlag lastDirection = WallFlag.Right;

    public class Cell{
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

        // Grid x
        public int gridX(){
            return x-X_OFFSET;
        }

        public int gridY(){
            return y-Y_OFFSET;
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
        UTurn,
        DriveBack,
      }
      
    public MazePlanner(BlockingQueue<GridPosition> highLevelRoute, DuckieEstimations estimations) {
        this.highLevelRoute = highLevelRoute;
        this.estimations = estimations;
        this.currentCell = new Cell(X_OFFSET, Y_OFFSET, 0);
        estimations.cells = this.cellMap;

        // initialize the cell map
        for(int x = 0; x < MAX_X; x++){
            for(int y = 0; y < MAX_Y; y++){
                cellMap[x][y] = new Cell(x, y, 0);
            }
        }
    }

    @Override
    public void update(double deltaTime) {
        // Get the position
        prevRealX = realX;
        prevRealY = realY;
        realX = (int)Math.floor(estimations.x / GRID_SIZE) + X_OFFSET;
        realY = (int)Math.floor(estimations.y / GRID_SIZE) + Y_OFFSET;

        if(planAhead){
            previousX = realX;
            previousY = realY;
            currentX = goalX;
            currentY = goalY;

            // Planning ahead
            System.out.println("Planning ahead");
            if(realX == goalX && realX == goalY){
                planAhead = false;
            }
        }
        else{
            previousX = currentX;
            previousY = currentY;
            currentX = realX;
            currentY = realY;
        }
        currentCell = cellMap[currentX][currentY];
            
        // Only count new cells we are a certain percentage in

        if((prevRealX != realX) || (prevRealY != realY)){
            newCell = true;
        }
        updateWalls();

        switch(mode) {
            case Start:
                // Start by going forward
                goalX = currentX + 1;
                goalY = currentY;
                highLevelRoute.add(createGridPosition(goalX, goalY));

                mode = Mode.Explore;
                break;
            case Explore:
                // If we are in a new cell, mark it only if we are certain percentage in the cell (1.0 = 100% in the center, 0% is from the edge)
                var inRatioX = Math.abs((Math.abs(estimations.x + GRID_SIZE*0.5) % GRID_SIZE)/GRID_SIZE - 0.5) * 2;
                var inRatioY = Math.abs((Math.abs(estimations.y + GRID_SIZE*0.5) % GRID_SIZE)/GRID_SIZE - 0.5) * 2;
                // Also mark when planning ahead
                if(newCell && ((inRatioX > visitedCellRatio) && (inRatioY > visitedCellRatio) || planAhead)){
                    System.out.println("New cell at " + realX + ", " + realY);
                    cellMap[realX][realY].visitCount++;
                    newCell = false;
                }

                // Mark T-crossings we have passed forward
                if(cellMap[realX][realY].walls.size() == 1 && estimations.leftSpeed > 0 && estimations.rightSpeed > 0){
                    // Only update if we are finding a new one
                    if(lastTCrossing == null || lastTCrossing.x != realX || lastTCrossing.y != realY){
                        System.out.println("T-crossing at " + realX + ", " + realY);
                    }
                    lastTCrossing = cellMap[realX][realY];
                }

                // Only explore if we reached our goal
                if(goalX == currentX && goalY == currentY && highLevelRoute.isEmpty()){
                    explore();
                    // Check if we need to plan ahead
                    planAhead = cellMap[goalX][goalY].walls.contains(lastDirection) && cellMap[goalX][goalY].walls.size() < 3;
                } else if(planAhead){
                    // Keep updating incase we find new walls
                    planAhead = cellMap[goalX][goalY].walls.contains(lastDirection);
                }

                // If we are at a T-crossing, turn around
                if(lastTCrossing != null && cellMap[goalX][goalY].x == lastTCrossing.x && cellMap[goalX][goalY].y == lastTCrossing.y){
                    System.out.println("Time for a U-turn");
                    mode = Mode.UTurn;
                }
                break;
            case UTurn:
                if(backTracking){
                    // We need to track where we came from
                    goalX = currentX;
                    goalY = currentY;

                    // Get the alternative directions we need to go
                    var possibleDirections = EnumSet.complementOf(lastTCrossing.walls.clone());
                    // Remove the inverse of the last direction
                    switch(lastDirection){
                        case Up:
                            possibleDirections.remove(WallFlag.Down);
                            break;
                        case Down:
                            possibleDirections.remove(WallFlag.Up);
                            break;
                        case Left:
                            possibleDirections.remove(WallFlag.Right);
                            break;
                        case Right:
                            possibleDirections.remove(WallFlag.Left);
                            break;
                    }
                    
                    // Pick a random direction
                    List<Cell.WallFlag> dirList = new ArrayList<Cell.WallFlag>();
                    for (WallFlag flag : possibleDirections) {
                        dirList.add(flag);
                    }
                    Random rand = new Random();
                    var pick = rand.nextInt(possibleDirections.size());
                    var direction1 = dirList.get(pick);
                    var direction2 = dirList.get((pick+1)%possibleDirections.size());
                    
                    // Dir1
                    var dir1XY = xyFromDirection(lastTCrossing.x, lastTCrossing.y, direction1);
                    highLevelRoute.add(createGridPosition(dir1XY[0], dir1XY[1]));
                    
                    // T-Cross
                    highLevelRoute.add(createGridPosition(lastTCrossing.x, lastTCrossing.y));
                    
                    // Dir2
                    var dir2XY = xyFromDirection(lastTCrossing.x, lastTCrossing.y, direction2);
                    highLevelRoute.add(createGridPosition(dir2XY[0], dir2XY[1]));
                    
                    // T-Cross
                    highLevelRoute.add(createGridPosition(lastTCrossing.x, lastTCrossing.y));

                    // Original
                    highLevelRoute.add(createGridPosition(currentX, currentY));

                    // Set the mode
                    mode = Mode.DriveBack;
                }
                case DriveBack:
                    if(backTracking){
                        // Wait until we are at the crossing
                        if(realX == lastTCrossing.x && realY == lastTCrossing.y){
                            System.out.println("Primed at " + realX + ", " + realY);
                            backTracking = false;
                        }
                    }
                    else{
                        // Wait until we are back where we came from
                        if(realX == goalX && realY == goalY){
                            mode = Mode.Explore;
                        }
                    }
                case Race:
                case Idle:
                default:
                break;
        }
    }

    void updateWalls(){
        var walls = estimations.walls.copyWalls();
        for(var wall : walls)
        {
            // Local coordinates
            var localX = wall.gridX() + X_OFFSET;
            var localY = wall.gridY() + Y_OFFSET;

            // Position the walls in the grid
            switch(wall.axis()){
                case Y:
                    // If we detect a left wall at the current cell
                    if(!cellMap[localX][localY].walls.contains(Cell.WallFlag.Left)){
                        System.out.println("Left Wall at " + wall.gridX() + ", " + wall.gridY());
                        cellMap[localX][localY].walls.add(Cell.WallFlag.Left);
                    }
                    // The we have a right wall at the cell to the left
                    if(!cellMap[localX - 1][localY].walls.contains(Cell.WallFlag.Right)){
                        System.out.println("Right Wall at " + (wall.gridX() - 1) + ", " + wall.gridY());
                        cellMap[localX - 1][localY].walls.add(Cell.WallFlag.Right);
                    }
                    break;
                case X:
                    // If we detect a bottom wall at the current cell
                    if(!cellMap[localX][localY].walls.contains(Cell.WallFlag.Down)){
                        System.out.println("Bottom Wall at " + wall.gridX() + ", " + wall.gridY());
                        cellMap[localX][localY].walls.add(Cell.WallFlag.Down);
                    }
                    // The we have a top wall at the cell below
                    if(!cellMap[localX][localY - 1].walls.contains(Cell.WallFlag.Up)){
                        System.out.println("Up Wall at " +  wall.gridX()+ ", " +  (wall.gridY() - 1));
                        cellMap[localX][localY - 1].walls.add(Cell.WallFlag.Up);
                    }
                    break;
            }
        }
    }

    public void explore(){        
        Cell.WallFlag newDirection = WallFlag.Up;  // Up is just a placeholder

        // If the current cell is a junction
        if(currentCell.isJunction()){
            System.out.println("Junction");
            /////////////////////////////////////////////////////////////////////////////////////////////////////
            //If only the entrance you just came from is marked, pick an arbitrary unmarked entrance, if any.  //
            /////////////////////////////////////////////////////////////////////////////////////////////////////
            // Check the possible directions
            List<Cell.WallFlag> possibleDirections = new ArrayList<Cell.WallFlag>();
            if(!currentCell.walls.contains(Cell.WallFlag.Up) && cellMap[currentX][currentY + 1].visitCount == 0){
                possibleDirections.add(Cell.WallFlag.Up);
            }
            if(!currentCell.walls.contains(Cell.WallFlag.Down) && cellMap[currentX][currentY - 1].visitCount == 0){
                possibleDirections.add(Cell.WallFlag.Down);
            }
            if(!currentCell.walls.contains(Cell.WallFlag.Left) && cellMap[currentX - 1][currentY].visitCount == 0){
                possibleDirections.add(Cell.WallFlag.Left);
            }
            if(!currentCell.walls.contains(Cell.WallFlag.Right) && cellMap[currentX + 1][currentY].visitCount == 0){
                possibleDirections.add(Cell.WallFlag.Right);
            }

            // Pick any entrance with the fewest marks (zero if possible, else one).
            if(possibleDirections.size() == 0){
                // Find the dirrection of the surrounding cell (up, down, left, right) with the least visits
                int minVisits = 10;
                Cell.WallFlag minDirection = Cell.WallFlag.Up;
                if(cellMap[currentX][currentY + 1].visitCount < minVisits && !currentCell.walls.contains(Cell.WallFlag.Up)){
                    minVisits = cellMap[currentX][currentY + 1].visitCount;
                    minDirection = Cell.WallFlag.Up;
                }
                if(cellMap[currentX][currentY - 1].visitCount < minVisits && !currentCell.walls.contains(Cell.WallFlag.Down)){
                    minVisits = cellMap[currentX][currentY - 1].visitCount;
                    minDirection = Cell.WallFlag.Down;
                }
                if(cellMap[currentX - 1][currentY].visitCount < minVisits && !currentCell.walls.contains(Cell.WallFlag.Left)){
                    minVisits = cellMap[currentX - 1][currentY].visitCount;
                    minDirection = Cell.WallFlag.Left;
                }
                if(cellMap[currentX + 1][currentY].visitCount < minVisits && !currentCell.walls.contains(Cell.WallFlag.Right)){
                    minVisits = cellMap[currentX + 1][currentY].visitCount;
                    minDirection = Cell.WallFlag.Right;
                }

                if(minVisits > 1){
                    System.out.println("We visited too much!");
                }
                // Go in that direction
                newDirection = minDirection;
            }
            // Perfer to go straight
            else if (previousX < currentX && possibleDirections.contains(Cell.WallFlag.Right)){
                newDirection = Cell.WallFlag.Right;
            } else if (previousX > currentX && possibleDirections.contains(Cell.WallFlag.Left)){
                newDirection = Cell.WallFlag.Left;
            } else if (previousY < currentY && possibleDirections.contains(Cell.WallFlag.Up)){
                newDirection = Cell.WallFlag.Up;
            } else if (previousY > currentY && possibleDirections.contains(Cell.WallFlag.Down)){
                newDirection = Cell.WallFlag.Down;
            } else {
                // Pick a random direction
                Random rand = new Random();
                newDirection = possibleDirections.get(rand.nextInt(possibleDirections.size()));
            }
        }
         else if (currentCell.walls.size() == 3){
            System.out.println("Turn around");
             // Dead end, turn around
             newDirection =  EnumSet.complementOf(currentCell.walls).iterator().next();
             
             // Switch to U-turn mode
             backTracking = true;
         }
        else{
            System.out.println("Straight");
            // Take the current wall and add the wall we came from to the current cell
            var currentWalls = EnumSet.copyOf(currentCell.walls);
            if (previousX < currentX){
                currentWalls.add(Cell.WallFlag.Left);
            } else if (previousX > currentX){
                currentWalls.add(Cell.WallFlag.Right);
            } else if (previousY < currentY){
                currentWalls.add(Cell.WallFlag.Down);
            } else if (previousY > currentY){
                currentWalls.add(Cell.WallFlag.Up);
            }

            currentWalls = EnumSet.complementOf(currentWalls);
            newDirection = currentWalls.iterator().next();
        }

        // Calculate the new position
        lastDirection = newDirection;
        var newXY = xyFromDirection(currentX, currentY, newDirection);

        // Add  the new route
        goalX = newXY[0];
        goalY = newXY[1];

        highLevelRoute.add(createGridPosition(goalX, goalY));
    }
    private GridPosition createGridPosition(int x, int y){
        return new GridPosition((byte)(x-X_OFFSET), (byte)(y-Y_OFFSET));
    }

    private int[] xyFromDirection(int x, int y, WallFlag flag){
        switch(flag){
            case Up:
                return new int[]{x, y + 1};
            case Down:
                return new int[]{x, y - 1};
            case Left:
                return new int[]{x - 1, y};
            case Right:
                return new int[]{x + 1, y};
            default:
                System.out.println("Invalid direction");
                return new int[]{x, y};
        }
    }
}
