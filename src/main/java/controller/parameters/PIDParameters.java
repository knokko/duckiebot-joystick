package controller.parameters;

public class PIDParameters {

    public volatile double Kp, Ki, Kd;
    public volatile double correctionP, correctionI, correctionD;

    public PIDParameters(double Kp, double Ki, double Kd) {
        this.Kp = Kp;
        this.Ki = Ki;
        this.Kd = Kd;
    }
}
