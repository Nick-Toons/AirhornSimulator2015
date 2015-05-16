package com.herrschreiber.airhornsimulator2015;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;

/**
 * Created by alex on 2/13/15.
 */
public class SoundPlayer {
    private static final String SOUNDS_PATH = "Sounds";
    private static final String TAG = "SoundPlayer";
    private final List<Sound> sounds;
    private AudioDispatcher audioDispatcher;

    public SoundPlayer() {
        sounds = new ArrayList<>();
    }

    public void loadSounds(Context context) throws IOException {
        AssetManager am = context.getAssets();
        String[] soundNames;
        try {
            soundNames = am.list(SOUNDS_PATH);
        } catch (IOException e) {
            throw new IOException("Failed to list sounds while loading sounds", e);
        }
        for (String soundName : soundNames) {
            Log.d("SoundPlayer", "Adding sound " + soundName);
            AssetFileDescriptor fd = am.openFd(new File(SOUNDS_PATH, soundName).getPath());
            Sound sound = null;
            try {
                sound = new Sound(soundName, fd);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error creating sound", e);
            }
            sounds.add(sound);
        }
    }

    public List<Sound> getSounds() {
        return sounds;
    }

    public void playSound(final Sound sound) {
        if (audioDispatcher != null)
            audioDispatcher.stop();
        sound.start();

        double sampleRate = sound.getSampleRate();
        WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.slowdownDefaults(2.0, sampleRate));
        audioDispatcher = new AudioDispatcher(sound, wsola.getInputBufferSize(), wsola.getOverlap());
        wsola.setDispatcher(audioDispatcher);
        audioDispatcher.addAudioProcessor(wsola);
        audioDispatcher.addAudioProcessor(new AndroidAudioPlayer(audioDispatcher.getFormat(), 1024));
        AsyncTask.THREAD_POOL_EXECUTOR.execute(audioDispatcher);
    }

}
