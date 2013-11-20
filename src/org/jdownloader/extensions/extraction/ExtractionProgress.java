package org.jdownloader.extensions.extraction;

import java.awt.Color;

import jd.plugins.PluginProgress;

import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.images.NewTheme;

public class ExtractionProgress extends PluginProgress {

    protected long   lastCurrent    = -1;
    protected long   lastTotal      = -1;
    protected long   startTimeStamp = -1;
    protected String message        = null;

    public ExtractionProgress(long current, long total, Color color) {
        super(current, total, color);
        setIcon(NewTheme.I().getIcon(org.jdownloader.gui.IconKey.ICON_COMPRESS, 16));
        message = T._.plugins_optional_extraction_status_extracting2();
    }

    @Override
    public void updateValues(long current, long total) {
        super.updateValues(current, total);
        if (startTimeStamp == -1 || lastTotal == -1 || lastTotal != total || lastCurrent == -1 || lastCurrent > current) {
            lastTotal = total;
            lastCurrent = current;
            startTimeStamp = System.currentTimeMillis();
            this.setETA(-1);
            return;
        }
        long currentTimeDifference = System.currentTimeMillis() - startTimeStamp;
        if (currentTimeDifference <= 0) return;
        long speed = (current * 10000) / currentTimeDifference;
        if (speed == 0) return;
        long eta = ((total - current) * 10000) / speed;
        this.setETA(eta);
    }

    @Override
    public String getMessage(Object requestor) {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
