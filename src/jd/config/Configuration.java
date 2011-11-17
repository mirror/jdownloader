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

    public static final String AUTOTRAIN_ERROR_LEVEL       = "AUTOTRAIN_ERROR_LEVEL";

    public static final String JAC_SHOW_TIMEOUT            = "JAC_SHOW_TIMEOUT";

    public static final String PARAM_CAPTCHA_JAC_DISABLE   = "CAPTCHA_JAC_DISBALE";

    public static final String PARAM_HTTPSEND_REQUESTS     = "HTTPSEND_REQUESTS";

    public static final String PARAM_HTTPSEND_REQUESTS_CLR = "HTTPSEND_REQUESTS_CLR";

    private static final long  serialVersionUID            = -2709887320616014389L;

    public static final String NAME                        = "jdownloaderconfig";

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
