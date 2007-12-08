package jd.controlling.interaction;

import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.ProgressController;
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
    private static final long        serialVersionUID                   = 4793649294489149258L;

    /**
     * parameternamen. Hier findet man nur die Namen! unter denen die parameter
     * in die hashmap abgelegt werden
     */


    /**
     * Gibt den reconnectbefehl an
     */
    public static String             PROPERTY_RECONNECT_COMMAND         = "InteractionExternReconnect_" + "Command";

 
    public static final String       PROPERTY_IP_WAIT_FOR_RETURN        = "WAIT_FOR_RETURN";

    /**
     * serialVersionUID
     */
    private static final String      NAME                               = "Extern Reconnect";

    private transient String         lastIP;

    /**
     * Maximale Reconnectanzahl
     */
    private static final int         MAX_RETRIES                        = 10;

    private static final String      PROPERTY_EXTERN_RECONNECT_DISABLED = "EXTERN_RECONNECT_DISABLED";

    private static final String      PROPERTY_RECONNECT_EXECUTE_FOLDER  = "RECONNECT_EXECUTE_FOLDER";

    private static final String PROPERTY_RECONNECT_PARAMETER = "RECONNECT_PARAMETER";

    private static final String PARAM_IPCHECKWAITTIME = "IPCHECKWAITTIME";

    private static final String PARAM_RETRIES = "RETRIES";

    private static final String PARAM_WAITFORIPCHANGE = "WAITFORIPCHANGE";

    private transient static boolean enabled                            = false;

    private int                      retries                            = 0;

    @Override
    public boolean doInteraction(Object arg) {
        if (!isEnabled() || getBooleanProperty(PROPERTY_EXTERN_RECONNECT_DISABLED, false)) {
            logger.info("Reconnect deaktiviert");
            return false;
        }
       
        retries++;
     ProgressController progress= new ProgressController(10);
     
     progress.setStatusText("ExternReconnect #"+retries);
       
        int waitForReturn=getIntegerProperty(PROPERTY_IP_WAIT_FOR_RETURN, 0);
        String executeIn=getStringProperty(PROPERTY_RECONNECT_EXECUTE_FOLDER);
        String command=getStringProperty(PROPERTY_RECONNECT_COMMAND);
        String parameter=getStringProperty(PROPERTY_RECONNECT_PARAMETER);
   
        int waittime = getIntegerProperty(PARAM_IPCHECKWAITTIME, 0);
        int maxretries = getIntegerProperty(PARAM_RETRIES, 0);
        int waitForIp = getIntegerProperty(PARAM_WAITFORIPCHANGE, 10);
       
        logger.info("Starting " + NAME + " #" + retries);
        String preIp = JDUtilities.getIPAddress();

        progress.increase(1);
        progress.setStatusText("ExternReconnect Old IP:"+preIp);
        logger.finer("IP befor: " + preIp);
       logger.finer("Execute Returns: "+ JDUtilities.runCommand(command, JDUtilities.splitByNewline(parameter), executeIn, waitForReturn));
        logger.finer("Wait " + waittime + " seconds ...");
        try {
            Thread.sleep(waittime * 1000);
        }
        catch (InterruptedException e) {
        }

        String afterIP = JDUtilities.getIPAddress();
        logger.finer("Ip after: " + afterIP);
        progress.setStatusText("ExternReconnect New IP:"+afterIP+"("+preIp+")");
        long endTime = System.currentTimeMillis() + waitForIp * 1000;
        logger.info("Wait "+waitForIp+" sek for new ip");
       
        while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp)|| afterIP.equals("offline"))) {
            try {
                Thread.sleep(5 * 1000);
            }
            catch (InterruptedException e) {
            }
            afterIP = JDUtilities.getIPAddress();
            progress.setStatusText("(WAITING)ExternReconnect New IP:"+afterIP+"("+preIp+")");
            logger.finer("Ip Check: " + afterIP);
        }
        if (!afterIP.equals(preIp) && !afterIP.equals("offline")) {
            progress.finalize();
            return true;
        }
        logger.finer("Retries: "+retries+"/"+maxretries);
        if (retries <= maxretries) {
            progress.finalize();
            return doInteraction(arg);
        }
        progress.finalize();
        return false;
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
    // Nichts zu tun. Interaction braucht keinen Thread
    }

    @Override
    public void initConfig() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this, PROPERTY_EXTERN_RECONNECT_DISABLED, "Event deaktiviert").setDefaultValue(false).setExpertEntry(true));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, this, PROPERTY_RECONNECT_COMMAND, "Befehl (absolute Pfade verwenden)"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, this, PROPERTY_RECONNECT_PARAMETER, "Parameter (1 Parameter/Zeile)").setExpertEntry(true));

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, this, PROPERTY_RECONNECT_EXECUTE_FOLDER, "Ausführen in (Ordner der Anwendung)"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, PARAM_IPCHECKWAITTIME, "Wartezeit bis zum ersten IP-Check[sek]", 0, 600).setDefaultValue(5).setExpertEntry(true));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, PARAM_RETRIES, "Max. Wiederholungen (-1 = unendlich)", -1, 20).setDefaultValue(5).setExpertEntry(true));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, PARAM_WAITFORIPCHANGE, "Auf neue IP warten [sek]", 0, 600).setDefaultValue(20).setExpertEntry(true));

        
        
       config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, PROPERTY_IP_WAIT_FOR_RETURN, "Warten x Sekunden bis Befehl beendet ist[sek]",0,600).setDefaultValue(0).setExpertEntry(true));
  
     
    }

    @Override
    public void resetInteraction() {
        retries = 0;
    }

}
