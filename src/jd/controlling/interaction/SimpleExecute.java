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
import java.io.File;
import java.io.Serializable;

import jd.parser.Regex;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Replacer;

public class SimpleExecute extends Interaction implements Serializable, ActionListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String CONFIGNAME = "INTERACTION_SIMPLEEXECUTE";
    public static String PROPERTY_COMMAND = "PROPERTY_COMMAND";

    public static String PROPERTY_WAIT_TERMINATION = "PROPERTY_WAIT_TERMINATION";
    /**
     * serialVersionUID
     */
    private static final String NAME = JDLocale.L("interaction.simpleExecute.name", "Programm/Script ausführen");

    private static final String PROPERTY_PARAMETER = "PROPERTY_PARAMETER";
    private static final String PROPERTY_EXECUTE_IN = "PROPERTY_EXECUTE_IN";
    private static final String PROPERTY_WAITTIME = "PROPERTY_WAITTIME";
    private static final String PROPERTY_USE_EXECUTE_IN = "PROPERTY_USE_EXECUTE_IN";

    @Override
    public boolean doInteraction(Object arg) {

        String command = Replacer.insertVariables(getStringProperty(PROPERTY_COMMAND));
        String parameter = Replacer.insertVariables(getStringProperty(PROPERTY_PARAMETER));

        logger.info(getStringProperty(PROPERTY_COMMAND));
        File path = new File(command);

        if (!path.exists()) {
            String[] params = Regex.getLines(parameter);
            if (params.length > 0) {
                path = new File(params[0]);
            }

        }
        String executeIn = path.getParent();
        if(getStringProperty(PROPERTY_EXECUTE_IN,null)!= null &&getStringProperty(PROPERTY_EXECUTE_IN,null).length()>0&&getBooleanProperty("PROPERTY_USE_EXECUTE_IN",false)){
            executeIn=getStringProperty(PROPERTY_EXECUTE_IN,null);
        }

        logger.finer("Execute Returns: " + JDUtilities.runCommand(command, Regex.getLines(parameter), executeIn, this.getBooleanProperty(PROPERTY_WAIT_TERMINATION, false) ? getIntegerProperty(PROPERTY_WAITTIME,60) : 0));
        return true;

    }

    @Override
    public String toString() {
        return NAME;
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
       
        ConfigEntry cfg;
        ConfigEntry conditionEntry;
        // ConfigEntry cfg;
        // (int type, ActionListener listener, String label)
        ConfigContainer extended = new ConfigContainer(this, JDLocale.L("interaction.simpleExecute.extended", "Erweiterte Einstellungen"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CONTAINER, extended));
        config.addEntry(cfg=new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDLocale.L("interaction.simpleExecute.test", "Jetzt ausführen")));

       
        //extended.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, this, "VARS", keys, JDLocale.L("interaction.simpleExecute.variables", "Available variables")).setExpertEntry(true));
    
       config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, this, PROPERTY_COMMAND, JDLocale.L("interaction.simpleExecute.cmd", "Befehl")).setInstantHelp(JDLocale.L("interaction.simpleExecute.helpLink","http://jdownloader.org/page.php?id=119")));

       extended.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, this, PROPERTY_PARAMETER, JDLocale.L("interaction.simpleExecute.parameter", "Parameter (1 Parameter pro Zeile)")).setExpertEntry(true));


       config.addEntry(conditionEntry=new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this, PROPERTY_WAIT_TERMINATION, JDLocale.L("interaction.simpleExecute.waitForTermination", "Warten bis Befehl beendet wurde")).setDefaultValue(false));
       extended.addEntry(cfg=new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, PROPERTY_WAITTIME, JDLocale.L("interaction.simpleExecute.waittime", "Maximale Ausführzeit"),0,60*60*24).setDefaultValue(60));
       cfg.setEnabledCondidtion(conditionEntry, "==", true);
       
       extended.addEntry(conditionEntry=new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this, PROPERTY_USE_EXECUTE_IN, JDLocale.L("interaction.simpleExecute.useexecutein", "Benutzerdefiniertes 'Ausführen in'")).setDefaultValue(false));

       extended.addEntry(cfg=new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, this, PROPERTY_EXECUTE_IN, JDLocale.L("interaction.simpleExecute.executein", "Ausführen in (Ordner)")));
       cfg.setEnabledCondidtion(conditionEntry, "==", true);
    }

    @Override
    public void resetInteraction() {
    }

    public void actionPerformed(ActionEvent e) {

       
        this.getConfig().requestSave();
        this.doInteraction(null);
    }


}
