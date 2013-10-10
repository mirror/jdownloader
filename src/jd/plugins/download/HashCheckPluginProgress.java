package jd.plugins.download;

import java.awt.Color;
import java.io.File;

import jd.nutils.Formatter;
import jd.plugins.PluginProgress;
import jd.plugins.download.raf.HashResult;

import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class HashCheckPluginProgress extends PluginProgress {

    private final String message;
    private long         lastCurrent    = -1;
    private long         startTimeStamp = -1;

    public HashCheckPluginProgress(File file, Color color, HashResult.TYPE type) {
        super(0, file.length(), color);
        setIcon(NewTheme.I().getIcon("hashsum", 16));
        message = _JDT._.system_download_doCRC2(type.name());
    }

    @Override
    public String getMessage(Object requestor) {
        if (requestor instanceof ETAColumn) { return Formatter.formatSeconds(getETA()); }
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
        if (currentTimeDifference <= 0) return;
        long speed = (current * 10000) / currentTimeDifference;
        if (speed == 0) return;
        long eta = ((total - current) * 10000) / speed;
        this.setETA(eta);
    }

}
