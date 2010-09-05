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

package jd.controlling.reconnect;

import java.util.logging.Logger;

import jd.config.ConfigContainer;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.gui.UserIF;
import jd.nrouter.IPCheck;
import jd.nutils.IPAddress;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public abstract class ReconnectMethod {

    private static final String   NA                    = "na";

    protected static final Logger LOG                   = JDLogger.getLogger();

    public static final String    PARAM_RECONNECT_TYPE  = "RECONNECT_TYPE";

    public static final int       LIVEHEADER            = 0;
    public static final int       EXTERN                = 1;
    public static final int       BATCH                 = 2;
    public static final int       CLR                   = 3;
    public static final int       PLUGIN                = 4;
    public static final String    PARAM_IPCHECKWAITTIME = "RECONNECT_IPCHECKWAITTIME2";

    public static final String    PARAM_RETRIES         = "RECONNECT_RETRIES2";

    public static final String    PARAM_WAITFORIPCHANGE = "RECONNECT_WAITFORIPCHANGE2";

    protected ConfigContainer     config                = null;

    protected ReconnectMethod() {
    }

    public final boolean doReconnect() {
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            /*
             * disabled ipcheck, let run 1 reconnect round and guess it has been
             * successful
             */
            this.doReconnectInternal(1);
            Reconnecter.setCurrentIP(ReconnectMethod.NA);
            return true;
        }
        int maxretries = JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RETRIES, 5);
        boolean ret = false;
        int retry = 0;
        if (maxretries < 0) {
            maxretries = Integer.MAX_VALUE;
        } else if (maxretries == 0) {
            maxretries = 1;
        }
        for (retry = 0; retry < maxretries; retry++) {
            if ((ret = this.doReconnectInternal(retry + 1)) == true) {
                break;
            }
        }
        return ret;
    }

    public final boolean doReconnectInternal(final int retry) {
        final ProgressController progress = new ProgressController(this.toString(), 10, "gui.images.reconnect");
        progress.setStatusText(JDL.L("reconnect.progress.1_retries", "Reconnect #") + retry);
        try {
            final Configuration configuration = JDUtilities.getConfiguration();
            final int waittime = configuration.getIntegerProperty(ReconnectMethod.PARAM_IPCHECKWAITTIME, 5);
            final int waitForIp = configuration.getIntegerProperty(ReconnectMethod.PARAM_WAITFORIPCHANGE, 30);

            ReconnectMethod.LOG.info("Starting " + this.toString() + " #" + retry);
            final String preIp = this.getIP();

            progress.increase(1);
            progress.setStatusText(JDL.L("reconnect.progress.2_oldIP", "Reconnect Old IP:") + preIp);
            if (!this.runCommands(progress)) {
                progress.doFinalize();
                ReconnectMethod.LOG.severe("An error occured while processing the reconnect ... Terminating");
                return false;
            }
            ReconnectMethod.LOG.finer("Initial Waittime: " + waittime + " seconds");
            try {
                Thread.sleep(waittime * 1000);
            } catch (final InterruptedException e) {
            }
            String afterIP = this.getIP();
            progress.setStatusText(JDL.LF("reconnect.progress.3_ipcheck", "Reconnect New IP: %s / %s", afterIP, preIp));
            long endTime = System.currentTimeMillis() + waitForIp * 1000;
            ReconnectMethod.LOG.info("Wait " + waitForIp + " sec for new ip");
            while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp) || afterIP.equals(ReconnectMethod.NA))) {
                ReconnectMethod.LOG.finer("IP before: " + preIp + " after: " + afterIP);
                try {
                    Thread.sleep(5 * 1000);
                } catch (final InterruptedException e) {
                }
                afterIP = this.getIP();
                progress.setStatusText(JDL.LF("reconnect.progress.3_ipcheck", "Reconnect New IP: %s / %s", afterIP, preIp));
            }
            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                /* stop here when ipcheck is disabled */
                Reconnecter.setCurrentIP(ReconnectMethod.NA);
                return true;
            }
            ReconnectMethod.LOG.finer("IP before: " + preIp + " after: " + afterIP);
            if (afterIP.equals(ReconnectMethod.NA) && !afterIP.equals(preIp)) {
                ReconnectMethod.LOG.warning("JD could disconnect your router, but could not connect afterwards. Try to rise the option 'Wait until first IP Check'");
                endTime = System.currentTimeMillis() + 120 * 1000;
                while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp) || afterIP.equals(ReconnectMethod.NA))) {
                    ReconnectMethod.LOG.finer("IP before: " + preIp + " after: " + afterIP);
                    try {
                        Thread.sleep(5 * 1000);
                    } catch (final InterruptedException e) {
                    }
                    afterIP = this.getIP();
                    progress.setStatusText(JDL.LF("reconnect.progress.3_ipcheck", "Reconnect New IP: %s / %s", preIp, afterIP));
                }
            }

            if (!afterIP.equals(preIp) && !afterIP.equals(ReconnectMethod.NA)) {
                ReconnectMethod.LOG.finer("IP before: " + preIp + " after: " + afterIP);
                /* Reconnect scheint erfolgreich gewesen zu sein */
                /* nun IP validieren */
                if (!IPAddress.validateIP(afterIP)) {
                    ReconnectMethod.LOG.warning("IP " + afterIP + " was filtered by mask: " + SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_MASK, "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"));
                    UserIF.getInstance().displayMiniWarning(JDL.L("reconnect.ipfiltered.warning.title", "Wrong IP!"), JDL.LF("reconnect.ipfiltered.warning.short", "The IP %s is not allowed!", afterIP));
                    Reconnecter.setCurrentIP(ReconnectMethod.NA);
                    return false;
                } else {
                    progress.doFinalize();
                    Reconnecter.setCurrentIP(afterIP);
                    return true;
                }
            }
            return false;
        } finally {
            progress.doFinalize();
        }
    }

    public final ConfigContainer getConfig() {
        if (this.config == null) {
            this.config = new ConfigContainer();
            this.initConfig();
        }
        return this.config;
    }

    private String getIP() {
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) { return ReconnectMethod.NA; }
        return IPCheck.getIPAddress();
    }

    protected abstract void initConfig();

    protected abstract boolean runCommands(ProgressController progress);

}
