package com.herrschreiber.airhornsimulator2015;

import android.util.Log;

import java.io.IOException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.resample.RateTransposer;

/**
 * Created by alex on 5/16/15.
 */
public class SampledSongSound extends Sound {
    private static final String TAG = "SampledSongSound";
    private AssetSound sample;
    private Song song;

    public SampledSongSound(String name, AssetSound sample, Song song) {
        super(name);
        this.sample = sample;
        this.song = song;
    }

    @Override
    public void init() throws IOException {
        sample.initialize();
        setAudioFormat(sample.getFormat());

        double duration = 0;
        for (NoteInfo note : song.getNotes()) {
            double endTime = note.getStartTime() + note.getDuration() + .01; // To compensate for math error
            if (endTime > duration) {
                duration = endTime;
            }
        }
        Log.d(TAG, "Duration of song: " + duration);

        final float sampleRate = sample.getSampleRate();
        final float[] floatBuffer = new float[(int) (duration * sampleRate)];
        final double sampleDuration = sample.getDuration();

        for (final NoteInfo note : song.getNotes()) {
            double noteDuration = note.getDuration();
            double pitchFactor = sample.getAveragePitch() / note.getPitch();
            double durationFactor = sampleDuration / noteDuration * pitchFactor;
            double gain = note.getVelocity();

            Log.d(TAG, "note: " + note + ", pitchFactor: " + pitchFactor + ", durationFactor: " + durationFactor);

            WaveformSimilarityBasedOverlapAdd wsola;
            RateTransposer rateTransposer = new RateTransposer(pitchFactor);
            wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(durationFactor, sampleRate));
            sample.start();
            final AudioDispatcher dispatcher = new AudioDispatcher(sample, wsola.getInputBufferSize(), wsola.getOverlap());
            wsola.setDispatcher(dispatcher);
            dispatcher.addAudioProcessor(new GainProcessor(gain));
            dispatcher.addAudioProcessor(wsola);
            dispatcher.addAudioProcessor(rateTransposer);
            dispatcher.addAudioProcessor(new AudioProcessor() {
                int dispatcherIndex = 0;

                @Override
                public void processingFinished() {
                }

                @Override
                public boolean process(AudioEvent audioEvent) {
                    int startIndex = (int) (note.getStartTime() * sampleRate) + dispatcherIndex;

                    float[] sampleBuffer = audioEvent.getFloatBuffer();
                    for (int i = 0; i < sampleBuffer.length; i++) {
                        if (i + startIndex >= floatBuffer.length) {
                            Log.e(TAG, "i(" + i + ") + startIndex(" + startIndex + ") >= floatBuffer.length(" + floatBuffer.length + ")");
                        } else {
                            floatBuffer[i + startIndex] += sampleBuffer[i];
                        }
                    }
                    dispatcherIndex += sampleBuffer.length;
                    return true;
                }
            });
            dispatcher.run();
        }
        TarsosDSPAudioFloatConverter converter = TarsosDSPAudioFloatConverter.getConverter(sample.getFormat());
        buffer = new byte[floatBuffer.length * 2];
        converter.toByteArray(floatBuffer, buffer);

        Log.i(TAG, "Finished creating song. Duration: " + getDuration());
    }
}
