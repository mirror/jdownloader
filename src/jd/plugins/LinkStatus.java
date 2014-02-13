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

import org.appwork.utils.BinaryLogic;
import org.appwork.utils.formatter.StringFormatter;

public class LinkStatus implements Serializable {

    /**
     * Controlling & Downloadinterface: Link has been downloaded
     */
    public final static int FINISHED                             = 1 << 1;

    /**
     * Plugins: a unknown error occured
     */
    public final static int ERROR_RETRY                          = 1 << 2;

    /**
     * Plugins: captcha input has been wrong
     */
    public final static int ERROR_CAPTCHA                        = 1 << 3;

    /**
     * Plugins: Download Limit has been reached. Ip is blocked
     */
    public final static int ERROR_IP_BLOCKED                     = 1 << 4;

    /**
     * Plugins & Downloadinterface: File not found - file not available any more
     */
    public final static int ERROR_FILE_NOT_FOUND                 = 1 << 5;

    /**
     * Plugins & Controlling: A Premium error occured. check value flag for further details
     */
    public static final int ERROR_PREMIUM                        = 1 << 8;

    /**
     * this indicates an error during download and we should be able to resume/retry the download
     */
    public static final int ERROR_DOWNLOAD_INCOMPLETE            = 1 << 9;

    /**
     * Plugins:Download is not possible right now. May be back later. Maybe server problems or anything like this
     */
    public static final int ERROR_TEMPORARILY_UNAVAILABLE        = 1 << 11;

    /**
     * hoster is temporarily not available, dont try other links for this host
     */
    public static final int ERROR_HOSTER_TEMPORARILY_UNAVAILABLE = 1 << 12;

    public static final int ERROR_ALREADYEXISTS                  = 1 << 13;

    /**
     * download failed in a way we cannot simply retry/restart it
     */
    public static final int ERROR_DOWNLOAD_FAILED                = 1 << 14;

    /**
     * Plugins & Downloadinterface: Serious fatal error. No retry. Download will be canceled finally.
     */
    public static final int ERROR_FATAL                          = 1 << 17;

    /**
     * Plugin out of date. This flag says that the plugin noticed parsing problems and might be out of date.
     */
    public static final int ERROR_PLUGIN_DEFECT                  = 1 << 22;

    /**
     * timeout reached during download
     */
    public static final int VALUE_TIMEOUT_REACHED                = 1 << 1;

    /**
     * local IO error, eg invalid rights, not existing folder...
     */
    public static final int VALUE_LOCAL_IO_ERROR                 = 1 << 2;

    private void readObject(final java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
        /* make sure we set transient variables here */
        stream.defaultReadObject();
    }

    private static final long  serialVersionUID = 3885661829491436448L;

    private final DownloadLink downloadLink;

    private String             errorMessage;

    private int                status           = ERROR_RETRY;
    private String             statusText       = null;
    private long               value            = 0;
    private int                retryCount       = 0;

    public LinkStatus(final DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
    }

    public long getValue() {
        return value;
    }

    @Deprecated
    public boolean hasStatus(final int status) {
        return (this.status & status) != 0;
    }

    public void setErrorMessage(final String string) {
        errorMessage = string;
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
        this.status = status;
    }

    public void setStatusText(final String l) {
        if (statusText != null && statusText.equals(l)) return;
        statusText = l;
        notifyChanges(new LinkStatusProperty(this, LinkStatusProperty.Property.STATUSTEXT, l));
    }

    public static String toString(int status) {
        final StringBuilder ret = new StringBuilder();
        for (Field f : LinkStatus.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {

                try {
                    if (BinaryLogic.containsAll(status, f.getInt(null))) {
                        if (ret.length() > 0) ret.append(", ");
                        ret.append(f.getName());
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret.toString();
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

}