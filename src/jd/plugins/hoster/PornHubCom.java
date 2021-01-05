//  jDownloader - Downloadmanager
//  Copyright (C) 2013  JD-Team support@jdownloader.org
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PornHubCom extends PluginForHost {
    /* Connection stuff */
    // private static final boolean FREE_RESUME = true;
    // private static final int FREE_MAXCHUNKS = 0;
    private static final int                      FREE_MAXDOWNLOADS                     = 5;
    private static final boolean                  ACCOUNT_FREE_RESUME                   = true;
    private static final int                      ACCOUNT_FREE_MAXCHUNKS                = 0;
    private static final int                      ACCOUNT_FREE_MAXDOWNLOADS             = 5;
    public static final long                      trust_cookie_age                      = 5 * 60 * 1000l;
    public static final boolean                   use_download_workarounds              = true;
    private static final String                   type_photo                            = "(?i).+/photo/\\d+";
    private static final String                   type_gif_webm                         = "(?i).+/(embed)?gif/\\d+";
    public static final String                    html_privatevideo                     = "id=\"iconLocked\"";
    public static final String                    html_privateimage                     = "profile/private-lock\\.png";
    public static final String                    html_purchase_only                    = "'Buy on video player'";
    public static final String                    html_premium_only                     = "<h2>\\s*Upgrade to Pornhub Premium to enjoy this video\\.</h2>";
    private String                                dlUrl                                 = null;
    /* Note: Video bitrates and resolutions are not exact, they can vary. */
    /* Quality, { videoCodec, videoBitrate, videoResolution, audioCodec, audioBitrate } */
    public static LinkedHashMap<String, String[]> formats                               = new LinkedHashMap<String, String[]>(new LinkedHashMap<String, String[]>() {
        {
            put("240", new String[] { "AVC", "400", "420x240", "AAC LC", "54" });
            put("480", new String[] { "AVC", "600", "850x480", "AAC LC", "54" });
            put("720", new String[] { "AVC", "1500", "1280x720", "AAC LC", "54" });
            put("1080", new String[] { "AVC", "4000", "1920x1080", "AAC LC", "96" });
            put("1440", new String[] { "AVC", "6000", " 2560x1440", "AAC LC", "96" });
            put("2160", new String[] { "AVC", "8000", "3840x2160", "AAC LC", "128" });
        }
    });
    public static final String                    BEST_ONLY                             = "BEST_ONLY";
    public static final String                    BEST_SELECTION_ONLY                   = "BEST_SELECTION_ONLY";
    public static final String                    FAST_LINKCHECK                        = "FAST_LINKCHECK";
    private final String                          REMOVED_VIDEO                         = ">\\s*This video has been removed\\s*<";
    public static final String                    PROPERTY_ACCOUNT_is_cookie_login_only = "is_cookie_login_only";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pornhub.com", "pornhub.org", "pornhubpremium.com", "pornhubpremium.org" });
        return ret;
    }

    @Override
    public String rewriteHost(String host) {
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            String urlPattern = "https?://(?:www\\.|[a-z]{2}\\.)?" + buildHostsPatternPart(domains) + "/(?:photo|(embed)?gif)/\\d+";
            urlPattern += "|https://pornhubdecrypted/.+";
            ret.add(urlPattern);
        }
        return ret.toArray(new String[0]);
    }

    @SuppressWarnings("deprecation")
    public PornHubCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.pornhub.com/create_account");
        Browser.setRequestIntervalLimitGlobal(getHost(), 333);
        this.setConfigElements();
    }

    public void correctDownloadLink(final DownloadLink link) {
        try {
            link.setPluginPatternMatcher(correctAddedURL(link.getPluginPatternMatcher()));
        } catch (PluginException e) {
        }
    }

    @Override
    public String getLinkID(DownloadLink link) {
        final String quality = link.getStringProperty("quality", null);
        final String format = link.getStringProperty("format", null);
        final String viewkey = link.getStringProperty("viewkey", null);
        if (quality != null && viewkey != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("pornhub://");
            sb.append(viewkey);
            sb.append("_");
            if (format != null) {
                sb.append(format);
            } else {
                // older links
                sb.append("mp4");
            }
            sb.append("_");
            sb.append(quality);
            return sb.toString();
        } else {
            return super.getLinkID(link);
        }
    }

    /** Corrects URL based on given/not given premium account (pornhubpremium.com or pornhub.com) */
    public static String correctAddedURL(final String input) throws PluginException {
        final String viewKey = getViewkeyFromURL(input);
        final boolean isPremium = isPremiumFromURL(input);
        if (input.matches(type_photo)) {
            return createPornhubImageLink(isPremium, viewKey, null);
        } else if (input.matches(type_gif_webm)) {
            return createPornhubGifLink(isPremium, viewKey, null);
        } else {
            return createPornhubVideoLink(isPremium, viewKey, null);
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.pornhub.com/terms";
    }

    public static Object RNKEYLOCK = new Object();

    public static Request getPage(Browser br, final Request request) throws Exception {
        br.getPage(request);
        String RNKEY = evalRNKEY(br);
        if (RNKEY != null) {
            int maxLoops = 8;// up to 3 loops in tests
            synchronized (RNKEYLOCK) {
                while (true) {
                    if (RNKEY == null) {
                        return br.getRequest();
                    } else if (--maxLoops > 0) {
                        br.setCookie(br.getHost(), "RNKEY", RNKEY);
                        Thread.sleep(1000 + ((8 - maxLoops) * 500));
                        br.getPage(request.cloneRequest());
                        RNKEY = evalRNKEY(br);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        } else {
            return br.getRequest();
        }
    }

    public static Request getPage(Browser br, final String url) throws Exception {
        return getPage(br, br.createGetRequest(url));
    }

    public static Request getFirstPageWithAccount(Plugin plugin, Account account, Browser br, final String url) throws Exception {
        if (account == null) {
            return getPage(br, url);
        } else {
            synchronized (account) {
                final boolean verifiedLogin = login(plugin, br, account, false);
                final Request request = getPage(br, url);
                if (!isLoggedInHtml(br)) {
                    plugin.getLogger().info("Not logged in?|VerifiedLogin:" + verifiedLogin);
                    login(plugin, br, account, true);
                    return getPage(br, url);
                } else {
                    return request;
                }
            }
        }
    }

    private void checkAvailability(final DownloadLink link, final Browser br) throws PluginException {
        if (br.containsHTML(">\\s*Video has been flagged for verification in accordance with our trust and safety policy.?\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Video has been flagged");
        } else if (br.containsHTML(">\\s*This content is unavailable in your country.?\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "This content is unavailable in your country", 24 * 60 * 60 * 1000l);
        } else if (br.containsHTML("<span>\\s*Video has been removed at the request of")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Video has been removed");
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @SuppressWarnings({ "deprecation", "static-access" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String source_url = link.getStringProperty("mainlink");
        String viewKey = null;
        Boolean isPremiumFromURL = null;
        try {
            final String url = link.getPluginPatternMatcher();
            viewKey = getViewkeyFromURL(url);
            isPremiumFromURL = isPremiumFromURL(url);
        } catch (PluginException e) {
            viewKey = getViewkeyFromURL(source_url);
            isPremiumFromURL = isPremiumFromURL(source_url);
        }
        /* User-chosen quality, set in decrypter */
        String html_filename = null;
        String server_filename = null;
        boolean isVideo = false;
        final String quality = link.getStringProperty("quality", null);
        /*
         * TODO account handling: Prefer account from handlePremium to be sure not to use ANY account for downloading but the account the
         * upper handling is using!
         */
        if (link.getDownloadURL().matches(type_photo)) {
            /* Offline links should also have nice filenames */
            link.setName(viewKey + ".jpg");
            br.setFollowRedirects(true);
            getPage(br, createPornhubImageLink(isPremiumFromURL, viewKey, null));
            if (br.containsHTML(html_privateimage)) {
                final Account account = AccountController.getInstance().getValidAccount(this);
                br.setFollowRedirects(true);
                getFirstPageWithAccount(this, account, br, createPornhubImageLink(isPremiumFromURL, viewKey, account));
                if (br.containsHTML(html_privateimage)) {
                    link.getLinkStatus().setStatusText("You're not authorized to watch/download this private image");
                    return AvailableStatus.TRUE;
                }
            }
            checkAvailability(link, br);
            String ext = null;
            final String gifVideoAsMp4 = br.getRegex("<video class=\"centerImageVid\"[^>]*>\\s+<source src=\"(https://[^\"]+)").getMatch(0);
            if (gifVideoAsMp4 != null) {
                /* "gif" images --> short mp4 videos without sound */
                this.dlUrl = gifVideoAsMp4;
                ext = "mp4";
            } else {
                /* Single image */
                String photoImageSection = br.getRegex("(<div id=\"photoImageSection\">.*?</div>)").getMatch(0);
                if (photoImageSection != null) {
                    dlUrl = new Regex(photoImageSection, "<img src=\"([^<>\"]+)\"").getMatch(0);
                }
                if (dlUrl == null) {
                    dlUrl = br.getRegex("name=\"twitter:image:src\" content=\"(https?[^<>\"]*?\\.[A-Za-z]{3,5})\"").getMatch(0);
                }
                if (dlUrl != null) {
                    ext = dlUrl.substring(dlUrl.lastIndexOf(".") + 1);
                }
            }
            if (dlUrl == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            html_filename = viewKey + "." + ext;
        } else if (link.getDownloadURL().matches(type_gif_webm)) {
            /* Offline links should also have nice filenames */
            link.setName(viewKey + ".webm");
            br.setFollowRedirects(true);
            getPage(br, createPornhubGifLink(isPremiumFromURL, viewKey, null));
            checkAvailability(link, br);
            String title = br.getRegex("class=\"gifTitle\">\\s*?<h1>([^<>\"]+)</h1>").getMatch(0);
            if (title == null) {
                html_filename = viewKey;
            } else {
                html_filename = viewKey + "_" + title;
            }
            html_filename += ".webm";
            dlUrl = br.getRegex("data\\-webm\\s*=\\s*\"(https?[^\"]+\\.webm)\"").getMatch(0);
            if (dlUrl == null) {
                dlUrl = br.getRegex("fileWebm\\s*=\\s*'(https?[^\"]+\\.webm)'").getMatch(0);
            }
            if (dlUrl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            /* Required later if e.g. directurl has to be refreshed! */
            isVideo = true;
            /* Offline links should also have nice filenames */
            link.setName(viewKey + ".mp4");
            html_filename = link.getStringProperty("decryptedfilename", null);
            dlUrl = link.getStringProperty("directlink", null);
            if (dlUrl == null || html_filename == null) {
                /* This should never happen as every url goes into the decrypter first! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            prepBr(br);
            final Account account = AccountController.getInstance().getValidAccount(this);
            br.setFollowRedirects(true);
            getFirstPageWithAccount(this, account, br, createPornhubVideoLink(isPremiumFromURL, viewKey, account));
            checkAvailability(link, br);
            if (br.containsHTML(html_privatevideo)) {
                link.getLinkStatus().setStatusText("You're not authorized to watch/download this private video");
                link.setName(html_filename);
                return AvailableStatus.TRUE;
            } else if (br.containsHTML(html_premium_only)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Premium only File", PluginException.VALUE_ID_PREMIUM_ONLY);
            } else if (br.containsHTML(REMOVED_VIDEO)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(html_purchase_only)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Premium/Purchase only File", PluginException.VALUE_ID_PREMIUM_ONLY);
            } else if (source_url == null || html_filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        String format = link.getStringProperty("format");
        if (format == null) {
            // older links
            format = "mp4";
        }
        server_filename = getFilenameFromURL(dlUrl);
        if (this.getPluginConfig().getBooleanProperty("USE_ORIGINAL_SERVER_FILENAME", false) && server_filename != null) {
            link.setFinalFileName(server_filename);
        } else {
            link.setFinalFileName(html_filename);
        }
        if (!verifyFinalURL(link, this.dlUrl)) {
            if (!isVideo) {
                /* We cannot refresh directurls of e.g. photo content - final downloadurls should be static --> WTF */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
            logger.info("Directurl has expired (?) --> Trying to generate new directurl");
            final Map<String, Map<String, String>> qualities = getVideoLinks(this, br);
            if (qualities == null || qualities.size() == 0) {
                logger.warning("Failed to find any video qualities");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.dlUrl = qualities.containsKey(quality) ? qualities.get(quality).get(format) : null;
            if (this.dlUrl == null) {
                logger.warning("Failed to get fresh directurl:" + format + "/" + quality);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Last chance */
            logger.info("Checking fresh directurl");
            if (!verifyFinalURL(link, this.dlUrl)) {
                logger.info("Fresh directurl did not lead to downloadable content");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to refresh directurl");
            }
        }
        return AvailableStatus.TRUE;
    }

    public boolean verifyFinalURL(final DownloadLink link, final String url) throws Exception {
        final String format = link.getStringProperty("format");
        if (StringUtils.equalsIgnoreCase("hls", format)) {
            final Browser hlsCheck = br.cloneBrowser();
            hlsCheck.setFollowRedirects(true);
            hlsCheck.setAllowedResponseCodes(new int[] { -1 });
            hlsCheck.getPage(url);
            if (hlsCheck.getHttpConnection().getResponseCode() != 200) {
                return false;
            } else if (!StringUtils.containsIgnoreCase(hlsCheck.getHttpConnection().getContentType(), "vnd.apple.mpegurl")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                final List<HlsContainer> hlsContainers = HlsContainer.getHlsQualities(hlsCheck);
                if (hlsContainers == null || hlsContainers.size() != 1) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    final List<M3U8Playlist> m3u8 = hlsContainers.get(0).getM3U8(hlsCheck);
                    URLConnectionAdapter con = null;
                    try {
                        con = hlsCheck.openHeadConnection(m3u8.get(0).getSegment(0).getUrl());
                        if (con.getResponseCode() != 200) {
                            return false;
                        } else if (StringUtils.containsIgnoreCase(con.getContentType(), "text")) {
                            return false;
                        } else {
                            link.setProperty("directlink", url);
                            return true;
                        }
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }
                }
            }
        } else {
            final Browser urlCheck = br.cloneBrowser();
            urlCheck.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = urlCheck.openHeadConnection(dlUrl);
                if (urlCheck.getHttpConnection().getResponseCode() != 200) {
                    try {
                        urlCheck.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    return false;
                } else if (StringUtils.containsIgnoreCase(urlCheck.getHttpConnection().getContentType(), "text")) {
                    try {
                        urlCheck.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    if (con.getLongContentLength() > 0) {
                        link.setDownloadSize(con.getLongContentLength());
                    }
                    link.setProperty("directlink", url);
                    return true;
                }
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
    }

    public static String getFilenameFromURL(final String url) {
        if (url == null) {
            return null;
        } else {
            /* 2019-07-15: TODO: Maybe add support for other directurls as well (e.g. pictures) */
            final String ret = new Regex(url, "/([^/]+\\.mp4)").getMatch(0);
            if (StringUtils.isEmpty(ret)) {
                return null;
            } else {
                return ret;
            }
        }
    }

    @SuppressWarnings({ "unchecked" })
    public static Map<String, Map<String, String>> getVideoLinks(Plugin plugin, final Browser br) throws Exception {
        boolean success = false;
        final Map<String, Map<String, String>> qualities = new LinkedHashMap<String, Map<String, String>>();
        String flashVars = br.getRegex("\\'flashvars\\' :[\t\n\r ]+\\{([^\\}]+)").getMatch(0);
        if (flashVars == null) {
            flashVars = br.getRegex("(var\\s*flashvars_\\d+.+quality_\\d+p;)\\s*playerObj").getMatch(0);
            if (flashVars == null) {
                flashVars = br.getRegex("(var\\s*flashvars_\\d+.+quality_\\d+p;)\\s+").getMatch(0);
            }
            if (flashVars == null) {
                /* Wider */
                flashVars = br.getRegex("(var\\s*flashvars_\\d+.+\\['url'\\]\\s*=\\s*quality_\\d+p;)").getMatch(0);
            }
            if (flashVars == null) {
                /* Wide open - risky */
                flashVars = br.getRegex("(var\\s*flashvars_\\d+.*?)(loadScriptUniqueId|</script)").getMatch(0);
            }
            final String flashVarsID = new Regex(flashVars, "flashvars_(\\d+)").getMatch(0);
            if (flashVarsID != null) {
                try {
                    flashVars = flashVars.replaceFirst("(?s)(playerObjList.+)", "");
                    final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(plugin);
                    final ScriptEngine engine = manager.getEngineByName("javascript");
                    engine.eval(flashVars + "var result=JSON.stringify(flashvars_" + flashVarsID + ");");
                    flashVars = String.valueOf(engine.get("result"));
                } catch (final Exception e) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
                }
            }
        }
        if (flashVars != null) {
            final Object obj = flashVars == null ? null : JavaScriptEngineFactory.jsonToJavaObject(flashVars);
            if (obj instanceof Map) {
                final Map<String, Object> values = (Map<String, Object>) obj;
                if (values == null || values.size() < 1) {
                    return null;
                }
                String dllink_temp = null;
                // dllink_temp = (String) values.get("video_url");
                final ArrayList<Object> entries = (ArrayList<Object>) values.get("mediaDefinitions");
                if (entries.size() == 0) {
                    /*
                     * 2019-04-30: Very rare case - video is supposed to be online but ZERO qualities are available --> Video won't load in
                     * browser either --> Offline
                     */
                    return qualities;
                }
                for (Object entry : entries) {
                    final LinkedHashMap<String, Object> e = (LinkedHashMap<String, Object>) entry;
                    String format = (String) e.get("format");
                    if (StringUtils.equalsIgnoreCase(format, "dash")) {
                        plugin.getLogger().info("Dash not yet supported");
                        continue;
                    }
                    format = format.toLowerCase(Locale.ENGLISH);
                    final Object qualityInfo = e.get("quality");
                    if (qualityInfo == null) {
                        continue;
                    } else if (qualityInfo instanceof List) {
                        // HLS with auto quality
                        continue;
                    }
                    dllink_temp = (String) e.get("videoUrl");
                    final Boolean encrypted = e.get("encrypted") == null ? null : ((Boolean) e.get("encrypted")).booleanValue();
                    if (encrypted == Boolean.TRUE) {
                        final String decryptkey = (String) values.get("video_title");
                        try {
                            dllink_temp = new BouncyCastleAESCounterModeDecrypt().decrypt(dllink_temp, decryptkey, 256);
                        } catch (Throwable t) {
                            /* Fallback for stable version */
                            dllink_temp = AESCounterModeDecrypt(dllink_temp, decryptkey, 256);
                        }
                        if (dllink_temp != null && (dllink_temp.startsWith("Error:") || !dllink_temp.startsWith("http"))) {
                            success = false;
                        } else {
                            success = true;
                        }
                    } else {
                        success = true;
                    }
                    final String quality = new Regex(qualityInfo.toString(), "(\\d+)").getMatch(0);
                    Map<String, String> formatMap = qualities.get(quality);
                    if (formatMap == null) {
                        formatMap = new HashMap<String, String>();
                        qualities.put(quality, formatMap);
                    }
                    formatMap.put(format, dllink_temp);
                }
            }
        }
        if (!success) {
            String[][] var_player_quality_dp;
            /* 0 = match for quality, 1 = match for url */
            int[] matchPlaces;
            if (isLoggedInHtml(br) && use_download_workarounds) {
                /*
                 * 2017-02-10: Workaround - download via official downloadlinks if the user has an account. Grab official downloadlinks via
                 * free/premium account. Keep in mind: Not all videos have official downloadlinks available for account mode - example:
                 * ph58072cd969005
                 */
                var_player_quality_dp = br.getRegex("href=\"(https?[^<>\"]+)\"><i></i><span>[^<]*?</span>\\s*?(\\d+)p\\s*?</a").getMatches();
                matchPlaces = new int[] { 1, 0 };
            } else {
                /* Normal stream download handling. */
                /* 2017-02-07: seems they have seperated into multiple vars to block automated download tools. */
                var_player_quality_dp = br.getRegex("var player_quality_(1080|720|480|360|240)p[^=]*?=\\s*('|\")(https?://.*?)\\2\\s*;").getMatches();
                matchPlaces = new int[] { 0, 2 };
            }
            if (var_player_quality_dp == null || var_player_quality_dp.length == 0) {
                String fvjs = br.getRegex("javascript\">\\s*(var flashvars[^;]+;)").getMatch(0);
                Pattern p = Pattern.compile("^\\s*?(var.*?var qualityItems_[\\d]* =.*?)$", Pattern.MULTILINE);
                String qualityItems = br.getRegex(p).getMatch(0);
                if (qualityItems != null) {
                    String[][] qs = new Regex(qualityItems, "var (quality_([^=]+?)p)=").getMatches();
                    final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
                    final ScriptEngine engine = manager.getEngineByName("javascript");
                    engine.eval(fvjs);
                    engine.eval(qualityItems);
                    for (int i = 0; i < qs.length; i++) {
                        final String url = engine.get(qs[i][0]).toString();
                        final String quality = qs[i][1];
                        Map<String, String> formatMap = qualities.get(quality);
                        if (formatMap == null) {
                            formatMap = new HashMap<String, String>();
                            qualities.put(quality, formatMap);
                        }
                        if (StringUtils.isNotEmpty(url)) {
                            if (StringUtils.containsIgnoreCase(url, "m3u8")) {
                                formatMap.put("hls", url);
                            } else {
                                formatMap.put("mp4", url);
                            }
                        }
                    }
                    return qualities;
                }
            }
            /*
             * Check if we have links - if not, the video might not have any official downloadlinks available or our previous code failed
             * for whatever reason.
             */
            if (var_player_quality_dp == null || var_player_quality_dp.length == 0) {
                /* Last chance fallback to embedded video. */
                /* 2017-02-09: For embed player - usually only 480p will be available. */
                /* Access embed video URL. */
                /* viewkey should never be null! */
                final String viewkey = getViewkeyFromURL(br.getURL());
                if (viewkey != null) {
                    final Browser brc = br.cloneBrowser();
                    getPage(brc, createPornhubVideoLinkEmbedFree(brc, viewkey));
                    var_player_quality_dp = brc.getRegex("\"quality_(\\d+)p\"\\s*?:\\s*?\"(https?[^\"]+)\"").getMatches();
                    matchPlaces = new int[] { 0, 1 };
                }
            }
            final int matchQuality = matchPlaces[0];
            final int matchUrl = matchPlaces[1];
            for (final String quality : new String[] { "1080", "720", "480", "360", "240" }) {
                for (final String[] var : var_player_quality_dp) {
                    // so far any of these links will work.
                    if (var[matchQuality].equals(quality)) {
                        String url = var[matchUrl];
                        url = url.replaceAll("( |\"|\\+)", "");
                        url = Encoding.unicodeDecode(url);
                        Map<String, String> formatMap = qualities.get(quality);
                        if (formatMap == null) {
                            formatMap = new HashMap<String, String>();
                            qualities.put(quality, formatMap);
                        }
                        if (StringUtils.isNotEmpty(url)) {
                            if (StringUtils.containsIgnoreCase(url, "m3u8")) {
                                formatMap.put("hls", url);
                            } else {
                                formatMap.put("mp4", url);
                            }
                        }
                    }
                }
            }
        }
        return qualities;
    }

    public static String getUserName(final Plugin plugin, final Browser br) {
        String ret = br.getRegex("\"username\"\\s*:\\s*(\".*?\")").getMatch(0);
        if (ret != null) {
            ret = JSonStorage.restoreFromString(ret, TypeRef.STRING);
        } else {
            ret = br.getRegex("/(?:model|users)/[^\"]+\"\\s*class\\s*=\\s*\"bolded\"\\s*>\\s*([^<>]+)\\s*</a>").getMatch(0);
        }
        return ret;
    }

    public static String getSiteTitle(final Plugin plugin, final Browser br) {
        String site_title = br.getRegex("<title>\\s*([^<>]*?)\\s*\\-\\s*Pornhub(\\.com)?\\s*</title>").getMatch(0);
        if (site_title == null) {
            site_title = br.getRegex("\"section_title overflow\\-title overflow\\-title\\-width\">([^<>]*?)</h1>").getMatch(0);
            if (site_title == null) {
                site_title = br.getRegex("<meta property\\s*=\\s*\"og:title\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
            }
        }
        if (site_title != null) {
            site_title = Encoding.htmlDecode(site_title);
            site_title = plugin.encodeUnicode(site_title);
        }
        return site_title;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    @SuppressWarnings("deprecation")
    private void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        final String format = downloadLink.getStringProperty("format");
        if (StringUtils.equalsIgnoreCase(format, "hls")) {
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            final List<HlsContainer> hlsContainers = HlsContainer.getHlsQualities(br, dlUrl);
            if (hlsContainers == null || hlsContainers.size() != 1) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new HLSDownloader(downloadLink, br, hlsContainers.get(0).getDownloadurl());
            dl.startDownload();
        } else {
            final boolean resume;
            final int maxchunks;
            if (downloadLink.getDownloadURL().matches(type_photo)) {
                resume = true;
                /* We only have small pictures --> No chunkload needed */
                maxchunks = 1;
                requestFileInformation(downloadLink);
            } else {
                resume = ACCOUNT_FREE_RESUME;
                maxchunks = ACCOUNT_FREE_MAXCHUNKS;
                if (br.containsHTML(html_privatevideo)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "You're not authorized to watch/download this private video");
                }
                if (dlUrl == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dlUrl, resume, maxchunks);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static final String PORNHUB_FREE      = "pornhub.com";
    private static final String PORNHUB_PREMIUM   = "pornhubpremium.com";
    private static final String COOKIE_ID_FREE    = "v2_free";
    private static final String COOKIE_ID_PREMIUM = "v2_premium";

    private static void setAccountType(Account account, Account.AccountType type) {
        account.setType(type);
        if (Account.AccountType.PREMIUM.equals(type)) {
            /* Premium accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
        } else {
            /* Free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
        }
    }

    public static boolean login(Plugin plugin, final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                /* 2017-01-25: Important - we often have redirects! */
                br.setFollowRedirects(true);
                prepBr(br);
                final Cookies freeCookies = account.loadCookies(COOKIE_ID_FREE);
                final Cookies premiumCookies = account.loadCookies(COOKIE_ID_PREMIUM);
                final boolean is_cookie_only_login = account.getBooleanProperty(PROPERTY_ACCOUNT_is_cookie_login_only, false);
                final boolean cookiesOk = freeCookies != null && premiumCookies != null && (isLoggedInFreeCookieFree(freeCookies) || isLoggedInPremiumCookie(premiumCookies));
                if (!force && cookiesOk && (System.currentTimeMillis() - account.getCookiesTimeStamp(COOKIE_ID_FREE) <= trust_cookie_age) && (System.currentTimeMillis() - account.getCookiesTimeStamp(COOKIE_ID_PREMIUM) <= trust_cookie_age)) {
                    br.setCookies(getProtocolFree() + PORNHUB_FREE, freeCookies);
                    br.setCookies(getProtocolPremium() + PORNHUB_PREMIUM, premiumCookies);
                    plugin.getLogger().info("Trust login cookies:" + account.getType());
                    /* We trust these cookies --> Do not check them */
                    return false;
                }
                if (freeCookies != null && premiumCookies != null) {
                    /* Check cookies - only perform a full login if they're not valid anymore. */
                    br.setCookies(getProtocolFree() + PORNHUB_FREE, freeCookies);
                    br.setCookies(getProtocolPremium() + PORNHUB_PREMIUM, premiumCookies);
                    if (AccountType.PREMIUM.equals(account.getType())) {
                        // fast check
                        getPage(br, (getProtocolPremium() + "pornhubpremium.com/user/login_status?ajax=1"));
                        if (br.containsHTML("\"success\"\\s*:\\s*(\"1\"|true)") && isLoggedInPremiumCookie(br.getCookies(PORNHUB_PREMIUM))) {
                            setAccountType(account, AccountType.PREMIUM);
                            plugin.getLogger().info("Verified(fast) premium->premium login cookies:" + account.getType());
                            saveCookies(br, account);
                            return true;
                        } else {
                            // slow check
                            getPage(br, (getProtocolPremium() + PORNHUB_PREMIUM));
                            if (isLoggedInHtmlPremium(br)) {
                                setAccountType(account, AccountType.PREMIUM);
                                plugin.getLogger().info("Verified(slow) premium->premium login cookies:" + account.getType());
                                saveCookies(br, account);
                                return true;
                            } else if (isLoggedInHtmlFree(br)) {
                                setAccountType(account, AccountType.FREE);
                                plugin.getLogger().info("Verified(slow) premium->free login cookies:" + account.getType());
                                saveCookies(br, account);
                                return true;
                            }
                        }
                    } else {
                        getPage(br, (getProtocolFree() + "www." + PORNHUB_FREE));
                        if (br.containsHTML("/authenticate/goToLoggedIn\"\\s*>\\s*Access your Pornhub Premium")) {
                            // 2020-12-29 - no auto redirect to pornhub premium
                            getPage(br, "/authenticate/goToLoggedIn");
                        }
                        if (isLoggedInHtmlFree(br)) {
                            setAccountType(account, AccountType.FREE);
                            plugin.getLogger().info("Verified(slow) free->free login cookies:" + account.getType());
                            saveCookies(br, account);
                            return true;
                        } else {
                            getPage(br, (getProtocolPremium() + "pornhubpremium.com/user/login_status?ajax=1"));
                            if (br.containsHTML("\"success\"\\s*:\\s*(\"1\"|true)") && isLoggedInPremiumCookie(br.getCookies(PORNHUB_PREMIUM))) {
                                setAccountType(account, AccountType.PREMIUM);
                                plugin.getLogger().info("Verified(fast) free->premium login cookies:" + account.getType());
                                saveCookies(br, account);
                                return true;
                            }
                        }
                    }
                    br.clearCookies(PORNHUB_FREE);
                    br.clearCookies(PORNHUB_PREMIUM);
                    plugin.getLogger().info("Cached login cookies failed:" + account.getType());
                }
                plugin.getLogger().info("Performing full login");
                if (is_cookie_only_login) {
                    plugin.getLogger().info("Cannot perform full login because this account only has cookies available");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                getPage(br, "https://www." + account.getHoster());
                getPage(br, "https://www." + account.getHoster() + "/login");
                if (br.containsHTML("Sorry we couldn't find what you were looking for")) {
                    getPage(br, "https://www." + account.getHoster() + "/login");
                }
                final Form loginform = br.getFormbyKey("username");
                // final String login_key = br.getRegex("id=\"login_key\" value=\"([^<>\"]*?)\"").getMatch(0);
                // final String login_hash = br.getRegex("id=\"login_hash\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", account.getUser());
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember", "1");
                loginform.setMethod(MethodType.POST);
                loginform.setAction("/front/authenticate");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.submitForm(loginform);
                // final String success = PluginJSonUtils.getJsonValue(br, "success");
                final String redirect = PluginJSonUtils.getJsonValue(br, "redirect");
                final String username = PluginJSonUtils.getJsonValue(br, "username");
                if (redirect != null && (redirect.startsWith("http") || redirect.startsWith("/"))) {
                    /* Required to get the (premium) cookies (multiple redirects). */
                    final boolean premiumExpired = redirect.contains(PORNHUB_PREMIUM) && redirect.contains("expired");
                    getPage(br, redirect);
                    if (premiumExpired && !br.getURL().contains(PORNHUB_FREE)) {
                        /*
                         * Expired pornhub premium --> It should still be a valid free account --> We might need to access a special url
                         * which redirects us to the pornhub free mainpage and sets the cookies.
                         */
                        final String pornhubMainpageCookieRedirectUrl = br.getRegex("\\'pornhubLink\\'\\s*?:\\s*?(?:\"|\\')(https?://(?:www\\.)?pornhub\\.(?:com|org)/[^<>\"\\']+)(?:\"|\\')").getMatch(0);
                        if (pornhubMainpageCookieRedirectUrl != null) {
                            getPage(br, pornhubMainpageCookieRedirectUrl);
                        }
                    }
                }
                if (!isLoggedInHtml(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (StringUtils.isEmpty(username)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (br.containsHTML("/authenticate/goToLoggedIn\"\\s*>\\s*Access your Pornhub Premium")) {
                    // 2020-12-29 - no auto redirect to pornhub premium
                    getPage(br, "/authenticate/goToLoggedIn");
                }
                if (isLoggedInHtmlPremium(br)) {
                    setAccountType(account, AccountType.PREMIUM);
                } else {
                    setAccountType(account, AccountType.FREE);
                }
                plugin.getLogger().info("Verified fresh login:" + account.getType());
                saveCookies(br, account);
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies(COOKIE_ID_FREE);
                    account.clearCookies(COOKIE_ID_PREMIUM);
                }
                throw e;
            }
        }
    }

    public static boolean isLoggedInHtml(final Browser br) {
        return br != null && br.containsHTML("class\\s*=\\s*\"signOut\"|/premium/lander\"\\s*>\\s*Logout\\s*<");
    }

    public static boolean isLoggedInHtmlPremium(final Browser br) {
        return br != null && br.getURL() != null && br.getURL().contains(PORNHUB_PREMIUM) && isLoggedInPremiumCookie(br.getCookies(getProtocolPremium() + PORNHUB_PREMIUM)) && isLoggedInHtml(br);
    }

    public static boolean isLoggedInHtmlFree(final Browser br) {
        return br != null && br.getURL() != null && !br.getURL().contains(PORNHUB_FREE) && isLoggedInFreeCookieFree(br.getCookies(getProtocolFree() + PORNHUB_FREE)) && isLoggedInHtml(br);
    }

    public static boolean isLoggedInFreeCookieFree(Cookies cookies) {
        return cookies != null && (cookies.get("gateway_security_key", Cookies.NOTDELETEDPATTERN) != null || cookies.get("il", Cookies.NOTDELETEDPATTERN) != null);
    }

    public static boolean isLoggedInPremiumCookie(Cookies cookies) {
        return cookies != null && (cookies.get("gateway_security_key", Cookies.NOTDELETEDPATTERN) != null || cookies.get("il", Cookies.NOTDELETEDPATTERN) != null);
    }

    public static void saveCookies(final Browser br, final Account acc) {
        acc.saveCookies(br.getCookies(getProtocolPremium() + PORNHUB_PREMIUM), COOKIE_ID_PREMIUM);
        acc.saveCookies(br.getCookies(getProtocolFree() + PORNHUB_FREE), COOKIE_ID_FREE);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (account) {
            final boolean isCookieLoginOnly = account.getBooleanProperty(PROPERTY_ACCOUNT_is_cookie_login_only, false);
            login(this, br, account, true);
            ai.setUnlimitedTraffic();
            if (AccountType.PREMIUM.equals(account.getType())) {
                ai.setStatus("Premium Account");
                if (isCookieLoginOnly) {
                    /* Never modify previous AccountInfo if we're logged in via cookies only! */
                    return account.getAccountInfo();
                } else {
                    return ai;
                }
            } else {
                ai.setStatus("Free Account");
                return ai;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* No need to login as we're already logged in. */
        doFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    /**
     * AES CTR(Counter) Mode for Java ported from AES-CTR-Mode implementation in JavaScript by Chris Veness
     *
     * @see <a href=
     *      "http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf">"Recommendation for Block Cipher Modes of Operation - Methods and Techniques"</a>
     */
    public static String AESCounterModeDecrypt(String cipherText, String key, int nBits) throws Exception {
        if (!(nBits == 128 || nBits == 192 || nBits == 256)) {
            return "Error: Must be a key mode of either 128, 192, 256 bits";
        }
        if (cipherText == null || key == null) {
            return "Error: cipher and/or key equals null";
        }
        String res = null;
        nBits = nBits / 8;
        byte[] data = Base64.decode(cipherText.toCharArray());
        /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
        byte[] k = Arrays.copyOf(key.getBytes(), nBits);
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        SecretKey secretKey = generateSecretKey(k, nBits);
        byte[] nonceBytes = Arrays.copyOf(Arrays.copyOf(data, 8), nBits / 2);
        IvParameterSpec nonce = new IvParameterSpec(nonceBytes);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, nonce);
        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
        res = new String(cipher.doFinal(data, 8, data.length - 8));
        return res;
    }

    public static SecretKey generateSecretKey(byte[] keyBytes, int nBits) throws Exception {
        try {
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            keyBytes = cipher.doFinal(keyBytes);
        } catch (InvalidKeyException e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unlimited Strength JCE Policy Files needed!", e);
        } catch (Throwable e1) {
            LogController.CL().log(e1);
        }
        System.arraycopy(keyBytes, 0, keyBytes, nBits / 2, nBits / 2);
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static class BouncyCastleAESCounterModeDecrypt {
        private String decrypt(String cipherText, String key, int nBits) throws Exception {
            if (!(nBits == 128 || nBits == 192 || nBits == 256)) {
                return "Error: Must be a key mode of either 128, 192, 256 bits";
            }
            if (cipherText == null || key == null) {
                return "Error: cipher and/or key equals null";
            }
            byte[] decrypted;
            nBits = nBits / 8;
            byte[] data = Base64.decode(cipherText.toCharArray());
            /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
            byte[] k = Arrays.copyOf(key.getBytes(), nBits);
            /* AES/CTR/NoPadding (SIC == CTR) */
            org.bouncycastle.crypto.BufferedBlockCipher cipher = new org.bouncycastle.crypto.BufferedBlockCipher(new org.bouncycastle.crypto.modes.SICBlockCipher(new org.bouncycastle.crypto.engines.AESEngine()));
            cipher.reset();
            SecretKey secretKey = generateSecretKey(k, nBits);
            byte[] nonceBytes = Arrays.copyOf(Arrays.copyOf(data, 8), nBits / 2);
            IvParameterSpec nonce = new IvParameterSpec(nonceBytes);
            /* true == encrypt; false == decrypt */
            cipher.init(true, new org.bouncycastle.crypto.params.ParametersWithIV(new org.bouncycastle.crypto.params.KeyParameter(secretKey.getEncoded()), nonce.getIV()));
            decrypted = new byte[cipher.getOutputSize(data.length - 8)];
            int decLength = cipher.processBytes(data, 8, data.length - 8, decrypted, 0);
            cipher.doFinal(decrypted, decLength);
            /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
            return new String(decrypted);
        }

        private SecretKey generateSecretKey(byte[] keyBytes, int nBits) throws Exception {
            try {
                SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
                /* AES/ECB/NoPadding */
                org.bouncycastle.crypto.BufferedBlockCipher cipher = new org.bouncycastle.crypto.BufferedBlockCipher(new org.bouncycastle.crypto.engines.AESEngine());
                cipher.init(true, new org.bouncycastle.crypto.params.KeyParameter(secretKey.getEncoded()));
                keyBytes = new byte[cipher.getOutputSize(secretKey.getEncoded().length)];
                int decLength = cipher.processBytes(secretKey.getEncoded(), 0, secretKey.getEncoded().length, keyBytes, 0);
                cipher.doFinal(keyBytes, decLength);
            } catch (Throwable e) {
                return null;
            }
            System.arraycopy(keyBytes, 0, keyBytes, nBits / 2, nBits / 2);
            return new SecretKeySpec(keyBytes, "AES");
        }
    }

    public static Browser prepBr(final Browser br) {
        // br.setCookie("http://pornhub.com/", "platform", "pc");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:44.0) Gecko/20100101 Firefox/44.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.8,de;q=0.6");
        br.getHeaders().put("Accept-Charset", null);
        br.setLoadLimit(br.getDefaultLoadLimit() * 4);
        return br;
    }

    public static String createPornhubImageLink(final Boolean isPremiumURL, final String viewkey, final Account acc) {
        if (Boolean.TRUE.equals(isPremiumURL)) {
            /* Premium url */
            return getProtocolPremium() + "www.pornhubpremium.com/photo/" + viewkey;
        } else {
            /* Free url */
            return getProtocolFree() + "www.pornhub.com/photo/" + viewkey;
        }
    }

    public static String createPornhubGifLink(final Boolean isPremiumURL, final String viewkey, final Account acc) {
        if (Boolean.TRUE.equals(isPremiumURL)) {
            /* Premium url */
            return getProtocolPremium() + "www.pornhubpremium.com/gif/" + viewkey;
        } else {
            /* Free url */
            return getProtocolFree() + "www.pornhub.com/gif/" + viewkey;
        }
    }

    public static String createPornhubVideoLink(final Boolean isPremiumURL, final String viewkey, final Account acc) {
        if (Boolean.TRUE.equals(isPremiumURL)) {
            /* Premium url */
            return getProtocolPremium() + "www.pornhubpremium.com/view_video.php?viewkey=" + viewkey;
        } else {
            /* Free url */
            return getProtocolFree() + "www.pornhub.com/view_video.php?viewkey=" + viewkey;
        }
    }

    public static String createPornhubVideoLinkEmbedFree(final Browser br, final String viewkey) {
        if (isLoggedInHtmlPremium(br)) {
            return createPornhubVideoLinkEmbedPremium(viewkey);
        } else {
            return createPornhubVideoLinkEmbedFree(viewkey);
        }
    }

    public static boolean isPremiumFromURL(final String url) {
        if (url != null && StringUtils.containsIgnoreCase(url, "pornhubpremium.com/")) {
            return true;
        } else {
            return false;
        }
    }

    public static String getViewkeyFromURL(final String url) throws PluginException {
        if (StringUtils.isEmpty(url)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            final String ret;
            if (url.matches(type_photo)) {
                ret = new Regex(url, "photo/([A-Za-z0-9\\-_]+)$").getMatch(0);
            } else if (url.matches(type_gif_webm)) {
                ret = new Regex(url, "gif/([A-Za-z0-9\\-_]+)$").getMatch(0);
            } else if (url.matches("(?i).+/embed/.+")) {
                ret = new Regex(url, "/embed/([a-z0-9]+)").getMatch(0);
            } else {
                ret = new Regex(url, "viewkey=([a-z0-9]+)").getMatch(0);
            }
            if (StringUtils.isEmpty(ret) || "null".equals(ret)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            return ret;
        }
    }

    /** Returns embed url for free- and free account mode. */
    public static String createPornhubVideoLinkEmbedFree(final String viewkey) {
        return String.format("https://www.pornhub.com/embed/%s", viewkey);
    }

    /** Returns embed url for premium account mode. */
    public static String createPornhubVideoLinkEmbedPremium(final String viewkey) {
        return String.format("https://www.pornhub.com/embed/%s", viewkey);
    }

    private boolean isVideo(final String url) {
        return url != null && url.contains("viewkey=");
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink downloadLink, final PluginForHost buildForThisPlugin) {
        if (buildForThisPlugin != null && !StringUtils.equals(this.getHost(), buildForThisPlugin.getHost())) {
            return downloadLink.getStringProperty("mainlink", null);
        } else {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        }
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        if (!downloadLink.isEnabled() && "".equals(downloadLink.getPluginPatternMatcher())) {
            /*
             * setMultiHostSupport uses a dummy DownloadLink, with isEnabled == false. we must set to true for the host to be added to the
             * supported host array.
             */
            return true;
        } else {
            /* Original plugin can handle every kind of URL! */
            final boolean downloadViaSourcePlugin = downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
            /* MOCHs can only handle video URLs! */
            final boolean downloadViaMOCH = !downloadLink.getHost().equalsIgnoreCase(plugin.getHost()) && isVideo(downloadLink.getStringProperty("mainlink", null));
            return downloadViaSourcePlugin || downloadViaMOCH;
        }
    }

    public static String getProtocolPremium() {
        return "https://";
    }

    public static String getProtocolFree() {
        return "https://";
    }

    public final static String evalRNKEY(Browser br) throws ScriptException {
        if (br.containsHTML("document.cookie=\"RNKEY") && br.containsHTML("leastFactor")) {
            ScriptEngineManager mgr = JavaScriptEngineFactory.getScriptEngineManager(null);
            ScriptEngine engine = mgr.getEngineByName("JavaScript");
            String js = br.toString();
            js = new Regex(js, "<script.*?>(?:<!--)?(.*?)(?://-->)?</script>").getMatch(0);
            js = js.replace("document.cookie=", "return ");
            js = js.replaceAll("(/\\*.*?\\*/)", "");
            engine.eval(js + " var ret=go();");
            final String answer = (engine.get("ret").toString());
            return new Regex(answer, "RNKEY=(.+)").getMatch(0);
        } else {
            return null;
        }
    }

    @Override
    public String getDescription() {
        return "JDownloader's Pornhub plugin helps downloading videoclips from pornhub.com.";
    }

    private void setConfigElements() {
        final ConfigEntry best = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), BEST_ONLY, "Always only grab the best resolution available?").setDefaultValue(false);
        getConfig().addEntry(best);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), BEST_SELECTION_ONLY, "Only grab best of selected resolutions below?").setDefaultValue(false).setEnabledCondidtion(best, false));
        final Iterator<Entry<String, String[]>> it = formats.entrySet().iterator();
        while (it.hasNext()) {
            /*
             * Format-name:videoCodec, videoBitrate, videoResolution, audioCodec, audioBitrate
             */
            String usertext = "Load ";
            final Entry<String, String[]> videntry = it.next();
            final String internalname = videntry.getKey();
            final String[] vidinfo = videntry.getValue();
            final String videoCodec = vidinfo[0];
            final String videoBitrate = vidinfo[1];
            final String videoResolution = vidinfo[2];
            final String audioCodec = vidinfo[3];
            final String audioBitrate = vidinfo[4];
            if (videoCodec != null) {
                usertext += videoCodec + " ";
            }
            if (videoBitrate != null) {
                usertext += videoBitrate + " ";
            }
            if (videoResolution != null) {
                usertext += videoResolution + " ";
            }
            if (audioCodec != null || audioBitrate != null) {
                usertext += "with audio ";
                if (audioCodec != null) {
                    usertext += audioCodec + " ";
                }
                if (audioBitrate != null) {
                    usertext += audioBitrate;
                }
            }
            if (usertext.endsWith(" ")) {
                usertext = usertext.substring(0, usertext.lastIndexOf(" "));
            }
            final ConfigEntry vidcfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), internalname, usertext).setDefaultValue(true).setEnabledCondidtion(best, false);
            getConfig().addEntry(vidcfg);
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_LINKCHECK, "Enable fast linkcheck?\r\nNOTE: If enabled, links will appear faster but filesize won't be shown before downloadstart.").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_ORIGINAL_SERVER_FILENAME", "Use original server filename?").setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}