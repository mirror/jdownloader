package org.jdownloader.controlling.ffmpeg.json;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class StreamInfo implements Storable {
    private StreamInfo(/* storable */) {

    }

    private ArrayList<Stream> streams;

    public ArrayList<Stream> getStreams() {
        return streams;
    }

    public void setStreams(ArrayList<Stream> streams) {
        this.streams = streams;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    private Format format;
}
