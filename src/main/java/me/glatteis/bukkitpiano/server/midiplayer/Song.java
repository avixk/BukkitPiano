package me.glatteis.bukkitpiano.server.midiplayer;

public class Song {
    String name;
    String fileName;
    int octave_offset;
    float volume_percent;
    String formattedName;
    boolean ignoreWarnings;
    public Song(String name, String fileName, String formattedName, int octave_offset, float volume_percent, boolean ignoreWarnings){
        this.name = name;
        this.fileName = fileName;
        this.volume_percent = volume_percent;
        this.octave_offset = octave_offset;
        this.formattedName = formattedName;
        this.ignoreWarnings = ignoreWarnings;
    }

    public String getName() {
        return name;
    }

    public int getOctave_offset() {
        return octave_offset;
    }

    public float getVolume_percent() {
        return volume_percent;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFormattedName() {
        return formattedName;
    }

    public boolean isIgnoreWarnings() {
        return ignoreWarnings;
    }
}
