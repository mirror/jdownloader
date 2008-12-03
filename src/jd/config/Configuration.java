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

package jd.config;

import java.io.Serializable;
import java.util.Vector;

import jd.controlling.interaction.Interaction;
import jd.router.RouterData;
import jd.utils.JDUtilities;

/**
 * In dieser Klasse werden die benutzerspezifischen Einstellungen festgehalten
 * 
 * @author astaldo
 */
public class Configuration extends Property implements Serializable {
    public static final String AUTOTRAIN_ERROR_LEVEL = "AUTOTRAIN_ERROR_LEVEL";

    public static final String CONFIG_INTERACTIONS = "EVENTS";

    public static final String JAC_SHOW_TIMEOUT = "JAC_SHOW_TIMEOUT";

    public static final String LOGGER_FILELOG = "LOGGER_FILELOG";

    public static final String PARAM_CAPTCHA_JAC_DISABLE = "CAPTCHA_JAC_DISBALE";

    public static final String PARAM_CLIPBOARD_ALWAYS_ACTIVE = "CLIPBOARD_ALWAYS_ACTIVE";

    public static final String PARAM_CURRENT_BROWSE_PATH = "CURRENT_BROWSE_PATH";

    public static final String PARAM_DISABLE_RECONNECT = "DISABLE_RECONNECT";

    public static final String PARAM_DO_CRC = "DO_CRC";

    public static final String PARAM_DOWNLOAD_CONNECT_TIMEOUT = "DOWNLOAD_CONNECT_TIMEOUT_V2";

    public static final String PARAM_DOWNLOAD_DIRECTORY = "DOWNLOAD_DIRECTORY";

    public static final String PARAM_DOWNLOAD_MAX_CHUNKS = "DOWNLOAD_MAX_CHUNKS";

    public static final String PARAM_DOWNLOAD_MAX_SIMULTAN = "DOWNLOAD_MAX_SIMULTAN";

    public static final String PARAM_DOWNLOAD_MAX_SIMULTAN_PER_HOST = "DOWNLOAD_MAX_SIMULTAN_PER_HOST";

    public static final String PARAM_DOWNLOAD_MAX_SPEED = "DOWNLOAD_MAX_SPEED";

    public static final String PARAM_DOWNLOAD_READ_TIMEOUT = "DOWNLOAD_READ_TIMEOUT_V2";

    public static final String PARAM_FILE_BROWSER = "PARAM_FILE_BROWSER";

    public static final String PARAM_FILE_EXISTS = "FILE_EXISTS";

    public static final String PARAM_FINISHED_DOWNLOADS_ACTION = "FINISHED_DOWNLOADS_ACTION";

    public static final String PARAM_GLOBAL_IP_CHECK_SITE = "GLOBAL_IP_CHECK_SITE";

    public static final String PARAM_GLOBAL_IP_DISABLE = "GLOBAL_IP_DISABLE";

    public static final String PARAM_GLOBAL_IP_MASK = "PARAM_GLOBAL_IP_MASK";

    public static final String PARAM_GLOBAL_IP_PATTERN = "GLOBAL_IP_PATTERN";

    public static final String PARAM_HOST_PRIORITY = "HOST_PRIORITY";

    public static final String PARAM_HTTPSEND_IP = "HTTPSEND_IP";

    public static final String PARAM_HTTPSEND_IPCHECKWAITTIME = "HTTPSEND_IPCHECKWAITTIME";

    public static final String PARAM_HTTPSEND_PASS = "HTTPSEND_PASS";

    public static final String PARAM_HTTPSEND_ROUTERNAME = "PARAM_HTTPSEND_ROUTERNAME";

    public static final String PARAM_HTTPSEND_REQUESTS = "HTTPSEND_REQUESTS";

    public static final String PARAM_HTTPSEND_REQUESTS_CLR = "HTTPSEND_REQUESTS_CLR";

    public static final String PARAM_HTTPSEND_RETRIES = "HTTPSEND_RETRIES";

    public static final String PARAM_HTTPSEND_USER = "HTTPSEND_USER";

    public static final String PARAM_HTTPSEND_WAITFORIPCHANGE = "HTTPSEND_WAITFORIPCHANGE";

    public static final String PARAM_INTERACTIONS = "INTERACTIONS";

    public static final String PARAM_JAC_METHODS = "JAC_METHODS";

    public static final String PARAM_LOGGER_LEVEL = "LOGGER_LEVEL";

    public static final String PARAM_RECONNECT_TYPE = "RECONNECT_TYPE";

    public static final String PARAM_RELOADCONTAINER = "RELOADCONTAINER";

    public static final String PARAM_SHOW_CONTAINER_ONLOAD_OVERVIEW = "SHOW_CONTAINER_ONLOAD_OVERVIEW";

    public static final String PARAM_UNRAR_INSTANCE = "UNRAR_INSTANCE";

    public static final String PARAM_UPDATE_HASH = "UPDATE_HASH";
    
    public static final String PARAM_UPDATE_VERSION = "UPDATE_VERSION";

    public static final String PARAM_USE_GLOBAL_PREMIUM = "USE_PREMIUM";

    public static final String PARAM_USE_PACKETNAME_AS_SUBFOLDER = "USE_PACKETNAME_AS_SUBFOLDER";

    public static final String PARAM_WEBUPDATE_AUTO_RESTART = "WEBUPDATE_AUTO_RESTART";

    public static final String PARAM_WEBUPDATE_DISABLE = "WEBUPDATE_DISABLE";

    public static final String PROXY_HOST = "PROXY_HOST";

    public static final String PROXY_PASS = "PROXY_PASS";

    public static final String PROXY_PASS_SOCKS = "PROXY_PASS_SOCKS";

    public static final String PROXY_PORT = "PROXY_PORT";

    public static final String PROXY_USER = "PROXY_USER";

    public static final String PROXY_USER_SOCKS = "PROXY_USER_SOCKS";

    public static final String SOCKS_HOST = "SOCKS_HOST";

    public static final String SOCKS_PORT = "SOCKS_PORT";

    public static final String USE_PROXY = "USE_PROXY";

    public static final String USE_SOCKS = "USE_SOCKS";

    /**
     * Gibt an ob die SerializeFunktionen im XMl MOdus Arbeiten oder nocht
     */
    public transient static boolean saveAsXML = false;

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -2709887320616014389L;

    /**
     * Die unterschiedlichen Interaktionen. (ZB Reconnect nach einem Download)
     */
    private Vector<Interaction> interactions = new Vector<Interaction>();

    /**
     * Gibt an wie oft Versucht werden soll eine neue IP zu bekommen. (1&1 lässt
     * grüßen)
     */
    private int reconnectRetries = 0;

    /**
     * Hier sind die Angaben für den Router gespeichert
     */
    private RouterData routerData = new RouterData();

    /**
     * Password für den Router
     */
    private String routerPassword = null;

    /**
     * Benutzername für den Router
     */
    private String routerUsername = null;

    /**
     * Wartezeit zwischen reconnect und erstem IP Check
     */
    private int waitForIPCheck = 0;

    /**
     * Konstruktor für ein Configuration Object
     */
    public Configuration() {
    }

    public String getDefaultDownloadDirectory() {
        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, JDUtilities.getResourceFile("downloads").getAbsolutePath());
    }

    /**
     * Gibt die Interactionen zurück. Alle eingestellten Interactionen werden
     * hier in einem vector zurückgegeben
     * 
     * @return Vector<Interaction>
     */
    public Vector<Interaction> getInteractions() {
        return interactions;
    }

    /**
     * Gibt den Wert zu key zurück. falls dieser Wert == null ist wird der
     * defaultValue zurückgegeben
     * 
     * @param key
     * @param defaultValue
     * @return Wert zu key oder defaultValue
     */
    @Override
    public Object getProperty(String key, Object defaultValue) {
        if (getProperty(key) == null) { return defaultValue; }
        return getProperty(key);
    }

    /**
     * @return the reconnectRetries
     */
    public int getReconnectRetries() {
        return reconnectRetries;
    }

    /**
     * Gibt das routerdata objekt zurück. darin sind alle informationen
     * gespeichert die aus der routerdata.xml importiert worden sind. (für einen
     * router)
     * 
     * @return Gibt das routerdata objekt zurück
     */
    public RouterData getRouterData() {
        return routerData;
    }

    /**
     * @return Gibt das Routeradmin Passwort zurück
     */
    public String getRouterPassword() {
        return routerPassword;
    }

    /**
     * @return gibt den router-admin-Username zurück
     */
    public String getRouterUsername() {
        return routerUsername;
    }

    /**
     * Wartezeit zwischen reconnect und erstem IP Check
     * 
     * @return Wartezeit zwischen reconnect und ip-check
     */
    public int getWaitForIPCheck() {
        return waitForIPCheck;
    }

    public void save() {
        JDUtilities.getDatabaseConnector().saveConfiguration("jdownloaderconfig", this);
    }

    /**
     * Setzt die Interactionen
     * 
     * @param interactions
     */
    public void setInteractions(Vector<Interaction> interactions) {
        this.interactions = interactions;
    }

    /**
     * @param reconnectRetries
     *            the reconnectRetries to set
     */
    public void setReconnectRetries(int reconnectRetries) {
        this.reconnectRetries = reconnectRetries;
    }

    /**
     * @param routerData
     */
    public void setRouterData(RouterData routerData) {
        this.routerData = routerData;
    }

    /**
     * @param routerPassword
     */
    public void setRouterPassword(String routerPassword) {
        this.routerPassword = routerPassword;
    }

    /**
     * @param routerUsername
     */
    public void setRouterUsername(String routerUsername) {
        this.routerUsername = routerUsername;
    }

    /**
     * Setzt die Wartezeit zwischen dem Reconnect und dem ersten IP-Check
     * 
     * @param waitForIPCheck
     */
    public void setWaitForIPCheck(int waitForIPCheck) {
        this.waitForIPCheck = waitForIPCheck;
    }

    /**
     * Gibt alle Properties der Config aus
     * 
     * @return toString
     */
    @Override
    public String toString() {
        return "Configuration " + getProperties() + " Interaction " + interactions;
    }
}
