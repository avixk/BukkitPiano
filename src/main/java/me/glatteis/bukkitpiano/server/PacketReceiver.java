package me.glatteis.bukkitpiano.server;

import me.glatteis.bukkitpiano.LoginPacket;
import me.glatteis.bukkitpiano.NotePacket;
import me.glatteis.bukkitpiano.PackMethods;
import me.glatteis.bukkitpiano.QuitPacket;
import me.glatteis.bukkitpiano.server.midiplayer.MidiPlayerHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;

import static me.glatteis.bukkitpiano.server.ServerMain.prefix;

/**
 * Created by Linus on 10.01.2016.
 */
public class PacketReceiver {

    private final DatagramPacket packet;
    private final DatagramSocket socket;

    private final ServerMain main;

    public PacketReceiver(ServerMain main) throws SocketException, UnknownHostException {
        this.main = main;
        socket = new DatagramSocket(ServerMain.port, InetAddress.getByName(ServerMain.address));
        socket.setSoTimeout(1);
        byte[] buf = new byte[1080];
        packet = new DatagramPacket(buf, buf.length);
        Bukkit.getLogger().info("Listening on " + socket.getLocalAddress() + " " + socket.getLocalPort());
    }


    static int packetsLastTick = 0;
    public static HashMap<Player,Integer> notesThisTick = new HashMap<>();
    public void recievePacket() {
        int packetsThisTick = 0;
        if(!notesThisTick.isEmpty()) notesThisTick.clear();
        try {
            while (socket.getBroadcast()) {
                socket.receive(packet); //blocking, seems to get stuck if SoTimeout is 1000
                Object o = PackMethods.unpack(packet.getData());
                if (o instanceof LoginPacket) {
                    LoginPacket loginPacket = ((LoginPacket) o);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().equals(loginPacket.mcName)) {
                            p.sendMessage(prefix + "§fA BukkitPiano client just requested a login. Type " +
                                    ChatColor.GOLD + "[/bukkitpiano link]" + ChatColor.RESET + " if this is you.");
                            PianoPlayer pianoPlayer = new PianoPlayer(p, loginPacket.programID);
                            for (PianoPlayer pianoPlayer2 : ServerMain.confirmationPlayers) {
                                if (pianoPlayer2.player.equals(pianoPlayer.player)) {
                                    ServerMain.confirmationPlayers.remove(pianoPlayer2);
                                    break;
                                }
                            }
                            for (PianoPlayer pianoPlayer2 : ServerMain.pianoPlayers) {
                                if (pianoPlayer2.player.equals(pianoPlayer.player)) {
                                    ServerMain.pianoPlayers.remove(pianoPlayer2);
                                    break;
                                }
                            }
                            ServerMain.confirmationPlayers.add(pianoPlayer);
                            break;
                        }
                    }
                    socket.send(packet);
                }
                if (ServerMain.pianoPlayers.isEmpty()) {
                    break;
                }
                if (o instanceof NotePacket) {
                    NotePacket notePacket = (NotePacket) o;
                    byte[] midiBytes = notePacket.midiData;
                    int channel = notePacket.channel;
                    for (PianoPlayer pianoPlayer : ServerMain.pianoPlayers) {
                        if (pianoPlayer.id == notePacket.id) {
                            int numNotesThisTick = 0;
                            if(notesThisTick.containsKey(pianoPlayer.player))
                                numNotesThisTick = notesThisTick.get(pianoPlayer.player);

                            if(numNotesThisTick == MidiPlayerHandler.max_notes_per_tick_streaming){
                                pianoPlayer.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                                        ServerMain.prefix + "§cToo many notes per tick! Some notes skipped. (" + numNotesThisTick + "/t)"));
                                numNotesThisTick++;
                                notesThisTick.put(pianoPlayer.player,numNotesThisTick);
                                break;
                            }else if(numNotesThisTick > MidiPlayerHandler.max_notes_per_tick_streaming){
                                break;
                            }
                            MusicalStuff.playNote(pianoPlayer.player, midiBytes, channel, false);
                            numNotesThisTick++;
                            notesThisTick.put(pianoPlayer.player,numNotesThisTick);
                            break;
                        }
                    }
                } else if (o instanceof QuitPacket) {
                    QuitPacket quitPacket = (QuitPacket) o;
                    for (PianoPlayer pianoPlayer : ServerMain.pianoPlayers) {
                        if (pianoPlayer.id == quitPacket.id) {
                            ServerMain.pianoPlayers.remove(pianoPlayer);
                            pianoPlayer.player.sendMessage(prefix + "§cBukkitPiano disconnected.");
                            break;
                        }
                    }
                }
                packetsThisTick++;

            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }
        if(packetsThisTick > 0) packetsLastTick = packetsThisTick;
    }

    public void disable() {
        socket.close();

    }

}
