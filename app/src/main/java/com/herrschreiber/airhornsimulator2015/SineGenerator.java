package com.herrschreiber.airhornsimulator2015;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

/**
 * Created by alex on 4/10/15.
 */
public class SineGenerator implements AudioProcessor {
    private float gain;
    private double frequency;
    private double phase;

    public SineGenerator() {
        this(1.0f, 440);
    }

    public SineGenerator(float gain, double frequency) {
        this.gain = gain;
        this.frequency = frequency;
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        float[] buffer = audioEvent.getFloatBuffer();
        double sampleRate = audioEvent.getSampleRate();
        for (int i = audioEvent.getOverlap(); i < buffer.length; i++) {
            buffer[i] = (float) Math.sin(phase);
            phase += 2 * Math.PI * frequency / sampleRate;
        }
        return true;
    }

    @Override
    public void processingFinished() {
    }
}
