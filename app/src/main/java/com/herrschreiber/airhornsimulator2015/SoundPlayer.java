package com.herrschreiber.airhornsimulator2015;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;

/**
 * Created by alex on 2/13/15.
 */
public class SoundPlayer {
    private static final String TAG = "SoundPlayer";
    public static final int AUDIO_BUFFER_SIZE = 1024;
    private final Map<String, AssetSound> sounds;
    private AudioDispatcher audioDispatcher;
    private Context context;

    public SoundPlayer(Context context) {
        this.context = context;
        sounds = new HashMap<>();
    }

    public void loadSounds() throws IOException {
        AssetManager assetManager = context.getAssets();
        String[] soundPaths;
        try {
            soundPaths = assetManager.list(AssetSound.AUDIO_PATH);
        } catch (IOException e) {
            throw new IOException("Failed to list sounds while loading sounds", e);
        }
        for (String soundPath : soundPaths) {
            Log.d("SoundPlayer", "Adding sound " + soundPath);
            AssetSound sound;
            try {
                sound = new AssetSound(soundPath, context);
            } catch (Exception e) {
                Log.e(TAG, "Error creating sound with path " + soundPath, e);
                continue;
            }
            sound.init();
            sounds.put(sound.getName(), sound);
        }
    }

    public Map<String, AssetSound> getSounds() {
        return sounds;
    }

    public List<AssetSound> listSounds() {
        return new ArrayList<>(sounds.values());
    }

    public void playSound(final Sound sound) {
        if (audioDispatcher != null)
            audioDispatcher.stop();
        sound.start();

        double sampleRate = sound.getFormat().getSampleRate();
        WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.slowdownDefaults(2.0, sampleRate));
        audioDispatcher = new AudioDispatcher(sound, wsola.getInputBufferSize(), wsola.getOverlap());
        wsola.setDispatcher(audioDispatcher);
//        audioDispatcher.addAudioProcessor(wsola);
        audioDispatcher.addAudioProcessor(new AndroidAudioPlayer(audioDispatcher.getFormat(), AUDIO_BUFFER_SIZE));
        AsyncTask.THREAD_POOL_EXECUTOR.execute(audioDispatcher);
    }

}
