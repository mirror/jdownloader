package org.jdownloader.extensions.streaming.mediaarchive;

import jd.plugins.DownloadLink;

import org.jdownloader.extensions.streaming.mediaarchive.prepare.AudioStream;

public class AudioMediaItem extends MediaItem {

    private AudioStream stream;

    public AudioMediaItem(DownloadLink dl) {
        super(dl);
    }

    public void setStream(AudioStream as) {
        this.stream = as;
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
}
