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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.nutils.OSDetector;
import jd.parser.Regex;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * Diese Klasse ruft ein Externes Programm auf. Anschließend wird auf eine Neue
 * IP geprüft
 * 
 * @author JD-Team
 */
public class ExternReconnect extends ReconnectMethod {

    private Configuration configuration;

    private static final String PROPERTY_RECONNECT_COMMAND = "InteractionExternReconnect_Command";

    private static final String PROPERTY_RECONNECT_PARAMETER = "EXTERN_RECONNECT__PARAMETER";

    private static final String PROPERTY_IP_WAIT_FOR_RETURN = "WAIT_FOR_RETURN5";

    public ExternReconnect() {
        configuration = JDUtilities.getConfiguration();
    }

    protected void initConfig() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, configuration, PROPERTY_RECONNECT_COMMAND, JDL.L("interaction.externreconnect.command", "Befehl (absolute Pfade verwenden)")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, PROPERTY_RECONNECT_PARAMETER, JDL.L("interaction.externreconnect.parameter", "Parameter (1 Parameter/Zeile)")));
    }

    protected boolean runCommands(ProgressController progress) {
        int waitForReturn = configuration.getIntegerProperty(PROPERTY_IP_WAIT_FOR_RETURN, 0);
        String command = configuration.getStringProperty(PROPERTY_RECONNECT_COMMAND, "").trim();

        File f = new File(command);
        if (!f.exists()) return false;
        String t = f.getAbsolutePath();
        String executeIn = t.substring(0, t.indexOf(f.getName()) - 1).trim();
        if (OSDetector.isWindows() || true) {
            /*
             * for windows we create a temporary batchfile that calls our
             * external tool and redirect its streams to nul
             */
            File bat = getDummyBat();
            if (bat == null) return false;
            try {
                BufferedWriter output = new BufferedWriter(new FileWriter(bat));
                if (executeIn.contains(" ")) {
                    output.write("cd \"" + executeIn + "\"\r\n");
                } else {
                    output.write("cd " + executeIn + "\r\n");
                }
                String parameter = configuration.getStringProperty(PROPERTY_RECONNECT_PARAMETER);
                String[] params = Regex.getLines(parameter);
                StringBuilder sb = new StringBuilder();
                for (String param : params) {
                    sb.append(param);
                    sb.append(" ");
                }
                if (executeIn.contains(" ")) {
                    output.write("\"" + command + "\"" + " " + sb.toString() + ">nul 2>nul");
                } else {
                    output.write(command + " " + sb.toString() + ">nul 2>nul");
                }
                output.close();
            } catch (Exception e) {
                JDLogger.exception(e);
                return false;
            }
            logger.finer("Execute Returns: " + JDUtilities.runCommand(bat.toString(), Regex.getLines(""), executeIn, waitForReturn));
        } else {
            /* other os, normal handling */
            String parameter = configuration.getStringProperty(PROPERTY_RECONNECT_PARAMETER);
            logger.finer("Execute Returns: " + JDUtilities.runCommand(command, Regex.getLines(parameter), executeIn, waitForReturn));
        }
        return true;
    }

    /**
     * get next available DummyBat for reconnect
     * 
     * @return
     */
    private File getDummyBat() {
        int number = 0;
        while (true) {
            if (number == 100) {
                logger.severe("Cannot create dummy Bat file, please delete all recon_*.bat files in tmp folder!");
                return null;
            }
            File tmp = JDUtilities.getResourceFile("tmp/recon_" + number + ".bat", true);
            if (tmp.exists()) {
                if (tmp.delete()) return tmp;
                tmp.deleteOnExit();
            } else {
                return tmp;
            }
            number++;
        }
    }

}
