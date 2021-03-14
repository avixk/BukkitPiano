package me.glatteis.bukkitpiano.server;

import me.glatteis.bukkitpiano.NoteHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Created by Linus on 11.01.2016.
 * Thanks Linus, awesome plugin! Updated for 1.16 by avixk on March 12th 2021
 */
public class MusicalStuff {

    public void playNote(Player player, byte[] midiData) {


        byte note = midiData[1];//note 6 is lowest 78 is highest
        int bukkitNote = note - 6; // bukkit note 0 is lowest bukkit note 72 highest

        Sound sound;

        if (bukkitNote > 48) {
            bukkitNote -= 48;
            sound = Sound.BLOCK_NOTE_BLOCK_BELL;//we have more noteblock sounds now!!!!
        } else if(bukkitNote > 24){
            bukkitNote -= 24;
            sound = Sound.BLOCK_NOTE_BLOCK_HARP;
        } else {
            sound = Sound.BLOCK_NOTE_BLOCK_BASS;
        }
        float velocity = midiData[2] / 127F;

        //player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
        //       "§eNote: " + note + " §6Bukkit Note: " + bukkitNote + " §eVelocity: " + velocity + " §6Sound: " + sound.name()));

        if (bukkitNote > 24 || bukkitNote < 0) return;
        float pitch = NoteHandler.getPitch(bukkitNote);

        player.getWorld().playSound(player.getEyeLocation(), sound, velocity, pitch);


        //color should be 0 to 1, if you add or remove octaves, change this so you get the full color spectrum.
        double color = (note-6) / 72D;

        //get the player's direction, set y to 0, and normalize. Basically it just makes looking up/down not affect anything.
        Vector unrotatedPlayerDirection = player.getEyeLocation().getDirection().multiply(new Vector(1,0,1)).normalize();

        //rotate the player's direction so we can use it for the x axis, don't need to normalize because i normalized unrotatedPlayerDirection
        Vector rotatedPlayerDirection = rotateVectorAroundY(unrotatedPlayerDirection,90).normalize();

        //subtract 0.5 to center it (color is from 0 to 1), and multiply by 3 to make it 3 blocks wide.
        rotatedPlayerDirection = rotatedPlayerDirection.multiply((color-0.5D)*3);

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

        //count should be 0 when creating colored particles. Also at least one offset must be non-zero or it wont work.
        player.getWorld().spawnParticle(Particle.NOTE,noteLocation,0,1,0,0, color);//count should be 0, and at least 1 offset must be non-zero, y is the color

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
