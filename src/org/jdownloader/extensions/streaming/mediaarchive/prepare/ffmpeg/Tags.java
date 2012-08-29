package org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg;

import org.appwork.storage.Storable;

public class Tags implements Storable {

    private Tags(/* Storable */) {

    }

    private String major_brand;

    public String getMajor_brand() {
        return major_brand;
    }

    public void setMajor_brand(String major_brand) {
        this.major_brand = major_brand;
    }

    public String getMinor_version() {
        return minor_version;
    }

    public void setMinor_version(String minor_version) {
        this.minor_version = minor_version;
    }

    public String getCompatible_brands() {
        return compatible_brands;
    }

    public void setCompatible_brands(String compatible_brands) {
        this.compatible_brands = compatible_brands;
    }

    public String getCreation_time() {
        return creation_time;
    }

    public void setCreation_time(String creation_time) {
        this.creation_time = creation_time;
    }

    private String minor_version;
    private String compatible_brands;
    private String creation_time;
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
