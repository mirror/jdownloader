package org.jdownloader.controlling.filter;

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ArchiveExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.AudioExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.DocumentExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.HashExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ImageExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.VideoExtensions;
import org.jdownloader.gui.translate._GUI;

public class FiletypeFilter extends Filter implements Storable {
    public FiletypeFilter() {
        // Storable
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final List<String> cond = new ArrayList<String>();
        if (hashEnabled) {
            cond.add(HashExtensions.MD5.getDesc());
        }
        if (videoFilesEnabled) {
            cond.add(VideoExtensions.ASF.getDesc());
        }
        if (archivesEnabled) {
            cond.add(ArchiveExtensions.ACE.getDesc());
        }
        if (audioFilesEnabled) {
            cond.add(AudioExtensions.AAC.getDesc());
        }
        if (imagesEnabled) {
            cond.add(ImageExtensions.BMP.getDesc());
        }
        if (docFilesEnabled) {
            cond.add(DocumentExtensions.TXT.getDesc());
        }
        if (customs != null) {
            cond.add(_GUI.T.FiletypeFilter_toString_custom(customs));
        }
        switch (getMatchType()) {
        case IS:
            for (int i = 0; i < cond.size(); i++) {
                if (i > 0) {
                    if (i < cond.size() - 1) {
                        sb.append(_GUI.T.FilterRule_toString_comma3(cond.get(i)));
                    } else {
                        sb.append(" " + _GUI.T.FilterRule_toString_or(cond.get(i)).trim());
                    }
                } else {
                    sb.append(cond.get(i));
                }
            }
            return _GUI.T.FiletypeFilter_toString_(sb.toString());
        default:
            for (int i = 0; i < cond.size(); i++) {
                if (i > 0) {
                    if (i < cond.size() - 1) {
                        sb.append(_GUI.T.FilterRule_toString_comma3(cond.get(i)));
                    } else {
                        sb.append(" " + _GUI.T.FilterRule_toString_or(cond.get(i).trim()));
                    }
                } else {
                    sb.append(cond.get(i));
                }
            }
            return _GUI.T.FiletypeFilter_toString_not(sb.toString());
        }
    }

    private boolean       audioFilesEnabled;
    private TypeMatchType matchType = TypeMatchType.IS;
    private boolean       useRegex;

    public boolean isAudioFilesEnabled() {
        return audioFilesEnabled;
    }

    /**
     * @param typeMatchType
     * @param enabled
     * @param audioFilesEnabled
     * @param videoFilesEnabled
     * @param archivesEnabled
     * @param imagesEnabled
     * @param customs
     * @param regex
     */
    public FiletypeFilter(TypeMatchType typeMatchType, boolean enabled, boolean hashEnabled, boolean audioFilesEnabled, boolean videoFilesEnabled, boolean archivesEnabled, boolean imagesEnabled, final boolean docFilesEnabled, String customs, boolean regex) {
        super();
        this.enabled = enabled;
        this.audioFilesEnabled = audioFilesEnabled;
        this.videoFilesEnabled = videoFilesEnabled;
        this.archivesEnabled = archivesEnabled;
        this.imagesEnabled = imagesEnabled;
        this.customs = customs;
        this.useRegex = regex;
        this.hashEnabled = hashEnabled;
        this.matchType = typeMatchType;
        this.docFilesEnabled = docFilesEnabled;
    }

    public boolean isUseRegex() {
        return useRegex;
    }

    public void setUseRegex(boolean useRegex) {
        this.useRegex = useRegex;
    }

    public TypeMatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(TypeMatchType matchType) {
        this.matchType = matchType;
    }

    public static enum TypeMatchType {
        IS,
        IS_NOT
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

    private boolean hashEnabled;

    public boolean isHashEnabled() {
        return hashEnabled;
    }

    public void setHashEnabled(boolean hashEnabled) {
        this.hashEnabled = hashEnabled;
    }

    private boolean videoFilesEnabled;
    private boolean docFilesEnabled;

    public boolean isDocFilesEnabled() {
        return docFilesEnabled;
    }

    public void setDocFilesEnabled(boolean docFilesEnabled) {
        this.docFilesEnabled = docFilesEnabled;
    }

    private boolean archivesEnabled;
    private boolean imagesEnabled;
    private String  customs;
}
