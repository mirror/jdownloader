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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premiumdebrid.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class PremiumDebridCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap        = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem                   = new AtomicInteger(20);
    private static final String                            NOCHUNKS                  = "NOCHUNKS";
    private static Object                                  LOCK                      = new Object();
    private static final String                            COOKIE_HOST               = "http://premiumdebrid.com";
    private static final long                              RETRIES_FAILED_DLLINKNULL = 10;

    public PremiumDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://premiumdebrid.com/index.php");
    }

    @Override
    public String getAGBLink() {
        return "http://premiumdebrid.com/index.php";
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return maxPrem.get();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        if (!login(account, false)) {
            account.setValid(false);
            return ac;
        }
        if (!"http://premiumdebrid.com/index.php".equals(br.getURL())) br.getPage("http://premiumdebrid.com/index.php");
        if (!br.containsHTML("class=\"userinfo\">Vip</font>")) {
            ac.setStatus("This is no premium account!");
            account.setValid(false);
            return ac;
        }
        final String expire = br.getRegex("<td>Expire in <span style=\\'color: #FFC500\\' class=\\'userinfo\\'>([^<>\"]*?)</span>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ac;
        }
        ac.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy", Locale.ENGLISH));
        try {
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }
        ac.setStatus("Premium User [Vip]");
        String trafficLeft = br.getRegex("User Traffic : <font color=\"#FFC500\" class=\"userinfo\">([^<>\"]*?)</font>").getMatch(0);
        if (trafficLeft != null) {
            if (trafficLeft.equals("Unlimeted") || trafficLeft.equals("Unlimited")) {
                ac.setUnlimitedTraffic();
            } else {
                ac.setTrafficLeft(SizeFormatter.getSize(trafficLeft));
            }
        }
        // now let's get a list of all supported hosts:
        ArrayList<String> supportedHosts = new ArrayList<String>();
        final HashMap<String, Boolean> hostOptions = new HashMap<String, Boolean>();
        final String[] allHosts = br.getRegex("<tr id=\"host_[a-z0-9\\-]+\">(.*?)</tr>").getColumn(0);
        if (allHosts != null && allHosts.length != 0) {
            for (final String hostinfo : allHosts) {
                String currentHost = new Regex(hostinfo, "templates/plugmod/image/[a-z0-9\\-\\.]+\" alt=\"[a-z0-9\\-\\.]+\" title=\"[a-z0-9\\-\\.]+\">([^<>\"]*?)</p></td>").getMatch(0);
                currentHost = Encoding.htmlDecode(currentHost.trim()).toLowerCase();
                supportedHosts.add(currentHost);
                if (hostinfo.contains("LINK GENERATOR")) {
                    hostOptions.put(currentHost, true);
                } else {
                    hostOptions.put(currentHost, false);
                }
            }
        }
        if (supportedHosts.contains("uploaded.net") || supportedHosts.contains("ul.to") || supportedHosts.contains("uploaded.to")) {
            if (!supportedHosts.contains("uploaded.net")) {
                supportedHosts.add("uploaded.net");
            }
            if (!supportedHosts.contains("ul.to")) {
                supportedHosts.add("ul.to");
            }
            if (!supportedHosts.contains("uploaded.to")) {
                supportedHosts.add("uploaded.to");
            }
        }

        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via premiumdebrid.com available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via premiumdebrid.com available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        account.setProperty("multiHostSupportHostsOptions", hostOptions);
        return ac;
    }

    @SuppressWarnings("unchecked")
    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                boolean fullLogin = force;
                /** Load cookies */
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        br.getPage("http://premiumdebrid.com/index.php");
                        if (br.containsHTML("google\\.com/recaptcha/")) fullLogin = true;
                    }
                } else {
                    fullLogin = true;
                }
                if (fullLogin) {
                    if (!"http://premiumdebrid.com/index.php".equals(br.getURL())) br.getPage("http://premiumdebrid.com/index.php");
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
                    if (rcID == null) {
                        logger.warning("Expected login captcha is not there!");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.setId(rcID);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "premiumdebrid.com", "http://premiumdebrid.com", true);
                    final String c = getCaptchaCode(cf, dummyLink);
                    br.postPage("http://premiumdebrid.com/login.php?action=login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                    if (br.getCookie("http://premiumdebrid.com/", "user") == null) {
                        final String lang = System.getProperty("user.language");
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!\r\nGehe außerdem sicher, dass du das Captcha richtig eingibst!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!\r\nAlso make sure to enter the login captcha correctly!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                saveCookies(account);
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                return true;
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("unchecked")
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        login(acc, false);
        int maxChunks = 0;
        if (link.getBooleanProperty(PremiumDebridCom.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        final Object ret = acc.getProperty("multiHostSupportHostsOptions", null);
        boolean download_generator = true;
        final HashMap<String, Boolean> multihostoptions = (HashMap<String, Boolean>) ret;
        if (multihostoptions != null && multihostoptions.containsKey(link.getHost())) {
            Boolean bool = multihostoptions.get(link.getHost());
            if (bool != null) download_generator = bool;
        } else {
            logger.info("Current host " + link.getHost() + " was not found in list -> Downloading via generator!");
        }
        String dllink = checkDirectLink(link, "premiumdebridcomdirectlink");

        if (dllink == null) {
            showMessage(link, "Phase 1/2: Generating downloadlink!");
            if (download_generator) {
                dllink = generatedllink_generator(link, acc);
            } else {
                dllink = generatedllink_downloader(link, acc);
            }
            if (dllink == null) {
                logger.info("premiumdebrid.com: dllink is null");
                int timesFailed = link.getIntegerProperty("timesfailedpremiumdebridcom_dllinknull", 1);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 10) {
                    timesFailed++;
                    link.setProperty("timesfailedpremiumdebridcom_dllinknull", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown server error");
                } else {
                    logger.info("premiumdebrid.com: dllink is null - disabling current host");
                    link.setProperty("timesfailedpremiumdebridcom_dllinknull", Property.NULL);
                    tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                }
            }
        }
        showMessage(link, "Phase 2/2: Download begins!");

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        /* It may happen that they send text/html as content type even though they send files */
        if (dl.getConnection().getContentType().contains("html") && dl.getConnection().getLongContentLength() < br.getLoadLimit()) {
            br.followConnection();
            logger.info("premiumdebrid.com: Unknown download error");
            int timesFailed = link.getIntegerProperty("timesfailedpremiumdebridcom_unknowndlerror", 1);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= RETRIES_FAILED_DLLINKNULL) {
                timesFailed++;
                link.setProperty("timesfailedpremiumdebridcom_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                logger.info("premiumdebrid.com: Unknown download error - disabling current host");
                link.setProperty("timesfailedpremiumdebridcom_unknowndlerror", Property.NULL);
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            }
        }
        // Multi-Host tags filenames - stop this
        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())).replace("PremiumDebrid.com", ""));
        link.setProperty("premiumdebridcomdirectlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(PremiumDebridCom.NOCHUNKS, false) == false) {
                    link.setProperty(PremiumDebridCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(PremiumDebridCom.NOCHUNKS, false) == false) {
                link.setProperty(PremiumDebridCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private void saveCookies(final Account acc) {
        /** Save cookies */
        final HashMap<String, String> cookies = new HashMap<String, String>();
        final Cookies add = this.br.getCookies(COOKIE_HOST);
        for (final Cookie c : add.getCookies()) {
            cookies.put(c.getKey(), c.getValue());
        }
        acc.setProperty("cookies", cookies);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
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

    private static final String PASSNEEDED = "<b>Security ID</b>";

    private String generatedllink_generator(final DownloadLink dl, final Account acc) throws IOException, PluginException {
        final String url = Encoding.urlEncode(dl.getDownloadURL());
        br.getPage("http://www.premiumdebrid.com/dl.php");
        if (br.containsHTML(PASSNEEDED)) {
            synchronized (LOCK) {
                br.setFollowRedirects(false);
                boolean failed = true;
                String premiumpass = null;
                for (int i = 0; i <= 3; i++) {
                    premiumpass = acc.getStringProperty("premiumdebridpremiumpassword", null);
                    if (premiumpass == null) premiumpass = Plugin.getUserInput("Please enter the premium password for premiumdebrid.com/link_generator/login.php", dl);
                    try {
                        br.postPage("http://www.premiumdebrid.com/link_generator/login.php", "submit=Login+Now&secure=" + Encoding.urlEncode(premiumpass));
                    } catch (final BrowserException omgwtf) {
                        // Probably redirectloop
                        acc.setProperty("premiumdebridpremiumpassword", Property.NULL);
                        continue;
                    }
                    if (br.containsHTML(PASSNEEDED) || br.getCookie("http://premiumdebrid.com/", "secureid") == null) {
                        acc.setProperty("premiumdebridpremiumpassword", Property.NULL);
                        continue;
                    }
                    acc.setProperty("premiumdebridpremiumpassword", premiumpass);
                    acc.setProperty("timesfailedpremiumdebridcom_premiumpassword", 0);
                    failed = false;
                    break;
                }
                if (failed) {
                    int timesFailed_premiumpass = acc.getIntegerProperty("timesfailedpremiumdebridcom_premiumpassword", 1);
                    if (br.getCookie("http://premiumdebrid.com/", "secureid") == null || timesFailed_premiumpass >= 3) throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPremium password missing or invalid!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    timesFailed_premiumpass++;
                    acc.setProperty("timesfailedpremiumdebridcom_premiumpassword", timesFailed_premiumpass);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Premium pass retry");
                }
                br.setFollowRedirects(true);
                // Save cookies so we don't have o enter the password next time
                saveCookies(acc);
            }
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "*/*");
        br.postPage("http://www.premiumdebrid.com/dl.php?rand=0." + System.currentTimeMillis(), "urllist=" + url);
        if (br.containsHTML("Hoster could not be parsed from link, the link was invalid or the hoster is not supported")) {
            logger.info("premiumdebrid.com: Disabling current host because server says 'Hoster could not be parsed from link, the link was invalid or the hoster is not supported'");
            tempUnavailableHoster(acc, dl, 60 * 60 * 1000l);
        } else if (br.containsHTML("Error: Download link not found\\.<")) {
            logger.info("premiumdebrid.com: Disabling current host because server says 'Error: Download link not found\\.<'");
            tempUnavailableHoster(acc, dl, 2 * 60 * 60 * 1000l);
        }
        String dllink = br.getRegex("here to download\\' href=\\'(https?[^<>\"]*?)\\'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("(\\'|\")(https?://(www\\.)?premiumdebrid\\.com/dl\\.php\\?file=[A-Za-z0-9]+)(\\'|\")").getMatch(1);
        return dllink;
    }

    private String generatedllink_downloader(final DownloadLink dl, final Account acc) throws Exception {
        final String url = Encoding.urlEncode(dl.getDownloadURL());
        String dllink = null;
        try {
            br.postPage("http://premiumdebrid.com/index.php", "link=" + url + "&iuser=&ipass=&comment=&yt_fmt=highest&tor_user=&tor_pass=&email=&method=tc&partSize=10&proxy=&proxyuser=&proxypass=&premium_acc=on&premium_user=&premium_pass=&path=%2Fvar%2Fwww%2Fhtml%2Ffiles%2F" + Encoding.urlEncode(acc.getUser()));
            if (br.containsHTML(">Error\\[Cookie Failed\\!\\]<")) {
                logger.info("premiumdebrid.com: Cookie error");
                int timesFailed = dl.getIntegerProperty("timesfailedpremiumdebridcom_cookie", 1);
                dl.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 10) {
                    timesFailed++;
                    dl.setProperty("timesfailedpremiumdebridcom_cookie", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
                } else {
                    dl.setProperty("timesfailedpremiumdebridcom_cookie", Property.NULL);
                    tempUnavailableHoster(acc, dl, 60 * 60 * 1000l);
                }
            } else if (br.containsHTML(">Error: Download Link \\[PREMIUM\\] not found")) {
                logger.info("premiumdebrid.com: premiumnotfound error");
                int timesFailed = dl.getIntegerProperty("timesfailedpremiumdebridcom_premiumnotfound", 1);
                dl.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 10) {
                    timesFailed++;
                    dl.setProperty("timesfailedpremiumdebridcom_premiumnotfound", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "premiumnotfound error");
                } else {
                    dl.setProperty("timesfailedpremiumdebridcom_premiumnotfound", Property.NULL);
                    tempUnavailableHoster(acc, dl, 60 * 60 * 1000l);
                }
            }
            final Form dlForm = br.getForm(0);
            if (dlForm == null || !dlForm.containsHTML("partSize")) {
                logger.info("premiumdebrid.com: Unknown error");
                int timesFailed = dl.getIntegerProperty("timesfailedpremiumdebridcom_unknown", 1);
                dl.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 10) {
                    timesFailed++;
                    dl.setProperty("timesfailedpremiumdebridcom_unknown", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
                } else {
                    dl.setProperty("timesfailedpremiumdebridcom_unknown", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            br.submitForm(dlForm);
            dllink = br.getRegex("\"(/files/[^<>\"]*?)\"").getMatch(0);
        } catch (final BrowserException e) {
            int timesFailed = dl.getIntegerProperty("timesfailedpremiumdebridcom_browserexception", 0);
            if (timesFailed <= 2) {
                timesFailed++;
                dl.setProperty("timesfailedpremiumdebridcom_browserexception", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
            } else {
                dl.setProperty("timesfailedpremiumdebridcom_browserexception", Property.NULL);
                tempUnavailableHoster(acc, dl, 60 * 60 * 1000l);
            }
        }
        if (dllink != null) dllink = "http://premiumdebrid.com" + dllink;
        return dllink;
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
                    if (unavailableMap.size() == 0) hostUnavailableMap.remove(account);
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