package me.glatteis.bukkitpiano.server;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class YamlFile {//version 0.1.0

    File file;
    YamlConfiguration yamlConfiguration = new YamlConfiguration();
    String identifier;

    public YamlFile(String fileString) {
        identifier = fileString;
        this.file = new File(ServerMain.plugin.getDataFolder() + "/" + identifier + ".yml");
        //Bukkit.getLogger().info("Registering YamlFile at " + this.file.getAbsolutePath());
        if (!this.file.exists()) {
            ServerMain.plugin.getDataFolder().mkdir();
            try{
                ServerMain.plugin.saveResource(fileString, false);
            }catch (Exception e){}
            try {
                this.file.createNewFile();
            } catch (IOException e) {}
        }
        try {
            yamlConfiguration.load(this.file);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to load " + file.getAbsolutePath() + ", this might be normal if it's the plugin's first time loading.");
        }
    }

    public File getFile() {
        return file;
    }

    public YamlConfiguration getConfig() {
        return yamlConfiguration;
    }

    int saveTask = 0;
    public void save(boolean force) {
        if(saveTask != 0){
            Bukkit.getScheduler().cancelTask(saveTask);
            saveTask = 0;
        }
        if(force){
            try {
                yamlConfiguration.save(file);
            } catch (IOException e) {
                e.printStackTrace();
                Bukkit.getLogger().warning("Could not save YamlFile '" + identifier + ".yml'! Data may be lost! It's avixk's fault!");
            }
            return;
        }
        saveTask = Bukkit.getScheduler().scheduleAsyncDelayedTask(ServerMain.plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    yamlConfiguration.save(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    Bukkit.getLogger().warning("Could not save YamlFile '" + identifier + ".yml'! Data may be lost! It's avixk's fault!");
                }
                saveTask = 0;
            }
        },1);
    }
}

