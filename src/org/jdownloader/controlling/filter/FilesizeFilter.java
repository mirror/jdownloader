package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.gui.translate._GUI;

public class FilesizeFilter extends Filter implements Storable {
    private long from;

    private FilesizeFilter() {
        // Storable
    }

    public String toString() {
        if (from == to) {
            return _GUI._.FilesizeFilter_toString_same(SizeFormatter.formatBytes(from));
        } else {
            return _GUI._.FilesizeFilter_toString_(SizeFormatter.formatBytes(from), SizeFormatter.formatBytes(to));
        }
    }

    /**
     * @param from
     * @param to
     * @param enabled
     */
    public FilesizeFilter(long from, long to, boolean enabled) {
        super();
        this.from = from;
        this.to = to;
        this.enabled = enabled;
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
