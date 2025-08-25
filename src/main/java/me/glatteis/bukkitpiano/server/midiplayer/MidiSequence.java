package me.glatteis.bukkitpiano.server.midiplayer;
import java.util.List;

public class MidiSequence {
    private final List<NoteEvent> events;
    private Song song;

    public MidiSequence(List<NoteEvent> events, Song song) {
        this.song = song;
        this.events = events;
    }

    public List<NoteEvent> getEvents() {
        return events;
    }

    public Song getSong() {
        return song;
    }
}