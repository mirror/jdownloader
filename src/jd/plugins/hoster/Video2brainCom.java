//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "video2brain.com" }, urls = { "https?://(?:www\\.)?video2brain\\.com/(de/tutorial/[a-z0-9\\-]+|en/lessons/[a-z0-9\\-]+|en/videos\\-\\d+\\.htm|fr/tuto/[a-z0-9\\-]+|es/tutorial/[a-z0-9\\-]+)" }, flags = { 2 })
public class Video2brainCom extends PluginForHost {

    public Video2brainCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.video2brain.com/en/support/faq");
    }

    @Override
    public String getAGBLink() {
        return "https://www.video2brain.com/en/imprint";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    /* Connection stuff */
    // private final boolean FREE_RESUME = false;
    // private final int FREE_MAXCHUNKS = 0;
    private final int     FREE_MAXDOWNLOADS    = 20;
    private final boolean RESUME_RTMP          = false;
    private final boolean RESUME_HTTP          = true;
    private final int     MAXCHUNKS_HTTP       = 0;
    private final int     ACCOUNT_MAXDOWNLOADS = 20;

    private boolean       premiumonly          = false;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        premiumonly = false;
        this.setBrowserExclusive();
        this.br = newBrowser(new Browser());
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            this.login(aa);
        }
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().matches("https?://(?:www\\.)?video2brain\\.com/[^/]+/[^/]+/[^/]+")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        boolean set_final_filename = true;
        final String url_language = getUrlLanguage(link);
        final String productid = this.br.getRegex("Video\\.product_id[\t\n\r ]*?=[\t\n\r ]*?(\\d+);").getMatch(0);
        final String videoid = this.br.getRegex("Video\\.active_video_id[\t\n\r ]*?=[\t\n\r ]*?(\\d+);").getMatch(0);
        String title = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        if (title == null) {
            /* Fallback to url filename */
            title = new Regex(this.br.getURL(), "([A-Za-z0-9\\-]+)$").getMatch(0);
        }
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = Encoding.htmlDecode(title.trim());
        String filename = "video2brain_" + url_language;
        if (this.br.containsHTML("class=\"only\\-register\\-player\"") || this.br.containsHTML("class=\"video\\-detail\\-player no\\-access\\-player standard\"")) {
            filename += "_paid_content";
            set_final_filename = false;
            premiumonly = true;
        } else if (this.br.containsHTML("var contentVideo_trailer")) {
            filename += "_trailer";
        } else {
            filename += "_tutorial";
        }
        if (productid != null && videoid != null) {
            filename += "_" + productid + "_" + videoid;
        }
        filename += "_" + title + ".mp4";
        filename = encodeUnicode(filename);
        if (set_final_filename) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleDownload(downloadLink);
    }

    @SuppressWarnings("deprecation")
    public void handleDownload(final DownloadLink downloadLink) throws Exception {
        if (premiumonly) {
            /* This can even happen to paid users! */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        final Browser br2 = newBrowser(new Browser());
        /* User-Agent is not necessarily needed! */
        br2.getHeaders().put("User-Agent", "iPad");
        String html5_http_url = this.br.getRegex("<video src=\\'(http[^<>\"\\']+)\\'").getMatch(0);
        final String access_exp = this.br.getRegex("Video\\.access_exp[\t\n\r ]*?=[\t\n\r ]*?(\\d+);").getMatch(0);
        final String access_hash = this.br.getRegex("Video\\.access_hash[\t\n\r ]*?=[\t\n\r ]*?\"([a-f0-9]+)\";").getMatch(0);
        if (html5_http_url != null && access_exp != null && access_hash != null) {
            /* Let's try to build our http url first - we can still fallback to rtmp if this fails! */
            /* They usually only use these urls for Apple devices. */
            try {
                final String postData = "expire=1&path=" + Encoding.urlEncode(html5_http_url) + "&access_exp=" + access_exp + "&access_hash=" + access_hash;
                br2.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br2.postPage("https://www.video2brain.com/de/custom/modules/cdn/cdn.cfc?method=getSecureTokenJSON", postData);
                final String final_http_url_token = br2.getRegex("\"([^<>\"\\'\\\\]+)").getMatch(0);
                if (final_http_url_token == null) {
                    html5_http_url = null;
                } else {
                    html5_http_url += "?" + final_http_url_token;
                }
            } catch (final Throwable e) {
                html5_http_url = null;
            }
        }

        if (html5_http_url != null) {
            /* Prefer http - quality-wise rtmp and http are the same! */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, html5_http_url, RESUME_HTTP, MAXCHUNKS_HTTP);
            if (dl.getConnection().getContentType().contains("html")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
            }
            dl.startDownload();
        } else {
            /* E.g. https://www.video2brain.com/en/video-info-8581.xml */
            String config_url = this.br.getRegex("configuration:[\t\n\r ]*?\"([^<>\"]*?)\"").getMatch(0);
            final String url_language = getUrlLanguage(downloadLink);
            if (config_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (config_url.startsWith("trailer")) {
                /* Fix trailer xml url */
                config_url = "/" + url_language + "/" + Encoding.unescape(config_url);
            }
            this.br.getPage(config_url);
            final String rtmpurl = this.br.getRegex("<src>(rtmp[^\n]+)").getMatch(0);
            if (rtmpurl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // final String playpath = new Regex(rtmpurl, "((?:flv|mp4):.+)").getMatch(0);
            try {
                dl = new RTMPDownload(this, downloadLink, rtmpurl);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setUrl(rtmpurl);
            // if (playpath != null) {
            // rtmp.setPlayPath(playpath);
            // }
            // rtmp.setApp("");
            rtmp.setFlashVer("WIN 20,0,0,306");
            // rtmp.setSwfUrl("https://www.video2brain.com/en/swf/Video2brainPlayer.swf?v=59");
            rtmp.setResume(RESUME_RTMP);
            ((RTMPDownload) dl).startDownload();
        }
    }

    @SuppressWarnings("deprecation")
    private String getUrlLanguage(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "video2brain\\.com/([^/]+)").getMatch(0);
    }

    private static final String MAINPAGE = "http://video2brain.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("deprecation")
    private void login(final Account account) throws Exception {
        synchronized (LOCK) {
            try {
                this.br = newBrowser(new Browser());
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.getPage("https://www." + this.getHost() + "/de/");
                    if (this.br.containsHTML("user\\-logout\\.htm\"")) {
                        /* Save new cookie timestamp */
                        account.saveCookies(this.br.getCookies(MAINPAGE), "");
                        return;
                    }
                    this.br = newBrowser(new Browser());
                }
                br.postPage("https://www.video2brain.com/de/custom/modules/user/user_ajax.cfc?method=login", "set_cookie=true&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                /* TODO: Maybe make sure this also works for users of other countries! */
                if (br.getCookie(MAINPAGE, "V2B_USER_DE") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                String continue_url = this.br.getRegex("\"url\":\"(https[^<>\"\\']+)\"").getMatch(0);
                if (continue_url != null) {
                    continue_url = continue_url.replace("\\", "");
                } else {
                    /* TODO: Maybe make sure this also works for users of other countries! */
                    continue_url = "/de/login";
                }
                this.br.getPage(continue_url);
                account.saveCookies(this.br.getCookies(MAINPAGE), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private Browser newBrowser(final Browser br) {
        br.setFollowRedirects(true);
        return br;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        if (br.containsHTML("class=\"subscription btn green\"")) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
        } else {
            /* TODO: Add expire date support if possible */
            // final String expire = br.getRegex("").getMatch(0);
            // if (expire == null) {
            // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            // throw new PluginException(LinkStatus.ERROR_PREMIUM,
            // "\r\nDein Account Typ wird bisher noch nicht unterstützt!\r\nBitte melde dich bei unserem Support!",
            // PluginException.VALUE_ID_PREMIUM_DISABLE);
            // } else {
            // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nPlease contact our support!",
            // PluginException.VALUE_ID_PREMIUM_DISABLE);
            // }
            // }
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
        }
        account.setMaxSimultanDownloads(ACCOUNT_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* No need to log in - we're already logged in! */
        // login(account);
        handleDownload(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_MAXDOWNLOADS;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}