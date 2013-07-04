package org.jdownloader.gui.views.downloads.columns;

import javax.swing.SwingConstants;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.translate._GUI;

public class DurationColumn extends ExtTextColumn<AbstractNode> {

    public DurationColumn() {
        super(_GUI._.DurationColumn_DurationColumn_object_());
        rendererField.setHorizontalAlignment(SwingConstants.CENTER);
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
            if (time > 0) { return TimeFormatter.formatMilliSeconds(time, 0); }
        }
        return null;
    }

}
