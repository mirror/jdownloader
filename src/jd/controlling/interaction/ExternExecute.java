//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.utils.JDUtilities;
import jd.utils.Replacer;
import jd.utils.locale.JDL;

public class ExternExecute extends Interaction implements Serializable, ActionListener {

    private static final long serialVersionUID = 4793649294489149258L;

    private static final String PROPERTY_COMMAND = "InteractionExternExecute_Command";
    private static final String PROPERTY_DISABLED = "PROPERTY_DISABLED";
    private static final String PROPERTY_EXECUTE_FOLDER = "PROPERTY_EXECUTE_FOLDER";
    private static final String PROPERTY_PARAMETER = "PROPERTY_PARAMETER";
    private static final String PROPERTY_WAIT_FOR_RETURN = "PROPERTY_WAIT_FOR_RETURN";

    public void actionPerformed(ActionEvent e) {
        doInteraction(null);
    }

    //@Override
    public boolean doInteraction(Object arg) {
        if (getBooleanProperty(PROPERTY_DISABLED, false)) {
            logger.info("deaktiviert");
            return false;
        }

        int waitForReturn = getIntegerProperty(PROPERTY_WAIT_FOR_RETURN, 0);
        String executeIn = Replacer.insertVariables(getStringProperty(PROPERTY_EXECUTE_FOLDER));
        String command = Replacer.insertVariables(getStringProperty(PROPERTY_COMMAND));
        String parameter = Replacer.insertVariables(getStringProperty(PROPERTY_PARAMETER));

        logger.info(getStringProperty(PROPERTY_COMMAND));

        logger.finer("Execute Returns: " + JDUtilities.runCommand(command, Regex.getLines(parameter), executeIn, waitForReturn));
        return true;
    }

    //@Override
    public String getInteractionName() {
        return JDL.L("interaction.externExecute.name", "Extern Execute");
    }

    //@Override
    public void initConfig() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDL.L("interaction.externExecute.test", "Programm aufrufen"),JDL.L("interaction.externExecute.test.long", "Test program execution"),null));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, this, "VARS", Replacer.getKeyList(), JDL.L("interaction.externExecute.variables", "Available variables")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this, PROPERTY_DISABLED, JDL.L("interaction.externExecute.disable", "Event deaktiviert")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, this, PROPERTY_COMMAND, JDL.L("interaction.externExecute.cmd", "Befehl (absolute Pfade verwenden)")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, this, PROPERTY_PARAMETER, JDL.L("interaction.externExecute.parameter", "Parameter (1 Parameter pro Zeile)")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, this, PROPERTY_EXECUTE_FOLDER, JDL.L("interaction.externExecute.executeIn", "Ausführen in (Ordner der Anwendung)")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, PROPERTY_WAIT_FOR_RETURN, JDL.L("interaction.externExecute.waitForTermination", "Warten x Sekunden bis Befehl beendet ist [sek](-1 für unendlich)"), -1, 1800).setDefaultValue(0));
    }

    //@Override
    public String toString() {
        return JDL.L("interaction.externExecute.toString", "Externes Programm aufrufen");
    }
}
