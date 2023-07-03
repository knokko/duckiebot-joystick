package controller.parameters;

public class DuckieParameters {

    // public final PIDParameters anglePID = new PIDParameters(1.5, 0.1, 0.01);
    // public final PIDParameters speedPID = new PIDParameters(0.5, 0.0, 0.05);

    // Skydriving + table: (1, 0, 0.3)
    // Carpet: (3, 0, 0.3)
    public final PIDParameters anglePID = new PIDParameters(3.00, 0.0, 0.3);
    public final PIDParameters speedPID = new PIDParameters(1.5, 0.0001, 0.5);
}
