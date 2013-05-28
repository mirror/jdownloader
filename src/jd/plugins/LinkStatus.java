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

import javax.swing.ImageIcon;

import jd.nutils.JDFlags;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.StringFormatter;
import org.jdownloader.translate._JDT;

public class LinkStatus implements Serializable {

    /**
     * Controlling: Link is in queue and is waiting for beeing processed
     */
    public static final int   TODO                                 = 1 << 0;

    /**
     * Controlling & Downloadinterface: Link has been downloaded
     */
    public final static int   FINISHED                             = 1 << 1;

    /**
     * Plugins: a unknown error occured
     */
    public final static int   ERROR_RETRY                          = 1 << 2;

    /**
     * Plugins: captcha input has been wrong
     */
    public final static int   ERROR_CAPTCHA                        = 1 << 3;

    /**
     * Plugins: Download Limit has been reached. Ip is blocked
     */
    public final static int   ERROR_IP_BLOCKED                     = 1 << 4;

    /**
     * Plugins & Downloadinterface: File not found - file not available any more
     */
    public final static int   ERROR_FILE_NOT_FOUND                 = 1 << 5;

    /**
     * Plugins & Controlling: A Premium error occured. check value flag for further details
     */
    public static final int   ERROR_PREMIUM                        = 1 << 8;

    /**
     * Downloadinterface: The download started, but has not been finished successfully
     */
    public static final int   ERROR_DOWNLOAD_INCOMPLETE            = 1 << 9;

    /**
     * Controlling: DownloadLink is in progress. Download or plugin is running
     */
    public static final int   DOWNLOADINTERFACE_IN_PROGRESS        = 1 << 10;

    /**
     * Plugins:Download is not possible right now. May be back later. Maybe server problems or anything like this
     */
    public static final int   ERROR_TEMPORARILY_UNAVAILABLE        = 1 << 11;

    /**
     * hoster is temporarily not available, dont try other links for this host
     */
    public static final int   ERROR_HOSTER_TEMPORARILY_UNAVAILABLE = 1 << 12;

    /**
     * Controlling & Downloadinterface: The destination file already exists on harddisk
     */
    public static final int   ERROR_ALREADYEXISTS                  = 1 << 13;

    /**
     * Downloadinterface: The actual download failed. Example: Chunkerrors
     */
    public static final int   ERROR_DOWNLOAD_FAILED                = 1 << 14;

    /**
     * DownloadInterface: A Connection Timeout occured. Maybe no connection to the internet?
     */
    public static final int   ERROR_NO_CONNECTION                  = 1 << 15;

    /**
     * Plugins & Downloadinterface: Serious fatal error. No retry. Download will be canceled finally.
     */
    public static final int   ERROR_FATAL                          = 1 << 17;

    /**
     * Controlling: The plugin is in progress.
     */
    public static final int   PLUGIN_IN_PROGRESS                   = 1 << 18;

    /**
     * DownloadINterface & Controlling a timeout occured
     */
    public static final int   ERROR_TIMEOUT_REACHED                = 1 << 20;

    /**
     * Downloadinterface Local IO problem. we could not write to harddisk
     */
    public static final int   ERROR_LOCAL_IO                       = 1 << 21;

    /**
     * Plugin out of date. This flag says that the plugin noticed parsing problems and might be out of date.
     */
    public static final int   ERROR_PLUGIN_DEFECT                  = 1 << 22;

    /**
     * We are waiting for user input.
     */
    public static final int   WAITING_USERIO                       = 1 << 23;
    /**
     * Error in post processing. for example downloading
     */
    public static final int   ERROR_POST_PROCESS                   = 1 << 24;
    /**
     * Hash Check failed after download process
     */
    public static final int   VALUE_FAILED_HASH                    = 1 << 27;

    private transient boolean isActive                             = false;
    private transient boolean inProgress                           = false;

    private void readObject(final java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
        /* nach dem deserialisieren sollen die transienten neu geholt werden */
        stream.defaultReadObject();
        isActive = false;
        this.inProgress = false;
    }

    /* temporarily ignore this link, use value to signal why */
    public static final int    TEMP_IGNORE                                     = 1 << 31;

    public static final int    TEMP_IGNORE_REASON_NOT_ENOUGH_HARDDISK_SPACE    = 1;
    public static final int    TEMP_IGNORE_REASON_NO_SUITABLE_ACCOUNT_FOUND    = 2;
    public static final int    TEMP_IGNORE_REASON_INVALID_DOWNLOAD_DESTINATION = 3;

    private static final long  serialVersionUID                                = 3885661829491436448L;

    private final DownloadLink downloadLink;

    private String             errorMessage;
    private int                lastestStatus                                   = TODO;
    private int                status                                          = TODO;
    private String             statusText                                      = null;
    private long               value                                           = 0;
    private long               waitUntil                                       = 0;
    private int                retryCount                                      = 0;

    private ImageIcon          statusIcon;

    public LinkStatus(final DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
    }

    /**
     * Fügt einen LinkStatus.* Status hinzu.Der alte status wird dabei nicht gelöscht.
     * 
     * @param status
     */
    public void addStatus(final int status) {
        if (lastestStatus == status) return;
        this.status |= status;
        if (JDFlags.hasSomeFlags(status, FINISHED, ERROR_ALREADYEXISTS)) {
            if (downloadLink.getFinishedDate() == -1l) downloadLink.setFinishedDate(System.currentTimeMillis());
        }
        lastestStatus = status;
    }

    public String getMessage(boolean customizedMessageOnly) {
        if (customizedMessageOnly) {
            if (errorMessage != null) return errorMessage;
            if (statusText != null) return statusText;
            return null;
        }
        switch (lastestStatus) {
        /* first we check for LinkStatus */
        case LinkStatus.FINISHED:
        case LinkStatus.TODO:
            if (statusText != null) return statusText;
            return null;
        case LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS:
            return _JDT._.download_connection_normal();
        case LinkStatus.ERROR_RETRY:
            return _JDT._.downloadlink_status_error_retry();
        case LinkStatus.ERROR_PLUGIN_DEFECT:
            if (errorMessage != null) return errorMessage;
            if (statusText != null) return statusText;
            return _JDT._.downloadlink_status_error_defect();
        case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
            return _JDT._.downloadlink_status_incomplete();
        case LinkStatus.ERROR_ALREADYEXISTS:
            return _JDT._.downloadlink_status_error_file_exists();
        case LinkStatus.ERROR_CAPTCHA:
            return _JDT._.downloadlink_status_error_captcha_wrong();
        case LinkStatus.ERROR_DOWNLOAD_FAILED:
            return _JDT._.downloadlink_status_error_downloadfailed();
        case LinkStatus.ERROR_IP_BLOCKED:
            return _JDT._.downloadlink_status_error_download_limit();
        case LinkStatus.ERROR_FILE_NOT_FOUND:
            return _JDT._.downloadlink_status_error_file_not_found();
        case LinkStatus.ERROR_POST_PROCESS:
            if (errorMessage != null) return errorMessage;
            if (statusText != null) return statusText;
            return _JDT._.downloadlink_status_error_post_process();
        case LinkStatus.ERROR_TIMEOUT_REACHED:
        case LinkStatus.ERROR_NO_CONNECTION:
            return _JDT._.downloadlink_status_error_no_connection();
        case LinkStatus.ERROR_PREMIUM:
            if (errorMessage != null) return errorMessage;
            return _JDT._.downloadlink_status_error_premium();
        case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
            if (errorMessage != null) return errorMessage;
            if (statusText != null) return statusText;
            return _JDT._.downloadlink_status_error_temp_unavailable();
        case LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE:
            if (errorMessage != null) return errorMessage;
            if (statusText != null) return statusText;
            return _JDT._.downloadlink_status_error_hoster_temp_unavailable();
        case LinkStatus.ERROR_FATAL:
            if (errorMessage != null) return errorMessage;
            if (statusText != null) return statusText;
            return _JDT._.downloadlink_status_error_fatal();
        case LinkStatus.WAITING_USERIO:
            return _JDT._.downloadlink_status_waitinguserio();
        case LinkStatus.TEMP_IGNORE:
            if (TEMP_IGNORE_REASON_NOT_ENOUGH_HARDDISK_SPACE == value) {
                return _JDT._.downloadlink_status_error();
            } else if (TEMP_IGNORE_REASON_NO_SUITABLE_ACCOUNT_FOUND == value) {
                return _JDT._.downloadlink_status_error_premium_noacc();
            } else if (TEMP_IGNORE_REASON_INVALID_DOWNLOAD_DESTINATION == value) { return _JDT._.downloadlink_status_error_invalid_dest(); }
            return null;
        }
        if (downloadLink.getAvailableStatus() == AvailableStatus.FALSE) return _JDT._.gui_download_onlinecheckfailed();
        return null;
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
        return !isActive && !inProgress && !hasOnlyStatus(FINISHED | ERROR_ALREADYEXISTS | ERROR_IP_BLOCKED | TODO | DOWNLOADINTERFACE_IN_PROGRESS | WAITING_USERIO | TEMP_IGNORE);
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
        waitUntil = 0;
    }

    public void setErrorMessage(final String string) {
        if (!downloadLink.isAborted() || string == null) {
            errorMessage = string;
        }
    }

    public void setInProgress(final boolean b) {
        if (b == this.inProgress) return;
        this.inProgress = b;
        notifyChanges(new LinkStatusProperty(this, LinkStatusProperty.Property.PROGRESS, b));
    }

    public void setActive(final boolean b) {
        if (b == isActive) return;
        isActive = b;
        notifyChanges(new LinkStatusProperty(this, LinkStatusProperty.Property.ACTIVE, b));
    }

    private void notifyChanges(LinkStatusProperty property) {
        DownloadLink dl = this.downloadLink;
        if (dl != null) dl.getFilePackage().nodeUpdated(dl, jd.controlling.packagecontroller.AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, property);
    }

    /**
     * Setzt den Linkstatus. Es dürfen nur LInkStatus.*STATUS ids verwendet werden
     * 
     * @param status
     */
    public void setStatus(final int status) {
        if (status == FINISHED) {
            resetWaitTime();
        }
        this.status = status;
        if (JDFlags.hasSomeFlags(status, FINISHED, ERROR_ALREADYEXISTS)) {
            if (downloadLink.getFinishedDate() == -1l) downloadLink.setFinishedDate(System.currentTimeMillis());
        }
        lastestStatus = status;
    }

    public void setStatusText(final String l) {
        if (statusText != null && statusText.equals(l)) return;
        statusText = l;
        notifyChanges(new LinkStatusProperty(this, LinkStatusProperty.Property.STATUSTEXT, l));
    }

    public void setValue(final long i) {
        value = i;
    }

    public void setWaitTime(final long milliSeconds) {
        waitUntil = System.currentTimeMillis() + milliSeconds;
    }

    public long getWaitTime() {
        return waitUntil;
    }

    @Override
    public String toString() {
        final StringBuilder ret = new StringBuilder();
        ret.append(StringFormatter.fillString(Integer.toBinaryString(status), "0", "", 32) + " < Statuscode\r\n");

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

    public DownloadLink _getDownloadLink() {
        return downloadLink;
    }

    /**
     * Use this function to reset linkstatus to {@link #TODO}, if no notResetIfFlag match.
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