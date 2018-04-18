package org.jdownloader.plugins;

import jd.nutils.Formatter;
import jd.plugins.PluginProgress;

import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

public class SleepPluginProgress extends PluginProgress {
    /**
     *
     */
    private final String message;
    private String       pluginMessage;

    public SleepPluginProgress(long total, String message) {
        super(0, total, null);
        setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
        this.message = message;
        pluginMessage = message;
    }

    @Override
    public PluginTaskID getID() {
        return PluginTaskID.WAIT;
    }

    @Override
    public String getMessage(Object requestor) {
        if (message != null) {
            return message;
        } else {
            return pluginMessage;
        }
    }

    @Override
    public void setCurrent(long current) {
        if (current > 0) {
            pluginMessage = _JDT.T.gui_download_waittime_status2(Formatter.formatSeconds(current / 1000));
        } else {
            pluginMessage = null;
        }
        super.setCurrent(current);
    }
}