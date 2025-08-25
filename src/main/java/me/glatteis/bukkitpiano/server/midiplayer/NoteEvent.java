package me.glatteis.bukkitpiano.server.midiplayer;
import me.glatteis.bukkitpiano.server.MusicalStuff;
import org.bukkit.entity.Player;

public class NoteEvent {
    private final long tick;
    private final int channel;
    private final int note;
    private final int velocity;
    private final Song song;

    public NoteEvent(Song song, long tick, int channel, int note, int velocity) {
        this.song = song;
        this.tick = tick;
        this.channel = channel;
        this.note = note;
        this.velocity = velocity;
    }

    public long getTick() {
        return tick;
    }

    public void play(Player player) {
        // Skip low-volume notes
        if (velocity < 1) return;

        MusicalStuff.playNote(player, new byte[]{0, (byte) ((note + (12 * song.octave_offset)) -12/* lower one octave because everything playedback is too high */), (byte) (velocity * song.volume_percent)} ,channel, song.isIgnoreWarnings());
    }

    public int getChannel() {
        return channel;
    }

    public int getNote() {
        return note;
    }

    public int getVelocity() {
        return velocity;
    }
}