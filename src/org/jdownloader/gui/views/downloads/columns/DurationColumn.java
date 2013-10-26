package org.jdownloader.gui.views.downloads.columns;

import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.PluginProgress;
import jd.plugins.download.DownloadPluginProgress;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.translate._GUI;

public class DurationColumn extends ExtTextColumn<AbstractNode> {

    public DurationColumn() {
        super(_GUI._.DurationColumn_DurationColumn_object_());
        rendererField.setHorizontalAlignment(SwingConstants.CENTER);
    }

    public JPopupMenu createHeaderPopup() {

        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);

    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 75;
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            long time = ((DownloadLink) value).getDownloadTime();
            PluginProgress progress = ((DownloadLink) value).getPluginProgress();
            if (progress instanceof DownloadPluginProgress) {
                time = time + ((DownloadPluginProgress) progress).getDuration();
            }
            if (time > 0) { return TimeFormatter.formatMilliSeconds(time, 0); }
        }
        return null;
    }

}
