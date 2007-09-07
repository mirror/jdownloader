package jd.controlling.interaction;

import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import jd.Configuration;
import jd.JDUtilities;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.router.RouterData;

/**
 * Diese Klasse führt einen Reconnect durch
 * 
 * @author astaldo
 */
public class HTTPReconnect extends Interaction {

    /**
     * serialVersionUID
     */
    private static final long   serialVersionUID = 1332164738388120767L;

    private static final String NAME             = "HTTPReconnect";

    public static String        VAR_USERNAME     = "%USERNAME%";

    public static String        VAR_PASSWORD     = "%PASSWORD%";

    private int                 retries          = 0;

    @Override
    public boolean doInteraction(Object arg) {
        Configuration configuration = JDUtilities.getConfiguration();
        retries++;
        logger.info("Starting HTTPReconnect #" + retries);
        String ipBefore;
        String ipAfter;
        RouterData routerData = configuration.getRouterData();
        String routerIP = routerData.getRouterIP();
        String routerUsername = configuration.getRouterUsername();
        String routerPassword = configuration.getRouterPassword();
        int routerPort = routerData.getRouterPort();
        String login = routerData.getLogin();
        String disconnect = routerData.getDisconnect();
        String connect = routerData.getConnect();
        if (routerUsername != null && routerPassword != null) Authenticator.setDefault(new InternalAuthenticator(routerUsername, routerPassword));

        String routerPage;

        // RouterPage zusammensetzen
        if (routerPort <= 0)
            routerPage = "http://" + routerIP + "/";
        else
            routerPage = "http://" + routerIP + ":" + routerPort + "/";
        RequestInfo requestInfo = null;

        // IP auslesen
        ipBefore = getIPAddress(routerPage, routerData);
        logger.fine("IP before:" + ipBefore);

        if (login != null && !login.equals("")) {
            login.replaceAll(VAR_USERNAME, routerUsername);
            login.replaceAll(VAR_PASSWORD, routerPassword);

            // Anmelden
            requestInfo = doThis("Login", isAbsolute(login) ? login : routerPage + login, requestInfo, routerData.getLoginRequestProperties(), routerData.getLoginPostParams(), routerData.getLoginType());
            if (requestInfo == null) {
                logger.severe("Login failed.");
                this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
               
                return false;
            }
            else if (!requestInfo.isOK()) {
                logger.severe("Login failed. HTTP-Code:" + requestInfo.getResponseCode());
                this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
                
                return false;
            }
        }
        // Disconnect
        requestInfo = doThis("Disconnect", isAbsolute(disconnect) ? disconnect : routerPage + disconnect, requestInfo, routerData.getDisconnectRequestProperties(), routerData.getDisconnectPostParams(), routerData.getDisconnectType());

        if (requestInfo == null) {
            logger.severe("Disconnect failed.");
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
          
            return false;
        }
        else if (!requestInfo.isOK()) {
            logger.severe("Disconnect failed HTTP-Code:" + requestInfo.getResponseCode());
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
           
            return false;
        }
        try {
            Thread.sleep(500);
        }
        catch (InterruptedException e) {
        }
        // Verbindung wiederaufbauen
        logger.fine("building connection");
        requestInfo = doThis("Rebuild", isAbsolute(connect) ? connect : routerPage + connect, null, routerData.getConnectRequestProperties(), routerData.getConnectPostParams(), routerData.getConnectType());

        if (requestInfo == null) {
            logger.severe("Reconnect failed.");
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            
            return false;
        }
        else if (!requestInfo.isOK()) {
            logger.severe("Reconnect failed. HTTP-Code:" + requestInfo.getResponseCode());
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
           
            return false;
        }

        // IP check
        ipAfter = getIPAddress(routerPage, routerData);
        logger.fine("IP after reconnect:" + ipAfter);
        if (ipBefore == null || ipAfter == null || ipBefore.equals(ipAfter)) {
            logger.severe("IP address did not change");
            if (retries < configuration.getReconnectRetries() || configuration.getReconnectRetries() <= 0) {
                return interact(arg);
            }
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            retries=0;
            return false;
        }
        this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        retries=0;
        return true;
    }

    private String getIPAddress(String routerPage, RouterData routerData) {
        String urlForIPAddress;
        try {
            if (isAbsolute(routerData.getIpAddressSite())) {
                urlForIPAddress = routerData.getIpAddressSite();
            }
            else {
                urlForIPAddress = routerPage + routerData.getIpAddressSite();
            }

            RequestInfo requestInfo = Plugin.getRequest(new URL(urlForIPAddress));
            return routerData.getIPAdress(requestInfo.getHtmlCode());
        }
        catch (IOException e1) {
            logger.severe("url not found. " + e1.toString());
        }
        return null;

    }

    @Override
    public String toString() {
        return "HTTPReconnect";
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    /**
     * @author coalado
     * @param url
     * @return Prüft ob enie url Absolut ist.
     */
    private boolean isAbsolute(String url) {
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
                e.printStackTrace();
            }
            catch (IOException e) {
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

    @Override
    public void run() {
        //Nichts zu tun. INteraction braucht keinen Thread
        
    }
}
