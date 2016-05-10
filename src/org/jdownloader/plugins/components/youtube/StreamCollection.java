package org.jdownloader.plugins.components.youtube;

import java.util.ArrayList;

public class StreamCollection extends ArrayList<YoutubeStreamData> {
    private int audioBitrate = -1;

    public int getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

}
