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
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.decrypter.MotherLessComCrawler;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.MotherlessComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { MotherLessComCrawler.class })
public class MotherLessCom extends PluginForHost {
    public static final String  text_subscribeFailed        = "Failed to subscribe to the owner of the video";
    public static final String  html_contentSubscriberVideo = "(?i)Here's another video instead\\.";
    public static final String  html_contentSubscriberImage = "(?i)Here's another image instead\\.";
    private final static String PROPERTY_DIRECTURL          = "directurl";
    public static final String  PROPERTY_TYPE               = "dltype";
    public static String        PROPERTY_TITLE              = "title";

    @SuppressWarnings("deprecation")
    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2500l);
        this.enablePremium("https://motherless.com/register");
    }

    @Override
    public String getAGBLink() {
        return "https://motherless.com/tou";
    }

    public static List<String[]> getPluginDomains() {
        return MotherLessComCrawler.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Fa-f0-9]+)$");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
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

    private String getFID(final DownloadLink link) {
        return getFID(link.getPluginPatternMatcher());
    }

    private static String getFID(final String url) {
        return new Regex(url, "https?://[^/]+/(.+)").getMatch(0);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        setWeakFilename(link);
        if (this.checkDirectLinkAndSetFilesize(link) != null) {
            logger.info("Linkcheck done via directurl");
            return AvailableStatus.TRUE;
        }
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final AvailableStatus status = parseFileInfoAndSetFilename(link);
        if (status == AvailableStatus.FALSE) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (dllink != null && !isDownload) {
            this.checkDirectLinkAndSetFilesize(link);
        }
        return AvailableStatus.TRUE;
    }

    public void setWeakFilename(final DownloadLink link) {
        if (!link.isNameSet()) {
            /* Set fallback filename */
            final String ext = getAssumedFileExtension(link);
            if (ext != null) {
                link.setName(this.getFID(link) + ext);
            } else {
                link.setName(this.getFID(link));
            }
        }
    }

    public static String getAssumedFileExtension(final DownloadLink link) {
        if (isVideo(link)) {
            return ".mp4";
        } else if (isImage(link)) {
            return ".jpg";
        } else {
            return null;
        }
    }

    public AvailableStatus parseFileInfoAndSetFilename(final DownloadLink link) {
        setWeakFilename(link);
        String dllink = null;
        if (isVideo(this.br)) {
            link.setProperty(PROPERTY_TYPE, "video");
        } else if (isImage(br)) {
            link.setProperty(PROPERTY_TYPE, "image");
        }
        if (isVideo(link)) {
            dllink = getVideoLink(br);
        } else if (isImage(link)) {
            dllink = getPictureLink(br);
            if (dllink == null) {
                dllink = br.getRegex("fileurl\\s*=\\s*\'(http://.*?)\'").getMatch(0);
            }
            // No link there but link to the full picture -> Offline
            if (dllink == null && br.containsHTML("<div id=\"media-media\">\\s*<div>\\s*<a href=\"/[A-Z0-9]+\\?full\"")) {
                return AvailableStatus.FALSE;
            }
        } else {
            /* Old/deprecated/directurls */
            dllink = link.getPluginPatternMatcher();
        }
        link.setProperty(PROPERTY_DIRECTURL, dllink);
        final String title = br.getRegex("class=\"media-meta-title\">\\s*<h1>([^<]+)</h1>").getMatch(0);
        if (title != null) {
            link.setProperty(PROPERTY_TITLE, Encoding.htmlDecode(title).trim());
        }
        setFilename(link);
        return AvailableStatus.TRUE;
    }

    /** Returns true if the content is offline. */
    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.containsHTML("class=\"error-not-found\"")) {
            return true;
        } else if (br.containsHTML("(?i)Violated Site Terms of Use|The page you're looking for cannot be found|You will be redirected to")) {
            return true;
        } else if (br.containsHTML("<img src=\"/images/icons.*/exclamation\\.png\" style=\"margin-top: -5px;\" />[\t\n\r ]+404")) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*The member has deleted the upload\\s*<")) {
            return true;
        } else {
            return false;
        }
    }

    public static void setFilename(final DownloadLink link) {
        final String fid = getFID(link.getPluginPatternMatcher());
        String title = link.getStringProperty(PROPERTY_TITLE);
        String ext = Plugin.getFileNameExtensionFromString(link.getStringProperty(PROPERTY_DIRECTURL));
        if (ext == null) {
            ext = getAssumedFileExtension(link);
        }
        if (title != null && !title.equalsIgnoreCase(fid)) {
            title = Encoding.htmlDecode(title).trim();
            /* Special handling for uploads with full/original filenames, already ending with '.mp4' */
            if (title.toLowerCase(Locale.ENGLISH).endsWith(ext) && PluginJsonConfig.get(MotherlessComConfig.class).isUseTitleAsFilenameIfExtensionFits()) {
                link.setFinalFileName(title);
            } else {
                link.setFinalFileName(title + "_ " + fid + ext);
            }
        } else {
            link.setFinalFileName(fid + ext);
        }
    }

    /** Returns true if we got a single image/video according to HTML code. */
    @Deprecated
    public static final boolean isSingleMedia(final Browser br) {
        final String mediaType = regexMediaType(br);
        if (mediaType != null) {
            return true;
        } else if (isVideo(br)) {
            return true;
        } else if (isImage(br)) {
            return true;
        } else if (isDownloadAccountOnly(br)) {
            return true;
        } else if (isViewSubscriberOnly(br)) {
            return true;
        } else if (isViewFriendsOnly(br)) {
            return true;
        } else {
            return false;
        }
    }

    public static final boolean isVideo(final Browser br) {
        final String mediaType = regexMediaType(br);
        if (StringUtils.equalsIgnoreCase(mediaType, "video")) {
            return true;
        } else if (isVideoNotOnlineYet(br)) {
            return true;
        } else if (br.containsHTML(MotherLessCom.html_contentSubscriberVideo)) {
            return true;
        } else {
            return false;
        }
    }

    public static final boolean isImage(final Browser br) {
        final String mediaType = regexMediaType(br);
        if (StringUtils.equalsIgnoreCase(mediaType, "image")) {
            return true;
        } else if (br.containsHTML(MotherLessCom.html_contentSubscriberImage)) {
            return true;
        } else {
            return false;
        }
    }

    public static String regexMediaType(final Browser br) {
        return br.getRegex("mediatype\\s*=\\s*'([A-Za-z0-9]+)").getMatch(0);
    }

    public static boolean isVideoNotOnlineYet(final Browser br) {
        if (br.containsHTML("(?i)(This video is being processed and will be available shortly|This video will be available in (less than a minute|\\d+ minutes?))")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isDownloadAccountOnly(final Browser br) {
        if (br.containsHTML("(?i)This link is only downloadable for registered users\\.")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isViewSubscriberOnly(final Browser br) {
        if (br.containsHTML("(?i)The upload is subscriber only\\. You can subscribe to the member from their")) {
            return true;
        } else if (br.containsHTML(html_contentSubscriberVideo)) {
            return true;
        } else if (br.containsHTML(html_contentSubscriberImage)) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isViewFriendsOnly(final Browser br) {
        if (br.containsHTML("(?i)>\\s*The content you are trying to view is for friends only")) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isVideo(final DownloadLink link) {
        if ("video".equals(link.getStringProperty(PROPERTY_TYPE))) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isImage(final DownloadLink link) {
        if ("image".equals(link.getStringProperty(PROPERTY_TYPE))) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (link != null) {
            if (isVideo(link)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final Account account) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            prepHeadersDownload(link, br);
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, url, isResumeable(link, null), this.getMaxChunks());
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                br.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    private String checkDirectLinkAndSetFilesize(final DownloadLink link) {
        String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                br2.getHeaders().put("Accept-Encoding", "identity");
                prepHeadersDownload(link, br2);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private void prepHeadersDownload(final DownloadLink link, final Browser brc) {
        br.getHeaders().put("Accept-Encoding", "identity");
        if (isVideo(link)) {
            brc.getHeaders().put("Referer", "http://motherless.com/scripts/jwplayer.flash.swf");
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, true);
        account.setType(AccountType.FREE);
        return new AccountInfo();
    }

    private void login(final Account account, final boolean validateCookies) throws Exception {
        br.setFollowRedirects(true);
        synchronized (account) {
            final Cookies cookies = account.loadCookies("xxy<");
            if (cookies != null) {
                br.setCookies(cookies);
                if (!validateCookies) {
                    logger.info("Trust cookies without checking");
                    return;
                } else {
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedIN(br)) {
                        logger.info("Successfully loggedin via cookies");
                        /* Update cookies */
                        account.saveCookies(this.br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
            }
            logger.info("Performing full login");
            br.getPage("https://" + this.getHost() + "/login");
            final Form loginform = br.getFormbyProperty("id", "motherless-login-modal-form");
            if (loginform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.put("username", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            if (loginform.hasInputFieldByName("botcheck")) {
                loginform.put("botcheck", "no+bots%21");
            }
            loginform.put("remember_me", "1");
            loginform.put("__remember_me", "0");
            br.submitForm(loginform);
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            if (!entries.get("status").toString().equalsIgnoreCase("ok")) {
                /* E.g. {"status":"bad","type":"warning","message":"Incorrect username or password"} */
                account.clearCookies("");
                throw new AccountInvalidException();
            }
            /* Double-check */
            br.getPage("/");
            if (!isLoggedIN(this.br)) {
                account.clearCookies("");
                throw new AccountInvalidException();
            }
            account.saveCookies(this.br.getCookies(br.getHost()), "");
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("(/logout|link-logout)") && br.getCookie(br.getHost(), "_auth", Cookies.NOTDELETEDPATTERN) != null) {
            return true;
        } else {
            return false;
        }
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private String getPictureLink(final Browser br) {
        String directlink = br.getRegex("\"(https?://members\\.motherless\\.com/img/.*?)\"").getMatch(0);
        if (StringUtils.isEmpty(directlink)) {
            directlink = br.getRegex("full_sized\\.jpg\" (.*?)\"(https?://s\\d+\\.motherless\\.com/dev\\d+/\\d+/\\d+/\\d+/\\d+.*?)\"").getMatch(1);
            if (StringUtils.isEmpty(directlink)) {
                directlink = br.getRegex("<div style=\"clear: left;\"></div>[\t\r\n ]+<img src=\"(https?://.*?)\"").getMatch(0);
                if (StringUtils.isEmpty(directlink)) {
                    directlink = br.getRegex("\\?full\">\\s*<img src=\"(?!https?://motherless\\.com/images/full_sized\\.jpg)(http://.*?)\"").getMatch(0);
                    if (StringUtils.isEmpty(directlink)) {
                        directlink = br.getRegex("\"(https?://[\\w\\-\\.]*images\\.motherlessmedia\\.com/images/[a-zA-Z0-9]+\\..{3,4}(?:\\?fs=opencloud)?)\"").getMatch(0);
                        if (StringUtils.isEmpty(directlink)) {
                            directlink = br.getRegex("\"(https?://s\\d+\\.motherlessmedia\\.com/dev[0-9/]+\\..{3,4})\"").getMatch(0);
                        }
                    }
                }
            }
        }
        return directlink;
    }

    private String getVideoLink(final Browser br) {
        String directlink = br.getRegex("addVariable\\(\\'file\\', \\'(https?://.*?\\.(flv|mp4))\\'\\)").getMatch(0);
        if (StringUtils.isEmpty(directlink)) {
            directlink = br.getRegex("(https?://[^/]+\\.motherlessmedia\\.com/[^<>\"]*?-720p\\.(flv|mp4))\"").getMatch(0);
            if (StringUtils.isEmpty(directlink)) {
                directlink = br.getRegex("(https?://[^/]+\\.motherlessmedia\\.com/[^<>\"]*?\\.(flv|mp4))\"").getMatch(0);
            }
        }
        if (StringUtils.isEmpty(directlink)) {
            directlink = PluginJSonUtils.getJsonValue(br, "file");
        }
        if (StringUtils.isEmpty(directlink)) {
            directlink = br.getRegex("__fileurl\\s*=\\s*'(https?://[^<>\"\\']+)").getMatch(0);
        }
        if (directlink != null && !directlink.contains("?start=0")) {
            // dllink += "?start=0";
        }
        return directlink;
    }

    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link);
        if (br.containsHTML("(?i)Subscribers Only")) {
            logger.info("Content is subscriber only --> Auto-subscribing to content-owner");
            String profileToSubscribe = br.getRegex("You can subscribe to the member from\\s*their <a href=(\"|')(https?://motherless\\.com)?/m/(.*?)\\1").getMatch(2);
            if (profileToSubscribe == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, text_subscribeFailed);
            }
            br.getPage("/subscribe/" + profileToSubscribe);
            final String token = br.getRegex("name=\"_token\" value=\"(.*?)\"").getMatch(0);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, text_subscribeFailed);
            }
            br.postPage("/subscribe/" + profileToSubscribe, "_token=" + token);
            if (!br.containsHTML("(?i)(>\\s*You are already subscribed to|>\\s*You are now subscribed to)")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, text_subscribeFailed);
            }
            br.getPage(link.getPluginPatternMatcher());
        }
        handleDownload(link, account);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (!attemptStoredDownloadurlDownload(link, account)) {
            requestFileInformation(link, true);
            /* Errorhandling */
            if (isVideoNotOnlineYet(br)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This video is being processed and will be available shortly");
            } else if (isDownloadAccountOnly(br)) {
                throw new AccountRequiredException("Only downloadable for registered users");
            } else if (isViewSubscriberOnly(br)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Content is for subscribers only!");
            } else if (isViewFriendsOnly(br)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Content is for friends only!");
            }
            final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            prepHeadersDownload(link, br);
            if (!attemptStoredDownloadurlDownload(link, account)) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken file(?)");
            }
            if (link.getFinalFileName() == null) {
                final String plugin_filename = link.getName();
                String server_filename = getFileNameFromHeader(dl.getConnection());
                if (server_filename != null) {
                    server_filename = Encoding.htmlDecode(server_filename).trim();
                }
                if (server_filename != null && plugin_filename.length() > server_filename.length()) {
                    /* Actually it should always use the plugin_filename. */
                    link.setFinalFileName(plugin_filename);
                } else {
                    link.setFinalFileName(server_filename);
                }
            }
        }
        dl.startDownload();
    }

    private int getMaxChunks() {
        return 1;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public Class<? extends MotherlessComConfig> getConfigInterface() {
        return MotherlessComConfig.class;
    }
}