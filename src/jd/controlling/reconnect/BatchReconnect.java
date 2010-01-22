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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class BatchReconnect extends ReconnectMethod {

    private final SubConfiguration configuration;

    private static final String PROPERTY_IP_WAIT_FOR_RETURN = "WAIT_FOR_RETURN5";

    private static final String PROPERTY_RECONNECT_EXECUTE_FOLDER = "RECONNECT_EXECUTE_FOLDER";

    private static final String PROPERTY_TERMINAL = "TERMINAL";

    private static final String PROPERTY_BATCHTEXT = "BATCH_TEXT";

    public BatchReconnect() {
        configuration = SubConfiguration.getConfig("BATCHRECONNECT");
    }

    protected void initConfig() {
        final ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, PROPERTY_TERMINAL, JDL.L("interaction.batchreconnect.terminal", "Interpreter")));
        if (OSDetector.isWindows()) {
            cfg.setDefaultValue("cmd /c");
        } else {
            cfg.setDefaultValue("/bin/bash");
        }
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, PROPERTY_BATCHTEXT, JDL.L("interaction.batchreconnect.batch", "Batch Script")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, configuration, PROPERTY_RECONNECT_EXECUTE_FOLDER, JDL.L("interaction.batchreconnect.executeIn", "Ausführen in (Ordner der Anwendung)")));
    }

    protected boolean runCommands(final ProgressController progress) {
        final int waitForReturn = configuration.getIntegerProperty(PROPERTY_IP_WAIT_FOR_RETURN, -1);
        final String executeIn = configuration.getStringProperty(PROPERTY_RECONNECT_EXECUTE_FOLDER);
        String command = configuration.getStringProperty(PROPERTY_TERMINAL);

        final String[] cmds = command.split("\\ ");
        final int cmdsLength1 = cmds.length - 1;
        command = cmds[0];
        for (int i = 0; i < cmdsLength1; i++) {
            cmds[i] = cmds[i + 1];
        }

        final String batch = configuration.getStringProperty(PROPERTY_BATCHTEXT, "");

        final String[] lines = Regex.getLines(batch);
        logger.info("Using Batch-Mode: using " + command + " as interpreter! (default: windows(cmd.exe) linux&mac(/bin/bash) )");
        for (final String element : lines) {
            cmds[cmdsLength1] = element;
            /*
             * if we have multiple lines, wait for each line to finish until
             * starting the next one
             */
            logger.finer("Execute Batchline: " + JDUtilities.runCommand(command, cmds, executeIn, lines.length >= 2 ? waitForReturn : -1));
        }

        return true;
    }

}
