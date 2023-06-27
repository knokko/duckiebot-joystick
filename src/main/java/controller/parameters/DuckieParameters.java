package controller.parameters;

public class DuckieParameters {

    public final PIDParameters anglePID = new PIDParameters(1.0, 0.0, 0.0);
    public final PIDParameters speedPID = new PIDParameters(0.75, 0.5, 0.2);
}
