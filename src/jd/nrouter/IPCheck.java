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
import jd.http.Browser;

/** IPCheck Provider using a Website that provides Client IP */
class WebIPCheck implements IPCheckProvider {

    private static int maxerror = 5;
    private String url;
    private Pattern pattern;
    private int errorcount = 0;
    private Browser br;

    public WebIPCheck(String url, String regex) {
        this.url = url;
        errorcount = 0;
        if (regex != null) pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        br = new Browser();
        br.setConnectTimeout(15000);
        br.setReadTimeout(15000);
    }

    public Object getIP() {
        if (errorcount > maxerror) {
            /* maxerror reached, pause this method */
            errorcount = 0;
            return IPCheck.CheckStatus.SEQFAILED;
        }
        try {
            /* call website and check for ip */
            Matcher matcher = pattern.matcher(br.getPage(url));
            if (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    errorcount = 0;/* reset error count */
                    return matcher.group(1);
                }
            }
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        /* error occured, return failed */
        errorcount++;
        return IPCheck.CheckStatus.FAILED;
    }

    public String getInfo() {
        return url;
    }
}

public class IPCheck {

    /** IPCheckProvider use this to Signal Error or Seq.failing */
    public static enum CheckStatus {
        FAILED, SEQFAILED
    }

    public static ArrayList<IPCheckProvider> IP_CHECK_SERVICES = new ArrayList<IPCheckProvider>();
    private static int IP_CHECK_INDEX = 0;
    private static final Object LOCK = new Object();

    private static IPCheckProvider CustomIPCheckProvider = null;

    /** this CustomWebIPCheck uses the UserDefined Settings */
    private static final IPCheckProvider CustomWebIPCheck = new IPCheckProvider() {

        private int maxerror = 2;
        private int errorcount = 0;
        private Browser br = new Browser();

        public Object getIP() {
            if (errorcount > maxerror) {
                /* maxerror reached, pause this method */
                errorcount = 0;
                return IPCheck.CheckStatus.SEQFAILED;
            }
            String site = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "Please enter Website for IPCheck here");
            String patt = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_PATTERN, "Please enter Regex for IPCheck here");
            try {
                new URL(site); /* check for valid website */
                /* call website and check for ip */
                br.setConnectTimeout(15000);
                br.setReadTimeout(15000);
                Matcher matcher = Pattern.compile(patt, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(br.getPage(site));
                if (matcher.find()) {
                    if (matcher.groupCount() > 0) {
                        errorcount = 0;/* reset error count */
                        return matcher.group(1);
                    }
                }
            } catch (Exception e) {
                JDLogger.exception(e);
            }
            /* error occured, return failed */
            errorcount++;
            return IPCheck.CheckStatus.FAILED;
        }

        public String getInfo() {
            return "Customized IPCheck: " + SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "Please enter Website for IPCheck here");
        }
    };

    static {
        /* IPCheck Service powered by JDownloader */
        IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck3.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck2.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck1.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck0.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        Collections.shuffle(IP_CHECK_SERVICES);
    }

    /** set a customizedIPCheckProvider, eg IPCheck via UPNP */
    public static void setCustomIPCheckProvider(IPCheckProvider cust) {
        CustomIPCheckProvider = cust;
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
    public static String getIPAddress() {
        Object ip = null;
        synchronized (LOCK) {
            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_BALANCE, true)) {
                if (CustomIPCheckProvider != null) {
                    /*
                     * is a customized IPCheckProvider set , then use it
                     */
                    ip = CustomIPCheckProvider.getIP();
                } else {
                    /* use registered ipcheckprovider (balanced) */
                    for (int i = 0; i < ((IP_CHECK_SERVICES.size() / 2) + 1); i++) {
                        ip = IPCheck.checkIPProvider();

                        if (ip == null || ip == CheckStatus.FAILED) {
                            /* normal error, wait 3 secs for retry */
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                            }
                        } else if (ip == CheckStatus.SEQFAILED) {
                            /* seq error, wait 9 secs for retry */
                            try {
                                Thread.sleep(9000);
                            } catch (InterruptedException e) {
                            }
                        } else if (ip instanceof String) {
                            break;
                        }
                    }
                }
            } else {
                /* use userdefined ipcheck, try it only once */
                ip = CustomWebIPCheck.getIP();
            }
        }
        if (ip == null || ip instanceof CheckStatus || !(ip instanceof String)) {
            JDLogger.getLogger().severe("IPCheck failed");
            return "na";
        }
        return ip.toString().trim();
    }

    /**
     * Uses IP_CHECK_SERVICES (current JDownloader IPCheck) to get the current
     * IP. rotates through IP_CHECK_SERVICES which is random sorted.
     * 
     * @return current IP string object or IPCheck.CheckStatus.FAILED if there
     *         has been an error or IPCheck.CheckStatus.SEQFAILED if this method
     *         should be paused
     */
    private static Object checkIPProvider() {
        if (IP_CHECK_SERVICES.size() == 0) return null;
        synchronized (LOCK) {
            IP_CHECK_INDEX = IP_CHECK_INDEX % IP_CHECK_SERVICES.size();
            IPCheckProvider ipcheck = IP_CHECK_SERVICES.get(IP_CHECK_INDEX);
            IP_CHECK_INDEX++;
            return ipcheck.getIP();
        }
    }

    public static String LATEST_IP = null;

}
