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

import java.io.File;
import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
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
public class ExternReconnect extends Interaction implements Serializable {
    /**
     * serialVersionUID
     */
    private static final String NAME = JDLocale.L("interaction.externreconnect.name", "Extern Reconnect");

    /**
     * parameternamen. Hier findet man nur die Namen! unter denen die parameter
     * in die hashmap abgelegt werden
     */

    private static final String PARAM_IPCHECKWAITTIME = "EXTERN_RECONNECT_IPCHECKWAITTIME";

    public static final String PARAM_RETRIES = "EXTERN_RECONNECT_RETRIES";

    private static final String PARAM_WAITFORIPCHANGE = "EXTERN_RECONNECT_WAITFORIPCHANGE";

    // private transient String lastIP;

    public static final String PROPERTY_IP_WAIT_FOR_RETURN = "WAIT_FOR_RETURN";

    /**
     * Gibt den reconnectbefehl an
     */
    public static String PROPERTY_RECONNECT_COMMAND = "InteractionExternReconnect_" + "Command";

    /**
     * Maximale Reconnectanzahl
     */
    // private static final int MAX_RETRIES = 10;
    // private static final String PROPERTY_EXTERN_RECONNECT_DISABLED =
    // "EXTERN_RECONNECT_DISABLED";
    private static final String PROPERTY_RECONNECT_PARAMETER = "EXTERN_RECONNECT__PARAMETER";

    /**
     * Unter diesen Namen werden die entsprechenden Parameter gespeichert
     * 
     */
    private static final long serialVersionUID = 4793649294489149258L;

    private int retries = 0;

    @Override
    public boolean doInteraction(Object arg) {

        retries++;
        ProgressController progress = new ProgressController(JDLocale.L("interaction.externreconnect.progress.0_title", "ExternReconnect"), 10);

        progress.setStatusText(JDLocale.L("interaction.externreconnect.progress.1_retries", "ExternReconnect #") + retries);

        int waittime = JDUtilities.getConfiguration().getIntegerProperty(PARAM_IPCHECKWAITTIME, 0);
        int maxretries = JDUtilities.getConfiguration().getIntegerProperty(PARAM_RETRIES, 0);
        int waitForIp = JDUtilities.getConfiguration().getIntegerProperty(PARAM_WAITFORIPCHANGE, 10);

        logger.info("Starting " + NAME + " #" + retries);
        String preIp = JDUtilities.getIPAddress();

        progress.increase(1);
        progress.setStatusText(JDLocale.L("interaction.externreconnect.progress.2_oldIP", "ExternReconnect Old IP:") + preIp);
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
        progress.setStatusText(JDLocale.LF("interaction.externreconnect.progress.3_ipcheck", "ExternReconnect New IP: %s / %s", afterIP, preIp));
        long endTime = System.currentTimeMillis() + waitForIp * 1000;
        logger.info("Wait " + waitForIp + " sek for new ip");

        while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp) || afterIP.equals("offline"))) {
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
            }
            afterIP = JDUtilities.getIPAddress();
            progress.setStatusText(JDLocale.LF("interaction.externreconnect.progress.3_ipcheck", "ExternReconnect New IP: %s / %s", afterIP, preIp));
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
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, JDUtilities.getConfiguration(), PROPERTY_RECONNECT_COMMAND, JDLocale.L("interaction.externreconnect.command", "Befehl (absolute Pfade verwenden)")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, JDUtilities.getConfiguration(), PROPERTY_RECONNECT_PARAMETER, JDLocale.L("interaction.externreconnect.parameter", "Parameter (1 Parameter/Zeile)")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), PARAM_IPCHECKWAITTIME, JDLocale.L("interaction.externreconnect.waitTimeToFirstIPCheck", "Wartezeit bis zum ersten IP-Check [sek]"), 0, 600).setDefaultValue(5));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), PARAM_RETRIES, JDLocale.L("interaction.externreconnect.retries", "Max. Wiederholungen (-1 = unendlich)"), -1, 20).setDefaultValue(5));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), PARAM_WAITFORIPCHANGE, JDLocale.L("interaction.externreconnect.waitForIp", "Auf neue IP warten [sek]"), 0, 600).setDefaultValue(20));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), PROPERTY_IP_WAIT_FOR_RETURN, JDLocale.L("interaction.externreconnect.waitForTermination", "Warten x Sekunden bis Befehl beendet ist [sek]"), 0, 600).setDefaultValue(0));

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
        int waitForReturn = JDUtilities.getConfiguration().getIntegerProperty(PROPERTY_IP_WAIT_FOR_RETURN, 0);
        String command = JDUtilities.getConfiguration().getStringProperty(PROPERTY_RECONNECT_COMMAND);

        File f = new File(command);
        String t = f.getAbsolutePath();
        String executeIn = t.substring(0, t.indexOf(f.getName()) - 1);

        String parameter = JDUtilities.getConfiguration().getStringProperty(PROPERTY_RECONNECT_PARAMETER);

        logger.finer("Execute Returns: " + JDUtilities.runCommand(command, Regex.getLines(parameter), executeIn, waitForReturn));

    }

    @Override
    public String toString() {
        return JDLocale.L("interaction.externreconnect.toString", "Externes Reconnectprogramm aufrufen");
    }

}
