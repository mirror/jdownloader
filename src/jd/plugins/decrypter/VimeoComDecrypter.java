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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.VimeoCom;
import jd.plugins.hoster.VimeoCom.VIMEO_URL_TYPE;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.plugins.components.containers.VimeoContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vimeo.com" }, urls = { "https?://(?:www\\.)?vimeo\\.com/(\\d+(?:/[a-f0-9]+)?|(?:[a-z]{2}/)?channels/[a-z0-9\\-_]+/\\d+|[A-Za-z0-9\\-_]+/videos|ondemand/[A-Za-z0-9\\-_]+(/\\d+)?|groups/[A-Za-z0-9\\-_]+(?:/videos/\\d+)?)|https?://player\\.vimeo.com/(?:video|external)/\\d+((\\?|#).+)?|https?://(?:www\\.)?vimeo\\.com/[a-z0-9]+/review/\\d+/[a-f0-9]+" })
public class VimeoComDecrypter extends PluginForDecrypt {
    private static final String type_player_private_external_direct = "https?://player\\.vimeo.com/external/\\d+\\.[A-Za-z]{1,5}\\.mp4.+";
    private static final String type_player_private_external_m3u8   = "https?://player\\.vimeo.com/external/\\d+\\.*?\\.m3u8.+";
    private static final String type_player_private_external        = "https?://player\\.vimeo.com/external/\\d+((\\&|\\?|#)forced_referer=[A-Za-z0-9=]+)?";
    private static final String type_player_private_forced_referer  = "https?://player\\.vimeo.com/video/\\d+.*?(\\&|\\?|#)forced_referer=[A-Za-z0-9=]+";
    /*
     * 2018-03-26: Such URLs will later have an important parameter "s" inside player.vimeo.com URL. Without this String, we cannot
     * watch/download them!!
     */
    public static final String  type_normal                         = ".+vimeo\\.com/\\d+.*";
    public static final String  type_player                         = "https?://player\\.vimeo.com/video/\\d+.+";

    public VimeoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String LINKTYPE_USER  = "https?://(?:www\\.)?vimeo\\.com/[A-Za-z0-9\\-_]+/videos";
    private static final String LINKTYPE_GROUP = "https?://(?:www\\.)?vimeo\\.com/groups/[A-Za-z0-9\\-_]+(?!videos/\\d+)";

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

    private boolean retryWithCustomReferer(final CryptedLink param, final PluginException e, final Browser br, final AtomicReference<String> referer) throws Exception {
        if (isEmbeddedForbidden(e, br) && SubConfiguration.getConfig("vimeo.com").getBooleanProperty("ASK_REF", Boolean.TRUE)) {
            final String vimeo_asked_referer = getUserInput("Please enter referer for this link", param);
            if (StringUtils.isNotEmpty(vimeo_asked_referer) && !StringUtils.equalsIgnoreCase(Browser.getHost(vimeo_asked_referer), "vimeo.com")) {
                referer.set(vimeo_asked_referer);
                return true;
            }
        }
        return false;
    }

    private boolean isEmbeddedForbidden(PluginException e, Browser br) {
        return e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND && ((br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 403) || (br.containsHTML(">\\s*Private Video on Vimeo\\s*<")));
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final SubConfiguration cfg = SubConfiguration.getConfig("vimeo.com");
        final boolean alwaysLogin = cfg.getBooleanProperty(VimeoCom.ALWAYS_LOGIN, false);
        init(cfg);
        int skippedLinks = 0;
        String parameter = param.toString().replace("http://", "https://");
        final String orgParameter = parameter;
        if (parameter.matches(type_player_private_external_m3u8)) {
            parameter = parameter.replaceFirst("(p=.*?)($|&)", "");
            final DownloadLink link = this.createDownloadlink(parameter);
            decryptedLinks.add(link);
            return decryptedLinks;
        } else if (parameter.matches(type_player_private_external_direct)) {
            final DownloadLink link = this.createDownloadlink("directhttp://" + parameter.replaceFirst("%20.+$", ""));
            decryptedLinks.add(link);
            final String fileName = Plugin.getFileNameFromURL(new URL(parameter));
            link.setForcedFileName(fileName);
            link.setFinalFileName(fileName);
            return decryptedLinks;
        } else if (parameter.matches(type_player_private_external)) {
            parameter = parameter.replace("/external/", "/video/");
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
                decryptedLinks.add(createOfflinelink(parameter, "Could not find that page"));
                return decryptedLinks;
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
                    return decryptedLinks;
                }
                if (i > 1) {
                    sleep(1000, param);
                    br.getPage(urlpart_pagination + "/page:" + i + "/sort:date/format:detail");
                }
                final String[] videoIDs = br.getRegex("id=\"clip_(\\d+)\"").getColumn(0);
                if (videoIDs == null || videoIDs.length == 0) {
                    logger.info("Found no videos on current page -> Stopping");
                    break;
                }
                for (final String videoID : videoIDs) {
                    if (dups.add(videoID)) {
                        decryptedLinks.add(createDownloadlink("http://vimeo.com/" + videoID));
                    } else {
                        logger.info("duplicate video detected:" + videoID);
                    }
                }
                logger.info("Decrypted page: " + i + " of " + numberofPages);
                logger.info("Found " + videoIDs.length + " videolinks on current page");
                logger.info("Found " + decryptedLinks.size() + " of " + totalVids + " total videolinks");
                if (decryptedLinks.size() >= totalVids) {
                    logger.info("Decrypted all videos, stopping");
                    break;
                }
            }
            logger.info("Decrypt done! Total amount of decrypted videolinks: " + decryptedLinks.size() + " of " + totalVids);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName("Videos of vimeo.com user " + userName);
            fp.addLinks(decryptedLinks);
        } else {
            /* Check if we got a forced Referer - if so, extract it, clean url, use it and set it on our DownloadLinks for later usage. */
            final AtomicReference<String> referer = new AtomicReference<String>(null);
            final String vimeo_forced_referer_url_part = new Regex(parameter, "((\\&|\\?|#)forced_referer=.+)").getMatch(0);
            if (vimeo_forced_referer_url_part != null) {
                parameter = parameter.replace(vimeo_forced_referer_url_part, "");
                final String vimeo_forced_referer = getForcedRefererFromURLParam(vimeo_forced_referer_url_part);
                if (vimeo_forced_referer != null) {
                    referer.set(vimeo_forced_referer);
                    logger.info("Use *forced* referer:" + vimeo_forced_referer);
                }
            }
            if (referer.get() == null) {
                final String vimeo_guessed_referer = guessReferer(param);
                if (vimeo_guessed_referer != null) {
                    referer.set(vimeo_guessed_referer);
                    logger.info("Use *guessed* referer:" + vimeo_guessed_referer);
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
                logger.info("Account required to crawl this link");
                final ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(getHost());
                final Account acc = accs != null && accs.size() > 0 ? accs.get(0) : null;
                loggedIn = acc != null && login(acc);
                if (!loggedIn) {
                    logger.info("Cannot crawl this link without account");
                    return decryptedLinks;
                } else {
                    lock = acc;
                }
            }
            final VIMEO_URL_TYPE urlType = jd.plugins.hoster.VimeoCom.getUrlType(parameter);
            synchronized (lock) {
                try {
                    try {
                        jd.plugins.hoster.VimeoCom.accessVimeoURL(this, this.br, parameter, referer, urlType);
                    } catch (final PluginException e2) {
                        if (retryWithCustomReferer(param, e2, br, referer)) {
                            jd.plugins.hoster.VimeoCom.accessVimeoURL(this, this.br, parameter, referer, urlType);
                        } else {
                            throw e2;
                        }
                    }
                } catch (final PluginException e2) {
                    logger.log(e2);
                    if (e2.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                        decryptedLinks.add(createOfflinelink(parameter, videoID, null));
                        return decryptedLinks;
                    } else {
                        throw e2;
                    }
                }
                if (isPasswordProtected(this.br)) {
                    try {
                        password = handlePW(param, videoID, this.br);
                    } catch (final DecrypterException edc) {
                        logger.info("User entered too many wrong passwords --> Cannot decrypt link: " + parameter);
                        decryptedLinks.add(createOfflinelink(parameter, videoID, null));
                        return decryptedLinks;
                    }
                }
                final String cleanVimeoURL = br.getURL();
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
                try {
                    final String json = getJsonFromHTML(this.br);
                    final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
                    if (entries.containsKey("vimeo_esi")) {
                        /* E.g. 'review' URLs */
                        final LinkedHashMap<String, Object> clipData = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "vimeo_esi/config/clipData");
                        final LinkedHashMap<String, Object> ownerMap = (LinkedHashMap<String, Object>) clipData.get("user");
                        title = (String) clipData.get("title");
                        if (StringUtils.isEmpty(unlistedHash)) {
                            unlistedHash = (String) clipData.get("unlistedHash");
                        }
                        if (StringUtils.isEmpty(reviewHash)) {
                            reviewHash = (String) clipData.get("reviewHash");
                        }
                    } else if (entries.containsKey("video")) {
                        /* player.vimeo.com or normal vimeo.com */
                        final LinkedHashMap<String, Object> video = (LinkedHashMap<String, Object>) entries.get("video");
                        if (video.containsKey("owner")) {
                            final LinkedHashMap<String, Object> ownerMap = (LinkedHashMap<String, Object>) video.get("owner");
                            ownerUrl = new Regex(ownerMap.get("url"), "vimeo\\.com/(.+)").getMatch(0);
                            ownerName = (String) ownerMap.get("name");
                        }
                        title = (String) video.get("title");
                        if (StringUtils.isEmpty(unlistedHash)) {
                            unlistedHash = (String) video.get("unlisted_hash");
                        }
                    } else if (entries.containsKey("clip")) {
                        /* E.g. normal URLs */
                        final LinkedHashMap<String, Object> clip = (LinkedHashMap<String, Object>) entries.get("clip");
                        if (clip.containsKey("owner")) {
                            final LinkedHashMap<String, Object> ownerMap = (LinkedHashMap<String, Object>) clip.get("owner");
                            ownerUrl = new Regex(ownerMap.get("url"), "vimeo\\.com/(.+)").getMatch(0);
                            ownerName = (String) ownerMap.get("name");
                        } else if (entries != null && entries.containsKey("owner")) {
                            final LinkedHashMap<String, Object> ownerMap = (LinkedHashMap<String, Object>) entries.get("owner");
                            ownerUrl = new Regex(ownerMap.get("url"), "vimeo\\.com/(.+)").getMatch(0);
                            ownerName = (String) ownerMap.get("name");
                        }
                        title = (String) clip.get("title");
                        if (StringUtils.isEmpty(unlistedHash)) {
                            unlistedHash = (String) clip.get("unlisted_hash");
                        }
                    } else if (StringUtils.containsIgnoreCase(parameter, "/ondemand/")) {
                        List<Map<String, Object>> clips = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "clips/extras_groups/{0}/clips");
                        if (clips != null) {
                            for (final Map<String, Object> clip : clips) {
                                if (StringUtils.equals(videoID, String.valueOf(clip.get("id")))) {
                                    title = (String) clip.get("name");
                                    break;
                                }
                            }
                        }
                        if (StringUtils.isEmpty(title)) {
                            clips = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "clips/main_groups/{0}/clips");
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
                } catch (final Throwable e) {
                    logger.log(e);
                }
                final boolean tryToFindOfficialDownloadURLs = this.qORG;
                final List<VimeoContainer> containers = jd.plugins.hoster.VimeoCom.find(this, urlType, br, videoID, tryToFindOfficialDownloadURLs, qALL || qMOBILE || qMOBILE || qHD, qALL || qMOBILE || qMOBILE || qHD, subtitle);
                if (containers == null) {
                    return null;
                }
                if (containers.size() == 0) {
                    return decryptedLinks;
                }
                /*
                 * Both APIs we use as fallback to find additional information can only be used to display public content - it will not help
                 * us if the user has e.g. added a private/password protected video.
                 */
                final boolean isPublicContent = VIMEO_URL_TYPE.NORMAL.equals(urlType) || VIMEO_URL_TYPE.RAW.equals(urlType);
                String embed_privacy = null;
                try {
                    if (!StringUtils.isAllNotEmpty(title, date, description, ownerName, ownerUrl) && isPublicContent) {
                        final Browser brc = br.cloneBrowser();
                        brc.setRequest(null);
                        brc.getPage("https://vimeo.com/api/v2/video/" + videoID + ".xml");
                        if (StringUtils.isEmpty(title)) {
                            title = brc.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
                            title = Encoding.htmlOnlyDecode(title);
                        }
                        if (StringUtils.isEmpty(date)) {
                            date = brc.getRegex("<upload_date>\\s*(.*?)\\s*</upload_date>").getMatch(0);
                        }
                        if (StringUtils.isEmpty(ownerName)) {
                            ownerName = brc.getRegex("<user_name>\\s*(.*?)\\s*</user_name>").getMatch(0);
                        }
                        if (StringUtils.isEmpty(ownerUrl)) {
                            ownerUrl = new Regex(brc.getRegex("<user_url>\\s*(.*?)\\s*</user_url>").getMatch(0), "vimeo\\.com/([^/]+)").getMatch(0);
                        }
                        if (StringUtils.isEmpty(description)) {
                            description = brc.getRegex("<description>\\s*(.*?)\\s*</description>").getMatch(0);
                            description = Encoding.htmlOnlyDecode(description);
                        }
                        embed_privacy = brc.getRegex("<description>\\s*(.*?)\\s*</description>").getMatch(0);
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
                try {
                    /* Fallback to find additional information */
                    if (StringUtils.equalsIgnoreCase(embed_privacy, "anywhere") && !StringUtils.isAllNotEmpty(title, date, description, ownerName, ownerUrl) && isPublicContent) {
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
                        if (best == null || container.getSource().ordinal() > (jd.plugins.hoster.VimeoCom.getVimeoVideoContainer(best, false)).getSource().ordinal()) {
                            dedupeMap.put(container.bestString(), link);
                        }
                    }
                }
                if (dedupeMap.size() > 0 || subtitles.size() > 0) {
                    decryptedLinks.addAll(subtitles);
                    if (cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_BEST, false)) {
                        decryptedLinks.add(determineBest(dedupeMap));
                    } else {
                        decryptedLinks.addAll(dedupeMap.values());
                    }
                    String fpName = "";
                    if (StringUtils.isNotEmpty(channelName)) {
                        fpName += Encoding.htmlDecode(channelName.trim()) + " - ";
                    }
                    if (date != null) {
                        try {
                            final String userDefinedDateFormat = cfg.getStringProperty("CUSTOM_DATE_3", "dd.MM.yyyy_HH-mm-ss");
                            SimpleDateFormat formatter = jd.plugins.hoster.VimeoCom.getFormatterForDate(date);
                            Date dateStr = formatter.parse(date);
                            String formattedDate = formatter.format(dateStr);
                            Date theDate = formatter.parse(formattedDate);
                            formatter = new SimpleDateFormat(userDefinedDateFormat);
                            formattedDate = formatter.format(theDate);
                            fpName += formattedDate + " - ";
                        } catch (final Throwable e) {
                            LogSource.exception(logger, e);
                        }
                    }
                    if (StringUtils.isNotEmpty(title)) {
                        fpName += title;
                    }
                    if (StringUtils.isNotEmpty(fpName)) {
                        final FilePackage fp = FilePackage.getInstance();
                        fp.setName(fpName);
                        fp.addLinks(decryptedLinks);
                    }
                }
            }
        }
        if ((decryptedLinks == null || decryptedLinks.size() == 0) && skippedLinks == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    public static String getVideoidFromURL(final String url) {
        String ret = new Regex(url, "https?://[^/]+/(?:video/)?(\\d+)").getMatch(0);
        if (ret == null) {
            ret = new Regex(url, "/(\\d+)").getMatch(0);
        }
        return ret;
    }

    public static String getUnlistedHashFromURL(final String url) {
        final String ret = new Regex(url, "https?://[^/]+/(?:(?:video|review)/)?(\\d+)/([a-f0-9]+)").getMatch(1);
        return ret;
    }

    public static String getReviewHashFromURL(final String url) {
        final String ret = new Regex(url, "/review/\\d+/([a-f0-9]+)").getMatch(0);
        return ret;
    }

    public static String getJsonFromHTML(final Browser br) {
        String ret = br.getRegex("window\\.vimeo\\.clip_page_config\\s*=\\s*(\\{.*?\\});").getMatch(0);
        if (ret == null) {
            ret = br.getRegex("window\\.vimeo\\.vod_title_page_config\\s*=\\s*(\\{.*?\\});").getMatch(0);
            if (ret == null) {
                ret = br.getRegex("window = _extend\\(window, (\\{.*?\\})\\);").getMatch(0);
                if (ret == null) {
                    /* player.vimeo.com */
                    ret = br.getRegex("var config = (\\{.*?\\});").getMatch(0);
                }
            }
        }
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

    private DownloadLink determineBest(HashMap<String, DownloadLink> bestMap) throws Exception {
        DownloadLink bestLink = null;
        int bestHeight = -1;
        for (final Map.Entry<String, DownloadLink> best : bestMap.entrySet()) {
            final DownloadLink link = best.getValue();
            final int height = jd.plugins.hoster.VimeoCom.getVimeoVideoContainer(link, false).getHeight();
            if (height > bestHeight) {
                bestLink = link;
                bestHeight = height;
            }
        }
        return bestLink;
    }

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
    private boolean subtitle;

    public void init(final SubConfiguration cfg) {
        qMOBILE = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_MOBILE, true);
        qHD = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_HD, true);
        qSD = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_SD, true);
        qORG = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_ORIGINAL, true);
        subtitle = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.SUBTITLE, true);
        qALL = !qMOBILE && !qHD && !qSD && !qORG;
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
        }
        switch (vvc.getQuality()) {
        case ORIGINAL:
            return qORG;
        case HD:
            return qHD;
        case SD:
            return qSD;
        case MOBILE:
            return qMOBILE;
        }
        return false;
    }

    private boolean pRatingAllowed(final VimeoContainer quality) {
        if (pALL) {
            return true;
        }
        final int height = quality.getHeight();
        // max down
        if (height >= 2560) {
            return p2560;
        }
        if (height >= 1140) {
            return p1440;
        }
        if (height >= 1080) {
            return p1080;
        }
        if (height >= 720) {
            return p720;
        }
        if (height >= 540) {
            return p540;
        }
        if (height >= 480) {
            return p480;
        }
        if (height >= 360) {
            return p360;
        }
        if (height >= 240) {
            return p240;
        }
        return false;
    }

    public static boolean isPasswordProtected(final Browser br) throws PluginException {
        return br.containsHTML("\\d+/password");
    }

    private Browser prepBrowser(final Browser ibr) throws PluginException {
        pluginLoaded();
        return ((jd.plugins.hoster.VimeoCom) vimeo_hostPlugin).prepBrGeneral(this, null, ibr);
    }

    private PluginForHost vimeo_hostPlugin = null;

    private String getXsrft(final Browser br) throws PluginException {
        return jd.plugins.hoster.VimeoCom.getXsrft(br);
    }

    private String getFormattedFilename(DownloadLink link) throws Exception {
        pluginLoaded();
        return ((jd.plugins.hoster.VimeoCom) vimeo_hostPlugin).getFormattedFilename(link);
    }

    private void pluginLoaded() throws PluginException {
        if (vimeo_hostPlugin == null) {
            vimeo_hostPlugin = JDUtilities.getPluginForHost("vimeo.com");
            if (vimeo_hostPlugin == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private String handlePW(final CryptedLink param, final String videoID, final Browser br) throws Exception {
        final List<String> passwords = getPreSetPasswords();
        // check for a password. Store latest password in DB
        /* Try stored password first */
        final String lastUsedPass = getPluginConfig().getStringProperty("lastusedpass", null);
        if (StringUtils.isNotEmpty(lastUsedPass) && !passwords.contains(lastUsedPass)) {
            passwords.add(lastUsedPass);
        }
        final String videourl = br.getURL();
        retry: for (int i = 0; i < 3; i++) {
            final Form pwpwform = getPasswordForm(br);
            pwpwform.put("token", getXsrft(br));
            final String password;
            if (passwords.size() > 0) {
                i -= 1;
                password = passwords.remove(0);
            } else {
                password = Plugin.getUserInput("Password for link: " + param.toString() + " ?", param);
            }
            if (password == null || "".equals(password)) {
                // empty pass?? not good...
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            pwpwform.put("password", Encoding.urlEncode(password));
            try {
                br.submitForm(pwpwform);
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
            getPluginConfig().setProperty("lastusedpass", password);
            return password;
        }
        throw new DecrypterException(DecrypterException.PASSWORD);
    }

    public static Form getPasswordForm(final Browser br) throws PluginException {
        final Form pwForm = br.getFormbyProperty("id", "pw_form");
        if (pwForm == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return pwForm;
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