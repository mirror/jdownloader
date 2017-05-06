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

import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gotporn.com", "hardsextube.com" }, urls = { "https?://(?:www\\.)?hardsextube\\.com/(video/)?(video|embed)/?-?\\d+|https?://(?:www\\.)?gotporn\\.com/[a-z0-9\\-]+/video\\-\\d+|https?://(?:www\\.)?gotporn\\.com/video/\\d+", "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32424" })
public class HardSexTubeCom extends antiDDoSForHost {
    public HardSexTubeCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.gotporn.com/");
    }

    @Override
    public String rewriteHost(String host) {
        if ("hardsextube.com".equals(getHost())) {
            if (host == null || "hardsextube.com".equals(host)) {
                return "gotporn.com";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return "http://www.gotporn.com/register/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        if (link.getDownloadURL().contains("gotporn")) {
            link.setUrlDownload("http://www.gotporn.com/video/video-" + new Regex(link.getDownloadURL(), "(\\d+)/?$").getMatch(0));
        }
    }

    private static final String NORESUME                           = "NORESUME";
    private boolean             only_downloadable_via_free_account = false;
    private final boolean       enable_site                        = true;
    private final boolean       enable_embed                       = false;
    private final boolean       enable_embed_2                     = false;
    private final boolean       enable_mobile                      = false;
    private String              dllink                             = null;

    /*
     * TODO: If we cannot avoid the crypto stuff anymore at some point, simply add account support, then we can use:
     * http://www.hardsextube.com/video/XXXXXX/download
     */
    /* Some older ways to get finallinks */
    // br.getPage("http://vidii.hardsextube.com/video/" + fid + "/confige.xml");
    // final String cdnurl = br.getRegex("\"(/cdnurl\\.php[^<>\"]*?)\"").getMatch(0);
    // br.getPage("http://www.hardsextube.com" + cdnurl);
    // br.getPage("http://www.hardsextube.com/cdnurl.php?eid=" + new Regex(downloadLink.getDownloadURL(), "(\\d+)/$").getMatch(0) +
    // "&start=0");
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        /* Make sure to correct old urls! */
        correctDownloadLink(downloadLink);
        dllink = null;
        final String vid = getVID(downloadLink);
        String ext = null;
        if (!downloadLink.isNameSet()) {
            downloadLink.setName(vid);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(downloadLink.getDownloadURL());
        if (!br.containsHTML("video-player-wrap") || !br.getURL().contains("/video/") || br.getRedirectLocation() != null || br.getHttpConnection().getResponseCode() == 302 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<meta itemprop=\"name\" content=\"([^<>\"]*?)\">").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1 class=\"title-block\">([^<>\"]*?)</h1>").getMatch(0);
        }
        if (filename == null) { // hardsextube 20170505
            filename = br.getRegex("gp-title=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("data-video-id=\"([^<>\"]*?)\"").getMatch(0);
        }
        filename = Encoding.htmlDecode(filename.trim());
        downloadLink.setProperty("plain_title", filename);
        if (enable_site) {
            dllink = this.br.getRegex("\"(http[^<>\"]*?)\" type=\\'video/mp4\\'").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (enable_mobile && dllink == null) {
            this.br = new Browser();
            this.br.setFollowRedirects(true);
            this.br.getHeaders().put("User-Agent", "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile");
            this.br.setCookie("http://hardsextube.com/", "video_quality", "hq");
            getPage("http://m.hardsextube.com/play/" + vid);
            dllink = br.getRegex("\"(https?://[a-z0-9\\-\\.]+\\.hardsextube\\.com/[^<>\"]*?\\.mp4[^<>\"]*?)\"").getMatch(0);
        }
        if (enable_embed && dllink == null) {
            /*
             * Via the embedded video stuff we can sometimes get the final link without having to decrypt anything
             */
            br.setFollowRedirects(false);
            getPage("http://www.hardsextube.com/embed/" + vid + "/");
            final String redirect = br.getRedirectLocation();
            if (redirect == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String name = new Regex(redirect, "\\&flvserver=(http[^<>\"]*?)\\&").getMatch(0);
            final String path = new Regex(redirect, "\\&flv=((?:/|%2F)(?:embed|content)[^<>\"\\&]*?)\\&").getMatch(0);
            if (name == null || path == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.htmlDecode(name + path);
        }
        if (enable_embed_2 && dllink == null) {
            /* Second embed way to find downloadlinks */
            getPage("http://www.hardsextube.com/video/" + vid + "/embedframe");
            dllink = br.getRegex("\"(http://[a-z0-9\\.]+\\.hardsextube\\.com/flvcontent/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                /* This will produce an invalid downloadlink */
                final String name = br.getRegex("\\&flvserver=(http[^<>\"]*?)\\&").getMatch(0);
                final String path = br.getRegex("\\&flv=((?:/|%2F)embed[^<>\"]*?)\\&").getMatch(0);
                if (name == null || path == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = Encoding.htmlDecode(name + path);
                only_downloadable_via_free_account = true;
            }
        }
        ext = getEXT(dllink);
        if (only_downloadable_via_free_account) {
            downloadLink.getLinkStatus().setStatusText("Only downloadable via free account");
            downloadLink.setName(filename + ext);
            return AvailableStatus.TRUE;
        }
        downloadLink.setFinalFileName(filename + ext);
        URLConnectionAdapter con = null;
        dllink = HTMLEntities.unhtmlentities(dllink);
        try {
            con = openAntiDDoSRequestConnection(br, br.createGetRequest(dllink));
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (only_downloadable_via_free_account) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        boolean resume = true;
        if (downloadLink.getBooleanProperty(HardSexTubeCom.NORESUME, false)) {
            logger.info("Resume is disabled for this try");
            resume = false;
            downloadLink.setProperty(HardSexTubeCom.NORESUME, Boolean.valueOf(false));
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 3 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume impossible, disabling it for the next try");
                downloadLink.setChunksProgress(null);
                downloadLink.setProperty(HardSexTubeCom.NORESUME, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://gotporn.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
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
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.postPage("http://www.gotporn.com/login", "remember-me=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "hstauth") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(false);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
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

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        String dllink = this.checkDirectLink(link, "premium_directlink");
        if (dllink == null) {
            br.getPage("http://www.hardsextube.com/video/" + getVID(link) + "/download");
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Final filename might not be set for account_only links --> Do this here */
            final String ext = getEXT(dllink);
            link.setFinalFileName(link.getStringProperty("plain_title", null) + ext);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    private String getVID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "(\\d+)/?$").getMatch(0);
    }

    private String getEXT(final String finallink) {
        String ext = null;
        if (finallink != null) {
            ext = getFileNameExtensionFromString(finallink);
            if (ext == null) {
                ext = ".mp4";
            } else if (ext.contains(".flv") && !ext.matches("\\.[A-Za-z0-9]{2,5}")) {
                ext = ".flv";
            } else if (ext.contains(".mp4") && !ext.matches("\\.[A-Za-z0-9]{2,5}")) {
                ext = ".mp4";
            }
        } else {
            ext = ".mp4";
        }
        return ext;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}