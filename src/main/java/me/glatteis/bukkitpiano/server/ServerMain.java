package me.glatteis.bukkitpiano.server;

import me.glatteis.bukkitpiano.server.midiplayer.MidiPlayer;
import me.glatteis.bukkitpiano.server.midiplayer.MidiPlayerHandler;
import me.glatteis.bukkitpiano.server.midiplayer.Song;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Linus on 10.01.2016.
 * Thanks Linus, awesome plugin! Updated for 1.16 by avixk on March 12th 2021
 */
public class ServerMain extends JavaPlugin implements Listener {

    public static Plugin plugin;

    private PacketReceiver packetReceiver;

    static List<UUID> debugPlayers = new ArrayList<>();
    static List<PianoPlayer> pianoPlayers = new ArrayList<>();
    static List<PianoPlayer> confirmationPlayers = new ArrayList<>();
    public static String prefix = "§b[BukkitPiano] ";
    private String[] message = new String[] {
            ChatColor.GREEN + "" + ChatColor.UNDERLINE + "BukkitPiano Help",
            "",
            "BukkitPiano is a plugin for playing the piano in Minecraft.",
            ChatColor.BLUE + "How can I play the piano in Minecraft?",
            ChatColor.BLUE + "Step 1: " + ChatColor.RESET + "Download the BukkitPiano JAR from <pretend there's a link here>",//TODO put the link there
            ChatColor.BLUE + "Step 2: " + ChatColor.RESET + "Plug in any MIDI controller.",
            ChatColor.BLUE + "Step 3: " + ChatColor.RESET + "Double click the JAR. If you have a mac, it might warn you about " +
                    "JARs being dangerous. BukkitPiano doesn't want do any harm. Just agree to open it.",
            ChatColor.BLUE + "Step 4: " + ChatColor.RESET + "Type in the IP of this server in the first line, and your " +
                    "player name in the second line.",
            ChatColor.BLUE + "Step 5: " + ChatColor.RESET + "Click 'connect'. Then type in the command in chat."
    };

    public void onDisable() {
        MidiPlayerHandler.cleanup();
        packetReceiver.disable();
    }

    public static int port = 8138;
    public static String address = "0.0.0.0";

    public void onEnable() {
        plugin = this;
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        Storage.init();
        port = getConfig().getInt("listen_port");
        address = getConfig().getString("listen_address");
        pianoPlayers = new ArrayList<PianoPlayer>();
        confirmationPlayers = new ArrayList<PianoPlayer>();
        try {
            packetReceiver = new PacketReceiver(this);

            new BukkitRunnable() {
                public void run() {
                    packetReceiver.recievePacket();
                }
            }.runTaskTimerAsynchronously(this, 1, 1);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        MidiPlayerHandler.init();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("bukkitpiano") || label.equalsIgnoreCase("piano")) {
            if(!sender.hasPermission("bukkitpiano.use")){
                sender.sendMessage("§cYou do not have permission to use this plugin.");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(message);
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + "§cOnly players can execute this command.");
                return true;
            }
            Player p = (Player) sender;
            if (args[0].equals("link")) {
                if(!p.hasPermission("bukkitpiano.link")){
                    p.sendMessage("§cYou do not have permission to stream midi, try to play something with §e/bukkitpiano play <song>§c instead.");
                    return true;
                }
                for (PianoPlayer pianoPlayer : confirmationPlayers) {
                    if (pianoPlayer.player.equals(sender)) {

                        sender.sendMessage(prefix + "§aYour BukkitPiano MIDI client has linked successfully. Try playing some notes!");
                        pianoPlayers.add(pianoPlayer);
                        confirmationPlayers.remove(pianoPlayer);
                        return true;
                    }
                }
                sender.sendMessage(prefix + "§cNo connection received yet! Start BukkitPiano.jar and connect, then try again. Contact staff for the jar, IP, and port.");
                return true;
            }else if (args[0].equals("debug") && p.isOp()) {
                if(debugPlayers.contains(((Player) sender).getUniqueId())){
                    debugPlayers.remove(p.getUniqueId());
                    sender.sendMessage(prefix + "§eDebug mode disabled");
                }else {
                    debugPlayers.add(p.getUniqueId());
                    sender.sendMessage(prefix + "§eDebug mode enabled");
                }
                return true;
            }else if (args[0].equals("play")) {
                if(args.length == 1){
                    String songs = "";
                    for(String songString : MidiPlayerHandler.getAvailableSongs(sender)){
                        if(!songs.isEmpty())
                            songs += ", ";
                        songs += songString;
                    }
                    if(songs.isEmpty()){
                        sender.sendMessage("§eYou don't have any songs available!");
                        return true;
                    }
                    sender.sendMessage("§aYour available songs: §e" + songs);
                    return true;
                }
                String songName = args[1];
                if(MidiPlayerHandler.getSong(songName) == null || !p.hasPermission("bukkitpiano.midi." + songName)){
                    String songs = "";
                    for(String songString : MidiPlayerHandler.getAvailableSongs(sender)){
                        if(!songs.isEmpty())
                            songs += ", ";
                        songs += songString;
                    }
                    if(!p.hasPermission("bukkitpiano.midi." + songName))
                        sender.sendMessage("§cYou don't have access to this song. Your available songs: §e" + songs);
                    else sender.sendMessage("§cSong not found. Your available songs: §e" + songs);
                    return true;
                }
                Song song = MidiPlayerHandler.getSong(songName);

                MidiPlayer midiPlayer = MidiPlayerHandler.getOrCreateSession(p);
                if(midiPlayer.isPlaying()){
                    midiPlayer.stop();
                }

                Storage.setMuted(p.getUniqueId(), false);
                midiPlayer.playMidiFile(song);
                sender.sendMessage("§aPlaying §e" + midiPlayer.getSequence().getSong().getFormattedName() + "§a...");

                return true;
            }else if (args[0].equals("pause")) {
                MidiPlayer midiPlayer = MidiPlayerHandler.getSession(p.getUniqueId());
                if(midiPlayer == null || !midiPlayer.isPlaying()){
                    sender.sendMessage("§cYou don't have anything playing!");
                    return true;
                }
                midiPlayer.stop();
                //p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText());
                sender.sendMessage("§ePaused track...");
                return true;
            }else if (args[0].equals("resume")) {
                MidiPlayer midiPlayer = MidiPlayerHandler.getSession(p.getUniqueId());
                if(midiPlayer == null){
                    sender.sendMessage("§cYou don't have anything playing!");
                    return true;
                }
                if(midiPlayer.getSequence() != null){
                    Storage.setMuted(p.getUniqueId(), false);
                    sender.sendMessage("§aResuming §e" + midiPlayer.getSequence().getSong().getFormattedName() + "§a...");
                    midiPlayer.resume();
                    return true;
                }
                return true;
            }else if (args[0].equals("dump")) {
                sender.sendMessage(MidiPlayerHandler.readMidiFile(args[1]));
                return true;
            }else if (args[0].equals("mute")) {
                MidiPlayer midiPlayer = MidiPlayerHandler.getSession(p.getUniqueId());
                if(midiPlayer != null && midiPlayer.isPlaying()){
                    sender.sendMessage("§cYou can't mute your own tunes!");
                    return true;
                }
                boolean muted = Storage.isMuted(p.getUniqueId());
                Storage.setMuted(p.getUniqueId(), !muted);
                if(!muted)
                    sender.sendMessage("§eBukkitPiano muted.");
                else
                    sender.sendMessage("§aBukkitPiano unmuted.");
                return true;
            }else if (args[0].equals("reload")) {
                reloadConfig();
                MidiPlayerHandler.cleanup();
                MidiPlayerHandler.init();
                sender.sendMessage("§aReloaded config.");
                return true;
            }else if (args[0].equals("test")) {

                return true;
            }
            sender.sendMessage("§cUsage: /bukkitpiano link|play|pause|resume");
        }
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (Map.Entry<UUID, MidiPlayer> p : MidiPlayerHandler.sessions.entrySet()) {
            if (p.getKey().equals(event.getPlayer().getUniqueId())) {
                p.getValue().setPlayer(event.getPlayer());
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        for (PianoPlayer p : pianoPlayers) {
            if (p.player.equals(event.getPlayer())) {
                pianoPlayers.remove(p);
                return;
            }
        }
    }

    private static final List<String> COMMANDS = List.of(new String[]{"link","mute","play","pause","resume"});
    private static final List<String> ADMIN_COMMANDS = List.of(new String[]{"reload","debug"});
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabCompleteHigh(TabCompleteEvent e){
        String[] buf = e.getBuffer().toLowerCase().split(" ");
        int length = buf.length;
        String relevant = buf[length - 1];
        if(e.getBuffer().endsWith(" ")){
            length++;
            relevant = "";
        }

        if(e.getBuffer().toLowerCase().startsWith("/bukkitpiano")|| e.getBuffer().toLowerCase().startsWith("/piano")){
            if(buf.length == 1 || length == 2){
                List<String> completions = new ArrayList<>();
                StringUtil.copyPartialMatches(relevant,COMMANDS,completions);
                if(e.getSender().hasPermission("bukkitpiano.admin")) StringUtil.copyPartialMatches(relevant,ADMIN_COMMANDS,completions);
                e.setCompletions(completions);
                return;
            }
            if(buf[1].equalsIgnoreCase("play")){
                if(length == 3){
                    List<String> completions = new ArrayList<>();
                    StringUtil.copyPartialMatches(relevant,MidiPlayerHandler.getAvailableSongs(e.getSender()),completions);
                    e.setCompletions(completions);
                    return;
                }
            }
            e.setCompletions(new ArrayList<>());
        }

    }
}