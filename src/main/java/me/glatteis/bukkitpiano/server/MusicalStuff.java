package me.glatteis.bukkitpiano.server;

import me.glatteis.bukkitpiano.NoteHandler;
import me.glatteis.bukkitpiano.server.midiplayer.MidiPlayerHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;

/**
 * Created by Linus on 11.01.2016.
 * Thanks Linus, awesome plugin! Updated for 1.16 by avixk on March 12th 2021
 */
public class MusicalStuff {
    public static void playNote(Player player, byte[] midiData, int channel, boolean ignoreWarnings) {
        byte note = midiData[1];//note 6 is lowest, 78 is highest
        note = (byte) (note - 12); //subtract 1 octave because it always seems low
        int bukkitNote = note - 6; // bukkit note 0 is lowest, bukkit note 72 highest
        float velocity = midiData[2] / 127F;

        Sound sound;
        if(channel > 1){
            bukkitNote -= 24;
            sound = NoteHandler.getSound(channel);
        }else {
            if (bukkitNote > 48) {
                bukkitNote -= 48;
                sound = Sound.BLOCK_NOTE_BLOCK_BELL;//we have more noteblock sounds now!!!!
            } else if(bukkitNote > 24){
                bukkitNote -= 24;
                sound = Sound.BLOCK_NOTE_BLOCK_HARP;
            } else {
                sound = Sound.BLOCK_NOTE_BLOCK_BASS;
            }
        }

        if (bukkitNote > 24){
            if(!ignoreWarnings) player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                    ServerMain.prefix + "§cOut of range! Channel " + channel + " note " + bukkitNote + " too high!"));
            return;
        }else if(bukkitNote < 0){
            if(!ignoreWarnings) player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                    ServerMain.prefix + "§4Out of range! Channel " + channel + " note " + bukkitNote + " too low!"));
            return;
        }else{
            if(MidiPlayerHandler.swing_hand_enable){
                if(!MidiPlayerHandler.swing_hand_only_while_sitting || player.isInsideVehicle()){
                    if(bukkitNote >= 12){
                        player.swingMainHand();
                    }else{
                        player.swingOffHand();
                    }
                }
            }
        }


        float pitch = NoteHandler.getPitch(bukkitNote);


        //Everything below is just for the note particles.

        //color is 0 to 2, if you add or remove octaves, change this so you get the full color spectrum.
        //double color = (note-6) / 72D;
        double color = bukkitNote / 12D;

        //get the player's direction, set y to 0, and normalize. Basically it just makes looking up/down not affect anything.
        Vector unrotatedPlayerDirection = player.getEyeLocation().getDirection().multiply(new Vector(1,0,1)).normalize();

        //rotate the player's direction so we can use it for the x axis, don't need to normalize because we normalized unrotatedPlayerDirection
        Vector rotatedPlayerDirection = rotateVectorAroundY(unrotatedPlayerDirection,90).normalize();

        //subtract 0.5 to center it (color is from 0 to 2), and multiply by 1.5 to make it 3 blocks wide.
        //rotatedPlayerDirection = rotatedPlayerDirection.multiply((color-1D)*1.5);
        rotatedPlayerDirection = rotatedPlayerDirection.multiply((((note-6) / 72D)-0.5D)*3);

        //do the same except with velocity and unrotated direction
        unrotatedPlayerDirection = unrotatedPlayerDirection.multiply((velocity-0.5D)*3);

        //add the two vectors together to get the final vector we add to the player's location
        Vector combinedPlayerDirection = rotatedPlayerDirection.add(unrotatedPlayerDirection);

        //note will be near the player's head.
        Location noteLocation = player.getEyeLocation().clone();

        //add the final vector to the player's eye location.
        noteLocation = noteLocation.add(combinedPlayerDirection);

        //also add 0.8 to the y so the notes spawn over your head instead of inside.
        noteLocation = noteLocation.add(0,0.8D,0);

        //make other channels a slightly different height
        if(channel > 1) noteLocation = noteLocation.add(0,1,0);

        //count should be 0 when creating colored particles. Also at least one offset must be non-zero or it wont work.

        //It may look pretty now, but the birth of that vector math was disgusting.


        Location finalNoteLocation = noteLocation;
        Bukkit.getScheduler().scheduleSyncDelayedTask(ServerMain.plugin, new Runnable() {
            @Override
            public void run() {
                player.getWorld().spawnParticle(Particle.NOTE, finalNoteLocation,0,1,0,0, color);

                for(Player listener : player.getWorld().getPlayersSeeingChunk(player.getLocation().getChunk())){
                    if(!Storage.isMuted(listener.getUniqueId()))
                        listener.playSound(player.getEyeLocation().add(0,1,0), sound, velocity, pitch);
                }
            }
        });

        if(ServerMain.debugPlayers.contains(player.getUniqueId()))
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                    " §eNote: " + bukkitNote +  " §eVelocity: " + midiData[2] + " §6Channel: " + channel + " §eSound: " + sound.name() + " §cPPT: " + PacketReceiver.packetsLastTick +  " §6Packet: " + Arrays.toString(midiData)));
    }


    // I got no idea how this works, I copied it from somewhere ¯\_(ツ)_/¯
    public static Vector rotateVectorAroundY(Vector vector, double degrees) {
        double rad = Math.toRadians(degrees);

        double currentX = vector.getX();
        double currentZ = vector.getZ();

        double cosine = Math.cos(rad);
        double sine = Math.sin(rad);

        return new Vector((cosine * currentX - sine * currentZ), vector.getY(), (sine * currentX + cosine * currentZ));
    }
}
