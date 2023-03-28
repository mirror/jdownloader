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
import java.net.MalformedURLException;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcrawler.LinkCrawlerDeepInspector;
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
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.PornHubComVideoCrawler;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPConnection;
import org.appwork.utils.net.httpconnection.SSLSocketStreamOptions;
import org.appwork.utils.net.httpconnection.SSLSocketStreamOptionsModifier;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.net.BCSSLSocketStreamFactory;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { PornHubComVideoCrawler.class })
public class PornHubCom extends PluginForHost {
    /* Connection stuff */
    // private static final boolean FREE_RESUME = true;
    // private static final int FREE_MAXCHUNKS = 0;
    private static final int                      FREE_MAXDOWNLOADS                     = 5;
    private static final boolean                  ACCOUNT_FREE_RESUME                   = true;
    private static final int                      ACCOUNT_FREE_MAXCHUNKS                = 0;
    private static final int                      ACCOUNT_FREE_MAXDOWNLOADS             = 5;
    public static final boolean                   use_download_workarounds              = true;
    private static final String                   type_photo                            = "(?i).+/photo/\\d+";
    private static final String                   type_gif_webm                         = "(?i).+/(embed)?gif/\\d+";
    private static final String                   type_modelhub                         = "(?i).+modelhub\\.com/.+";
    public static final String                    html_privatevideo                     = "id=\"iconLocked\"";
    public static final String                    html_privateimage                     = "profile/private-lock\\.png";
    public static final String                    html_purchase_only                    = "(?i)'Buy on video player'";
    public static final String                    html_premium_only                     = "(?i)<h2>\\s*Upgrade to Pornhub Premium to enjoy this video\\.</h2>";
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
    public static final String                    CRAWL_VIDEO_HLS                       = "CRAWL_VIDEO_HLS";
    public static final String                    CRAWL_VIDEO_MP4                       = "CRAWL_VIDEO_MP4";
    public static final String                    CRAWL_THUMBNAIL                       = "CRAWL_THUMBNAIL";
    public static final String                    FAST_LINKCHECK                        = "FAST_LINKCHECK";
    private static final String                   USE_ORIGINAL_SERVER_FILENAME          = "USE_ORIGINAL_SERVER_FILENAME";
    public static final String                    GIFS_WEBM                             = "GIFS_WEBM";
    private final String                          REMOVED_VIDEO                         = ">\\s*This video has been removed\\s*<";
    public static final String                    PROPERTY_FORMAT                       = "format";
    public static final String                    PROPERTY_QUALITY                      = "quality";
    public static final String                    PROPERTY_DIRECTLINK                   = "directlink";
    public static final String                    PROPERTY_DATE                         = "date";
    public static final String                    PROPERTY_CATEGORIES_COMMA_SEPARATED   = "categories_comma_separated";
    public static final String                    PROPERTY_TAGS_COMMA_SEPARATED         = "tags_comma_separated";
    public static final String                    PROPERTY_ACTORS_COMMA_SEPARATED       = "actors_comma_separated";
    public static final String                    PROPERTY_USERNAME                     = "username";
    public static final String                    PROPERTY_VIEWKEY                      = "viewkey";
    public static final String                    PROPERTY_ACCOUNT_is_cookie_login_only = "is_cookie_login_only";

    public static List<String[]> getPluginDomains() {
        return PornHubComVideoCrawler.getPluginDomains();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
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

    private boolean isSupportedDomain(final String domain) {
        for (final String domainTmp : getAnnotationNames()) {
            if (domainTmp.equalsIgnoreCase(domain)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public PornHubCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.pornhub.com/create_account");
        this.setConfigElements();
    }

    public static void setSSLSocketStreamOptions(Browser br) {
        br.setSSLSocketStreamOptions(new SSLSocketStreamOptionsModifier() {
            @Override
            public SSLSocketStreamOptions modify(SSLSocketStreamOptions sslSocketStreamOptions, HTTPConnection httpConnection) {
                final SSLSocketStreamOptions ret = new SSLSocketStreamOptions(sslSocketStreamOptions) {
                    public org.appwork.utils.net.httpconnection.SSLSocketStreamFactory getSSLSocketStreamFactory() {
                        return new BCSSLSocketStreamFactory();
                    };
                };
                ret.getDisabledCipherSuites().clear();
                // ret.getCustomFactorySettings().add("JSSE_TLS1.3_ENABLED");
                ret.getCustomFactorySettings().add("BC_TLS1.3_ENABLED");
                return ret;
            }
        });
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser ret = super.createNewBrowserInstance();
        setSSLSocketStreamOptions(ret);
        return ret;
    }

    @Override
    public void init() {
        for (String domain : domainsFree) {
            Browser.setRequestIntervalLimitGlobal(domain, 333);
        }
        for (String domain : domainsPremium) {
            Browser.setRequestIntervalLimitGlobal(domain, 333);
        }
    }

    public void correctDownloadLink(final DownloadLink link) throws MalformedURLException {
        try {
            String url = link.getPluginPatternMatcher();
            url = correctAddedURL(this.getHost(), url);
            link.setPluginPatternMatcher(url);
        } catch (final PluginException e) {
        }
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        final String quality = link.getStringProperty(PROPERTY_QUALITY);
        final String viewkey = link.getStringProperty(PROPERTY_VIEWKEY);
        if (quality != null && viewkey != null) {
            final StringBuilder sb = new StringBuilder(32);
            sb.append("pornhub://");
            sb.append(viewkey);
            sb.append("_");
            sb.append(getFormat(link));
            sb.append("_");
            sb.append(quality);
            return sb.toString();
        } else {
            return super.getMirrorID(link);
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String quality = link.getStringProperty(PROPERTY_QUALITY);
        final String viewkey = link.getStringProperty(PROPERTY_VIEWKEY);
        if (quality != null && viewkey != null) {
            final StringBuilder sb = new StringBuilder(32);
            sb.append("pornhub://");
            sb.append(viewkey);
            sb.append("_");
            sb.append(getFormat(link));
            sb.append("_");
            sb.append(quality);
            return sb.toString();
        } else {
            return super.getLinkID(link);
        }
    }

    public static int getUrlCrawlLanguageHandlingMode() {
        return SubConfiguration.getConfig("pornhub.com").getIntegerProperty(PornHubCom.SETTING_URL_CRAWL_LANGUAGE_HANDLING, default_SETTING_URL_CRAWL_LANGUAGE_HANDLING);
    }

    public static String getPreferredSubdomain(final String url) throws MalformedURLException {
        final String originalSubdomain = Browser.getSubdomain(url, false);
        if (getUrlCrawlLanguageHandlingMode() == 1 && originalSubdomain != null) {
            return originalSubdomain;
        } else {
            return "www.";
        }
    }

    /**
     * Corrects single video/gif URL based on given/not and user preference.
     *
     * @throws MalformedURLException
     */
    public static String correctAddedURL(final String pluginDomain, final String url) throws PluginException, MalformedURLException {
        final String viewKey = getViewkeyFromURL(url);
        final String urlDomain = Browser.getHost(url);
        if ("pornhubdecrypted".equals(urlDomain)) {
            /* do not modify pornhubdecrypted URLs */
            return url;
        } else if (url.matches(type_modelhub)) {
            /* Do not modify modelhub URLs */
            return url;
        }
        final String preferredSubdomain = getPreferredSubdomain(url);
        if (url.matches(type_photo)) {
            return createPornhubImageLink(pluginDomain, preferredSubdomain, urlDomain, viewKey, null);
        } else if (url.matches(type_gif_webm)) {
            return createPornhubGifLink(pluginDomain, preferredSubdomain, urlDomain, viewKey, null);
        } else {
            return createPornhubVideoLink(pluginDomain, preferredSubdomain, urlDomain, viewKey, null);
        }
    }

    private String getFormat(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_FORMAT, "mp4");
    }

    public static boolean requiresPremiumAccount(final String url) {
        if (url == null) {
            return false;
        } else if (url.matches(PornHubComVideoCrawler.PATTERN_MODELHUB_USER_PHOTO_ALBUMS)) {
            return false;
        } else if (url.matches(type_modelhub)) {
            return true;
        } else {
            return false;
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

    public static Request getFirstPageWithAccount(final PornHubCom plg, final Account account, final String url) throws Exception {
        if (account == null) {
            return getPage(plg.getBrowser(), url);
        } else {
            synchronized (account) {
                final boolean verifiedLogin = plg.login(account, false);
                final Request request = getPage(plg.getBrowser(), url);
                if (!isLoggedInHtml(plg.getBrowser())) {
                    plg.getLogger().info("Not logged in?|VerifiedLogin:" + verifiedLogin);
                    plg.login(account, true);
                    return getPage(plg.getBrowser(), url);
                } else {
                    return request;
                }
            }
        }
    }

    public static boolean isGeoRestricted(final Browser br) {
        final String[] errorMessages = new String[] { "Dieser Inhalt ist in deinem Land nicht verfügbar", "Ce contenu n'est pas disponible dans votre pays", "Este contenido no está disponible en tu país", "Questo contenuto non è disponibile nel tuo Paese", "Este conteúdo não está disponível no seu país", "Ten materiał jest niedostępny w Twoim kraju", "Этот контент не доступен в Вашей стране", "このコンテンツはあなたの国ではご利用いただけません。", "Deze content is niet beschikbaar in je land", "Tento Obsah není ve vaší zemi dostupný", "此内容在您的国家不可播放。" };
        for (final String errorMessage : errorMessages) {
            if (br.containsHTML(">\\s*" + Pattern.quote(errorMessage) + "\\.?\\s*<")) {
                return true;
            }
        }
        return br.containsHTML("class\\s*=\\s*\"geoBlocked\"") || br.containsHTML(">\\s*This (?:video|content) is unavailable in your country.?\\s*<");
    }

    public static boolean isFlagged(final Browser br) {
        return br.containsHTML(">\\s*Video has been flagged for verification in accordance with our trust and safety policy.?\\s*<");
    }

    public static boolean hasOfflineRemovedVideoText(final Browser br) {
        return br.containsHTML("<span[^>]*>\\s*Video has been removed at the request of") || br.containsHTML("<span[^>]*>\\s*This video has been removed\\s*</span>") || br.containsHTML("<span[^>]*>\\s*This video is currently unavailable\\s*</span>");
    }

    public static boolean hasOfflineVideoNotice(final Browser br) {
        return br.containsHTML("<div[^>]*class[^>]*video-notice[^>]*>\\s*<p>\\s*<span>\\s*This video has been disabled") || br.containsHTML("<h2 style[^>]*>\\s*(This video has been disabled|Dieses Video wurde deaktiviert|Cette vidéo a été désactivée|Este vídeo ha sido deshabilitado|Questo video è stato disattivato|O vídeo foi desativado|Ten film został zablokowany|Это видео было отключено|このビデオは利用できません|Deze video werd uitgeschakeld|Video bylo deaktivováno|此视频已下架)\\.?\\s*</h2>");
    }

    public static boolean hasPendingReview(final Browser br) {
        return br.containsHTML("<h2 style[^>]*>\\s*(GIF is unavailable pending review|La GIF è ancora in fase di verifica e non è al momento disponibile|GIF está indisponível com revisão pendente|GIF is niet beschikbaar in afwachting van review)\\.?\\s*</h2>");
    }

    private void checkAvailability(final DownloadLink link, final Browser br) throws PluginException {
        if (StringUtils.containsIgnoreCase(br.getURL(), "/premium/login")) {
            throw new AccountRequiredException();
        } else if (isFlagged(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Video has been flagged");
        } else if (isGeoRestricted(br)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This content is unavailable in your country", 24 * 60 * 60 * 1000l);
        } else if (hasOfflineRemovedVideoText(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Video has been removed");
        } else if (hasOfflineVideoNotice(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Video has been disabled");
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (hasPendingReview(br)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unavailable due to pending review");
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        prepBr(br);
        final String source_url = link.getStringProperty("mainlink");
        String viewKey = null;
        try {
            final String url = link.getPluginPatternMatcher();
            viewKey = getViewkeyFromURL(url);
        } catch (PluginException e) {
            viewKey = getViewkeyFromURL(source_url);
        }
        /* User-chosen quality, set in decrypter */
        String html_filename = null;
        String server_filename = null;
        boolean isVideo = false;
        final String quality = link.getStringProperty(PROPERTY_QUALITY, null);
        /*
         * TODO account handling: Prefer account from handlePremium to be sure not to use ANY account for downloading but the account the
         * upper handling is using!
         */
        dlUrl = null;
        boolean cachedURLFlag = false;
        if (link.getPluginPatternMatcher().matches(type_photo)) {
            final String linkHost = Browser.getHost(link.getPluginPatternMatcher());
            /* Offline links should also have nice filenames */
            link.setName(viewKey + ".jpg");
            br.setFollowRedirects(true);
            getPage(br, createPornhubImageLink(this.getHost(), getPreferredSubdomain(link.getPluginPatternMatcher()), linkHost, viewKey, null));
            if (br.containsHTML(html_privateimage)) {
                br.setFollowRedirects(true);
                getFirstPageWithAccount(this, account, createPornhubImageLink(this.getHost(), getPreferredSubdomain(link.getPluginPatternMatcher()), linkHost, viewKey, account));
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
        } else if (link.getPluginPatternMatcher().matches(type_gif_webm)) {
            final String linkHost = Browser.getHost(link.getPluginPatternMatcher());
            /* Offline links should also have nice filenames */
            boolean webm = link.getBooleanProperty("webm", getPluginConfig().getBooleanProperty(GIFS_WEBM, true));
            link.setName(viewKey + ".webm");
            br.setFollowRedirects(true);
            getPage(br, createPornhubGifLink(this.getHost(), getPreferredSubdomain(link.getPluginPatternMatcher()), linkHost, viewKey, null));
            checkAvailability(link, br);
            String title = br.getRegex("data-gif-title\\s*=\\s*\"(.*?)\"").getMatch(0);
            if (title == null) {
                title = br.getRegex("<title\\s*>\\s*(.*?)\\s*(&#124;|\\|)").getMatch(0);
            }
            if (title == null || StringUtils.containsIgnoreCase(title, "view_video.php")) {
                html_filename = viewKey;
            } else {
                html_filename = title + "_" + viewKey;
            }
            if (!webm) {
                // link or default is gif, check for gif
                html_filename += ".gif";
                dlUrl = br.getRegex("data\\-gif\\s*=\\s*\"(https?[^\"]+" + Pattern.quote(viewKey) + "[^\\\"]*\\.gif)\"").getMatch(0);
                if (dlUrl == null) {
                    logger.info("gif not found for:" + viewKey);
                }
            }
            if (dlUrl == null) {
                // gif -> don't fail but fallback to webm
                webm = true;
                html_filename += ".webm";
                dlUrl = br.getRegex("data\\-webm\\s*=\\s*\"(https?[^\"]+" + Pattern.quote(viewKey) + "[^\\\"]*\\.webm)\"").getMatch(0);
                if (dlUrl == null) {
                    dlUrl = br.getRegex("fileWebm\\s*=\\s*'(https?[^']+" + Pattern.quote(viewKey) + "[^']*\\.webm)'").getMatch(0);
                }
                if (dlUrl == null) {
                    logger.info("webm not found for:" + viewKey);
                }
            }
            link.setProperty("webm", webm);
        } else {
            /* Required later if e.g. directurl has to be refreshed! */
            isVideo = true;
            /* Offline links should also have nice filenames */
            link.setName(viewKey + ".mp4");
            html_filename = link.getStringProperty("decryptedfilename", null);
            dlUrl = link.getStringProperty(PROPERTY_DIRECTLINK, null);
            cachedURLFlag = true;
            if (dlUrl == null || html_filename == null) {
                /* This should never happen as every url goes into the decrypter first! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.setFollowRedirects(true);
            getFirstPageWithAccount(this, account, createPornhubVideoLink(this.getHost(), getPreferredSubdomain(link.getPluginPatternMatcher()), Browser.getHost(source_url), viewKey, account));
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
        String format = link.getStringProperty(PROPERTY_FORMAT);
        if (format == null) {
            // older links
            format = "mp4";
        }
        server_filename = getFilenameFromURL(dlUrl);
        if (this.getPluginConfig().getBooleanProperty(USE_ORIGINAL_SERVER_FILENAME, false) && server_filename != null) {
            link.setFinalFileName(server_filename);
        } else {
            link.setFinalFileName(html_filename);
        }
        if (!StringUtils.isEmpty(this.dlUrl)) {
            if (!verifyFinalURL(link, format, this.dlUrl, cachedURLFlag)) {
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
                if (this.dlUrl == null && StringUtils.equalsIgnoreCase("mp4", format)) {
                    // 2020-01-11, only HLS available, auto check for hls
                    logger.warning("Failed to get fresh directurl:" + format + "/" + quality + "|auto check for hls");
                    format = "hls";
                    this.dlUrl = qualities.containsKey(quality) ? qualities.get(quality).get(format) : null;
                }
                if (this.dlUrl == null) {
                    logger.warning("Failed to get fresh directurl:" + format + "/" + quality);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    /* Last chance */
                    logger.warning("Check fresh directurl:" + format + "/" + quality + "/" + dlUrl);
                    if (!verifyFinalURL(link, format, this.dlUrl, false)) {
                        logger.info("Fresh directurl did not lead to downloadable content");
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to refresh directurl");
                    } else {
                        link.setProperty(PROPERTY_FORMAT, format);
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public boolean verifyFinalURL(final DownloadLink link, final String format, final String url, final boolean cachedURLFlag) throws Exception {
        try {
            if (StringUtils.equalsIgnoreCase("hls", format)) {
                final Browser hlsCheck = br.cloneBrowser();
                hlsCheck.setFollowRedirects(true);
                hlsCheck.setAllowedResponseCodes(new int[] { -1 });
                hlsCheck.getPage(url);
                if (hlsCheck.getHttpConnection().getResponseCode() != 200) {
                    /* Directurl needs to be refreshed */
                    return false;
                } else if (!LinkCrawlerDeepInspector.looksLikeMpegURL(hlsCheck.getHttpConnection())) {
                    /* Obligatory seconds check. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    final List<HlsContainer> hlsContainers = HlsContainer.getHlsQualities(hlsCheck.cloneBrowser());
                    if (hlsContainers == null || hlsContainers.size() != 1) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        URLConnectionAdapter con = null;
                        try {
                            final List<M3U8Playlist> m3u8list = hlsContainers.get(0).getM3U8(hlsCheck);
                            if (m3u8list == null || m3u8list.size() == 0) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            final Browser segmentCheck = br.cloneBrowser();
                            segmentCheck.setFollowRedirects(true);
                            con = (Thread.currentThread() instanceof SingleDownloadController) ? segmentCheck.openGetConnection(m3u8list.get(0).getSegment(0).getUrl()) : null;
                            if (con == null || looksLikeDownloadableContent(con)) {
                                final HLSDownloader downloader = new HLSDownloader(link, br, br.getURL(), m3u8list);
                                final long estimatedSize = downloader.getEstimatedSize();
                                if (estimatedSize > 0) {
                                    link.setDownloadSize(estimatedSize);
                                }
                                link.setProperty(PROPERTY_DIRECTLINK, url);
                                return true;
                            } else {
                                segmentCheck.followConnection(true);
                            }
                        } catch (IOException e) {
                            logger.log(e);
                        } finally {
                            if (con != null) {
                                con.disconnect();
                            }
                        }
                        return false;
                    }
                }
            } else {
                final Browser urlCheck = br.cloneBrowser();
                urlCheck.setFollowRedirects(true);
                URLConnectionAdapter con = null;
                try {
                    con = urlCheck.openHeadConnection(dlUrl);
                    if (urlCheck.getHttpConnection().getResponseCode() != 200) {
                        urlCheck.followConnection(true);
                        return false;
                    } else if (StringUtils.containsIgnoreCase(urlCheck.getHttpConnection().getContentType(), "text")) {
                        urlCheck.followConnection(true);
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        if (con.getLongContentLength() > 0) {
                            link.setDownloadSize(con.getLongContentLength());
                        }
                        link.setProperty(PROPERTY_DIRECTLINK, url);
                        return true;
                    }
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
            }
        } catch (PluginException e) {
            if (cachedURLFlag) {
                logger.log(e);
                return false;
            } else {
                throw e;
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
    public static Map<String, Map<String, String>> getVideoLinks(final Plugin plugin, final Browser br) throws Exception {
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
            boolean embed = false;
            if (flashVars == null) {
                /* Wide open - risky, embed */
                flashVars = br.getRegex("(var\\s*flashvars[^_].*?)(loadScriptUniqueId|</script)").getMatch(0);
                embed = flashVars != null;
            }
            final String flashVarsID = new Regex(flashVars, "flashvars_(\\d+)").getMatch(0);
            if (flashVarsID != null || embed) {
                try {
                    flashVars = flashVars.replaceFirst("(?s)(playerObjList.+)", "");
                    final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(plugin);
                    final ScriptEngine engine = manager.getEngineByName("javascript");
                    engine.eval("var document={};document.referrer=\"\";");
                    if (embed) {
                        engine.eval(flashVars + "var result=JSON.stringify(flashvars);");
                    } else {
                        engine.eval(flashVars + "var result=JSON.stringify(flashvars_" + flashVarsID + ");");
                    }
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
                final List<Object> entries = (List<Object>) values.get("mediaDefinitions");
                if (entries.size() == 0) {
                    /*
                     * 2019-04-30: Very rare case - video is supposed to be online but ZERO qualities are available --> Video won't load in
                     * browser either --> Offline
                     */
                    return qualities;
                }
                final List<Object> medias = new ArrayList<Object>(entries);
                while (medias.size() > 0) {
                    final Map<String, Object> e = (Map<String, Object>) medias.remove(0);
                    String format = (String) e.get("format");
                    if (StringUtils.equalsIgnoreCase(format, "dash")) {
                        plugin.getLogger().info("Dash not yet supported:" + JSonStorage.toString(e));
                        continue;
                    }
                    dllink_temp = (String) e.get("videoUrl");
                    format = format.toLowerCase(Locale.ENGLISH);
                    Object qualityInfo = e.get("quality");
                    if (qualityInfo == null) {
                        continue;
                    } else if (qualityInfo instanceof List) {
                        final int size = ((List) qualityInfo).size();
                        if (size == 1) {
                            qualityInfo = ((List) qualityInfo).get(0);
                        } else {
                            if (StringUtils.equalsIgnoreCase(format, "mp4")) {
                                final boolean mp4Workaround = MP4_WORKAROUND.get();
                                try {
                                    if (TRY_MP4.get()) {
                                        final Browser brc = br.cloneBrowser();
                                        brc.setFollowRedirects(true);
                                        // no keep alive for this request
                                        brc.getHeaders().put("Connection", "close");
                                        brc.getPage(dllink_temp);
                                        final List<Object> mp4Medias = plugin.restoreFromString(brc.toString(), TypeRef.LIST);
                                        medias.addAll(mp4Medias);
                                    } else {
                                        plugin.getLogger().info("progressive MP4 seems no longer to be supported!");
                                    }
                                } catch (IOException ioe) {
                                    plugin.getLogger().log(ioe);
                                } catch (final JSonMapperException jme) {
                                    if (!mp4Workaround) {
                                        plugin.getLogger().info("Enable mp4 workaround");
                                        MP4_WORKAROUND.set(true);
                                    } else {
                                        plugin.getLogger().info("Disable mp4 support");
                                        TRY_MP4.set(false);
                                    }
                                    plugin.getLogger().log(jme);
                                    plugin.getLogger().info("Found invalid/broken quality: " + dllink_temp);
                                }
                                continue;
                            } else if (StringUtils.equalsIgnoreCase(format, "hls")) {
                                try {
                                    for (final Object quality : (List) qualityInfo) {
                                        final String q = quality + "P";
                                        final String replacement = new Regex(dllink_temp, "(" + q + "_\\d+K)").getMatch(0);
                                        if (replacement != null && quality instanceof Number && qualities.get(quality.toString()) == null) {
                                            final String m3u8 = dllink_temp.replaceFirst("(\\d+/,)(.*?)(,_\\d+\\.mp4)", "$1" + replacement + "$3");
                                            final Browser brc = br.cloneBrowser();
                                            brc.setFollowRedirects(true);
                                            brc.getPage(m3u8);
                                            final List<HlsContainer> hlsQualities = HlsContainer.getHlsQualities(brc);
                                            if (hlsQualities.size() == 1) {
                                                Map<String, String> formatMap = qualities.get(quality.toString());
                                                if (formatMap == null) {
                                                    // prefer single quality m3u8 files, see blow
                                                    formatMap = new HashMap<String, String>();
                                                    qualities.put(quality.toString(), formatMap);
                                                    formatMap.put(format, dllink_temp);
                                                }
                                            }
                                        }
                                    }
                                } catch (IOException ioe) {
                                    plugin.getLogger().log(ioe);
                                } catch (JSonMapperException jme) {
                                    plugin.getLogger().log(jme);
                                }
                                continue;
                            }
                            plugin.getLogger().info("Skip:" + JSonStorage.toString(e));
                            continue;
                        }
                    }
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
                try {
                    final String viewkey = getViewkeyFromURL(br.getURL());
                    if (viewkey != null) {
                        final Browser brc = br.cloneBrowser();
                        getPage(brc, createPornhubVideoLinkEmbedFree(plugin.getHost(), brc, viewkey));
                        var_player_quality_dp = brc.getRegex("\"quality_(\\d+)p\"\\s*?:\\s*?\"(https?[^\"]+)\"").getMatches();
                        matchPlaces = new int[] { 0, 1 };
                    }
                } catch (PluginException e) {
                    plugin.getLogger().log(e);
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
        String ret = br.getRegex("\"author\"\\s*:\\s*(\"[^\"]+\")").getMatch(0);
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link);
    }

    @SuppressWarnings("deprecation")
    private void doFree(final DownloadLink link) throws Exception, PluginException {
        final String format = link.getStringProperty(PROPERTY_FORMAT);
        if (StringUtils.equalsIgnoreCase(format, "hls")) {
            if (StringUtils.isEmpty(dlUrl)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            checkFFmpeg(link, "Download a HLS Stream");
            final List<HlsContainer> hlsContainers = HlsContainer.getHlsQualities(br.cloneBrowser(), dlUrl);
            if (hlsContainers == null || hlsContainers.size() != 1) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new HLSDownloader(link, br, hlsContainers.get(0).getDownloadurl());
            dl.startDownload();
        } else {
            final boolean resume;
            final int maxchunks;
            if (link.getDownloadURL().matches(type_photo)) {
                resume = true;
                /* We only have small pictures --> No chunkload needed */
                maxchunks = 1;
                requestFileInformation(link);
            } else {
                resume = ACCOUNT_FREE_RESUME;
                maxchunks = ACCOUNT_FREE_MAXCHUNKS;
                if (br.containsHTML(html_privatevideo)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "You're not authorized to watch/download this private video");
                }
                if (StringUtils.isEmpty(dlUrl)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dlUrl, resume, maxchunks);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
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

    public static final String COOKIE_ID_FREE    = "v2_free";
    public static final String COOKIE_ID_PREMIUM = "v2_premium";

    public static String getPrimaryFreeDomain() {
        return PornHubCom.domainsFree[0];
    }

    public static String getPrimaryPremiumDomain() {
        return PornHubCom.domainsPremium[0];
    }

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

    @Override
    public String getUserInput(String title, String message, DownloadLink link) throws PluginException {
        try {
            return super.getUserInput(title, message, link);
        } catch (PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_FATAL) {
                return null;
            } else {
                throw e;
            }
        }
    }

    public static final String[] domainsFree                     = new String[] { "pornhub.com", "pornhub.org" };
    public static final String[] domainsPremium                  = new String[] { "pornhubpremium.com", "modelhub.com" };
    public static final String   PROPERTY_LAST_USED_LOGIN_DOMAIN = "last_used_login_domain";

    public boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                final String preferredLoginFreeDomain = getConfiguredDomainLoginFree(this.getHost());
                final String preferredLoginPremiumDomain = getConfiguredDomainLoginPremium(this.getHost());
                // Load cookies
                br.setCookiesExclusive(true);
                /* 2017-01-25: Important - we often have redirects! */
                br.setFollowRedirects(true);
                prepBr(br);
                final String freeCookieDomain = getPreferredFreeCookieDomain(account);
                final Cookies freeCookies = account.loadCookies(COOKIE_ID_FREE);
                final Cookies premiumCookies = account.loadCookies(COOKIE_ID_PREMIUM);
                final boolean is_cookie_only_login = account.getBooleanProperty(PROPERTY_ACCOUNT_is_cookie_login_only, false);
                final boolean cookiesOk = freeCookies != null && premiumCookies != null && (isLoggedInFreeCookieFree(freeCookies) || isLoggedInPremiumCookie(premiumCookies));
                if (!force && cookiesOk) {
                    br.setCookies(freeCookieDomain, freeCookies);
                    br.setCookies(preferredLoginPremiumDomain, premiumCookies);
                    logger.info("Trust login cookies without check:" + account.getType());
                    /* We trust these cookies --> Do not check them */
                    return false;
                }
                if (freeCookies != null && premiumCookies != null) {
                    /* Check cookies - only perform a full login if they're not valid anymore. */
                    br.setCookies(freeCookieDomain, freeCookies);
                    br.setCookies(preferredLoginPremiumDomain, premiumCookies);
                    if (checkLoginSetAccountTypeAndSaveCookies(br, account, true)) {
                        logger.info("Cookie login successful");
                        return true;
                    } else {
                        br.clearCookies(null);
                        logger.info("Cached login cookies failed");
                    }
                }
                if (is_cookie_only_login) {
                    if (account.hasEverBeenValid()) {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                    } else {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                    }
                }
                logger.info("Performing full login");
                prepBr(br);
                // getPage(br, "https://www." + preferredLoginFreeDomain);
                getPage(br, getProtocolFree() + "www." + preferredLoginFreeDomain + "/login");
                if (br.containsHTML("(?i)Sorry we couldn't find what you were looking for")) {
                    /* Try again */
                    getPage(br, getProtocolFree() + "www." + preferredLoginFreeDomain + "/login");
                }
                final Form loginform = br.getFormbyKey("username");
                // final String login_key = br.getRegex("id=\"login_key\" value=\"([^<>\"]*?)\"").getMatch(0);
                // final String login_hash = br.getRegex("id=\"login_hash\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                String token1 = null;
                if (loginform.hasInputFieldByName("token")) {
                    token1 = loginform.getInputField("token").getValue();
                }
                loginform.put("username", account.getUser());
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember_me", "on");
                loginform.setMethod(MethodType.POST);
                loginform.setAction("/front/authenticate");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.submitForm(loginform);
                Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                // final String success = PluginJSonUtils.getJsonValue(br, "success");
                final Number twoStepVerification = ((Number) entries.get("twoStepVerification"));
                if (twoStepVerification != null && twoStepVerification.intValue() == 1) {
                    /* At this point we know that username and password are correct! */
                    final String authyId = (String) entries.get("authyId");
                    final String authyIdHashed = (String) entries.get("authyIdHashed");
                    final String token2 = (String) entries.get("autoLoginParameter");
                    final String phoneNumber = (String) entries.get("phoneNumber");
                    if (StringUtils.isEmpty(authyId) || StringUtils.isEmpty(token2) || StringUtils.isEmpty(phoneNumber)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    logger.info("2FA code required");
                    String twoFACode = this.getUserInput("Enter 2-Factor SMS Authentication code for number " + phoneNumber, null);
                    if (twoFACode != null) {
                        twoFACode = twoFACode.trim();
                    }
                    /* 2021-03-08: I also got 7-digit codes... */
                    if (twoFACode == null || !twoFACode.matches("^\\d{4,}$")) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiges Format der 2-faktor-Authentifizierung!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid 2-factor-authentication code format!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    final Form loginform2 = new Form();
                    loginform2.setAction(br.getURL());
                    loginform2.setMethod(MethodType.POST);
                    loginform2.put("username", Encoding.urlEncode(account.getUser()));
                    loginform2.put("token2", Encoding.urlEncode(token2));
                    loginform2.put("verification_modal", "1");
                    loginform2.put("authyId", authyId);
                    loginform2.put("authyIdHashed", StringUtils.valueOrEmpty(authyIdHashed));
                    if (token1 != null) {
                        loginform2.put("token", Encoding.urlEncode(token1));
                    }
                    loginform2.put("verification_code", twoFACode);
                    br.submitForm(loginform2);
                    entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                }
                final String redirect = (String) entries.get("redirect");
                final Boolean expiredPremiumUser = (Boolean) entries.get("expiredPremiumUser");
                /*
                 * 2022-06-27: remember_me is always "false" even though we check the "remember_me" checkbox/field. It's the same in browser
                 * though!
                 */
                // final Boolean rememberMe = (Boolean) entries.get("remember_me");
                // final String username = (String) entries.get("username");
                if ((Integer) ReflectionUtils.cast(entries.get("success"), Integer.class) != 1) {
                    throw new AccountInvalidException();
                }
                if (redirect != null && (redirect.startsWith("http") || redirect.startsWith("/"))) {
                    /* Required to get the (premium) cookies (multiple redirects). */
                    final boolean premiumExpired;
                    if (expiredPremiumUser != null && expiredPremiumUser == Boolean.TRUE) {
                        premiumExpired = true;
                    } else {
                        premiumExpired = isPremiumFromURL(redirect) && redirect.contains("expired");
                    }
                    getPage(br, redirect);
                    if (premiumExpired && !isPremiumDomain(br.getHost())) {
                        /**
                         * Expired pornhub premium --> It should still be a valid free account --> We might need to access a special url
                         * which redirects us to the pornhub free mainpage and sets the cookies. </br> 2022-06-27: Old code but let's leave
                         * it in for now as we can't know if it is still needed.
                         */
                        logger.info("Expired premium --> Free account (?)");
                        final String pornhubMainpageCookieRedirectUrl = br.getRegex("\\'pornhubLink\\'\\s*?:\\s*?(?:\"|\\')(https?://(?:www\\.)?pornhub\\.(?:com|org)/[^<>\"\\']+)(?:\"|\\')").getMatch(0);
                        if (pornhubMainpageCookieRedirectUrl != null) {
                            getPage(br, pornhubMainpageCookieRedirectUrl);
                        } else {
                            logger.info("Failed to find pornhubMainpageCookieRedirectUrl");
                        }
                    }
                } else {
                    /* Fallback */
                    getPage(br, getProtocolFree() + "www." + preferredLoginFreeDomain);
                }
                if (!isLoggedInHtml(br)) {
                    if (twoStepVerification != null && twoStepVerification.intValue() == 1) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid 2-factor-authentication code", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Check if we're really logged in and set account type. */
                if (!checkLoginSetAccountTypeAndSaveCookies(br, account, false)) {
                    if (isAccountAgeVerificationRequired(br, account)) {
                        throw new AccountInvalidException("Please verify your age to access Pornhub Premium");
                    } else {
                        /* This should never happen */
                        logger.warning("Invalid logins although full login seemed to be successful");
                        throw new AccountInvalidException();
                    }
                }
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

    private boolean isAccountAgeVerificationRequired(final Browser br, final Account account) {
        final String[] errorMessages = new String[] { "Please verify your age to access Pornhub Premium", "Bitte überprüfen Sie Ihr Alter, um auf Pornhub Premium zugreifen zu können", "Veuillez vérifier votre âge pour accéder à Pornhub Premium" };
        for (final String errorMessage : errorMessages) {
            if (br.containsHTML("title\\s*:\\s*'" + Pattern.quote(errorMessage) + "'")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks login and sets account-type. </br> Expects browser instance to be logged in already (cookies need to be there).
     *
     * @throws Exception
     */
    private boolean checkLoginSetAccountTypeAndSaveCookies(final Browser br, final Account account, final boolean accessMainpage) throws Exception {
        final String preferredLoginFreeDomain = getConfiguredDomainLoginFree(this.getHost());
        final String preferredLoginPremiumDomain = getConfiguredDomainLoginPremium(this.getHost());
        final String freeCookieDomain = getPreferredFreeCookieDomain(account);
        final boolean useNewHandling = true;
        if (useNewHandling) {
            /* 2022-06-27: New simpler handling */
            if (accessMainpage) {
                getPage(br, (getProtocolFree() + "www." + preferredLoginFreeDomain));
            }
            final String hopefullyFreeDomain = br.getHost();
            final String premiumAuthURL = br.getRegex("(?i)(/authenticate/goToLoggedIn)\"\\s*>\\s*Access your Pornhub Premium").getMatch(0);
            if (premiumAuthURL != null) {
                /* This will redirect to pornhub premium website and set required cookies */
                logger.info("Looks like account is premium");
                getPage(br, premiumAuthURL);
            }
            if (!isLoggedInHtml(br)) {
                return false;
            }
            if (isLoggedInHtmlPremium(br)) {
                setAccountType(account, AccountType.PREMIUM);
            } else {
                setAccountType(account, AccountType.FREE);
            }
            if (isFreeDomain(hopefullyFreeDomain)) {
                /* Free cookies shall be available and we're currently on a free domain -> Get cookies from that domain */
                account.saveCookies(br.getCookies(hopefullyFreeDomain), COOKIE_ID_FREE);
                logger.info("User preferred free domain: " + preferredLoginFreeDomain + " | Actually used free domain: " + hopefullyFreeDomain);
                if (!account.getStringProperty(PROPERTY_LAST_USED_LOGIN_DOMAIN, freeCookieDomain).equals(hopefullyFreeDomain)) {
                    /* This is needed so when we check the login cookies next time, cookies will be set on the correct domain. */
                    logger.info("Old free domain: " + freeCookieDomain + " | New free domain: " + hopefullyFreeDomain);
                    account.setProperty(PROPERTY_LAST_USED_LOGIN_DOMAIN, hopefullyFreeDomain);
                }
            } else {
                /* No free cookies available --> This should never happen --> Use user defined domain to get cookies from */
                logger.warning("Failed to find current free domain");
                account.saveCookies(br.getCookies(freeCookieDomain), COOKIE_ID_FREE);
            }
            account.saveCookies(br.getCookies(preferredLoginPremiumDomain), COOKIE_ID_PREMIUM);
            return true;
        } else {
            /* Old handling */
            if (AccountType.PREMIUM.equals(account.getType())) {
                /* fast check */
                getPage(br, (getProtocolPremium() + preferredLoginPremiumDomain + "/user/login_status?ajax=1"));
                if (br.containsHTML("\"success\"\\s*:\\s*(\"1\"|true)") && isLoggedInPremiumCookie(br.getCookies(br.getHost()))) {
                    setAccountType(account, AccountType.PREMIUM);
                    logger.info("Verified(fast) premium->premium login cookies:" + account.getType());
                    account.saveCookies(br.getCookies(freeCookieDomain), COOKIE_ID_FREE);
                    account.saveCookies(br.getCookies(preferredLoginPremiumDomain), COOKIE_ID_PREMIUM);
                    return true;
                } else {
                    /* slow check */
                    getPage(br, (getProtocolPremium() + preferredLoginPremiumDomain));
                    if (isLoggedInHtmlPremium(br)) {
                        setAccountType(account, AccountType.PREMIUM);
                        logger.info("Verified(slow) premium->premium login cookies:");
                        account.saveCookies(br.getCookies(freeCookieDomain), COOKIE_ID_FREE);
                        account.saveCookies(br.getCookies(preferredLoginPremiumDomain), COOKIE_ID_PREMIUM);
                        return true;
                    } else if (isLoggedInHtmlFree(br)) {
                        setAccountType(account, AccountType.FREE);
                        logger.info("Verified(slow) premium->free login cookies:" + account.getType());
                        account.saveCookies(br.getCookies(freeCookieDomain), COOKIE_ID_FREE);
                        account.saveCookies(br.getCookies(preferredLoginPremiumDomain), COOKIE_ID_PREMIUM);
                        return true;
                    }
                }
            } else {
                getPage(br, (getProtocolFree() + "www." + preferredLoginFreeDomain));
                if (br.containsHTML("(?i)/authenticate/goToLoggedIn\"\\s*>\\s*Access your Pornhub Premium")) {
                    // 2020-12-29 - no auto redirect to pornhub premium
                    getPage(br, "/authenticate/goToLoggedIn");
                }
                if (isLoggedInHtmlFree(br)) {
                    setAccountType(account, AccountType.FREE);
                    logger.info("Verified(slow) free->free login cookies:" + account.getType());
                    account.saveCookies(br.getCookies(freeCookieDomain), COOKIE_ID_FREE);
                    account.saveCookies(br.getCookies(preferredLoginPremiumDomain), COOKIE_ID_PREMIUM);
                    return true;
                } else {
                    getPage(br, (getProtocolPremium() + preferredLoginPremiumDomain + "/user/login_status?ajax=1"));
                    if (br.containsHTML("\"success\"\\s*:\\s*(\"1\"|true)") && isLoggedInPremiumCookie(br.getCookies(preferredLoginPremiumDomain))) {
                        setAccountType(account, AccountType.PREMIUM);
                        logger.info("Verified(fast) free->premium login cookies:" + account.getType());
                        account.saveCookies(br.getCookies(freeCookieDomain), COOKIE_ID_FREE);
                        account.saveCookies(br.getCookies(preferredLoginPremiumDomain), COOKIE_ID_PREMIUM);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isLoggedInHtml(final Browser br) {
        return br != null && (br.containsHTML("(?i)class\\s*=\\s*\"signOut\"|/premium/lander\"\\s*>\\s*Log ?out\\s*<") || br.containsHTML("class\\s*=\\s*\"[^\"]*ph-icon-logout[^\"]*\"[^>]*>[^<]*</i>\\s*<span[^>]*>\\s*Log ?Out\\s*<"));
    }

    public static boolean isLoggedInHtmlPremium(final Browser br) {
        return br != null && br.getURL() != null && PornHubCom.isPremiumDomain(br.getHost()) && isLoggedInPremiumCookie(br.getCookies(br.getHost())) && isLoggedInHtml(br);
    }

    public static boolean isLoggedInHtmlFree(final Browser br) {
        return br != null && br.getURL() != null && !PornHubCom.isPremiumDomain(br.getHost()) && isLoggedInFreeCookieFree(br.getCookies(br.getHost())) && isLoggedInHtml(br);
    }

    public static boolean isLoggedInFreeCookieFree(final Cookies cookies) {
        return isLoggedInCookie(cookies);
    }

    public static boolean isLoggedInPremiumCookie(final Cookies cookies) {
        return isLoggedInCookie(cookies);
    }

    public static boolean isLoggedInCookie(final Cookies cookies) {
        return cookies != null && (cookies.get("gateway_security_key", Cookies.NOTDELETEDPATTERN) != null || cookies.get("il", Cookies.NOTDELETEDPATTERN) != null);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (account) {
            final boolean isCookieLoginOnly = account.getBooleanProperty(PROPERTY_ACCOUNT_is_cookie_login_only, false);
            login(account, true);
            ai.setUnlimitedTraffic();
            if (AccountType.PREMIUM.equals(account.getType())) {
                if (isCookieLoginOnly) {
                    /* Never modify previous AccountInfo if we're logged in via cookies only! */
                    return account.getAccountInfo();
                } else {
                    return ai;
                }
            } else {
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

    private static final AtomicBoolean MP4_WORKAROUND = new AtomicBoolean(false);
    private static final AtomicBoolean TRY_MP4        = new AtomicBoolean(true);

    public static Browser prepBr(final Browser br) {
        if (TRY_MP4.get() && MP4_WORKAROUND.get()) {
            br.setCookie("http://pornhub.com/", "platform", "mac");
            br.getHeaders().put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15");
        }
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.8,de;q=0.6");
        br.getHeaders().put("Accept-Charset", null);
        for (String domain : domainsFree) {
            br.setCookie(domain, "age_verified", "1");
            /* Mandatory since 2023-03-23 */
            br.setCookie(domain, "cookiesBannerSeen", "1");
            br.setCookie(domain, "accessAgeDisclaimerPH", "1");
        }
        for (String domain : domainsPremium) {
            br.setCookie(domain, "age_verified", "1");
            /* Mandatory since 2023-03-23 */
            br.setCookie(domain, "cookiesBannerSeen", "1");
            br.setCookie(domain, "accessAgeDisclaimerPH", "1");
        }
        if (getUrlCrawlLanguageHandlingMode() == 0) {
            // make sure that english language will be used in this mode
            for (String domain : domainsFree) {
                br.setCookie(domain, "lang", "en");
            }
            for (String domain : domainsPremium) {
                br.setCookie(domain, "lang", "en");
            }
        }
        br.setLoadLimit(br.getDefaultLoadLimit() * 4);
        return br;
    }

    public static String createPornhubImageLink(final String pluginDomain, final String subdomain, final String urlDomain, final String viewkey, final Account acc) {
        if (PornHubCom.isPremiumDomain(urlDomain)) {
            /* Premium url */
            return getProtocolPremium() + subdomain + getConfiguredDomainURL(pluginDomain, urlDomain) + "/photo/" + viewkey;
        } else {
            /* Free url */
            return getProtocolFree() + subdomain + getConfiguredDomainURL(pluginDomain, urlDomain) + "/photo/" + viewkey;
        }
    }

    public static String createPornhubGifLink(final String pluginDomain, final String subdomain, final String urlDomain, final String viewkey, final Account acc) {
        if (PornHubCom.isPremiumDomain(urlDomain)) {
            /* Premium url */
            return getProtocolPremium() + subdomain + getConfiguredDomainURL(pluginDomain, urlDomain) + "/gif/" + viewkey;
        } else {
            /* Free url */
            return getProtocolFree() + subdomain + getConfiguredDomainURL(pluginDomain, urlDomain) + "/gif/" + viewkey;
        }
    }

    public static String createPornhubVideoLink(final String pluginDomain, final String subdomain, final String urlDomain, final String viewkey, final Account acc) {
        if (PornHubCom.isPremiumDomain(urlDomain)) {
            /* Premium url */
            return getProtocolPremium() + subdomain + getConfiguredDomainURL(pluginDomain, urlDomain) + "/view_video.php?viewkey=" + viewkey;
        } else {
            /* Free url */
            return getProtocolFree() + subdomain + getConfiguredDomainURL(pluginDomain, urlDomain) + "/view_video.php?viewkey=" + viewkey;
        }
    }

    public static String createPornhubVideoLinkEmbedFree(final String pluginDomain, final Browser br, final String viewkey) {
        if (isLoggedInHtmlPremium(br)) {
            return createPornhubVideoLinkEmbedPremium(pluginDomain, viewkey);
        } else {
            return createPornhubVideoLinkEmbedFree(pluginDomain, viewkey);
        }
    }

    /** Returns embed url for free- and free account mode. */
    public static String createPornhubVideoLinkEmbedFree(final String pluginDomain, final String viewkey) {
        return String.format("https://www.%s/embed/%s", getConfiguredDomainURL(pluginDomain, getPrimaryFreeDomain()), viewkey);
    }

    /** Returns embed url for premium account mode. */
    public static String createPornhubVideoLinkEmbedPremium(final String pluginDomain, final String viewkey) {
        // return String.format("https://www.%s/embed/%s", getConfiguredDomainURL(pluginDomain, DOMAIN_PORNHUB_PREMIUM), viewkey);
        return createPornhubVideoLinkEmbedFree(pluginDomain, viewkey);
    }

    public static boolean isPremiumFromURL(final String url) {
        return isPremiumDomain(Browser.getHost(url));
    }

    /** Checks if given domain is a <b>supported</b> premium domain. */
    public static boolean isPremiumDomain(final String domain) {
        if (domain == null) {
            return false;
        } else {
            for (final String premiumDomain : domainsPremium) {
                if (StringUtils.containsIgnoreCase(domain, premiumDomain)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean isFreeDomain(final String domain) {
        if (!isPremiumDomain(domain)) {
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
            } else if (url.matches(".+/video/ph[a-f0-9]+$")) {
                ret = new Regex(url, "(ph[a-f0-9]+)").getMatch(0);
            } else if (url.matches(".+/pornhubdecrypted/ph[a-f0-9]+.+")) {
                ret = new Regex(url, "pornhubdecrypted/(ph[a-f0-9]+)").getMatch(0);
            } else {
                ret = new Regex(url, "viewkey=([a-z0-9]+)").getMatch(0);
            }
            if (StringUtils.isEmpty(ret) || "null".equals(ret)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                return ret;
            }
        }
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

    @Override
    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        if (!link.isEnabled() && "".equals(link.getPluginPatternMatcher())) {
            /*
             * setMultiHostSupport uses a dummy DownloadLink, with isEnabled == false. we must set to true for the host to be added to the
             * supported host array.
             */
            return true;
        } else {
            if ("debrid-link.fr".equals(plugin.getHost())) {
                // only supports 360p
                return false;
            } else {
                /* Original plugin can handle every kind of URL! */
                final boolean downloadViaSourcePlugin = link.getHost().equalsIgnoreCase(plugin.getHost());
                /* MOCHs can only handle video URLs! */
                final boolean downloadViaMOCH = !link.getHost().equalsIgnoreCase(plugin.getHost()) && isVideo(link.getStringProperty("mainlink", null));
                return downloadViaSourcePlugin || downloadViaMOCH;
            }
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
        return "JDownloader's Pornhub plugin helps downloading videoclips from pornhub(premium).com.";
    }

    public static final String   SELECTED_DOMAIN                             = "selected_domain2";
    public static final int      default_SELECTED_DOMAIN                     = 0;
    public static final String[] DOMAINS                                     = new String[] { "pornhub.com & pornhubpremium.com", "pornhub.org & pornhubpremium.com" };
    public static final String   SETTING_URL_CRAWL_LANGUAGE_HANDLING         = "url_crawl_language_handling";
    public static final int      default_SETTING_URL_CRAWL_LANGUAGE_HANDLING = 0;
    public static final String[] SETTING_URL_CRAWL_LANGUAGE_HANDLING_OPTIONS = new String[] { "Replace subdomain in url with 'www.' -> Title language English", "Do not touch subdomain -> Title language can vary depending on URL" };

    /** Returns user configured domain based on domain given in URL we want to access. */
    public static String getConfiguredDomainURL(final String pluginDomain, final String domainFromURL) {
        if (domainFromURL == null) {
            throw new IllegalArgumentException("domainFromURL is null!");
        }
        if (!domainCanBeChanged(domainFromURL)) {
            return domainFromURL;
        }
        final SubConfiguration cfg = SubConfiguration.getConfig(pluginDomain);
        switch (cfg.getIntegerProperty(SELECTED_DOMAIN, default_SELECTED_DOMAIN)) {
        case 1:
            return domainFromURL.replaceFirst("\\.com$", ".org");
        case 0:
        default:
            return domainFromURL.replaceFirst("\\.org$", ".com");
        }
    }

    public static boolean domainCanBeChanged(final String domain) {
        if (domain.equalsIgnoreCase("modelhub.com") || domain.equalsIgnoreCase("pornhubpremium.com")) {
            /* E.g. modelhub.org / pornhubpremium.org does not exist. */
            return false;
        } else {
            return true;
        }
    }

    private String getPreferredFreeCookieDomain(final Account account) {
        final String lastUsedLoginDomain = account.getStringProperty(PROPERTY_LAST_USED_LOGIN_DOMAIN);
        if (lastUsedLoginDomain != null && !isPremiumDomain(lastUsedLoginDomain)) {
            /* Return stored domain. */
            return lastUsedLoginDomain;
        } else {
            return getConfiguredDomainLoginFree(this.getHost());
        }
    }

    /** Returns user configured domain for login process free account. */
    public static String getConfiguredDomainLoginFree(final String pluginDomain) {
        return getConfiguredDomainURL(pluginDomain, getPrimaryFreeDomain());
    }

    /** Returns user configured domain for login process premium account. */
    public static String getConfiguredDomainLoginPremium(final String pluginDomain) {
        if (true) {
            /* right now https://pornhubpremium.org does not work */
            return getPrimaryPremiumDomain();
        } else {
            return getConfiguredDomainURL(pluginDomain, getPrimaryPremiumDomain());
        }
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CRAWL_VIDEO_HLS, "Crawl video (HLS)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CRAWL_VIDEO_MP4, "Crawl video (HTTP)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CRAWL_THUMBNAIL, "Crawl video thumbnail?").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_LINKCHECK, "Enable fast linkcheck?\r\nNOTE: If enabled, links will appear faster but filesize won't be shown before downloadstart.").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_ORIGINAL_SERVER_FILENAME, "Use original server filename?").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GIFS_WEBM, "Prefer webm over old gif format?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SELECTED_DOMAIN, DOMAINS, "Select preferred domain").setDefaultValue(default_SELECTED_DOMAIN));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SETTING_URL_CRAWL_LANGUAGE_HANDLING, SETTING_URL_CRAWL_LANGUAGE_HANDLING_OPTIONS, "URL crawl handling").setDefaultValue(default_SETTING_URL_CRAWL_LANGUAGE_HANDLING));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            // link.removeProperty("webm");
        }
    }
}