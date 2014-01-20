package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.Icon;

import jd.controlling.AccountController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginCache;
import jd.plugins.PluginException;
import jd.plugins.PluginProgress;
import jd.plugins.hoster.YoutubeDashV2.YoutubeConfig;
import jd.utils.locale.JDL;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.config.PluginJsonConfig;

import de.savemytube.flv.FLV;

public class YoutubeHelper {

    private Browser br;

    public Browser getBr() {
        return br;
    }

    public void setBr(Browser br) {
        this.br = br;
    }

    private final YoutubeConfig           cfg;

    private final LogSource               logger;
    private String                        base;
    private List<YoutubeVariantInterface> variants;

    public List<YoutubeVariantInterface> getVariants() {
        return variants;
    }

    private Map<String, YoutubeVariantInterface> variantsMap;

    public Map<String, YoutubeVariantInterface> getVariantsMap() {
        return variantsMap;
    }

    public static final String YT_EXT             = "YT_EXT";
    public static final String YT_TITLE           = "YT_TITLE";
    public static final String YT_PLAYLIST_INT    = "YT_PLAYLIST_INT";
    public static final String YT_ID              = "YT_ID";
    public static final String YT_AGE_GATE        = "YT_AGE_GATE";
    public static final String YT_CHANNEL         = "YT_CHANNEL";
    public static final String YT_USER            = "YT_USER";
    public static final String YT_DATE            = "YT_DATE";
    public static final String YT_VARIANTS        = "YT_VARIANTS";
    public static final String YT_VARIANT         = "YT_VARIANT";
    public static final String YT_STREAMURL_VIDEO = "YT_STREAMURL_VIDEO";
    public static final String YT_STREAMURL_AUDIO = "YT_STREAMURL_AUDIO";

    private static String handleRule(String s, final String line) throws PluginException {

        final String method = new Regex(line, "\\.([\\w\\d]+?)\\(\\s*\\)").getMatch(0);
        if ("reverse".equals(method)) {
            //
            s = new StringBuilder(s).reverse().toString();

            return s;
        }
        // slice
        final String i = new Regex(line, "\\.slice\\((\\d+)\\)").getMatch(0);
        if (i != null) {
            //
            s = s.substring(Integer.parseInt(i));

            return s;
        }

        final String idx = new Regex(line, "=..\\([^,]+\\,(\\d+)\\)").getMatch(0);
        if (idx != null) {
            final int idxI = Integer.parseInt(idx);
            s = YoutubeHelper.pk(s, idxI);

            return s;

        }

        if (new Regex(line, "\\.split\\(\"\"\\)").matches()) { return s; }
        if (new Regex(line, "\\.join\\(\"\"\\)").matches()) { return s; }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown Signature Rule: " + line);

    }

    protected static void convertToMp3(DownloadLink downloadLink) {
        try {
            downloadLink.setPluginProgress(new PluginProgress(0, 100, null) {
                {
                    setIcon(new AbstractIcon(IconKey.ICON_AUDIO, 18));

                }

                @Override
                public long getCurrent() {
                    return 95;
                }

                @Override
                public Icon getIcon(Object requestor) {
                    if (requestor instanceof ETAColumn) return null;
                    return super.getIcon(requestor);
                }

                @Override
                public String getMessage(Object requestor) {
                    if (requestor instanceof ETAColumn) return "";
                    return "Create Mp3";
                }
            });
            File file = new File(downloadLink.getFileOutput());

            new FLV(downloadLink.getFileOutput(), true, true);

            file.delete();
            File finalFile = new File(downloadLink.getFileOutput().replaceAll(".tmp$", ""));
            finalFile.delete();
            new File(downloadLink.getFileOutput().replaceAll(".tmp$", ".mp3")).renameTo(finalFile);
            new File(downloadLink.getFileOutput().replaceAll(".tmp$", ".avi")).delete();
            downloadLink.setDownloadSize(finalFile.length());
            downloadLink.setDownloadCurrent(finalFile.length());
            try {
                downloadLink.setFinalFileOutput(finalFile.getAbsolutePath());
                downloadLink.setCustomFileOutputFilenameAppend(null);
                downloadLink.setCustomFileOutputFilename(null);
            } catch (final Throwable e) {
            }
        } finally {
            downloadLink.setPluginProgress(null);
        }
    }

    protected static String pk(final String s, final int idxI) {
        final char c = s.charAt(0);
        final StringBuilder sb = new StringBuilder();
        sb.append(s.charAt(idxI % s.length()));
        sb.append(s.substring(1, idxI));
        sb.append(c);
        sb.append(s.substring(idxI + 1));
        return sb.toString();
    }

    public YoutubeHelper(final Browser br, final YoutubeConfig cfg, final LogSource logger) {
        this.br = br;
        this.logger = logger;

        this.cfg = cfg;

        if (cfg.isPreferHttpsEnabled()) {
            this.base = "https://www.youtube.com";
        } else {
            this.base = "http://www.youtube.com";
        }
        ArrayList<YoutubeVariantInterface> variants = new ArrayList<YoutubeVariantInterface>();
        HashMap<String, YoutubeVariantInterface> variantsMap = new HashMap<String, YoutubeVariantInterface>();
        for (YoutubeVariant v : YoutubeVariant.values()) {
            variants.add(v);

        }
        ArrayList<YoutubeCustomVariantStorable> list = cfg.getCustomVariants();
        if (list != null) {
            for (YoutubeCustomVariantStorable v : list) {
                try {
                    variants.add(v.toVariant());
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        }
        // create map
        for (YoutubeVariantInterface v : variants) {
            variantsMap.put(v.getUniqueId(), v);
        }
        this.variants = Collections.unmodifiableList(variants);
        this.variantsMap = Collections.unmodifiableMap(variantsMap);
    }

    /**
     * *
     * 
     * @param br
     * 
     * @param s
     * @return
     * @throws IOException
     * @throws PluginException
     */
    String descrambleSignature(final String sig) throws IOException, PluginException {
        if (sig == null) { return null; }
        String jsUrl = this.br.getMatch("\"js\"\\: \"(.+?)\"");
        jsUrl = jsUrl.replace("\\/", "/");
        jsUrl = "http:" + jsUrl;
        String des = null;

        Browser clone = br.cloneBrowser();

        String jsContent = getAbsolute(jsUrl, jsUrl, clone);
        final String descrambler = new Regex(jsContent, "\\w+\\.signature\\=([\\w\\d]+)\\([\\w\\d]+\\)").getMatch(0);
        final String func = "function " + descrambler + "\\(([^)]+)\\)\\{(.+?return.*?)\\}";
        des = new Regex(jsContent, Pattern.compile(func)).getMatch(1);

        String s = sig;
        // Debug code.
        // Context cx = null;
        // Object result = null;
        // try {
        //
        // try {
        // cx = ContextFactory.getGlobal().enterContext();
        //
        // } catch (java.lang.SecurityException e) {
        // /* in case classshutter already set */
        // }
        // Scriptable scope = cx.initStandardObjects();
        // String all = new Regex(js, Pattern.compile("function " + descrambler +
        // "\\(([^)]+)\\)\\{(.+?return.*?)\\}.*?\\{.*?\\}")).getMatch(-1);
        // result = cx.evaluateString(scope, all + " " + descrambler + "(\"" + sig + "\")", "<cmd>", 1, null);
        //
        // } finally {
        // try {
        // Context.exit();
        // } catch (final Throwable e) {
        // }
        // }
        for (final String line : new Regex(des, "[^;]+").getColumn(-1)) {

            s = YoutubeHelper.handleRule(s, line);
        }

        return s;

    }

    protected void extractData(final YoutubeClipData vid) {
        if (StringUtils.isEmpty(vid.title) && this.br.containsHTML("&title=")) {
            final String match = this.br.getRegex("&title=([^&$]+)").getMatch(0);
            if (match != null) {
                vid.title = Encoding.htmlDecode(match.replaceAll("\\+", " ").trim());
            }

        }
        if (StringUtils.isEmpty(vid.title)) {
            final String match = this.br.getRegex("<title>(.*?) - YouTube</title>").getMatch(0);
            if (match != null) {
                vid.title = Encoding.htmlDecode(match.replaceAll("\\+", " ").trim());

            }
        }

        if (vid.length <= 0) {
            final String match = this.br.getRegex("\"length_seconds\"\\: (\\d+)").getMatch(0);
            if (match != null) {
                vid.length = Integer.parseInt(match);

            }
        }
        if (StringUtils.isEmpty(vid.title)) {
            final String match = this.br.getRegex("<meta name=\"title\" content=\"(.*?)\">").getMatch(0);
            if (match != null) {
                vid.title = Encoding.htmlDecode(match.trim());

            }
        }

        if (vid.date <= 0) {
            final Locale locale = Locale.ENGLISH;
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy", locale);
            String date = this.br.getRegex("class=\"watch-video-date\" >([ ]+)?(\\d{1,2} [A-Za-z]{3} \\d{4})</span>").getMatch(1);

            if (date == null) {
                formatter = new SimpleDateFormat("dd MMM yyyy", locale);
                date = this.br.getRegex("<strong>Published on (\\d{1,2} [A-Za-z]{3} \\d{4})</strong>").getMatch(1);
            }

            if (date != null) {
                try {
                    vid.date = formatter.parse(date).getTime();
                } catch (final Exception e) {
                    final LogSource log = LogController.getInstance().getPreviousThreadLogSource();
                    log.log(e);

                }
            }
        }
        if (StringUtils.isEmpty(vid.channel)) {
            final String match = this.br.getRegex("feature=watch\"[^>]+dir=\"ltr[^>]+>(.*?)</a>(\\s+)?<span class=\"yt-user").getMatch(0);
            if (match != null) {
                vid.channel = Encoding.htmlDecode(match.trim());

            }
        }
        if (StringUtils.isEmpty(vid.user)) {
            final String match = this.br.getRegex("temprop=\"url\" href=\"http://(www\\.)?youtube\\.com/user/([^<>\"]*?)\"").getMatch(1);
            if (match != null) {
                vid.user = Encoding.htmlDecode(match.trim());

            }
        }

        //

    }

    public String getAbsolute(final String absolute, String id, Browser clone) throws IOException {
        if (clone == null) clone = br;
        if (id == null) id = absolute;
        PluginCache pluginCache = Plugin.getCache("youtube.com");
        Object page = pluginCache.getCache(id, null);
        if (page != null && page instanceof MinTimeWeakReference) {
            String content = ((MinTimeWeakReference<String>) page).get();
            if (StringUtils.isNotEmpty(content)) {
                clone.setRequest(new GetRequest(absolute));
                clone.getRequest().setHtmlCode(content);
                return content;
            }
        }

        clone.getPage(absolute);
        if (clone.getRequest().getHttpConnection().getResponseCode() == 200) {
            pluginCache.setCache(id, new MinTimeWeakReference<String>(clone.getRequest().getHtmlCode(), 30000, id));
        }
        return clone.getRequest().getHtmlCode();

    }

    protected void handleContentWarning(final Browser br) throws Exception {
        // nsfw testlink https://www.youtube.com/watch?v=p7S_u5TzI-I
        // youtube shows an extra screen the first time a user wants to see a age-protected video.
        // <div class="content">
        // <h1 id="unavailable-message" class="message">
        // Content Warning
        //
        // </h1>
        // <div id="unavailable-submessage" class="submessage">
        // <div id="watch7-player-age-gate-content">
        // <p>This video may be inappropriate for some users.</p>
        //
        // <p>By confirming, you agree that this warning will no longer be shown in the future.</p>
        // <form action="/verify_age?action_confirm=1" method="POST">
        // <input type="hidden" name="next_url" value="/watch?v=p7S_u5TzI-I">
        // <input type="hidden" name="set_racy" value="true">
        // <input type="hidden" name="session_token" value="d5tNczUUbnz7-G160SZlqEFiM798MTM4OTE3NzA4M0AxMzg5MDkwNjgz"/>
        // <button onclick=";return true;" class=" yt-uix-button yt-uix-button-primary yt-uix-button-size-default" type="submit"
        // role="button"><span class="yt-uix-button-content">I understand and wish to proceed </span></button>
        // </form>
        //
        // <p class="safety-mode-message">If you would instead prefer to avoid potentially inappropriate content, consider
        // activating YouTube's <a href="//support.google.com/youtube/bin/answer.py?answer=174084&amp;hl=en-GB">Safety Mode</a>.</p>
        // </div>

        final Form forms[] = br.getForms();
        if (forms != null) {
            for (final Form form : forms) {
                if (form.getAction() != null && form.getAction().contains("verify_age")) {
                    this.logger.info("Verify Age");
                    br.submitForm(form);
                    break;
                }
            }
        }

    }

    public Map<YoutubeITAG, YoutubeStreamData> loadVideo(final YoutubeClipData vid) throws Exception {
        final Map<YoutubeITAG, YoutubeStreamData> ret = new HashMap<YoutubeITAG, YoutubeStreamData>();
        final YoutubeConfig cfg = PluginJsonConfig.get(YoutubeConfig.class);

        this.br.setFollowRedirects(true);
        /* this cookie makes html5 available and skip controversy check */

        this.br.setCookie("youtube.com", "PREF", "f2=40100000&hl=en-GB");
        this.br.getHeaders().put("User-Agent", "Wget/1.12");

        getAbsolute(base + "/watch?v=" + vid.videoID, null, br);
        if (this.br.containsHTML("id=\"unavailable-submessage\" class=\"watch-unavailable-submessage\"")) { return null; }

        this.extractData(vid);
        boolean getVideoInfoWorkaroundUsed = false;
        if (this.br.containsHTML("age-gate")) {
            vid.ageCheck = true;

            this.handleContentWarning(this.br);

            if (this.br.containsHTML("age-gate")) {
                // try to bypass
                getVideoInfoWorkaroundUsed = true;
                this.br.getPage(this.base + "/get_video_info?video_id=" + vid.videoID);
            }
            this.extractData(vid);
        }

        // check if video is private
        final String unavailableReason = this.br.getRegex("<div id=\"player-unavailable\" class=\"  player-width player-height    player-unavailable \">.*?<h. id=\"unavailable-message\"[^>]*?>([^<]+)").getMatch(0);
        if (unavailableReason != null) {
            vid.error = Encoding.htmlDecode(unavailableReason.replaceAll("\\+", " ").trim());
            return null;
        }

        String html5_fmt_map;
        String dashFmt;
        if (getVideoInfoWorkaroundUsed) {
            // age check bypass active

            html5_fmt_map = this.br.getRegex("url_encoded_fmt_stream_map=(.*?)(&|$)").getMatch(0);
            html5_fmt_map = Encoding.htmlDecode(html5_fmt_map);

            dashFmt = this.br.getRegex("adaptive_fmts=(.*?)(&|$)").getMatch(0);
            dashFmt = Encoding.htmlDecode(dashFmt);

        } else {
            // regular url testlink: http://www.youtube.com/watch?v=4om1rQKPijI

            html5_fmt_map = this.br.getRegex("\"url_encoded_fmt_stream_map\": (\".*?\")").getMatch(0);

            html5_fmt_map = JSonStorage.restoreFromString(html5_fmt_map, new TypeRef<String>() {
            });

            dashFmt = this.br.getRegex("\"adaptive_fmts\": (\".*?\")").getMatch(0);
            dashFmt = JSonStorage.restoreFromString(dashFmt, new TypeRef<String>() {
            });
        }

        for (final String line : html5_fmt_map.split("\\,")) {
            final YoutubeStreamData match = this.parseLine(vid, line);
            if (match != null) {
                ret.put(match.itag, match);
            }
        }
        if (dashFmt != null) {
            for (final String line : dashFmt.split("\\,")) {
                final YoutubeStreamData match = this.parseLine(vid, line);
                if (match != null) {
                    ret.put(match.itag, match);
                }
            }
        }

        for (YoutubeStreamData sd : loadThumbnails(vid)) {
            ret.put(sd.itag, sd);
        }

        return ret;
    }

    private List<YoutubeStreamData> loadThumbnails(YoutubeClipData vid) {
        ArrayList<YoutubeStreamData> ret = new ArrayList<YoutubeStreamData>();
        String best = br.getRegex("<meta property=\"og\\:image\" content=\".*?/(\\w+\\.jpg)\">").getMatch(0);
        ret.add(new YoutubeStreamData(vid, "http://img.youtube.com/vi/" + vid.videoID + "/default.jpg", YoutubeITAG.IMAGE_LQ));
        if (best != null && best.equals("default.jpg")) return ret;
        ret.add(new YoutubeStreamData(vid, "http://img.youtube.com/vi/" + vid.videoID + "/mqdefault.jpg", YoutubeITAG.IMAGE_MQ));
        if (best != null && best.equals("mqdefault.jpg")) return ret;
        ret.add(new YoutubeStreamData(vid, "http://img.youtube.com/vi/" + vid.videoID + "/hqdefault.jpg", YoutubeITAG.IMAGE_HQ));
        if (best != null && best.equals("hqdefault.jpg")) return ret;

        ret.add(new YoutubeStreamData(vid, "http://img.youtube.com/vi/" + vid.videoID + "/maxresdefault.jpg", YoutubeITAG.IMAGE_MAX));

        return ret;
    }

    public void login(final Account account, final boolean refresh, final boolean showDialog) throws Exception {

        try {
            this.br.setDebug(true);
            this.br.setCookiesExclusive(true);
            this.br.clearCookies("youtube.com");
            br.setCookie("http://youtube.com", "PREF", "hl=en-GB");
            if (account.getProperty("cookies") != null) {
                @SuppressWarnings("unchecked")
                final HashMap<String, String> cookies = (HashMap<String, String>) account.getProperty("cookies");
                if (cookies != null) {
                    if (cookies.containsKey("LOGIN_INFO")) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie("youtube.com", key, value);
                        }

                        if (refresh == false) {
                            return;
                        } else {
                            this.br.getPage("http://www.youtube.com");
                            if (!this.br.containsHTML("<span class=\"yt-uix-button-content\">Sign In </span></button></div>")) { return; }
                        }
                    }
                }
            }

            this.br.setFollowRedirects(true);
            this.br.getPage(this.replaceHttps("http://www.youtube.com/"));
            /* first call to google */
            this.br.getPage("https://www.google.com/accounts/ServiceLogin?uilel=3&service=youtube&passive=true&continue=http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26nomobiletemp%3D1%26hl%3Den_US%26next%3D%252Findex&hl=en_US&ltmpl=sso");
            String checkConnection = this.br.getRegex("iframeUri: \\'(https.*?)\\'").getMatch(0);
            if (checkConnection != null) {
                /*
                 * don't know if this is important but seems to set pstMsg to 1 ;)
                 */
                checkConnection = Encoding.unescape(checkConnection);
                try {
                    this.br.cloneBrowser().getPage(checkConnection);
                } catch (final Exception e) {
                    this.logger.info("checkConnection failed, continuing anyways...");
                }
            }
            final Form form = this.br.getForm(0);
            form.put("pstMsg", "1");
            form.put("dnConn", "https%3A%2F%2Faccounts.youtube.com&continue=http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26nomobiletemp%3D1%26hl%3Den_US%26next%3D%252F");
            form.put("Email", Encoding.urlEncode(account.getUser()));
            form.put("Passwd", Encoding.urlEncode(account.getPass()));
            form.put("GALX", this.br.getCookie("http://www.google.com", "GALX"));
            form.put("timeStmp", "");
            form.put("secTok", "");
            form.put("rmShown", "1");
            form.put("signIn", "Anmelden");
            form.put("asts", "");
            this.br.setFollowRedirects(false);
            final String cook = this.br.getCookie("http://www.google.com", "GALX");
            if (cook == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            this.br.submitForm(form);
            if (this.br.getRedirectLocation() == null) {
                final String page = Encoding.htmlDecode(this.br.toString());
                final String red = new Regex(page, "url='(https?://.*?)'").getMatch(0);
                if (red == null) {
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                this.br.getPage(red);
            }
            /* second call to google */
            this.br.getPage(this.br.getRedirectLocation());
            if (this.br.containsHTML("Google will check if this")) {
                if (showDialog) {
                    UserIO.getInstance().requestMessageDialog(0, "Youtube Login Error", "Please logout and login again at youtube.com, account check needed!");
                }
                account.setValid(false);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }

            // 2-step verification
            if (this.br.containsHTML("2-step verification")) {
                final String step = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.NO_ICON, JDL.L("plugins.hoster.youtube.2step.title", "2-Step verification required"), JDL.L("plugins.hoster.youtube.2step.message", "Youtube.com requires Google's 2-Step verification. Please input the code from your phone or the backup list."), "", null, null, null);
                Form stepform = this.br.getForm(0);
                stepform.put("smsUserPin", step);
                stepform.remove("exp");
                stepform.remove("ltmpl");
                this.br.setFollowRedirects(true);
                this.br.submitForm(stepform);

                if (this.br.containsHTML("The code you entered didn&#39;t verify")) {
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.youtube.2step.failed", "2-Step verification code couldn't be verified!"));
                }

                stepform = this.br.getForm(0);
                if (stepform != null) {
                    stepform.remove("nojssubmit");
                    this.br.submitForm(stepform);
                    this.br.getPage(this.replaceHttps("http://www.youtube.com/signin?action_handle_signin=true"));
                } else {
                    String url = this.br.getRegex("\"(https?://www\\.youtube\\.com/signin\\?action_handle_signin.*?)\"").getMatch(0);
                    if (url != null) {
                        url = Encoding.unescape(url);
                        this.br.getPage(url);
                    }
                }
            } else if (this.br.containsHTML("class=\"gaia captchahtml desc\"")) {
                if (true) {
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.youtube.logincaptcha.failed", "The captcha login verification is broken. Please contact our support."));
                }
                final String captchaLink = this.br.getRegex("<img src=\\'(https?://accounts\\.google\\.com/Captcha\\?[^<>\"]*?)\\'").getMatch(0);
                if (captchaLink == null) {
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.youtube.logincaptcha.failed", "The captcha login verification is broken. Please contact our support."));
                }
                // final DownloadLink dummyLink = new DownloadLink(this, "Account", "youtube.com", "http://youtube.com", true);
                // final String c = getCaptchaCode(captchaLink, dummyLink);
                // Lots of stuff needed here
                // br.postPage("https://accounts.google.com/LoginVerification", "");
                throw new WTFException("Not Implemented");

            } else {
                this.br.setFollowRedirects(true);
                this.br.getPage(this.br.getRedirectLocation());

                final String location = Encoding.unescape(this.br.getRegex("location\\.replace\\(\"(.*?)\"").getMatch(0));
                this.br.getPage(location);
            }
            if (this.br.getCookie("http://www.youtube.com", "LOGIN_INFO") == null) {
                account.setValid(false);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies cYT = this.br.getCookies("youtube.com");
            for (final Cookie c : cYT.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            // set login cookie of the account.
            account.setProperty("cookies", cookies);
        } catch (final PluginException e) {
            account.setProperty("cookies", null);
            throw e;
        }

    }

    public void login(final boolean refresh, final boolean showDialog) {

        ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("youtube.com");
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {

                    try {

                        this.login(n, refresh, showDialog);
                        if (n.isValid()) { return; }
                    } catch (final Exception e) {

                        n.setValid(false);
                        return;
                    }

                }
            }
        }

        // dbueg

        accounts = AccountController.getInstance().getAllAccounts("youtube.jd");
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {

                    try {

                        this.login(n, refresh, showDialog);
                        if (n.isValid()) { return; }
                    } catch (final Exception e) {

                        n.setValid(false);
                        return;
                    }

                }
            }
        }
        return;
    }

    public static final String YT_LENGTH_SECONDS     = "YT_LENGTH_SECONDS";
    public static final String YT_STATIC_URL         = "YT_STATIC_URL";
    public static final String YT_STREAMURL_DATA     = "YT_STREAMURL_DATA";
    public static final String YT_SUBTITLE_CODE      = "YT_SUBTITLE_CODE";
    public static final String YT_SUBTITLE_CODE_LIST = "YT_SUBTITLE_CODE_LIST";

    public String createFilename(DownloadLink link) {
        String var = link.getStringProperty(YoutubeHelper.YT_VARIANT, "");
        YoutubeVariantInterface variant = getVariantById(var);
        YoutubeConfig cfg = PluginJsonConfig.get(YoutubeConfig.class);
        LogSource logger = LogController.getInstance().getLogger(TbCmV2.class.getName());
        String formattedFilename = cfg.getFilenamePattern();
        // validate the pattern
        if ((!formattedFilename.contains("*videoname*") && !formattedFilename.contains("*videoid*")) || !formattedFilename.contains("*ext*")) formattedFilename = jd.plugins.hoster.Youtube.defaultCustomFilename;
        if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = "*videoname**quality**ext*";
        try {
            formattedFilename = variant.modifyFileName(formattedFilename, link);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // playlistnumber
        String format = new Regex(formattedFilename, "\\*videonumber\\[(.+?)\\]\\*").getMatch(0);

        if (StringUtils.isEmpty(format)) format = "0000";
        DecimalFormat df;
        try {
            df = new DecimalFormat(format);
        } catch (Exception e) {
            logger.log(e);
            df = new DecimalFormat("0000");
        }
        int playlistNumber = link.getIntegerProperty(YoutubeHelper.YT_PLAYLIST_INT, -1);
        formattedFilename = formattedFilename.replaceAll("\\*videonumber(\\[[^\\]]+\\])?\\*", playlistNumber >= 0 ? df.format(playlistNumber) : "");
        // date
        format = new Regex(formattedFilename, "\\*date\\[(.+?)\\]\\*").getMatch(0);
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG, TranslationFactory.getDesiredLocale());

        if (StringUtils.isNotEmpty(format)) {
            try {
                formatter = new SimpleDateFormat(format);
            } catch (Exception e) {
                logger.log(e);

            }
        }
        long timestamp = link.getLongProperty(YoutubeHelper.YT_DATE, -1);
        formattedFilename = formattedFilename.replaceAll("\\*date(\\[[^\\]]+\\])?\\*", timestamp > 0 ? formatter.format(timestamp) : "");
        // channelname
        formattedFilename = formattedFilename.replace("*channelname*", link.getStringProperty(YoutubeHelper.YT_CHANNEL, ""));
        formattedFilename = formattedFilename.replace("*username*", link.getStringProperty(YoutubeHelper.YT_USER, ""));
        formattedFilename = formattedFilename.replace("*agegate*", link.getBooleanProperty(YoutubeHelper.YT_AGE_GATE, false) + "");
        formattedFilename = formattedFilename.replace("*ext*", link.getStringProperty(YoutubeHelper.YT_EXT, "unknown"));
        formattedFilename = formattedFilename.replace("*videoid*", link.getStringProperty(YoutubeHelper.YT_ID, ""));

        try {

            formattedFilename = formattedFilename.replace("*quality*", variant.getQualityExtension());
        } catch (Exception e) {
            // old variant
            formattedFilename = formattedFilename.replace("*quality*", "[INVALID LINK!]");
        }
        formattedFilename = formattedFilename.replace("*videoname*", link.getStringProperty(YoutubeHelper.YT_TITLE, "")).replace("*title*", link.getStringProperty(YoutubeHelper.YT_TITLE, ""));
        formattedFilename = formattedFilename.replace("*variant*", link.getStringProperty(YoutubeHelper.YT_VARIANT, ""));
        try {
            formattedFilename = variant.modifyFileName(formattedFilename, link);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return formattedFilename;
    }

    protected YoutubeStreamData parseLine(final YoutubeClipData vid, final String line) throws MalformedURLException, IOException, PluginException {

        final LinkedHashMap<String, String> query = Request.parseQuery(line);
        if (line.contains("conn=rtmp")) {
            logger.info("Stream is not supported: " + query);
            vid.error = "RTMP(E) Stream not supported";
            return null;
        }
        String url = Encoding.htmlDecode(Encoding.urlDecode(query.get("url"), true));

        String signature = new Regex(url, "(sig|signature)=(.*?)(\\&|$)").getMatch(1);

        if (StringUtils.isEmpty(signature)) {
            // verified 7.1.24
            // non dash?
            signature = query.get("sig");
        }
        if (StringUtils.isEmpty(signature)) {
            signature = query.get("signature");
        }
        if (StringUtils.isEmpty(signature)) {
            // verified 7.1.213
            signature = this.descrambleSignature(query.get("s"));
        }

        if (url != null && !url.contains("sig")) {

            url = url + "&signature=" + signature;
        }
        int bitrate = -1;
        String bitrateString = query.get("bitrate");
        if (StringUtils.isNotEmpty(bitrateString)) {
            bitrate = Integer.parseInt(bitrateString);
        }
        final YoutubeITAG itag = YoutubeITAG.get(Integer.parseInt(query.get("itag")));

        final String quality = Encoding.urlDecode(query.get("quality"), false);
        System.out.println(query);
        if (url != null && itag != null) {

            return new YoutubeStreamData(vid, url, itag);
        } else {

            this.logger.info("Unkown Line: " + line);
            this.logger.info(query + "");
        }
        return null;
    }

    private String replaceHttps(final String s) {

        final boolean prefers = this.cfg.isPreferHttpsEnabled();

        if (prefers) {
            return s.replaceFirst("http://", "https://");
        } else {
            return s.replaceFirst("https://", "http://");
        }
    }

    public void setupProxy() {
        if (br == null) return;
        if (this.cfg.isProxyEnabled()) {
            final HTTPProxyStorable proxy = this.cfg.getProxy();

            // int PROXY_PORT = cfg.getProxyPort();
            // if (StringUtils.isEmpty(PROXY_ADDRESS) || PROXY_PORT < 0) return;
            // PROXY_ADDRESS = new Regex(PROXY_ADDRESS, "^[0-9a-zA-Z]+://").matches() ? PROXY_ADDRESS : "http://" + PROXY_ADDRESS;
            // org.appwork.utils.net.httpconnection.HTTPProxy proxy =
            // org.appwork.utils.net.httpconnection.HTTPProxy.parseHTTPProxy(PROXY_ADDRESS + ":" + PROXY_PORT);

            if (proxy != null) {
                HTTPProxy prxy = HTTPProxy.getHTTPProxy(proxy);
                if (prxy != null) {
                    this.br.setProxy(prxy);
                } else {

                }
                return;
            }

        }
        this.br.setProxy(this.br.getThreadProxy());
    }

    public ArrayList<YoutubeSubtitleInfo> loadSubtitles(YoutubeClipData vid) throws IOException {
        ArrayList<YoutubeSubtitleInfo> urls = new ArrayList<YoutubeSubtitleInfo>();
        String ttsUrl = br.getRegex("\"ttsurl\": (\"http.*?\")").getMatch(0);
        if (ttsUrl != null) {
            ttsUrl = JSonStorage.restoreFromString(ttsUrl, new TypeRef<String>() {

            });
        } else {
            return urls;
        }
        getAbsolute(replaceHttps(ttsUrl + "&asrs=1&fmts=1&tlangs=1&ts=" + System.currentTimeMillis() + "&type=list"), "subtitle-" + vid.videoID, br);

        ttsUrl = ttsUrl.replaceFirst("v=[a-zA-Z0-9\\-_]+", "");

        String[] matches = br.getRegex("<track id=\"(.*?)\".*?/>").getColumn(0);
        HashSet<String> duplicate = new HashSet<String>();

        for (String trackID : matches) {
            String lang = br.getRegex("<track id=\"" + trackID + "\".*?lang_code=\"(.*?)\".*?/>").getMatch(0);
            String name = br.getRegex("<track id=\"" + trackID + "\".*?name=\"(.*?)\".*?/>").getMatch(0);
            String kind = br.getRegex("<track id=\"" + trackID + "\".*?kind=\"(.*?)\".*?/>").getMatch(0);
            String langOrg = br.getRegex("<track id=\"" + trackID + "\".*?lang_original=\"(.*?)\".*?/>").getMatch(0);
            String langTrans = br.getRegex("<track id=\"" + trackID + "\".*?lang_translated=\"(.*?)\".*?/>").getMatch(0);
            if (name == null) name = "";
            if (kind == null) kind = "";
            if (duplicate.add(lang) == false) continue;
            if (StringUtils.isNotEmpty(langTrans)) {
                langOrg = langTrans;
            }
            if (StringUtils.isEmpty("langOrg")) {
                langOrg = new Locale("lang").getDisplayLanguage(Locale.ENGLISH);
            }
            urls.add(new YoutubeSubtitleInfo(ttsUrl, lang, name, kind, langOrg));

        }
        return urls;
    }

    public YoutubeVariantInterface getVariantById(String ytv) {
        return variantsMap.get(ytv);
    }

    public List<YoutubeVariantInterface> getVariantByIds(String... extra) {
        ArrayList<YoutubeVariantInterface> ret = new ArrayList<YoutubeVariantInterface>();
        if (extra != null) {
            for (String s : extra) {
                YoutubeVariantInterface v = getVariantById(s);
                if (v != null) {
                    ret.add(v);
                }
            }
        }
        return ret;
    }

}