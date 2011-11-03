package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.gui.translate._GUI;

public class FilesizeFilter extends Filter implements Storable {
    private long          from;
    private SizeMatchType matchType = SizeMatchType.BETWEEN;

    private FilesizeFilter() {
        // Storable
    }

    public static enum SizeMatchType {
        BETWEEN,
        NOT_BETWEEN
    }

    public String toString() {
        switch (getMatchType()) {
        case BETWEEN:
            if (from == to) {
                return _GUI._.FilesizeFilter_toString_same(SizeFormatter.formatBytes(from));
            } else {
                return _GUI._.FilesizeFilter_toString_(SizeFormatter.formatBytes(from), SizeFormatter.formatBytes(to));
            }

        default:
            if (from == to) {
                return _GUI._.FilesizeFilter_toString_same_not(SizeFormatter.formatBytes(from));
            } else {
                return _GUI._.FilesizeFilter_toString_not(SizeFormatter.formatBytes(from), SizeFormatter.formatBytes(to));
            }
        }

    }

    /**
     * @param from
     * @param to
     * @param enabled
     * @param matchType
     */
    public FilesizeFilter(long from, long to, boolean enabled, SizeMatchType matchType) {
        super();
        this.from = from;
        this.to = to;
        this.enabled = enabled;
        this.matchType = matchType;
    }

    public SizeMatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(SizeMatchType matchType) {
        this.matchType = matchType;
    }

    private long to;

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

}
