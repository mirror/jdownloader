package jd.plugins;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class LinkStatus {
    private DownloadLink downloadLink;
    private int status = TODO;
    private String statusText=null;
    private int lastestStatus=  TODO;
    private String errorMessage;
    private int value;
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

//    /**
//     * Ein unbekannter Fehler ist aufgetreten. Der Download Soll wiederholt
//     * werden
//     */
//    public final static int ERROR__RETRY = 1 << 7;

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
    public static final int ERROR_LINK_IN_PROGRESS = 1<<19;
    public static final int ERROR_TIMEOUT_REACHED = 1<<20;
    public static final int ERROR_LOCAL_IO = 1<<21;

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
        return "linksatus";
        // String ret = "";
        // int speed;
        //
        // if (getRemainingWaittime() > 0) { return this.statusText + "Warten:
        // (" + JDUtilities.formatSeconds((int) (getRemainingWaittime() / 1000))
        // + "sek)"; }
        // if (this.isPluginActive() && (speed = getDownloadSpeed()) > 0) {
        // if (getDownloadMax() < 0) {
        // return JDUtilities.formatKbReadable(speed / 1024) + "/s " +
        // JDLocale.L("gui.download.filesize_unknown", "(Dateigröße
        // unbekannt)");
        // } else {
        // if (getDownloadSpeed() == 0) {
        //
        // if (this.downloadInstance != null && downloadInstance.getChunkNum() >
        // 1) {
        // return JDUtilities.formatKbReadable(speed / 1024) + "/s " + "(" +
        // downloadInstance.getChunksDownloading() + "/" +
        // downloadInstance.getChunkNum() + ")";
        // } else {
        // return JDUtilities.formatKbReadable(speed / 1024) + "/s ";
        // }
        // } else {
        // long remainingBytes = this.getDownloadMax() -
        // this.getDownloadCurrent();
        // long eta = remainingBytes / speed;
        // if (this.downloadInstance != null && downloadInstance.getChunkNum() >
        // 1) {
        // // logger.info("ETA " + JDUtilities.formatSeconds((int)
        // // eta) + " @ " + (speed / 1024) + " kb/s." + "(" +
        // // downloadInstance.getChunksDownloading() + "/" +
        // // downloadInstance.getChunks() + ")");
        // ret += "ETA " + JDUtilities.formatSeconds((int) eta) + " @ " +
        // JDUtilities.formatKbReadable(speed / 1024) + "/s " + "(" +
        // downloadInstance.getChunksDownloading() + "/" +
        // downloadInstance.getChunkNum() + ")";
        // } else {
        // ret += "ETA " + JDUtilities.formatSeconds((int) eta) + " @ " +
        // JDUtilities.formatKbReadable(speed / 1024) + "/s ";
        // }
        // if (this.statusText != null && this.statusText.length() > 0) {
        // ret += " [" + statusText + "]";
        // }
        // return ret;
        // }
        // }
        // }
        //
        // ret += this.toStatusString() + " ";
        // if (!this.isEnabled() && this.getStatus() != LinkStatus.FINISHED) {
        // ret += JDLocale.L("gui.downloadlink.disabled", "[deaktiviert]") + "
        // ";
        // this.getLinkStatus().setStatusText("");
        // return ret;
        // }
        // if (this.isAborted() && this.getStatus() != LinkStatus.FINISHED) {
        // ret += JDLocale.L("gui.downloadlink.aborted", "[abgebrochen]") + " ";
        // this.getLinkStatus().setStatusText("");
        // return ret;
        // }
        // if (this.isAvailabilityChecked() && !this.isAvailable() &&
        // this.getStatus() != LinkStatus.FINISHED) {
        // ret += JDLocale.L("gui.downloadlink.offline", "[offline]") + " ";
        // }
        //
        // // logger.info(statusText == null ? ret : ret + statusText);
        // if (this.getCrcStatus() == DownloadLink.CRC_STATUS_BAD) {
        // ret += "[" + JDLocale.L("gui.downloadlink.status.crcfailed",
        // "Checksum Fehler") + "] ";
        //
        // } else if (this.getCrcStatus() == DownloadLink.CRC_STATUS_OK) {
        // ret += "[" + JDLocale.L("gui.downloadlink.status.crcok", "Checksum
        // OK") + "] ";
        // }
        // if (statusText != null && ret.contains(statusText)) return ret;
        // return statusText == null ? ret : ret + statusText;

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

    /**
     * Setzt den Linkstatus. Es dürfen nur LInkStatus.*STATUS ids verwendet
     * werden
     * 
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
        this.lastestStatus=status;
    }

    /**
     * Fügt einen LinkStatus.* Status hinzu.Der alte status wird dabei nicht
     * gelöscht.
     * 
     * @param status
     */
    public void addStatus(int status) {
        this.status |= status;
        this.lastestStatus=status;

    }

    /** Entfernt eine Statusid */
    public void removeStatus(int status) {
        this.status ^= status;
    }

    /**
     * Gibt zurück ob der zugehörige Link einen bestimmten status hat.
     * 
     * @param status
     * @return
     */
    public boolean hasStatus(int status) {

        return (this.status | status) > 0;
    }

    public void setStatusText(String l) {
        this.statusText = l;

    }

    public boolean isFailed() {
        // TODO Auto-generated method stub
        return !this.isPluginActive() && !this.hasStatus(LinkStatus.FINISHED | LinkStatus.TODO);
    }

    public boolean isPluginActive() {
        this.addStatus(PLUGIN_IN_PROGRESS);
        return false;
    }

    public void setInProgress(boolean b) {
        this.removeStatus(PLUGIN_IN_PROGRESS);

    }

    public int getLatestStatus() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isStatus(int status) {
      return this.status==status;
    }

    public void setErrorMessage(String string) {
       this.errorMessage=string;
        
    }

    public void setValue(int i) {
    this.value=i;
        
    }

    public int getValue() {
        return value;
    }

    public void exceptionToErrorMessage(Exception e) {
        this.setErrorMessage(JDUtilities.convertExceptionReadable(e));
        
    }

}
