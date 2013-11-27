package org.jdownloader.controlling.ffmpeg;

import javax.swing.Icon;

import jd.plugins.PluginProgress;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.gui.views.downloads.columns.ProgressColumn;
import org.jdownloader.images.AbstractIcon;

public class FFMpegProgress extends PluginProgress {

    private String message;

    public FFMpegProgress() {
        super(0, 100, null);
        setIcon(new AbstractIcon("ffmpeg", 18));

    }

    @Override
    public Icon getIcon(Object requestor) {
        if (requestor instanceof ETAColumn) return null;
        return super.getIcon(requestor);
    }

    @Override
    public String getMessage(Object requestor) {
        if (requestor instanceof ETAColumn) return null;
        if (requestor instanceof ProgressColumn) return null;
        return _GUI._.FFMpegProgress_getMessage_merging_();
    }

}
