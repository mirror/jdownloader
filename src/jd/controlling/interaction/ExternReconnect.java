package jd.controlling.interaction;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ruft ein Externes Programm auf. Anschließend wird auf eine Neue
 * IP geprüft
 * 
 * @author coalado
 */
public class ExternReconnect extends Interaction implements Serializable {

    /**
     * Unter diesen Namen werden die entsprechenden Parameter gespeichert
     * 
     */
    private static final long        serialVersionUID            = 4793649294489149258L;

    /**
     * parameternamen. Hier findet man nur die Namen! unter denen die parameter
     * in die hashmap abgelegt werden
     */
    /**
     * Regex zum IP finden
     */
    public static String             PROPERTY_IP_REGEX           = "InteractionExternReconnect_" + "IPAddressRegEx";

    /**
     * Offlinestring. wird aufd er zielseite dieser string gefunden gilt die
     * Verbindung als beendet
     */
    public static String             PROPERTY_IP_OFFLINE         = "InteractionExternReconnect_" + "IPAddressOffline";

    /**
     * Gibt an wie lange nach dem Programmaufruf gewartet werden soll bis zu ip
     * überprüfung
     */
    public static String             PROPERTY_IP_WAITCHECK       = "InteractionExternReconnect_" + "WaitIPCheck";

    /**
     * Gibt an wieviele Versuche unternommen werden
     */
    public static String             PROPERTY_IP_RETRIES         = "InteractionExternReconnect_" + "Retries";

    /**
     * Gibt den reconnectbefehl an
     */
    public static String             PROPERTY_RECONNECT_COMMAND  = "InteractionExternReconnect_" + "Command";

    /**
     * Seite zur IP Prüfung
     */
    public static String             PROPERTY_IP_CHECK_SITE      = "InteractionExternReconnect_" + "Site";

    public static final String       PROPERTY_IP_WAIT_FOR_RETURN = "InteractionExternReconnect_" + "WaitReturn";

    /**
     * serialVersionUID
     */

    private static final String      NAME                        = "Extern Reconnect";
    private transient String lastIP;
    /**
     * Maximale Reconnectanzahl
     */
    private static final int         MAX_RETRIES                 = 10;

    private transient static boolean enabled                     = false;

    private int                      retries                     = 0;

    @Override
    public boolean doInteraction(Object arg) {
        retries++;
        int maxRetries = 0;
        if (JDUtilities.getConfiguration().getProperty(PROPERTY_IP_RETRIES) != null) maxRetries = Integer.parseInt((String) JDUtilities.getConfiguration().getProperty(PROPERTY_IP_RETRIES));
        logger.info("Starting " + NAME + " #" + retries);
        String ipBefore;
        String ipAfter;

        // IP auslesen
        ipBefore = getIPAddress();
        if(ipBefore!=null&&lastIP!=null && !lastIP.equals(ipBefore)){
            
            
            lastIP=ipBefore;
            logger.info("IP Wechsel vermutet:"+lastIP+ "! Falls nicht auf die Rückkehr des Reconnecttools gewartet wird, sollte die Wartezeit bis zum IP-Check erhöht werden");
            return true;
        }
        if(ipBefore!=null){
            lastIP=ipBefore;
        }
        logger.fine("IP before:" + ipBefore);
        if (JDUtilities.getConfiguration().getProperty(PROPERTY_IP_WAIT_FOR_RETURN) == null || (Boolean) JDUtilities.getConfiguration().getProperty(PROPERTY_IP_WAIT_FOR_RETURN)) {
            logger.info("Warte auf Rückkehr");
            try {
                JDUtilities.runCommandAndWait((String) JDUtilities.getConfiguration().getProperty(PROPERTY_RECONNECT_COMMAND));
            }
            catch (IOException e1) {

                e1.printStackTrace();
                return false;
            }
        }
        else {
            logger.info("Nicht warten");
            try {
                JDUtilities.runCommand((String) JDUtilities.getConfiguration().getProperty(PROPERTY_RECONNECT_COMMAND));
            }
            catch (Exception e) {

                e.printStackTrace();
                return false;
            }
        }
        if (JDUtilities.getConfiguration().getProperty(PROPERTY_IP_WAITCHECK) != null) {
            try {
                logger.fine("Wait "+JDUtilities.getConfiguration().getProperty(PROPERTY_IP_WAITCHECK)+" sek");
                Thread.sleep(Integer.parseInt((String) JDUtilities.getConfiguration().getProperty(PROPERTY_IP_WAITCHECK)) * 1000);
            }
            catch (NumberFormatException e) {
            }
            catch (InterruptedException e) {
            }
        }
        // IP check
        ipAfter = getIPAddress();
        logger.fine("IP after reconnect:" + ipAfter);
        if (ipBefore == null || ipAfter == null || ipBefore.equals(ipAfter)) {
            logger.severe("IP address did not change");
            if (retries < ExternReconnect.MAX_RETRIES && (retries < maxRetries || maxRetries <= 0)) {
                return doInteraction(arg);
            }
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            retries = 0;
            return false;
        }
        lastIP=ipAfter;
        this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        retries = 0;
        return true;
    }

    private String getIPAddress() {
        String urlForIPAddress = (String) JDUtilities.getConfiguration().getProperty(PROPERTY_IP_CHECK_SITE);
        String ipAddressOffline = (String) JDUtilities.getConfiguration().getProperty(PROPERTY_IP_OFFLINE);
        String ipAddressRegEx = (String) JDUtilities.getConfiguration().getProperty(PROPERTY_IP_REGEX);
        try {
            RequestInfo requestInfo = Plugin.getRequest(new URL(urlForIPAddress));
            String data = requestInfo.getHtmlCode();
            String ipAddress = null;
            if (data == null) return null;
            if (ipAddressOffline != null && ipAddressOffline.length() > 0 && data.contains(ipAddressOffline)) {
                logger.fine("offline");
                return null;
            }
            Pattern pattern = Pattern.compile(ipAddressRegEx);
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) ipAddress = matcher.group(1);
            return ipAddress;
        }
        catch (SocketTimeoutException e){
            logger.severe("Timeout. Es wurde keine Verbindung gefunden. Wartezeit bis zum IP check verlängern!" + e.toString());
        }
        catch (IOException e1) {
            logger.severe(urlForIPAddress + " url not found. " + e1.toString());
        }
        return null;

    }

    @Override
    public String toString() {
        return "Externes Reconnectprogramm aufrufen";
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean en) {
        enabled = en;
    }

    @Override
    public void run() {
    // Nichts zu tun. INteraction braucht keinen Thread

    }

    @Override
    public void initConfig() {
        // TODO Auto-generated method stub
        
    }
}
