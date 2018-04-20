package org.jdownloader.plugins;

import jd.nutils.Formatter;
import jd.plugins.PluginProgress;

import org.appwork.utils.StringUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

public class SleepPluginProgress extends PluginProgress {
    /**
     *
     */
    private final String    message;
    private volatile String pluginMessage = null;

    public SleepPluginProgress(long total, String message) {
        super(0, total, null);
        setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
        if (StringUtils.isEmpty(message)) {
            this.message = null;
        } else {
            this.message = message;
        }
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