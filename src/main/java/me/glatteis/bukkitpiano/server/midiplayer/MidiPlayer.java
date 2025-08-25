package me.glatteis.bukkitpiano.server.midiplayer;

import me.glatteis.bukkitpiano.server.ServerMain;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;

public class MidiPlayer {
    public int position = 0;
    MidiSequence sequence;
    Player playingplayer;
    boolean playing = false;
    Song song;

    public MidiPlayer(Player player){
        this.playingplayer = player;
    }

    public void playMidiFile(Song song){
        this.song = song;
        sequence = MidiPlayerHandler.load(new File(ServerMain.plugin.getDataFolder() + "/midi/" + song.getFileName()),song);
        position = 0;
        playing = true;
        step();
    }

    private long lastTick = 0;
    private int lastTask = 0;
    private NoteEvent currentNote;
    public void step(){
        int stepsThisTick = 0;
        currentNote = sequence.getEvents().get(position);
        long waittime = currentNote.getTick() - lastTick;
        while(waittime == 0){
            if(stepsThisTick >= MidiPlayerHandler.max_notes_per_tick_midi){
                stepsThisTick++;
                if(!song.isIgnoreWarnings()) playingplayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                        ServerMain.prefix + "§cToo many notes per tick! Some notes skipped. (" + stepsThisTick + "/t)"));
                tick(false);
            }/*else if(stepsThisTick > MidiPlayerHandler.max_notes_per_tick_midi){
                tick(false);
            }*/else {
                tick(true);
            }
            stepsThisTick++;
            if(!playing) return;
            waittime = currentNote.getTick() - lastTick;
        }
        lastTask = Bukkit.getScheduler().scheduleSyncDelayedTask(ServerMain.plugin, new Runnable() {
            @Override
            public void run() {
                tick(true);
                if(playing) step();
            }
        },waittime);
    }

    public void tick(boolean playNote){
        if(sequence == null || playingplayer == null || !playingplayer.isOnline()){
            stop();
            return;
        }
        if(playNote) currentNote.play(playingplayer);
        lastTick = currentNote.getTick();
        position++;
        if(position >= sequence.getEvents().size()){
            //song over
            stop();
            position = 0;
            Bukkit.getScheduler().scheduleSyncDelayedTask(ServerMain.plugin, new Runnable() {
                @Override
                public void run() {
                    playingplayer.sendMessage("§aSong finished. Type §e/bukkitpiano resume§a to replay.");
                }
            },20);
            return;
        }
        currentNote = sequence.getEvents().get(position);
    }

    public void stop() {
        playing = false;
        Bukkit.getScheduler().cancelTask(lastTask);
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getPosition() {
        return position;
    }

    public MidiSequence getSequence() {
        return sequence;
    }

    public Player getPlayingplayer() {
        return playingplayer;
    }

    public void resume() {
        playing = true;
        step();
    }

    public void setPlayer(Player player) {
        this.playingplayer = player;
    }
}
