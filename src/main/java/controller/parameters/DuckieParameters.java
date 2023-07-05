package controller.parameters;

public class DuckieParameters {

    public final PIDParameters anglePID = new PIDParameters(20.00, 0.0, 2.5);
    public final PIDParameters speedPID = new PIDParameters(1.5, 0.0001, 0.5);
}
