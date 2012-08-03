package org.jdownloader.extensions.streaming.mediainfo;

public class VideoInfoTrack extends MediaInfoTrack {

    public VideoInfoTrack() {
        super("video");
    }

    public String getFormat() {
        return get("Format");
    }

    public String getCodec() {
        return get("Codec_ID");
    }

    public int getWidth() {
        try {

            return Integer.parseInt(get("Width").replaceAll("\\D", ""));
        } catch (Throwable e) {
            return -1;
        }
    }

    public int getHeight() {
        try {

            return Integer.parseInt(get("Height").replaceAll("\\D", ""));
        } catch (Throwable e) {
            return -1;
        }
    }

    public long getBitrate() {
        try {

            return Long.parseLong(get("Bit_rate").replaceAll("\\D", "")) * 1024;
        } catch (Throwable e) {
            return -1;
        }
    }

}
