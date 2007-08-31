package jd.controlling.interaction;

import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;

import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.router.RouterData;

/**
 * Diese Klasse führt einen Reconnect durch
 * 
 * @author astaldo
 */
public class HTTPReconnect extends Interaction{

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1332164738388120767L;
    private static final String NAME ="HTTPReconnect";
    public static String VAR_USERNAME = "%USERNAME%";
    public static String VAR_PASSWORD = "%PASSWORD%";
    

    @Override
    public boolean interact() {
        logger.info("Starting HTTPReconnect");
        String ipBefore;
        String ipAfter;
        RouterData routerData = configuration.getRouterData();
        String routerIP       = configuration.getRouterIP();
        String routerUsername = configuration.getRouterUsername();
        String routerPassword = configuration.getRouterPassword();
        int routerPort        = configuration.getRouterPort();
        String login          = routerData.getLogin();
        String disconnect     = routerData.getDisconnect();
        String connect        = routerData.getConnect();
        if(routerUsername != null && routerPassword != null)
            Authenticator.setDefault(new InternalAuthenticator(routerUsername, routerPassword));

        //IP auslesen
//        ipBefore = getIPAddress(routerData);

        String routerPage;
        
        //RouterPage zusammensetzen
        if(routerPort<=0)
            routerPage = "http://"+routerIP+"/";
        else
            routerPage = "http://"+routerIP+":"+routerPort+"/";
        RequestInfo requestInfo = null;
        if(login != null){
            login.replaceAll(VAR_USERNAME, routerUsername);
            login.replaceAll(VAR_PASSWORD, routerPassword);
            
            //Anmelden
            requestInfo = doThis(
                    "Login",
                    routerPage+login,
                    requestInfo,
                    routerData.getLoginRequestProperties(),
                    routerData.getLoginPostParams(),
                    routerData.getLoginType());
        }
        
        //Disconnect
        requestInfo = doThis(
                "Disconnect",
                routerPage+disconnect,
                requestInfo,
                routerData.getDisconnectRequestProperties(),
                routerData.getDisconnectPostParams(),
                routerData.getDisconnectType());

        // Verbindung wiederaufbauen
        logger.fine("building connection");
        requestInfo = doThis(
                "Rebuild",
                routerPage+connect,
                null,
                null,
                null,
                RouterData.TYPE_WEB_GET);


        // IP check
//        ipAfter = getIPAddress(routerData);
//        if(ipBefore.equals(ipAfter)){
//            logger.severe("IP address did not change");
//            return false;
//        }

        return true;
    }
//    private String getIPAddress(RouterData routerData){
//        try {
//            String urlForIPAddress = routerData.getIpAddressSite();
//            RequestInfo requestInfo = Plugin.getRequest(new URL(urlForIPAddress));
//            return routerData.getIPAdress(requestInfo.getHtmlCode());
//        }
//        catch (IOException e1) { e1.printStackTrace(); }
//        return null;
//
//    }
    @Override
    public String toString() { return "HTTPReconnect"; }
    @Override
    public String getName() {
        return NAME;
    }
    private RequestInfo doThis(String action, String page, RequestInfo requestInfo, HashMap<String, String> requestProperties, String params, int type){
        RequestInfo newRequestInfo = null;
        if(type == RouterData.TYPE_WEB_POST){
            logger.fine(action+" via POST:"+page);
            try {
                if(requestInfo == null){
                    newRequestInfo = Plugin.postRequest(
                        new URL(page),
                        params);
                }
                else{
                    newRequestInfo = Plugin.postRequest(
                            new URL(page),
                            requestInfo.getCookie(),
                            null,
                            requestProperties, 
                            params,
                            true);
                }
            }
            catch (MalformedURLException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }
        }
        else{
            logger.fine(action+" via GET:"+page);
            try {
                if(requestProperties == null){
                    newRequestInfo = Plugin.getRequest(
                            new URL(page));
                }
                else{
                    newRequestInfo = Plugin.getRequest(
                            new URL(page),
                            requestInfo.getCookie(),
                            null,
                            true);
                }
            }
            catch (MalformedURLException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }
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
}
