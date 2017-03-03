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

package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "multihosters.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class MultihostersCom extends PluginForHost {

    private static WeakHashMap<Account, HashMap<String, Long>> hostUnavailableMap = new WeakHashMap<Account, HashMap<String, Long>>();

    public MultihostersCom(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(4 * 1000l);
        this.enablePremium("http://www.multihosters.com/Members/SelectProduct.aspx");
    }

    /* Important - all of these belong together: zevera.com, multihosters.com, putdrive.com(?!) */
    private static final String NICE_HOST         = "multihosters.com";
    private static final String NICE_HOSTproperty = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private Account             currAcc           = null;
    private DownloadLink        currDownloadLink  = null;

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        logger.info("Multihosters: Accountinfo called");
        AccountInfo ac = new AccountInfo();
        ac.setProperty("multiHostSupport", Property.NULL);
        final boolean follow = br.isFollowingRedirects();
        br.setFollowRedirects(true);
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String loginPage = null;
        String username = Encoding.urlEncode(account.getUser());
        String pass = Encoding.urlEncode(account.getPass());
        long trafficLeft = -1;
        String hosts = null;
        loginPage = br.getPage("http://www.multihosters.com/jDownloader.ashx?cmd=accountinfo&login=" + username + "&pass=" + pass);
        /* Looks like it means "No traffic" but this is what their API returns if logindata = invalid. */
        if (br.containsHTML("No trafic")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        String infos[] = br.getRegex("(.*?)(,|$)").getColumn(0);

        String endSubscriptionDate = new Regex(infos[1], "EndSubscriptionDate:(.+)").getMatch(0);
        ac.setValidUntil(TimeFormatter.getMilliSeconds(endSubscriptionDate, "yyyy/MM/dd HH:mm:ss", null));

        String dayTrafficLimit = new Regex(infos[0], "DayTrafficLimit:(\\d+)").getMatch(0);
        String availableTodayTraffic = new Regex(infos[3], "AvailableTodayTraffic:(\\d+)").getMatch(0);
        String availableExtraTraffic = new Regex(infos[4], "AvailableExtraTraffic:(\\d+)").getMatch(0);
        logger.info("Multihosters: AvailableTodayTraffic=" + availableTodayTraffic);
        if (dayTrafficLimit.equals("0") && availableTodayTraffic.equals("0") && availableExtraTraffic.equals("0")) {
            ac.setTrafficLeft(0);
        } else if (availableTodayTraffic.equals("0")) {
            ac.setUnlimitedTraffic();
        } else {
            ac.setTrafficLeft(SizeFormatter.getSize(availableTodayTraffic + "MiB"));
        }
        trafficLeft = Long.parseLong(availableTodayTraffic);
        hosts = br.getPage("http://www.multihosters.com/jDownloader.ashx?cmd=gethosters");
        br.setFollowRedirects(follow);
        if (loginPage == null || trafficLeft < 0) {
            account.setValid(false);
            account.setTempDisabled(false);
        } else {
            if (br.containsHTML("<[^>]+>")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Provider issued html", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            ArrayList<String> supportedHosts = new ArrayList<String>();
            if (!"0".equals(hosts.trim())) {
                String hoster[] = new Regex(hosts, "(.*?)(,|$)").getColumn(0);
                if (hoster != null) {
                    for (String host : hoster) {
                        if (host == null || host.length() == 0) {
                            continue;
                        }
                        supportedHosts.add(host.trim());
                    }
                }
            }
            account.setValid(true);
            ac.setMultiHostSupport(this, supportedHosts);
        }
        ac.setStatus("Premium Account");
        account.setType(AccountType.PREMIUM);
        return ac;
    }

    @Override
    public String getAGBLink() {
        return "http://multihosters.com";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return 6;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, link);

        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }

        final String user = Encoding.urlEncode(account.getUser());
        final String pw = Encoding.urlEncode(account.getPass());
        final String url = Encoding.urlEncode(link.getDownloadURL());
        final String dllink = "http://www.multihosters.com/jDownloader.ashx?cmd=generatedownloaddirect&login=" + user + "&pass=" + pw + "&olink=" + url + "&FilePass=";

        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);

        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        }
        /*
         * download is not contentdisposition, so remove this host from premiumHosts list
         */
        br.followConnection();
        /* temp disabled the host */
        if (br.containsHTML("No trafic")) {
            // account has no traffic, disable hoster for 1h
            tempUnavailableHoster(5 * 60 * 1000l);
        } else if (br.containsHTML(">You have exceeded the maximum amount of fair usage of our service")) {
            /*
             * Free account limits reached and an additional download-try failed or account cookie is invalid -> permanently disable account
             */
            String statusMessage;
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                statusMessage = "\r\nFree Account Limits erreicht. Kaufe dir einen premium Account um weiter herunterladen zu können.";
            } else {
                statusMessage = "\r\nFree account limits reached. Buy a premium account to continue downloading.";
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (br.containsHTML("Error")) {
            handleErrorRetries("stpd_error", 5, 5 * 60 * 1000);
        } else {
            handleErrorRetries("unknown_error", 5, 2 * 60 * 1000);
        }

        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetries(final String error, final int maxRetries, final long disableTime) throws PluginException {
        int timesFailed = this.currDownloadLink.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        this.currDownloadLink.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(disableTime);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(final long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(this.currAcc);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(this.currAcc, unavailableMap);
            }
            /* wait 30 mins to retry this host */
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account != null) {
            synchronized (hostUnavailableMap) {
                final HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
                if (unavailableMap != null) {
                    final Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                    if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}