package org.jdownloader.extensions.streaming.mediaarchive.storage;

import jd.controlling.downloadcontroller.DownloadLinkStorable;

import org.appwork.storage.Storable;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;

public class MediaItemStorable implements Storable {
    private String[] actors;
    private String   album;

    private String   artist;

    private String   creator;

    private long     date = 0l;

    private String[] dlnaProfiles;

    private String[] genres;

    private String   majorBrand;

    public String[] getActors() {
        return actors;
    }

    public void setActors(String[] actors) {
        this.actors = actors;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String[] getDlnaProfiles() {
        return dlnaProfiles;
    }

    public void setDlnaProfiles(String[] dlnaProfiles) {
        this.dlnaProfiles = dlnaProfiles;
    }

    public String[] getGenres() {
        return genres;
    }

    public void setGenres(String[] genres) {
        this.genres = genres;
    }

    public String getMajorBrand() {
        return majorBrand;
    }

    public void setMajorBrand(String majorBrand) {
        this.majorBrand = majorBrand;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private String               title;
    private String               thumbnailPath;
    private DownloadLinkStorable downloadLink;
    private String               infoString;

    protected MediaItemStorable(/* Storable */) {

    }

    public DownloadLinkStorable getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(DownloadLinkStorable downloadLink) {
        this.downloadLink = downloadLink;
    }

    public void setInfoString(String result) {
        infoString = result;
    }

    public String getInfoString() {
        return infoString;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    private String containerFormat;
    private long   size = -1;

    public void setContainerFormat(String majorBrand) {
        this.containerFormat = majorBrand;

    }

    public String getContainerFormat() {
        return containerFormat;
    }

    public void setSize(long l) {
        this.size = l;
    }

    public long getSize() {

        return size;
    }

    protected void fillMediaItem(MediaItem ret) {
        ret.setActors(getActors());
        ret.setAlbum(getAlbum());
        ret.setArtist(getArtist());
        ret.setContainerFormat(getContainerFormat());
        ret.setCreator(getCreator());
        ret.setDate(getDate());
        ret.setDlnaProfiles(getDlnaProfiles());
        ret.setGenres(getGenres());
        ret.setInfoString(getInfoString());
        ret.setMajorBrand(getMajorBrand());
        ret.setSize(getSize());
        ret.setThumbnailPath(getThumbnailPath());
        ret.setTitle(getTitle());
    }

    protected void init(MediaItem mi) {
        setDownloadLink(new DownloadLinkStorable(mi.getDownloadLink()));
        setActors(mi.getActors());
        setAlbum(mi.getAlbum());
        setArtist(mi.getArtist());
        setContainerFormat(mi.getContainerFormat());
        setCreator(mi.getCreator());
        setDate(mi.getDate());
        setDlnaProfiles(mi.getDlnaProfiles());
        setGenres(mi.getGenres());
        setInfoString(mi.getInfoString());
        setMajorBrand(mi.getMajorBrand());
        setSize(mi.getSize());
        setThumbnailPath(mi.getThumbnailPath());
        setTitle(mi.getTitle());
    }
}
