package controller.parameters;

public class DuckieParameters {

    // Decent angle PID: (15, 0, 1.1)
    public final PIDParameters anglePID = new PIDParameters(15.00, 0.0, 1.5);
    public final PIDParameters speedPID = new PIDParameters(1.5, 0.0001, 0.5);
}
