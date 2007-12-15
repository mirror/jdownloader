package jd.controlling.interaction;

import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import jd.config.Configuration;
import jd.controlling.ProgressController;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.router.RouterData;
import jd.utils.JDUtilities;

/**
 * Diese Klasse führt einen Reconnect durch
 * 
 * @author astaldo
 */
public class HTTPReconnect extends Interaction {
    private transient static boolean enabled          = true;
    /**
     * 
     */
    private static final long        serialVersionUID = 5208110144587103071L;
    /**
     * serialVersionUID
     */
    private static final String      NAME             = "HTTP Reconnect(routercontrol)";
    /**
     * Maximal 10 versuche
     */
    private static final int         MAX_RETRIES      = 10;
    public static String             VAR_USERNAME     = "%USERNAME%";
    public static String             VAR_PASSWORD     = "%PASSWORD%";
    private int                      retries          = 0;
    @Override
    public boolean doInteraction(Object arg) {
        if (!isEnabled()) {
            logger.info("Reconnect deaktiviert");
            return false;
        }
        ProgressController progress = new ProgressController("HTTPReconnect",5);
        Configuration configuration = JDUtilities.getConfiguration();
        retries++;
        logger.info("Starting HTTPReconnect #" + retries);
        progress.setStatusText("HTTPReconnect retry #"+retries);
        progress.increase(1);
        String ipBefore;
        String ipAfter;
        RouterData routerData = configuration.getRouterData();
        if (routerData == null) {
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            progress.finalize();
            return false;
        }
        String routerIP = routerData.getRouterIP();
        String routerUsername = configuration.getRouterUsername();
        String routerPassword = configuration.getRouterPassword();
        int routerPort = routerData.getRouterPort();
        String login = routerData.getLogin();
        String disconnect = routerData.getDisconnect();
        String connect = routerData.getConnect();
        int waitTime = configuration.getWaitForIPCheck();
        if (routerUsername != null && routerPassword != null) Authenticator.setDefault(new InternalAuthenticator(routerUsername, routerPassword));
        String routerPage;
        // RouterPage zusammensetzen
        if (routerPort <= 0)
            routerPage = "http://" + routerIP + "/";
        else
            routerPage = "http://" + routerIP + ":" + routerPort + "/";
        RequestInfo requestInfo = null;
        // IP auslesen
        ipBefore = JDUtilities.getIPAddress();
        progress.increase(1);
        progress.setStatusText("(login)HTTPReconnect - Old IP: "+ipBefore);
        logger.fine("IP before:" + ipBefore);
        if (login != null && !login.equals("")) {
            login.replaceAll(VAR_USERNAME, routerUsername);
            login.replaceAll(VAR_PASSWORD, routerPassword);
            // Anmelden
            requestInfo = doThis("Login", isAbsolute(login) ? login : routerPage + login, requestInfo, routerData.getLoginRequestProperties(), routerData.getLoginPostParams(), routerData.getLoginType());
            if (requestInfo == null) {
                logger.severe("Login failed.");
                this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
                progress.finalize();
                return false;
            }
            else if (!requestInfo.isOK()) {
                logger.severe("Login failed. HTTP-Code:" + requestInfo.getResponseCode());
                this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
                progress.finalize();
                return false;
            }
        }
        // Disconnect
        progress.increase(1);
        progress.setStatusText("(disconnect)HTTPReconnect - Old IP: "+ipBefore);
        requestInfo = doThis("Disconnect", isAbsolute(disconnect) ? disconnect : routerPage + disconnect, requestInfo, routerData.getDisconnectRequestProperties(), routerData.getDisconnectPostParams(), routerData.getDisconnectType());
        if (requestInfo == null) {
            logger.severe("Disconnect failed.");
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            progress.finalize();
            return false;
        }
        else if (!requestInfo.isOK()) {
            logger.severe("Disconnect failed HTTP-Code:" + requestInfo.getResponseCode());
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            progress.finalize();
            return false;
        }
        try {
            Thread.sleep(500);
        }
        catch (InterruptedException e) {
        }
        
        progress.setStatusText("(connect)HTTPReconnect - Old IP: "+ipBefore);
        // Verbindung wiederaufbauen
        progress.increase(1);
        logger.fine("building connection");
        requestInfo = doThis("Rebuild", isAbsolute(connect) ? connect : routerPage + connect, null, routerData.getConnectRequestProperties(), routerData.getConnectPostParams(), routerData.getConnectType());
        if (requestInfo == null) {
            logger.severe("Reconnect failed.");
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            progress.finalize();
            return false;
        }
        else if (!requestInfo.isOK()) {
            logger.severe("Reconnect failed. HTTP-Code:" + requestInfo.getResponseCode());
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            progress.finalize();
            return false;
        }
        // IP check
        if (waitTime > 0) {
            progress.setRange(progress.getMax()+waitTime);
            progress.setStatusText("(wait)HTTPReconnect - "+waitTime+" Sec");
            logger.fine("wait " + waitTime + " seconds");
            progress.increase(1);
            try {
                Thread.sleep(waitTime * 1000);
            }
            catch (InterruptedException e) {
            }
        }
        // IP check
        progress.increase(1);
        ipAfter = JDUtilities.getIPAddress();
        progress.setStatusText("(ipCheck)HTTPReconnect - "+ipBefore+"/"+ipAfter);
        logger.fine("IP after reconnect:" + ipAfter);
        if (ipBefore == null && ipAfter == null) {
            logger.info("Es konnte keine IP ausgelesen werden.");
            progress.finalize();
            return true;
        }
        if (ipBefore == null || ipAfter == null || ipBefore.equals(ipAfter)) {
            logger.severe("IP address did not change");
            if (retries < HTTPReconnect.MAX_RETRIES && (retries < configuration.getReconnectRetries() || configuration.getReconnectRetries() <= 0)) {
                progress.finalize();
                return doInteraction(arg);
            }
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            retries = 0;
            progress.finalize();
            return false;
        }
        this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        retries = 0;
        progress.finalize();
        return true;
    }
   
    @Override
    public String toString() {
        return "Interner HTTP Reconnect";
    }
    @Override
    public String getInteractionName() {
        return NAME;
    }
    /**
     * @author coalado
     * @param url
     * @return Prüft ob eine URL absolut ist.
     */
    private boolean isAbsolute(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            return uri.isAbsolute();
        }
        catch (URISyntaxException e) {
            return false;
        }
    }
    private RequestInfo doThis(String action, String page, RequestInfo requestInfo, HashMap<String, String> requestProperties, String params, int type) {
        RequestInfo newRequestInfo = null;
        if (type == RouterData.TYPE_WEB_POST) {
            logger.fine(action + " via POST:" + page);
            try {
                if (requestInfo == null) {
                    // newRequestInfo = Plugin.postRequest(
                    // new URL(page),
                    // params);
                    newRequestInfo = Plugin.postRequest(new URL(page), "", null, requestProperties, params, true);
                }
                else if (requestInfo != null) {
                    newRequestInfo = Plugin.postRequest(new URL(page), requestInfo.getCookie(), null, requestProperties, params, true);
                }
                else {
                    newRequestInfo = Plugin.postRequest(new URL(page), params);
                }
            }
            catch (MalformedURLException e) {
                logger.severe("url wrong." + e.toString());
                 e.printStackTrace();
            }
            catch (IOException e) {
                logger.severe("url not found." + e.toString());
                 e.printStackTrace();
            }
        }
        else {
            logger.fine(action + " via GET:" + page);
            try {
                if (requestProperties == null) {
                    newRequestInfo = Plugin.getRequest(new URL(page));
                }
                else if (requestInfo != null) {
                    newRequestInfo = Plugin.getRequest(new URL(page), requestInfo.getCookie(), null, true);
                }
                else {
                    newRequestInfo = Plugin.getRequest(new URL(page));
                }
            }
            catch (MalformedURLException e) {
                logger.severe("url wrong." + e.toString());
            }
            catch (IOException e) {
                logger.severe("url not found." + e.toString());
            }
        }
        return newRequestInfo;
    }
    private class InternalAuthenticator extends Authenticator {
        private String username, password;
        public InternalAuthenticator(String user, String pass) {
            username = user;
            password = pass;
        }
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }
    public static boolean isEnabled() {
        return enabled;
    }
    public static void setEnabled(boolean en) {
        enabled = en;
    }
    @Override
    public void run() {
    // Nichts zu tun. Interaction braucht keinen Thread
    }
    @Override
    public void initConfig() {}
    @Override
    public void resetInteraction() {
        retries = 0;
    }
}
