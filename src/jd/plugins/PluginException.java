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

import org.appwork.utils.StringUtils;

public class PluginException extends Exception {
    private static final long serialVersionUID              = -413339039711789194L;
    /* do not use final, as the compiler will replace Reference with value, no more Exceptions but broken ErrorHandling in stable */
    public static int         VALUE_ID_PREMIUM_TEMP_DISABLE = 0;
    public static int         VALUE_ID_PREMIUM_DISABLE      = 1;
    public static int         VALUE_ID_PREMIUM_ONLY         = 2;
    private String            localizedMessage;
    private final int         linkStatus;
    private final long        value;

    public PluginException(int linkStatus) {
        this(linkStatus, null, -1);
    }

    @Override
    public String getLocalizedMessage() {
        if (StringUtils.isNotEmpty(localizedMessage)) {
            return localizedMessage;
        }
        return super.getLocalizedMessage();
    }

    public PluginException localizedMessage(String localizedMessage) {
        if (!StringUtils.equals(getMessage(), localizedMessage)) {
            this.localizedMessage = localizedMessage;
        }
        return this;
    }

    public PluginException(int linkStatus, String errorMsgEnglish, long value, Throwable cause) {
        super(errorMsgEnglish, cause);
        this.linkStatus = linkStatus;
        this.value = value;
    }

    public PluginException(int linkStatus, String errorMessage, long value) {
        this(linkStatus, errorMessage, value, null);
    }

    public PluginException(int linkStatus, String errorMessage, Throwable cause) {
        this(linkStatus, errorMessage, -1, cause);
    }

    public PluginException(int linkStatus, String errorMessage) {
        this(linkStatus, errorMessage, -1);
    }

    public PluginException(int linkStatus, long value) {
        this(linkStatus, null, value);
    }

    @Override
    public String toString() {
        final String s = getClass().getName();
        final String ret = super.getMessage();
        if (ret != null) {
            return s + ": " + ret + "->" + LinkStatus.toString(getLinkStatus()) + "|Value:" + getValue();
        } else {
            return s + ": " + LinkStatus.toString(getLinkStatus()) + "|Value:" + getValue();
        }
    }

    public long getValue() {
        return value;
    }

    /**
     * JD2Only
     *
     * @return
     */
    public int getLinkStatus() {
        return linkStatus;
    }

}
