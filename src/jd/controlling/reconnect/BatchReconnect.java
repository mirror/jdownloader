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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.OSDetector;
import jd.parser.Regex;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class BatchReconnect extends ReconnectMethod {

    private SubConfiguration configuration;

    private static final String PROPERTY_DO_OUTPUT = "PROPERTY_DO_OUTPUT";

    private static final String PROPERTY_IP_WAIT_FOR_RETURN = "WAIT_FOR_RETURN";

    private static final String PROPERTY_RECONNECT_EXECUTE_FOLDER = "RECONNECT_EXECUTE_FOLDER";

    private static final String PROPERTY_TERMINAL = "TERMINAL";

    private static final String PROPERTY_BATCHTEXT = "BATCH_TEXT";

    public BatchReconnect() {
        configuration = JDUtilities.getSubConfig("BATCHRECONNECT");
    }

    @Override
    public void initConfig() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, PROPERTY_TERMINAL, JDLocale.L("interaction.batchreconnect.terminal", "Interpreter")));
        if (OSDetector.isWindows()) {
            cfg.setDefaultValue("cmd /c");
        } else {
            cfg.setDefaultValue("/bin/bash");
        }
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, PROPERTY_BATCHTEXT, JDLocale.L("interaction.batchreconnect.batch", "Batch Script")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, configuration, PROPERTY_RECONNECT_EXECUTE_FOLDER, JDLocale.L("interaction.batchreconnect.executeIn", "Ausführen in (Ordner der Anwendung)")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, configuration, PROPERTY_IP_WAIT_FOR_RETURN, JDLocale.L("interaction.batchreconnect.waitForTermination", "Warten x Sekunden bis Befehl beendet ist [sek]"), 0, 600).setDefaultValue(0));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, PROPERTY_DO_OUTPUT, JDLocale.L("interaction.batchreconnect.doOutput", "Rückgaben im Log anzeigen")).setDefaultValue(false));
    }

    @Override
    protected boolean runCommands(ProgressController progress) {
        int waitForReturn = configuration.getIntegerProperty(PROPERTY_IP_WAIT_FOR_RETURN, 0);
        String executeIn = configuration.getStringProperty(PROPERTY_RECONNECT_EXECUTE_FOLDER);
        String command = configuration.getStringProperty(PROPERTY_TERMINAL);

        String[] cmds = command.split("\\ ");
        command = cmds[0];
        for (int i = 0; i < cmds.length - 1; i++) {
            cmds[i] = cmds[i + 1];
        }

        String batch = configuration.getStringProperty(PROPERTY_BATCHTEXT, "");

        String[] lines = Regex.getLines(batch);
        logger.info("Batch Verarbeitung aktiviert. Als Befehl muss der Interpreter eingetragen sein (windows: cmd.exe linux z.b. bash mac: teminal ?) Aktueller interpreter: " + command);
        for (String element : lines) {
            cmds[cmds.length - 1] = element;
            logger.finer("Execute Batchline: " + JDUtilities.runCommand(command, cmds, executeIn, waitForReturn));
        }

        return true;
    }

    @Override
    public String toString() {
        return JDLocale.L("interaction.batchreconnect.toString", "Batch reconnect durchführen");
    }

}
