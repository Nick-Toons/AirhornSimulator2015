package com.herrschreiber.airhornsimulator2015;

import android.app.Application;

/**
 * Created by alex on 5/16/15.
 */
public class AirhornSimulatorApplication extends Application {
    private SoundPlayer soundPlayer;
    private SongParser songParser;

    @Override
    public void onCreate() {
        super.onCreate();
        soundPlayer = new SoundPlayer(getApplicationContext());
        songParser = new SongParser(getApplicationContext());
    }

    public SoundPlayer getSoundPlayer() {
        return soundPlayer;
    }

    public SongParser getSongParser() {
        return songParser;
    }
}
