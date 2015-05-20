package com.herrschreiber.airhornsimulator2015;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alex on 5/16/15.
 */
public class SongParser {
    private static final String SONGS_PATH = "Songs";
    private static final int PITCH_OF_A4 = 57;
    private static final double FREQUENCY_OF_A4 = 440D;
    private static final String NOTE_SYMBOLS[] = {
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A",
            "A#", "B"
    };
    private static final String TAG = "SongParser";
    private Context context;
    private List<Song> songs = null;

    public SongParser(Context context) {
        this.context = context;
    }

    private static double parseNoteSymbol(String str)
            throws IllegalArgumentException {
        str = str.trim().toUpperCase();
        for (int i = NOTE_SYMBOLS.length - 1; i >= 0; i--) {
            if (str.startsWith(NOTE_SYMBOLS[i])) {
                String octaveStr = str.substring(NOTE_SYMBOLS[i].length()).trim();
                int octave = Integer.parseInt(octaveStr);
                int pitch = 12 * octave + i;
                double freq = Math.pow(2, (pitch - PITCH_OF_A4) / 12.0) * FREQUENCY_OF_A4;
                Log.i(TAG, "str: '" + str + "', pitch: " + pitch + ", freq: " + freq);
                return freq;
            }
        }
        throw new IllegalArgumentException("Note symbol not valid: '" + str + "'");
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
        List<NoteInfo> notes = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(path)));
        String line;
        double tempo = 90.0 / 60.0;
        double time = 0;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");
            if (parts.length < 2 || parts.length > 4) {
                throw new IOException("Invalid midi format. " + parts.length + " parts in a line");
            }
            String note = parts[0];
            double duration = Double.parseDouble(parts[1]);
            double velocity = 1.0;
            if (parts.length == 3) {
                velocity = Double.parseDouble(parts[2]);
            }
            if (note.equals("TEMPO")) {
                tempo = duration / 60.0;
                continue;
            }
            duration /= tempo;
            if (!note.equals("R")) {
                notes.add(new NoteInfo(time, duration, parseNoteSymbol(note), velocity));
            }
            time += duration;
        }
        reader.close();
        Log.i(TAG, "Parsed song " + name + ", notes: " + notes);
        return new Song(name, notes);
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
