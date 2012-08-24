package org.jdownloader.extensions.streaming.mediaarchive.storage;

import jd.controlling.downloadcontroller.DownloadLinkStorable;

import org.appwork.storage.Storable;
import org.jdownloader.extensions.streaming.mediaarchive.AudioMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.AudioStream;

public class AudioItemStorable extends MediaItemStorable implements Storable {

    protected AudioItemStorable(/* storable */) {
        super();
    }

    private AudioStream stream;

    public void setStream(AudioStream stream) {
        this.stream = stream;
    }

    private String artist;

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public AudioStream getStream() {
        return stream;
    }

    private String album;
    private String title;

    public static AudioItemStorable create(AudioMediaItem mi) {
        AudioItemStorable ret = new AudioItemStorable();
        ret.setStream(mi.getStream());
        ret.setAlbum(mi.getAlbum());
        ret.setArtist(mi.getArtist());
        ret.setThumbnailPath(mi.getThumbnailPath());
        ret.setDownloadLink(new DownloadLinkStorable(mi.getDownloadLink()));
        ret.setTitle(mi.getTitle());
        ret.setSize(mi.getSize());
        ret.setContainerFormat(mi.getContainerFormat());
        return ret;
    }

    public AudioMediaItem toAudioMediaItem() {

        AudioMediaItem ret = new AudioMediaItem(getDownloadLink()._getDownloadLink());
        ret.setThumbnailPath(getThumbnailPath());
        ret.setAlbum(getAlbum());
        ret.setArtist(getArtist());
        ret.setSize(getSize());
        ret.setContainerFormat(getContainerFormat());
        ret.setTitle(getTitle());
        ret.setStream(getStream());

        return ret;
    }
}
