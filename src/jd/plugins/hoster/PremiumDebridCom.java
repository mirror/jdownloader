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
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premiumdebrid.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class PremiumDebridCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(20);
    private static final String                            NOCHUNKS           = "NOCHUNKS";
    private static Object                                  LOCK               = new Object();
    private static final String                            COOKIE_HOST        = "http://premiumdebrid.com";

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
        String hosts[] = null;
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
        hosts = br.getRegex("\"templates/plugmod/icons/([^<>\"]*?)\\.png\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
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
        supportedHosts.remove("youtube.com");

        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via premiumdebrid.com available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via premiumdebrid.com available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
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
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                return true;
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
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
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        login(acc, false);
        String url = Encoding.urlEncode(link.getDownloadURL());
        int maxChunks = 0;
        if (link.getBooleanProperty(PremiumDebridCom.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        String dllink = checkDirectLink(link, "premiumdebridcomdirectlink");
        if (dllink == null) {
            showMessage(link, "Phase 1/2: Generating downloadlink!");
            try {
                br.postPage("http://premiumdebrid.com/index.php", "link=" + url + "&iuser=&ipass=&comment=&yt_fmt=highest&tor_user=&tor_pass=&email=&method=tc&partSize=10&proxy=&proxyuser=&proxypass=&premium_acc=on&premium_user=&premium_pass=&path=%2Fvar%2Fwww%2Fhtml%2Ffiles%2Ftit%40nium");
                if (br.containsHTML(">Error\\[Cookie Failed\\!\\]<")) {
                    int timesFailed = link.getIntegerProperty("timesfailedpremiumdebridcom", 0);
                    if (timesFailed <= 2) {
                        timesFailed++;
                        link.setProperty("timesfailedpremiumdebridcom", timesFailed);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
                    } else {
                        link.setProperty("timesfailedpremiumdebridcom", Property.NULL);
                        tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                    }
                }
                final Form dlForm = br.getForm(0);
                if (dlForm == null || !dlForm.containsHTML("partSize")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                for (int i = 1; i <= 120; i++) {
                    showMessage(link, "Check " + i + "/120: Generating link");
                    br.submitForm(dlForm);
                    dllink = br.getRegex("\"(/files/[^<>\"]*?)\"").getMatch(0);
                    if (dllink != null) {
                        break;
                    }
                    sleep(5 * 1000l, link);
                }
            } catch (final BrowserException e) {
                int timesFailed = link.getIntegerProperty("timesfailedpremiumdebridcom", 0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty("timesfailedpremiumdebridcom", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
                } else {
                    link.setProperty("timesfailedpremiumdebridcom", Property.NULL);
                    tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                }
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = "http://premiumdebrid.com" + dllink;
        }
        showMessage(link, "Phase 2/2: Download begins!");

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info("Unhandled download error on premiumdebrid.com: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premiumdebridcomdirectlink", dllink);
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            final String errormessage = link.getLinkStatus().getErrorMessage();
            if (errormessage != null && (errormessage.startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload")) || errormessage.equals("Unerwarteter Mehrfachverbindungsfehlernull"))) {
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(PremiumDebridCom.NOCHUNKS, false) == false) {
                    link.setProperty(PremiumDebridCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
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