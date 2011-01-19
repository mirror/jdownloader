//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.optional.antireconnect;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.nutils.OSDetector;
import jd.utils.JDUtilities;

import org.appwork.utils.AwReg;

public class JDAntiReconnectThread extends Thread implements Runnable {

    private boolean               running       = true;
    private boolean               run           = false;
    private boolean               clients       = false;
    private ArrayList<String>     iplist        = new ArrayList<String>();
    private String                lastIpsString = "";
    private String                lastIp        = "";
    private final Logger          logger;
    private final JDAntiReconnect jdAntiReconnect;

    public JDAntiReconnectThread(JDAntiReconnect jdAntiReconnect) {
        super();
        this.logger = JDLogger.getLogger();
        this.jdAntiReconnect = jdAntiReconnect;
    }

    public void parselist(String ipString) {
        if (ipString == null) return;
        if (ipString != lastIpsString) {
            lastIpsString = ipString;
            iplist.clear();

            String[] iparray = ipString.replaceAll(" ", "").split("\\s");
            for (int i = 0; i < iparray.length; i++) {
                if (iparray[i].indexOf("-") == -1) {
                    if (validateIP(iparray[i])) {
                        iplist.add(iparray[i]);
                    }
                } else {
                    final Pattern IP_PATTERN = Pattern.compile("(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)-(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
                    if (IP_PATTERN.matcher(iparray[i]).matches()) {
                        AwReg paramid = new AwReg(iparray[i], "(\\d+\\.\\d+\\.\\d+)\\.(\\d+)-(\\d+)");
                        for (int u = Integer.valueOf(paramid.getMatch(1)); u < Integer.valueOf(paramid.getMatch(2)); u++) {
                            iplist.add(paramid.getMatch(0) + "." + u);
                        }
                    }
                }
            }
        }
    }

    public void run() {
        while (running) {
            try {

                switch (jdAntiReconnect.getPluginConfig().getIntegerProperty("CONFIG_MODE", 0)) {
                case 0:
                    if (run) {
                        run = false;
                        this.setClients(false);
                    }
                    break;
                case 1:

                    if (!run) {
                        run = true;
                        logger.fine("JDAntiReconnect: Start");
                    }
                    parselist(jdAntiReconnect.getPluginConfig().getStringProperty("CONFIG_IPS"));
                    this.clients = false;
                    PingClients();
                    if (!this.clients) this.setClients(false);
                    break;

                case 2:
                    if (!run) {
                        run = true;
                        logger.fine("JDAntiReconnect: Start");
                    }
                    parselist(jdAntiReconnect.getPluginConfig().getStringProperty("CONFIG_IPS"));
                    this.clients = false;
                    ARPClients();
                    if (!this.clients) this.setClients(false);
                    break;
                default:
                    logger.finest("JDAntiReconnect: Config error");
                    jdAntiReconnect.getPluginConfig().setProperty("CONFIG_MODE", 0);
                }

                Thread.sleep(jdAntiReconnect.getPluginConfig().getIntegerProperty("CONFIG_EACH", 10000));
            } catch (InterruptedException e) {

            }
        }
        logger.fine("JDAntiReconnect: Terminated");
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean PingClients() {
        if (lastIp != "") {
            try {
                if (InetAddress.getByName(lastIp).isReachable(jdAntiReconnect.getPluginConfig().getIntegerProperty("CONFIG_TIMEOUT"))) {
                    this.setClients(true);
                    return true;
                } else {
                    lastIp = "";
                }
            } catch (Exception e) {
                logger.fine("JDAntiReconnect: IO-Exception");
            }
        }
        for (String i : iplist) {
            try {
                if (InetAddress.getByName(i).isReachable(jdAntiReconnect.getPluginConfig().getIntegerProperty("CONFIG_TIMEOUT"))) {
                    this.setClients(true);
                    lastIp = i;
                    logger.fine("JDAntiReconnect: Online " + i);
                    return true;
                }
            } catch (Exception e) {
                logger.fine("JDAntiReconnect: IO-Exception");
            }

        }
        return false;
    }

    public boolean ARPClients() {
        if (lastIp != "") {
            try {
                if (callArpTool(lastIp)) {
                    this.setClients(true);
                    return true;
                } else {
                    lastIp = "";
                }
            } catch (Exception e) {
                logger.fine("JDAntiReconnect: IO-Exception");
            }
        }
        for (String i : iplist) {
            try {
                if (callArpTool(i)) {
                    this.setClients(true);
                    lastIp = i;
                    logger.fine("JDAntiReconnect: Client Online " + i);
                    return true;
                }
            } catch (Exception e) {
                logger.fine("JDAntiReconnect: IO-Exception");
            }

        }
        return false;
    }

    public void setClients(boolean clients2) {
        if (this.clients != clients2) {
            this.clients = clients2;
            if (clients2 == true) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, jdAntiReconnect.getPluginConfig().getBooleanProperty("CONFIG_NEWRECONNECT"));
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, jdAntiReconnect.getPluginConfig().getIntegerProperty("CONFIG_NEWDOWNLOADS"));
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, jdAntiReconnect.getPluginConfig().getIntegerProperty("CONFIG_NEWSPEED"));
            } else {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, jdAntiReconnect.getPluginConfig().getBooleanProperty("CONFIG_OLDRECONNECT"));
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, jdAntiReconnect.getPluginConfig().getIntegerProperty("CONFIG_OLDDOWNLOADS"));
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, jdAntiReconnect.getPluginConfig().getIntegerProperty("CONFIG_OLDSPEED"));
            }
        }
    }

    private static boolean callArpTool(final String ipAddress) throws IOException, InterruptedException {

        if (OSDetector.isWindows()) { return callArpToolWindows(ipAddress); }

        if (callArpToolDefault(ipAddress) == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Validates the givvei ip. a) checks if it is a valid IP adress (regex)
     * 
     * @param iPaddress
     * @return
     */
    public static boolean validateIP(final String iPaddress) {
        final Pattern IP_PATTERN = Pattern.compile("(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
        if (IP_PATTERN.matcher(iPaddress).matches()) { return true; }
        return false;
    }

    private static String callArpToolDefault(final String ipAddress) throws IOException, InterruptedException {
        String out = null;
        final InetAddress hostAddress = InetAddress.getByName(ipAddress);
        ProcessBuilder pb = null;
        try {
            pb = new ProcessBuilder(new String[] { "ping", ipAddress });
            pb.start();
            /*-n for dont resolv ip, MUCH MUCH faster*/
            out = JDUtilities.runCommand("arp", new String[] { "-n", ipAddress }, null, 10);
            pb.directory();
            if (!out.matches("(?is).*((" + hostAddress.getHostName() + "|" + hostAddress.getHostAddress() + ").*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?|.*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?.*(" + hostAddress.getHostName() + "|" + hostAddress.getHostAddress() + ")).*")) {
                out = null;
            }
        } catch (final Exception e) {
            if (pb != null) {
                pb.directory();
            }
        }
        if (out == null || out.trim().length() == 0) {
            try {
                pb = new ProcessBuilder(new String[] { "ping", ipAddress });
                pb.start();
                out = JDUtilities.runCommand("ip", new String[] { "neigh", "show" }, null, 10);
                pb.directory();
                if (out != null) {
                    if (!out.matches("(?is).*((" + hostAddress.getHostName() + "|" + hostAddress.getHostAddress() + ").*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?|.*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?.*(" + hostAddress.getHostName() + "|" + hostAddress.getHostAddress() + ")).*")) {
                        out = null;
                    } else {
                        out = new AwReg(out, "(" + hostAddress.getHostName() + "|" + hostAddress.getHostAddress() + ")[^\r\n]*").getMatch(-1);
                    }
                }
            } catch (final Exception e) {
                if (pb != null) {
                    pb.directory();
                }
            }
        }
        return out;
    }

    private static boolean callArpToolWindows(final String ipAddress) throws IOException, InterruptedException {
        final ProcessBuilder pb = new ProcessBuilder(new String[] { "ping", ipAddress });
        pb.start();

        final String[] parts = JDUtilities.runCommand("arp", new String[] { "-a" }, null, 10).split(System.getProperty("line.separator"));
        pb.directory();
        for (final String part : parts) {
            if (part.indexOf(ipAddress + " ") > -1) { return true; }
        }
        return false;
    }
}
