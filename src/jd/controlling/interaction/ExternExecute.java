package jd.controlling.interaction;

import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.utils.JDUtilities;

/**
 * Diese Klasse führt einen Reconnect durch
 * 
 * @author astaldo
 */
public class ExternExecute extends Interaction implements Serializable {
    /**
     * 
     */
    private static final long   serialVersionUID          = 4793649294489149258L;
    /**
     * Unter diesen Namen werden die entsprechenden Parameter gespeichert
     *
     */
    public static String        PROPERTY_COMMAND          = "InteractionExternExecute_" + "Command";
    /**
     * Unter diesen Namen werden die entsprechenden Parameter gespeichert
     *
     */
    public static String        PROPERTY_WAIT_TERMINATION = "InteractionExternExecute_" + "WaitTermination";
    /**
     * serialVersionUID
     */
    private static final String NAME                      = "Extern Execute";
    @Override
    public boolean doInteraction(Object arg) {
        boolean wait = (Boolean) getProperty(PROPERTY_WAIT_TERMINATION);
        String command = (String) getProperty(PROPERTY_COMMAND);
        if (command == null) return false;
        command = JDUtilities.replacePlaceHolder(command);
        logger.info("Call " + command);
       
       
            JDUtilities.runCommand(command, "", null, wait?60:0);
            return true;
    
    }
    @Override
    public String toString() {
        return "Externes Programm aufrufen";
    }
    @Override
    public String getInteractionName() {
        return NAME;
    }
    @Override
    public void run() {
    // Nichts zu tun. Interaction braucht keinen Thread
    }
    @Override
    public void initConfig() {
      
    }
    @Override
    public void resetInteraction() {
    }
}
