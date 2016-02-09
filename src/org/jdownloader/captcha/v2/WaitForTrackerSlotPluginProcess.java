package org.jdownloader.captcha.v2;

import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.translate._JDT;

import jd.nutils.Formatter;
import jd.plugins.PluginProgress;

public class WaitForTrackerSlotPluginProcess extends PluginProgress {
    /**
     *
     */

    private final String message;
    private String       pluginMessage;

    public WaitForTrackerSlotPluginProcess(long total, String message) {
        super(0, total, null);

        setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
        this.message = message;
        pluginMessage = _JDT.T.WaitForTrackerSlotPluginProcess(message, Formatter.formatSeconds(total / 1000));
    }

    @Override
    public PluginTaskID getID() {
        return PluginTaskID.WAIT_CAPTCHA_SLOT;
    }

    @Override
    public String getMessage(Object requestor) {
        return pluginMessage;
    }

    @Override
    public void setCurrent(long current) {
        if (current > 0) {
            pluginMessage = _JDT.T.WaitForTrackerSlotPluginProcess(message, Formatter.formatSeconds(current / 1000));
        }
        super.setCurrent(current);
    }
}