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
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import jd.controlling.JDLogger;
import jd.controlling.proxy.ProxyController;
import jd.nutils.Formatter;
import jd.nutils.JDFlags;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.StringFormatter;
import org.jdownloader.translate.JDT;

public class LinkStatus implements Serializable {

    /**
     * Controlling: Link muß noch bearbeitet werden.
     */
    public static final int   TODO                                 = 1 << 0;

    /**
     * Controlling & Downloadinterface: Link wurde erfolgreich heruntergeladen
     */
    public final static int   FINISHED                             = 1 << 1;

    /**
     * Plugins: Ein unbekannter Fehler ist aufgetreten
     */
    public final static int   ERROR_RETRY                          = 1 << 2;

    /**
     * Plugins: Captcha Text war falsch
     */
    public final static int   ERROR_CAPTCHA                        = 1 << 3;

    /**
     * Plugins: Download Limit wurde erreicht
     */
    public final static int   ERROR_IP_BLOCKED                     = 1 << 4;

    /**
     * Plugins & Downloadinterface: Die Datei konnte nicht gefunden werden
     */
    public final static int   ERROR_FILE_NOT_FOUND                 = 1 << 5;

    /**
     * Plugins & Controlling: zeigt einen Premiumspezifischen fehler an
     */
    public static final int   ERROR_PREMIUM                        = 1 << 8;

    /**
     * Downloadinterface: Zeigt an dass der Link nicht vollständig geladen wurde
     */
    public static final int   ERROR_DOWNLOAD_INCOMPLETE            = 1 << 9;

    /**
     * Controlling: Zeigt an, dass der Link gerade heruntergeladen wird
     */
    public static final int   DOWNLOADINTERFACE_IN_PROGRESS        = 1 << 10;

    /**
     * Plugins: Der download ist zur Zeit nicht möglich
     */
    public static final int   ERROR_TEMPORARILY_UNAVAILABLE        = 1 << 11;

    /**
     * hoster is temporarily not available, dont try other links for this host
     */
    public static final int   ERROR_HOSTER_TEMPORARILY_UNAVAILABLE = 1 << 12;

    /**
     * Controlling & Downloadinterface: Zeigt an, dass die Datei auf der
     * Festplatte schon existiert
     */
    public static final int   ERROR_ALREADYEXISTS                  = 1 << 13;

    /**
     * Downloadinterface: Zeigt an dass der Eigentliche Download im
     * Downloadinterface fehlgeschlagen ist. z.B. Misslungender Chunkload
     */
    public static final int   ERROR_DOWNLOAD_FAILED                = 1 << 14;

    /**
     * DownloadInterface: Zeigt an dass es einen Timeout gab und es scheinbar
     * keine Verbindung emhr zum internet gibt
     */
    public static final int   ERROR_NO_CONNECTION                  = 1 << 15;

    /**
     * Plugins & Downloadinterface: Schwerwiegender fehler. Der Download wird
     * sofort abgebrochen. Es werden keine weiteren versuche mehr gestartet
     */
    public static final int   ERROR_FATAL                          = 1 << 17;

    /**
     * Controlling: Zeigt an, dass das zugehörige Plugin den link gerade
     * bearbeitet
     */
    public static final int   PLUGIN_IN_PROGRESS                   = 1 << 18;

    /**
     * Conttrolling, Downloadinterface, Plugins Zeigt an, dass gerade ein
     * anderes Plugin an der Lokalen Datei arbeitet. Wird eingesetzt um dem
     * Controller mitzuteilen, dass bereits ein Mirror dieser Datei geladen
     * wird.
     */
    public static final int   ERROR_LINK_IN_PROGRESS               = 1 << 19;

    /**
     * DownloadINterface & Controlling zeigt an dass es zu einem plugintimeout
     * gekommen ist
     */
    public static final int   ERROR_TIMEOUT_REACHED                = 1 << 20;

    /**
     * Downloadinterface LOCAL Input output Fehler. Es kann nicht geschrieben
     * werden etc.
     */
    public static final int   ERROR_LOCAL_IO                       = 1 << 21;

    /**
     * Plugins Wird bei schweren Parsing Fehler eingesetzt. Über diesen Code
     * kann das Plugin mitteilen dass es defekt ist und aktualisiert werden muss
     */
    public static final int   ERROR_PLUGIN_DEFECT                  = 1 << 22;

    /**
     * Zeigt an, das auf User-Eingaben gewartet wird
     */
    public static final int   WAITING_USERIO                       = 1 << 23;

    public static final int   ERROR_POST_PROCESS                   = 1 << 24;

    public static final int   VALUE_FAILED_HASH                    = 1 << 27;

    private transient boolean isActive                             = false;
    private transient boolean inProgress                           = false;

    private void readObject(final java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
        /* nach dem deserialisieren sollen die transienten neu geholt werden */
        stream.defaultReadObject();
        isActive = false;
        this.inProgress = false;
    }

    /**
     * Beispiel:HTTP-Plugin Ein als HTTP-Direktlink erkannter Link verweist auf
     * den contenttype text/html was bedeutet, dass es sich wahrscheinlich um
     * ein Hosting Service handelt für das es noch kein Plugin gibt.
     */
    public static final int                 ERROR_PLUGIN_NEEDED       = 1 << 30;

    public static final int                 NOT_ENOUGH_HARDDISK_SPACE = 1 << 31;

    private static HashMap<Integer, String> toStringHelper            = new HashMap<Integer, String>();
    static {
        final Field[] fields = LinkStatus.class.getDeclaredFields();
        for (final Field field : fields) {
            if (field.getModifiers() == 25) {
                try {
                    toStringHelper.put(field.getInt(LinkStatus.class), field.getName());
                } catch (Exception e) {
                    JDLogger.exception(e);
                }
            }
        }
    }

    private static final long               serialVersionUID          = 3885661829491436448L;

    private final DownloadLink              downloadLink;
    private String                          errorMessage;
    private int                             lastestStatus             = TODO;
    private int                             status                    = TODO;
    private String                          statusText                = null;
    private long                            totalWaitTime             = 0;
    private long                            value                     = 0;
    private long                            waitUntil                 = 0;
    private int                             retryCount                = 0;

    private ImageIcon                       statusIcon;

    public LinkStatus(final DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
    }

    /**
     * Fügt einen LinkStatus.* Status hinzu.Der alte status wird dabei nicht
     * gelöscht.
     * 
     * @param status
     */
    public void addStatus(final int status) {
        this.status |= status;
        if (JDFlags.hasSomeFlags(status, FINISHED)) {
            if (downloadLink.getFinishedDate() == -1l) downloadLink.setFinishedDate(System.currentTimeMillis());
        }
        lastestStatus = status;
    }

    public void exceptionToErrorMessage(Exception e) {
        setErrorMessage(JDUtilities.convertExceptionReadable(e));
    }

    private String getDefaultErrorMessage() {
        switch (lastestStatus) {
        case LinkStatus.ERROR_RETRY:
            return JDT._.downloadlink_status_error_retry();
        case LinkStatus.ERROR_PLUGIN_DEFECT:
            return JDT._.downloadlink_status_error_defect();
        case LinkStatus.ERROR_PLUGIN_NEEDED:
            return JDT._.downloadlink_status_error_no_plugin_available();
        case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
            return JDT._.downloadlink_status_incomplete();
        case LinkStatus.ERROR_ALREADYEXISTS:
            return JDT._.downloadlink_status_error_file_exists();
        case LinkStatus.ERROR_CAPTCHA:
            return JDT._.downloadlink_status_error_captcha_wrong();
        case LinkStatus.ERROR_DOWNLOAD_FAILED:
            return JDT._.downloadlink_status_error_downloadfailed();
        case LinkStatus.ERROR_IP_BLOCKED:
            return JDT._.downloadlink_status_error_download_limit();
        case LinkStatus.ERROR_FILE_NOT_FOUND:
            return JDT._.downloadlink_status_error_file_not_found();
        case LinkStatus.ERROR_POST_PROCESS:
            return JDT._.downloadlink_status_error_post_process();
        case LinkStatus.ERROR_TIMEOUT_REACHED:
        case LinkStatus.ERROR_NO_CONNECTION:
            return JDT._.downloadlink_status_error_no_connection();
        case LinkStatus.ERROR_PREMIUM:
            return JDT._.downloadlink_status_error_premium();
        case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
            return JDT._.downloadlink_status_error_temp_unavailable();
        case LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE:
            return JDT._.downloadlink_status_error_hoster_temp_unavailable();
        case LinkStatus.ERROR_FATAL:
            return JDT._.downloadlink_status_error_fatal();
        case LinkStatus.WAITING_USERIO:
            return JDT._.downloadlink_status_waitinguserio();
        case LinkStatus.NOT_ENOUGH_HARDDISK_SPACE:
            return JDT._.downloadlink_status_error();
        }
        return null;
    }

    public String getLongErrorMessage() {
        String ret = errorMessage;
        if (ret == null) {
            ret = getDefaultErrorMessage();
        }
        if (ret == null) {
            ret = JDT._.downloadlink_status_error_unexpected();
        }
        return ret;
    }

    public int getLatestStatus() {
        return lastestStatus;
    }

    public void setLatestStatus(final int s) {
        lastestStatus = s;
    }

    public long getRemainingWaittime() {
        final long now = System.currentTimeMillis();
        final long ab = waitUntil - now;
        return Math.max(0l, ab);
    }

    /**
     * Erstellt den Statustext, fügt eine eventl Wartezeit hzin und gibt diesen
     * Statusstrin (bevorzugt an die GUI) zurück
     * 
     * @return Statusstring mit eventl Wartezeit
     */
    public String getStatusString() {
        final StringBuilder ret = new StringBuilder();
        if (hasStatus(LinkStatus.ERROR_POST_PROCESS)) {
            if (getErrorMessage() != null) {
                ret.append(getErrorMessage());
            } else if (getStatusText() != null) {
                ret.append(getStatusText());
            } else {
                ret.append(JDT._.gui_downloadlink_errorpostprocess3());
            }
            return ret.toString();
        }
        if (hasStatus(LinkStatus.FINISHED)) return this.getStatusText() != null ? " > " + this.getStatusText() : "";
        if (hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) return this.getLongErrorMessage();

        if (!downloadLink.isEnabled() && !hasStatus(LinkStatus.FINISHED)) {
            if (downloadLink.isAborted() && (statusText == null || statusText.trim().length() == 0)) {
                ret.append(JDT._.gui_downloadlink_aborted()).append(' ');
            } else if (downloadLink.isAborted()) {
                ret.append(statusText);
            }
            if (errorMessage != null) {
                if (ret.length() > 0) ret.append(new char[] { ':', ' ' });
                ret.append(errorMessage);
            }
            return ret.toString();
        }

        /* ip blocked */
        if (hasStatus(ERROR_IP_BLOCKED) && ProxyController.getInstance().getRemainingIPBlockWaittime(downloadLink.getHost()) > 0) {
            ret.append(JDT._.gui_download_waittime_status2(Formatter.formatSeconds(getRemainingWaittime() / 1000)));
            if (errorMessage != null) return errorMessage + " " + ret.toString();
            return ret.toString();
        }
        /* temp unavail */
        if (hasStatus(ERROR_TEMPORARILY_UNAVAILABLE) && getRemainingWaittime() > 0) {
            ret.append(JDT._.gui_download_waittime_status2(Formatter.formatSeconds(getRemainingWaittime() / 1000)));
            if (errorMessage != null) return errorMessage + " " + ret.toString();
            return ret.toString();
        }
        /* hoster temp unavail */
        if (hasStatus(ERROR_HOSTER_TEMPORARILY_UNAVAILABLE) && ProxyController.getInstance().getRemainingTempUnavailWaittime(downloadLink.getHost()) > 0) {
            ret.append(JDT._.gui_download_waittime_status2(Formatter.formatSeconds(getRemainingWaittime() / 1000)));
            if (errorMessage != null) return errorMessage + " " + ret.toString();
            return ret.toString();
        }

        if (isFailed()) return getLongErrorMessage();
        final DownloadInterface dli = downloadLink.getDownloadInstance();
        if (downloadLink.getDefaultPlugin() != null && ProxyController.getInstance().getRemainingIPBlockWaittime(downloadLink.getHost()) > 0 && !downloadLink.getLinkStatus().isPluginActive()) { return JDT._.gui_downloadlink_hosterwaittime(); }
        if (downloadLink.getDefaultPlugin() != null && ProxyController.getInstance().getRemainingTempUnavailWaittime(downloadLink.getHost()) > 0 && !downloadLink.getLinkStatus().isPluginActive()) { return JDT._.gui_downloadlink_hostertempunavail(); }
        if (dli == null && hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
            removeStatus(DOWNLOADINTERFACE_IN_PROGRESS);
        }
        if (hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
            final long speed = downloadLink.getDownloadSpeed();
            String chunkString = "";
            if (dli != null && dli.getChunkNum() > 1) chunkString = " (" + dli.getChunksDownloading() + "/" + dli.getChunkNum() + ")";

            if (speed > 0) {
                if (downloadLink.getDownloadSize() < 0) {
                    return Formatter.formatReadable(speed) + "/s " + JDT._.gui_download_filesize_unknown();
                } else {
                    long remainingBytes = downloadLink.getDownloadSize() - downloadLink.getDownloadCurrent();
                    long eta = remainingBytes / speed;
                    return "ETA " + Formatter.formatSeconds((int) eta) + " @ " + Formatter.formatReadable(speed) + "/s" + chunkString;
                }
            } else {
                return JDT._.gui_download_create_connection() + chunkString;
            }
        }

        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) return JDT._.gui_download_onlinecheckfailed();
        if (errorMessage != null) return errorMessage;
        if (statusText != null) return statusText;
        return "";
    }

    public long getTotalWaitTime() {
        return totalWaitTime;
    }

    public long getValue() {
        return value;
    }

    private boolean hasOnlyStatus(final int statusCode) {
        return (status & ~statusCode) == 0;
    }

    /**
     * Gibt zurück ob der zugehörige Link einen bestimmten status hat.
     * 
     * @param status
     * @return
     */
    public boolean hasStatus(final int status) {
        return (this.status & status) != 0;
    }

    public boolean isFailed() {
        return !isActive && !inProgress && !hasOnlyStatus(FINISHED | ERROR_ALREADYEXISTS | ERROR_IP_BLOCKED | TODO | DOWNLOADINTERFACE_IN_PROGRESS | WAITING_USERIO);
    }

    public boolean isPluginActive() {
        return isActive;
    }

    public boolean isPluginInProgress() {
        return this.inProgress;
    }

    public boolean isStatus(final int status) {
        return this.status == status;
    }

    /** Entfernt eine Statusid */
    public void removeStatus(final int status) {
        int mask = 0xffffffff;
        mask &= ~status;
        this.status &= mask;
    }

    public void reset(boolean resetRetryCounter) {
        setStatus(TODO);
        setLatestStatus(TODO);
        errorMessage = null;
        statusText = null;
        if (resetRetryCounter) retryCount = 0;
        value = 0;
        resetWaitTime();
    }

    public void reset() {
        reset(true);
    }

    public void resetWaitTime() {
        totalWaitTime = 0;
        waitUntil = 0;
    }

    public void setErrorMessage(final String string) {
        if (!downloadLink.isAborted() || string == null) {
            errorMessage = string;
        }
    }

    public void setInProgress(final boolean b) {
        this.inProgress = b;
    }

    public void setActive(final boolean b) {
        isActive = b;
    }

    /**
     * Setzt den Linkstatus. Es dürfen nur LInkStatus.*STATUS ids verwendet
     * werden
     * 
     * @param status
     */
    public void setStatus(final int status) {
        if (status == FINISHED) {
            resetWaitTime();
        }
        this.status = status;
        if (JDFlags.hasSomeFlags(status, FINISHED)) {
            if (downloadLink.getFinishedDate() == -1l) downloadLink.setFinishedDate(System.currentTimeMillis());
        }
        lastestStatus = status;
    }

    public void setStatusText(final String l) {
        statusText = l;
    }

    public void setValue(final long i) {
        value = i;
    }

    public void setWaitTime(final long milliSeconds) {
        waitUntil = System.currentTimeMillis() + milliSeconds;
        totalWaitTime = milliSeconds;
    }

    public static String toString(final int status) {
        return toStringHelper.get(status);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(StringFormatter.fillString(Integer.toBinaryString(status), "0", "", 32) + " < Statuscode\r\n");
        String latest = "";
        for (Entry<Integer, String> entry : toStringHelper.entrySet()) {
            int value = entry.getKey();
            if (hasStatus(value)) {
                if (value == lastestStatus) latest = "Latest: " + entry.getValue() + "\r\n";
                sb.append(StringFormatter.fillString(Integer.toBinaryString(value), "0", "", 32)).append(" | ").append(entry.getValue()).append("\r\n");
            }
        }

        final StringBuilder ret = new StringBuilder(latest);
        ret.append(sb.toString());

        if (statusText != null) {
            ret.append("StatusText: ").append(statusText).append("\r\n");
        }
        if (errorMessage != null) {
            ret.append("ErrorMessage: ").append(errorMessage).append("\r\n");
        }
        return ret.toString();
    }

    public void setRetryCount(final int retryCount) {
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

    public void setStatusIcon(ImageIcon statusIcon) {
        this.statusIcon = statusIcon;
    }

    public ImageIcon getStatusIcon() {
        return statusIcon;
    }

    public boolean isFinished() {
        return hasStatus(ERROR_ALREADYEXISTS) || hasStatus(FINISHED);
    }

    /**
     * Use this function to reset linkstatus to {@link #TODO}, if no
     * notResetIfFlag match.
     */
    public void resetStatus(int... notResetIfFlag) {
        if (this.downloadLink != null) {
            int curState = LinkStatus.TODO;
            int curLState = LinkStatus.TODO;
            String tmp2 = null;
            String tmp3 = null;
            int resetFlag = 0;
            for (final int flag : notResetIfFlag) {
                resetFlag = resetFlag | flag;
            }
            final LinkStatus linkStatus = downloadLink.getLinkStatus();
            for (final int flag : notResetIfFlag) {
                if (linkStatus.hasStatus(flag)) {
                    curState = linkStatus.getStatus();
                    curLState = linkStatus.getLatestStatus();
                    tmp2 = linkStatus.getErrorMessage();
                    tmp3 = linkStatus.getStatusText();
                    break;
                }
            }
            // filter flags
            curState = JDFlags.filterFlags(curState, resetFlag | LinkStatus.TODO);
            curLState = JDFlags.filterFlags(curLState, resetFlag | LinkStatus.TODO);
            /* reset and if needed restore the old state */
            linkStatus.reset();
            linkStatus.setStatus(curState);
            linkStatus.setLatestStatus(curLState);
            linkStatus.setErrorMessage(tmp2);
            linkStatus.setStatusText(tmp3);
        }
    }
}