package controller;

import controller.desired.DesiredPose;
import controller.desired.DesiredVelocity;
import controller.estimation.DuckieEstimations;
import controller.updater.ControllerFunction;
import state.DuckieControls;
import state.DuckieState;

import static controller.util.DuckieBot.GRID_SIZE;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

public class Positionator implements ControllerFunction{
    private final DuckieEstimations estimations;
    private final DesiredVelocity desiredVelocity;
    private final DuckieControls controls;
    private final DuckieState trackedState;

    private double bootStrapSpeed = 0.05;
    private double turnSpeed = 0.015;
    private double moveSpeed = 0.05;
    private double currentDeviation = 0;
    private double lastDeviation = 0;
    private double stopSpeed = 0.001;
    List<ToFAngle> angleSweep = new ArrayList<>();
    private double maximumDeviation = 0.08;
    private double minimumDeviation = 0.005;
    private double duckieDistance = 0.05;//GRID_SIZE/2;
    private double originalAngle = 0;

    public Positionator(DesiredVelocity desiredVelocity, DuckieState trackedState,
            DuckieEstimations estimations, DuckieControls controls) {
        this.estimations = estimations;
        this.desiredVelocity = desiredVelocity;
        this.controls = controls;
        this.trackedState = trackedState;
    }

    record ToFAngle(double angle, double tof) {}

    double correctedAngle(double angle) {
        if (angle < -0.5) angle += 1;
        if (angle > 0.5) angle -= 1;
        return angle;
    }

    private void updateDeviation(){
        lastDeviation = currentDeviation;
        currentDeviation = Math.abs(correctedAngle(estimations.angle) - correctedAngle(originalAngle));
        // Print
        if(lastDeviation != currentDeviation) {
            System.out.println("Angle: " + estimations.angle + "\tDeviation: " + currentDeviation + "\tTof: " + trackedState.tof);
            angleSweep.add(new ToFAngle(estimations.angle, trackedState.tof));
        }
    }
    public void startReposition() {
        System.out.println("Repositioning");
        // Setup the code
        controls.override = true;
        originalAngle = estimations.angle;
        lastDeviation = 0;
        currentDeviation = 0;
        angleSweep.clear();

        // Turn the duckie left
        System.out.println("Turning left");
        controls.velLeft = -bootStrapSpeed;
        controls.velRight = bootStrapSpeed;
        currentDeviation = Math.abs(correctedAngle(estimations.angle - originalAngle));
        while(currentDeviation < maximumDeviation) {
            controls.velLeft = -turnSpeed;
            controls.velRight = turnSpeed;
            updateDeviation();
        }

        // Stop the duckie
        System.out.println("Stopping");
        controls.velRight = -stopSpeed;
        controls.velLeft = stopSpeed;
        currentDeviation = Math.abs(correctedAngle(estimations.angle - originalAngle));
        while(Math.abs(estimations.leftSpeed) + Math.abs(estimations.rightSpeed) == 0) {
        }

        // Turn back to the original angle
        System.out.println("Turning back");
        controls.velLeft = +bootStrapSpeed;
        controls.velRight =-bootStrapSpeed;
        currentDeviation = Math.abs(correctedAngle(estimations.angle - originalAngle));
        while(currentDeviation >= minimumDeviation) {
            controls.velLeft =  +turnSpeed;
            controls.velRight = -turnSpeed;
            updateDeviation();
        }

        // Turn the duckie right
        System.out.println("Turning right");
        currentDeviation = Math.abs(correctedAngle(estimations.angle - originalAngle));
        while(currentDeviation < maximumDeviation) {
            updateDeviation();
        }

        // Stop the duckie
        System.out.println("Stopping");
        controls.velRight = +stopSpeed;
        controls.velLeft  = -stopSpeed;
        while(Math.abs(estimations.leftSpeed) + Math.abs(estimations.rightSpeed) == 0) {
        }

        // Find the angle with the lowest tof
        System.out.println("Finding best angle");
        
        // Average the tof for each angle
        List<ToFAngle> averagedSweep = new ArrayList<>();
        angleSweep.stream().forEach(a -> {
            var sum = angleSweep.stream().filter(b -> b.angle == a.angle).mapToDouble(b -> b.tof).sum();
            var count = angleSweep.stream().filter(b -> b.angle == a.angle).count();
            averagedSweep.add(new ToFAngle(a.angle, sum / count));
        });

        var bestAngle = angleSweep.stream().min((a, b) -> Double.compare(a.tof, b.tof)).get().angle;
        System.out.println("Best angle: " + bestAngle);

        // Turn the duckie to the best angle
        System.out.println("Turning to best angle");
        originalAngle += (bestAngle - originalAngle);
        if(originalAngle > 1) originalAngle -= 1;
        if(originalAngle < -1) originalAngle += 1;
        controls.velLeft  = -bootStrapSpeed;
        controls.velRight = +bootStrapSpeed;
        currentDeviation = Math.abs(correctedAngle(estimations.angle - originalAngle));
        while(currentDeviation >= minimumDeviation) {
            controls.velLeft =  -turnSpeed;
            controls.velRight = +turnSpeed;
            updateDeviation();
        }
    
        // Stop the duckie
        System.out.println("Stopping");
        controls.velRight = -stopSpeed;
        controls.velLeft  = +stopSpeed;
        while(Math.abs(estimations.leftSpeed) + Math.abs(estimations.rightSpeed) == 0) {
        }

        // Find the distance to the wall
        var originalToF = trackedState.tof;
        System.out.println("Original tof: " + originalToF);

        // Move the duckie forward or backwards until the tof equal to GRID_SIZE
        System.out.println("Moving to/from wall");
        if (originalToF < duckieDistance) {
            controls.velLeft = moveSpeed;
            controls.velRight = moveSpeed;
            while(trackedState.tof > duckieDistance) {
            }
        } 
        else if (originalToF > duckieDistance){
            controls.velLeft = -moveSpeed;
            controls.velRight = -moveSpeed;
            while(trackedState.tof < duckieDistance) {
            }
        }
        else{
            // Stop the duckie
            controls.velRight = stopSpeed;
            controls.velLeft = stopSpeed;
        }

        // Stop the duckie
        System.out.println("Stopping");
        controls.velRight = -stopSpeed;
        controls.velLeft = stopSpeed;

        // Relase to controls
        System.out.println("Releasing controls");
        //controls.override = false;
    }

    @Override
    public void update(double deltaTime) {
        if(desiredVelocity.needReposition) {
            desiredVelocity.needReposition = false;
            startReposition();
        }
    }
}
