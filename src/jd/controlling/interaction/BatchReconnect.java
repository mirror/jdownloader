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

import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class BatchReconnect extends Interaction implements Serializable {

    private static final String NAME = JDLocale.L("interaction.batchreconnect.name", "Batch Reconnect");

    private static final String PARAM_IPCHECKWAITTIME = "EXTERN_RECONNECT_IPCHECKWAITTIME";

    public static final String PARAM_RETRIES = "EXTERN_RECONNECT_RETRIES";

    private static final String PARAM_WAITFORIPCHANGE = "EXTERN_RECONNECT_WAITFORIPCHANGE";

    private static final String PROPERTY_DO_OUTPUT = "PROPERTY_DO_OUTPUT";

    public static final String PROPERTY_IP_WAIT_FOR_RETURN = "WAIT_FOR_RETURN";

    private static final String PROPERTY_RECONNECT_EXECUTE_FOLDER = "RECONNECT_EXECUTE_FOLDER";

    public static String PROPERTY_TERMINAL = "TERMINAL";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private int retries = 0;

    @Override
    public boolean doInteraction(Object arg) {

        retries++;
        ProgressController progress = new ProgressController(JDLocale.L("interaction.batchreconnect.progress.0_title", "Batch Reconnect"), 10);

        progress.setStatusText(JDLocale.L("interaction.batchreconnect.progress.1_retries", "BatchReconnect #") + retries);

        SubConfiguration conf = JDUtilities.getSubConfig("BATCHRECONNECT");
        int waittime = conf.getIntegerProperty(PARAM_IPCHECKWAITTIME, 0);
        int maxretries = conf.getIntegerProperty(PARAM_RETRIES, 0);
        int waitForIp = conf.getIntegerProperty(PARAM_WAITFORIPCHANGE, 10);

        logger.info("Starting " + NAME + " #" + retries);
        String preIp = JDUtilities.getIPAddress();

        progress.increase(1);
        progress.setStatusText(JDLocale.L("interaction.batchreconnect.progress.2_oldIP", "BatchReconnect Old IP:") + preIp);
        logger.finer("IP before: " + preIp);
        runCommands();
        logger.finer("Wait " + waittime + " seconds ...");
        try {
            Thread.sleep(waittime * 1000);
        } catch (InterruptedException e) {
        }

        String afterIP = JDUtilities.getIPAddress();

        if (!JDUtilities.validateIP(afterIP)) {
            logger.warning("IP " + afterIP + " was filtered by mask: " + JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_GLOBAL_IP_MASK, "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"));
            JDUtilities.getGUI().displayMiniWarning(String.format(JDLocale.L("interaction.reconnect.ipfiltered.warning.short", "Die IP %s wurde als nicht erlaubt identifiziert"), afterIP), null, 20);
            afterIP = "offline";
        }
        logger.finer("Ip after: " + afterIP);
        progress.setStatusText(JDLocale.LF("interaction.batchreconnect.progress.3_ipcheck", "BatchReconnect New IP: %s / %s", afterIP, preIp));
        long endTime = System.currentTimeMillis() + waitForIp * 1000;
        logger.info("Wait " + waitForIp + " sek for new ip");

        while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp) || afterIP.equals("offline"))) {
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
            }
            afterIP = JDUtilities.getIPAddress();
            progress.setStatusText(JDLocale.LF("interaction.batchreconnect.progress.3_ipcheck", "BatchReconnect New IP: %s / %s", afterIP, preIp));
            logger.finer("Ip Check: " + afterIP);
        }
        if (!afterIP.equals(preIp) && !afterIP.equals("offline")) {
            progress.finalize();
            return true;
        }

        if (maxretries == -1 || retries <= maxretries) {
            progress.finalize();
            return doInteraction(arg);
        }
        progress.finalize();
        return false;
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    @Override
    public void initConfig() {
        SubConfiguration conf = JDUtilities.getSubConfig("BATCHRECONNECT");
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, conf, PROPERTY_TERMINAL, JDLocale.L("interaction.batchreconnect.terminal", "Interpreter")));
        if (System.getProperty("os.name").indexOf("Linux") >= 0) {
            cfg.setDefaultValue("/bin/bash");
        } else if (System.getProperty("os.name").indexOf("Windows") >= 0) {
            cfg.setDefaultValue("cmd /c");
        } else {
            cfg.setDefaultValue("/bin/bash");
        }
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, conf, "BATCH_TEXT", JDLocale.L("interaction.batchreconnect.batch", "Batch Script")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, conf, PROPERTY_RECONNECT_EXECUTE_FOLDER, JDLocale.L("interaction.batchreconnect.executeIn", "Ausführen in (Ordner der Anwendung)")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, conf, PARAM_IPCHECKWAITTIME, JDLocale.L("interaction.batchreconnect.waitTimeToFirstIPCheck", "Wartezeit bis zum ersten IP-Check [sek]"), 0, 600).setDefaultValue(5));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, conf, PARAM_RETRIES, JDLocale.L("interaction.batchreconnect.retries", "Max. Wiederholungen (-1 = unendlich)"), -1, 20).setDefaultValue(5));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, conf, PARAM_WAITFORIPCHANGE, JDLocale.L("interaction.batchreconnect.waitForIp", "Auf neue IP warten [sek]"), 0, 600).setDefaultValue(20));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, conf, PROPERTY_IP_WAIT_FOR_RETURN, JDLocale.L("interaction.batchreconnect.waitForTermination", "Warten x Sekunden bis Befehl beendet ist [sek]"), 0, 600).setDefaultValue(0));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, conf, PROPERTY_DO_OUTPUT, JDLocale.L("interaction.batchreconnect.doOutput", "Rückgaben im Log anzeigen")).setDefaultValue(false));

    }

    @Override
    public void resetInteraction() {
        retries = 0;
    }

    @Override
    public void run() {
        // Nichts zu tun. Interaction braucht keinen Thread
    }

    private void runCommands() {
        SubConfiguration conf = JDUtilities.getSubConfig("BATCHRECONNECT");
        int waitForReturn = conf.getIntegerProperty(PROPERTY_IP_WAIT_FOR_RETURN, 0);
        String executeIn = conf.getStringProperty(PROPERTY_RECONNECT_EXECUTE_FOLDER);
        String command = conf.getStringProperty(PROPERTY_TERMINAL);

        String[] cmds = command.split("\\ ");
        command = cmds[0];
        for (int i = 0; i < cmds.length; i++) {
            if (i < cmds.length - 1) {
                cmds[i] = cmds[i + 1];
            }

        }

        String batch = conf.getStringProperty("BATCH_TEXT", "");

        String[] lines = Regex.getLines(batch);
        logger.info("Batch Verarbeitung aktiviert. Als Befehl muss der Interpreter eingetragen sein (windows: cmd.exe linux z.b. bash mac: teminal ?) Aktueller interpreter: " + command);
        for (String element : lines) {
            cmds[cmds.length - 1] = element;
            logger.finer("Execute Batchline: " + JDUtilities.runCommand(command, cmds, executeIn, waitForReturn));

        }

    }

    @Override
    public String toString() {
        return JDLocale.L("interaction.batchreconnect.toString", "Batch reconnect durchführen");
    }

}
