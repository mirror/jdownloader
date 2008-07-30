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


package jd.controlling.interaction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.Regex;
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
     * serialVersionUID
     */
    private static final String NAME                      = JDLocale.L("interaction.externExecute.name","Extern Execute");
    /**
     * Unter diesen Namen werden die entsprechenden Parameter gespeichert
     *
     */
    public static String        PROPERTY_COMMAND          = "InteractionExternExecute_" + "Command";
    private static final String PROPERTY_DISABLED = "PROPERTY_DISABLED";
    private static final String PROPERTY_EXECUTE_FOLDER = "PROPERTY_EXECUTE_FOLDER";
    private static final String PROPERTY_PARAMETER = "PROPERTY_PARAMETER";

    private static final String PROPERTY_WAIT_FOR_RETURN = "PROPERTY_WAIT_FOR_RETURN";
    /**
     * Unter diesen Namen werden die entsprechenden Parameter gespeichert
     *
     */
    public static String        PROPERTY_WAIT_TERMINATION = "InteractionExternExecute_" + "WaitTermination";
    /**
     * 
     */
    private static final long   serialVersionUID          = 4793649294489149258L;
    
    public void actionPerformed(ActionEvent e) {
        
       this.doInteraction(null);
        
    }
    
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
     
  
       logger.finer("Execute Returns: "+ JDUtilities.runCommand(command, Regex.getLines(parameter), executeIn, waitForReturn));
       return true;
    
    }
    
    @Override
    public String getInteractionName() {
        return NAME;
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
    
    @Override
    public void run() {
    // Nichts zu tun. Interaction braucht keinen Thread
    }
    @Override
    public String toString() {
        return JDLocale.L("interaction.externExecute.toString","Externes Programm aufrufen");
    }
}
