//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "video.fc2.com" }, urls = { "https?://(?:video\\.fc2\\.com|xiaojiadianvideo\\.asia|jinniumovie\\.be)/((?:[a-z]{2}/)?(?:a/)?flv2\\.swf\\?i=|(?:[a-z]{2}/)?(?:a/)?content/)\\w+" })
public class VideoFCTwoCom extends PluginForHost {
    public VideoFCTwoCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fc2.com");
        setConfigElements();
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "video.fc2.com", "xiaojiadianvideo.asia", "jinniumovie.be" };
    }

    private String        finalURL              = null;
    private long          finalURLType          = -1;
    private String        trailerURL            = null;
    private boolean       server_issues         = false;
    private final boolean fastLinkCheck_default = true;
    private final String  fastLinkCheck         = "fastLinkCheck";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), fastLinkCheck, "Enable fast linkcheck, doesn't perform filesize checks! Filesize will be updated when download starts.").setDefaultValue(fastLinkCheck_default));
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(?:i=|content/)(.+)").getMatch(0);
    }

    private Browser prepareBrowser(Browser prepBr) {
        if (prepBr == null) {
            prepBr = new Browser();
        }
        prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
        prepBr.setCustomCharset("utf-8");
        return prepBr;
    }

    @Override
    public String getAGBLink() {
        return "http://help.fc2.com/common/tos/en/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 4;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        final boolean subContent = new Regex(link.getDownloadURL(), "/a/content/").matches();
        link.setUrlDownload("http://video.fc2.com/en/" + (subContent ? "a/content/" : "content/") + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)/?$").getMatch(0));
    }
    /*
     * IMPORTANT NOTE: Free (unregistered) Users can watch (&download) videos up to 2 hours in length - if videos are longer, users can only
     * watch the first two hours of them - afterwards they will get this message: http://i.snag.gy/FGl1E.jpg
     */

    private void login(final Account account, final boolean verifyCookies) throws IOException, PluginException, InterruptedException {
        prepareBrowser(br);
        br.setCookiesExclusive(true);
        final Cookies cookies = account.loadCookies("");
        if (cookies != null) {
            this.br.setCookies(this.getHost(), cookies);
            if (!verifyCookies) {
                logger.info("Trust cookies without login");
                return;
            } else {
                br.getPage("https://video.fc2.com/a/");
                if (isLoggedINVideoFC2()) {
                    logger.info("Cookie login successful");
                    return;
                } else {
                    logger.info("Cookie login failed");
                }
            }
        }
        br.setFollowRedirects(true);
        br.getPage("https://secure.id.fc2.com/index.php?mode=login&switch_language=en");
        final Form loginform = br.getFormbyProperty("name", "form_login");
        if (loginform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        loginform.put("email", Encoding.urlEncode(account.getUser()));
        loginform.put("Submit.x", new Random().nextInt(100) + "");
        loginform.put("Submit.y", new Random().nextInt(100) + "");
        loginform.put("password", Encoding.urlEncode(account.getPass()));
        loginform.remove("keep_login");
        /* TODO: "Keep login" functionality is serverside broken? I'm not able to select this on their website/it doesn't get set. */
        // loginform.put("keep_login", "1");
        if (loginform.hasInputFieldByName("recaptcha")) {
            final DownloadLink dlinkbefore = this.getDownloadLink();
            try {
                final DownloadLink dl_dummy;
                if (dlinkbefore != null) {
                    dl_dummy = dlinkbefore;
                } else {
                    dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                    this.setDownloadLink(dl_dummy);
                }
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                loginform.put("recaptcha", Encoding.urlEncode(recaptchaV2Response));
            } finally {
                this.setDownloadLink(dlinkbefore);
            }
        }
        br.submitForm(loginform);
        if (!isLoggedINFC2()) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        br.getPage("https://video.fc2.com/a/");
        if (!isLoggedINVideoFC2()) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "fc2 Account is valid but service 'video.fc2.com' has not been added yet.", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        account.saveCookies(this.br.getCookies(this.getHost()), "");
    }

    private boolean isLoggedINFC2() {
        return br.containsHTML("/logout\\.php");
    }

    private boolean isLoggedINVideoFC2() {
        return br.containsHTML("/logoff\\.php");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        synchronized (account) {
            final AccountInfo ai = new AccountInfo();
            this.login(account, true);
            br.getPage("https://id.fc2.com");
            final String userinfo = br.getRegex("(http://video\\.fc2\\.com(/[a-z]{2})?/mem_login\\.php\\?uid=[^\"]+)").getMatch(0);
            if (userinfo != null) {
                br.getPage(userinfo);
            }
            ai.setUnlimitedTraffic();
            String expire = br.getRegex("premium till (\\d{2}/\\d{2}/\\d{2})").getMatch(0);
            if (expire != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yy", null));
                ai.setStatus("Premium Account");
                account.setProperty("free", false);
            } else if (br.containsHTML(">Paying Member</span>|>Type</li><li[^>]*>Premium Member</li>")) {
                account.setType(AccountType.PREMIUM);
            } else {
                account.setType(AccountType.FREE);
            }
            return ai;
        }
    }

    private void doDownload(final Account account, final DownloadLink link) throws Exception {
        /* OLD-API handling */
        final String error = br.getRegex("^err_code=(\\d+)").getMatch(0);
        if (error != null) {
            switch (Integer.parseInt(error)) {
            case 503:
                // :-)
                break;
            case 601:
                /* reconnect */
                logger.info("video.fc2.com: reconnect is needed!");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading");
            case 602:
                /* reconnect */
                logger.info("video.fc2.com: reconnect is needed!");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading");
            case 603:
                link.setProperty("ONLYFORPREMIUM", true);
                break;
            default:
                logger.info("video.fc2.com: Unknown error code: " + error);
            }
        }
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(this.finalURL)) {
            if (!StringUtils.isEmpty(this.trailerURL)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Only trailer available", 10 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (onlyForPremiumUsers(link)) {
            if (account != null && !account.getBooleanProperty("free", false)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "premium only requirement, when premium account has been used!");
            }
            if (link.getDownloadSize() > 0) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This is a sample video. Full video is only downloadable for Premium Users!");
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for Premium Users!");
        }
        if (finalURLType == 2) {
            br.getPage(finalURL);
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, finalURL);
            dl.startDownload();
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, finalURL, true, -4);
            if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 503 && requestHeadersHasKeyNValueContains(br, "server", "nginx")) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY, "Service unavailable. Try again later.", 5 * 60 * 1000l);
            } else if (!looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The dllink seems not to be a file!");
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                if (br.containsHTML("not found")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl.startDownload();
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        doDownload(null, link);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        br = new Browser();
        this.login(account, false);
        requestFileInformation(link);
        doDownload(account, link);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.finalURL = null;
        this.finalURLType = -1;
        this.server_issues = false;
        String dllink = link.getDownloadURL();
        final String fid = getFID(link);
        // this comes first, due to subdoman issues and cached cookie etc.
        Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            this.login(account, false);
        }
        if (account == null) {
            // no accounts prepareBrowser wouldn't have happened!
            prepareBrowser(br);
        }
        br.setFollowRedirects(true);
        br.getPage(dllink);
        if (isOffline(fid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String newAPIVideotoken = br.getRegex("\\'ae\\'\\s*?,\\s*?\\'([a-f0-9]{32})\\'").getMatch(0);
        String filename = null;
        String uploadername = null;
        /* 2019-01-28: Some videos are still based on their old (flash-)player and cannot be checked via their new API! */
        final boolean useNewAPI = account == null && newAPIVideotoken != null;
        if (useNewAPI) {
            /* 2019-01-28: New way, does not yet have (premium) account support! */
            Map<String, Object> entries;
            if (newAPIVideotoken == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("X-FC2-Video-Access-Token", newAPIVideotoken);
            br.getPage("http://video.fc2.com/api/v3/videoplayer/" + fid + "?" + newAPIVideotoken + "=1&tk=&fs=0");
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            filename = (String) entries.get("title");
            uploadername = (String) JavaScriptEngineFactory.walkJson(entries, "owner/name");
            if (StringUtils.isEmpty(filename)) {
                /* Fallback */
                filename = fid;
            }
            br.getPage("http://video.fc2.com/api/v3/videoplaylist/" + fid + "?sh=1&fs=0");
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            finalURLType = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "type"), -1);
            this.finalURL = (String) JavaScriptEngineFactory.walkJson(entries, "playlist/master");
            if (finalURL == null) {
                finalURL = (String) JavaScriptEngineFactory.walkJson(entries, "playlist/nq");
            }
            this.trailerURL = (String) JavaScriptEngineFactory.walkJson(entries, "playlist/sample");
            if (!StringUtils.isEmpty(finalURL) && finalURL.startsWith("/")) {
                finalURL = br.getURL(finalURL).toString();
            }
        } else {
            // capturing the title in this manner reduces lazy regex scope to just this found string vs entire document.
            uploadername = br.getRegex("Submitter : <a href=\"[^\"]+\" rel=\"nofollow\">([^<>\"]+)</a>").getMatch(0);
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (filename != null) {
                filename = new Regex(filename, ".*?◎?(.*?) \\-.*?").getMatch(0);
            }
            if (filename == null || filename.isEmpty() || filename.matches("[\\s\\p{Z}]+")) {
                filename = br.getRegex("title=\".*?◎([^\"]+)").getMatch(0);
            }
            if (dllink.endsWith("/")) {
                dllink = dllink.substring(0, dllink.length() - 1);
            }
            String upid = dllink.substring(dllink.lastIndexOf("/") + 1);
            String gk = getKey();
            if (upid == null || gk == null) {
                // quite a few of these patterns are too generic, 'this content... is now in javascript variable. errmsg span is also
                // present in
                // ALL pages just doesn't contain text when not valid...
                if (br.containsHTML("This content has already been deleted") || br.getURL().contains("/err.php") || br._getURL().getPath().equals("/404.php") || br.containsHTML("class=\"errmsg\"") || br.getURL().endsWith("://video.fc2.com/")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            /* get url */
            link.setProperty("ONLYFORPREMIUM", false);
            final String from = br.getRegex("\\&from=(\\d+)\\&").getMatch(0);
            final String tk = br.getRegex("\\&tk=([A-Za-z0-9]*?)\\&").getMatch(0);
            final String version = "WIN%2015%2C0%2C0%2C189";
            final String encodedlink = Encoding.urlEncode(br.getURL()).replaceAll("\\.", "%2E").replaceFirst("%2F$", "");
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("Accept-Charset", null);
            /* Extra step is only needed for premium accounts. */
            if (account != null && !account.getBooleanProperty("free", true)) {
                if (tk == null || from == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getPage("/ginfo_payment.php?mimi=" + getMimi(upid) + "&upid=" + upid + "&gk=" + gk + "&tk=" + tk + "&from=" + from + "&href=" + encodedlink + "&lang=en&v=" + upid + "&fversion=" + version + "&otag=0");
            } else {
                br.getPage("/ginfo.php?otag=0&tk=null&href=" + encodedlink + "&upid=" + upid + "&gk=" + gk + "&fversion=" + version + "&playid=null&lang=en&playlistid=null&mimi=" + getMimi(upid) + "&v=" + upid);
            }
            if (br.getHttpConnection() == null) {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if ("23764902a26fbd6345d3cc3533d1d5eb".equalsIgnoreCase(JDHash.getMD5(br.toString()))) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String error = br.getRegex("^err_code=(\\d+)").getMatch(0);
            if (br.getRegex("\\&charge_second=\\d+").matches()) {
                error = "603";
            }
            AvailableStatus aError = null;
            if (error != null) {
                switch (Integer.parseInt(error)) {
                case 403:
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                case 503:
                    // :-)
                    break;
                case 601:
                    /* reconnect */
                    logger.info("video.fc2.com: reconnect is needed!");
                    aError = AvailableStatus.TRUE;
                case 602:
                    /* reconnect */
                    logger.info("video.fc2.com: reconnect is needed!");
                    aError = AvailableStatus.TRUE;
                case 603:
                    link.setProperty("ONLYFORPREMIUM", true);
                    break;
                default:
                    logger.info("video.fc2.com: Unknown error code: " + error);
                    aError = AvailableStatus.UNCHECKABLE;
                }
            }
            // return aError
            if (aError != null) {
                return aError;
            }
            finalURL = br.getRegex("filepath=(https?://.*?)$").getMatch(0);
            prepareFinalLink();
        }
        // prevent NPE
        if (filename != null) {
            if (!StringUtils.isEmpty(uploadername)) {
                filename = uploadername + "_" + filename;
            }
            filename = filename.replaceAll("\\p{Z}", " ");
            // why do we do this?? http://board.jdownloader.org/showthread.php?p=304933#post304933
            // filename = filename.replaceAll("[\\.\\d]{3,}$", "");
            filename = filename.trim();
            filename = filename.replaceAll("(:|,|\\s)", "_");
            filename += ".mp4";
            link.setFinalFileName(Encoding.htmlDecode(filename));
        }
        if (!this.getPluginConfig().getBooleanProperty(fastLinkCheck, fastLinkCheck_default) && finalURL != null && finalURLType != 2) {
            br.getHeaders().put("Referer", null);
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(finalURL);
                if (looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setDownloadSize(con.getCompleteContentLength());
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    server_issues = true;
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

    private boolean isOffline(final String linkid) {
        return br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("err.php") || !br.getURL().contains(linkid) || br.toString().contains("(Removed) **************");
    }

    private String getMimi(String s) {
        return JDHash.getMD5(s + "_" + "gGddgPfeaf_gzyr");
    }

    private String getKey() {
        String javaScript = br.getRegex("eval(\\(f.*?)[\r\n]+").getMatch(0);
        if (javaScript == null) {
            javaScript = br.getRegex("(var __[0-9a-zA-Z]+ = \'undefined\'.*?\\})[\r\n]+\\-\\->").getMatch(0);
        }
        if (javaScript == null) {
            return null;
        }
        Object result = new Object();
        ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        ScriptEngine engine = manager.getEngineByName("javascript");
        Invocable inv = (Invocable) engine;
        try {
            if (!javaScript.startsWith("var")) {
                engine.eval(engine.eval(javaScript).toString());
            }
            engine.eval(javaScript);
            engine.eval("var window = new Object();");
            result = inv.invokeFunction("getKey");
        } catch (Throwable e) {
            return null;
        }
        return result != null ? result.toString() : null;
    }

    private void prepareFinalLink() {
        if (finalURL != null) {
            finalURL = finalURL.replaceAll("\\&mid=", "?mid=");
            String t = new Regex(finalURL, "cdnt=(\\d+)").getMatch(0);
            String h = new Regex(finalURL, "cdnh=([0-9a-f]+)").getMatch(0);
            finalURL = new Regex(finalURL, "(.*?)\\&sec=").getMatch(0);
            if (t != null && h != null) {
                finalURL = finalURL + "&px-time=" + t + "&px-hash=" + h;
            }
        }
    }

    private boolean onlyForPremiumUsers(DownloadLink downloadLink) {
        return downloadLink.getBooleanProperty("ONLYFORPREMIUM", false);
    }

    /**
     * If import Browser request headerfield contains key of k && key value of v
     *
     * @author raztoki
     */
    private boolean requestHeadersHasKeyNValueContains(final Browser ibr, final String k, final String v) {
        if (k == null || v == null || ibr == null || ibr.getHttpConnection() == null) {
            return false;
        }
        if (ibr.getHttpConnection().getHeaderField(k) != null && ibr.getHttpConnection().getHeaderField(k).toLowerCase(Locale.ENGLISH).contains(v.toLowerCase(Locale.ENGLISH))) {
            return true;
        }
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}