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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.http.Browser;

/** this CustomWebIPCheck uses the UserDefined Settings */
class CustomWebIPCheck implements IPCheckProvider {

    private final int     maxerror   = 2;
    private int           errorcount = 0;
    private final Browser br         = new Browser();

    public String getInfo() {
        return "Customized IPCheck: " + SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "Please enter Website for IPCheck here");
    }

    public IP getIP() {
        if (this.errorcount > this.maxerror) {
            /* maxerror reached , pause this method */
            this.errorcount = 0;
            return new IP_NA("MAXREACHED", IP_NA.IP_NA_SEQ_FAILED);
        }
        final String site = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "Please enter Website for IPCheck here");
        final String patt = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_PATTERN, "Please enter Regex for IPCheck here");
        Exception ee = null;
        try {
            /* check for valid website */
            new URL(site);
            /* call website and check for ip */
            this.br.setConnectTimeout(15000);
            this.br.setReadTimeout(15000);
            final Matcher matcher = Pattern.compile(patt, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(this.br.getPage(site));
            if (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    /* reset error count */
                    this.errorcount = 0;
                    return IP.getIP(matcher.group(1));
                }
            }
        } catch (final Exception e) {
            ee = e;
        }
        /* error occured , return failed */
        this.errorcount++;
        return new IP_NA(ee != null ? ee.getMessage() : null, IP_NA.IP_NA_FAILED);
    }
}

public class IPCheck {

    /** IPCheckProvider use this to Signal Error or Seq.failing */
    public static enum CheckStatus {
        FAILED, SEQFAILED
    }

    public static ArrayList<IPCheckProvider> IP_CHECK_SERVICES  = new ArrayList<IPCheckProvider>();
    private static int                       IP_CHECK_INDEX     = 0;
    private static final Object              LOCK               = new Object();

    private static final IPCheckProvider     CUSTOM_WEB_IPCHECK = new CustomWebIPCheck();

    static {
        /* IPCheck Service powered by JDownloader */
        IPCheck.IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck3.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        IPCheck.IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck2.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        IPCheck.IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck1.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        IPCheck.IP_CHECK_SERVICES.add(new WebIPCheck("http://ipcheck0.jdownloader.org", "(\\d+\\.\\d+\\.\\d+\\.\\d+)"));
        Collections.shuffle(IPCheck.IP_CHECK_SERVICES);
    }

    public static String                     LATEST_IP          = null;

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
    private static IP checkIPProvider() {
        if (IPCheck.IP_CHECK_SERVICES.size() == 0) { return IP_NA.IP_NA; }
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
    public static IP getIPAddress() {

        IP ip = null;
        synchronized (IPCheck.LOCK) {
            try {
                if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_BALANCE, true)) {
                    /* use registered ipcheckprovider (balanced) */
                    for (int i = 0; i < IPCheck.IP_CHECK_SERVICES.size() / 2 + 1; i++) {
                        ip = IPCheck.checkIPProvider();
                        if (ip.isValid()) return ip;
                        if (ip instanceof IP_NA) {
                            IP_NA n = (IP_NA) ip;
                            if (n.getReasonID() == IP_NA.IP_NA_FAILED) {
                                /* normal error, wait 3 secs for retry */
                                Thread.sleep(3000);
                            } else if (n.getReasonID() == IP_NA.IP_NA_SEQ_FAILED) {
                                /* seq error, wait 9 secs for retry */
                                Thread.sleep(9000);
                            }
                        }
                    }
                } else {
                    ip = IPCheck.CUSTOM_WEB_IPCHECK.getIP();
                }
            } catch (Exception e) {
                ip = new IP_NA(e.getMessage());
            }
        }
        if (ip instanceof IP_NA || ip instanceof IP_INVALID) {
            JDLogger.getLogger().severe("IPCheck failed");
        }
        return ip;
    }
}

/** IPCheck Provider using a Website that provides Client IP */
class WebIPCheck implements IPCheckProvider {

    private static int    maxerror   = 5;
    private final String  url;
    private Pattern       pattern;
    private int           errorcount = 0;
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

    public IP getIP() {
        if (this.errorcount > WebIPCheck.maxerror) {
            /* maxerror reached, pause this method */
            this.errorcount = 0;
            return new IP_NA("MAXREACHED", IP_NA.IP_NA_SEQ_FAILED);
        }
        Exception ee = null;
        try {
            /* call website and check for ip */
            final Matcher matcher = this.pattern.matcher(this.br.getPage(this.url));
            if (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    this.errorcount = 0;/* reset error count */
                    return IP.getIP(matcher.group(1));
                }
            }
        } catch (final Exception e) {
            ee = e;
        }
        /* error occured , return failed */
        this.errorcount++;
        return new IP_NA(ee != null ? ee.getMessage() : null, IP_NA.IP_NA_FAILED);
    }
}
