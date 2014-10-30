//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "u-labs.de" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class UlabsDe extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";
    private static final String                            MAINPAGE           = "http://u-labs.de";
    private static final String                            NICE_HOST          = MAINPAGE.replaceAll("(https://|http://)", "");
    private static final String                            NICE_HOSTproperty  = MAINPAGE.replaceAll("(https://|http://|\\.|\\-)", "");

    public UlabsDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://u-labs.de/register.php");
    }

    @Override
    public String getAGBLink() {
        return "https://u-labs.de/impressum.php";
    }

    private boolean useAPI() {
        return false;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        if (this.useAPI()) {
            ac = api_fetchAccountInfo(account);
        } else {
            ac = site_fetchAccountInfo(account);
        }
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
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        String dllink = checkDirectLink(link, NICE_HOST + "directlink");
        if (dllink == null) {
            if (this.useAPI()) {
                dllink = api_get_dllink(link, acc);
            } else {
                dllink = site_get_dllink(link, acc);
            }
        }
        int maxChunks = 1;
        if (link.getBooleanProperty(UlabsDe.NOCHUNKS, false)) {
            maxChunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            final String errorcode = new Regex(br.getURL(), "code=(\\d+)").getMatch(0);
            if ("1".equals(errorcode)) {
                logger.info("Your registration / account is invalid");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid registration/account", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if ("2".equals(errorcode)) {
                logger.info("Invalid URL --> Temporarily remove current host from hostlist");
                tempUnavailableHoster(acc, link, 3 * 60 * 60 * 1000);
            } else if ("3".equals(errorcode)) {
                logger.info("Not enough traffic to download file --> Temporarily remove current host from hostlist");
                tempUnavailableHoster(acc, link, 1 * 60 * 60 * 1000);
            } else if ("4".equals(errorcode)) {
                logger.info("Downloadlink seems to be offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if ("5".equals(errorcode)) {
                logger.info("An unknown error happened");
                tempUnavailableHoster(acc, link, 1 * 60 * 60 * 1000);
            } else if ("6".equals(errorcode)) {
                logger.info("An 'API error' happened");
                tempUnavailableHoster(acc, link, 1 * 60 * 60 * 1000);
            } else if ("7".equals(errorcode)) {
                logger.info("The host whose downloadlink you tried is not supported by this multihost  --> Temporarily remove current host from hostlist");
                tempUnavailableHoster(acc, link, 3 * 60 * 60 * 1000);
            } else if ("8".equals(errorcode)) {
                logger.info("There are no available host-accounts at the moment  --> Temporarily remove current host from hostlist");
                tempUnavailableHoster(acc, link, 15 * 60 * 60 * 1000);
            } else if ("10".equals(errorcode)) {
                logger.info("Your account is banned at the moment --> Disable it");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Banned account!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (errorcode != null) {
                logger.warning("Unhandled errorcode: " + errorcode);
            }
            logger.info("Unhandled download error on " + NICE_HOST + ": " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(UlabsDe.NOCHUNKS, false) == false) {
                    link.setProperty(UlabsDe.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            if (maxChunks == 1) {
                link.setProperty(NICE_HOST + "directlink", Property.NULL);
            }
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(UlabsDe.NOCHUNKS, false) == false) {
                link.setProperty(UlabsDe.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
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
    private void handlePluginBroken(final Account acc, final DownloadLink dl, final String error, final int maxRetries) throws PluginException {
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
            // tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l);
            /* Test mode, usually never throw plugin defect */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private static Object LOCK = new Object();

    private AccountInfo site_fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        if (!site_login(account, true)) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        if (br.getURL() == null || !br.getURL().equals("https://u-labs.de/accountBoerse/home/index/")) {
            br.getPage("https://u-labs.de/accountBoerse/home/index/");
        }
        final String[] hosts = br.getRegex("\"name\":[\t\n\r ]+\"([^<>\"]*?)\"").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (final String host : hosts) {
            supportedHosts.add(host.toLowerCase());
        }
        long trafficavailable = 0;
        long trafficmax = 0;
        final String[] traffics = br.getRegex("\"freeTraffic\":[\t\n\r ]+(\\d+)").getColumn(0);
        final String[] traffics_max = br.getRegex("\"totalTraffic\":[\t\n\r ]+(\\d+)").getColumn(0);
        for (final String traffic : traffics) {
            trafficavailable += Long.parseLong(traffic) * 1024 * 1024;
        }
        for (final String trafficmx : traffics_max) {
            trafficmax += Long.parseLong(trafficmx) * 1024 * 1024;
        }
        ac.setTrafficLeft(trafficavailable);
        ac.setTrafficMax(trafficmax);
        /* They only have free accounts */
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(true);
        ac.setMultiHostSupport(this, supportedHosts);
        ac.setStatus("Free User");
        return ac;
    }

    @SuppressWarnings("unchecked")
    private boolean site_login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return true;
                    }
                }
                br.setFollowRedirects(true);
                String postData = "cookieuser=1&s=&securitytoken=guest&do=login&url=https%3A%2F%2Fu-labs.de%2FaccountBoerse%2Fhome%2Findex%2F&hiddenlogin=do&vb_login_username=" + Encoding.urlEncode(account.getUser()) + "&vb_login_password=" + Encoding.urlEncode(account.getPass());
                br.postPage("https://u-labs.de/login.php?do=login", postData);
                if (br.getCookie(MAINPAGE, "bb_password") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                return true;
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                return false;
            }
        }
    }

    private String site_get_dllink(final DownloadLink link, final Account acc) throws Exception {
        String dllink;
        String hoster;
        site_login(acc, false);
        br.getPage("https://u-labs.de/accountBoerse/home/index/");
        final String userid = br.getRegex("\\'userid\\':[\t\n\r ]+(\\d+)").getMatch(0);
        final String authentificationToken = br.getRegex("\\'authentificationToken\\':[\t\n\r ]+\\'([^<>\"]*?)\\'").getMatch(0);
        final String externalBackendURL = br.getRegex("\\'externalBackendURL\\':[\t\n\r ]+\\'(http[^<>\"]*?)\\'").getMatch(0);
        if (userid == null || authentificationToken == null || externalBackendURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Assumption that they only have 2 supported hosts */
        if (link.getDownloadURL().contains("share-online")) {
            hoster = "shareOnlineBiz";
        } else {
            hoster = "uploadedNet";
        }
        dllink = externalBackendURL + "&url=" + Encoding.urlEncode(link.getDownloadURL()) + "&authentificationToken=" + authentificationToken + "&userid=" + userid + "&hoster=" + hoster;
        return dllink;
    }

    private AccountInfo api_fetchAccountInfo(final Account account) throws Exception {
        if (!api_login(account, true)) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        return null;
    }

    private boolean api_login(final Account account, final boolean force) throws Exception {
        return false;
    }

    private String api_get_dllink(final DownloadLink link, final Account acc) throws Exception {
        return null;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
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
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
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