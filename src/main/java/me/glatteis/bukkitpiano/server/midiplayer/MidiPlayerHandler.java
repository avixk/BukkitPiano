package me.glatteis.bukkitpiano.server.midiplayer;

import me.glatteis.bukkitpiano.server.ServerMain;
import me.glatteis.bukkitpiano.server.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import javax.sound.midi.*;
import java.io.File;
import java.util.*;

public class MidiPlayerHandler {
    public static List<Song> songs = new ArrayList<>();
    public static HashMap<UUID,MidiPlayer> sessions = new HashMap<>();
    public static int max_notes_per_tick_streaming = 30;
    public static int max_notes_per_tick_midi = 30;
    public static boolean swing_hand_enable = true;
    public static boolean swing_hand_only_while_sitting = false;


    public static void init(){
        if(!new File(ServerMain.plugin.getDataFolder() + "/midi").exists()){
            String[] default_songs = {
                    "Mii.mid",
                    "Bohemian_Rhapsody-bukkitpiano_edit.mid",
                    "Wintergatan-Marble_Machine-bukkitpiano_edit.mid",
                    "Jeopardy-bukkitpiano_edit.mid",
                    "Stairway_to_Heaven-bukkitpiano_edit.mid",
                    "Through_the_Fire_and_Flames-bukkitpiano_edit.mid",
                    "Toccata_and_Fugue_in_D_minor-Bach-bukkitpiano_edit.mid",
                    "Dummy-Undertale-bukkitpiano_edit.mid",
                    "rush_e-bukkitpiano_edit.mid",
                    "Never_Gonna_Give_You_Up-bukkitpiano_edit.mid",
                    "Spear_of_Justice-bukkitpiano_edit.mid",
                    "Pirates_of_The_Caribbean-bukkitpiano_edit.mid",
                    "Megalovania-bukkitpiano_edit.mid",
                    "Fur_Elise-bukkitpiano_edit.mid",
                    "Sweet_Child_O_Mine-bukkitpiano_edit.mid",
                    "Moonlight_Sonata-bukkitpiano_edit.mid",
                    "The_Final_Countdown-bukkitpiano_edit.mid"};
            for(String default_song : default_songs){
                try {
                    Util.copyFromJarToDataFolder(ServerMain.plugin,"midi/" + default_song);
                }catch (Exception e){
                    ServerMain.plugin.getLogger().warning("Failed to copy default song " + default_song);
                }
            }
        }

        if(ServerMain.plugin.getConfig().contains("swing_hand.enable"))
            swing_hand_enable = ServerMain.plugin.getConfig().getBoolean("swing_hand.enable");
        if(ServerMain.plugin.getConfig().contains("swing_hand.only_while_sitting"))
            swing_hand_only_while_sitting = ServerMain.plugin.getConfig().getBoolean("swing_hand.only_while_sitting");
        if(ServerMain.plugin.getConfig().contains("max_notes_per_tick.streaming"))
            max_notes_per_tick_streaming = ServerMain.plugin.getConfig().getInt("max_notes_per_tick.streaming");
        if(ServerMain.plugin.getConfig().contains("max_notes_per_tick.midi"))
            max_notes_per_tick_midi = ServerMain.plugin.getConfig().getInt("max_notes_per_tick.midi");
        songs.clear();
        for(String songName : ServerMain.plugin.getConfig().getConfigurationSection("midiPlayer.songs").getKeys(false)){
            if(songName.equals("example"))continue;
            int volume_percent = 1;
            int octave_offset = 0;
            if(ServerMain.plugin.getConfig().contains("midiPlayer.songs." + songName + ".volume_percent"))
                volume_percent = ServerMain.plugin.getConfig().getInt("midiPlayer.songs." + songName + ".volume_percent");
            if(ServerMain.plugin.getConfig().contains("midiPlayer.songs." + songName + ".octave_offset"))
                octave_offset = ServerMain.plugin.getConfig().getInt("midiPlayer.songs." + songName + ".octave_offset");
            String fileName = ServerMain.plugin.getConfig().getString("midiPlayer.songs." + songName + ".file");
            boolean ignoreWarnings = false;
            if(ServerMain.plugin.getConfig().contains("midiPlayer.songs." + songName + ".ignore_warnings"))
                ignoreWarnings = ServerMain.plugin.getConfig().getBoolean("midiPlayer.songs." + songName + ".ignore_warnings");
            String formattedName = Util.translateAllColorCodes(ServerMain.plugin.getConfig().getString("midiPlayer.songs." + songName + ".name"));
            File file = new File(ServerMain.plugin.getDataFolder() + "/midi/" + fileName);
            if(!file.exists()){
                ServerMain.plugin.getLogger().warning("Midi file for " + songName + " was not found at " + file.getPath());
                continue;
            }
            Song song = new Song(songName,fileName,formattedName,octave_offset,volume_percent,ignoreWarnings);
            songs.add(song);
            if(Bukkit.getPluginManager().getPermission("bukkitpiano.midi." + songName) == null)
                Bukkit.getPluginManager().addPermission(new Permission("bukkitpiano.midi." + songName, PermissionDefault.NOT_OP));
        }
    }
    public static MidiSequence load(File file, Song song) {
        ServerMain.plugin.getLogger().info("Loading MIDI file " + file.getName());
        long start = System.currentTimeMillis();
        try {
            Sequence sequence = MidiSystem.getSequence(file);
            int resolution = sequence.getResolution();
            List<NoteEvent> events = new ArrayList<>();
            List<MidiEvent> allEvents = new ArrayList<>();

            // Collect all events
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    allEvents.add(track.get(i));
                }
            }

            // Sort events by tick
            Collections.sort(allEvents, (e1, e2) -> Long.compare(e1.getTick(), e2.getTick()));

            long currentTempo = 500000; // Default tempo (120 BPM)
            long lastTick = 0;
            long lastTime = 0; // Microseconds

            for (MidiEvent midiEvent : allEvents) {
                long tick = midiEvent.getTick();
                long deltaTicks = tick - lastTick;
                long deltaTime = deltaTicks * currentTempo / resolution;
                lastTime += deltaTime;
                lastTick = tick;

                MidiMessage msg = midiEvent.getMessage();

                if (msg instanceof ShortMessage sm) {
                    //ServerMain.plugin.getLogger().info( tick + " " + msg.getStatus()+ " " + msg.getLength()+ " " + Arrays.toString(msg.getMessage()) + " " + sm.getCommand() + " " + msg);
                    if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                        long gameTicks = (lastTime / 50000); // Convert to 20-tick/second
                        events.add(new NoteEvent(
                                song,
                                gameTicks,
                                sm.getChannel() + 1,
                                sm.getData1(),
                                sm.getData2()
                        ));
                    }
                } else if (msg instanceof MetaMessage meta) {
                //ServerMain.plugin.getLogger().info( tick + " " +msg.getStatus()+ " " + msg.getLength()+ " msgtype:" + meta.getType() + " " + Arrays.toString(msg.getMessage()) + " " + new String(meta.getData()) + " " + msg);
                    if (meta.getType() == 0x51) { // Tempo change
                        byte[] data = meta.getData();
                        currentTempo = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF); // Not a clue how this works
                    }else if (meta.getType() == 0x2F) { // end of track

                    }else if (meta.getType() == 0x58) { // time signature

                    }else if (meta.getType() == 0x03) { // track name
                        String trackName = new String(meta.getData());

                    }
                    // https://mido.readthedocs.io/en/latest/meta_message_types.html
                }
            }
            ServerMain.plugin.getLogger().info("Took " + (System.currentTimeMillis() - start) + " ms.");
            return new MidiSequence(events, song);
        } catch (Exception e) {
            e.printStackTrace();
            ServerMain.plugin.getLogger().info("Took " + (System.currentTimeMillis() - start) + " ms.");
            ServerMain.plugin.getLogger().info("Failed to load MIDI file " + file.getName());
            return null;
        }
    }
    public static String readMidiFile(String name) {
        StringBuilder output = new StringBuilder();
        Sequence sequence = null;
        try {
            sequence = MidiSystem.getSequence(new File(ServerMain.plugin.getDataFolder() + "/midi/" + name));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        float divisionType = sequence.getDivisionType();
        String divisionTypeStr;
        if (divisionType == Sequence.PPQ) {
            divisionTypeStr = "PPQ (Ticks per quarter note)";
        } else if (divisionType == Sequence.SMPTE_24) {
            divisionTypeStr = "SMPTE 24 frames/sec";
        } else if (divisionType == Sequence.SMPTE_25) {
            divisionTypeStr = "SMPTE 25 frames/sec";
        } else if (divisionType == Sequence.SMPTE_30) {
            divisionTypeStr = "SMPTE 30 frames/sec";
        } else if (divisionType == Sequence.SMPTE_30DROP) {
            divisionTypeStr = "SMPTE 30 drop frames/sec";
        } else {
            divisionTypeStr = "Unknown";
        }
        output.append("MIDI File Information:\n");
        output.append("Division Type: ").append(divisionTypeStr).append("\n");
        output.append("Resolution: ").append(sequence.getResolution()).append("\n");
        output.append("Number of Tracks: ").append(sequence.getTracks().length).append("\n\n");

        int trackNumber = 0;
        for (Track track : sequence.getTracks()) {
            output.append("Track ").append(trackNumber++).append(":\n");
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                long tick = event.getTick();
                MidiMessage message = event.getMessage();
                output.append("  [Tick: ").append(tick).append("] ");
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    int command = sm.getCommand();
                    int channel = sm.getChannel();
                    int data1 = sm.getData1();
                    int data2 = sm.getData2();
                    String commandStr;
                    switch (command) {
                        case ShortMessage.NOTE_ON:
                            commandStr = "NOTE_ON";
                            output.append(String.format("%s - Channel: %d, Note: %d, Velocity: %d",
                                    commandStr, channel, data1, data2));
                            break;
                        case ShortMessage.NOTE_OFF:
                            commandStr = "NOTE_OFF";
                            output.append(String.format("%s - Channel: %d, Note: %d, Velocity: %d",
                                    commandStr, channel, data1, data2));
                            break;
                        case ShortMessage.PROGRAM_CHANGE:
                            commandStr = "PROGRAM_CHANGE";
                            output.append(String.format("%s - Channel: %d, Program: %d",
                                    commandStr, channel, data1));
                            break;
                        case ShortMessage.CONTROL_CHANGE:
                            commandStr = "CONTROL_CHANGE";
                            output.append(String.format("%s - Channel: %d, Controller: %d, Value: %d",
                                    commandStr, channel, data1, data2));
                            break;
                        case ShortMessage.PITCH_BEND:
                            commandStr = "PITCH_BEND";
                            int bend = (data2 << 7) | data1;
                            output.append(String.format("%s - Channel: %d, Bend: %d",
                                    commandStr, channel, bend));
                            break;
                        case ShortMessage.CHANNEL_PRESSURE:
                            commandStr = "CHANNEL_PRESSURE";
                            output.append(String.format("%s - Channel: %d, Pressure: %d",
                                    commandStr, channel, data1));
                            break;
                        case ShortMessage.POLY_PRESSURE:
                            commandStr = "POLY_PRESSURE";
                            output.append(String.format("%s - Channel: %d, Note: %d, Pressure: %d",
                                    commandStr, channel, data1, data2));
                            break;
                        default:
                            commandStr = "Unknown Command: " + command;
                            output.append(String.format("%s - Channel: %d, Data1: %d, Data2: %d",
                                    commandStr, channel, data1, data2));
                            break;
                    }
                } else if (message instanceof MetaMessage) {
                    MetaMessage mm = (MetaMessage) message;
                    int type = mm.getType();
                    byte[] data = mm.getData();
                    String typeStr;
                    switch (type) {
                        case 0x00:
                            typeStr = "Sequence Number";
                            break;
                        case 0x01:
                            typeStr = "Text Event: " + new String(data);
                            break;
                        case 0x02:
                            typeStr = "Copyright: " + new String(data);
                            break;
                        case 0x03:
                            typeStr = "Track Name: " + new String(data);
                            break;
                        case 0x04:
                            typeStr = "Instrument Name: " + new String(data);
                            break;
                        case 0x05:
                            typeStr = "Lyric: " + new String(data);
                            break;
                        case 0x06:
                            typeStr = "Marker: " + new String(data);
                            break;
                        case 0x07:
                            typeStr = "Cue Point: " + new String(data);
                            break;
                        case 0x20:
                            typeStr = "Channel Prefix";
                            break;
                        case 0x2F:
                            typeStr = "End of Track";
                            break;
                        case 0x51:
                            typeStr = "Set Tempo";
                            break;
                        case 0x54:
                            typeStr = "SMPTE Offset";
                            break;
                        case 0x58:
                            typeStr = "Time Signature";
                            break;
                        case 0x59:
                            typeStr = "Key Signature";
                            break;
                        case 0x7F:
                            typeStr = "Sequencer Specific";
                            break;
                        default:
                            typeStr = "Unknown Meta Type: " + type;
                            break;
                    }
                    output.append("Meta Message - Type: ").append(typeStr);
                    if (data.length > 0 && type != 0x01 && type != 0x02 && type != 0x03 && type != 0x04 &&
                            type != 0x05 && type != 0x06 && type != 0x07) {
                        output.append(", Data: ");
                        for (byte b : data) {
                            output.append(String.format("%02X ", b));
                        }
                    }
                } else if (message instanceof SysexMessage) {
                    SysexMessage sm = (SysexMessage) message;
                    byte[] data = sm.getData();
                    output.append("SysEx Message - Length: ").append(data.length).append(", Data: ");
                    for (byte b : data) {
                        output.append(String.format("%02X ", b));
                    }
                } else {
                    output.append("Unknown Message Type");
                }
                output.append("\n");
            }
            output.append("\n");
        }
        return output.toString();
    }
    public static void cleanup(){
        for(Map.Entry<UUID, MidiPlayer> session : sessions.entrySet()){
            session.getValue().stop();
        }
    }


    public static MidiPlayer getOrCreateSession(Player player){
        MidiPlayer midiPlayer = getSession(player.getUniqueId());
        if(midiPlayer != null)
            return midiPlayer;
        MidiPlayer session = new MidiPlayer(player);
        sessions.put(player.getUniqueId(),session);
        return session;
    }
    public static MidiPlayer getSession(UUID player){
        return sessions.get(player);
    }

    public static Iterable<String> getAvailableSongs(CommandSender sender) {
        List<String> out = new ArrayList<>();
        for (Song song : songs) {
            if(sender == null  || sender.hasPermission("bukkitpiano.midi." + song.getName())){
                out.add(song.getName());
            }
        }
        return out;
    }
    public static Song getSong(String songName){
        for(Song song : songs){
            if(song.getName().equals(songName))
                return song;
        }
        return null;
    }

}
