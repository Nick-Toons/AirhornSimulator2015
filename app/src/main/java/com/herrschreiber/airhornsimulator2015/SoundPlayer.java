package com.herrschreiber.airhornsimulator2015;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.tarsos.dsp.AudioDispatcher;

/**
 * Created by alex on 2/13/15.
 */
public class SoundPlayer {
    public static final int AUDIO_BUFFER_SIZE = 1024;
    private static final String TAG = "SoundPlayer";
    private Map<String, AssetSound> sounds;
    private AudioDispatcher audioDispatcher;
    private Context context;

    public SoundPlayer(Context context) {
        this.context = context;
    }

    public void loadSounds() throws IOException {
        if (sounds != null) return;
        sounds = new HashMap<>();
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
            sounds.put(sound.getName(), sound);
        }
    }

    public Map<String, AssetSound> getSounds() {
        return sounds;
    }

    public List<AssetSound> listSounds() {
        List<AssetSound> soundList = new ArrayList<>(sounds.values());
        Collections.sort(soundList, new Comparator<AssetSound>() {
            @Override
            public int compare(AssetSound lhs, AssetSound rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        return soundList;
    }

    public void playSound(final Sound sound) {
        this.stop();
        sound.start();

        audioDispatcher = new AudioDispatcher(sound, AUDIO_BUFFER_SIZE, AUDIO_BUFFER_SIZE / 2);
        audioDispatcher.addAudioProcessor(new AndroidAudioPlayer(audioDispatcher.getFormat(), AUDIO_BUFFER_SIZE));
        AsyncTask.THREAD_POOL_EXECUTOR.execute(audioDispatcher);
    }

    public void stop() {
        if (audioDispatcher != null)
            audioDispatcher.stop();
    }
}
