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
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XHamsterCom extends PluginForHost {
    public XHamsterCom(PluginWrapper wrapper) {
        super(wrapper);
        /* Actually only free accounts are supported */
        this.enablePremium("https://faphouse.com/join");
        setConfigElements();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /** Make sure this is the same in classes XHamsterCom and XHamsterGallery! */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xhamster.com", "xhamster.xxx", "xhamster.desi", "xhamster.one", "xhamster1.desi", "xhamster2.desi", "xhamster3.desi", "openxh.com", "openxh1.com", "openxh2.com", "megaxh.com", "xhvid.com" });
        return ret;
    }

    public static String[] getDeadDomains() {
        /* Add dead domains here so plugin can correct domain in added URL if it is a dead domain. */
        return new String[] {};
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
            /* Videos current pattern */
            String pattern = "https?://(?:[a-z0-9\\-]+\\.)?" + buildHostsPatternPart(domains) + "/videos/[a-z0-9\\-_]+-[A-Za-z0-9]+";
            /* Embed pattern: 2020-05-08: /embed/123 = current pattern, x?embed.php = old one */
            pattern += "|https?://(?:[a-z0-9\\-]+\\.)?" + buildHostsPatternPart(domains) + "/(embed/[A-Za-z0-9]+|x?embed\\.php\\?video=[A-Za-z0-9]+)";
            /* Movies old pattern --> Redirects to TYPE_VIDEOS_2 (or TYPE_VIDEOS_3) */
            pattern += "|https?://(?:[a-z0-9\\-]+\\.)?" + buildHostsPatternPart(domains) + "/movies/[0-9]+/[^/]+\\.html";
            /* Premium pattern */
            pattern += "|https?://(?:gold\\.xhamsterpremium\\.com|faphouse\\.com)/videos/([A-Za-z0-9]+)";
            ret.add(pattern);
        }
        return ret.toArray(new String[0]);
    }

    public static String buildHostsPatternPart(String[] domains) {
        final StringBuilder pattern = new StringBuilder();
        pattern.append("(?:");
        for (int i = 0; i < domains.length; i++) {
            final String domain = domains[i];
            if (i > 0) {
                pattern.append("|");
            }
            if ("xhamster.com".equals(domain)) {
                pattern.append("xhamster\\d*\\.(?:com|xxx|desi|one)");
            } else {
                pattern.append(Pattern.quote(domain));
            }
        }
        pattern.append(")");
        return pattern.toString();
    }

    /* DEV NOTES */
    /* Porn_plugin */
    private static final String   SETTING_ALLOW_MULTIHOST_USAGE          = "ALLOW_MULTIHOST_USAGE";
    private final boolean         default_allow_multihoster_usage        = false;
    private final String          SETTING_SELECTED_VIDEO_FORMAT          = "SELECTED_VIDEO_FORMAT";
    /* The list of qualities/formats displayed to the user */
    private static final String[] FORMATS                                = new String[] { "Best available", "240p", "480p", "720p", "960p", "1080p", "1440p", "2160p" };
    public static final String    domain_premium                         = "faphouse.com";
    public static final String    api_base_premium                       = "https://faphouse.com/api";
    private static final String   TYPE_MOVIES                            = "(?i)^https?://[^/]+/movies/(\\d+)/([^/]+)\\.html$";
    private static final String   TYPE_VIDEOS                            = "(?i)^https?://[^/]+/videos/([A-Za-z0-9]+)$";
    private static final String   TYPE_VIDEOS_2                          = "(?i)^https?://[^/]+/videos/([a-z0-9\\-_]+)-(\\d+)$";
    private static final String   TYPE_VIDEOS_3                          = "(?i)^https?://[^/]+/videos/([a-z0-9\\-_]+)-([A-Za-z0-9]+)$";
    private final String          PROPERTY_USERNAME                      = "username";
    private final String          PROPERTY_DATE                          = "date";
    private final String          PROPERTY_TAGS                          = "tags";
    private final String          PROPERTY_ACCOUNT_LAST_USED_FREE_DOMAIN = "last_used_free_domain";
    private final String          PROPERTY_ACCOUNT_PREMIUM_LOGIN_URL     = "premium_login_url";

    private void setConfigElements() {
        String user_text;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            user_text = "Erlaube den Download von Links dieses Anbieters über Multihoster (nicht empfohlen)?\r\n<html><b>Kann die Anonymität erhöhen, aber auch die Fehleranfälligkeit!</b>\r\nAktualisiere deine(n) Multihoster Account(s) nach dem Aktivieren dieser Einstellung um diesen Hoster in der Liste der unterstützten Hoster deines/r Multihoster Accounts zu sehen (sofern diese/r ihn unterstützen).</html>";
        } else {
            user_text = "Allow links of this host to be downloaded via multihosters (not recommended)?\r\n<html><b>This might improve anonymity but perhaps also increase error susceptibility!</b>\r\nRefresh your multihoster account(s) after activating this setting to see this host in the list of the supported hosts of your multihost account(s) (in case this host is supported by your used multihost(s)).</html>";
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_ALLOW_MULTIHOST_USAGE, user_text).setDefaultValue(default_allow_multihoster_usage));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SETTING_SELECTED_VIDEO_FORMAT, FORMATS, "Preferred format").setDefaultValue(0));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "Filename_id", "Only for videos: Change Choose file name to 'filename_ID.exe' e.g. 'test_48604.mp4' ?").setDefaultValue(false));
    }

    @Override
    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        if (this.getPluginConfig().getBooleanProperty(SETTING_ALLOW_MULTIHOST_USAGE, default_allow_multihoster_usage)) {
            return true;
        } else {
            return link.getHost().equalsIgnoreCase(plugin.getHost());
        }
    }

    @Override
    public String getAGBLink() {
        return "http://xhamster.com/terms.php";
    }

    public static final String  TYPE_MOBILE    = "(?i).+m\\.xhamster\\.+";
    public static final String  TYPE_EMBED     = "(?i)^https?://[^/]+/(?:x?embed\\.php\\?video=|embed/)([A-Za-z0-9\\-]+)";
    private static final String TYPE_PREMIUM   = ".+(xhamsterpremium\\.com|faphouse\\.com).+";
    private static final String NORESUME       = "NORESUME";
    private final String        recaptchav2    = "<div class=\"text\">In order to watch this video please prove you are a human\\.\\s*<br> Click on checkbox\\.</div>";
    private String              dllink         = null;
    private String              vq             = null;
    public static final String  DOMAIN_CURRENT = "xhamster.com";

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(getCorrectedURL(link.getPluginPatternMatcher()));
    }

    public static String getCorrectedURL(String url) {
        /*
         * Remove language-subdomain to enforce original/English language else xhamster may auto-translate video-titles based on that
         * subdomain.
         */
        url = url.replaceFirst("://(www\\.)?([a-z]{2}\\.)?", "://");
        final String domainFromURL = Browser.getHost(url, true);
        String newDomain = domainFromURL;
        for (final String deadDomain : getDeadDomains()) {
            if (StringUtils.equalsIgnoreCase(domainFromURL, deadDomain)) {
                newDomain = DOMAIN_CURRENT;
                break;
            }
        }
        if (url.matches(TYPE_MOBILE) || url.matches(TYPE_EMBED)) {
            url = "https://" + newDomain + "/videos/" + new Regex(url, TYPE_EMBED).getMatch(0);
        } else {
            /* Change domain if needed */
            url = url.replaceFirst(Pattern.quote(domainFromURL), newDomain);
        }
        return url;
    }

    private boolean isPremiumURL(final String url) {
        if (url == null) {
            return false;
        } else if (url.matches(TYPE_PREMIUM)) {
            return true;
        } else {
            return false;
        }
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

    private static String getFID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else {
            return getFID(link.getPluginPatternMatcher());
        }
    }

    private static String getFID(final String url) {
        if (url == null) {
            return null;
        } else {
            if (url.matches(TYPE_EMBED)) {
                return new Regex(url, TYPE_EMBED).getMatch(0);
            } else if (url.matches(TYPE_MOBILE)) {
                return new Regex(url, "https?://[^/]+/[^/]+/[^/]*?([a-z0-9]+)(/|$|\\?)").getMatch(0);
            } else if (url.matches(TYPE_MOVIES)) {
                return new Regex(url, TYPE_MOVIES).getMatch(0);
            } else if (url.matches(TYPE_VIDEOS)) {
                return new Regex(url, TYPE_VIDEOS).getMatch(0);
            } else if (url.matches(TYPE_VIDEOS_2)) {
                return new Regex(url, TYPE_VIDEOS_2).getMatch(1);
            } else if (url.matches(TYPE_VIDEOS_3)) {
                return new Regex(url, TYPE_VIDEOS_3).getMatch(1);
            } else {
                /* This should never happen */
                return null;
            }
        }
    }

    private static String getUrlTitle(final String url) {
        if (url.matches(TYPE_MOVIES)) {
            return new Regex(url, TYPE_MOVIES).getMatch(1);
        } else if (url.matches(TYPE_VIDEOS_2)) {
            return new Regex(url, TYPE_VIDEOS_2).getMatch(0);
        } else if (url.matches(TYPE_VIDEOS_3)) {
            return new Regex(url, TYPE_VIDEOS_3).getMatch(0);
        } else {
            /* All other linktypes do not contain any title hint --> Return fid */
            return null;
        }
    }

    private String getFallbackFileTitle(final String url) {
        if (url == null) {
            return null;
        }
        final String urlTitle = getUrlTitle(url);
        if (urlTitle != null) {
            return urlTitle;
        } else {
            return getFID(getDownloadLink());
        }
    }

    /**
     * Returns string containing url-name AND linkID e.g. xhamster.com/videos/some-name-here-bla-7653421 --> linkpart =
     * 'some-name-here-bla-7653421'
     */
    private String getLinkpart(final DownloadLink dl) {
        String linkpart = null;
        if (dl.getPluginPatternMatcher().matches(TYPE_MOBILE)) {
            linkpart = new Regex(dl.getPluginPatternMatcher(), "https?://[^/]+/[^/]+/(.+)").getMatch(0);
        } else if (!dl.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            linkpart = new Regex(dl.getPluginPatternMatcher(), "videos/([\\w\\-]+\\-[a-z0-9]+)").getMatch(0);
        }
        if (linkpart == null) {
            /* Fallback e.g. for embed URLs */
            linkpart = getFID(dl);
        }
        return linkpart;
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            link.setName(getFallbackFileTitle(link.getPluginPatternMatcher()) + ".mp4");
        }
        br.setFollowRedirects(true);
        prepBr(this, br);
        /* quick fix to force old player */
        String title = null;
        if (account != null) {
            login(account, link.getPluginPatternMatcher(), true);
        } else {
            br.getPage(link.getPluginPatternMatcher());
        }
        /* Check for self-embed */
        String selfEmbeddedURL = br.getRegex("<iframe[^>]*src\\s*=\\s*\"(https?://xh\\.video/(?:e|p|v)/" + getFID(link) + ")\"[^>]*></iframe>").getMatch(0);
        if (selfEmbeddedURL == null) {
            selfEmbeddedURL = br.getRegex("<iframe[^>]*src\\s*=\\s*\"(https?://xh\\.video/(?:e|p|v)/[^\"]+)\"[^>]*></iframe>").getMatch(0);
        }
        if (selfEmbeddedURL != null) {
            /* 2022-09-12: xhamster.one sometimes shows a different page and self-embeds */
            logger.info("Found self-embed: " + selfEmbeddedURL);
            br.getPage(selfEmbeddedURL);
            /* Now this may have sent us to an embed URL --> Fix that */
            final String urlCorrected = getCorrectedURL(br.getURL());
            if (!StringUtils.equalsIgnoreCase(br.getURL(), urlCorrected)) {
                logger.info("Corrected URL: Old: " + br.getURL() + " | New: " + urlCorrected);
                br.getPage(urlCorrected);
            }
        }
        /* Set some Packagizer properties */
        final String username = br.getRegex("class=\"entity-author-container__name\"[^>]*href=\"https?://[^/]+/users/([^<>\"]+)\"").getMatch(0);
        final String datePublished = br.getRegex("\"datePublished\":\"(\\d{4}-\\d{2}-\\d{2})\"").getMatch(0);
        if (username != null) {
            link.setProperty(PROPERTY_USERNAME, username);
        }
        if (datePublished != null) {
            link.setProperty(PROPERTY_DATE, datePublished);
        }
        final String[] tagsList = br.getRegex("<a class=\"categories-container__item\"[^>]*href=\"https?://[^/]+/tags/([^\"]+)\"").getColumn(0);
        if (tagsList.length > 0) {
            final StringBuilder sb = new StringBuilder();
            for (String tag : tagsList) {
                tag = Encoding.htmlDecode(tag).trim();
                if (StringUtils.isNotEmpty(tag)) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(tag);
                }
            }
            if (sb.length() > 0) {
                link.setProperty(PROPERTY_TAGS, sb.toString());
            }
        }
        final int responsecode = br.getRequest().getHttpConnection().getResponseCode();
        if (responsecode == 423) {
            if (isVideoOnlyForFriends(br)) {
                return AvailableStatus.TRUE;
            } else if (br.containsHTML("(?i)<title>\\s*Page was deleted\\s*</title>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (isPasswordProtected(br)) {
                return AvailableStatus.TRUE;
            } else {
                String exactErrorMessage = br.getRegex("class=\"item-status not-found\">\\s*<i class=\"xh-icon smile-sad cobalt\"></i>\\s*<div class=\"status-text\">([^<>]+)</div>").getMatch(0);
                if (exactErrorMessage == null) {
                    /* 2021-07-27 */
                    exactErrorMessage = br.getRegex("class=\"error-title\"[^>]*>([^<>\"]+)<").getMatch(0);
                }
                if (exactErrorMessage != null) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 423: " + exactErrorMessage, 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 423", 60 * 60 * 1000l);
                }
            }
        } else if (responsecode == 404 || responsecode == 410 || responsecode == 452) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (this.isPremiumURL(link.getPluginPatternMatcher())) {
            /* Premium content */
            title = br.getRegex("class=\"video__title\">([^<]+)</h1>").getMatch(0);
            if (title == null) {
                title = br.getRegex("property=\"og:title\" content=\"([^\"]+)\"").getMatch(0);
            }
            if (account == null || account.getType() != AccountType.PREMIUM) {
                /* Free / Free-Account users can only download trailers. */
                dllink = br.getRegex("<video src=\"(http[^<>\"]+)\"").getMatch(0);
            } else {
                /* Premium users can download the full videos in different qualities. */
                if (isDownload) {
                    dllink = getDllinkPremium(true);
                } else {
                    final String filesizeStr = getDllinkPremium(false);
                    if (filesizeStr != null) {
                        link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                    }
                }
            }
            if (title != null) {
                link.setFinalFileName(title + ".mp4");
            }
        } else {
            // embeded correction --> Usually not needed
            if (link.getPluginPatternMatcher().matches("(?i).+/xembed\\.php.*")) {
                logger.info("Trying to change embed URL --> Real URL");
                String realpage = br.getRegex("(?i)main_url=(https?[^\\&]+)").getMatch(0);
                if (realpage != null) {
                    logger.info("Successfully changed: " + link.getPluginPatternMatcher() + " ----> " + realpage);
                    link.setUrlDownload(Encoding.htmlDecode(realpage));
                    br.getPage(link.getPluginPatternMatcher());
                } else {
                    logger.info("Failed to change embed URL --> Real URL");
                }
            }
            // recaptchav2 here, don't trigger captcha until download....
            if (br.containsHTML(recaptchav2)) {
                if (!isDownload) {
                    /* Do not ask user to solve captcha during availablecheck, only during download! */
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
            if (br.containsHTML("(?i)(403 Forbidden|>\\s*This video was deleted\\s*<)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String onlyforFriendsWithThisName = isVideoOnlyForFriendsOf(br);
            if (onlyforFriendsWithThisName != null) {
                link.getLinkStatus().setStatusText("Only downloadable for friends of " + onlyforFriendsWithThisName);
                return AvailableStatus.TRUE;
            } else if (isPasswordProtected(br)) {
                return AvailableStatus.TRUE;
            }
            dllink = this.getDllink(br);
            final String fid = getFID(link);
            title = br.getRegex("\"videoEntity\"\\s*:\\s*\\{[^\\}\\{]*\"title\"\\s*:\\s*\"([^<>\"]*?)\"").getMatch(0);
            if (title == null) {
                title = br.getRegex("<h1.*?itemprop=\"name\">(.*?)</h1>").getMatch(0);
                if (title == null) {
                    title = br.getRegex("\"title\":\"([^<>\"]*?)\"").getMatch(0);
                }
            }
            if (title == null) {
                title = br.getRegex("<title.*?>([^<>\"]*?)\\s*\\-\\s*xHamster(" + buildHostsPatternPart(getPluginDomains().get(0)) + ")?</title>").getMatch(0);
            }
            if (title == null) {
                /* Fallback to URL filename - first try to get nice name from URL. */
                title = new Regex(br.getURL(), "/(?:videos|movies)/(.+)\\d+(?:$|\\?)").getMatch(0);
                if (title == null) {
                    /* Last chance */
                    title = new Regex(br.getURL(), "https?://[^/]+/(.+)").getMatch(0);
                }
            }
            String ext;
            if (!StringUtils.isEmpty(dllink) && dllink.contains(".m3u8")) {
                ext = ".mp4";
            } else if (!StringUtils.isEmpty(dllink)) {
                ext = getFileNameExtensionFromString(dllink, ".mp4");
            } else {
                ext = ".mp4";
            }
            if (title != null) {
                if (getPluginConfig().getBooleanProperty("Filename_id", true)) {
                    title += "_" + fid;
                } else {
                    title = fid + "_" + title;
                }
                if (vq != null) {
                    title = Encoding.htmlDecode(title.trim() + "_" + vq).trim();
                } else {
                    title = Encoding.htmlDecode(title).trim();
                }
                title += ext;
                link.setFinalFileName(title);
            }
            if (dllink == null && isPaidContent(br)) {
                link.getLinkStatus().setStatusText("To download, you have to buy this video");
                return AvailableStatus.TRUE;
            }
        }
        /* 2020-01-31: Do not check filesize if we're currently in download mode as directurl may expire then. */
        if (link.getView().getBytesTotal() <= 0 && !isDownload && !StringUtils.isEmpty(dllink) && !StringUtils.containsIgnoreCase(dllink, ".m3u8")) {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = brc.openHeadConnection(dllink);
                if (looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
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

    /**
     * @returns: Not null = video is only available for friends of user XXX
     */
    private String isVideoOnlyForFriendsOf(final Browser br) {
        String friendsname = br.getRegex(">([^<>\"]*?)</a>\\'s friends only</div>").getMatch(0);
        if (StringUtils.isEmpty(friendsname)) {
            /* 2019-06-05 */
            friendsname = br.getRegex("This video is visible to <br>friends of <a href=\"[^\"]+\">([^<>\"]+)</a> only").getMatch(0);
        }
        if (friendsname != null) {
            return Encoding.htmlDecode(friendsname).trim();
        } else {
            return null;
        }
    }

    private boolean isVideoOnlyForFriends(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 423 && br.containsHTML(">\\s*This (gallery|video) is visible (for|to) <")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isPasswordProtected(final Browser br) {
        return br.containsHTML("class\\s*=\\s*\"video\\-password\\-block\"");
    }

    private boolean isPaidContent(final Browser br) {
        if (br.containsHTML("(?i)class\\s*=\\s*\"buy_tips\"|<tipt>\\s*This video is paid\\s*</tipt>")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns best filesize if isDownload == false, returns best downloadurl if isDownload == true.
     *
     * @throws Exception
     */
    private String getDllinkPremium(final boolean isDownload) throws Exception {
        final String[] htmls = br.getRegex("(<a[^<>]*class\\s*=\\s*\"list__item[^\"]*\".*?</a>)").getColumn(0);
        int foundHighestQualityHeight = -1;
        int foundUserPreferredHeight = -1;
        String internalVideoID = null;
        String filesizeHighestStr = null;
        String filesizeUserPreferredStr = null;
        final int userPreferredQualityHeight = getPreferredQualityHeight();
        for (final String html : htmls) {
            final String qualityIdentifierStr = new Regex(html, "(\\d+)p").getMatch(0);
            final String qualityFilesizeStr = new Regex(html, "\\((\\d+ (MB|GB))\\)").getMatch(0);
            if (qualityIdentifierStr == null || qualityFilesizeStr == null) {
                /* Skip invalid items */
                continue;
            }
            if (internalVideoID == null) {
                /* This id is the same for every quality. */
                internalVideoID = new Regex(html, "data\\-el\\-item\\-id\\s*=\\s*\"(\\d+)\"").getMatch(0);
            }
            final int heightTmp = Integer.parseInt(qualityIdentifierStr);
            if (heightTmp == userPreferredQualityHeight) {
                foundUserPreferredHeight = heightTmp;
                filesizeUserPreferredStr = qualityFilesizeStr;
                break;
            }
            if (heightTmp > foundHighestQualityHeight || foundHighestQualityHeight == -1) {
                foundHighestQualityHeight = heightTmp;
                filesizeHighestStr = qualityFilesizeStr;
            }
        }
        final int chosenQualityHeight;
        final String chosenQualityFilesizeStr;
        if (filesizeUserPreferredStr != null) {
            /* Found user preferred quality */
            chosenQualityFilesizeStr = filesizeUserPreferredStr;
            chosenQualityHeight = foundUserPreferredHeight;
        } else {
            /* Highest quality */
            chosenQualityFilesizeStr = filesizeHighestStr;
            chosenQualityHeight = foundHighestQualityHeight;
        }
        if (!isDownload) {
            /* Return filesize */
            return chosenQualityFilesizeStr;
        } else {
            /* Return downloadurl */
            if (internalVideoID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(String.format(api_base_premium + "/videos/%s/original-video-config", internalVideoID));
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
            final Map<String, Object> downloadFormats = (Map<String, Object>) entries.get("downloadFormats");
            return (String) downloadFormats.get(Integer.toString(chosenQualityHeight));
        }
    }

    private int getPreferredQualityHeight() {
        final int selected_format = getPluginConfig().getIntegerProperty(SETTING_SELECTED_VIDEO_FORMAT, 0);
        switch (selected_format) {
        default:
        case 7:
            return 2160;
        case 6:
            return 1440;
        case 5:
            return 1080;
        case 4:
            return 960;
        case 3:
            return 720;
        case 2:
            return 480;
        case 1:
            return 240;
        case 0:
            return -1;
        }
    }

    /**
     * NOTE: They also have .mp4 version of the videos in the html code -> For mobile devices Those are a bit smaller in size
     */
    @SuppressWarnings("deprecation")
    public String getDllink(final Browser br) throws IOException, PluginException {
        final SubConfiguration cfg = getPluginConfig();
        final int selected_format = cfg.getIntegerProperty(SETTING_SELECTED_VIDEO_FORMAT, 0);
        final List<String> qualities = new ArrayList<String>();
        switch (selected_format) {
        /* Fallthrough to automatically choose the next best quality */
        default:
        case 7:
            qualities.add("2160p");
        case 6:
            qualities.add("1440p");
        case 5:
            qualities.add("1080p");
        case 4:
            qualities.add("960p");
        case 3:
            qualities.add("720p");
        case 2:
            qualities.add("480p");
        case 1:
            qualities.add("240p");
        }
        Map<String, Object> hlsMaster = null;
        try {
            final Map<String, Object> json = restoreFromString(br.getRegex(">\\s*window\\.initials\\s*=\\s*(\\{.*?\\})\\s*;\\s*<").getMatch(0), TypeRef.MAP);
            final List<Map<String, Object>> sources = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(json, "xplayerSettings/sources/standard/mp4");
            if (sources != null) {
                for (final String quality : qualities) {
                    for (Map<String, Object> source : sources) {
                        final String qualityTmp = (String) source.get("quality");
                        String url = (String) source.get("url");
                        if (hlsMaster == null && StringUtils.containsIgnoreCase(url, ".m3u8")) {
                            hlsMaster = source;
                            continue;
                        }
                        if (!StringUtils.equalsIgnoreCase(quality, qualityTmp) || StringUtils.isEmpty(url)) {
                            continue;
                        }
                        String fallback = (String) source.get("fallback");
                        /* We found the quality we were looking for. */
                        url = br.getURL(url).toString();
                        fallback = fallback != null ? br.getURL(fallback).toString() : null;
                        if (verifyURL(url)) {
                            logger.info("Sources(url):" + quality + "->" + url);
                            return url;
                        } else if (fallback != null && verifyURL(fallback)) {
                            logger.info("Sources(fallback):" + quality + "->" + fallback);
                            return fallback;
                        } else {
                            logger.info("Sources(failed):" + quality);
                            break;
                        }
                    }
                }
            }
        } catch (final JSonMapperException e) {
            logger.log(e);
        }
        logger.info("Did not find any matching quality:" + qualities);
        if (hlsMaster != null) {
            /* 2021-02-01 */
            logger.info("Try fallback to HLS download -> " + hlsMaster);
            return (String) hlsMaster.get("url");
        }
        final String newPlayer = Encoding.htmlDecode(br.getRegex("videoUrls\":\"(\\{.*?\\]\\})").getMatch(0));
        if (newPlayer != null) {
            // new player
            final Map<String, Object> map = restoreFromString(JSonStorage.restoreFromString("\"" + newPlayer + "\"", TypeRef.STRING), TypeRef.MAP);
            if (map != null) {
                for (final String quality : qualities) {
                    final Object list = map.get(quality);
                    if (list != null && list instanceof List) {
                        final List<String> urls = (List<String>) list;
                        if (urls.size() > 0) {
                            vq = quality;
                            logger.info("videoUrls:" + quality + "->" + quality);
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
                for (String url : urls) {
                    url = JSonStorage.restoreFromString(url, TypeRef.STRING);
                    if (StringUtils.containsIgnoreCase(url, ".mp4")) {
                        final boolean verified = verifyURL(url);
                        if (verified) {
                            vq = quality;
                            logger.info("oldPlayer:" + quality + "->" + quality);
                            return url;
                        }
                    }
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
                    logger.info("old3D" + quality + "->" + quality);
                    return best;
                }
            }
        }
        // is the rest still in use/required?
        String ret = null;
        logger.info("Video quality selection failed.");
        int urlmodeint = 0;
        final String urlmode = br.getRegex("url_mode=(\\d+)").getMatch(0);
        if (urlmode != null) {
            urlmodeint = Integer.parseInt(urlmode);
        }
        if (urlmodeint == 1) {
            /* Example-ID: 1815274, 1980180 */
            final Regex secondway = br.getRegex("\\&srv=(https?[A-Za-z0-9%\\.]+\\.xhcdn\\.com)\\&file=([^<>\"]*?)\\&");
            String server = br.getRegex("\\'srv\\'\\s*:\\s*\\'(.*?)\\'").getMatch(0);
            if (server == null) {
                server = secondway.getMatch(0);
            }
            String file = br.getRegex("\\'file\\'\\s*:\\s*\\'(.*?)\\'").getMatch(0);
            if (file == null) {
                file = secondway.getMatch(1);
            }
            if (server == null || file == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (file.startsWith("http")) {
                // Examplelink (ID): 968106
                ret = file;
            } else {
                // Examplelink (ID): 986043
                ret = server + "/key=" + file;
            }
            logger.info("urlmode:" + urlmodeint + "->" + ret);
        } else {
            /* E.g. url_mode == 3 */
            /* Example-ID: 685813 */
            String flashvars = br.getRegex("flashvars\\s*:\\s*\"([^<>\"]*?)\"").getMatch(0);
            ret = br.getRegex("\"(https?://\\d+\\.xhcdn\\.com/key=[^<>\"]*?)\" class=\"mp4Thumb\"").getMatch(0);
            if (ret == null) {
                ret = br.getRegex("\"(https?://\\d+\\.xhcdn\\.com/key=[^<>\"]*?)\"").getMatch(0);
            }
            if (ret == null) {
                ret = br.getRegex("\"(https?://\\d+\\.xhcdn\\.com/key=[^<>\"]*?)\"").getMatch(0);
            }
            if (ret == null) {
                ret = br.getRegex("flashvars.*?file=(https?%3.*?)&").getMatch(0);
            }
            if (ret == null && flashvars != null) {
                /* E.g. 4753816 */
                flashvars = Encoding.htmlDecode(flashvars);
                flashvars = flashvars.replace("\\", "");
                final String[] qualities2 = { "1080p", "720p", "480p", "360p", "240p" };
                for (final String quality : qualities2) {
                    ret = new Regex(flashvars, "\"" + quality + "\"\\s*:\\s*\\[\"(https?[^<>\"]*?)\"\\]").getMatch(0);
                    if (ret != null) {
                        logger.info("urlmode:" + urlmodeint + "|quality:" + quality + "->" + ret);
                        break;
                    }
                }
            }
        }
        if (ret == null) {
            // urlmode fails, eg: 1099006
            ret = br.getRegex("video\\s*:\\s*\\{[^\\}]+file\\s*:\\s*('|\")(.*?)\\1").getMatch(1);
            if (ret == null) {
                ret = PluginJSonUtils.getJson(br, "fallback");
                if (!StringUtils.isEmpty(ret)) {
                    ret = ret.replace("\\", "");
                    logger.info("urlmode(fallback):" + urlmodeint + "->" + ret);
                }
            }
        }
        if (ret != null) {
            if (ret.contains("&amp;")) {
                ret = Encoding.htmlDecode(ret);
            }
            return ret;
        } else {
            return null;
        }
    }

    public boolean verifyURL(String url) throws IOException, PluginException {
        URLConnectionAdapter con = null;
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        try {
            con = br2.openHeadConnection(url);
            if (looksLikeDownloadableContent(con)) {
                return true;
            } else {
                br2.followConnection(true);
                throw new IOException();
            }
        } catch (final IOException e) {
            logger.log(e);
            return false;
        } finally {
            try {
                con.disconnect();
            } catch (final Exception e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @SuppressWarnings("deprecation")
    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (!link.getPluginPatternMatcher().matches(TYPE_PREMIUM) && StringUtils.isEmpty(dllink)) {
            // Access the page again to get a new direct link because by checking the availability the first linkisn't valid anymore
            String passCode = link.getDownloadPassword();
            if (isPasswordProtected(br)) {
                final boolean passwordHandlingBroken = true;
                if (passwordHandlingBroken) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Password-protected handling broken svn.jdownloader.org/issues/88690");
                }
                if (passCode == null) {
                    passCode = getUserInput("Password?", link);
                }
                if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    /* New way */
                    final String videoID = getFID(link);
                    if (videoID == null) {
                        /* This should never happen */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    /* 2020-09-03: Browser sends crypted password but uncrypted password seems to work fine too */
                    final String json = String.format("[{\"name\":\"entityUnlockModelSync\",\"requestData\":{\"model\":{\"id\":null,\"$id\":\"c280e6b4-d696-479c-bb7d-eb0627d36fb1\",\"modelName\":\"entityUnlockModel\",\"itemState\":\"changed\",\"password\":\"%s\",\"entityModel\":\"videoModel\",\"entityID\":%s}}}]", passCode, videoID);
                    br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                    br.getHeaders().put("content-type", "text/plain");
                    br.getHeaders().put("accept", "*/*");
                    br.postPageRaw("/x-api", json);
                    /*
                     * 2020-09-03: E.g. wrong password:
                     * [{"name":"entityUnlockModelSync","extras":{"result":false,"error":{"password":"Falsches Passwort"}},"responseData":{
                     * "$id":"c280e6b4-d696-479c-bb7d-eb0627d36fb1"}}]
                     */
                    if (br.containsHTML("\"password\"")) {
                        link.setDownloadPassword(null);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                    }
                    link.setDownloadPassword(passCode);
                    /*
                     * 2020-09-03: WTF:
                     * [{"name":"entityUnlockModelSync","extras":{"result":false,"showCaptcha":true,"code":"403 Forbidden"},"responseData":{
                     * "$id":"c280e6b4-d696-479c-bb7d-eb0627d36fb1"}}]
                     */
                } else {
                    /* Old way */
                    br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode));
                    if (isPasswordProtected(br)) {
                        link.setDownloadPassword(null);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                    }
                    link.setDownloadPassword(passCode);
                }
            } else {
                dllink = getDllink(br);
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            if (this.isVideoOnlyForFriends(br)) {
                throw new AccountRequiredException("You need to be friends with uploader");
            } else if (isVideoOnlyForFriendsOf(br) != null) {
                throw new AccountRequiredException();
            } else if (isPaidContent(br)) {
                throw new AccountRequiredException("Paid content");
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (StringUtils.containsIgnoreCase(dllink, ".m3u8")) {
            /* 2021-02-01: HLS download */
            br.getPage(this.dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
            dl.startDownload();
        } else {
            boolean resume = true;
            if (link.getBooleanProperty(NORESUME, false)) {
                resume = false;
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, this.dllink, resume, 0);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 416) {
                    logger.info("Response code 416 --> Handling it");
                    if (link.getBooleanProperty(NORESUME, false)) {
                        link.setProperty(NORESUME, Boolean.valueOf(false));
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 30 * 60 * 1000l);
                    }
                    link.setProperty(NORESUME, Boolean.valueOf(true));
                    link.setChunksProgress(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Server error 416");
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error");
                }
            }
            dl.startDownload();
        }
    }

    public void login(final Account account, final String customCheckURLStr, final boolean force) throws Exception {
        synchronized (account) {
            // used in finally to restore browser redirect status.
            final boolean frd = br.isFollowingRedirects();
            try {
                br.setCookiesExclusive(true);
                prepBr(this, br);
                br.setFollowRedirects(true);
                /**
                 * 2020-01-31: They got their free page xhamster.com and paid faphouse.com. This plugin will always try *to login into both.
                 * Free users can also login via their premium page but they just cannot watch anything or only trailers. </br>
                 */
                boolean forceLogincheckFree = force;
                boolean forceLogincheckPremium = force;
                if (customCheckURLStr != null && force) {
                    /* Custom check-URL given -> Only check free or premium login depending on the link -> Speeds things upp. */
                    if (this.isPremiumURL(customCheckURLStr)) {
                        forceLogincheckFree = false;
                        // forceLogincheckPremium = true;
                    } else {
                        // forceLogincheckFree = true;
                        forceLogincheckPremium = false;
                    }
                }
                this.loginFree(br, account, customCheckURLStr, forceLogincheckFree);
                this.loginPremium(br, account, customCheckURLStr, forceLogincheckPremium);
                if (customCheckURLStr != null) {
                    final URL customCheckURL = new URL(customCheckURLStr);
                    if (br.getURL() == null || !br.getURL().endsWith(customCheckURL.getPath())) {
                        br.getPage(customCheckURLStr);
                    }
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                    account.clearCookies("premium");
                }
                throw e;
            } finally {
                br.setFollowRedirects(frd);
            }
        }
    }

    private void loginFree(final Browser br, final Account account, final String customCheckURL, final boolean validateCookies) throws IOException, PluginException, InterruptedException {
        final Cookies cookies = account.loadCookies("");
        if (cookies != null) {
            logger.info("Trying free cookie login");
            String freeDomain = account.getStringProperty(PROPERTY_ACCOUNT_LAST_USED_FREE_DOMAIN);
            if (freeDomain == null) {
                logger.info("Determining current free domain");
                if (customCheckURL != null && !this.isPremiumURL(customCheckURL)) {
                    br.getPage(customCheckURL);
                } else {
                    br.getPage("https://" + this.getHost() + "/");
                }
                freeDomain = br.getHost();
                logger.info("Current free domain is: " + freeDomain);
                account.setProperty(PROPERTY_ACCOUNT_LAST_USED_FREE_DOMAIN, freeDomain);
            }
            br.setCookies(freeDomain, cookies, true);
            if (!validateCookies) {
                /* Do not check cookies */
                return;
            } else {
                if (checkLoginFree(br, account, customCheckURL)) {
                    logger.info("Free cookie login successful");
                    /* Save new cookie timestamp */
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    account.setProperty(PROPERTY_ACCOUNT_LAST_USED_FREE_DOMAIN, br.getHost());
                    return;
                } else {
                    /* Try full login */
                    logger.info("Free cookie login failed");
                    br.clearCookies(null);
                }
            }
        }
        /* Access website ig it hasn't been accessed before. */
        logger.info("Performing full login");
        if (br.getRequest() == null) {
            br.getPage("https://" + this.getHost() + "/");
        }
        final String urlBeforeLogin = br.getURL();
        final String siteKeyV3 = PluginJSonUtils.getJson(br, "recaptchaKeyV3");
        final String siteKey = PluginJSonUtils.getJson(br, "recaptchaKey");
        final String id = createID();
        final String requestdataFormat = "[{\"name\":\"authorizedUserModelSync\",\"requestData\":{\"model\":{\"id\":null,\"$id\":\"%s\",\"modelName\":\"authorizedUserModel\",\"itemState\":\"unchanged\"},\"trusted\":true,\"username\":\"%s\",\"password\":\"%s\",\"remember\":1,\"redirectURL\":null,\"captcha\":\"\",\"g-recaptcha-response\":\"%s\"}}]";
        final String requestdataFormatCaptcha = "[{\"name\":\"authorizedUserModelSync\",\"requestData\":{\"model\":{\"id\":null,\"$id\":\"%s\",\"modelName\":\"authorizedUserModel\",\"itemState\":\"unchanged\"},\"username\":\"%s\",\"password\":\"%s\",\"remember\":1,\"redirectURL\":null,\"captcha\":\"\",\"trusted\":true,\"g-recaptcha-response\":\"%s\"}}]";
        String requestData = String.format(requestdataFormat, id, account.getUser(), account.getPass(), "");
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        brc.postPageRaw("/x-api", requestData);
        if (brc.containsHTML("showCaptcha\":true")) {
            logger.info("Captcha required");
            final String recaptchaV2Response;
            if (!StringUtils.isEmpty(siteKeyV3)) {
                /* 2020-03-17 */
                recaptchaV2Response = getCaptchaHelperHostPluginRecaptchaV2Invisible(this, brc, siteKeyV3).getToken();
            } else {
                /* Old */
                recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, brc, siteKey).getToken();
            }
            requestData = String.format(requestdataFormatCaptcha, id, account.getUser(), account.getPass(), recaptchaV2Response);
            /* TODO: Fix this */
            brc.postPageRaw("/x-api", requestData);
        }
        /* First login or not a premium account? One more step required! */
        if (!account.hasProperty(PROPERTY_ACCOUNT_PREMIUM_LOGIN_URL)) {
            if (!this.checkLoginFree(br, account, urlBeforeLogin)) {
                /* This should never happen! */
                throw new AccountInvalidException();
            }
        }
        account.saveCookies(brc.getCookies(br.getHost()), "");
    }

    private boolean checkLoginFree(final Browser br, final Account account, final String customCheckURL) throws IOException {
        String freeDomain = account.getStringProperty(PROPERTY_ACCOUNT_LAST_USED_FREE_DOMAIN);
        if (freeDomain == null) {
            freeDomain = br.getHost();
        }
        if (customCheckURL == null || isPremiumURL(customCheckURL)) {
            br.getPage("https://" + freeDomain + "/");
        } else {
            final String customCheckURLWithCorrectDomain = customCheckURL.replaceFirst(Browser.getHost(customCheckURL, true), freeDomain);
            br.getPage(customCheckURLWithCorrectDomain);
        }
        if (isLoggedInHTMLFree(br)) {
            logger.info("Free cookie login successful");
            /* Save new cookie timestamp */
            account.saveCookies(br.getCookies(br.getHost()), "");
            account.setProperty(PROPERTY_ACCOUNT_LAST_USED_FREE_DOMAIN, br.getHost());
            final String premiumLoginLink = br.getRegex("\"(https?://[^/]+/faphouse/out\\?xhMedium=[^\"]+)\"").getMatch(0);
            if (premiumLoginLink != null) {
                /* Premium lgin is possible. This does not mean that this is a premium account!! */
                account.setProperty(PROPERTY_ACCOUNT_PREMIUM_LOGIN_URL, premiumLoginLink);
            } else {
                account.removeProperty(PROPERTY_ACCOUNT_PREMIUM_LOGIN_URL);
                account.setType(AccountType.FREE);
            }
            return true;
        } else {
            /* Try full login */
            logger.info("Free cookie login failed");
            return false;
        }
    }

    private boolean isLoggedinHTMLAndCookiesFree(final Browser br) {
        final boolean loggedinCookies = br.getCookie(br.getHost(), "UID", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(br.getHost(), "_id", Cookies.NOTDELETEDPATTERN) != null;
        final boolean loggedinHTML = isLoggedInHTMLFree(br);
        if (loggedinCookies || loggedinHTML) {
            return true;
        } else {
            return false;
        }
    }

    /** Use this for faphouse.com */
    private boolean isLoggedinHTMLPremium(final Browser br, final Account account) {
        final String subscriptionStatus = PluginJSonUtils.getJson(br, "userHasSubscription");
        final String currentUserId = PluginJSonUtils.getJson(br, "currentUserId");
        if (subscriptionStatus == null || subscriptionStatus.equals("null") && (currentUserId == null || currentUserId.equals("null"))) {
            /* We are not logged in */
            return false;
        } else {
            /* We are logged in -> Determine premium status */
            if ("true".equals(subscriptionStatus)) {
                account.setType(AccountType.PREMIUM);
            } else {
                account.setType(AccountType.FREE);
            }
            return true;
        }
    }

    private boolean isLoggedInHTMLFree(final Browser br) {
        return br.containsHTML("class\\s*=\\s*\"profile-link-info-name\"");
    }

    private void loginPremium(final Browser br, final Account account, final String customCheckURL, final boolean validateCookies) throws IOException, PluginException, InterruptedException {
        final Cookies premiumCookies = account.loadCookies("premium");
        if (premiumCookies != null) {
            br.setCookies(domain_premium, premiumCookies);
            if (!validateCookies) {
                return;
            }
            if (this.checkLoginPremium(br, account, customCheckURL)) {
                account.saveCookies(br.getCookies(domain_premium), "premium");
                return;
            } else {
                br.clearCookies(domain_premium);
            }
        }
        final String premiumLoginURL = account.getStringProperty(PROPERTY_ACCOUNT_PREMIUM_LOGIN_URL);
        if (premiumLoginURL == null) {
            logger.info("Looks like this is not a premium account -> Do not attempt premium login (full login would require captcha)");
            return;
        }
        /* Magic link from xhamster.com which will redirect to faphouse.com and should grant us premium login cookies. */
        logger.info("Attempting premium login via magic link");
        br.getPage(premiumLoginURL);
        final String redirecturl = br.getRegex("http-equiv=\"refresh\" content=\"\\d+; url=(https?://[^\"]+)").getMatch(0);
        if (redirecturl != null) {
            br.getPage(redirecturl);
        } else {
            logger.warning("Failed to find redirect to premium domain -> Possible login failure");
        }
        if (this.isLoggedinHTMLPremium(br, account)) {
            logger.info("Premium login via magic link was successful");
            account.saveCookies(br.getCookies(domain_premium), "premium");
            return;
        } else {
            logger.info("Premium login via magic link failed --> Attempting full login");
            br.clearCookies(br.getHost());
        }
        logger.info("Performing full premium login");
        /* Login premium --> Same logindata */
        br.getPage("https://" + domain_premium + "/");
        String rcKey = br.getRegex("data-site-key=\"([^\"]+)\"").getMatch(0);
        if (rcKey == null) {
            /* Fallback: reCaptchaKey timestamp: 2020-08-04 */
            rcKey = "6LfoawAVAAAAADDXDc7xDBOkr1FQqdfUrEH5Z7up";
        }
        final Browser brc = br.cloneBrowser();
        final String recaptchaV2Response = getCaptchaHelperHostPluginRecaptchaV2Invisible(this, brc, rcKey).getToken();
        final String csrftoken = br.getRegex("data-name=\"csrf-token\" content=\"([^<>\"]+)\"").getMatch(0);
        if (csrftoken != null) {
            brc.getHeaders().put("x-csrf-token", csrftoken);
        } else {
            logger.warning("Failed to find csrftoken --> Premium login might fail because of this");
        }
        brc.postPageRaw("/api/auth/signin", String.format("{\"login\":\"%s\",\"password\":\"%s\",\"rememberMe\":\"1\",\"trackingParamsBag\":\"W10=\",\"g-recaptcha-response\":\"%s\",\"recaptcha\":\"%s\"}", account.getUser(), PluginJSonUtils.escape(account.getPass()), recaptchaV2Response, recaptchaV2Response));
        final String userId = PluginJSonUtils.getJson(brc, "userId");
        final String success = PluginJSonUtils.getJson(brc, "success");
        if ("true".equalsIgnoreCase(success) && !StringUtils.isEmpty(userId)) {
            logger.info("Premium login successful");
            account.saveCookies(brc.getCookies(domain_premium), "premium");
        } else {
            logger.info("Premium login failed");
            throw new AccountInvalidException();
        }
    }

    /** Checks premium login status and sets AccountInfo */
    private boolean checkLoginPremium(final Browser br, final Account account, final String customCheckURL) throws IOException {
        if (customCheckURL != null && this.isPremiumURL(customCheckURL)) {
            /* Check via html */
            br.getPage(customCheckURL);
            return this.isLoggedinHTMLPremium(br, account);
        } else {
            /* Check vja ajax request -> json */
            br.getPage(api_base_premium + "/subscription/get");
            /**
             * Returns "null" if cookies are valid but this is not a premium account. </br>
             * Redirects to mainpage if cookies are invalid. </br>
             * Return json if cookies are valid. </br>
             * Can also return json along with http responsecode 400 for valid cookies but user is non-premium.
             */
            final boolean looksLikeJsonResponse = br.getRequest().getHtmlCode().startsWith("{");
            if (br.getHttpConnection().getContentType().contains("json") && (looksLikeJsonResponse || br.toString().equals("null"))) {
                logger.info("Premium domain cookies seem to be VALID");
                final AccountInfo ai = new AccountInfo();
                ai.setUnlimitedTraffic();
                if (looksLikeJsonResponse) {
                    /* Premium domain cookies are valid and we can expect json */
                    /*
                     * E.g. error 400 for free users:
                     * {"errors":{"_global":["Payment system temporary unavailable. Please try later."]},"userId":1234567}
                     */
                    final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    long expireTimestamp = 0;
                    final String expireStr = (String) entries.get("expiredAt");
                    final Boolean isTrial = (Boolean) entries.get("isTrial");
                    final Boolean hasGoldSubscription = (Boolean) entries.get("hasGoldSubscription");
                    if (!StringUtils.isEmpty(expireStr)) {
                        expireTimestamp = TimeFormatter.getMilliSeconds(expireStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                    }
                    if (Boolean.TRUE.equals(hasGoldSubscription) || Boolean.TRUE.equals(isTrial) || expireTimestamp > System.currentTimeMillis()) {
                        account.setType(AccountType.PREMIUM);
                        if (Boolean.TRUE.equals(isTrial)) {
                            /* Trial account */
                            ai.setStatus(AccountType.PREMIUM.getLabel() + " [Trial]");
                        }
                        if (expireTimestamp > System.currentTimeMillis()) {
                            ai.setValidUntil(expireTimestamp, br);
                        }
                    } else {
                        /* Expired premium or free account */
                        account.setType(AccountType.FREE);
                    }
                } else {
                    /* Premium cookies are not given (or no json to check for premium) -> Must be a free account */
                    account.setType(AccountType.FREE);
                }
                account.setAccountInfo(ai);
                return true;
            } else {
                logger.info("Premium domain cookies seem to be invalid");
                return false;
            }
        }
    }

    @Deprecated
    private void oldLoginHandling(final Account account) throws IOException, PluginException, InterruptedException {
        this.loginPremium(br, account, null, true);
        /* Store premium domain cookies */
        account.saveCookies(br.getCookies(br.getURL()), "premium");
        br.getPage(api_base_premium + "/auth/endpoints");
        String xhamsterComLoginURL = PluginJSonUtils.getJson(this.br, "https://xhamster.com/premium/in");
        if (StringUtils.isEmpty(xhamsterComLoginURL)) {
            /* Fallback */
            xhamsterComLoginURL = br.getRegex("(https?://[^/]+/premium/in\\?[^<>\"]+)").getMatch(0);
        }
        if (StringUtils.isEmpty(xhamsterComLoginURL)) {
            logger.warning("Looks like this is a free account");
        } else {
            logger.info("Looks like this is a premium account");
            /* Now we should also be logged in as free- user! */
            br.getPage(xhamsterComLoginURL);
        }
        if (!isLoggedinHTMLAndCookiesFree(br)) {
            logger.info("Free login failed!");
            throw new AccountInvalidException();
        }
        account.saveCookies(br.getCookies(br.getHost()), "");
        account.setProperty(PROPERTY_ACCOUNT_LAST_USED_FREE_DOMAIN, br.getHost());
    }

    protected CaptchaHelperHostPluginRecaptchaV2 getCaptchaHelperHostPluginRecaptchaV2Invisible(PluginForHost plugin, Browser br, final String key) throws PluginException {
        return new CaptchaHelperHostPluginRecaptchaV2(this, br, key) {
            @Override
            public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                return TYPE.INVISIBLE;
            }
        };
    }

    private String createID() {
        StringBuffer result = new StringBuffer();
        byte bytes[] = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        if (bytes[6] == 15) {
            bytes[6] |= 64;
        }
        if (bytes[8] == 63) {
            bytes[8] |= 128;
        }
        for (int i = 0; i < bytes.length; i++) {
            result.append(String.format("%02x", bytes[i] & 0xFF));
            if (i == 3 || i == 5 || i == 7 || i == 9) {
                result.append("-");
            }
        }
        return result.toString();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        // final AccountInfo ai = new AccountInfo();
        login(account, null, true);
        // if (this.checkLoginPremium(br, account, null) && br.getRequest().getHtmlCode().startsWith("{")) {
        // /* Premium domain cookies are valid and we can expect json */
        // /*
        // * E.g. error 400 for free users:
        // * {"errors":{"_global":["Payment system temporary unavailable. Please try later."]},"userId":1234567}
        // */
        // final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        // long expireTimestamp = 0;
        // final String expireStr = (String) entries.get("expiredAt");
        // final Boolean isTrial = (Boolean) entries.get("isTrial");
        // final Boolean hasGoldSubscription = (Boolean) entries.get("hasGoldSubscription");
        // if (!StringUtils.isEmpty(expireStr)) {
        // expireTimestamp = TimeFormatter.getMilliSeconds(expireStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        // }
        // if (Boolean.TRUE.equals(hasGoldSubscription) || Boolean.TRUE.equals(isTrial) || expireTimestamp > System.currentTimeMillis()) {
        // account.setType(AccountType.PREMIUM);
        // if (Boolean.TRUE.equals(isTrial)) {
        // /* Trial account */
        // ai.setStatus(AccountType.PREMIUM.getLabel() + " [Trial]");
        // }
        // if (expireTimestamp > System.currentTimeMillis()) {
        // ai.setValidUntil(expireTimestamp, br);
        // }
        // } else {
        // /* Expired premium or free account */
        // account.setType(AccountType.FREE);
        // }
        // } else {
        // /* Premium cookies are not given (or no json to check for premium) -> Must be a free account */
        // account.setType(AccountType.FREE);
        // }
        return account.getAccountInfo();
    }

    public static void prepBr(Plugin plugin, Browser br) {
        for (String host : new String[] { "xhamster.com", "xhamster.xxx", "xhamster.desi", "xhamster.one", "xhamster1.desi", "xhamster2.desi" }) {
            br.setCookie(host, "lang", "en");
            br.setCookie(host, "playerVer", "old");
        }
        /**
         * 2022-07-22: Workaround for possible serverside bug: In some countries, xhamster seems to redirect users to xhamster2.com. If
         * those users send an Accept-Language header of "de,en-gb;q=0.7,en;q=0.3" they can get stuck in a redirect-loop between
         * deu.xhamster3.com and deu.xhamster3.com. </br>
         * See initial report: https://board.jdownloader.org/showthread.php?t=91170
         */
        final String acceptLanguage = "en-gb;q=0.7,en;q=0.3";
        br.setAcceptLanguage(acceptLanguage);
        br.getHeaders().put("Accept-Language", acceptLanguage);
        br.setAllowedResponseCodes(new int[] { 400, 410, 423, 452 });
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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