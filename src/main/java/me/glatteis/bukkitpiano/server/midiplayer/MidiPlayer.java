package me.glatteis.bukkitpiano.server.midiplayer;

import me.glatteis.bukkitpiano.server.ServerMain;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MidiPlayer {
    public int position = 0;
    MidiSequence sequence;
    Player playingplayer;
    boolean playing = false;
    Song song;
    List<String> song_queue = new ArrayList<>();
    //List<String> finished_songs = new ArrayList<>();

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
        if(waittime < 0) waittime = 0;
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

    public boolean advanceSong(){
        if(song_queue.isEmpty()){
            //if(!finished_songs.isEmpty()) playingplayer.sendMessage("§aPlaylist finished. Type §e/bukkitpiano resume§a to replay.");
            playingplayer.sendMessage("§aSong finished. Type §e/bukkitpiano resume§a to replay.");
            return false;
        }
        String songname = song_queue.removeFirst();
        song = MidiPlayerHandler.getSong(songname);
        while(song == null){
            playingplayer.sendMessage("§cSong " + songname + " was in the queue but cannot be found.");
            ServerMain.plugin.getLogger().warning("Song " + songname + " queued for " + playingplayer.getName() + " is not a currently loaded song.");
            if(song_queue.isEmpty()) break;
            songname = song_queue.removeFirst();
            song = MidiPlayerHandler.getSong(songname);
        }
        if(song == null){
            //if(!finished_songs.isEmpty()) playingplayer.sendMessage("§aPlaylist finished. Type §e/bukkitpiano resume§a to replay.");
            playingplayer.sendMessage("§aSong finished. Type §e/bukkitpiano resume§a to replay.");
            return false;
        }
        sequence = MidiPlayerHandler.load(new File(ServerMain.plugin.getDataFolder() + "/midi/" + song.getFileName()),song);
        playingplayer.sendMessage("§aSong finished. Next up: §b" + song.getFormattedName() + "§a...");
        return true;
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
                    //finished_songs.add(songname);
                    boolean willContinue = advanceSong();
                    if(willContinue) resume();
                }
            }, ServerMain.plugin.getConfig().getInt("midiPlayer.after_song_delay"));
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

    public Song getCurrentSong() {
        return song;
    }

    public Player getPlayingplayer() {
        return playingplayer;
    }

    public void queueSong(String songname){
        song_queue.add(songname);
    }

    public void resume() {
        playing = true;
        step();
    }

    public void setPlayer(Player player) {
        this.playingplayer = player;
    }

    public List<String> getSongQueue() {
        return song_queue;
    }

    public String stateToString(){
        String queue = "";
        for(String queuesong : song_queue){
            if(queue.isEmpty()) queue += queuesong;
            else queue += "%" + queuesong;
        }
        String out = "";
        out += isPlaying() + "/";
        out += position + "/";
        out += (song == null? "noSong" : song.name)  + "/";
        out += queue  + "/";
        return out;
    }
    public boolean returnToStateFromString(String string){
        try {
            ServerMain.plugin.getLogger().info("Loading " + playingplayer.getName() + "'s state from string " + string);
            String[] splitString = string.split("/");
            boolean wasPlaying = Boolean.parseBoolean(splitString[0]);
            int oldPosition = Integer.parseInt(splitString[1]);
            String songName = splitString[2];
            String[] splitQueue = splitString[3].split("%");

            for(String queueSong : splitQueue){
                queueSong(queueSong);
            }
            if(!songName.isEmpty() && !songName.equals("noSong")){
                song = MidiPlayerHandler.getSong(songName);
                sequence = MidiPlayerHandler.load(new File(ServerMain.plugin.getDataFolder() + "/midi/" + song.getFileName()),song);
                position = oldPosition;
                if(position <= 0) lastTick = 0;
                else lastTick = sequence.getEvents().get(position - 1).getTick();
            }

            if(wasPlaying) resume();
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
