package com.herrschreiber.airhornsimulator2015;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alex on 5/16/15.
 */
public class SongParser {
    private static final String SONGS_PATH = "Songs";
    private Context context;
    private List<Song> songs = null;

    public SongParser(Context context) {
        this.context = context;
    }

    public void parseSongs() throws IOException {
        if (songs == null) {
            songs = new ArrayList<>();
            String[] songNames = context.getAssets().list(SONGS_PATH);
            for (String songName : songNames) {
                songs.add(parseMidiFile(songName));
            }
            songs.add(scale());
        }
    }

    public List<Song> getSongs() {
        return songs;
    }

    private Song parseMidiFile(String songName) throws IOException {
        String path = new File(SONGS_PATH, songName).getPath();
        String name = songName;
        int pos = name.lastIndexOf('.');
        if (pos != -1) {
            name = name.substring(0, pos);
        }
        return new Song(name);
    }

    private Song scale() {
        List<NoteInfo> notes = new ArrayList<>();
        notes.add(new NoteInfo(0.0, 0.5, 261.63, 1.0));
        notes.add(new NoteInfo(0.5, 1.0, 293.66, 0.5));
        notes.add(new NoteInfo(1.5, 0.5, 329.63, 0.5));
        notes.add(new NoteInfo(2.0, 1.0, 349.23, 1.0));
        notes.add(new NoteInfo(3.0, 0.5, 392.00, 1.0));
        notes.add(new NoteInfo(3.5, 1.0, 440.00, 0.5));
        notes.add(new NoteInfo(4.5, 0.5, 493.88, 0.5));
        notes.add(new NoteInfo(5.0, 1.0, 523.25, 1.0));
        return new Song("scale", notes);
    }
}
