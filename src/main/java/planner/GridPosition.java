package planner;

public record GridPosition(byte x, byte y) {

    @Override
    public String toString() {
        return String.format("GridPosition(%d, %d)", x, y);
    }
}
