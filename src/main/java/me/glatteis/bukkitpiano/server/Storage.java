package me.glatteis.bukkitpiano.server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Storage {
    public static List<UUID> mutedPlayers = new ArrayList<>();
    public static YamlFile storageFile;

    public static void init(){
        storageFile = new YamlFile("storage");
        if(storageFile.getConfig().contains("players"))
            for(String entry : storageFile.getConfig().getConfigurationSection("players").getKeys(false)){
                if(storageFile.getConfig().contains("players." + entry + ".muted") && storageFile.getConfig().getBoolean("players." + entry + ".muted"))
                    try {
                        mutedPlayers.add(UUID.fromString(entry));
                    }catch (Exception e){

                    }
            }
    }

    public static boolean isMuted(UUID player){
        return mutedPlayers.contains(player);
    }
    public static void setMuted(UUID player, boolean muted){
        if(muted)mutedPlayers.add(player);
        else mutedPlayers.remove(player);
        storageFile.getConfig().set("players." + player.toString() + ".muted", muted);
        storageFile.save(true);
    }

}
