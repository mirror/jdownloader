package org.jdownloader.extensions.streaming.mediaarchive;

import jd.plugins.DownloadLink;

import org.jdownloader.extensions.streaming.dlna.profiles.audio.AbstractAudioProfile;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.AudioStream;

public class AudioMediaItem extends MediaItem {

    public AudioMediaItem(DownloadLink dl) {
        super(dl);
    }

    public void setStream(AudioStream as) {
        this.stream = as;
    }

    private AudioStream stream;

    public AudioStream getStream() {
        return stream;
    }

    @Override
    public String getMimeTypeString() {
        return "audio/" + getContainerFormat();
    }

    public void update(AudioMediaItem node) {
        super.update(node);
        this.stream = node.stream;

    }

    public ProfileMatch matches(AbstractAudioProfile p) {
        return null;
    }
}
