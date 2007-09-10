package jd.controlling.interaction;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.JDUtilities;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;

/**
 * Diese Klasse ruft ein Externes Programm auf. Anschließend wird auf eine Neue IP geprüft
 * 
 * @author coalado
 */
public class ExternReconnect extends Interaction implements Serializable {

    
    /**
     * Unter diesen Namen werden die entsprechenden Parameter gespeichert
     *
     */
    private static final long   serialVersionUID           = 4793649294489149258L;
/**
 * parameternamen. Hier findet man nur die Namen! unter denen die parameter in die hashmap abgelegt werden
 */
    /**
     * Regex zum IP finden
     */
    public static String        PROPERTY_IP_REGEX          = "InteractionExternReconnect_" + "IPAddressRegEx";
/**
 * Offlinestring. wird aufd er zielseite dieser string gefunden gilt die Verbindung als beendet
 */
    public static String        PROPERTY_IP_OFFLINE        = "InteractionExternReconnect_" + "IPAddressOffline";
/**
 * Gibt an wie lange nach dem Programmaufruf gewartet werden soll bis zu ip überprüfung
 */
    public static String        PROPERTY_IP_WAITCHECK      = "InteractionExternReconnect_" + "WaitIPCheck";
/**
 * Gibt an wieviele Versuche unternommen werden
 */
    public static String        PROPERTY_IP_RETRIES        = "InteractionExternReconnect_" + "Retries";
/**
 * Gibt den reconnectbefehl an
 */
    public static String        PROPERTY_RECONNECT_COMMAND = "InteractionExternReconnect_" + "Command";
/**
 * Seite zur IP Prüfung
 */
    public static String        PROPERTY_IP_CHECK_SITE     = "InteractionExternReconnect_" + "Site";

    /**
     * serialVersionUID
     */

    private static final String NAME                       = "Extern Reconnect";
    /**
     * Maximale Reconnectanzahl
     */
    private static final int MAX_RETRIES = 10;

    private int                 retries                    = 0;

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
        logger.fine("IP before:" + ipBefore);
        try {
            logger.info(JDUtilities.runCommandWaitAndReturn((String) JDUtilities.getConfiguration().getProperty(PROPERTY_RECONNECT_COMMAND)));
        }
        catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return false;
        }
        if (JDUtilities.getConfiguration().getProperty(PROPERTY_IP_WAITCHECK) != null) {
            try {
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
            if (retries<ExternReconnect.MAX_RETRIES &&(retries < maxRetries || maxRetries <= 0)) {
                return doInteraction(arg);
            }
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            retries = 0;
            return false;
        }
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

    @Override
    public void run() {
    // Nichts zu tun. INteraction braucht keinen Thread

    }
}
