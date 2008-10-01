//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class LinkStatus implements Serializable {
    /**
     * Controlling Zeigt an dass der Link gerade heruntergeladen wird
     */
    public static final int DOWNLOADINTERFACE_IN_PROGRESS = 1 << 10;
    /**
     * Controlling Die AGB wurde noch nicht unterzeichnet.
     */
    public static final int ERROR_AGB_NOT_SIGNED = 1 << 16;
    /**
     * Controlling,Downloadinterface Zeigt an dass die Datei auf der festplatte
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
     * 
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

    /**
     * Zeigt an, das auf User-Eingaben gewartet wird
     */
    public static final int WAITING_USERIO = 1 << 23;

    private static final long serialVersionUID = 3885661829491436448L;
    /**
     * Controlling: Link muß noch bearbeitet werden.
     */
    public final static int TODO = 1 << 0;
    public static final int VALUE_ID_PREMIUM_TEMP_DISABLE = 0;
    public static final int VALUE_ID_PREMIUM_DISABLE = 1;
    private DownloadLink downloadLink;
    private String errorMessage;

    private int lastestStatus = TODO;
    private int status = TODO;
    private transient String statusText = null;
    private long totalWaitTime = 0;
    private long value = 0;
    private long waitUntil = 0;
    private int retryCount = 0;

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
        lastestStatus = status;
    }

    public void exceptionToErrorMessage(Exception e) {
        setErrorMessage(JDUtilities.convertExceptionReadable(e));
    }

    private String getDefaultErrorMessage() {
        switch (lastestStatus) {

        case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
            return JDLocale.L("downloadlink.status.incomplete", "Incomplete");
        case LinkStatus.ERROR_AGB_NOT_SIGNED:
            return JDLocale.L("downloadlink.status.error.agb_not_signed", "TOCs not signed");
        case LinkStatus.ERROR_ALREADYEXISTS:
            return JDLocale.L("downloadlink.status.error.file_exists", "File exists");
        case LinkStatus.ERROR_CAPTCHA:
            return JDLocale.L("downloadlink.status.error.captcha_wrong", "Captcha wrong");
        case LinkStatus.ERROR_DOWNLOAD_FAILED:
            return JDLocale.L("downloadlink.status.error.downloadfailed", "Download failed");
        case LinkStatus.ERROR_IP_BLOCKED:
            return JDLocale.L("downloadlink.status.error.download_limit", "Download Limit reached");
        case LinkStatus.ERROR_FILE_NOT_FOUND:
            return JDLocale.L("downloadlink.status.error.file_not_found", "File not found");
        case LinkStatus.ERROR_TIMEOUT_REACHED:
        case LinkStatus.ERROR_NO_CONNECTION:
            return JDLocale.L("downloadlink.status.error.no_connection", "No Connection");
        case LinkStatus.ERROR_PREMIUM:
            return JDLocale.L("downloadlink.status.error.premium", "Premium Error");
        case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
            return JDLocale.L("downloadlink.status.error.temp_unavailable", "Temp. unavailable");
        case LinkStatus.ERROR_FATAL:
            return JDLocale.L("downloadlink.status.error.fatal", "Fatal Error");
        case LinkStatus.WAITING_USERIO:
            return JDLocale.L("downloadlink.status.waitinguserio", "Waiting for user input");
        }
        return null;

    }

    public String getLongErrorMessage() {
        String ret = errorMessage;
        if (ret == null) {
            ret = getDefaultErrorMessage();
        }
        if (ret == null) {
            ret = JDLocale.L("downloadlink.status.error_unexpected", "Unexpected Error");
        }
        return ret;
    }

    public int getLatestStatus() {
        return lastestStatus;
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
        if (hasStatus(LinkStatus.FINISHED)) {

        return JDLocale.L("gui.downloadlink.finished", "[finished]")+(this.getStatusText()!=null?"> "+this.getStatusText():""); }

        if (!downloadLink.isEnabled() && !hasStatus(LinkStatus.FINISHED)) {
            if (downloadLink.isAborted()) {
                ret += JDLocale.L("gui.downloadlink.aborted", "[interrupted]");
            } else {
                ret += JDLocale.L("gui.downloadlink.disabled", "[deaktiviert]");

            }

            if (errorMessage != null) {

                ret += ": " + errorMessage;
            }
            return ret;

        }

        if (isFailed()) { return getLongErrorMessage(); }

        if (hasStatus(ERROR_IP_BLOCKED) && downloadLink.getPlugin().getRemainingHosterWaittime() > 0) {
            if (errorMessage == null) {
                ret = String.format(JDLocale.L("gui.download.waittime_status", "Wait %s min"), JDUtilities.formatSeconds((downloadLink.getPlugin().getRemainingHosterWaittime() / 1000)));
            } else {
                ret = String.format(JDLocale.L("gui.download.waittime_status", "Wait %s min"), JDUtilities.formatSeconds((downloadLink.getPlugin().getRemainingHosterWaittime() / 1000))) + errorMessage;

            }
            return ret;
        }

        if (downloadLink.getPlugin() != null && downloadLink.getPlugin().getRemainingHosterWaittime() > 0) { return JDLocale.L("gui.downloadlink.hosterwaittime", "[wait for new ip]"); }

        if (downloadLink.getDownloadInstance() == null && hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
            removeStatus(DOWNLOADINTERFACE_IN_PROGRESS);
        }
        if (hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
            int speed = Math.max(0, downloadLink.getDownloadSpeed());
            String chunkString = "(" + downloadLink.getDownloadInstance().getChunksDownloading() + "/" + downloadLink.getDownloadInstance().getChunkNum() + ")";

            if (speed > 0) {
                if (downloadLink.getDownloadSize() < 0) {
                    return JDUtilities.formatKbReadable(speed / 1024) + "/s " + JDLocale.L("gui.download.filesize_unknown", "(Filesize unknown)");
                } else {

                    long remainingBytes = downloadLink.getDownloadSize() - downloadLink.getDownloadCurrent();
                    long eta = remainingBytes / speed;
                    return "ETA " + JDUtilities.formatSeconds((int) eta) + " @ " + JDUtilities.formatKbReadable(speed / 1024) + "/s " + chunkString;

                }
            } else {
                return JDLocale.L("gui.download.create_connection", "Connecting...") + chunkString;

            }
        }
        if (downloadLink.isAvailabilityChecked() && !downloadLink.isAvailable()) { return JDLocale.L("gui.download.onlinecheckfailed", "[Not available]"); }
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
        return !hasOnlyStatus(FINISHED | ERROR_IP_BLOCKED | TODO | PLUGIN_IN_PROGRESS | DOWNLOADINTERFACE_IN_PROGRESS | WAITING_USERIO);
    }

    public boolean isPluginActive() {
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

        statusText = null;
        retryCount = 0;
        totalWaitTime = 0;
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

    /**
     * Setzt den Linkstatus. Es dürfen nur LInkStatus.*STATUS ids verwendet
     * werden
     * 
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
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
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }

        }
        return null;
    }

    @Override
    public String toString() {
        Class<? extends LinkStatus> cl = this.getClass();
        Field[] fields = cl.getDeclaredFields();
        StringBuffer sb = new StringBuffer();
        sb.append(JDUtilities.fillString(Integer.toBinaryString(status), "0", "", 32) + " <Statuscode\r\n");
        String latest = "";
        for (Field field : fields) {
            if (field.getModifiers() == 25) {
                int value;
                try {
                    value = field.getInt(this);
                    if (hasStatus(value)) {
                        if (value == lastestStatus) {
                            latest = "latest:" + field.getName() + "\r\n";
                            sb.append(JDUtilities.fillString(Integer.toBinaryString(value), "0", "", 32) + " |" + field.getName() + "\r\n");

                        } else {

                            sb.append(JDUtilities.fillString(Integer.toBinaryString(value), "0", "", 32) + " |" + field.getName() + "\r\n");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
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

}
