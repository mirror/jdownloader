//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.DeviantArtComConfig;
import org.jdownloader.plugins.components.config.DeviantArtComConfig.ImageDownloadMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLSearch;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/([\\w\\-]+/(art|journal)/[\\w\\-]+-\\d+|([\\w\\-]+/)?status(?:-update)?/\\d+)" })
public class DeviantArtCom extends PluginForHost {
    private final String               TYPE_DOWNLOADALLOWED_HTML             = "(?i)class=\"text\">HTML download</span>";
    private final String               TYPE_DOWNLOADFORBIDDEN_HTML           = "<div class=\"grf\\-indent\"";
    private boolean                    downloadHTML                          = false;
    private final String               PATTERN_ART                           = "(?i)https?://[^/]+/([\\w\\-]+)/art/([\\w\\-]+)-(\\d+)";
    private final String               PATTERN_JOURNAL                       = "(?i)https?://[^/]+/([\\w\\-]+)/journal/([\\w\\-]+)-(\\d+)";
    public static final String         PATTERN_STATUS                        = "(?i)https?://[^/]+/([\\w\\-]+)/([\\w\\-]+/)?status(?:-update)?/(\\d+)";
    public static final String         PROPERTY_USERNAME                     = "username";
    public static final String         PROPERTY_TITLE                        = "title";
    public static final String         PROPERTY_TYPE                         = "type";
    private static final String        PROPERTY_OFFICIAL_DOWNLOADURL         = "official_downloadurl";
    private static final String        PROPERTY_UNLIMITED_JWT_IMAGE_URL      = "image_unlimitedjwt_url";
    private static final String        PROPERTY_IMAGE_DISPLAY_OR_PREVIEW_URL = "image_display_or_preview_url";
    private static final String        PROPERTY_VIDEO_DISPLAY_OR_PREVIEW_URL = "video_display_or_preview_url";
    /* Don't touch the following! */
    private static final AtomicInteger freeDownloadsRunning                  = new AtomicInteger(0);
    private static final AtomicInteger accountDownloadsRunning               = new AtomicInteger(0);
    /* 2022-10-31: Normal login process won't work due to their anti DDoS protection -> Only cookie login is possible */
    private final boolean              allowCookieLoginOnly                  = true;

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if (allowCookieLoginOnly) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
        } else {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
        }
    }

    /**
     * @author raztoki, pspzockerscene, Jiaz
     */
    @SuppressWarnings("deprecation")
    public DeviantArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + this.getHost() + "/join/");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + this.getHost() + "/about/policy/service/";
    }

    @Override
    public void init() {
        super.init();
        Browser.setRequestIntervalLimitGlobal(getHost(), 1500);
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return false;
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    /**
     * github.com/mikf/gallery-dl/blob/master/gallery_dl/extractor/deviantart.py
     *
     * All credit goes to @Ironchest337 </br>
     * 2023-09-19: Doesn't work anymore(?) Ticket: https://svn.jdownloader.org/issues/90403
     */
    public static String buildUnlimitedJWT(final DownloadLink link, final String url) throws UnsupportedEncodingException {
        final String path = new Regex(url, "(/f/.+)").getMatch(0);
        if (path != null) {
            final String b64Header = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0";
            final String payload = "{\"sub\":\"urn:app:\",\"iss\":\"urn:app:\",\"obj\":[[{\"path\":\"" + PluginJSonUtils.escape(path) + "\"}]],\"aud\":[\"urn:service:file.download\"]}";
            final String ret = b64Header + "." + Base64.encodeToString(payload.getBytes("UTF-8")).replaceFirst("(=+$)", "") + ".";
            return ret;
        } else {
            return null;
        }
    }

    public static Map<String, Object> parseDeviationJSON(final Plugin plugin, final DownloadLink link, Map<String, Object> deviation) {
        // author can also be id(number) of author in users map
        final Map<String, Object> author = deviation.get("author") instanceof Map ? (Map<String, Object>) deviation.get("author") : null;
        if (author != null) {
            link.setProperty(PROPERTY_USERNAME, author.get("username"));
        }
        setTitleProperty(link, (String) deviation.get("title"));
        link.setProperty(PROPERTY_TYPE, deviation.get("type"));
        final Map<String, Object> ret = new HashMap<String, Object>();
        final Map<String, Object> media = (Map<String, Object>) deviation.get("media");
        if (media != null) {
            String displayedImageURL = null;
            String unlimitedImageURL = null;
            Number unlimitedImageSize = null;
            String displayedVideoURL = null;
            Number displayedVideoSize = null;
            final boolean isImage = isImage(link);
            final boolean isVideo = isVideo(link);
            try {
                final String baseUri = (String) media.get("baseUri");
                final String prettyName = (String) media.get("prettyName");
                final List<Map<String, Object>> types = (List<Map<String, Object>>) media.get("types");
                if (types != null && StringUtils.isAllNotEmpty(baseUri, prettyName)) {
                    Map<String, Object> bestType = null;
                    final List<String> bestTypesList;
                    if (isImage) {
                        bestTypesList = Arrays.asList(new String[] { "fullview" });
                    } else if (isVideo) {
                        bestTypesList = Arrays.asList(new String[] { "video" });
                    } else {
                        bestTypesList = new ArrayList<String>(0);
                    }
                    typeStringLoop: for (final String typeString : bestTypesList) {
                        for (final Map<String, Object> type : types) {
                            if (typeString.equals(type.get("t"))) {
                                if (isImage) {
                                    bestType = type;
                                    break typeStringLoop;
                                } else if (isVideo) {
                                    if (bestType == null || ((Number) type.get("h")).intValue() > ((Number) bestType.get("h")).intValue()) {
                                        bestType = type;
                                    }
                                }
                            }
                        }
                    }
                    if (bestType != null) {
                        if (isImage) {
                            String c = (String) bestType.get("c");
                            if (c == null) {
                                if ("fullview".equals(bestType.get("t"))) {
                                    // r=1? o=true??(maybe original)
                                    c = "";// raw image without any processing?
                                } else {
                                    final Number h = (Number) bestType.get("h");
                                    final Number w = (Number) bestType.get("w");
                                    if (h != null && w != null) {
                                        c = "/v1/fit/w_" + w + ",h_" + h + "/";
                                    }
                                }
                            }
                            if (c != null) {
                                if (c.isEmpty() || c.matches("(?i).*/v1/.+")) {
                                    try {
                                        final String jwt = buildUnlimitedJWT(link, baseUri);
                                        if (jwt != null) {
                                            unlimitedImageURL = baseUri + "?token=" + jwt;
                                            unlimitedImageSize = (Number) bestType.get("f");
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().log(e);
                                    }
                                }
                                c = c.replaceFirst(",q_\\d+(,strp)?", "");
                                final List<String> tokens = (List<String>) media.get("token");
                                displayedImageURL = baseUri + c.replaceFirst("<prettyName>", Matcher.quoteReplacement(prettyName));
                                if (tokens != null) {
                                    displayedImageURL = displayedImageURL + "?token=" + tokens.get(0);
                                }
                            }
                        } else if (isVideo) {
                            displayedVideoURL = (String) bestType.get("b");
                            displayedVideoSize = (Number) bestType.get("f");
                        }
                    }
                    if (isImage && StringUtils.isEmpty(displayedImageURL)) {
                        try {
                            final String jwt = buildUnlimitedJWT(link, baseUri);
                            if (jwt != null) {
                                unlimitedImageURL = baseUri + "?token=" + jwt;
                            }
                        } catch (Exception e) {
                            plugin.getLogger().log(e);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(e);
            }
            if (unlimitedImageURL != null && isImage) {
                link.setProperty(PROPERTY_UNLIMITED_JWT_IMAGE_URL, unlimitedImageURL);
            }
            if (displayedImageURL != null && isImage) {
                link.setProperty(PROPERTY_IMAGE_DISPLAY_OR_PREVIEW_URL, displayedImageURL);
            }
            if (displayedVideoURL != null && isVideo) {
                link.setProperty(PROPERTY_VIDEO_DISPLAY_OR_PREVIEW_URL, displayedVideoURL);
            }
            ret.put("displayedImageURL", displayedImageURL);
            ret.put("unlimitedImageURL", unlimitedImageURL);
            ret.put("unlimitedImageSize", unlimitedImageSize);
            ret.put("displayedVideoURL", displayedVideoURL);
            ret.put("displayedVideoSize", displayedVideoSize);
        }
        return ret;
    }

    private static String setTitleProperty(final DownloadLink link, String title) {
        if (title != null) {
            title = title.replaceAll("(?i) on deviantart$", "");
            link.setProperty(PROPERTY_TITLE, title);
        }
        return title;
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            link.setName(new URL(link.getPluginPatternMatcher()).getPath() + getAssumedFileExtension(account, link));
        }
        this.setBrowserExclusive();
        prepBR(this.br);
        br.setFollowRedirects(true);
        if (account != null) {
            login(account, false);
        }
        final String fid = getFID(link);
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("/error\\-title\\-oops\\.png\\)") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL()) && !br.getURL().contains(fid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = null;
        String displayedImageURL = null;
        Number unlimitedImageSize = null;
        Number displayedVideoSize = null;
        String officialDownloadurl = null;
        String json = br.getRegex("window\\.__INITIAL_STATE__ = JSON\\.parse\\(\"(.*?)\"\\);").getMatch(0);
        Number officialDownloadsizeBytes = null;
        Number originalFileSizeBytes = null;
        String tierAccess = null;
        if (json != null) {
            json = PluginJSonUtils.unescape(json);
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            final Map<String, Object> entities = (Map<String, Object>) entries.get("@@entities");
            final Map<String, Object> user = (Map<String, Object>) entities.get("user");
            final Map<String, Object> deviation = (Map<String, Object>) entities.get("deviation");
            Map<String, Object> thisArt = (Map<String, Object>) deviation.get(fid);
            String alternativeDeviationID = null;
            if (thisArt == null) {
                /* E.g. https://www.deviantart.com/shinysmeargle/status-update/12312835 */
                final Iterator<Entry<String, Object>> iterator = deviation.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Entry<String, Object> entry = iterator.next();
                    final String key = entry.getKey();
                    if (key.matches("^\\d+-" + fid + "$")) {
                        alternativeDeviationID = key;
                        thisArt = (Map<String, Object>) entry.getValue();
                    }
                }
            }
            tierAccess = (String) thisArt.get("tierAccess");
            final Map<String, Object> deviationResult = parseDeviationJSON(this, link, thisArt);
            displayedImageURL = (String) deviationResult.get("displayedImageURL");
            unlimitedImageSize = (Number) deviationResult.get("unlimitedImageSize");
            displayedVideoSize = (Number) deviationResult.get("displayedVideoSize");
            final Map<String, Object> thisUser = (Map<String, Object>) user.get(thisArt.get("author").toString());
            title = link.getStringProperty(PROPERTY_TITLE);
            if (title == null) {
                title = (String) thisArt.get("title");
                setTitleProperty(link, title);
            }
            String username = link.getStringProperty(PROPERTY_USERNAME);
            if (username == null) {
                username = thisUser.get("username").toString();
                link.setProperty(PROPERTY_USERNAME, username);
            }
            if (title != null) {
                title += " by " + username + "_ " + fid;
            } else if (isStatus(link)) {
                /* A status typically doesn't have a title so we goota create our own. */
                title = fid + " by " + username;
            }
            if (StringUtils.isEmpty(officialDownloadurl)) {
                final Map<String, Object> deviationExtended = (Map<String, Object>) entities.get("deviationExtended");
                if (deviationExtended != null) {
                    Map<String, Object> deviationExtendedThisArt = (Map<String, Object>) deviationExtended.get(fid);
                    if (deviationExtendedThisArt == null && alternativeDeviationID != null) {
                        /*
                         * PATTERN_STATUS might be listed with key <someNumbers>-<fid> also typically deviationExtended will only contain
                         * one item.
                         */
                        deviationExtendedThisArt = (Map<String, Object>) deviationExtended.get(alternativeDeviationID);
                    }
                    if (deviationExtendedThisArt != null) {
                        final Map<String, Object> download = (Map<String, Object>) deviationExtendedThisArt.get("download");
                        final Map<String, Object> originalFile = (Map<String, Object>) deviationExtendedThisArt.get("originalFile");
                        if (download != null) {
                            officialDownloadurl = download.get("url").toString();
                            officialDownloadsizeBytes = (Number) download.get("filesize");
                        }
                        if (originalFile != null) {
                            originalFileSizeBytes = (Number) originalFile.get("filesize");
                        }
                    }
                }
            }
        }
        /* Fallbacks via website-html */
        if (title == null) {
            title = HTMLSearch.searchMetaTag(br, "og:title");
            setTitleProperty(link, title);
        }
        if (StringUtils.isEmpty(displayedImageURL) && isImage(link)) {
            displayedImageURL = HTMLSearch.searchMetaTag(br, "og:image");
            if (displayedImageURL != null) {
                link.setProperty(PROPERTY_IMAGE_DISPLAY_OR_PREVIEW_URL, displayedImageURL);
            }
        }
        final String officialDownloadFilesizeStr = br.getRegex("(?i)>\\s*Image size\\s*</div><div [^>]*>\\d+x\\d+px\\s*(\\d+[^>]+)</div>").getMatch(0);
        // final boolean accountNeededForOfficialDownload = br.containsHTML("(?i)Log in to download");
        if (StringUtils.isEmpty(officialDownloadurl)) {
            officialDownloadurl = br.getRegex("data-hook=\"download_button\"[^>]*href=\"(https?://[^\"]+)").getMatch(0);
            if (officialDownloadurl != null) {
                officialDownloadurl = Encoding.htmlOnlyDecode(officialDownloadurl);
            }
        }
        link.setProperty(PROPERTY_OFFICIAL_DOWNLOADURL, officialDownloadurl);
        final boolean isImage = isImage(link);
        final boolean isVideo = isVideo(link);
        final boolean isLiterature = isLiterature(link);
        final boolean isStatus = isStatus(link);
        final DeviantArtComConfig cfg = PluginJsonConfig.get(DeviantArtComConfig.class);
        final ImageDownloadMode mode = cfg.getImageDownloadMode();
        String forcedExt = null;
        /* Check if either user wants to download the html code or if we have a linktype which needs this. */
        if (link.getPluginPatternMatcher().matches(PATTERN_JOURNAL) || link.getPluginPatternMatcher().matches(PATTERN_STATUS) || isLiterature || isStatus) {
            downloadHTML = true;
            forcedExt = ".html";
        } else if (isImage) {
            if (mode == ImageDownloadMode.HTML) {
                downloadHTML = true;
                forcedExt = ".html";
            }
        } else if (br.containsHTML(TYPE_DOWNLOADALLOWED_HTML) || br.containsHTML(TYPE_DOWNLOADFORBIDDEN_HTML)) {
            downloadHTML = true;
            forcedExt = ".html";
        }
        String dllink = null;
        try {
            dllink = getDirecturl(br, downloadHTML, link, account);
        } catch (final PluginException e) {
            /**
             * This will happen if the item is not downloadable. </br>
             * We're ignoring this during linkcheck as by now we know the file is online.
             */
        }
        String extByMimeType = null;
        boolean allowGrabFilesizeFromHeader = false;
        if (downloadHTML) {
            try {
                link.setDownloadSize(br.getRequest().getHtmlCode().getBytes("UTF-8").length);
            } catch (final UnsupportedEncodingException ignore) {
                ignore.printStackTrace();
            }
        } else if (StringUtils.equalsIgnoreCase(dllink, officialDownloadurl) && (officialDownloadFilesizeStr != null || officialDownloadsizeBytes != null)) {
            /*
             * Set filesize of official download if: User wants official download and it is available and/or if user wants official
             * downloads only (set filesize even if official downloadurl was not found).
             */
            if (officialDownloadsizeBytes != null) {
                link.setVerifiedFileSize(officialDownloadsizeBytes.longValue());
            } else {
                link.setDownloadSize(SizeFormatter.getSize(officialDownloadFilesizeStr.replace(",", "")));
            }
        } else if (mode == ImageDownloadMode.OFFICIAL_DOWNLOAD_ELSE_PREVIEW) {
            if (isVideo && displayedVideoSize != null) {
                link.setVerifiedFileSize(displayedVideoSize.longValue());
            } else if (isImage && unlimitedImageSize != null && dllink != null && !dllink.matches("(?i).*/v1/.+")) {
                link.setVerifiedFileSize(unlimitedImageSize.longValue());
            } else {
                allowGrabFilesizeFromHeader = true;
            }
        }
        final String extByURL = dllink != null ? Plugin.getFileNameExtensionFromURL(dllink) : null;
        final String ext;
        if (forcedExt != null) {
            /* Forced ext has highest priority */
            ext = forcedExt;
        } else if (extByMimeType != null) {
            /* This one we know for sure! */
            ext = extByMimeType;
        } else if (extByURL != null) {
            ext = extByURL;
        } else {
            ext = null;
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            if (ext != null) {
                title = this.correctOrApplyFileNameExtension(title, ext);
                link.setFinalFileName(title);
            } else {
                title = this.correctOrApplyFileNameExtension(title, getAssumedFileExtension(account, link));
                link.setName(title);
            }
        } else if (!StringUtils.isEmpty(dllink)) {
            /* Last resort fallback */
            final String filenameFromURL = Plugin.getFileNameFromURL(new URL(dllink));
            if (filenameFromURL != null) {
                link.setName(filenameFromURL);
            }
        }
        if ("locked".equalsIgnoreCase(tierAccess)) {
            /* Paid content. All we could download would be a blurred image of the content. */
            /* Example: https://www.deviantart.com/ohshinakai/art/Stretched-to-the-limit-Shanoli-996105058 */
            if (originalFileSizeBytes != null) {
                link.setDownloadSize(originalFileSizeBytes.longValue());
            }
            if (isDownload) {
                throw new AccountRequiredException();
            } else {
                return AvailableStatus.TRUE;
            }
        }
        if (allowGrabFilesizeFromHeader && !cfg.isFastLinkcheckForSingleItems() && !isDownload && !StringUtils.isEmpty(dllink)) {
            /* No filesize value given -> Obtain from header */
            final Browser br2 = br.cloneBrowser();
            /* Workaround for old downloadcore bug that can lead to incomplete files */
            br2.getHeaders().put("Accept-Encoding", "identity");
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                handleConnectionErrors(br2, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String mimeTypeExtTmp = getExtensionFromMimeType(con.getRequest().getResponseHeader("Content-Type"));
                if (mimeTypeExtTmp != null) {
                    extByMimeType = "." + mimeTypeExtTmp;
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

    public static boolean isStatus(DownloadLink link) {
        if (StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_TYPE), "status") || link.getPluginPatternMatcher().matches(PATTERN_STATUS)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isImage(DownloadLink link) {
        return StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_TYPE), "image");
    }

    public static boolean isVideo(DownloadLink link) {
        return StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_TYPE), "film");
    }

    public static boolean isLiterature(DownloadLink link) {
        return StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_TYPE), "literature");
    }

    /**
     * Returns assumed file extension based on all information we currently have. Use this only for weak filenames e.g. before linkcheck is
     * done.
     */
    public static String getAssumedFileExtension(final Account account, final DownloadLink link) {
        if (isVideo(link)) {
            return ".mp4";
        } else if (isImage(link)) {
            // TODO: add isGif support
            if (PluginJsonConfig.get(DeviantArtComConfig.class).getImageDownloadMode() == ImageDownloadMode.HTML) {
                return ".html";
            } else {
                try {
                    final String url = getDirecturl(null, false, link, account);
                    final String ext = getFileNameExtensionFromURL(url);
                    if (ext != null) {
                        return ext;
                    }
                } catch (Exception e) {
                }
                return ".jpg";
            }
        } else if (isLiterature(link) || isStatus(link)) {
            /* TODO: Add proper handling to only download relevant text of this type of link and write it into .txt file. */
            return ".html";
            // return ".txt";
        } else {
            return ".html";
        }
    }

    private static boolean isAccountRequiredForOfficialDownload(final Browser br) {
        if (br.containsHTML("(?i)aria-label=\"Log in to download\"")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean looksLikeAccountRequired(final Browser br) {
        if (looksLikeAccountRequiredUploaderDecision(br) || looksLikeAccountRequiredMatureContent(br)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean looksLikeAccountRequiredUploaderDecision(final Browser br) {
        if (br.containsHTML("(?i)has limited the viewing of this artwork\\s*<")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean looksLikeAccountRequiredMatureContent(final Browser br) {
        if (br.containsHTML("(?i)>\\s*This content is intended for mature audiences")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private static String getDirecturl(final Browser br, final boolean downloadHTML, final DownloadLink link, final Account account) throws PluginException {
        final long oldVerifiedFilesize = link.getVerifiedFileSize();
        if (oldVerifiedFilesize != -1) {
            link.setVerifiedFileSize(-1);
            /* Don't loose this value. */
            link.setDownloadSize(oldVerifiedFilesize);
        }
        String dllink = null;
        if (downloadHTML) {
            if (br == null) {
                return null;
            } else {
                dllink = br.getURL();
            }
        } else if (isImage(link)) {
            /* officialDownloadurl can be given while account is not given -> Will lead to error 404 then! */
            final String officialDownloadurl = link.getStringProperty(PROPERTY_OFFICIAL_DOWNLOADURL);
            final DeviantArtComConfig cfg = PluginJsonConfig.get(DeviantArtComConfig.class);
            final ImageDownloadMode mode = cfg.getImageDownloadMode();
            if (mode == ImageDownloadMode.OFFICIAL_DOWNLOAD_ONLY) {
                /* User only wants to download items with official download option available but it is not available in this case. */
                if (isAccountRequiredForOfficialDownload(br)) {
                    /* Looks like official download is not available at all for this item */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Official download not available");
                } else if (account == null) {
                    /* Account is required to be able to use official download option. */
                    throw new AccountRequiredException();
                } else if (officialDownloadurl == null) {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Official download broken or login issue");
                } else {
                    dllink = officialDownloadurl;
                }
            } else if (account != null && officialDownloadurl != null) {
                dllink = officialDownloadurl;
            } else {
                final boolean devAllowUnlimitedJwtImageURL = false; // 2023-09-19: Doesn't work anymore
                final String unlimitedURL = link.getStringProperty(PROPERTY_UNLIMITED_JWT_IMAGE_URL);
                final String imageURL = link.getStringProperty(PROPERTY_IMAGE_DISPLAY_OR_PREVIEW_URL);
                String ret = imageURL;
                if (devAllowUnlimitedJwtImageURL && (imageURL == null || imageURL.matches("(?i).+/v1/.+")) && unlimitedURL != null) {
                    ret = unlimitedURL;
                }
                dllink = ret;
            }
        } else if (isVideo(link)) {
            /* officialDownloadurl can be given while account is not given -> Will lead to error 404 then! */
            return link.getStringProperty(PROPERTY_VIDEO_DISPLAY_OR_PREVIEW_URL);
        }
        return dllink;
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account, true);
        link.setVerifiedFileSize(-1);
        if (this.downloadHTML) {
            /* Write text to file */
            final File dest = new File(link.getFileOutput());
            IO.writeToFile(dest, br.getRequest().getHtmlCode().getBytes(br.getRequest().getCharsetFromMetaTags()), IO.SYNC.META_AND_DATA);
            /* Set filesize so user can see it in UI. */
            link.setVerifiedFileSize(dest.length());
            /* Set progress to finished - the "download" is complete ;) */
            link.getLinkStatus().setStatus(LinkStatus.FINISHED);
        } else {
            /* Download file */
            final String dllink = this.getDirecturl(br, downloadHTML, link, account);
            if (StringUtils.isEmpty(dllink)) {
                if (this.looksLikeAccountRequired(br)) {
                    throw new AccountRequiredException();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            /* Workaround for old downloadcore bug that can lead to incomplete files */
            br.getHeaders().put("Accept-Encoding", "identity");
            /* Remove hashInfo before download in case quality/mirror/user settings have changed. will be updated again on download */
            link.setHashInfo(null);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, isResumeable(link, account), 1);
            handleConnectionErrors(br, dl.getConnection());
            /* Add a download slot */
            controlMaxFreeDownloads(account, link, +1);
            try {
                /* Start download */
                dl.startDownload();
            } finally {
                /* Remove download slot */
                controlMaxFreeDownloads(account, link, -1);
            }
        }
    }

    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        if (account != null) {
            synchronized (accountDownloadsRunning) {
                final int before = accountDownloadsRunning.get();
                final int after = before + num;
                accountDownloadsRunning.set(after);
                logger.info("accountDownloadsRunning(" + link.getName() + ")|max:" + getMaxSimultanPremiumDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        } else {
            synchronized (freeDownloadsRunning) {
                final int before = freeDownloadsRunning.get();
                final int after = before + num;
                freeDownloadsRunning.set(after);
                logger.info("freeDownloadsRunning(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        }
    }

    @Override
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter con) {
        if (super.looksLikeDownloadableContent(con)) {
            return true;
        } else if (downloadHTML && con.getContentType().contains("html")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // final int max = 100;
        final int running = freeDownloadsRunning.get();
        // final int ret = Math.min(running + 1, max);
        // return ret;
        return running + 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        // final int max = 100;
        final int running = accountDownloadsRunning.get();
        // final int ret = Math.min(running + 1, max);
        // return ret;
        return running + 1;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 401) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 401", 10 * 60 * 1000l);
            } else if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 10 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 1 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Media broken?");
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        account.setType(AccountType.FREE);
        /**
         * Try to get unique username even if users use cookie login as they can theoretically enter whatever they want into username field.
         */
        final Cookies userCookies = account.loadUserCookies();
        String realUsername = getUsernameFromCookies(br);
        if (userCookies != null && !userCookies.isEmpty()) {
            if (!StringUtils.isEmpty(realUsername)) {
                account.setUser(realUsername);
            } else {
                logger.warning("Failed to find real username inside cookies");
            }
        }
        return ai;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies == null && allowCookieLoginOnly) {
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                if (userCookies != null) {
                    br.setCookies(this.getHost(), userCookies);
                    if (!force) {
                        /* Do not validate cookies */
                        return;
                    }
                    br.getPage("https://www." + this.getHost());
                    if (this.isLoggedIN(br)) {
                        logger.info("User cookie login successful");
                        return;
                    } else {
                        logger.info("User cookie login failed");
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                } else if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Do not validate cookies */
                        return;
                    }
                    br.getPage("https://www. " + this.getHost());
                    if (this.isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Performing full login");
                br.getHeaders().put("Referer", "https://www." + this.getHost());
                br.getPage("https://www." + this.getHost() + "/users/login"); // Not allowed to go directly to /users/login/
                if (br.containsHTML("Please confirm you are human")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                final boolean allowCaptcha = false;
                if (allowCaptcha && (br.containsHTML("Please confirm you are human") || (br.containsHTML("px-blocked") && br.containsHTML("g-recaptcha")))) {
                    // disabled because perimeterx code is incomplete
                    final DownloadLink dummyLink = new DownloadLink(this, "Account Login", getHost(), getHost(), true);
                    final DownloadLink odl = this.getDownloadLink();
                    this.setDownloadLink(dummyLink);
                    final CaptchaHelperHostPluginRecaptchaV2 captcha = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Lcj-R8TAAAAABs3FrRPuQhLMbp5QrHsHufzLf7b");
                    if (odl != null) {
                        this.setDownloadLink(odl);
                    }
                    final String uuid = new Regex(br.getURL(), "uuid=(.*?)($|&)").getMatch(0);
                    String vid = new Regex(br.getURL(), "vid=(.*?)($|&)").getMatch(0);
                    if (StringUtils.isEmpty(vid)) {
                        vid = "null";
                    }
                    br.setCookie(getHost(), "_pxCaptcha", URLEncoder.encode(captcha.getToken(), "UTF-8") + ":" + uuid + ":" + vid);
                    br.getPage("https://www.deviantart.com/users/login");
                }
                final Form loginform = br.getFormbyActionRegex("(?i).*do/signin");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember", "on");
                br.submitForm(loginform);
                if (!isLoggedIN(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final AccountInvalidException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        final String username = getUsernameFromCookies(br);
        if (!StringUtils.isEmpty(username) && br.containsHTML("data-hook=\"user_link\" data-username=\"" + Pattern.quote(username))) {
            return true;
        } else if (br.containsHTML("/users/logout")) {
            return true;
        } else {
            return false;
        }
    }

    private String getUsernameFromCookies(final Browser br) {
        String userinfoCookie = br.getCookie(br.getHost(), "userinfo", Cookies.NOTDELETEDPATTERN);
        if (userinfoCookie != null) {
            userinfoCookie = Encoding.htmlDecode(userinfoCookie);
            return PluginJSonUtils.getJson(userinfoCookie, "username");
        }
        return null;
    }

    public static Browser prepBR(final Browser br) {
        /* Needed to view mature content */
        br.setCookie("deviantart.com", "agegate_state", "1");
        return br;
    }

    @Override
    public String getDescription() {
        return "JDownloader's Deviantart Plugin helps downloading data from deviantart.com.";
    }

    @Override
    public Class<? extends DeviantArtComConfig> getConfigInterface() {
        return DeviantArtComConfig.class;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}