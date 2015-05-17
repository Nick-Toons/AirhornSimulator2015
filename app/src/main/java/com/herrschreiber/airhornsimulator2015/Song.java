package com.herrschreiber.airhornsimulator2015;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alex on 5/16/15.
 */
public class Song {
    private String name;
    private List<NoteInfo> notes;

    public Song(String name) {
        this.name = name;
        notes = new ArrayList<>();
    }

    public Song(String name, List<NoteInfo> notes) {
        this.name = name;
        this.notes = notes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<NoteInfo> getNotes() {
        return notes;
    }

    public void setNotes(List<NoteInfo> notes) {
        this.notes = notes;
    }
}
