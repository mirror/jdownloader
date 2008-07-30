package jd.plugins;

import java.io.Serializable;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class LinkStatus implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 3885661829491436448L;
    private DownloadLink downloadLink;
    private int status = TODO;
    private transient String statusText = null;
    private int lastestStatus = TODO;
    private transient String errorMessage;
    private int value;
    private long waitUntil = 0;
    private int totalWaitTime = 0;
    /**
     * Link muß noch bearbeitet werden
     */
    public final static int TODO = 1 << 0;

    /**
     * Link wurde erfolgreich heruntergeladen
     */
    public final static int FINISHED = 1 << 1;

    /**
     * Ein unbekannter Fehler ist aufgetreten
     */
    public final static int ERROR_RETRY = 1 << 2;

    /**
     * Captcha Text war falsch
     */
    public final static int ERROR_CAPTCHA_WRONG = 1 << 3;

    /**
     * Download Limit wurde erreicht
     */
    public final static int ERROR_TRAFFIC_LIMIT = 1 << 4;

    /**
     * Die Datei konnte nicht gefunden werden
     */
    public final static int ERROR_FILE_NOT_FOUND = 1 << 5;

    /**
     * Die Datei konnte nicht gefunden werden
     */
    public final static int ERROR_BOT_DETECTED = 1 << 6;

    // /**
    // * Ein unbekannter Fehler ist aufgetreten. Der Download Soll wiederholt
    // * werden
    // */

    /**
     * zeigt einen Premiumspezifischen fehler an
     */
    public static final int ERROR_PREMIUM = 1 << 8;

    /**
     * Zeigt an dass der Link nicht vollständig geladen wurde
     */
    public static final int ERROR_DOWNLOAD_INCOMPLETE = 1 << 9;

    /**
     * Zeigt an dass der Link gerade heruntergeladen wird
     */
    public static final int DOWNLOADINTERFACE_IN_PROGRESS = 1 << 10;

    /**
     * Der download ist zur Zeit nicht möglich
     */
    public static final int ERROR_TEMPORARILY_UNAVAILABLE = 1 << 11;

    /**
     * das PLugin meldet einen Fehler. Der Fehlerstring kann via Parameter
     * übergeben werden
     */
    public static final int ERROR_PLUGIN_SPECIFIC = 1 << 12;

    public static final int ERROR_ALREADYEXISTS = 1 << 13;

    public static final int ERROR_DOWNLOAD_FAILED = 1 << 14;

    public static final int ERROR_NO_CONNECTION = 1 << 15;

    public static final int ERROR_AGB_NOT_SIGNED = 1 << 16;

    /**
     * Schwerwiegender fehler. Der Download wird sofort abgebrochen. Es werden
     * keine weiteren versuche mehr gestartet
     */
    public static final int ERROR_FATAL = 1 << 17;
    /**
     * Ziegt an, dass das zugehörige PLugind en link gerade bearbeitet
     */
    public static final int PLUGIN_IN_PROGRESS = 1 << 18;
    public static final int ERROR_LINK_IN_PROGRESS = 1 << 19;
    public static final int ERROR_TIMEOUT_REACHED = 1 << 20;
    public static final int ERROR_LOCAL_IO = 1 << 21;

    public LinkStatus(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;

    }

    /**
     * Erstellt den Statustext, fügt eine eventl Wartezeit hzin und gibt diesen
     * Statusstrin (bevorzugt an die GUI) zurück
     * 
     * @return Statusstring mit eventl Wartezeit
     */

    public String getStatusText() {
        String ret = "";
        if (this.hasStatus(LinkStatus.FINISHED)) {

        return JDLocale.L("gui.downloadlink.finished", "[finished]"); }

        if (!downloadLink.isEnabled() && this.hasStatus(LinkStatus.FINISHED)) {
            ret += JDLocale.L("gui.downloadlink.disabled", "[deaktiviert]");
            if (this.errorMessage != null)
            ;
            ret += ": " + errorMessage;
            return errorMessage;

        }

        if (isFailed()) { return this.getErrorMessage(); }

        // String ret = "";

        //    
        if (hasStatus(ERROR_TRAFFIC_LIMIT) && getRemainingWaittime() > 0) {
            if (statusText == null) {
                ret = String.format(JDLocale.L("gui.download.waittime_status", "Wait %s min"), JDUtilities.formatSeconds((int) (getRemainingWaittime() / 1000)));
            } else {
                ret = String.format(JDLocale.L("gui.download.waittime_status", "Wait %s min"), JDUtilities.formatSeconds((int) (getRemainingWaittime() / 1000))) + statusText;

            }
            return ret;
        }

        // + "sek)"; }
if(downloadLink.getDownloadInstance()==null&&hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)){
    removeStatus(DOWNLOADINTERFACE_IN_PROGRESS);
}
        if (hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
            int speed = Math.max(0, downloadLink.getDownloadSpeed());
            String chunkString = "(" + downloadLink.getDownloadInstance().getChunksDownloading() + "/" + downloadLink.getDownloadInstance().getChunkNum() + ")";
            if (downloadLink.getDownloadMax() < 0) {
                return JDUtilities.formatKbReadable(speed / 1024) + "/s " + JDLocale.L("gui.download.filesize_unknown", "(Dateigröße unbekannt)");
            } else {
                if (speed > 0) {

                    long remainingBytes = downloadLink.getDownloadMax() - downloadLink.getDownloadCurrent();
                    long eta = remainingBytes / speed;
                    return "ETA " + JDUtilities.formatSeconds((int) eta) + " @ " + JDUtilities.formatKbReadable(speed/1024) + "/s " + chunkString;
                } else {
                    return JDUtilities.formatKbReadable(speed) + "/s " + chunkString;

                }

            }
        }

        if (this.statusText != null) { return statusText; }
        return "";

    }

    private String getErrorMessage() {
        String ret = errorMessage;
        if (ret == null) ret = this.getDefaultErrorMessage();
        if (ret == null) ret = JDLocale.L("downloadlink.status.error_unexpected", "Unexpected Error");
        return ret;
    }

    private String toStatusString() {
        switch (status) {
        case LinkStatus.FINISHED:
            return JDLocale.L("downloadlink.status.done", "Finished");
        case LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS:
            return JDLocale.L("downloadlink.status.downloadInProgress", "Loading");
        case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
            return JDLocale.L("downloadlink.status.incomplete", "Incomplete");
        case LinkStatus.TODO:
            return JDLocale.L("downloadlink.status.todo", "");
        case LinkStatus.ERROR_AGB_NOT_SIGNED:
            return JDLocale.L("downloadlink.status.error.agb_not_signed", "TOCs not signed");
        case LinkStatus.ERROR_ALREADYEXISTS:
            return JDLocale.L("downloadlink.status.error.file_exists", "File exists");
        case LinkStatus.ERROR_BOT_DETECTED:
            return JDLocale.L("downloadlink.status.error.bot_detected", "Bot detected");
            // case LinkStatus.ERROR_CAPTCHA_IMAGEERROR:
            // return
            // JDLocale.L("downloadlink.status.error.captcha_image_error",
            // "Captcha Img Error");
        case LinkStatus.ERROR_CAPTCHA_WRONG:
            return JDLocale.L("downloadlink.status.error.captcha_wrong", "Captcha wrong");
        case LinkStatus.ERROR_DOWNLOAD_FAILED:
            return JDLocale.L("downloadlink.status.error.downloadfailed", "Download failed");
        case LinkStatus.ERROR_TRAFFIC_LIMIT:
            return JDLocale.L("downloadlink.status.error.download_limit", "Download Limit reached");
            // case LinkStatus.ERROR_FILE_NOT_FOUND:
            // return JDLocale.L("downloadlink.status.error.file_abused", "File
            // abused");
        case LinkStatus.ERROR_FILE_NOT_FOUND:
            return JDLocale.L("downloadlink.status.error.file_not_found", "File not found");

            // case LinkStatus.ERROR_NO_FREE_SPACE:
            // return JDLocale.L("downloadlink.status.error.no_free_space", "No
            // Free Space");
        case LinkStatus.ERROR_NO_CONNECTION:
            return JDLocale.L("downloadlink.status.error.no_connection", "No Connection");
            // case LinkStatus.ERROR_LINK_IN_PROGRESS:
            // return JDLocale.L("downloadlink.status.error.not_owner", "Link is
            // already in progress");
        case LinkStatus.ERROR_PLUGIN_SPECIFIC:
            return JDLocale.L("downloadlink.status.error.plugin_specific", "Plg.:");
        case LinkStatus.ERROR_PREMIUM:
            return JDLocale.L("downloadlink.status.error.premium", "Premium Error");
            // case LinkStatus.ERROR_SECURITY:
            // return JDLocale.L("downloadlink.status.error.security",
            // "Read/Write Error");
            // case LinkStatus.ERROR_TRAFFIC_LIMIT:
            // return JDLocale.L("downloadlink.status.error.static_wait",
            // "Waittime");
        case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
            return JDLocale.L("downloadlink.status.error.temp_unavailable", "Temp. unavailable");
            // case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
            // return JDLocale.L("downloadlink.status.error.many_users", "Too
            // many User");
        case LinkStatus.ERROR_RETRY:
            return JDLocale.L("downloadlink.status.error.unknown", "Unknown Error");
        case LinkStatus.ERROR_FATAL:
            return JDLocale.L("downloadlink.status.error.fatal", "Fatal Error");
        default:
            return JDLocale.L("downloadlink.status.error_def", "Error");
        }

    }

    private String getDefaultErrorMessage() {
        switch (this.lastestStatus) {

        case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
            return JDLocale.L("downloadlink.status.incomplete", "Incomplete");

        case LinkStatus.ERROR_AGB_NOT_SIGNED:
            return JDLocale.L("downloadlink.status.error.agb_not_signed", "TOCs not signed");
        case LinkStatus.ERROR_ALREADYEXISTS:
            return JDLocale.L("downloadlink.status.error.file_exists", "File exists");
        case LinkStatus.ERROR_BOT_DETECTED:
            return JDLocale.L("downloadlink.status.error.bot_detected", "Bot detected");

        case LinkStatus.ERROR_CAPTCHA_WRONG:
            return JDLocale.L("downloadlink.status.error.captcha_wrong", "Captcha wrong");
        case LinkStatus.ERROR_DOWNLOAD_FAILED:
            return JDLocale.L("downloadlink.status.error.downloadfailed", "Download failed");
        case LinkStatus.ERROR_TRAFFIC_LIMIT:
            return JDLocale.L("downloadlink.status.error.download_limit", "Download Limit reached");

        case LinkStatus.ERROR_FILE_NOT_FOUND:
            return JDLocale.L("downloadlink.status.error.file_not_found", "File not found");

        case LinkStatus.ERROR_NO_CONNECTION:
            return JDLocale.L("downloadlink.status.error.no_connection", "No Connection");

        case LinkStatus.ERROR_PREMIUM:
            return JDLocale.L("downloadlink.status.error.premium", "Premium Error");

        case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
            return JDLocale.L("downloadlink.status.error.temp_unavailable", "Temp. unavailable");

        case LinkStatus.ERROR_FATAL:
            return JDLocale.L("downloadlink.status.error.fatal", "Fatal Error");

        }
        return null;

    }

    /**
     * Setzt den Linkstatus. Es dürfen nur LInkStatus.*STATUS ids verwendet
     * werden
     * 
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
        this.lastestStatus = status;
        System.out.println("");
    }

    /**
     * Fügt einen LinkStatus.* Status hinzu.Der alte status wird dabei nicht
     * gelöscht.
     * 
     * @param status
     */
    public void addStatus(int status) {
        this.status |= status;
        this.lastestStatus = status;

        System.out.println("");

    }

    /** Entfernt eine Statusid */
    public void removeStatus(int status) {
        int mask = 0xffffffff;
        mask &= ~status;
        this.status &= mask;
    }

    /**
     * Gibt zurück ob der zugehörige Link einen bestimmten status hat.
     * 
     * @param status
     * @return
     */
    public boolean hasStatus(int status) {

        return (this.status & status) > 0;
    }

    public void setStatusText(String l) {
        this.statusText = l;

    }

    public boolean isFailed() {

        return !this.hasStatus(LinkStatus.FINISHED | LinkStatus.TODO | LinkStatus.ERROR_LINK_IN_PROGRESS | LinkStatus.ERROR_TRAFFIC_LIMIT);
    }

    public boolean isPluginActive() {
        return this.hasStatus(PLUGIN_IN_PROGRESS);

    }

    public void setInProgress(boolean b) {
        if (b) {
            this.addStatus(PLUGIN_IN_PROGRESS);
        } else {
            this.removeStatus(PLUGIN_IN_PROGRESS);
        }

    }

    public int getLatestStatus() {

        return this.lastestStatus;
    }

    public boolean isStatus(int status) {
        return this.status == status;
    }

    public void setErrorMessage(String string) {
        this.errorMessage = string;

    }

    public void setValue(int i) {
        this.value = i;

    }

    public int getValue() {
        return value;
    }

    public void exceptionToErrorMessage(Exception e) {
        this.setErrorMessage(JDUtilities.convertExceptionReadable(e));

    }

    public String toString() {
        return Integer.toBinaryString(status);
    }

    public void setWaitTime(int milliSeconds) {
        this.waitUntil = System.currentTimeMillis() + milliSeconds;
        this.totalWaitTime = milliSeconds;

    }

    public void reset() {
        setStatus(TODO);
        waitUntil = 0;
        this.errorMessage = null;
        this.statusText = null;
        totalWaitTime = 0;
    }

    public int getRemainingWaittime() {

        return Math.max(0, (int) (waitUntil - System.currentTimeMillis()));
    }

    public int getTotalWaitTime() {

        return totalWaitTime;
    }

    public void resetWaitTime() {
        totalWaitTime = 0;
        waitUntil = 0;
       ((PluginForHost)downloadLink.getPlugin()).resetHosterWaitTime();
    }

}
