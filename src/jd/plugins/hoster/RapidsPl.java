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
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.UnavailableHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapids.pl" }, urls = { "" })
public class RapidsPl extends PluginForHost {

    private static HashMap<Account, HashMap<String, UnavailableHost>> hostUnavailableMap = new HashMap<Account, HashMap<String, UnavailableHost>>();
    private static final String                                       NOCHUNKS           = "NOCHUNKS";

    private static final String                                       NICE_HOST          = "rapids.pl";
    private static final String                                       NICE_HOSTproperty  = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                                       COOKIE_HOST        = "http://" + NICE_HOST;

    private static Object                                             LOCK               = new Object();
    private static boolean                                            pluginloaded       = false;

    public RapidsPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://rapids.pl/doladuj");
    }

    @Override
    public String getAGBLink() {
        return "http://rapids.pl/pomoc/regulamin";
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        return br;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ac = new AccountInfo();
        try {
            login(account, true);
            String apikey = account.getStringProperty("apikey", null);
            if (apikey == null) {
                br.getPage("/profil/api");
                // 64-bit key (changed 08.08.2016)
                apikey = br.getRegex("<strong>Klucz:\\s*([a-z0-9]{64})\\s*<").getMatch(0);
                if (apikey == null) {
                    // 32 bit key (old)
                    apikey = br.getRegex("<strong>Klucz:\\s*([a-z0-9]{32})\\s*<").getMatch(0);
                }
                if (apikey == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    account.setProperty("apikey", apikey);
                }
            }
            // check if account is valid
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
            ac.setStatus("Premium Account");
            br.getPage("/");
            final String availableTraffic = br.getRegex("Pozostały transfer: <strong>([^<>\"]*?)</strong>").getMatch(0);
            if (availableTraffic == null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                ac.setTrafficLeft(SizeFormatter.getSize(availableTraffic.replaceAll("\\s*", "")));
            }
            // now let's get a list of all supported hosts:
            final ArrayList<String> supportedHosts = new ArrayList<String>();
            final String[][] hostList = { { "uploaded", "uploaded.to", "uploaded.net", "ul.to" }, { "freakshare", "freakshare.com" }, { "turbobit", "turbobit.net" }, { "depositfiles", "depositfiles.com" }, { "filefactory", "filefactory.com" }, { "redtube", "redtube.com" }, { "tube8", "tube8.com" }, { "wrzuta", "wrzuta.pl" }, { "rapidgator", "rapidgator.net" }, { "crocko", "crocko.com" }, { "hitfile", "hitfile.net" }, { "mediafire", "mediafire.com" }, { "shareonline", "share-online.biz" }, { "hellupload", "hellupload.com" }, { "fastshare", "fastshare.cz" }, { "egofiles", "egofiles.com" }, { "ultramegabit", "ultramegabit.com" }, { "lumfile", "lumfile.com" }, { "catshare", "catshare.net" }, { "filesmonster", "filesmonster.com" }, { "fileparadox", "fileparadox.in" }, { "novafile", "novafile.com" }, { "depfile", "depfile.com" }, { "4shared", "4shared.com" },
                    { "keep2share", "keep2share.cc" }, { "datafile", "datafile.com" }, { "nitroflare", "nitroflare.com" } };
            for (final String hostSet[] : hostList) {
                if (br.containsHTML("/services/" + hostSet[0] + "\\.big")) {
                    for (int i = 1; i <= hostSet.length - 1; i++) {
                        final String originalDomain = hostSet[i];
                        supportedHosts.add(originalDomain);
                    }
                }
            }
            ac.setMultiHostSupport(this, supportedHosts);
        } catch (PluginException e) {
            account.removeProperty("apikey");
            throw e;
        }
        return ac;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {

        synchronized (hostUnavailableMap) {
            HashMap<String, UnavailableHost> unavailableMap = hostUnavailableMap.get(null);
            UnavailableHost nue = unavailableMap != null ? unavailableMap.get(link.getHost()) : null;
            if (nue != null) {
                final Long lastUnavailable = nue.getErrorTimeout();
                final String errorReason = nue.getErrorReason();
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable for this multihoster: " + errorReason != null ? errorReason : "via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(null);
                    }
                }
            }
            unavailableMap = hostUnavailableMap.get(account);
            nue = unavailableMap != null ? unavailableMap.get(link.getHost()) : null;
            if (nue != null) {
                final Long lastUnavailable = nue.getErrorTimeout();
                final String errorReason = nue.getErrorReason();
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable for this account: " + errorReason != null ? errorReason : "via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }

        this.br = newBrowser();
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "finallink");
        if (dllink == null) {
            br.postPage("https://rapids.pl/api/check", "key=" + account.getStringProperty("apikey", null) + "&link=" + Encoding.urlEncode(link.getDownloadURL()));
            handleAPIErrors(account, link);
            dllink = PluginJSonUtils.getJsonValue(br, "dlUrl");
            showMessage(link, "Phase 1/2: Generating final downloadlink");
            if (dllink == null) {
                handleErrorRetries(account, link, "dllink null", 5, 10 * 60 * 1000l);
                // logger.info(NICE_HOST + ": Final link is null -> Plugin is broken");
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        showMessage(link, "Phase 2/2: Download begins!");
        int maxChunks = 0;
        if (link.getBooleanProperty(RapidsPl.NOCHUNKS, false)) {
            maxChunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrorRetries(account, link, "unknown error", 5, 20 * 60 * 1000l);
        }
        link.setProperty(NICE_HOSTproperty + "finallink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
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
        if ("Link nie został rozpoznany!".equals(PluginJSonUtils.getJsonValue(br, "message"))) {
            tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l, "Link nie został rozpoznany");
        } else if ("Brak dostępu do API".equals(PluginJSonUtils.getJsonValue(br, "error"))) {
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

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
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
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
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
                br.postPage("https://rapids.pl/konto/loguj", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
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
                account.removeProperty("apikey");
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
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
    private void handleErrorRetries(final Account account, final DownloadLink downloadlink, final String error, final int maxRetries, final long disableTime) throws PluginException {
        int timesFailed = downloadlink.getIntegerProperty(NICE_HOSTproperty + "-failedtimes_" + error, 0);
        if (timesFailed <= maxRetries) {
            logger.info("Retrying -> " + error);
            timesFailed++;
            downloadlink.setProperty(NICE_HOSTproperty + "-failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            downloadlink.setProperty(NICE_HOSTproperty + "-failedtimes_" + error, Property.NULL);
            logger.info("Disabling current host -> " + error);
            tempUnavailableHoster(account, downloadlink, disableTime, error);
        }
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout, final String reason) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }

        final UnavailableHost nue = new UnavailableHost(System.currentTimeMillis() + timeout, reason);

        synchronized (hostUnavailableMap) {
            HashMap<String, UnavailableHost> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, UnavailableHost>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait 'long timeout' to retry this host */
            unavailableMap.put(downloadLink.getHost(), nue);
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}