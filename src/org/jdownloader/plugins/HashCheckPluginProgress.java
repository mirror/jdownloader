package org.jdownloader.plugins;

import java.awt.Color;
import java.io.File;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

import jd.plugins.PluginProgress;
import jd.plugins.download.HashInfo.TYPE;

public class HashCheckPluginProgress extends PluginProgress {

    private final String message;
    private long         lastCurrent    = -1;
    private long         startTimeStamp = -1;
    private final TYPE   type;

    public HashCheckPluginProgress(File file, Color color, TYPE type) {
        super(0, file != null ? file.length() : 1, color);
        setIcon(new AbstractIcon(IconKey.ICON_HASHSUM, 16));
        this.type = type;
        if (type != null) {
            message = _JDT.T.system_download_doCRC2(type.name());
        } else {
            message = _JDT.T.system_download_doCRC2_waiting();
        }
    }

    @Override
    public PluginTaskID getID() {
        return PluginTaskID.HASH;
    }

    @Override
    public String getMessage(Object requestor) {
        if (requestor instanceof ETAColumn) {
            if (type != null) {
                return TimeFormatter.formatMilliSeconds(getETA(), 0);
            } else {
                return "";
            }
        }
        return message;
    }

    @Override
    public void setCurrent(long current) {
        super.setCurrent(current);

        if (lastCurrent == -1 || lastCurrent > current) {
            lastCurrent = current;
            startTimeStamp = System.currentTimeMillis();
            this.setETA(-1);
            return;
        }
        long currentTimeDifference = System.currentTimeMillis() - startTimeStamp;
        if (currentTimeDifference <= 0) {
            return;
        }
        long speed = (current * 10000) / currentTimeDifference;
        if (speed == 0) {
            return;
        }
        long eta = ((total - current) * 10000) / speed;
        this.setETA(eta);

    }

}
