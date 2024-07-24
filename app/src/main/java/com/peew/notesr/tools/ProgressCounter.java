package com.peew.notesr.tools;

public class ProgressCounter {
    public static final float MAX = 100f;

    private float progress;
    private float step = 1f;

    public void increase() {
        float newProgress = progress + step;
        progress = Math.min(newProgress, MAX);
    }

    public int getProgress() {
        return Math.round(progress);
    }

    public void setStep(float step) {
        this.step = step;
    }
}
