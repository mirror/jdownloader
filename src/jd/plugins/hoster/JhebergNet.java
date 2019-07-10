//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "jheberg.net" }, urls = { "" })
public class JhebergNet extends antiDDoSForHost {
    private static final String          DOMAIN            = "https://jheberg.net/";
    private static final String          HOST              = "https://www.jheberg.net/";
    private static final String          NICE_HOST         = "jheberg.net";
    private static final String          NICE_HOSTproperty = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static MultiHosterManagement mhm               = new MultiHosterManagement("jheberg.net");

    public JhebergNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://jheberg.net/");
    }

    @Override
    public String getAGBLink() {
        return "https://jheberg.net/cgv/";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept", "application/json");
        br.setCookie(DOMAIN, "jlanguage", "en-US");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return account != null;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        if (dllink == null) {
            mhm.handleErrorGeneric(account, link, "dllink_null", 50, 5 * 60 * 1000l);
        }
        /* We want to follow redirects in final stage */
        br.setFollowRedirects(true);
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (dl.getConnection().isContentDisposition() || dl.getConnection().getLongContentLength() > 1024 * 1024l) {
            dl.startDownload();
        } else {
            try {
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.containsHTML("Download not available at the moment")) {
                mhm.handleErrorGeneric(account, link, "download_not_available_at_the_moment", 50, 5 * 60 * 1000l);
            }
            mhm.handleErrorGeneric(account, link, "unknowndlerror", 50, 5 * 60 * 1000l);
        }
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        mhm.runCheck(account, link);
        this.login(account);
        String dllink = null;
        if (false) {
            // reuse doesn't work reliable for me.
            dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        }
        if (dllink == null) {
            /* request Download */
            this.br.setFollowRedirects(true);
            getPage("https://dashboard.jheberg.net/downloader");
            final String token = getToken(br);
            if (token == null) {
                mhm.handleErrorGeneric(account, link, "token_null", 50, 5 * 60 * 1000l);
            }
            final Request downloader = br.createPostRequest("https://dashboard.jheberg.net/downloader", "csrfmiddlewaretoken=" + token + "&urls[]=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            downloader.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            sendRequest(downloader);
            final HashMap<String, Object> map = JSonStorage.restoreFromString(downloader.getHtmlCode(), TypeRef.HASHMAP);
            final List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("urls");
            if (list == null || list.size() != 1) {
                mhm.handleErrorGeneric(account, link, "list_null", 50, 5 * 60 * 1000l);
            }
            final Map<String, Object> url = list.get(0);
            final Long status = JavaScriptEngineFactory.toLong(url.get("status"), -1);
            if (status.longValue() == 409) {
                // not yet supported
                account.getAccountInfo().removeMultiHostSupport(link.getHost());
                throw new PluginException(LinkStatus.ERROR_RETRY, "Hoster is not yet supported");
            } else if (status.longValue() == 200 && url.containsKey("id")) {
                final String id = (String) url.get("id");
                final Request backend = br.createGetRequest("https://api.jheberg.net/backend?type=DOWNLOADER");
                sendRequest(backend);
                final List<Object> servers = JSonStorage.restoreFromString(backend.getHtmlCode(), TypeRef.LIST);
                final Map<String, Object> server = (Map<String, Object>) servers.get(new Random().nextInt(servers.size()));
                dllink = server.get("url") + "download/" + id;
            } else {
                mhm.handleErrorGeneric(account, link, "unknown_serverside_download_status", 50, 5 * 60 * 1000l);
            }
        }
        handleDL(account, link, dllink);
    }

    protected String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = openAntiDDoSRequestConnection(br2, br2.createGetRequest(dllink));
                if (con.isOK() && (con.isContentDisposition() || con.getLongContentLength() > 1024 * 1024l)) {
                    return dllink;
                } else {
                    downloadLink.removeProperty(property);
                    return null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.removeProperty(property);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account);
        final Request me = br.createGetRequest("https://api.jheberg.net/me");
        me.getHeaders().put("X-Auth-Token", br.getHostCookie("sessionid"));
        me.getHeaders().put("Origin", "https://dashboard.jheberg.net");
        sendRequest(me);
        final Map<String, Object> map = JSonStorage.restoreFromString(me.getHtmlCode(), TypeRef.HASHMAP);
        final Map<String, Object> subscription = (Map<String, Object>) map.get("subscription");
        final List<Map<String, Object>> current = (List<Map<String, Object>>) subscription.get("current");
        Long expireDate = null;
        for (Map<String, Object> entry : current) {
            final String expiration = (String) entry.get("expiration");
            if (expiration != null) {
                final long expire = TimeFormatter.getMilliSeconds(expiration, "yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SS", Locale.ENGLISH);
                if (expireDate == null || expire > expireDate) {
                    expireDate = expire;
                }
            }
        }
        if (expireDate != null) {
            ai.setValidUntil(expireDate.longValue());
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(20);
            account.setConcurrentUsePossible(true);
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(20);
        }
        final Request hoster = br.createGetRequest("https://api.jheberg.net/hoster");
        hoster.getHeaders().put("Origin", "https://dashboard.jheberg.net");
        sendRequest(hoster);
        final List<Object> list = JSonStorage.restoreFromString(hoster.getHtmlCode(), TypeRef.LIST);
        List<String> supportedHosts = new ArrayList<String>();
        final Map<String, String> mapping = new HashMap<String, String>();
        mapping.put("1fichier", "1fichier.com");
        mapping.put("Turbobit", "turbobit.net");
        mapping.put("Openload", "openload.co");
        mapping.put("Rapidgator", "rapidgator.net");
        mapping.put("Mega", "mega.co.nz");
        mapping.put("Uploaded", "uploaded.to");
        mapping.put("Nitroflare", "nitroflare.com");
        mapping.put("Filefactory", "filefactory.com");
        mapping.put("Uptobox", "uptobox.com");
        mapping.put("Uploadboy", "uploadboy.com");
        mapping.put("Filesupload", "filesupload.org");
        mapping.put("Clicknupload", "clicknupload.org");
        mapping.put("Filerio", "filerio.com");
        mapping.put("2giga", "2giga.link");
        mapping.put("Filecloud", "filecloud.io");
        mapping.put("Bdupload", "bdupload.info");
        mapping.put("Nofile", "nofile.io");
        mapping.put("Uppit", "uppit.com");
        mapping.put("Indishare", "indishare.me");
        for (final Object entry : list) {
            final Map<String, Object> host = (Map<String, Object>) entry;
            final String name = (String) host.get("name");
            final String mapped = mapping.get(name);
            if (mapped != null) {
                supportedHosts.add(mapped);
            } else {
                supportedHosts.add(name);
            }
        }
        supportedHosts = ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void login(final Account account) throws Exception {
        synchronized (account) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                this.br = newBrowser();
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean freshLogin = true;
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    getPage("https://dashboard.jheberg.net/");
                    if (br.getHostCookie("sessionid", Cookies.NOTDELETEDPATTERN) == null) {
                        freshLogin = true;
                    } else if (!br.containsHTML("/signout")) {
                        freshLogin = true;
                    } else {
                        freshLogin = false;
                    }
                }
                if (freshLogin) {
                    getPage(HOST + "signin");
                    final String token = getToken(br);
                    if (token == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    postPage(HOST + "signin", "csrfmiddlewaretoken=" + token + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember_me=on");
                    if (br.getHostCookie("sessionid", Cookies.NOTDELETEDPATTERN) == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if (!br.containsHTML("/signout")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Save cookies */
                account.saveCookies(br.getCookies(getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private String getToken(Browser br) {
        return br.getRegex("name\\s*=\\s*\\'csrfmiddlewaretoken\\'\\s*value\\s*=\\s*\\'([^<>\"]*?)\\'").getMatch(0);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}