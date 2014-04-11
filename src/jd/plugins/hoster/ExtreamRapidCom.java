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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "extreamrapid.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class ExtreamRapidCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(20);
    private static final String                            NOCHUNKS           = "NOCHUNKS";
    private static Object                                  LOCK               = new Object();
    private static final String                            COOKIE_HOST        = "http://extreamrapid.com";
    private static final String                            NICE_HOST          = "extreamrapid.com";

    public ExtreamRapidCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://extreamrapid.com/terms.php");
    }

    @Override
    public String getAGBLink() {
        return "http://extreamrapid.com/terms.php";
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
        if (!"http://extreamrapid.com/vip/index.php".equals(br.getURL())) br.getPage("http://extreamrapid.com/vip/index.php");
        final String acctype = br.getRegex("<td>Account Type : <font color=\"#FFC500\" class=\"userinfo\">([^<>\"]*?)</font>").getMatch(0);
        if (acctype == null || !acctype.toLowerCase().contains("vip")) {
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
        ac.setStatus("Account type: " + acctype);
        String trafficLeft = br.getRegex("User Traffic : <font color=\"#FFC500\" class=\"userinfo\">([^<>\"]*?)</font>").getMatch(0);
        if (trafficLeft != null) {
            if (trafficLeft.equals("Unlimeted") || trafficLeft.equals("Unlimited")) {
                ac.setUnlimitedTraffic();
            } else {
                ac.setTrafficLeft(SizeFormatter.getSize(trafficLeft));
            }
        }
        // now let's get a list of all supported hosts:
        final String hostsText = br.getRegex("<div dir=\"rtl\" align=\"left\" style=\"padding\\-left:5px;\">(.*?)</td>").getMatch(0);
        if (hostsText != null) hosts = new Regex(hostsText, "class=\"plugincollst\">([^<>\"]*?)</span>").getColumn(0);
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
        ac.setStatus("Account valid");
        ac.setProperty("multiHostSupport", supportedHosts);
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
                        br.getPage(COOKIE_HOST + "/vip/index.php");
                        if (!br.containsHTML("<td>User IP :")) fullLogin = true;
                    }
                } else {
                    fullLogin = true;
                }
                if (fullLogin) {
                    if (!"http://extreamrapid.com/vip/index.php".equals(br.getURL())) br.getPage("http://extreamrapid.com/vip/index.php");
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.postPage(COOKIE_HOST + "/vip/login.php?action=login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    if (br.getCookie(COOKIE_HOST, "user") == null) {
                        final String lang = System.getProperty("user.language");
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!\r\nGehe außerdem sicher, dass du das Captcha richtig eingibst!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!\r\nAlso make sure to enter the login captcha correctly!", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        int maxChunks = 0;
        if (link.getBooleanProperty(ExtreamRapidCom.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        String dllink = checkDirectLink(link, "extreamrapidcomdirectlink");
        if (dllink == null) {
            showMessage(link, "Phase 1/2: Generating downloadlink!");
            dllink = generateDllink(link, acc);
            if (dllink == null) {
                logger.info(NICE_HOST + ": dllink is null");
                int timesFailed = link.getIntegerProperty("timesfailedextreamrapidcom_dllinknull", 1);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 10) {
                    timesFailed++;
                    link.setProperty("timesfailedextreamrapidcom_dllinknull", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown server error");
                } else {
                    link.setProperty("timesfailedextreamrapidcom_dllinknull", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        showMessage(link, "Phase 2/2: Download begins!");

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info(NICE_HOST + ": Unknown download error");
            int timesFailed = link.getIntegerProperty("timesfailedextreamrapidcom_unknowndlerror", 1);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 10) {
                timesFailed++;
                link.setProperty("timesfailedextreamrapidcom_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                logger.info(NICE_HOST + ": Unknown download error - disabling current host");
                link.setProperty("timesfailedextreamrapidcom_unknowndlerror", Property.NULL);
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            }
        }
        link.setProperty("extreamrapidcomdirectlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(ExtreamRapidCom.NOCHUNKS, false) == false) {
                    link.setProperty(ExtreamRapidCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(ExtreamRapidCom.NOCHUNKS, false) == false) {
                link.setProperty(ExtreamRapidCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private String generateDllink(final DownloadLink dl, final Account acc) throws Exception {
        final String url = Encoding.urlEncode(dl.getDownloadURL());
        String dllink = null;
        try {
            br.postPage("http://extreamrapid.com/vip/index.php", "link=" + url + "&referer=&iuser=&ipass=&comment=&yt_fmt=highest&tor_user=&tor_pass=&mu_cookie=&cookie=&email=&method=tc&partSize=10&proxy=&proxyuser=&proxypass=&premium_acc=on&premium_user=&premium_pass=&path=%2Fvar%2Fwww%2Fhtml%2Fvip%2Ffiles%2Fjdownloader");
            if (br.containsHTML(">Error\\[Cookie Failed\\!\\]<")) {
                logger.info(NICE_HOST + ": Cookie error");
                int timesFailed = dl.getIntegerProperty("timesfailedextreamrapidcom_cookie", 1);
                if (timesFailed <= 10) {
                    timesFailed++;
                    dl.setProperty("timesfailedextreamrapidcom_cookie", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
                } else {
                    logger.info(NICE_HOST + ": Cookie error - disabling current host");
                    dl.setProperty("timesfailedextreamrapidcom_cookie", Property.NULL);
                    tempUnavailableHoster(acc, dl, 60 * 60 * 1000l);
                }
            }
            final Form dlForm = br.getForm(0);
            if (dlForm == null || !dlForm.containsHTML("partSize")) {
                logger.info(NICE_HOST + ": Unknown error");
                int timesFailed = dl.getIntegerProperty("timesfailedextreamrapidcom_unknown", 1);
                dl.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 10) {
                    timesFailed++;
                    dl.setProperty("timesfailedextreamrapidcom_unknown", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
                } else {
                    logger.info(NICE_HOST + ": Unknown error - disabling current host");
                    dl.setProperty("timesfailedextreamrapidcom_unknown", Property.NULL);
                    tempUnavailableHoster(acc, dl, 60 * 60 * 1000l);
                }
            }
            br.submitForm(dlForm);
            dllink = br.getRegex("\"(/vip/files/[^<>\"]*?)\"").getMatch(0);
        } catch (final BrowserException e) {
            int timesFailed = dl.getIntegerProperty("timesfailedextreamrapidcom_browserexception", 0);
            if (timesFailed <= 2) {
                timesFailed++;
                dl.setProperty("timesfailedextreamrapidcom_browserexception", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
            } else {
                dl.setProperty("timesfailedextreamrapidcom_browserexception", Property.NULL);
                tempUnavailableHoster(acc, dl, 60 * 60 * 1000l);
            }
        }
        if (dllink != null) dllink = COOKIE_HOST + dllink;
        return dllink;
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