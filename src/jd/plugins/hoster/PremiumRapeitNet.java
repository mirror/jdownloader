//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium.rapeit.net" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class PremiumRapeitNet extends antiDDoSForHost {

    /* Tags: Script vinaget.us */
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";
    private static final String                            MAINPAGE           = "https://premium.rapeit.net";
    private static final String                            NICE_HOST          = MAINPAGE.replaceAll("(https://|http://)", "");
    private static final String                            NICE_HOSTproperty  = MAINPAGE.replaceAll("(https://|http://|\\.|\\-)", "");

    public PremiumRapeitNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE);
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/#tou";
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        ac = site_fetchAccountInfo(account);
        return ac;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /* no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {

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

        String dllink;
        site_login(account, false);
        dllink = checkDirectLink(link, NICE_HOST + "directlink");
        if (dllink == null) {
            dllink = site_get_dllink(link, account);
        }
        int maxChunks = 1;
        if (link.getBooleanProperty(PremiumRapeitNet.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        final long downloadCurrentRaw = link.getDownloadCurrentRaw();
        /*
         * 2016-02-18: Enabled resume - no idea why it was disabled before but it works fine and it is really needed for this host as
         * downloads often stop in the middle (buggy servers?) so we don't want them to restart from zero ...
         */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info("Unhandled download error on " + NICE_HOST + ": " + br.toString());
            handleErrorRetriesn(account, link, "unknownerror_download", 20);
        }
        link.setProperty(NICE_HOST + "directlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
            }
        } catch (final PluginException e) {
            if (link.getDownloadCurrent() > downloadCurrentRaw + (1024 * 1024l)) {
                throw e;
            } else {
                if (maxChunks == 1) {
                    link.setProperty(NICE_HOST + "directlink", Property.NULL);
                }
                // New V2 chunk errorhandling
                /* unknown error, we disable multiple chunks */
                if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(PremiumRapeitNet.NOCHUNKS, false) == false) {
                    link.setProperty(PremiumRapeitNet.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw e;
            }
        }
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param dl
     *            : The DownloadLink
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetriesn(final Account acc, final DownloadLink dl, final String error, final int maxRetries) throws PluginException {
        int timesFailed = dl.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        dl.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(acc, dl, 1 * 60 * 1000l);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private static Object LOCK = new Object();

    /* They only have accounts with traffic, no free/premium difference (other than no traffic) */
    private AccountInfo site_fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        if (!site_login(account, true)) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder Login-Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort (und Captcha) stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nQuick help:\r\nYou're sure that the username and password (and captcha) you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        if (!br.getURL().matches("https?://premium.rapeit.net/")) {
            getPage(MAINPAGE);
        }
        final String[] hosts = br.getRegex("ON / Domain: ([^<>\"]*?)\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        if (hosts != null && hosts.length != 0) {
            for (final String hostSet : hosts) {
                final String[] domains = hostSet.split(", ");
                for (final String domain : domains) {
                    supportedHosts.add(domain);
                }
            }
            ac.setMultiHostSupport(this, supportedHosts);
        }
        // These information are not available (anymore). 20150606
        String traffic_left = br.getRegex("Available: <strong>([^<>\"]*?)</strong>").getMatch(0);
        String traffic_downloaded = br.getRegex("Total spent: <strong>([^<>\"]*?)</strong>").getMatch(0);
        if (traffic_left != null && traffic_downloaded != null) {
            traffic_left = traffic_left.replace(",", ".");
            traffic_downloaded = traffic_downloaded.replace(",", ".");
            final long trafficleft;
            if (traffic_left.contains("-")) {
                /* Negative traffic --> Nothing left ;) */
                trafficleft = 0;
            } else {
                trafficleft = SizeFormatter.getSize(traffic_left);
            }
            final long trafficmax = trafficleft + SizeFormatter.getSize(traffic_downloaded);
            if (trafficleft == 0 && trafficmax == 0) {
                account.setType(AccountType.FREE);
            } else {
                account.setType(AccountType.PREMIUM);
            }
            ac.setTrafficLeft(trafficleft);
            ac.setTrafficMax(trafficmax);
        } else {
            /* Don't fail here just because the traffic information is null. */
            ac.setUnlimitedTraffic();
            account.setType(AccountType.PREMIUM);
        }
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(true);
        if (account.getType() == AccountType.PREMIUM) {
            ac.setStatus("Premium Account");
        } else {
            ac.setStatus("Free Account");
        }
        return ac;
    }

    private boolean site_login(final Account account, final boolean force) throws Exception {
        if (!account.getUser().matches(".+@.+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse als Benutzername an!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress as username!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    /* Avoid login captcha */
                    getPage(MAINPAGE);
                    if (br.containsHTML(">Logged in as <strong>")) {
                        return true;
                    } else {
                        /* Foced login (check) failed - clear cookies and perform a full login! */
                        this.br = new Browser();
                    }
                }

                br.setFollowRedirects(true);
                getPage(MAINPAGE + "/?login");
                String postData = "emailaddress=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                final DownloadLink dummyLink = new DownloadLink(this, "Account", NICE_HOST, MAINPAGE, true);
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    final Recaptcha rc = new Recaptcha(br, this);
                    rc.findID();
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String code = getCaptchaCode("recaptcha", cf, dummyLink);
                    postData += "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(code);
                } else {
                    final String code = getCaptchaCode("/random_image.php", dummyLink);
                    postData += "&txtCaptcha=" + Encoding.urlEncode(code);
                }
                postPage("/?login", postData);
                if (br.getCookie(MAINPAGE, "session") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder Login-Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort (und Captcha) stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nQuick help:\r\nYou're sure that the username and password (and captcha) you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                account.clearCookies("");
                return false;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private String site_get_dllink(final DownloadLink link, final Account acc) throws Exception {
        String dllink;
        final String url = Encoding.urlEncode(link.getDownloadURL());
        postPage(MAINPAGE, "inputlink=" + url);
        if (br.containsHTML(">You can't generate files until you have available premium bandwidth")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNo premium bandwidth", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        dllink = br.getRegex("href=\"(https?://[a-z0-9]+\\.rapeit\\.net(:\\d+)?/dl/[^<>\"]+)\" target=\"_blank\"").getMatch(0);
        if (dllink == null) {
            if (br.getRedirectLocation() != null && br.getRedirectLocation().matches("https?://premium\\.rapeit\\.net/?")) {
                // they probably switched/enforce from http to https or https to http
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            }
            handleErrorRetriesn(acc, link, "dllink_null", 10);
        }
        dllink = dllink.replace("\\", "");
        return dllink;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                final URLConnectionAdapter con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}