package jd.plugins;


 import org.jdownloader.translate.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import jd.nutils.Formatter;
import jd.utils.locale.JDL;

public class DownloadLinkInfo {

    private DownloadLink link = null;

    private String formattedSize = null;
    private String formattedLoaded = null;
    private String formattedRemaining = null;
    private String dateAdded = null;
    private String dateFinished = null;
    private String loadingFrom = null;
    private String statusString = null;
    private String formattedWaittime = null;
    private String progressString = null;
    private long lastReset = 0;
    private boolean dateAddedReset = false;
    private boolean dateFinishedReset = false;

    private static SimpleDateFormat dateFormat = null;

    static {
        try {
            dateFormat = new SimpleDateFormat(JDT._.jd_gui_swing_jdgui_views_downloadview_TableRenderer_TableRenderer_dateformat());
        } catch (Exception e) {
            dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm");
        }
    }

    public DownloadLinkInfo(DownloadLink link) {
        this.link = link;
    }

    public void reset(Long last) {
        if (lastReset < last) {
            formattedSize = null;
            formattedLoaded = null;
            formattedRemaining = null;
            if (dateAddedReset) dateAdded = null;
            if (dateFinishedReset) dateFinished = null;
            // loadingFrom = null;
            statusString = null;
            formattedWaittime = null;
            progressString = null;
            lastReset = last;
        }
    }

    public String getFormattedSize() {
        if (formattedSize != null) return formattedSize;
        if (link.getDownloadMax() <= 0) {
            formattedSize = "Unknown Filesize";
        } else {
            formattedSize = Formatter.formatReadable(link.getDownloadSize());
        }
        return formattedSize;
    }

    public String getFormattedLoaded() {
        if (formattedLoaded != null) return formattedLoaded;
        if (link.getDownloadCurrent() <= 0) {
            formattedLoaded = "0 B";
        } else {
            formattedLoaded = Formatter.formatReadable(link.getDownloadCurrent());
        }
        return formattedLoaded;
    }

    public String getFormattedRemaining() {
        if (formattedRemaining != null) return formattedRemaining;
        if (link.getDownloadMax() < 0) {
            formattedSize = "Unknown Filesize";
        } else {
            formattedRemaining = Formatter.formatReadable(link.getRemainingKB());
        }
        return formattedRemaining;
    }

    public String getFormattedWaittime() {
        if (formattedWaittime != null) return formattedWaittime;
        formattedWaittime = Formatter.formatSeconds(link.getLinkStatus().getRemainingWaittime() / 1000);
        return formattedWaittime;
    }

    public String getDateAdded() {
        if (dateAdded != null) return dateAdded;
        if (link.getCreated() <= 0) {
            dateAdded = "";
            dateAddedReset = true;
        } else {
            dateAddedReset = false;
            final Date date = new Date();
            date.setTime(link.getCreated());
            dateAdded = dateFormat.format(date);
        }
        return dateAdded;
    }

    public String getFinishedDate() {
        if (dateFinished != null) return dateFinished;
        if (link.getFinishedDate() <= 0) {
            dateFinished = "";
            dateFinishedReset = true;
        } else {
            dateFinishedReset = false;
            final Date date = new Date();
            date.setTime(link.getFinishedDate());
            dateFinished = dateFormat.format(date);
        }
        return dateFinished;
    }

    public String getLoadingFrom() {
        if (loadingFrom != null) return loadingFrom;
        return loadingFrom = JDT._.jd_gui_swing_jdgui_views_downloadview_TableRenderer_loadingFrom() + " " + link.getHost();
    }

    public String getStatusString() {
        if (statusString != null) return statusString;
        statusString = link.getLinkStatus().getStatusString();
        return statusString;
    }

    public String getProgressString() {
        if (progressString != null) return progressString;
        StringBuilder sb = new StringBuilder();
        sb.append(getFormattedLoaded()).append('/').append(getFormattedSize());
        progressString = sb.toString();
        return progressString;
    }
}