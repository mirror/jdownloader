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

package jd.nrouter;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectMethod;
import jd.controlling.reconnect.plugins.ReconnectPluginController;
import jd.http.Browser;
import jd.utils.JDUtilities;

import org.appwork.utils.logging.Log;

public class IPCheck {

    /** IPCheckProvider use this to Signal Error or Seq.failing */
    public static enum CheckStatus {
        FAILED, SEQFAILED
    }

    public static ArrayList<IPCheckProvider> IP_CHECK_SERVICES = new ArrayList<IPCheckProvider>();
    private static int IP_CHECK_INDEX = 0;
    private static final Object LOCK = new Object();

    /** this CustomWebIPCheck uses the UserDefined Settings */
    private static final IPCheckProvider CUSTOM_WEB_IPCHECK = new IPCheckProvider() {

        private final int maxerror = 2;
        private int errorcount = 0;
        private final Browser br = new Browser();

        public String getInfo() {
            return "Customized IPCheck: " + SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "Please enter Website for IPCheck here");
        }

        public Object getIP() {
            if (this.errorcount > this.maxerror) {
                /*
                 * maxerror reached , pause this method
                 */
                this.errorcount = 0;
                return IPCheck.CheckStatus.SEQFAILED;
            }
            final String site = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "Please enter Website for IPCheck here");
            final String patt = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_PATTERN, "Please enter Regex for IPCheck here");
            try {
                new URL(site); /*
                                * check for valid website
                                */
                /*
                 * call website and check for ip
                 */
                this.br.setConnectTimeout(15000);
                this.br.setReadTimeout(15000);
                final Matcher matcher = Pattern.compile(patt, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(this.br.getPage(site));
                if (matcher.find()) {
                    if (matcher.groupCount() > 0) {
                        this.errorcount = 0;/*
                                             * reset error count
                                             */
                        return matcher.group(1);
                    }
                }
            } catch (final Exception e) {
                JDLogger.exception(e);
            }
            /*
             * error occured , return failed
             */
            this.errorcount++;
            return IPCheck.CheckStatus.FAILED;
        }
    };

    static {
        /* IPCheck Service powered by JDownloader */
        IPCheck.IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck3.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        IPCheck.IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck2.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        IPCheck.IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck1.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        IPCheck.IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck0.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        Collections.shuffle(IPCheck.IP_CHECK_SERVICES);
    }

    public static String LATEST_IP = null;

    /**
     * Uses IP_CHECK_SERVICES (current JDownloader IPCheck) to get the current
     * IP. rotates through IP_CHECK_SERVICES which is random sorted.
     * 
     * @Deprecated Returning Object is Bad. Better: return IP, or throw
     *             exceptions
     * 
     * @return current IP string object or IPCheck.CheckStatus.FAILED if there
     *         has been an error or IPCheck.CheckStatus.SEQFAILED if this method
     *         should be paused
     */
    @Deprecated
    private static Object checkIPProvider() {
        if (IPCheck.IP_CHECK_SERVICES.size() == 0) { return null; }
        synchronized (IPCheck.LOCK) {
            IPCheck.IP_CHECK_INDEX = IPCheck.IP_CHECK_INDEX % IPCheck.IP_CHECK_SERVICES.size();
            final IPCheckProvider ipcheck = IPCheck.IP_CHECK_SERVICES.get(IPCheck.IP_CHECK_INDEX);
            IPCheck.IP_CHECK_INDEX++;
            return ipcheck.getIP();
        }
    }

    /**
     * Fetches the current IP Address by asking
     * 
     * a.) one of the above CHECK_SERVICES (random)
     * 
     * b.) the userdefined IP check location
     * 
     * @return ip or "na" for notavailable
     */

    @SuppressWarnings("deprecation")
    public static String getIPAddress() {

        Object ip = null;
        synchronized (IPCheck.LOCK) {
            // check if user uses a reconnect plugin

            if (JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER) == ReconnectMethod.PLUGIN && ReconnectPluginController.getInstance().getActivePlugin().canCheckIP()) {

                try {
                    ip = ReconnectPluginController.getInstance().getActivePlugin().getExternalIP();
                } catch (final Throwable e) {
                    Log.exception(e);
                }

            } else if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_BALANCE, true)) {

                /* use registered ipcheckprovider (balanced) */
                for (int i = 0; i < IPCheck.IP_CHECK_SERVICES.size() / 2 + 1; i++) {
                    ip = IPCheck.checkIPProvider();

                    if (ip == null || ip == CheckStatus.FAILED) {
                        /* normal error, wait 3 secs for retry */
                        try {
                            Thread.sleep(3000);
                        } catch (final InterruptedException e) {
                        }
                    } else if (ip == CheckStatus.SEQFAILED) {
                        /* seq error, wait 9 secs for retry */
                        try {
                            Thread.sleep(9000);
                        } catch (final InterruptedException e) {
                        }
                    } else if (ip instanceof String) {
                        break;
                    }

                }
            } else {
                /* use userdefined ipcheck, try it only once */
                ip = IPCheck.CUSTOM_WEB_IPCHECK.getIP();
            }
        }
        // URGS! see @Deprecated comment
        if (ip == null || ip instanceof CheckStatus || !(ip instanceof String)) {
            JDLogger.getLogger().severe("IPCheck failed");
            return "na";
        }
        return ip.toString().trim();
    }

}

/** IPCheck Provider using a Website that provides Client IP */
class WebIPCheck implements IPCheckProvider {

    private static int maxerror = 5;
    private final String url;
    private Pattern pattern;
    private int errorcount = 0;
    private final Browser br;

    public WebIPCheck(final String url, final String regex) {
        this.url = url;
        this.errorcount = 0;
        if (regex != null) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }
        this.br = new Browser();
        this.br.setConnectTimeout(15000);
        this.br.setReadTimeout(15000);
    }

    public String getInfo() {
        return this.url;
    }

    public Object getIP() {
        if (this.errorcount > WebIPCheck.maxerror) {
            /* maxerror reached, pause this method */
            this.errorcount = 0;
            return IPCheck.CheckStatus.SEQFAILED;
        }
        try {
            /* call website and check for ip */
            final Matcher matcher = this.pattern.matcher(this.br.getPage(this.url));
            if (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    this.errorcount = 0;/* reset error count */
                    return matcher.group(1);
                }
            }
        } catch (final Exception e) {
            JDLogger.exception(e);
        }
        /* error occured, return failed */
        this.errorcount++;
        return IPCheck.CheckStatus.FAILED;
    }
}
