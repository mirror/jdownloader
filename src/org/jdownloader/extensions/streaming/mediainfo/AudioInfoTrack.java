package org.jdownloader.extensions.streaming.mediainfo;

public class AudioInfoTrack extends MediaInfoTrack {

    public AudioInfoTrack() {
        super("audio");
    }

    public String getFormat() {
        return get("Format");
    }

    public byte getChannels() {
        try {
            return Byte.parseByte(get("Channel_s_"));
        } catch (Throwable e) {
            return -1;
        }
    }

    public byte getBitDepth() {
        try {
            return Byte.parseByte(get("Bit_depth"));
        } catch (Throwable e) {
            return -1;
        }
    }
}
