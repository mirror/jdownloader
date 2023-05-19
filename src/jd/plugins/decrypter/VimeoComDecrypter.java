//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.VimeoCom;
import jd.plugins.hoster.VimeoCom.VIMEO_URL_TYPE;
import jd.plugins.hoster.VimeoCom.WrongRefererException;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.containers.VimeoContainer;
import org.jdownloader.plugins.components.containers.VimeoContainer.Quality;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VimeoComDecrypter extends PluginForDecrypt {
    private final String type_player_private_external_direct = "(?i)https?://player\\.vimeo.com/external/\\d+\\.(source|hd|sd)\\.(mp4|mov|wmv|avi|flv).+";
    private final String type_player_private_play_direct     = "(?i)https?://player\\.vimeo.com/play/\\d+.+";
    private final String type_player_private_external_m3u8   = "(?i)https?://player\\.vimeo.com/external/\\d+\\..*?\\.m3u8.+";
    private final String type_player_private_external        = "(?i)https?://player\\.vimeo.com/external/\\d+((\\&|\\?|#)forced_referer=[A-Za-z0-9=]+)?";
    /*
     * 2018-03-26: Such URLs will later have an important parameter "s" inside player.vimeo.com URL. Without this String, we cannot
     * watch/download them!!
     */
    private final String type_normal                         = ".+vimeo\\.com/\\d+.*";
    private final String type_player                         = "https?://player\\.vimeo.com/video/\\d+.+";

    public VimeoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 250);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "vimeo.com" });
        return ret;
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
            /* Main domain URLs */
            StringBuilder pattern = new StringBuilder();
            pattern.append("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/");
            pattern.append("(");
            pattern.append("\\d+(?:/[a-f0-9]{2,})?").append("|");
            /* Single video of a channel */
            pattern.append("(?:[a-z]{2}/)?channels/[a-z0-9\\-_]+/\\d+").append("|");
            /* manage own video */
            pattern.append("manage/videos/(\\d+)").append("|");
            /* All videos of a user/channel */
            pattern.append("[A-Za-z0-9\\-_]+/videos").append("|");
            pattern.append("ondemand/[A-Za-z0-9\\-_]+(/\\d+)?").append("|");
            /* All videos of a group and single video of a group */
            pattern.append("groups/[A-Za-z0-9\\-_]+(?:/videos/\\d+)?").append("|");
            /* "Review" --> Also just a single video but with a special additional ID */
            pattern.append("[a-z0-9]+/review/\\d+/[a-f0-9]{2,}");
            pattern.append(")");
            /* Showcase video URLs (= single videos with special URL pattern) */
            pattern.append("|");
            pattern.append("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/showcase/\\d+/video/(\\d+).*");
            /* Showcase URLs */
            pattern.append("|");
            pattern.append("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/showcase/\\d+(?:/embed)?.*");
            /* Embedded content URLs */
            pattern.append("|");
            /* VimeoComDecrypter.type_player_private_external_direct */
            pattern.append("https?://player\\." + buildHostsPatternPart(domains) + "/external/\\d+\\.(source|hd|sd)\\.(mp4|mov|wmv|avi|flv).+");
            pattern.append("|");
            /* VimeoComDecrypter.type_player_private_play_direct */
            pattern.append("https?://player\\." + buildHostsPatternPart(domains) + "/play/\\d+.+");
            pattern.append("|");
            /* VimeoComDecrypter.type_player_private_external_m3u8 */
            pattern.append("https?://player\\." + buildHostsPatternPart(domains) + "/external/\\d+\\.[^/\\?]+?\\.m3u8.+");
            pattern.append("|");
            // /* embedded other */
            pattern.append("https?://player\\." + buildHostsPatternPart(domains) + "/(?:video|external)/\\d+((/config\\?|\\?|#).+)?");
            pattern.append("|");
            pattern.append("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/shortest/[^/]+");
            ret.add(pattern.toString());
        }
        return ret.toArray(new String[0]);
    }

    public static final String LINKTYPE_USER           = "(?i)https?://(?:www\\.)?vimeo\\.com/[A-Za-z0-9\\-_]+/videos";
    public static final String LINKTYPE_GROUP          = "(?i)https?://(?:www\\.)?vimeo\\.com/groups/[A-Za-z0-9\\-_]+(?!videos/\\d+)";
    public static final String LINKTYPE_SHOWCASE       = "(?i)https?://(?:www\\.)?vimeo\\.com/showcase/(\\d+)(?:/embed)?.*";
    public static final String LINKTYPE_SHOWCASE_VIDEO = "(?i)https?://(?:www\\.)?vimeo\\.com/showcase/(\\d+)/video/(\\d+).*";
    public static final String LINKTYPE_SHORT_REDIRECT = "(?i)https?://(?:www\\.)?vimeo\\.com/shortest/.+";

    private String guessReferer(CryptedLink param) {
        CrawledLink check = getCurrentLink().getSourceLink();
        while (check != null) {
            final String ret = check.getURL();
            if (check == check.getSourceLink() || !StringUtils.equalsIgnoreCase(Browser.getHost(ret), "vimeo.com")) {
                return ret;
            } else {
                check = check.getSourceLink();
            }
        }
        return null;
    }

    private String getForcedRefererFromURLParam(final String urlParam) {
        final String value = new Regex(urlParam, "forced_referer=([A-Za-z0-9=]+)").getMatch(0);
        if (value != null) {
            String ret = null;
            if (value.matches("^[a-fA-F0-9]+$") && value.length() % 2 == 0) {
                final byte[] bytes = HexFormatter.hexToByteArray(value);
                ret = bytes != null ? new String(bytes) : null;
            }
            if (ret == null) {
                ret = Encoding.Base64Decode(value);
            }
            return ret;
        } else {
            return null;
        }
    }

    private boolean retryWithCustomReferer(VIMEO_URL_TYPE urlType, final CryptedLink param, final Exception e, final Browser br, final AtomicReference<String> referer) throws Exception {
        if (isEmbeddedForbidden(urlType, e, br) && SubConfiguration.getConfig("vimeo.com").getBooleanProperty("ASK_REF", Boolean.TRUE)) {
            final String vimeo_asked_referer = getUserInput("Referer?", "Please enter referer for this link", param);
            if (StringUtils.isNotEmpty(vimeo_asked_referer)) {
                try {
                    if (!StringUtils.equalsIgnoreCase(new URL(vimeo_asked_referer).getHost(), "vimeo.com")) {
                        referer.set(vimeo_asked_referer);
                        return true;
                    }
                } catch (MalformedURLException exception) {
                }
            }
        }
        return false;
    }

    private boolean isEmbeddedForbidden(VIMEO_URL_TYPE urlType, Exception e, Browser br) {
        if (e instanceof WrongRefererException) {
            return true;
        } else if (e instanceof PluginException) {
            switch (urlType) {
            case SHOWCASE:
                return ((PluginException) e).getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND && br.containsHTML("\"clips\"\\s*:\\s*\\[\\s*\\]");
            case PLAYER:
            case PLAYER_UNLISTED:
            case CONFIG_TOKEN:
                return ((PluginException) e).getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND && ((br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 403) || (br.containsHTML(">\\s*Private Video on Vimeo\\s*<") || br.containsHTML("Because of its privacy settings, this video cannot be played here.")));
            default:
                return ((PluginException) e).getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND && (br.containsHTML(">\\s*Private Video on Vimeo\\s*<") || br.containsHTML("Because of its privacy settings, this video cannot be played here."));
            }
        } else {
            return false;
        }
    }

    @Override
    public String getCrawlerLoggerID(CrawledLink link) {
        final String url = link.getURL();
        final String ret = super.getCrawlerLoggerID(link);
        if (url.matches(LINKTYPE_USER)) {
            return ret + "_user";
        } else if (url.matches(LINKTYPE_GROUP)) {
            return ret + "_group";
        } else {
            return ret;
        }
    }

    public DownloadLink createExternalOrPlayLink(String url) throws Exception {
        if (url.matches(type_player_private_external_m3u8) || url.matches(type_player_private_play_direct) || url.matches(type_player_private_external_direct)) {
            final String vimeo_forced_referer = getForcedRefererFromURLParam(url);
            if ((url.matches(type_player_private_external_direct) || url.matches(type_player_private_play_direct)) && !url.matches("^.*(\\?|&)download=1.*")) {
                // download parameter results in content-disposition header with filename
                url = url.replaceAll("download=\\d+", "download=1");
                if (!url.matches("^.*(\\?|&)download=1.*")) {
                    url = URLHelper.parseLocation(new URL(url), "&download=1");
                }
            }
            final DownloadLink link = createDownloadlink(url.replaceAll("https?://", "decryptedforVimeoHosterPlugin://"));
            link.setProperty("directURL", url);
            if (url.matches(type_player_private_play_direct)) {
                link.setProperty(VimeoCom.VIMEOURLTYPE, VIMEO_URL_TYPE.PLAY);
            } else {
                link.setProperty(VimeoCom.VIMEOURLTYPE, VIMEO_URL_TYPE.EXTERNAL);
            }
            // TODO: parse profile_id parameter and fill properties like resolution,quality....
            // profile_id=107 ?
            // profile_id=113 ?
            // profile_id=112 ?
            // profile_id=113 ?
            // profile_id=119 ?
            // profile_id=174 ?
            // profile_id=175 ?
            final String videoID = getVideoidFromURL(url);
            if (videoID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("videoID", videoID);
            if (vimeo_forced_referer != null) {
                link.setProperty("vimeo_forced_referer", vimeo_forced_referer);
            }
            String fileName = Plugin.getFileNameFromURL(new URL(url));
            if (url.matches(type_player_private_external_direct)) {
                final String details[] = new Regex(fileName, "(\\d+)\\.(.*?)\\.(.+)").getRow(0);
                if (details != null && details.length == 3) {
                    link.setProperty("videoExt", "." + details[2]);
                    try {
                        final VimeoContainer.Quality quality = VimeoContainer.Quality.valueOf(details[1].toUpperCase(Locale.ENGLISH));
                        link.setProperty("videoQuality", quality.name());
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }
            } else {
                link.setProperty("videoTitle", fileName);
            }
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                link.setAvailable(true);
            }
            if (url.matches(type_player_private_external_direct) || url.matches(type_player_private_play_direct)) {
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setAllowedResponseCodes(410);
                    brc.setFollowRedirects(false);
                    brc.getPage(url);
                    if (brc.getHttpConnection().getResponseCode() == 403) {
                        /* 403 could also mean that an account is required but probably not here in this context. */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (brc.getHttpConnection().getResponseCode() == 410) {
                        // expired link
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if (brc.getRedirectLocation() != null) {
                        fileName = UrlQuery.parse(brc.getRedirectLocation()).getDecoded("filename");
                        if (fileName != null) {
                            final String videoTitle = fileName.replaceFirst("(\\.(mp4|mov|wmv|avi|flv))", "");
                            final String extension = fileName.replaceFirst("(.+?)\\.([a-z0-9]{3})$", "$2");
                            if (StringUtils.isNotEmpty(videoTitle)) {
                                link.setProperty("videoTitle", videoTitle);
                            }
                            if (StringUtils.isNotEmpty(extension)) {
                                link.setProperty("videoExt", "." + extension);
                            }
                        }
                    } else {
                        // fast crawling can end up in "This video does not exist" but still may be online
                        throw new IOException();
                    }
                } catch (IOException e) {
                    logger.log(e);
                }
            }
            link.setFinalFileName(getFormattedFilename(link));
            return link;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported:" + url);
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final SubConfiguration cfg = SubConfiguration.getConfig("vimeo.com");
        final boolean alwaysLogin = cfg.getBooleanProperty(VimeoCom.ALWAYS_LOGIN, false);
        init(cfg);
        int skippedLinks = 0;
        String parameter = param.toString().replace("http://", "https://");
        final String orgParameter = parameter;
        if (parameter.matches(type_player_private_external_m3u8)) {
            parameter = parameter.replaceFirst("(p=.*?)($|&)", "");
            ret.add(createExternalOrPlayLink(parameter));
            return ret;
        } else if (parameter.matches(type_player_private_external_direct) || parameter.matches(type_player_private_play_direct)) {
            ret.add(createExternalOrPlayLink(parameter));
            return ret;
        } else if (parameter.matches(type_player_private_external)) {
            parameter = parameter.replace("/external/", "/video/");
        } else if (parameter.matches(LINKTYPE_SHORT_REDIRECT)) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            final String redirect = br.getRedirectLocation();
            if (redirect == null) {
                ret.add(createOfflinelink(parameter));
                return ret;
            }
            ret.add(this.createDownloadlink(redirect));
            return ret;
        }
        // when testing and dropping to frame, components will fail without clean browser.
        br = new Browser();
        br = prepBrowser(br);
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 400, 410 });
        String password = null;
        if (parameter.matches(LINKTYPE_USER) || parameter.matches(LINKTYPE_GROUP)) {
            if (alwaysLogin) {
                final ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(getHost());
                if (accs != null && accs.size() > 0) {
                    login(accs.get(0));
                }
            }
            /* Decrypt all videos of a user- or group. */
            br.getPage(parameter);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                ret.add(createOfflinelink(parameter, "Could not find that page"));
                return ret;
            }
            final String urlpart_pagination;
            final String user_or_group_id;
            String userName = null;
            if (parameter.matches(LINKTYPE_USER)) {
                user_or_group_id = new Regex(parameter, "vimeo\\.com/([A-Za-z0-9\\-_]+)/videos").getMatch(0);
                userName = br.getRegex(">Here are all of the videos that <a href=\"/user\\d+\">([^<>\"]*?)</a> has uploaded to Vimeo").getMatch(0);
                urlpart_pagination = "/" + user_or_group_id + "/videos";
            } else {
                user_or_group_id = new Regex(parameter, "vimeo\\.com/groups/([A-Za-z0-9\\-_]+)").getMatch(0);
                urlpart_pagination = "/groups/" + user_or_group_id;
            }
            if (userName == null) {
                userName = user_or_group_id;
            }
            final String totalVideoNum = br.getRegex(">(\\d+(,\\d+)?) Total</a>").getMatch(0);
            int numberofPages = 1;
            final String[] pages = br.getRegex("/page:(\\d+)/").getColumn(0);
            if (pages != null && pages.length != 0) {
                for (final String apage : pages) {
                    final int currentp = Integer.parseInt(apage);
                    if (currentp > numberofPages) {
                        numberofPages = currentp;
                    }
                }
            }
            final int totalVids;
            if (totalVideoNum != null) {
                totalVids = Integer.parseInt(totalVideoNum.replace(",", ""));
            } else {
                /* Assume number of videos/page. 12 at the moment,24.05.2019 */
                totalVids = numberofPages * 12;
            }
            final Set<String> dups = new HashSet<String>();
            for (int i = 1; i <= numberofPages; i++) {
                if (this.isAbort()) {
                    logger.info("Decrypt process aborted by user: " + parameter);
                    return ret;
                }
                if (i > 1) {
                    sleep(1000, param);
                    br.getPage(urlpart_pagination + "/page:" + i + "/sort:date/format:detail");
                }
                final String[] videoIDs = br.getRegex("id=\"clip_(\\d+)\"").getColumn(0);
                if (videoIDs == null || videoIDs.length == 0) {
                    logger.info("Found no videos on current page!?:" + i);
                } else {
                    logger.info("Found " + videoIDs.length + " videos on current page:" + i);
                }
                for (final String videoID : videoIDs) {
                    if (dups.add(videoID)) {
                        ret.add(createDownloadlink("http://" + this.getHost() + "/" + videoID));
                    } else {
                        logger.info("duplicate video detected:" + videoID);
                    }
                }
                logger.info("Decrypted page: " + i + " of " + numberofPages);
                logger.info("Found " + videoIDs.length + " videolinks on current page");
                logger.info("Found " + ret.size() + " of " + totalVids + " total videolinks");
                if (ret.size() >= totalVids) {
                    logger.info("Decrypted all videos, stopping");
                    break;
                }
            }
            logger.info("Decrypt done! Total amount of decrypted videolinks: " + ret.size() + " of " + totalVids);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName("Videos of vimeo.com user " + userName);
            fp.addLinks(ret);
        } else {
            final VIMEO_URL_TYPE urlType = jd.plugins.hoster.VimeoCom.getUrlType(parameter);
            if (VIMEO_URL_TYPE.EXTERNAL.equals(urlType)) {
                /* 2021-11-12: Unsupported pattern? */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Check if we got a forced Referer - if so, extract it, clean url, use it and set it on our DownloadLinks for later usage. */
            final AtomicReference<String> referer = new AtomicReference<String>();
            final String vimeo_forced_referer_url_part = new Regex(parameter, "((\\&|\\?|#)forced_referer=.+)").getMatch(0);
            if (referer.get() == null && vimeo_forced_referer_url_part != null) {
                parameter = parameter.replace(vimeo_forced_referer_url_part, "");
                final String vimeo_forced_referer = getForcedRefererFromURLParam(vimeo_forced_referer_url_part);
                if (vimeo_forced_referer != null) {
                    referer.set(vimeo_forced_referer);
                    logger.info("Use *forced* referer:" + vimeo_forced_referer);
                }
            }
            if (referer.get() == null) {
                switch (urlType) {
                case CONFIG_TOKEN:
                case PLAYER:
                    final UrlQuery query = UrlQuery.parse(parameter);
                    final String referrer = query.getDecoded("referrer");
                    if (StringUtils.startsWithCaseInsensitive(referrer, "http://") || StringUtils.startsWithCaseInsensitive(referrer, "https://")) {
                        referer.set(referrer);
                        logger.info("Use *config/player* referer:" + referrer);
                    }
                    break;
                default:
                    break;
                }
                if (referer.get() == null) {
                    final String vimeo_guessed_referer = guessReferer(param);
                    if (vimeo_guessed_referer != null) {
                        referer.set(vimeo_guessed_referer);
                        logger.info("Use *guessed* referer:" + vimeo_guessed_referer);
                    }
                }
            }
            String videoID = getVideoidFromURL(parameter);
            if (videoID == null && !parameter.contains("ondemand")) {
                /* This should never happen but can happen when adding support for new linktypes. */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            Object lock = new Object();
            boolean loggedIn = false;
            if (alwaysLogin) {
                final ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(getHost());
                if (accs != null && accs.size() > 0) {
                    final Account acc = accs.get(0);
                    loggedIn = login(acc);
                    if (loggedIn) {
                        lock = acc;
                    }
                }
            }
            /* Log in if required */
            if (loggedIn == false && StringUtils.containsIgnoreCase(parameter, "/ondemand/")) {
                // TODO: add check for free/accountOnly ondemand content
                logger.info("Account required to crawl this link");
                final ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(getHost());
                final Account acc = accs != null && accs.size() > 0 ? accs.get(0) : null;
                loggedIn = acc != null && login(acc);
                if (!loggedIn) {
                    logger.info("Maybe cannot crawl this link without account!");
                } else {
                    lock = acc;
                }
            }
            final Map<String, Object> properties;
            if (param.getDownloadLink() != null) {
                properties = param.getDownloadLink().getProperties();
            } else {
                properties = new HashMap<String, Object>();
            }
            synchronized (lock) {
                try {
                    try {
                        jd.plugins.hoster.VimeoCom.accessVimeoURL(this, this.br, parameter, referer, urlType, properties);
                    } catch (final Exception e) {
                        if (retryWithCustomReferer(urlType, param, e, br, referer)) {
                            jd.plugins.hoster.VimeoCom.accessVimeoURL(this, this.br, parameter, referer, urlType, properties);
                        } else {
                            throw e;
                        }
                    }
                } catch (final DecrypterRetryException e) {
                    logger.log(e);
                    if (RetryReason.PASSWORD.equals(e.getReason())) {
                        password = handlePW(param, this.br);
                    } else {
                        throw e;
                    }
                } catch (final PluginException e) {
                    logger.log(e);
                    if (e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                        if (isPasswordProtected(this.br)) {
                            password = handlePW(param, this.br);
                        } else {
                            ret.add(createOfflinelink(parameter, videoID, null));
                            return ret;
                        }
                    } else if (isPasswordProtected(this.br)) {
                        password = handlePW(param, this.br);
                    } else {
                        throw e;
                    }
                }
                if (isPasswordProtected(this.br)) {
                    password = handlePW(param, this.br);
                }
                if (VIMEO_URL_TYPE.SHOWCASE.equals(urlType)) {
                    final String showcaseID = new Regex(parameter, LINKTYPE_SHOWCASE).getMatch(0);
                    final String refURL = referer.get();
                    final String appendReferer;
                    if (refURL != null) {
                        appendReferer = "#forced_referer=" + HexFormatter.byteArrayToHex(refURL.getBytes("UTF-8"));
                    } else {
                        appendReferer = "";
                    }
                    final String jsonStringOld = br.getRegex("<script\\s*id\\s*=\\s*\"app-data\"\\s*type\\s*=\\s*\"application/json\"\\s*>\\s*(.*?)\\s*</script>").getMatch(0);
                    boolean apiMode = true;
                    if (jsonStringOld != null) {
                        final Map<String, Object> json = restoreFromString(jsonStringOld, TypeRef.MAP);
                        final List<Map<String, Object>> clips = (List<Map<String, Object>>) json.get("clips");
                        if (clips == null) {
                            final String error = (String) json.get("error");
                            if ("not_accessible".equals(error)) {
                                // TODO: refURL handling missing
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, error);
                            } else {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, error);
                            }
                        }
                        for (Map<String, Object> clip : clips) {
                            final String config = (String) clip.get("config");
                            if (config != null) {
                                apiMode = false;
                                final DownloadLink clipEntry = this.createDownloadlink(config + appendReferer);
                                ret.add(clipEntry);
                            }
                        }
                    }
                    if (apiMode) {
                        br.getPage("/showcase/" + showcaseID + "/auth");
                        String passCode = null;
                        if (br.getHttpConnection().getResponseCode() == 401) {
                            logger.info("Password required");
                            /* Extra token required TODO: This is ugly and should be part of "getJWT()"! */
                            final Browser brc = br.cloneBrowser();
                            brc.getPage("https://vimeo.com/_rv/viewer");
                            final Map<String, Object> entries = restoreFromString(brc.toString(), TypeRef.MAP);
                            final String xsrft_token = (String) entries.get("xsrft");
                            boolean success = false;
                            int attempt = 0;
                            do {
                                passCode = getUserInput("Password?", param);
                                final Form pwform = new Form();
                                pwform.setMethod(MethodType.POST);
                                pwform.setAction(br.getURL());
                                pwform.put("password", Encoding.urlEncode(passCode));
                                pwform.put("token", Encoding.urlEncode(xsrft_token));
                                pwform.put("referer_url", "/showcase/" + showcaseID);
                                logger.info("Password attempt " + attempt + ":" + passCode);
                                br.submitForm(pwform);
                                if (br.getHttpConnection().getResponseCode() == 401) {
                                    attempt++;
                                    continue;
                                } else {
                                    /* Correct password */
                                    success = true;
                                    break;
                                }
                            } while (attempt <= 2);
                            if (!success) {
                                throw new DecrypterException(DecrypterException.PASSWORD);
                            }
                        }
                        final String passwordCookieKey = showcaseID + "_albumpassword";
                        String passwordCookieValue = null;
                        if (passCode != null) {
                            passwordCookieValue = br.getCookie(br.getHost(), passwordCookieKey);
                        }
                        final String jwtToken = VimeoCom.getVIEWER(this, br)[0];
                        String nextPage = "/albums/" + showcaseID + "/videos?fields=link&page=1&per_page=10";
                        while (!this.isAbort() && nextPage != null) {
                            logger.info("Crawling video album page: " + nextPage);
                            final Browser brc = br.cloneBrowser();
                            brc.getHeaders().put("Authorization", "jwt " + jwtToken);
                            String response = brc.getPage("//api.vimeo.com" + nextPage);
                            if (response.matches("(?s)^\\s*\\{.+\\}\\s*$")) {
                                final Map<String, Object> map = restoreFromString(response, TypeRef.MAP);
                                final List<Map<String, Object>> data = (List<Map<String, Object>>) map.get("data");
                                if (data != null && data.size() > 0) {
                                    for (Map<String, Object> entry : data) {
                                        final String link = (String) entry.get("link");
                                        final DownloadLink clipEntry = this.createDownloadlink(link + appendReferer);
                                        if (passCode != null) {
                                            /* Store this for faster post-processing */
                                            clipEntry.setDownloadPassword(passCode);
                                            if (passwordCookieValue != null) {
                                                clipEntry.setProperty(VimeoCom.PROPERTY_PASSWORD_COOKIE_KEY, passwordCookieKey);
                                                clipEntry.setProperty(VimeoCom.PROPERTY_PASSWORD_COOKIE_VALUE, passwordCookieValue);
                                            }
                                        }
                                        ret.add(clipEntry);
                                    }
                                    nextPage = (String) JavaScriptEngineFactory.walkJson(map, "paging/next");
                                    continue;
                                }
                            }
                            break;
                        }
                    }
                    return ret;
                }
                /*
                 * We used to simply change the vimeo.com/player/XXX links to normal vimeo.com/XXX links but in some cases, videos can only
                 * be accessed via their 'player'-link with a specified Referer - if the referer is not given in such a case the site will
                 * say that our video would be a private video.
                 */
                String ownerName = null;
                String ownerUrl = null;
                String unlistedHash = getUnlistedHashFromURL(orgParameter);
                String reviewHash = null;
                String date = null;
                String channelName = null;
                String channelUrl = null;
                String title = null;
                String description = null;
                String embed_privacy = null;
                boolean tryAlternativeMetaInfos = true;
                try {
                    final String json = VimeoCom.getJsonFromHTML(this, this.br);
                    final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json);
                    if (entries != null) {
                        if (StringUtils.containsIgnoreCase(br.getURL(), "api.vimeo.com")) {
                            title = (String) entries.get("name");
                            ownerName = (String) JavaScriptEngineFactory.walkJson(entries, "user/name");
                            ownerUrl = (String) JavaScriptEngineFactory.walkJson(entries, "user/link");
                            description = (String) JavaScriptEngineFactory.walkJson(entries, "description");
                            if (StringUtils.isEmpty(unlistedHash)) {
                                final String link = (String) JavaScriptEngineFactory.walkJson(entries, "link");
                                unlistedHash = new Regex(link, "\\.com/\\d+/([a-f0-9]{2,})").getMatch(0);
                            }
                            date = (String) JavaScriptEngineFactory.walkJson(entries, "created_time");
                            tryAlternativeMetaInfos = false;
                        }
                        if (!StringUtils.isEmpty(PluginJSonUtils.getJson(json, "reviewHash"))) {
                            /* E.g. 'review' URLs (new handling 2020-06-25) */
                            final Map<String, Object> clipData = (Map<String, Object>) entries.get("clipData");
                            title = (String) clipData.get("title");
                            description = (String) JavaScriptEngineFactory.walkJson(clipData, "description");
                            if (StringUtils.isEmpty(unlistedHash)) {
                                unlistedHash = (String) clipData.get("unlistedHash");
                            }
                            if (StringUtils.isEmpty(reviewHash)) {
                                reviewHash = (String) clipData.get("reviewHash");
                            }
                            ownerName = (String) JavaScriptEngineFactory.walkJson(clipData, "user/name");
                            ownerUrl = (String) JavaScriptEngineFactory.walkJson(clipData, "user/url");
                            tryAlternativeMetaInfos = false;
                        } else if (entries.containsKey("vimeo_esi")) {
                            /* E.g. 'review' URLs (old handling) */
                            final Map<String, Object> clipData = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "vimeo_esi/config/clipData");
                            final Map<String, Object> ownerMap = (Map<String, Object>) clipData.get("user");
                            if (ownerMap != null) {
                                ownerUrl = new Regex(ownerMap.get("url"), "(?:vimeo\\.com)?/(.+)").getMatch(0);
                                ownerName = (String) ownerMap.get("display_name");
                                if (ownerName == null) {
                                    ownerName = (String) ownerMap.get("name");
                                }
                            }
                            title = (String) clipData.get("title");
                            if (StringUtils.isEmpty(unlistedHash)) {
                                unlistedHash = (String) clipData.get("unlistedHash");
                            }
                            if (StringUtils.isEmpty(reviewHash)) {
                                reviewHash = (String) clipData.get("reviewHash");
                            }
                        } else if (entries.containsKey("video")) {
                            /* player.vimeo.com or normal vimeo.com */
                            final Map<String, Object> video = (Map<String, Object>) entries.get("video");
                            Map<String, Object> ownerMap = null;
                            if (video.containsKey("owner")) {
                                ownerMap = (Map<String, Object>) video.get("owner");
                            } else if (entries != null && entries.containsKey("owner")) {
                                ownerMap = (Map<String, Object>) entries.get("owner");
                            }
                            if (ownerMap != null) {
                                ownerUrl = new Regex(ownerMap.get("url"), "(?:vimeo\\.com)?/(.+)").getMatch(0);
                                ownerName = (String) ownerMap.get("display_name");
                                if (ownerName == null) {
                                    ownerName = (String) ownerMap.get("name");
                                }
                            }
                            date = (String) JavaScriptEngineFactory.walkJson(video, "uploaded_on");
                            if (date == null && video.containsKey("seo")) {
                                date = (String) JavaScriptEngineFactory.walkJson(video, "seo/upload_date");
                            }
                            title = (String) video.get("title");
                            if (StringUtils.isEmpty(unlistedHash)) {
                                unlistedHash = (String) video.get("unlisted_hash");
                            }
                        } else if (entries.containsKey("clip")) {
                            /* E.g. normal URLs */
                            final Map<String, Object> clip = (Map<String, Object>) entries.get("clip");
                            Map<String, Object> ownerMap = null;
                            if (clip.containsKey("owner")) {
                                ownerMap = (Map<String, Object>) clip.get("owner");
                            } else if (entries != null && entries.containsKey("owner")) {
                                ownerMap = (Map<String, Object>) entries.get("owner");
                            }
                            if (ownerMap != null) {
                                ownerUrl = new Regex(ownerMap.get("url"), "(?:vimeo\\.com)?/(.+)").getMatch(0);
                                ownerName = (String) ownerMap.get("display_name");
                                if (ownerName == null) {
                                    ownerName = (String) ownerMap.get("name");
                                }
                            }
                            date = (String) JavaScriptEngineFactory.walkJson(clip, "uploaded_on");
                            title = (String) clip.get("title");
                            if (StringUtils.isEmpty(unlistedHash)) {
                                unlistedHash = (String) clip.get("unlisted_hash");
                            }
                        } else if (StringUtils.containsIgnoreCase(parameter, "/ondemand/")) {
                            if (videoID != null && JavaScriptEngineFactory.walkJson(entries, "clips/extras_groups/{0}") != null) {
                                final List<Map<String, Object>> clips = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "clips/extras_groups/{0}/clips");
                                if (clips != null) {
                                    for (final Map<String, Object> clip : clips) {
                                        if (StringUtils.equals(videoID, String.valueOf(clip.get("id")))) {
                                            title = (String) clip.get("name");
                                            break;
                                        }
                                    }
                                }
                            }
                            if (StringUtils.isEmpty(title) && JavaScriptEngineFactory.walkJson(entries, "clips/main_groups/{0}") != null) {
                                final List<Map<String, Object>> clips = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "clips/main_groups/{0}/clips");
                                if (clips != null) {
                                    if (videoID == null && clips.size() == 1) {
                                        videoID = String.valueOf(clips.get(0).get("id"));
                                    }
                                    for (final Map<String, Object> clip : clips) {
                                        if (StringUtils.equals(videoID, String.valueOf(clip.get("id")))) {
                                            title = (String) clip.get("name");
                                            break;
                                        }
                                    }
                                }
                            }
                            if (StringUtils.isEmpty(title)) {
                                title = (String) entries.get("name");
                            }
                        }
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
                /* Grab all qualities possible. */
                final List<VimeoContainer> containers = jd.plugins.hoster.VimeoCom.find(this, urlType, br, videoID, unlistedHash, properties, download, web, web, subtitle);
                if (containers == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (download || web) {
                    boolean hasVideo = false;
                    for (final VimeoContainer container : containers) {
                        switch (container.getSource()) {
                        case DOWNLOAD:
                        case HLS:
                        case WEB:
                            hasVideo = true;
                            break;
                        default:
                            break;
                        }
                    }
                    if (!hasVideo) {
                        ret.add(createLinkCrawlerRetry(getCurrentLink(), new DecrypterRetryException(RetryReason.PLUGIN_DEFECT, "UNSUPPORTED_STREAMING_TYPE_HLS_SPLIT_AUDIO_VIDEO_" + videoID, "Unsupported streaming type! See: https://board.jdownloader.org/showthread.php?p=513109#post513109")));
                    }
                }
                if (containers.isEmpty()) {
                    return ret;
                }
                /*
                 * Both APIs we use as fallback to find additional information can only be used to display public content - it will not help
                 * us if the user has e.g. added a private/password protected video.
                 */
                final boolean isPublicContent = (VIMEO_URL_TYPE.NORMAL.equals(urlType) || VIMEO_URL_TYPE.RAW.equals(urlType)) && unlistedHash == null;
                try {
                    if (tryAlternativeMetaInfos && !StringUtils.isAllNotEmpty(title, date, description, ownerName, ownerUrl) && isPublicContent && reviewHash == null) {
                        final Browser brc = br.cloneBrowser();
                        brc.setRequest(null);
                        brc.getPage("https://vimeo.com/api/v2/video/" + videoID + ".json");
                        if (StringUtils.isEmpty(title)) {
                            title = PluginJSonUtils.getJson(brc, "title");
                        }
                        if (StringUtils.isEmpty(date)) {
                            date = PluginJSonUtils.getJson(brc, "upload_date");
                        }
                        if (StringUtils.isEmpty(ownerName)) {
                            ownerName = PluginJSonUtils.getJson(brc, "user_name");
                        }
                        if (StringUtils.isEmpty(ownerUrl)) {
                            ownerUrl = PluginJSonUtils.getJson(brc, "user_url");
                        }
                        if (StringUtils.isEmpty(description)) {
                            description = PluginJSonUtils.getJson(brc, "description");
                        }
                        embed_privacy = PluginJSonUtils.getJson(brc, "embed_privacy");
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
                try {
                    /* Fallback to find additional information */
                    if (tryAlternativeMetaInfos && (embed_privacy == null || StringUtils.equalsIgnoreCase(embed_privacy, "anywhere")) && !StringUtils.isAllNotEmpty(title, date, description, ownerName, ownerUrl) && isPublicContent) {
                        /*
                         * We're doing this request ONLY to find additional information which we were not able to get before (upload_date,
                         * description) - also this can be used as a fallback to find data which should have been found before (e.g. title,
                         * channel_name).
                         */
                        final Browser brc = br.cloneBrowser();
                        brc.setRequest(null);
                        /* https://developer.vimeo.com/api/oembed/videos */
                        brc.getPage("https://vimeo.com/api/oembed.json?url=" + URLEncode.encodeURIComponent(parameter));
                        final String author_url = PluginJSonUtils.getJson(brc, "author_url");
                        if (StringUtils.isEmpty(title)) {
                            title = PluginJSonUtils.getJson(brc, "title");
                        }
                        if (StringUtils.isEmpty(channelName)) {
                            channelName = PluginJSonUtils.getJson(brc, "channel_name");
                        }
                        if (StringUtils.isEmpty(ownerName)) {
                            ownerName = PluginJSonUtils.getJson(brc, "author_name");
                        }
                        if (StringUtils.isEmpty(ownerUrl) && author_url != null) {
                            ownerUrl = new Regex(author_url, "/(user\\d+)$").getMatch(0);
                            if (StringUtils.isEmpty(ownerUrl)) {
                                ownerUrl = new Regex(author_url, "^https?://[^/]+/(.+)$").getMatch(0);
                            }
                        }
                        if (StringUtils.isEmpty(channelUrl)) {
                            channelUrl = new Regex(PluginJSonUtils.getJson(brc, "channel_url"), "vimeo\\.com/channels/([^/]+)").getMatch(0);
                        }
                        if (StringUtils.isEmpty(date)) {
                            date = PluginJSonUtils.getJson(brc, "upload_date");
                        }
                        if (StringUtils.isEmpty(description)) {
                            description = PluginJSonUtils.getJson(brc, "description");
                        }
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
                if (StringUtils.isEmpty(channelName)) {
                    channelName = ownerName;
                }
                if (StringUtils.isEmpty(title)) {
                    /* Fallback */
                    title = videoID;
                }
                final HashMap<String, DownloadLink> dedupeMap = new HashMap<String, DownloadLink>();
                final List<DownloadLink> subtitles = new ArrayList<DownloadLink>();
                final String cleanVimeoURL;
                if (br.getURL().contains("api.vimeo.com")) {
                    switch (urlType) {
                    case PLAYER_UNLISTED:
                        if (unlistedHash == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        } else {
                            cleanVimeoURL = "https://player.vimeo.com/" + videoID + "?h=" + unlistedHash;
                        }
                        break;
                    case PLAYER:
                    case UNLISTED:
                        if (unlistedHash != null) {
                            cleanVimeoURL = "https://vimeo.com/" + videoID + "/" + unlistedHash;
                        } else {
                            cleanVimeoURL = "https://player.vimeo.com/" + videoID;
                        }
                        break;
                    default:
                    case RAW:
                        cleanVimeoURL = parameter;
                        break;
                    }
                } else {
                    cleanVimeoURL = br.getURL();
                }
                if (ownerUrl != null) {
                    ownerUrl = URLHelper.parseLocation(new URL("https://vimeo.com/"), ownerUrl);
                }
                for (final VimeoContainer container : containers) {
                    final boolean isSubtitle = VimeoContainer.Source.SUBTITLE.equals(container.getSource());
                    if (!isSubtitle && (!qualityAllowed(container) || !pRatingAllowed(container))) {
                        skippedLinks++;
                        continue;
                    }
                    // there can be multiple hd/sd etc need to identify with framesize.
                    final String linkdupeid = container.createLinkID(videoID);
                    final DownloadLink link = createDownloadlink(parameter.replaceAll("https?://", "decryptedforVimeoHosterPlugin://"));
                    link.setLinkID(linkdupeid);
                    link.setProperty(VimeoCom.VIMEOURLTYPE, urlType);
                    link.setProperty("videoID", videoID);
                    if (unlistedHash != null) {
                        link.setProperty("specialVideoID", unlistedHash);
                    }
                    // videoTitle is required!
                    link.setProperty("videoTitle", title);
                    link.setContentUrl(cleanVimeoURL);
                    if (password != null) {
                        link.setDownloadPassword(password);
                    }
                    if (referer.get() != null) {
                        link.setProperty("vimeo_forced_referer", referer.get());
                    }
                    if (date != null) {
                        link.setProperty("originalDate", date);
                    }
                    if (ownerUrl != null) {
                        link.setProperty("ownerUrl", ownerUrl);
                    }
                    if (ownerName != null) {
                        link.setProperty("ownerName", ownerName);
                    }
                    if (channelUrl != null) {
                        link.setProperty("channelUrl", channelUrl);
                    }
                    if (channelName != null) {
                        link.setProperty("channel", channelName);
                    }
                    if (container != null) {
                        link.setProperty("directURL", container.getDownloadurl());
                    }
                    link.setProperty(jd.plugins.hoster.VimeoCom.VVC, container);
                    link.setFinalFileName(getFormattedFilename(link));
                    if (container.getFilesize() > -1) {
                        link.setDownloadSize(container.getFilesize());
                    } else if (container.getEstimatedSize() != null) {
                        link.setDownloadSize(container.getEstimatedSize());
                    }
                    if (!StringUtils.isEmpty(description)) {
                        link.setComment(description);
                    }
                    link.setAvailable(true);
                    if (isSubtitle) {
                        subtitles.add(link);
                    } else {
                        final DownloadLink best = dedupeMap.get(container.bestString());
                        /* we wont use size as its not always shown for different qualities. use quality preference */
                        final int ordial_current = container.getSource().ordinal();
                        if (best == null || ordial_current > (jd.plugins.hoster.VimeoCom.getVimeoVideoContainer(best, false)).getSource().ordinal()) {
                            dedupeMap.put(container.bestString(), link);
                        }
                    }
                }
                if (dedupeMap.size() > 0 || subtitles.size() > 0) {
                    ret.addAll(subtitles);
                    if (cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_BEST, false)) {
                        ret.add(determineBest(dedupeMap));
                    } else {
                        ret.addAll(dedupeMap.values());
                    }
                    String formattedDate = null;
                    if (date != null) {
                        try {
                            final String userDefinedDateFormat = cfg.getStringProperty(VimeoCom.CUSTOM_DATE, "dd.MM.yyyy_HH-mm-ss");
                            SimpleDateFormat formatter = jd.plugins.hoster.VimeoCom.getFormatterForDate(date);
                            final Date dateStr = formatter.parse(date);
                            formattedDate = formatter.format(dateStr);
                            final Date theDate = formatter.parse(formattedDate);
                            formatter = new SimpleDateFormat(userDefinedDateFormat);
                            formattedDate = formatter.format(theDate);
                        } catch (final Throwable e) {
                            logger.log(e);
                        }
                    }
                    String customPackagename = cfg.getStringProperty(VimeoCom.CUSTOM_PACKAGENAME_SINGLE_VIDEO, VimeoCom.defaultCustomPackagenameSingleVideo);
                    if (StringUtils.isEmpty(customPackagename)) {
                        /* Fallback */
                        customPackagename = VimeoCom.defaultCustomPackagenameSingleVideo;
                    }
                    customPackagename = customPackagename.replace("*date*", formattedDate == null ? "" : formattedDate);
                    customPackagename = customPackagename.replace("*videoid*", videoID);
                    customPackagename = customPackagename.replace("*channelname*", channelName == null ? "" : channelName);
                    customPackagename = customPackagename.replace("*videoname*", title);
                    /* Generate packagename */
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(customPackagename);
                    fp.addLinks(ret);
                }
            }
        }
        if ((ret == null || ret.size() == 0) && skippedLinks == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return ret;
        }
    }

    public static String getVideoidFromURL(final String url) {
        if (url.matches(LINKTYPE_SHOWCASE_VIDEO)) {
            return new Regex(url, LINKTYPE_SHOWCASE_VIDEO).getMatch(1);
        } else {
            String ret = new Regex(url, "https?://[^/]+/play/[^/](?:\\?|&)s=(\\d+)").getMatch(0);
            if (ret == null) {
                ret = new Regex(url, "https?://[^/]+/(?:video|external|play/)?(\\d+)").getMatch(0);
                if (ret == null) {
                    ret = new Regex(url, "/(\\d+)").getMatch(0);
                }
            }
            return ret;
        }
    }

    public static String getPlayerConfigTokenFromURL(final String url) {
        final String ret = new Regex(url, "https?://[^/]+/(?:(?:video|review)/)?(?:\\d+)/config\\?.*token=(.+?)($|$)").getMatch(0);
        return ret;
    }

    public static String getUnlistedHashFromURL(final String url) {
        String ret = new Regex(url, "https?://[^/]+/(?:(?:video|review)/)?(?:\\d+)/(?!config)([a-f0-9]{2,})").getMatch(0);
        if (ret == null) {
            ret = new Regex(url, "https?://player\\.vimeo.com/[^#]*(?:\\?|&)h=([a-f0-9]{2,})").getMatch(0);
        }
        return ret;
    }

    public static String getReviewHashFromURL(final String url) {
        final String ret = new Regex(url, "/review/\\d+/([a-f0-9]{2,})").getMatch(0);
        return ret;
    }

    public boolean login(Account account) throws Exception {
        try {
            VimeoCom.login(this, br, account);
            return true;
        } catch (PluginException e) {
            logger.log(e);
            handleAccountException(account, e);
            return false;
        }
    }

    public static boolean iranWorkaround(final Browser br, final String videoID) throws IOException {
        /* Workaround for User from Iran */
        if (br.containsHTML("<body><iframe src=\"http://10\\.10\\.\\d+\\.\\d+\\?type=(Invalid Site)?\\&policy=MainPolicy")) {
            br.getPage("//player.vimeo.com/config/" + videoID);
            return true;
        } else {
            return false;
        }
    }

    private DownloadLink determineBest(final Map<String, DownloadLink> bestMap) throws Exception {
        DownloadLink bestLink = null;
        VimeoContainer bestContainer = null;
        for (final Map.Entry<String, DownloadLink> entry : bestMap.entrySet()) {
            final DownloadLink link = entry.getValue();
            final VimeoContainer container = jd.plugins.hoster.VimeoCom.getVimeoVideoContainer(link, false);
            if (bestLink == null || Quality.ORIGINAL.equals(container.getQuality()) || Quality.SOURCE.equals(container.getQuality())) {
                bestLink = link;
                bestContainer = container;
            } else if (container.getHeight() > bestContainer.getHeight()) {
                bestLink = link;
                bestContainer = container;
            } else if (container.getHeight() == bestContainer.getHeight() && container.getSource().ordinal() > bestContainer.getSource().ordinal()) {
                bestLink = link;
                bestContainer = container;
            }
        }
        return bestLink;
    }

    private boolean download;
    private boolean web;
    private boolean subtitle;
    private boolean qMOBILE;
    private boolean qHD;
    private boolean qSD;
    private boolean qORG;
    private boolean qALL;
    private boolean p240;
    private boolean p360;
    private boolean p480;
    private boolean p540;
    private boolean p720;
    private boolean p1080;
    private boolean p1440;
    private boolean p2560;
    private boolean pALL;

    public void init(final SubConfiguration cfg) {
        qMOBILE = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_MOBILE, true);
        qHD = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_HD, true);
        qSD = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_SD, true);
        qORG = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_ORIGINAL, true);
        subtitle = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.SUBTITLE, true);
        qALL = !qMOBILE && !qHD && !qSD && !qORG;
        download = qORG;
        web = qALL || qMOBILE || qHD || qSD;
        // p ratings
        p240 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_240, true);
        p360 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_360, true);
        p480 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_480, true);
        p540 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_540, true);
        p720 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_720, true);
        p1080 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_1080, true);
        p1440 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_1440, true);
        p2560 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_2560, true);
        pALL = !p240 && !p360 && !p480 && !p540 && !p720 && !p1080 && !p1440 && !p1440;
    }

    private boolean qualityAllowed(final VimeoContainer vvc) {
        if (qALL) {
            return true;
        } else {
            switch (vvc.getQuality()) {
            case ORIGINAL:
            case SOURCE:
                return qORG;
            case UHD_4K:
            case UHD:
            case FHD:
            case HD:
                return qHD;
            case SD:
                return qSD;
            case MOBILE:
                return qMOBILE;
            default:
                return false;
            }
        }
    }

    private boolean pRatingAllowed(final VimeoContainer quality) {
        if (pALL) {
            return true;
        } else {
            final int height = quality.getHeight();
            // max down
            if (height >= 2560 || StringUtils.equalsIgnoreCase(quality.getRawQuality(), "2560p")) {
                return p2560;
            } else if (height >= 1440 || StringUtils.equalsIgnoreCase(quality.getRawQuality(), "1440p")) {
                return p1440;
            } else if (height >= 1080 || StringUtils.equalsIgnoreCase(quality.getRawQuality(), "1080p")) {
                return p1080;
            } else if (height >= 720 || StringUtils.equalsIgnoreCase(quality.getRawQuality(), "720p")) {
                return p720;
            } else if (height >= 540 || StringUtils.equalsIgnoreCase(quality.getRawQuality(), "540p")) {
                return p540;
            } else if (height >= 480 || StringUtils.equalsIgnoreCase(quality.getRawQuality(), "480p")) {
                return p480;
            } else if (height >= 360 || StringUtils.equalsIgnoreCase(quality.getRawQuality(), "360p")) {
                return p360;
            } else if (height >= 240 || height >= 232 || StringUtils.equalsIgnoreCase(quality.getRawQuality(), "240p")) {
                return p240;
            } else {
                return false;
            }
        }
    }

    private static boolean isPasswordProtected(final Browser br) throws PluginException {
        return VimeoCom.isPasswordProtected(br);
    }

    private Browser prepBrowser(final Browser ibr) throws PluginException {
        return jd.plugins.hoster.VimeoCom.prepBrGeneral(this, null, ibr);
    }

    private String getXsrft(final Browser br) throws PluginException {
        return jd.plugins.hoster.VimeoCom.getXsrft(br);
    }

    private String getFormattedFilename(DownloadLink link) throws Exception {
        return jd.plugins.hoster.VimeoCom.getFormattedFilename(link);
    }

    private String handlePW(final CryptedLink param, final Browser br) throws Exception {
        final List<String> passwords = getPreSetPasswords();
        // check for a password. Store latest password in DB
        /* Try stored password first */
        final String lastUsedPass = getPluginConfig().getStringProperty("lastusedpass");
        if (StringUtils.isNotEmpty(lastUsedPass) && !passwords.contains(lastUsedPass)) {
            passwords.add(lastUsedPass);
        }
        final String videourl = br.getURL();
        String urlToAccessOnCorrectPassword = null;
        String videoID = null;
        retry: for (int i = 0; i < 3; i++) {
            final Form pwform;
            final String token;
            if (jd.plugins.hoster.VimeoCom.isPasswordProtectedReview(br)) {
                pwform = new Form();
                pwform.setMethod(MethodType.POST);
                videoID = new Regex(videourl, "/review/data/(\\d+)").getMatch(0);
                if (videoID == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                pwform.setAction("/" + videoID + "/password");
                /* First get "xsrft" token ... */
                final String viewer[] = jd.plugins.hoster.VimeoCom.getVIEWER(this, br);
                final String vuid = viewer[1];
                token = viewer[2];
                if (StringUtils.isEmpty(token)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                pwform.put("is_review", "1");
                if (!StringUtils.isEmpty(vuid)) {
                    br.setCookie(br.getURL(), "vuid", vuid);
                }
                urlToAccessOnCorrectPassword = videourl;
            } else {
                pwform = getPasswordForm(br);
                token = getXsrft(br);
                if (StringUtils.isEmpty(token)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            pwform.put("token", token);
            final String password;
            if (passwords.size() > 0) {
                i -= 1;
                password = passwords.remove(0);
            } else {
                password = getUserInput("Password for link: " + param.toString() + " ?", param);
            }
            if (password == null || "".equals(password)) {
                // empty pass?? not good...
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            pwform.put("password", Encoding.urlEncode(password));
            try {
                br.submitForm(pwform);
            } catch (final Throwable e) {
                /* HTTP/1.1 418 I'm a teapot --> lol */
                if (br.getHttpConnection().getResponseCode() == 401 || br.getHttpConnection().getResponseCode() == 418) {
                    logger.warning("Wrong password for Link: " + param.toString());
                    if (i < 2) {
                        br.getPage(videourl);
                        continue retry;
                    } else {
                        logger.warning("Exausted password retry count. " + param.toString());
                        throw new DecrypterException(DecrypterException.PASSWORD);
                    }
                }
            }
            if (isPasswordProtected(br) || br.getHttpConnection().getResponseCode() == 405 || "false".equalsIgnoreCase(br.toString())) {
                br.getPage(videourl);
                continue retry;
            }
            if (urlToAccessOnCorrectPassword != null) {
                br.getPage(urlToAccessOnCorrectPassword);
            }
            getPluginConfig().setProperty("lastusedpass", password);
            return password;
        }
        throw new DecrypterException(DecrypterException.PASSWORD);
    }

    public static Form getPasswordForm(final Browser br) throws PluginException {
        final Form pwForm = br.getFormbyProperty("id", "pw_form");
        if (pwForm == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return pwForm;
        }
    }

    public static String createPrivateVideoUrlWithReferer(final String vimeo_video_id, final String referer_url) throws IOException {
        final String private_vimeo_url = "https://player.vimeo.com/video/" + vimeo_video_id + "?forced_referer=" + HexFormatter.byteArrayToHex(referer_url.getBytes("UTF-8"));
        return private_vimeo_url;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}