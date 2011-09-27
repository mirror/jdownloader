package org.jdownloader.controlling.filter;

import java.util.ArrayList;

import org.appwork.storage.Storable;
import org.jdownloader.gui.translate._GUI;

public class FiletypeFilter extends Filter implements Storable {
    private FiletypeFilter() {
        // Storable
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        ArrayList<String> cond = new ArrayList<String>();
        if (archivesEnabled) {

            cond.add(_GUI._.FiletypeFilter_toString_archives());
        }
        if (audioFilesEnabled) {

            cond.add(_GUI._.FiletypeFilter_toString_audio());
        }

        if (imagesEnabled) {

            cond.add(_GUI._.FiletypeFilter_toString_image());
        }

        if (videoFilesEnabled) {

            cond.add(_GUI._.FiletypeFilter_toString_video());
        }
        if (customs != null) {

            cond.add(_GUI._.FiletypeFilter_toString_custom(customs));

        }

        for (int i = 0; i < cond.size(); i++) {
            if (i > 0) {
                if (i < cond.size() - 1) {
                    sb.append(_GUI._.FilterRule_toString_comma(cond.get(i)));
                } else {
                    sb.append(_GUI._.FilterRule_toString_or(cond.get(i)));
                }

            } else {
                sb.append(cond.get(i));
            }

        }
        return _GUI._.FiletypeFilter_toString_(sb.toString());

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
