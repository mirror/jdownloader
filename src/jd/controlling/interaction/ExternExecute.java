package jd.controlling.interaction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Replacer;

/**
 * Diese Klasse führt einen Reconnect durch
 * 
 * @author astaldo
 */
public class ExternExecute extends Interaction implements Serializable,ActionListener {
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
    private static final String NAME                      = JDLocale.L("interaction.externExecute.name","Extern Execute");
    private static final String PROPERTY_DISABLED = "PROPERTY_DISABLED";

    private static final String PROPERTY_PARAMETER = "PROPERTY_PARAMETER";
    private static final String PROPERTY_EXECUTE_FOLDER = "PROPERTY_EXECUTE_FOLDER";
    private static final String PROPERTY_WAIT_FOR_RETURN = "PROPERTY_WAIT_FOR_RETURN";
    @Override
    public boolean doInteraction(Object arg) {
        
        if (getBooleanProperty(PROPERTY_DISABLED, false)) {
            logger.info("deaktiviert");
            return false;
        }
       
        int waitForReturn=getIntegerProperty(PROPERTY_WAIT_FOR_RETURN, 0);
        String executeIn=Replacer.insertVariables(getStringProperty(PROPERTY_EXECUTE_FOLDER));
        String command=Replacer.insertVariables(getStringProperty(PROPERTY_COMMAND));
        String parameter=Replacer.insertVariables(getStringProperty(PROPERTY_PARAMETER));   
        
        logger.info(getStringProperty(PROPERTY_COMMAND));
     
  
       logger.finer("Execute Returns: "+ JDUtilities.runCommand(command, JDUtilities.splitByNewline(parameter), executeIn, waitForReturn));
       return true;
    
    }
    @Override
    public String toString() {
        return JDLocale.L("interaction.externExecute.toString","Externes Programm aufrufen");
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
        
       // ConfigEntry cfg;
        //(int type, ActionListener listener, String label)
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDLocale.L("interaction.externExecute.test","Programm aufrufen")));
        
        String[] keys=new  String[Replacer.KEYS.length];
        for( int i=0; i<Replacer.KEYS.length;i++){
            keys[i]="%"+Replacer.KEYS[i][0]+"%"+"   ("+Replacer.KEYS[i][1]+")";
        }
        
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, this, "VARS", keys, JDLocale.L("interaction.externExecute.variables","Available variables")));
        
        
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this, PROPERTY_DISABLED, JDLocale.L("interaction.externExecute.disable","Event deaktiviert")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, this, PROPERTY_COMMAND, JDLocale.L("interaction.externExecute.cmd","Befehl (absolute Pfade verwenden)")));
        
       config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, this, PROPERTY_PARAMETER, JDLocale.L("interaction.externExecute.parameter","Parameter (1 Parameter pro Zeile)")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, this, PROPERTY_EXECUTE_FOLDER, JDLocale.L("interaction.externExecute.executeIn","Ausführen in (Ordner der Anwendung)")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, PROPERTY_WAIT_FOR_RETURN, JDLocale.L("interaction.externExecute.waitForTermination","Warten x Sekunden bis Befehl beendet ist[sek](-1 für unendlich)"),-1,1800).setDefaultValue(0));
        
      
    }
    @Override
    public void resetInteraction() {
    }
    public void actionPerformed(ActionEvent e) {
        
       this.doInteraction(null);
        
    }
}
