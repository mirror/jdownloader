package jd.plugins;

import java.text.SimpleDateFormat;
import java.util.Date;

import jd.nutils.Formatter;
import jd.utils.locale.JDL;

public class FilePackageInfo {

    private FilePackage fp = null;

    private String formattedSize = null;
    private String formattedLoaded = null;
    private String formattedRemaining = null;
    private String dateAdded = null;
    private String dateFinished = null;
    private String size = null;
    private String statusString = null;
    private String progressString = null;
    private long lastReset = 0;
    private boolean dateAddedReset = false;
    private boolean dateFinishedReset = false;

    private static SimpleDateFormat dateFormat = null;
    private static String strDownloadLinkActive = JDL.L("gui.treetable.packagestatus.links_active", "Active");
    private static String strETA = JDL.L("gui.eta", "ETA");

    static {
        try {
            dateFormat = new SimpleDateFormat(JDL.L("jd.gui.swing.jdgui.views.downloadview.TableRenderer.TableRenderer.dateformat", "dd.MM.yy HH:mm"));
        } catch (Exception e) {
            dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm");
        }
    }

    public FilePackageInfo(FilePackage fp) {
        this.fp = fp;
    }

    public void reset(Long last) {
        if (lastReset < last) {
            formattedSize = null;
            formattedLoaded = null;
            formattedRemaining = null;
            if (dateAddedReset) dateAdded = null;
            if (dateFinishedReset) dateFinished = null;
            statusString = null;
            progressString = null;
            lastReset = last;
            size = null;
        }
    }

    public String getFormattedSize() {
        if (formattedSize != null) return formattedSize;
        if (fp.getTotalEstimatedPackageSize() < 0) {
            formattedSize = "Unknown Filesize";
        } else {
            formattedSize = Formatter.formatReadable(fp.getTotalEstimatedPackageSize());
        }
        return formattedSize;
    }

    public String getFormattedLoaded() {
        if (formattedLoaded != null) return formattedLoaded;
        if (fp.getTotalKBLoaded() <= 0) {
            formattedLoaded = "0 B";
        } else {
            formattedLoaded = Formatter.formatReadable(fp.getTotalKBLoaded());
        }
        return formattedLoaded;
    }

    public String getFormattedRemaining() {
        if (formattedRemaining != null) return formattedRemaining;
        formattedRemaining = Formatter.formatReadable(fp.getRemainingKB());
        return formattedRemaining;
    }

    public String getDateAdded() {
        if (dateAdded != null) return dateAdded;
        if (fp.getCreated() <= 0) {
            dateAdded = "";
            dateAddedReset = true;
        } else {
            dateAddedReset = false;
            final Date date = new Date();
            date.setTime(fp.getCreated());
            dateAdded = dateFormat.format(date);
        }
        return dateAdded;
    }

    public String getFinishedDate() {
        if (dateFinished != null) return dateFinished;
        if (fp.getFinishedDate() <= 0) {
            dateFinished = "";
            dateFinishedReset = true;
        } else {
            dateFinishedReset = false;
            final Date date = new Date();
            date.setTime(fp.getFinishedDate());
            dateFinished = dateFormat.format(date);
        }
        return dateFinished;
    }

    public String getSize() {
        if (size != null) return size;
        size = "[" + fp.size() + "]";
        return size;
    }

    public String getStatusString() {
        if (statusString != null) return statusString;
        StringBuilder sb = new StringBuilder();
        if (fp.getTotalDownloadSpeed() > 0) {
            sb.append('[').append(fp.getLinksInProgress()).append('/').append(fp.size()).append("] ");
            sb.append(strETA).append(' ').append(Formatter.formatSeconds(fp.getETA())).append(" @ ").append(Formatter.formatReadable(fp.getTotalDownloadSpeed())).append("/s");
        } else if (fp.getLinksInProgress() > 0) {
            sb.append(fp.getLinksInProgress()).append('/').append(fp.size()).append(' ').append(strDownloadLinkActive);
        }
        statusString = sb.toString();
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
