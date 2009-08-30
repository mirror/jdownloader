//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins;

import java.io.Serializable;
import java.lang.reflect.Field;

import javax.swing.ImageIcon;

import jd.controlling.JDLogger;
import jd.nutils.Formatter;
import jd.nutils.JDFlags;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class LinkStatus implements Serializable {
    /**
     * Controlling Zeigt an, dass der Link gerade heruntergeladen wird
     */
    public static final int DOWNLOADINTERFACE_IN_PROGRESS = 1 << 10;

    /**
     * Controlling Die AGB wurde noch nicht unterzeichnet.
     */
    public static final int ERROR_AGB_NOT_SIGNED = 1 << 16;
    /**
     * Controlling,Downloadinterface Zeigt an, dass die Datei auf der Festplatte
     * schon existiert
     */
    public static final int ERROR_ALREADYEXISTS = 1 << 13;

    /**
     * PLugins: Captcha Text war falsch
     */
    public final static int ERROR_CAPTCHA = 1 << 3;
    /**
     * Downloadinterface Zeigt an dass der Eigentliche Download im
     * Downloadinterface fehlgeschlagen ist. z.B. Misslungender Chunkload
     */
    public static final int ERROR_DOWNLOAD_FAILED = 1 << 14;

    /**
     * Downloadinterface Zeigt an dass der Link nicht vollständig geladen wurde
     */
    public static final int ERROR_DOWNLOAD_INCOMPLETE = 1 << 9;
    /**
     * Plugins & Downloadinterface Schwerwiegender fehler. Der Download wird
     * sofort abgebrochen. Es werden keine weiteren versuche mehr gestartet
     */
    public static final int ERROR_FATAL = 1 << 17;

    /**
     * Plugins & Downloadinterface: Die Datei konnte nicht gefunden werden
     */
    public final static int ERROR_FILE_NOT_FOUND = 1 << 5;

    /**
     * Plugins: Download Limit wurde erreicht
     */
    public final static int ERROR_IP_BLOCKED = 1 << 4;
    /**
     * Conttrolling, Downloadinterface, Plugins Zeigt an, dass gerade ein
     * anderes Plugin an der Lokalen Datei arbeitet. Wird eingesetzt um dem
     * Controller mitzuteilen, dass bereits ein Mirror dieser Datei geladen
     * wird.
     */
    public static final int ERROR_LINK_IN_PROGRESS = 1 << 19;

    /**
     * Downloadinterface LOCAL Input output Fehler. Es kann nicht geschrieben
     * werden etc.
     */
    public static final int ERROR_LOCAL_IO = 1 << 21;

    /**
     * DownloadInterface Zeigt an dass es einen Timeout gab und es scheinbar
     * keine Verbindung emhr zum internet gibt
     */
    public static final int ERROR_NO_CONNECTION = 1 << 15;

    /**
     * Plugins Wird bei Schwerenb Parsing fehler eingesetzt. Über diesen Code
     * kann das Plugin mitteilen dass es defekt ist und aktualisiert werden muss
     */
    public static final int ERROR_PLUGIN_DEFEKT = 1 << 22;

    /**
     * Plugins | Controlling zeigt einen Premiumspezifischen fehler an
     */
    public static final int ERROR_PREMIUM = 1 << 8;

    /**
     * Plugins: Ein unbekannter Fehler ist aufgetreten
     */
    public final static int ERROR_RETRY = 1 << 2;

    /**
     * PLugins Der download ist zur Zeit nicht möglich
     */
    public static final int ERROR_TEMPORARILY_UNAVAILABLE = 1 << 11;

    /**
     * DownloadINterface & Controlling zeigt an dass es zu einem plugintimeout
     * gekommen ist
     */
    public static final int ERROR_TIMEOUT_REACHED = 1 << 20;

    /**
     * Controlling & Downloadinterface: Link wurde erfolgreich heruntergeladen
     */
    public final static int FINISHED = 1 << 1;

    /**
     * Controlling Ziegt an, dass das zugehörige Plugin den link gerade
     * bearbeitet
     */
    public static final int PLUGIN_IN_PROGRESS = 1 << 18;

    public static final int PLUGIN_ACTIVE = 1 << 29;

    /**
     * Zeigt an, das auf User-Eingaben gewartet wird
     */
    public static final int WAITING_USERIO = 1 << 23;
    public static final int ERROR_POST_PROCESS = 1 << 24;
    private static final long serialVersionUID = 3885661829491436448L;

    /**
     * Controlling: Link muß noch bearbeitet werden.
     */
    public final static int TODO = 1 << 0;
    public static final int VALUE_ID_PREMIUM_TEMP_DISABLE = 0;
    public static final int VALUE_ID_PREMIUM_DISABLE = 1;
    public static final int VALUE_FAILED_CHUNK = 1 << 26;
    public static final int VALUE_FAILED_HASH = 1 << 27;

    private DownloadLink downloadLink;
    private String errorMessage;

    private int lastestStatus = TODO;
    private int status = TODO;
    private String statusText = null;
    private long totalWaitTime = 0;
    private long value = 0;
    private long waitUntil = 0;
    private int retryCount = 0;

    private ImageIcon statusIcon;

    public LinkStatus(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
    }

    /**
     * Fügt einen LinkStatus.* Status hinzu.Der alte status wird dabei nicht
     * gelöscht.
     * 
     * @param status
     */
    public void addStatus(int status) {
        this.status |= status;
        if (JDFlags.hasSomeFlags(status, FINISHED)) {
            if (downloadLink.getFinishedDate() != -1) downloadLink.setFinishedDate(System.currentTimeMillis());
        }
        lastestStatus = status;
    }

    public void exceptionToErrorMessage(Exception e) {
        setErrorMessage(JDUtilities.convertExceptionReadable(e));
    }

    private String getDefaultErrorMessage() {
        switch (lastestStatus) {
        case LinkStatus.ERROR_RETRY:
            return JDL.L("downloadlink.status.error.retry", "Unknown error, retrying");
        case LinkStatus.ERROR_PLUGIN_DEFEKT:
            return JDL.L("downloadlink.status.error.defect", "Plugin outdated");
        case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
            return JDL.L("downloadlink.status.incomplete", "Incomplete");
        case LinkStatus.ERROR_AGB_NOT_SIGNED:
            return JDL.L("downloadlink.status.error.agb_not_signed", "TOCs not signed");
        case LinkStatus.ERROR_ALREADYEXISTS:
            return JDL.L("downloadlink.status.error.file_exists", "File exists");
        case LinkStatus.ERROR_CAPTCHA:
            return JDL.L("downloadlink.status.error.captcha_wrong", "Captcha wrong");
        case LinkStatus.ERROR_DOWNLOAD_FAILED:
            return JDL.L("downloadlink.status.error.downloadfailed", "Download failed");
        case LinkStatus.ERROR_IP_BLOCKED:
            return JDL.L("downloadlink.status.error.download_limit", "Download Limit reached");
        case LinkStatus.ERROR_FILE_NOT_FOUND:
            return JDL.L("downloadlink.status.error.file_not_found", "File not found");
        case LinkStatus.ERROR_POST_PROCESS:
            return JDL.L("downloadlink.status.error.post_process", "Processing error");
        case LinkStatus.ERROR_TIMEOUT_REACHED:
        case LinkStatus.ERROR_NO_CONNECTION:
            return JDL.L("downloadlink.status.error.no_connection", "No Connection");
        case LinkStatus.ERROR_PREMIUM:
            return JDL.L("downloadlink.status.error.premium", "Premium Error");
        case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
            return JDL.L("downloadlink.status.error.temp_unavailable", "Temp. unavailable");
        case LinkStatus.ERROR_FATAL:
            return JDL.L("downloadlink.status.error.fatal", "Fatal Error");
        case LinkStatus.WAITING_USERIO:
            return JDL.L("downloadlink.status.waitinguserio", "Waiting for user input");
        }
        return null;
    }

    public String getLongErrorMessage() {
        String ret = errorMessage;
        if (ret == null) {
            ret = getDefaultErrorMessage();
        }
        if (ret == null) {
            ret = JDL.L("downloadlink.status.error_unexpected", "Unexpected Error");
        }
        return ret;
    }

    public int getLatestStatus() {
        return lastestStatus;
    }

    public void setLatestStatus(int s) {
        lastestStatus = s;
    }

    public long getRemainingWaittime() {
        long now = System.currentTimeMillis();
        long ab = waitUntil - now;
        return Math.max(0l, ab);
    }

    /**
     * Erstellt den Statustext, fügt eine eventl Wartezeit hzin und gibt diesen
     * Statusstrin (bevorzugt an die GUI) zurück
     * 
     * @return Statusstring mit eventl Wartezeit
     */
    public String getStatusString() {
        String ret = "";
        if (hasStatus(LinkStatus.ERROR_POST_PROCESS)) {
            if (getErrorMessage() != null) {
                ret += getErrorMessage();
            } else if (getStatusText() != null) {
                ret += getStatusText();
            } else {
                ret += JDL.L("gui.downloadlink.errorpostprocess3", "[convert failed]");
            }
            return ret;
        }
        if (hasStatus(LinkStatus.FINISHED)) return this.getStatusText() != null ? "> " + this.getStatusText() : "";
        if (hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) return this.getLongErrorMessage();

        if (!downloadLink.isEnabled() && !hasStatus(LinkStatus.FINISHED)) {
            if (downloadLink.isAborted() && (statusText == null || statusText.trim().length() == 0)) {
                ret += JDL.L("gui.downloadlink.aborted", "[interrupted]") + " ";
            } else if (downloadLink.isAborted()) {
                ret += statusText;
            }
            if (errorMessage != null) {
                if (ret.length() > 0) ret += ": ";
                ret += errorMessage;
            }
            return ret;
        }

        /* ip blocked */
        if (hasStatus(ERROR_IP_BLOCKED) && downloadLink.getPlugin().getRemainingHosterWaittime() > 0) {
            ret = JDL.LF("gui.download.waittime_status2", "Wait %s", Formatter.formatSeconds((downloadLink.getPlugin().getRemainingHosterWaittime() / 1000)));
            if (errorMessage != null) ret = errorMessage + " " + ret;
            return ret;
        }
        /* temp unavail */
        if (hasStatus(ERROR_TEMPORARILY_UNAVAILABLE) && getRemainingWaittime() > 0) {
            ret = JDL.LF("gui.download.waittime_status2", "Wait %s", Formatter.formatSeconds(getRemainingWaittime() / 1000));
            if (errorMessage != null) ret = errorMessage + " " + ret;
            return ret;
        }

        if (isFailed()) { return getLongErrorMessage(); }
        if (downloadLink.getPlugin() != null && downloadLink.getPlugin().getRemainingHosterWaittime() > 0 && !downloadLink.getLinkStatus().isPluginActive()) { return JDL.L("gui.downloadlink.hosterwaittime", "[wait for new ip]"); }

        if (downloadLink.getDownloadInstance() == null && hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
            removeStatus(DOWNLOADINTERFACE_IN_PROGRESS);
        }
        if (hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
            int speed = downloadLink.getDownloadSpeed();
            String chunkString = "(" + downloadLink.getDownloadInstance().getChunksDownloading() + "/" + downloadLink.getDownloadInstance().getChunkNum() + ")";

            if (speed > 0) {
                if (downloadLink.getDownloadSize() < 0) {
                    return Formatter.formatReadable(speed) + "/s " + JDL.L("gui.download.filesize_unknown", "(Filesize unknown)");
                } else {

                    long remainingBytes = downloadLink.getDownloadSize() - downloadLink.getDownloadCurrent();
                    long eta = remainingBytes / speed;
                    return "ETA " + Formatter.formatSeconds((int) eta) + " @ " + Formatter.formatReadable(speed) + "/s " + chunkString;
                }
            } else {
                return JDL.L("gui.download.create_connection", "Connecting...") + chunkString;

            }
        }

        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) { return JDL.L("gui.download.onlinecheckfailed", "[Not available]"); }
        if (this.errorMessage != null) return errorMessage;
        if (statusText != null) { return statusText; }
        return "";
    }

    public long getTotalWaitTime() {
        return totalWaitTime;
    }

    public long getValue() {
        return value;
    }

    private boolean hasOnlyStatus(int statusCode) {
        return (status & ~statusCode) == 0;
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

    public boolean isFailed() {
        return !hasOnlyStatus(FINISHED | ERROR_ALREADYEXISTS | ERROR_IP_BLOCKED | TODO | PLUGIN_ACTIVE | PLUGIN_IN_PROGRESS | DOWNLOADINTERFACE_IN_PROGRESS | WAITING_USERIO);
    }

    public boolean isPluginActive() {
        return hasStatus(PLUGIN_ACTIVE);
    }

    public boolean isPluginInProgress() {
        return hasStatus(PLUGIN_IN_PROGRESS);
    }

    public boolean isStatus(int status) {
        return this.status == status;
    }

    /** Entfernt eine Statusid */
    public void removeStatus(int status) {
        int mask = 0xffffffff;
        mask &= ~status;
        this.status &= mask;
    }

    public void reset() {
        setStatus(TODO);
        setLatestStatus(TODO);
        errorMessage = null;
        statusText = null;
        retryCount = 0;
        value = 0;
        resetWaitTime();
    }

    public void resetWaitTime() {
        totalWaitTime = 0;
        waitUntil = 0;
    }

    public void setErrorMessage(String string) {
        if (downloadLink.isAborted() && string != null) return;
        errorMessage = string;
    }

    public void setInProgress(boolean b) {
        if (b) {
            addStatus(PLUGIN_IN_PROGRESS);
        } else {
            removeStatus(PLUGIN_IN_PROGRESS);
        }
    }

    public void setActive(boolean b) {
        if (b) {
            addStatus(PLUGIN_ACTIVE);
        } else {
            removeStatus(PLUGIN_ACTIVE);
        }
    }

    /**
     * Setzt den Linkstatus. Es dürfen nur LInkStatus.*STATUS ids verwendet
     * werden
     * 
     * @param status
     */
    public void setStatus(int status) {
        if (status == FINISHED) {
            resetWaitTime();
        }
        this.status = status;
        if (JDFlags.hasSomeFlags(status, FINISHED)) {
            downloadLink.setFinishedDate(System.currentTimeMillis());
        }
        lastestStatus = status;
    }

    public void setStatusText(String l) {
        statusText = l;
    }

    public void setValue(long i) {
        value = i;
    }

    public void setWaitTime(long milliSeconds) {
        waitUntil = System.currentTimeMillis() + milliSeconds;
        totalWaitTime = milliSeconds;
    }

    public static String toString(int status) {
        Field[] fields = LinkStatus.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getModifiers() == 25) {
                int value;
                try {
                    value = field.getInt(null);
                    if (value == status) { return field.getName(); }

                } catch (IllegalArgumentException e) {
                    JDLogger.exception(e);
                } catch (IllegalAccessException e) {
                    JDLogger.exception(e);
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        Class<? extends LinkStatus> cl = this.getClass();
        Field[] fields = cl.getDeclaredFields();
        StringBuilder sb = new StringBuilder();
        sb.append(Formatter.fillString(Integer.toBinaryString(status), "0", "", 32) + " <Statuscode\r\n");
        String latest = "";
        for (Field field : fields) {
            if (field.getModifiers() == 25) {
                int value;
                try {
                    value = field.getInt(this);
                    if (hasStatus(value)) {
                        if (value == lastestStatus) {
                            latest = "latest:" + field.getName() + "\r\n";
                            sb.append(Formatter.fillString(Integer.toBinaryString(value), "0", "", 32) + " |" + field.getName() + "\r\n");

                        } else {

                            sb.append(Formatter.fillString(Integer.toBinaryString(value), "0", "", 32) + " |" + field.getName() + "\r\n");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    JDLogger.exception(e);
                } catch (IllegalAccessException e) {
                    JDLogger.exception(e);
                }
            }
        }

        String ret = latest + sb;

        if (statusText != null) {
            ret += "StatusText: " + statusText + "\r\n";
        }
        if (errorMessage != null) {
            ret += "ErrorMessage: " + errorMessage + "\r\n";
        }
        return ret;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getStatusText() {
        return statusText;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getStatus() {
        return status;
    }

    public void setStatusIcon(ImageIcon scaledImageIcon) {
        statusIcon = scaledImageIcon;
    }

    public ImageIcon getStatusIcon() {
        return statusIcon;
    }

    public boolean isFinished() {
        return hasStatus(ERROR_ALREADYEXISTS) || hasStatus(FINISHED);
    }
}