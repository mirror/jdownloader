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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "xhamster.com" }, urls = { "https?://(?:www\\.)?(?:[a-z]{2}\\.)?(?:m\\.xhamster\\.com/(?:preview|movies)/\\d+(?:/[^/]+\\.html)?|xhamster\\.(?:com|xxx)/(x?embed\\.php\\?video=\\d+|movies/[0-9]+/[^/]+\\.html|videos/[\\w\\-]+-\\d+))" })
public class XHamsterCom extends PluginForHost {
    public XHamsterCom(PluginWrapper wrapper) {
        super(wrapper);
        // Actually only free accounts are supported
        this.enablePremium("http://xhamsterpremiumpass.com/");
        setConfigElements();
    }

    /* DEV NOTES */
    /* Porn_plugin */
    private static final String   ALLOW_MULTIHOST_USAGE           = "ALLOW_MULTIHOST_USAGE";
    private static final boolean  default_allow_multihoster_usage = false;
    private static final String   HTML_PASSWORD_PROTECTED         = "id='videoPass'";
    private static final String   HTML_PAID_VIDEO                 = "class=\"buy_tips\"|<tipt>This video is paid</tipt>";
    private static final String   DOMAIN_CURRENT                  = "xhamster.com";
    final String                  SELECTED_VIDEO_FORMAT           = "SELECTED_VIDEO_FORMAT";
    /* The list of qualities/formats displayed to the user */
    private static final String[] FORMATS                         = new String[] { "Best available", "240p", "480p", "720p", "960p", "1080p", "1440p" };
    private boolean               friendsOnly                     = false;

    private void setConfigElements() {
        String user_text;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            user_text = "Erlaube den Download von Links dieses Anbieters über Multihoster (nicht empfohlen)?\r\n<html><b>Kann die Anonymität erhöhen, aber auch die Fehleranfälligkeit!</b>\r\nAktualisiere deine(n) Multihoster Account(s) nach dem Aktivieren dieser Einstellung um diesen Hoster in der Liste der unterstützten Hoster deines/r Multihoster Accounts zu sehen (sofern diese/r ihn unterstützen).</html>";
        } else {
            user_text = "Allow links of this host to be downloaded via multihosters (not recommended)?\r\n<html><b>This might improve anonymity but perhaps also increase error susceptibility!</b>\r\nRefresh your multihoster account(s) after activating this setting to see this host in the list of the supported hosts of your multihost account(s) (in case this host is supported by your used multihost(s)).</html>";
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MULTIHOST_USAGE, user_text).setDefaultValue(default_allow_multihoster_usage));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SELECTED_VIDEO_FORMAT, FORMATS, "Preferred Format").setDefaultValue(0));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "Filename_id", "Choose file name + id?").setDefaultValue(false));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        if (this.getPluginConfig().getBooleanProperty(ALLOW_MULTIHOST_USAGE, default_allow_multihoster_usage)) {
            return true;
        } else {
            return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        }
    }

    @Override
    public String getAGBLink() {
        return "http://xhamster.com/terms.php";
    }

    private static final String TYPE_MOBILE = "(?i).+m\\.xhamster\\.com/[^/]+/(\\d+)(?:/.+\\.html)?$";
    private static final String TYPE_EMBED  = "(?i)^https?://(?:www\\.)?xhamster\\.(?:com|xxx)/x?embed\\.php\\?video=\\d+$";
    private static final String NORESUME    = "NORESUME";
    private static Object       ctrlLock    = new Object();
    private final String        recaptchav2 = "<div class=\"text\">In order to watch this video please prove you are a human\\.\\s*<br> Click on checkbox\\.</div>";
    private String              dllink      = null;
    private String              vq          = null;

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("://(www\\.)?([a-z]{2}\\.)?", "://"));
        if (link.getDownloadURL().matches(TYPE_MOBILE) || link.getDownloadURL().matches(TYPE_EMBED)) {
            link.setUrlDownload("http://xhamster.com/movies/" + getFID(link) + "/" + System.currentTimeMillis() + new Random().nextInt(10000) + ".html");
        } else {
            final String thisdomain = new Regex(link.getDownloadURL(), "https?://(?:www\\.)?([^/]+)/.+").getMatch(0);
            link.getDownloadURL().replace(thisdomain, DOMAIN_CURRENT);
        }
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        String fid;
        if (dl.getDownloadURL().matches(TYPE_EMBED)) {
            fid = new Regex(dl.getDownloadURL(), "(\\d+)").getMatch(0);
        } else if (dl.getDownloadURL().matches(TYPE_MOBILE)) {
            fid = new Regex(dl.getDownloadURL(), TYPE_MOBILE).getMatch(0);
        } else {
            fid = new Regex(dl.getDownloadURL(), "movies/(\\d+)/").getMatch(0);
            if (fid == null) {
                fid = new Regex(dl.getDownloadURL(), "videos/[\\w\\-]+-(\\d+)").getMatch(0);
            }
        }
        return fid;
    }

    /**
     * NOTE: They also have .mp4 version of the videos in the html code -> For mobile devices Those are a bit smaller in size
     */
    @SuppressWarnings("deprecation")
    public String getDllink() throws IOException, PluginException {
        final SubConfiguration cfg = getPluginConfig();
        final int selected_format = cfg.getIntegerProperty(SELECTED_VIDEO_FORMAT, 0);
        final List<String> qualities = new ArrayList<String>();
        switch (selected_format) {
        case 1:
            qualities.add("240p");
            break;
        case 2:
            qualities.add("480p");
            break;
        case 3:
            qualities.add("720p");
            break;
        case 4:
            qualities.add("960p");
            break;
        case 5:
            qualities.add("1080p");
            break;
        case 6:
            qualities.add("1440p");
            break;
        default:
            qualities.add("1440p");
            qualities.add("1080p");
            qualities.add("960p");
            qualities.add("720p");
            qualities.add("480p");
            qualities.add("240p");
            break;
        }
        final String newPlayer = br.getRegex("videoUrls\":\"(\\{.*?\\]\\})").getMatch(0);
        if (newPlayer != null) {
            // new player
            final Map<String, Object> map = JSonStorage.restoreFromString(JSonStorage.restoreFromString("\"" + newPlayer + "\"", TypeRef.STRING), TypeRef.HASHMAP);
            if (map != null) {
                for (final String quality : qualities) {
                    final Object list = map.get(quality);
                    if (list != null && list instanceof List) {
                        final List<String> urls = (List<String>) list;
                        if (urls.size() > 0) {
                            vq = quality;
                            return urls.get(0);
                        }
                    }
                }
            }
        }
        for (final String quality : qualities) {
            // old player
            final String urls[] = br.getRegex(quality + "\"\\s*:\\s*(\"https?:[^\"]+\")").getColumn(0);
            if (urls != null && urls.length > 0) {
                String best = null;
                for (String url : urls) {
                    url = JSonStorage.restoreFromString(url, TypeRef.STRING);
                    if (best == null || StringUtils.containsIgnoreCase(url, ".mp4")) {
                        best = url;
                    }
                }
                if (best != null) {
                    vq = quality;
                    return best;
                }
            }
        }
        for (final String quality : qualities) {
            // 3d videos
            final String urls[] = br.getRegex(quality + "\"\\s*,\\s*\"url\"\\s*:\\s*(\"https?:[^\"]+\")").getColumn(0);
            if (urls != null && urls.length > 0) {
                String best = null;
                for (String url : urls) {
                    url = JSonStorage.restoreFromString(url, TypeRef.STRING);
                    if (best == null || StringUtils.containsIgnoreCase(url, ".mp4")) {
                        best = url;
                    }
                }
                if (best != null) {
                    vq = quality;
                    return best;
                }
            }
        }
        // is the rest still in use/required?
        String dllink = null;
        logger.info("Video quality selection failed.");
        int urlmodeint = 0;
        final String urlmode = br.getRegex("url_mode=(\\d+)").getMatch(0);
        if (urlmode != null) {
            urlmodeint = Integer.parseInt(urlmode);
        }
        if (urlmodeint == 1) {
            /* Example-ID: 1815274, 1980180 */
            final Regex secondway = br.getRegex("\\&srv=(https?[A-Za-z0-9%\\.]+\\.xhcdn\\.com)\\&file=([^<>\"]*?)\\&");
            String server = br.getRegex("\\'srv\\': \\'(.*?)\\'").getMatch(0);
            if (server == null) {
                server = secondway.getMatch(0);
            }
            String file = br.getRegex("\\'file\\': \\'(.*?)\\'").getMatch(0);
            if (file == null) {
                file = secondway.getMatch(1);
            }
            if (server == null || file == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (file.startsWith("http")) {
                // Examplelink (ID): 968106
                dllink = file;
            } else {
                // Examplelink (ID): 986043
                dllink = server + "/key=" + file;
            }
        } else {
            /* E.g. url_mode == 3 */
            /* Example-ID: 685813 */
            String flashvars = br.getRegex("flashvars: \"([^<>\"]*?)\"").getMatch(0);
            dllink = br.getRegex("\"(https?://\\d+\\.xhcdn\\.com/key=[^<>\"]*?)\" class=\"mp4Thumb\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://\\d+\\.xhcdn\\.com/key=[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://\\d+\\.xhcdn\\.com/key=[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("flashvars.*?file=(http%3.*?)&").getMatch(0);
            }
            if (dllink == null && flashvars != null) {
                /* E.g. 4753816 */
                flashvars = Encoding.htmlDecode(flashvars);
                flashvars = flashvars.replace("\\", "");
                final String[] qualities2 = { "1080p", "720p", "480p", "360p", "240p" };
                for (final String quality : qualities2) {
                    dllink = new Regex(flashvars, "\"" + quality + "\":\\[\"(http[^<>\"]*?)\"\\]").getMatch(0);
                    if (dllink != null) {
                        break;
                    }
                }
            }
        }
        if (dllink == null) {
            // urlmode fails, eg: 1099006
            dllink = br.getRegex("video\\s*:\\s*\\{[^\\}]+file\\s*:\\s*('|\")(.*?)\\1").getMatch(1);
        }
        dllink = Encoding.htmlDecode(dllink);
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        return requestFileInformation(downloadLink, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink downloadLink, final boolean isDownload) throws Exception {
        synchronized (ctrlLock) {
            friendsOnly = false;
            downloadLink.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            br.setFollowRedirects(true);
            prepBr();
            // quick fix to force old player
            br.setCookie(MAINPAGE, "playerVer", "old");
            String filename = null;
            final Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa != null) {
                login(aa, false);
            }
            br.getPage(downloadLink.getDownloadURL());
            final int responsecode = br.getRequest().getHttpConnection().getResponseCode();
            if (responsecode == 423) {
                if (br.containsHTML(">\\s*This (gallery|video) is visible (for|to) <")) {
                    friendsOnly = true;
                    return AvailableStatus.TRUE;
                }
                if (br.containsHTML("Conversion of video processing")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Conversion of video processing", 60 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (responsecode == 404 || responsecode == 410 || responsecode == 452) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            // embeded correction --> Usually not needed
            if (downloadLink.getDownloadURL().contains(".com/xembed.php")) {
                String realpage = br.getRegex("main_url=(http[^\\&]+)").getMatch(0);
                if (realpage != null) {
                    downloadLink.setUrlDownload(Encoding.htmlDecode(realpage));
                    br.getPage(downloadLink.getDownloadURL());
                }
            }
            // recaptchav2 here, don't trigger captcha until download....
            if (br.containsHTML(recaptchav2)) {
                if (!isDownload) {
                    return AvailableStatus.UNCHECKABLE;
                } else {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    final Browser captcha = br.cloneBrowser();
                    captcha.getHeaders().put("Accept", "*/*");
                    captcha.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    captcha.getPage("/captcha?g-recaptcha-response=" + recaptchaV2Response);
                    br.getPage(br.getURL());
                }
            }
            if (br.containsHTML("(403 Forbidden|>This video was deleted<)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = getSiteTitle();
            final String onlyfor = br.getRegex(">([^<>\"]*?)</a>\\'s friends only</div>").getMatch(0);
            if (onlyfor != null) {
                downloadLink.getLinkStatus().setStatusText("Only downloadable for friends of " + onlyfor);
                downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "movies/[0-9]+/(.*?)\\.html").getMatch(0) + ".mp4");
                return AvailableStatus.TRUE;
            } else if (br.containsHTML(HTML_PASSWORD_PROTECTED)) {
                downloadLink.getLinkStatus().setStatusText("This video is password protected");
                return AvailableStatus.TRUE;
            }
            if (downloadLink.getFinalFileName() == null || dllink == null) {
                filename = getFilename(downloadLink);
                if (filename == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                downloadLink.setFinalFileName(filename);
                if (br.containsHTML(HTML_PAID_VIDEO)) {
                    downloadLink.getLinkStatus().setStatusText("To download, you have to buy this video");
                    return AvailableStatus.TRUE;
                } else if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            if (downloadLink.getDownloadSize() <= 0) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(dllink);
                    if (!con.getContentType().contains("html")) {
                        downloadLink.setDownloadSize(con.getLongContentLength());
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
            return AvailableStatus.TRUE;
        }
    }

    private String getSiteTitle() {
        final String title = br.getRegex("<title.*?>([^<>\"]*?)\\s*\\-\\s*xHamster(\\.com)?</title>").getMatch(0);
        return title;
    }

    private String getFilename(final DownloadLink link) throws PluginException, IOException {
        final String fid = getFID(link);
        String filename = br.getRegex("<h1 itemprop=\"name\">(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title.*?>([^<>\"]*?), Free Porn: xHamster</title>").getMatch(0);
        }
        if (filename == null) {
            filename = getSiteTitle();
        }
        if (filename == null) {
            /* Fallback to URL filename - first try to get nice name from URL. */
            filename = new Regex(br.getURL(), "/(?:videos|movies)/(.+)\\d+$").getMatch(0);
            if (filename == null) {
                /* Last chance */
                filename = new Regex(br.getURL(), "https?://[^/]+/(.+)").getMatch(0);
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = getDllink();
        String ext;
        if (dllink != null) {
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        } else {
            ext = ".flv";
        }
        if (vq != null) {
            filename = Encoding.htmlDecode(filename.trim() + "_" + vq);
        } else {
            filename = Encoding.htmlDecode(filename.trim());
        }
        if (getPluginConfig().getBooleanProperty("Filename_id", true)) {
            filename += "_" + fid;
        } else {
            filename = fid + "_" + filename;
        }
        filename += ext;
        return filename;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink, true);
        doFree(downloadLink);
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink downloadLink) throws Exception {
        if (friendsOnly) {
            throw new AccountRequiredException("You need to be friends with uploader");
        }
        // Access the page again to get a new direct link because by checking the availability the first linkisn't valid anymore
        String passCode = downloadLink.getStringProperty("pass", null);
        br.getPage(downloadLink.getDownloadURL());
        final String onlyfor = br.getRegex(">([^<>\"]*?)</a>\\'s friends only</div>").getMatch(0);
        if (onlyfor != null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (br.containsHTML(HTML_PASSWORD_PROTECTED)) {
            if (passCode == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            }
            br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode));
            if (br.containsHTML(HTML_PASSWORD_PROTECTED)) {
                downloadLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            downloadLink.setFinalFileName(getFilename(downloadLink));
        } else if (br.containsHTML(HTML_PAID_VIDEO)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        final String dllink = getDllink();
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean resume = true;
        if (downloadLink.getBooleanProperty(NORESUME, false)) {
            resume = false;
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resume, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Response code 416 --> Handling it");
                if (downloadLink.getBooleanProperty(NORESUME, false)) {
                    downloadLink.setProperty(NORESUME, Boolean.valueOf(false));
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 30 * 60 * 1000l);
                }
                downloadLink.setProperty(NORESUME, Boolean.valueOf(true));
                downloadLink.setChunksProgress(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error 416");
            }
            br.followConnection();
            if (br.containsHTML(">Video not found<")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            logger.info("xhamster.com: Unknown error -> Retrying!");
            int timesFailed = downloadLink.getIntegerProperty("timesfailedxhamstercom_unknown", 0);
            downloadLink.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                downloadLink.setProperty("timesfailedxhamstercom_unknown", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                downloadLink.setProperty("timesfailedxhamstercom_unknown", Property.NULL);
                logger.info("xhamster.com: Unknown error -> Plugin is broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        downloadLink.setProperty("pass", passCode);
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://xhamster.com";
    private static Object       LOCK     = new Object();

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            // used in finally to restore browser redirect status.
            final boolean frd = br.isFollowingRedirects();
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBr();
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(true);
                if (true) {
                    /* 2017-11-23: Broken at the moment */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getPage("https://de.xhamster.com/login");
                Browser br = this.br.cloneBrowser();
                final long now = System.currentTimeMillis();
                final String xsid;
                {
                    final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
                    final ScriptEngine engine = manager.getEngineByName("javascript");
                    engine.eval("res1 = Math.floor(Math.random()*100000000).toString(16);");
                    engine.eval("now = " + now);
                    engine.eval("res2 = now.toString(16).substring(0,8);");
                    xsid = (String) engine.get("res1") + ":" + (String) engine.get("res2");
                }
                // br.setCookie(MAINPAGE, "xsid", xsid);
                // now some other fingerprint set via js, again cookie and login form
                // final String fingerprint = JDHash.getMD5(System.getProperty("user.timezone") + System.getProperty("os.name"));
                // br.setCookie(MAINPAGE, "fingerprint", fingerprint);
                // login.put("_", now + "");
                String postData = "[{\"name\":\"authorizedUserModelFetch\",\"requestData\":{\"$id\":\"<TODO_FIXME>\",\"id\":null,\"trusted\":true,\"username\":\"" + account.getUser() + "\",\"password\":\"" + account.getPass() + "\",\"remember\":1,\"redirectURL\":null,\"captcha\":\"true\"}}]";
                br.postPageRaw("/x-api", postData);
                // 2017-11-23: TODO: Check/fix captcha login
                if (br.containsHTML("\"errors\":\"invalid_captcha\"") && br.containsHTML("\\$\\('#loginCaptchaRow'\\)\\.show\\(\\)")) {
                    final Form login = br.getFormbyProperty("name", "loginForm");
                    // samtimes not found loginForm. br.getFormbyAction("/login.php")
                    if (login == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    // they use recaptchav2 now.
                    if (this.getDownloadLink() == null) {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", "xhamster.com", "http://xhamster.com", true);
                        this.setDownloadLink(dummyLink);
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    br = this.br.cloneBrowser();
                    login.put("_", System.currentTimeMillis() + "");
                    br.getHeaders().put("Accept", "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript, */*; q=0.01");
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    login.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    br.submitForm(login);
                }
                if (br.getCookie(MAINPAGE, "UID") == null || br.getCookie(MAINPAGE, "_id") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            } finally {
                br.setFollowRedirects(frd);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        /*
         * logic to manipulate full login. Useful for sites that show captcha when you login too many times in a given time period. Or sites
         * that present captcha to users all the time!
         */
        if (account.getCookiesTimeStamp("") != 0 && (System.currentTimeMillis() - 6 * 3480000l <= account.getCookiesTimeStamp(""))) {
            login(account, false);
            // because we have used cached login, we should verify that the cookie is still valid...
            br.getPage(MAINPAGE);
            if (br.getCookie(MAINPAGE, "UID") == null || br.getCookie(MAINPAGE, "_id") == null) {
                // we should assume cookie is invalid, and perform a full login!
                br = new Browser();
                login(account, true);
            }
        } else {
            login(account, true);
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Free Account");
        account.setProperty("free", true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, true);
        login(account, false);
        doFree(link);
    }

    private void prepBr() {
        br.setCookie(MAINPAGE, "lang", "en");
        br.setAllowedResponseCodes(new int[] { 410, 423, 452 });
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}