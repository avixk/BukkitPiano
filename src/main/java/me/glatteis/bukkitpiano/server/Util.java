package me.glatteis.bukkitpiano.server;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    public static String translateAllColorCodes(String message)    {
        return ChatColor.translateAlternateColorCodes('§',translateHexColorCodes(message.replace("&","§")));
    }
    public static String translateHexColorCodes(String message)    {
        if(false)return message;
        //Bukkit.getLogger().info(message);
        final Pattern hexPattern = Pattern.compile("§#" + "([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find())
        {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, "§" + "x"
                    + "§" + group.charAt(0) + "§" + group.charAt(1)
                    + "§" + group.charAt(2) + "§" + group.charAt(3)
                    + "§" + group.charAt(4) + "§" + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }
    public static void copyFromJarToDataFolder(Plugin plugin, String filename) throws IOException {
        File file = new File(plugin.getDataFolder() + "/" + filename);
        file.getParentFile().mkdirs();
        InputStream stream = plugin.getResource(filename);
        Files.copy(stream, file.toPath());
    }
}
