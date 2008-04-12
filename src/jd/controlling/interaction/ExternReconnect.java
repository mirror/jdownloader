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
import jd.controlling.ProgressController;
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
     * Unter diesen Namen werden die entsprechenden Parameter gespeichert
     * 
     */
    private static final long   serialVersionUID                  = 4793649294489149258L;

    /**
     * parameternamen. Hier findet man nur die Namen! unter denen die parameter
     * in die hashmap abgelegt werden
     */

    /**
     * Gibt den reconnectbefehl an
     */
    public static String        PROPERTY_RECONNECT_COMMAND        = "InteractionExternReconnect_" + "Command";

    public static final String  PROPERTY_IP_WAIT_FOR_RETURN       = "WAIT_FOR_RETURN";

    /**
     * serialVersionUID
     */
    private static final String NAME                              = JDLocale.L("interaction.externreconnect.name", "Extern Reconnect");

    // private transient String lastIP;

    /**
     * Maximale Reconnectanzahl
     */
    // private static final int MAX_RETRIES = 10;
    // private static final String PROPERTY_EXTERN_RECONNECT_DISABLED =
    // "EXTERN_RECONNECT_DISABLED";
    private static final String PROPERTY_RECONNECT_EXECUTE_FOLDER = "RECONNECT_EXECUTE_FOLDER";

    private static final String PROPERTY_RECONNECT_PARAMETER      = "EXTERN_RECONNECT__PARAMETER";

    private static final String PARAM_IPCHECKWAITTIME             = "EXTERN_RECONNECT_IPCHECKWAITTIME";

    private static final String PARAM_RETRIES                     = "EXTERN_RECONNECT_RETRIES";

    private static final String PARAM_WAITFORIPCHANGE             = "EXTERN_RECONNECT_WAITFORIPCHANGE";

    private int                 retries                           = 0;

    @Override
    public boolean doInteraction(Object arg) {

        retries++;
        ProgressController progress = new ProgressController(JDLocale.L("interaction.externreconnect.progress.0_title", "ExternReconnect"), 10);

        progress.setStatusText(JDLocale.L("interaction.externreconnect.progress.1_retries", "ExternReconnect #") + retries);
/*
        int waitForReturn = JDUtilities.getConfiguration().getIntegerProperty(PROPERTY_IP_WAIT_FOR_RETURN, 0);
        String executeIn = JDUtilities.getConfiguration().getStringProperty(PROPERTY_RECONNECT_EXECUTE_FOLDER);
        String command = JDUtilities.getConfiguration().getStringProperty(PROPERTY_RECONNECT_COMMAND);
        String parameter = JDUtilities.getConfiguration().getStringProperty(PROPERTY_RECONNECT_PARAMETER);
*/
        int waittime = JDUtilities.getConfiguration().getIntegerProperty(PARAM_IPCHECKWAITTIME, 0);
        int maxretries = JDUtilities.getConfiguration().getIntegerProperty(PARAM_RETRIES, 0);
        int waitForIp = JDUtilities.getConfiguration().getIntegerProperty(PARAM_WAITFORIPCHANGE, 10);

        logger.info("Starting " + NAME + " #" + retries);
        String preIp = JDUtilities.getIPAddress();

        progress.increase(1);
        progress.setStatusText(JDLocale.L("interaction.externreconnect.progress.2_oldIP", "ExternReconnect Old IP:") + preIp);
        logger.finer("IP befor: " + preIp);
        runCommands();
        logger.finer("Wait " + waittime + " seconds ...");
        try {
            Thread.sleep(waittime * 1000);
        }
        catch (InterruptedException e) {
        }

        String afterIP = JDUtilities.getIPAddress();
        logger.finer("Ip after: " + afterIP);
        String pattern = JDLocale.L("interaction.externreconnect.progress.3_ipcheck", "ExternReconnect New IP: %s / %s");
        progress.setStatusText(JDUtilities.sprintf(pattern, new String[] { afterIP, preIp }));
        long endTime = System.currentTimeMillis() + waitForIp * 1000;
        logger.info("Wait " + waitForIp + " sek for new ip");

        while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp) || afterIP.equals("offline"))) {
            try {
                Thread.sleep(5 * 1000);
            }
            catch (InterruptedException e) {
            }
            afterIP = JDUtilities.getIPAddress();
            pattern = JDLocale.L("interaction.externreconnect.progress.3_ipcheck", "ExternReconnect New IP: %s / %s");
            progress.setStatusText(JDUtilities.sprintf(pattern, new String[] { afterIP, preIp }));
            logger.finer("Ip Check: " + afterIP);
        }
        if (!afterIP.equals(preIp) && !afterIP.equals("offline")) {
            progress.finalize();
            return true;
        }

        if (retries <= maxretries) {
            progress.finalize();
            return doInteraction(arg);
        }
        progress.finalize();
        return false;
    }

    private void runCommands() {
        int waitForReturn = JDUtilities.getConfiguration().getIntegerProperty(PROPERTY_IP_WAIT_FOR_RETURN, 0);
        String executeIn = JDUtilities.getConfiguration().getStringProperty(PROPERTY_RECONNECT_EXECUTE_FOLDER);
        String command = JDUtilities.getConfiguration().getStringProperty(PROPERTY_RECONNECT_COMMAND);
        String parameter = JDUtilities.getConfiguration().getStringProperty(PROPERTY_RECONNECT_PARAMETER);
    

            logger.finer("Execute Returns: " + JDUtilities.runTestCommand(command, JDUtilities.splitByNewline(parameter), executeIn, waitForReturn, false));
    

    }

    @Override
    public String toString() {
        return JDLocale.L("interaction.externreconnect.toString", "Externes Reconnectprogramm aufrufen");
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
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, JDUtilities.getConfiguration(), PROPERTY_RECONNECT_COMMAND, JDLocale.L("interaction.externreconnect.command", "Befehl (absolute Pfade verwenden)")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, JDUtilities.getConfiguration(), PROPERTY_RECONNECT_PARAMETER, JDLocale.L("interaction.externreconnect.parameter", "Parameter (1 Parameter/Zeile)")).setExpertEntry(true));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), PROPERTY_RECONNECT_EXECUTE_FOLDER, JDLocale.L("interaction.externreconnect.executeIn", "Ausführen in (Ordner der Anwendung)")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), PARAM_IPCHECKWAITTIME, JDLocale.L("interaction.externreconnect.waitTimeToFirstIPCheck", "Wartezeit bis zum ersten IP-Check[sek]"), 0, 600).setDefaultValue(5).setExpertEntry(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), PARAM_RETRIES, JDLocale.L("interaction.externreconnect.retries", "Max. Wiederholungen (-1 = unendlich)"), -1, 20).setDefaultValue(5).setExpertEntry(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), PARAM_WAITFORIPCHANGE, JDLocale.L("interaction.externreconnect.waitForIp", "Auf neue IP warten [sek]"), 0, 600).setDefaultValue(20).setExpertEntry(true));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), PROPERTY_IP_WAIT_FOR_RETURN, JDLocale.L("interaction.externreconnect.waitForTermination", "Warten x Sekunden bis Befehl beendet ist[sek]"), 0, 600).setDefaultValue(0).setExpertEntry(true));
     
    }

    @Override
    public void resetInteraction() {
        retries = 0;
    }

}
