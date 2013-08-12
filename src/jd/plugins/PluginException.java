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

import org.jdownloader.plugins.SkipReason;

public class PluginException extends Exception {

    private static final long serialVersionUID              = -413339039711789194L;

    public static final int   VALUE_ID_PREMIUM_TEMP_DISABLE = 0;
    public static final int   VALUE_ID_PREMIUM_DISABLE      = 1;
    /* do not final it, as the compiler will replace Reference with value, no more Exceptions but broken ErrorHandling in stable */
    public static int         VALUE_ID_PREMIUM_ONLY         = 2;

    private int               linkStatus                    = -1;
    private String            errorMessage                  = null;
    private long              value                         = -1;
    private SkipReason        skipReason                    = null;

    public PluginException(int linkStatus) {
        this.linkStatus = linkStatus;
    }

    public PluginException(int linkStatus, String errorMessage, long value) {
        this(linkStatus);
        this.errorMessage = errorMessage;
        this.value = value;
    }

    public PluginException(int linkStatus, String errorMessage) {
        this(linkStatus);
        this.errorMessage = errorMessage;
    }

    public PluginException(int linkStatus, long value) {
        this(linkStatus);
        this.value = value;
    }

    public PluginException(SkipReason skipReason) {
        this.skipReason = skipReason;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getValue() {
        return value;
    }

    public SkipReason getSkipReason() {
        return skipReason;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void fillLinkStatus(LinkStatus linkStatus) {
        if (getSkipReason() == null) {
            linkStatus.addStatus(this.linkStatus);
            if (value >= 0) linkStatus.setValue(value);
            if (errorMessage != null) linkStatus.setErrorMessage(errorMessage);
        }
    }

    public int getLinkStatus() {
        return linkStatus;
    }

}
