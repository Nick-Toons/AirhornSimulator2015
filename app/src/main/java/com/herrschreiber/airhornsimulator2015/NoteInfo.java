package com.herrschreiber.airhornsimulator2015;

/**
 * Created by alex on 5/16/15.
 */
public class NoteInfo {
    private double startTime;
    private double duration;
    private double pitch;
    private double velocity;

    public NoteInfo() {
    }

    public NoteInfo(double startTime, double duration, double pitch, double velocity) {
        this.startTime = startTime;
        this.duration = duration;
        this.pitch = pitch;
        this.velocity = velocity;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    @Override
    public String toString() {
        return "NoteInfo{" + "startTime=" + startTime + ", duration=" + duration + ", pitch=" + pitch + ", velocity=" + velocity + '}';
    }
}
