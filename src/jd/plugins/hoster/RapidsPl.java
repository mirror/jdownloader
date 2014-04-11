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
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapids.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class RapidsPl extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(20);
    private static final String                            NOCHUNKS           = "NOCHUNKS";

    private static final String                            NICE_HOST          = "rapids.pl";
    private static final String                            NICE_HOSTproperty  = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            COOKIE_HOST        = "http://" + NICE_HOST;

    private static Object                                  LOCK               = new Object();
    private static boolean                                 pluginloaded       = false;

    public RapidsPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://rapids.pl/doladuj");
    }

    @Override
    public String getAGBLink() {
        return "http://rapids.pl/pomoc/regulamin";
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return maxPrem.get();
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ac = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        String apikey = account.getStringProperty("apikey", null);
        if (apikey == null) {
            br.getPage("http://rapids.pl/profil/api");
            apikey = br.getRegex("<strong>Klucz: ([a-z0-9]{32})<").getMatch(0);
            if (apikey == null) {
                account.setValid(false);
                return ac;
            }
            account.setProperty("apikey", apikey);
        }
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        try {
            maxPrem.set(20);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }
        ac.setStatus("Premium User");
        br.getPage("http://rapids.pl/");
        final String availableTraffic = br.getRegex("Pozostały transfer: <strong>([^<>\"]*?)</strong>").getMatch(0);
        if (availableTraffic == null) {
            account.setValid(false);
            return ac;
        } else {
            ac.setTrafficLeft(SizeFormatter.getSize(availableTraffic.replaceAll("\\s*", "")));
        }
        // now let's get a list of all supported hosts:
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[][] hostList = { { "uploaded", "uploaded.to", "uploaded.net", "ul.to" }, { "netload", "netload.in" }, { "freakshare", "freakshare.com" }, { "turbobit", "turbobit.net" }, { "depositfiles", "depositfiles.com" }, { "filefactory", "filefactory.com" }, { "redtube", "redtube.com" }, { "tube8", "tube8.com" }, { "uploading", "uploading.com" }, { "wrzuta", "wrzuta.pl" }, { "extabit", "extabit.com" }, { "bitshare", "bitshare.com" }, { "filepost", "filepost.com" }, { "rapidgator", "rapidgator.net" }, { "letitbit", "letitbit.net" }, { "crocko", "crocko.com" }, { "megashares", "megashares.com" }, { "hitfile", "hitfile.net" }, { "shareflare", "shareflare.net" }, { "vipfile", "vip-file.com" }, { "mediafire", "mediafire.com" }, { "shareonline", "share-online.biz" }, { "hellupload", "hellupload.com" }, { "fastshare", "fastshare.cz" }, { "egofiles", "egofiles.com" },
                { "putlocker", "putlocker.com" }, { "ultramegabit", "ultramegabit.com" }, { "lumfile", "lumfile.com" }, { "ryushare", "ryushare.com" }, { "luckyshare", "luckyshare.net" }, { "catshare", "catshare.net" }, { "creafile", "creafile.net" }, { "filesmonster", "filesmonster.com" }, { "fileparadox", "fileparadox.in" }, { "novafile", "novafile.com" }, { "depfile", "depfile.com" }, { "4shared", "4shared.com" }, { "keep2share", "keep2share.cc" }, { "5fantastic", "5fantastic.pl" }, { "dizzcloud", "dizzcloud.com" }, { "sharingmaster", "sharingmaster.com" }, { "datafile", "datafile.com" } };
        for (final String hostSet[] : hostList) {
            if (br.containsHTML("/services/" + hostSet[0] + "\\.big")) {
                for (int i = 1; i <= hostSet.length - 1; i++) {
                    final String originalDomain = hostSet[i];
                    supportedHosts.add(originalDomain);
                }
            }
        }

        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts available");
            ac.setProperty("multiHostSupport", supportedHosts);
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

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        this.br = newBrowser();
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "finallink");
        if (dllink == null) {
            br.postPage("http://rapids.pl/api/check", "key=" + acc.getStringProperty("apikey", null) + "&link=" + Encoding.urlEncode(link.getDownloadURL()));
            br.getRequest().setHtmlCode(unescape(br.toString()));
            handleAPIErrors(acc, link);
            dllink = br.getRegex("\"dlUrl\":\"(http[^<>\"]*?)\"").getMatch(0);
            showMessage(link, "Phase 1/2: Generating final downloadlink");
            if (dllink == null) {
                logger.info(NICE_HOST + ": Final link is null");
                int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "failedtimes_dllinknull", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty(NICE_HOSTproperty + "failedtimes_dllinknull", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Final download link not found");
                } else {
                    link.setProperty(NICE_HOSTproperty + "failedtimes_dllinknull", Property.NULL);
                    logger.info(NICE_HOST + ": Final link is null -> Plugin is broken");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dllink = dllink.replace("\\", "");
        }
        showMessage(link, "Phase 2/2: Download begins!");
        int maxChunks = 0;
        if (link.getBooleanProperty(RapidsPl.NOCHUNKS, false)) {
            maxChunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info(NICE_HOST + ": Unknown download error");
            int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "failedtimes_unknowndlerror", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty(NICE_HOSTproperty + "failedtimes_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                link.setProperty(NICE_HOSTproperty + "failedtimes_unknowndlerror", Property.NULL);
                logger.info(NICE_HOST + ": Unknown download error -> Plugin is broken");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(NICE_HOSTproperty + "finallink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(RapidsPl.NOCHUNKS, false) == false) {
                    link.setProperty(RapidsPl.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(RapidsPl.NOCHUNKS, false) == false) {
                link.setProperty(RapidsPl.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private void handleAPIErrors(final Account acc, final DownloadLink dl) throws PluginException {
        if (br.containsHTML("\"message\":\"Link nie został rozpoznany\\!\"")) {
            tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"error\":\"Brak dostępu do API\"")) {
            // Maybe wrong API key
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
            } catch (final Exception e) {
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

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
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

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                newBrowser();
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
                        return;
                    }
                }
                br.setFollowRedirects(true);
                final String lang = System.getProperty("user.language");
                br.postPage("http://rapids.pl/konto/zaloguj", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(COOKIE_HOST, "remember_me") == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) throw new IllegalStateException("youtube plugin not found!");
            pluginloaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}