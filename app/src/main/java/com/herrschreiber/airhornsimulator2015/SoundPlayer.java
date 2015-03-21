package com.herrschreiber.airhornsimulator2015;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.MultichannelToMono;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;
import be.tarsos.dsp.resample.RateTransposer;

/**
 * Created by alex on 2/13/15.
 */
public class SoundPlayer {
    public static final String SOUNDS_PATH = "sounds";
    public static final int DEFAULT_PRIORITY = 1;
    public static final int DEFAULT_RATE = 1;
    public static final int DEFAULT_VOLUME = 1;
    public static final int MAX_STREAMS = 2;
    private SoundPool soundPool;
    private List<Sound> sounds;

    public SoundPlayer() {
        soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
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
            if (soundName.contains("raw")) // i dont even know
                continue;
            Log.d("SoundPlayer", "Adding sound " + soundName);
            AssetFileDescriptor fd = am.openFd(new File(SOUNDS_PATH, soundName).getPath());
            int id = soundPool.load(fd, DEFAULT_PRIORITY);
            Sound sound = new Sound(soundName, id);
            sounds.add(sound);
        }
    }

    public List<Sound> getSounds() {
        return sounds;
    }

    public void playSound(Sound sound) {
        soundPool.play(sound.getId(), DEFAULT_VOLUME, DEFAULT_VOLUME, DEFAULT_PRIORITY, 0, DEFAULT_RATE);
    }

    public static class Sound {
        private String name;
        private int id;

        public Sound(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Sound{");
            sb.append("name='").append(name).append('\'');
            sb.append(", id=").append(id);
            sb.append('}');
            return sb.toString();
        }
    }
}
