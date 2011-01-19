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

package jd.config;

import java.io.Serializable;

import jd.utils.JDUtilities;

/**
 * In dieser Klasse werden die benutzerspezifischen Einstellungen festgehalten
 * 
 * @author astaldo
 */
public class Configuration extends SubConfiguration implements Serializable {

    public static final String AUTOTRAIN_ERROR_LEVEL                  = "AUTOTRAIN_ERROR_LEVEL";

    public static final String JAC_SHOW_TIMEOUT                       = "JAC_SHOW_TIMEOUT";

    public static final String PARAM_CAPTCHA_JAC_DISABLE              = "CAPTCHA_JAC_DISBALE";

    public static final String PARAM_CAPTCHA_SIZE                     = "CAPTCHA_SIZE";

    public static final String PARAM_CLIPBOARD_ALWAYS_ACTIVE          = "CLIPBOARD_ALWAYS_ACTIVE2";

    public static final String PARAM_CURRENT_BROWSE_PATH              = "CURRENT_BROWSE_PATH";

    public static final String PARAM_ALLOW_RECONNECT                  = "ALLOW_RECONNECT";

    public static final String PARAM_DO_CRC                           = "DO_CRC";

    public static final String PARAM_DOWNLOAD_DIRECTORY               = "DOWNLOAD_DIRECTORY";

    public static final String PARAM_DOWNLOAD_MAX_CHUNKS              = "DOWNLOAD_MAX_CHUNKS";

    public static final String PARAM_DOWNLOAD_PAUSE_SPEED             = "PAUSE_SPEED";

    public static final String PARAM_DOWNLOAD_MAX_SIMULTAN            = "DOWNLOAD_MAX_SIMULTAN";

    public static final String PARAM_DOWNLOAD_MAX_SIMULTAN_PER_HOST   = "DOWNLOAD_MAX_SIMULTAN_PER_HOST";

    public static final String PARAM_DOWNLOAD_MAX_SPEED               = "DOWNLOAD_MAX_SPEED_V2";

    public static final String PARAM_FILE_EXISTS                      = "FILE_EXISTS";

    public static final String PARAM_FINISHED_DOWNLOADS_ACTION        = "FINISHED_DOWNLOADS_ACTION";

    public static final String PARAM_GLOBAL_IP_CHECK_SITE             = "GLOBAL_IP_CHECK_SITE3";

    public static final String PARAM_GLOBAL_IP_DISABLE                = "GLOBAL_IP_DISABLE2";

    public static final String PARAM_GLOBAL_IP_BALANCE                = "GLOBAL_IP_BALANCE3";

    public static final String PARAM_GLOBAL_IP_MASK                   = "PARAM_GLOBAL_IP_MASK2";

    public static final String PARAM_GLOBAL_IP_PATTERN                = "GLOBAL_IP_PATTERN3";

    public static final String PARAM_HTTPSEND_IP                      = "HTTPSEND_IP";

    public static final String PARAM_HTTPSEND_PASS                    = "HTTPSEND_PASS";

    public static final String PARAM_HTTPSEND_REQUESTS                = "HTTPSEND_REQUESTS";

    public static final String PARAM_HTTPSEND_REQUESTS_CLR            = "HTTPSEND_REQUESTS_CLR";

    public static final String PARAM_HTTPSEND_USER                    = "HTTPSEND_USER";

    public static final String PARAM_LOGGER_LEVEL                     = "LOGGER_LEVEL3";

    public static final String PARAM_RELOADCONTAINER                  = "RELOADCONTAINER";

    public static final String PARAM_SHOW_CONTAINER_ONLOAD_OVERVIEW   = "SHOW_CONTAINER_ONLOAD_OVERVIEW";

    public static final String PARAM_UPDATE_VERSION                   = "UPDATE_VERSION";

    public static final String PARAM_USE_GLOBAL_PREMIUM               = "USE_PREMIUM_V3";

    public static final String PARAM_USE_PACKETNAME_AS_SUBFOLDER      = "USE_PACKETNAME_AS_SUBFOLDER";

    public static final String PARAM_CREATE_SUBFOLDER_BEFORE_DOWNLOAD = "CREATE_SUBFOLDER_BEFORE_DOWNLOAD";

    public static final String PARAM_WEBUPDATE_AUTO_RESTART           = "WEBUPDATE_AUTO_RESTART";

    public static final String PARAM_WEBUPDATE_DISABLE                = "WEBUPDATE_DISABLE";

    private static final long  serialVersionUID                       = -2709887320616014389L;

    public static final String PARAM_WEBUPDATE_AUTO_SHOW_CHANGELOG    = "WEBUPDATE_AUTO_SHOW_CHANGELOG";

    public static final String NAME                                   = "jdownloaderconfig";

    public static final String PARAM_IPCHECKWAITTIME                  = "RECONNECT_IPCHECKWAITTIME2";

    public static final String PARAM_RETRIES                          = "RECONNECT_RETRIES2";

    public static final String PARAM_WAITFORIPCHANGE                  = "RECONNECT_WAITFORIPCHANGE2";

    /**
     * Konstruktor für ein Configuration Object
     */
    public Configuration() {
    }

    @Override
    public void save() {
        JDUtilities.getDatabaseConnector().saveConfiguration(Configuration.NAME, this);
        this.changes = false;
    }

    @Override
    public String toString() {
        return Configuration.NAME;
    }

}
