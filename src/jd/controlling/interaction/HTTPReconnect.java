package jd.controlling.interaction;

import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

import jd.Configuration;
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
    private transient static final long serialVersionUID = 1332164738388120767L;

    @Override
    public boolean interact() {
        logger.info("trying to reconnect..");
        String ipBefore;
        String ipAfter;
        RouterData routerData = Configuration.getRouterData();
        String routerUsername = Configuration.getRouterUsername();
        String routerPassword = Configuration.getRouterPassword();
        String disconnect     = routerData.getConnectionDisconnect();
        String connect        = routerData.getConnectionConnect();
        Authenticator.setDefault(new InternalAuthenticator(routerUsername, routerPassword));

        //IP auslesen
        ipBefore = getIPAddress(routerData);

        //Trennen
        logger.fine("disconnecting router");
        if(disconnect.startsWith(RouterData.HTTP_POST)){
            disconnect = disconnect.substring(RouterData.HTTP_POST.length()+1);
            String[] params = disconnect.split("\\?"); 
            try {
                Plugin.postRequest(new URL(params[0]), params[1]);
            }
            catch (MalformedURLException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }
        }
        else{
            if(disconnect.startsWith(RouterData.HTTP_GET)){
                disconnect = disconnect.substring(RouterData.HTTP_GET.length());
            }
            try {
                Plugin.getRequest(new URL(disconnect));
            }
            catch (MalformedURLException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }
        }

        // Verbindung wiederaufbauen
        logger.fine("building connection");
        try {
            Plugin.getRequest(new URL(connect));
        }
        catch (MalformedURLException e) { e.printStackTrace(); }
        catch (IOException e)           { e.printStackTrace(); }

        // IP check
        ipAfter = getIPAddress(routerData);
        if(ipBefore.equals(ipAfter)){
            logger.severe("IP address did not change");
            return false;
        }

        return true;
    }
    private String getIPAddress(RouterData routerData){
        try {
            String urlForIPAddress = routerData.getStatusIPAddress().getWebsite();
            RequestInfo requestInfo = Plugin.getRequest(new URL(urlForIPAddress));
            return routerData.getIPAdress(requestInfo.getHtmlCode());
        }
        catch (IOException e1) { e1.printStackTrace(); }
        return null;

    }
    @Override
    public String toString() { return "HTTPReconnect "+Configuration.getRouterData(); }

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
