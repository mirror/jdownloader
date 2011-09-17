package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;

public class FiletypeFilter extends Filter implements Storable {
    private FiletypeFilter() {
        // Storable
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        return "Type is ***";
    }

    private boolean audioFilesEnabled;

    public boolean isAudioFilesEnabled() {
        return audioFilesEnabled;
    }

    /**
     * @param enabled
     * @param audioFilesEnabled
     * @param videoFilesEnabled
     * @param archivesEnabled
     * @param imagesEnabled
     * @param customs
     */
    public FiletypeFilter(boolean enabled, boolean audioFilesEnabled, boolean videoFilesEnabled, boolean archivesEnabled, boolean imagesEnabled, String customs) {
        super();
        this.enabled = enabled;
        this.audioFilesEnabled = audioFilesEnabled;
        this.videoFilesEnabled = videoFilesEnabled;
        this.archivesEnabled = archivesEnabled;
        this.imagesEnabled = imagesEnabled;
        this.customs = customs;
    }

    public void setAudioFilesEnabled(boolean audioFilesEnabled) {
        this.audioFilesEnabled = audioFilesEnabled;
    }

    public boolean isVideoFilesEnabled() {
        return videoFilesEnabled;
    }

    public void setVideoFilesEnabled(boolean videoFilesEnabled) {
        this.videoFilesEnabled = videoFilesEnabled;
    }

    public boolean isArchivesEnabled() {
        return archivesEnabled;
    }

    public void setArchivesEnabled(boolean archivesEnabled) {
        this.archivesEnabled = archivesEnabled;
    }

    public boolean isImagesEnabled() {
        return imagesEnabled;
    }

    public void setImagesEnabled(boolean imagesEnabled) {
        this.imagesEnabled = imagesEnabled;
    }

    public String getCustoms() {
        return customs;
    }

    public void setCustoms(String customs) {
        this.customs = customs;
    }

    private boolean videoFilesEnabled;
    private boolean archivesEnabled;
    private boolean imagesEnabled;
    private String  customs;
}
