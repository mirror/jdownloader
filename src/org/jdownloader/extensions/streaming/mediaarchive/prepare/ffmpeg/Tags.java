package org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg;

import org.appwork.storage.Storable;

public class Tags implements Storable {

    private Tags(/* Storable */) {

    }

    private String title;
    private String artist;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

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

    private String album;

}
