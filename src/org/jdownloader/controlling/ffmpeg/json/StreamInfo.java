package org.jdownloader.controlling.ffmpeg.json;

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;

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

    public List<Stream> getVideoStreams() {
        return getStreams("video");
    }

    public List<Stream> getAudioStreams() {
        return getStreams("audio");
    }

    public List<Stream> getStreams(final String type) {
        final List<Stream> ret = new ArrayList<Stream>();
        final List<Stream> streams = getStreams();
        if (streams != null) {
            for (final Stream stream : streams) {
                if (StringUtils.equalsIgnoreCase(type, stream.getCodec_type())) {
                    ret.add(stream);
                }
            }
        }
        return ret;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    private Format format;
}
