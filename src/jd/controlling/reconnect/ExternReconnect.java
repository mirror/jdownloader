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

package jd.controlling.reconnect;

import java.io.File;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ruft ein Externes Programm auf. Anschließend wird auf eine Neue
 * IP geprüft
 * 
 * @author JD-Team
 */
public class ExternReconnect extends ReconnectMethod {

    private static final String PROPERTY_IP_WAIT_FOR_RETURN = "WAIT_FOR_RETURN";

    private static final String PROPERTY_RECONNECT_COMMAND = "InteractionExternReconnect_Command";

    private static final String PROPERTY_RECONNECT_PARAMETER = "EXTERN_RECONNECT__PARAMETER";

    public ExternReconnect() {
        configuration = JDUtilities.getConfiguration();
    }

    @Override
    public void initConfig() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, configuration, PROPERTY_RECONNECT_COMMAND, JDLocale.L("interaction.externreconnect.command", "Befehl (absolute Pfade verwenden)")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, PROPERTY_RECONNECT_PARAMETER, JDLocale.L("interaction.externreconnect.parameter", "Parameter (1 Parameter/Zeile)")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, configuration, PROPERTY_IP_WAIT_FOR_RETURN, JDLocale.L("interaction.externreconnect.waitForTermination", "Warten x Sekunden bis Befehl beendet ist [sek]"), 0, 600).setDefaultValue(0));
    }

    @Override
    protected boolean runCommands(ProgressController progress) {
        int waitForReturn = configuration.getIntegerProperty(PROPERTY_IP_WAIT_FOR_RETURN, 0);
        String command = configuration.getStringProperty(PROPERTY_RECONNECT_COMMAND);

        File f = new File(command);
        String t = f.getAbsolutePath();
        String executeIn = t.substring(0, t.indexOf(f.getName()) - 1);

        String parameter = configuration.getStringProperty(PROPERTY_RECONNECT_PARAMETER);

        logger.finer("Execute Returns: " + JDUtilities.runCommand(command, Regex.getLines(parameter), executeIn, waitForReturn));

        return true;
    }

    @Override
    public String toString() {
        return JDLocale.L("interaction.externreconnect.toString", "Externes Reconnectprogramm aufrufen");
    }

}
