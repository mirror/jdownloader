package jd.plugins.download;

import java.awt.Color;

import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.PluginProgress;

import org.jdownloader.gui.views.downloads.columns.ProgressColumn;
import org.jdownloader.gui.views.downloads.columns.TaskColumn;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class DownloadPluginProgress extends PluginProgress {

    private final DownloadInterface downloadInterface;
    private final DownloadLink      link;
    private final String            unknownFileSize = _JDT._.gui_download_filesize_unknown() + " \u221E";
    private final long              startTimeStamp;
    private final String            normal;

    public DownloadPluginProgress(DownloadLink link, DownloadInterface downloadInterface, Color color) {
        super(0, 0, color);
        this.link = link;
        this.downloadInterface = downloadInterface;
        setIcon(NewTheme.I().getIcon("download", 16));
        startTimeStamp = downloadInterface.getStartTimeStamp();
        normal = _JDT._.download_connection_normal();
    }

    @Override
    public long getCurrent() {
        return downloadInterface.getTotalLinkBytesLoadedLive();
    };

    @Override
    public long getTotal() {
        return link.getKnownDownloadSize();
    }

    @Override
    public String getMessage(Object requestor) {
        if (requestor instanceof TaskColumn) { return normal; }
        long total = getTotal();
        if (requestor instanceof ProgressColumn) {
            if (total > 0) {
                return String.valueOf(getPercent());
            } else {
                return "~";
            }
        }
        if (total < 0) { return unknownFileSize; }
        long speed = getSpeed();
        if (speed > 0) {
            long remainingBytes = (getTotal() - getCurrent());
            if (remainingBytes > 0) {
                long eta = remainingBytes / speed;
                return Formatter.formatSeconds(eta);
            }
        }
        return null;
    }

    public long getDuration() {
        return System.currentTimeMillis() - startTimeStamp;
    }

    public long getSpeed() {
        return downloadInterface.getManagedConnetionHandler().getSpeed();
    }

}
