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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.DeviantArtComConfig;
import org.jdownloader.plugins.components.config.DeviantArtComConfig.ImageDownloadMode;
import org.jdownloader.plugins.config.PluginJsonConfig;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/([\\w\\-]+/art/[\\w\\-]+-\\d+|status(?:-update)?/\\d+)" })
public class DeviantArtCom extends PluginForHost {
    // private static final String DLLINK_REFRESH_NEEDED = "https?://(www\\.)?deviantart\\.com/download/.+";
    private final String       TYPE_DOWNLOADALLOWED_HTML             = "(?i)class=\"text\">HTML download</span>";
    private final String       TYPE_DOWNLOADFORBIDDEN_HTML           = "<div class=\"grf\\-indent\"";
    private boolean            downloadHTML                          = false;
    private final String       PATTERN_ART                           = "https?://[^/]+/([\\w\\-]+)/art/([\\w\\-]+)-(\\d+)";
    private final String       PATTERN_JOURNAL                       = "https?://[^/]+/([\\w\\-]+)/journal/([\\w\\-]+)-(\\d+)";
    private final String       LINKTYPE_STATUS                       = "https?://[^/]+/([\\w\\-]+)/status(?:-update)?/(\\d+)";
    public static final String PROPERTY_TYPE                         = "type";
    private final String       PROPERTY_OFFICIAL_DOWNLOADURL         = "official_downloadurl";
    private final String       PROPERTY_IMAGE_DISPLAY_OR_PREVIEW_URL = "image_display_or_preview_url";

    /**
     * @author raztoki
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

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        prepBR(this.br);
        br.setFollowRedirects(true);
        if (account != null) {
            login(account, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("/error\\-title\\-oops\\.png\\)") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = getFID(link);
        String title = null;
        String displayedImageURL = null;
        String officialDownloadurl = null;
        String json = br.getRegex("window\\.__INITIAL_STATE__ = JSON\\.parse\\(\"(.*?)\"\\);").getMatch(0);
        Number officialDownloadsizeBytes = null;
        if (json != null) {
            /* TODO: Make much more use of this json */
            json = PluginJSonUtils.unescape(json);
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            final Map<String, Object> entities = (Map<String, Object>) entries.get("@@entities");
            final Map<String, Object> deviationExtended = (Map<String, Object>) entities.get("deviationExtended");
            final Map<String, Object> deviationExtendedThisArt = deviationExtended == null ? null : (Map<String, Object>) deviationExtended.get(fid);
            final Map<String, Object> user = (Map<String, Object>) entities.get("user");
            final Map<String, Object> deviation = (Map<String, Object>) entities.get("deviation");
            final Map<String, Object> thisArt = (Map<String, Object>) deviation.get(fid);
            final Map<String, Object> media = (Map<String, Object>) thisArt.get("media");
            if (media != null) {
                displayedImageURL = (String) media.get("baseUri");
            }
            final Map<String, Object> thisUser = (Map<String, Object>) user.get(thisArt.get("author").toString());
            title = thisArt.get("title").toString() + " by " + thisUser.get("username"); // prefer title from json
            link.setProperty(PROPERTY_TYPE, thisArt.get("type"));
            if (deviationExtendedThisArt != null) {
                final Map<String, Object> download = (Map<String, Object>) deviationExtendedThisArt.get("download");
                if (download != null) {
                    officialDownloadurl = download.get("url").toString();
                    officialDownloadsizeBytes = (Number) download.get("filesize");
                }
            }
        }
        /* Fallbacks via website-html */
        if (title == null) {
            title = HTMLSearch.searchMetaTag(br, "og:title");
        }
        if (title != null) {
            title = title.replaceAll("(?i) on deviantart$", "");
        }
        if (StringUtils.isEmpty(displayedImageURL)) {
            displayedImageURL = HTMLSearch.searchMetaTag(br, "og:image");
        }
        /* TODO: Test this with video- and animation content */
        String ext = null;
        boolean setOfficialDownloadFilesize = false;
        final String officialDownloadFilesizeStr = br.getRegex("(?i)>\\s*Image size\\s*</div><div [^>]*>\\d+x\\d+px\\s*(\\d+[^>]+)</div>").getMatch(0);
        // final boolean accountNeededForOfficialDownload = br.containsHTML("(?i)Log in to download");
        if (StringUtils.isEmpty(officialDownloadurl)) {
            officialDownloadurl = br.getRegex("data-hook=\"download_button\"[^>]*href=\"(https?://[^\"]+)").getMatch(0);
            if (officialDownloadurl != null) {
                officialDownloadurl = officialDownloadurl.replace("&amp;", "&");
            }
        }
        link.setProperty(PROPERTY_OFFICIAL_DOWNLOADURL, officialDownloadurl);
        final boolean isImage = StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_TYPE), "image");
        if (displayedImageURL != null && isImage) {
            link.setProperty(PROPERTY_IMAGE_DISPLAY_OR_PREVIEW_URL, displayedImageURL);
        }
        final DeviantArtComConfig cfg = PluginJsonConfig.get(DeviantArtComConfig.class);
        final ImageDownloadMode mode = cfg.getImageDownloadMode();
        /* Check if either user wants to download the html code or if we have a linktype which needs this. */
        if (mode == ImageDownloadMode.HTML || link.getPluginPatternMatcher().matches(PATTERN_JOURNAL) || link.getPluginPatternMatcher().matches(LINKTYPE_STATUS) || StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_TYPE), "literature")) {
            downloadHTML = true;
            ext = ".html";
        } else if (isImage) {
            /* Correct file-extension will be detected later */
            ext = null;
        } else if (br.containsHTML(TYPE_DOWNLOADALLOWED_HTML) || br.containsHTML(TYPE_DOWNLOADFORBIDDEN_HTML)) {
            downloadHTML = true;
            ext = ".html";
        } else if (looksLikeAccountRequiredUploaderDecision(br)) {
            /* Account needed to view/download */
            ext = ".html";
        } else {
            /* Undefined case */
            ext = ".html";
        }
        String dllink = null;
        try {
            dllink = this.getDirecturl(link, account);
        } catch (final PluginException e) {
        }
        link.setVerifiedFileSize(-1); // reset this every time as user can change settings
        if (downloadHTML) {
            try {
                link.setDownloadSize(br.getRequest().getHtmlCode().getBytes("UTF-8").length);
            } catch (final UnsupportedEncodingException ignore) {
                ignore.printStackTrace();
            }
        } else if ((officialDownloadFilesizeStr != null || officialDownloadsizeBytes != null) && (setOfficialDownloadFilesize || mode == ImageDownloadMode.OFFICIAL_DOWNLOAD_ONLY)) {
            /*
             * Set filesize of official download if: User wants official download and it is available and/or if user wants official
             * downloads only (set filesize even if official downloadurl was not found).
             */
            if (officialDownloadsizeBytes != null) {
                /* 2022-11-22: Do not set verifiedFilesize for now. This is still under development. */
                // link.setVerifiedFileSize(officialDownloadsizeBytes.longValue());
                link.setDownloadSize(officialDownloadsizeBytes.longValue());
            } else {
                link.setDownloadSize(SizeFormatter.getSize(officialDownloadFilesizeStr.replace(",", "")));
            }
        } else if (mode == ImageDownloadMode.OFFICIAL_DOWNLOAD_ELSE_PREVIEW && cfg.isFastLinkcheckForSingleItems() == false && !isDownload && !StringUtils.isEmpty(dllink)) {
            final Browser br2 = br.cloneBrowser();
            /* Workaround for old downloadcore bug that can lead to incomplete files */
            br2.getHeaders().put("Accept-Encoding", "identity");
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (!looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String mimeTypeExt = getExtensionFromMimeType(con.getRequest().getResponseHeader("Content-Type"));
                if (mimeTypeExt != null) {
                    ext = "." + mimeTypeExt;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        if (ext == null && dllink != null) {
            ext = Plugin.getFileNameExtensionFromURL(dllink);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            title = this.correctOrApplyFileNameExtension(title, ext);
            link.setFinalFileName(title);
        } else if (!StringUtils.isEmpty(dllink)) {
            /* Last resort fallback */
            final String filenameFromURL = Plugin.getFileNameFromURL(new URL(dllink));
            if (filenameFromURL != null) {
                link.setName(filenameFromURL);
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean isAccountRequiredForOfficialDownload(final Browser br) {
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

    private String getDirecturl(final DownloadLink link, final Account account) throws PluginException {
        final boolean isImage = StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_TYPE), "image");
        final String displayedImageURL = link.getStringProperty(PROPERTY_IMAGE_DISPLAY_OR_PREVIEW_URL);
        /* officialDownloadurl can be given while account is not given -> Will lead to error 404 then! */
        final String officialDownloadurl = link.getStringProperty(PROPERTY_OFFICIAL_DOWNLOADURL);
        link.setVerifiedFileSize(-1);
        String dllink = null;
        if (downloadHTML) {
            dllink = br.getURL();
        } else if (isImage) {
            final DeviantArtComConfig cfg = PluginJsonConfig.get(DeviantArtComConfig.class);
            final ImageDownloadMode mode = cfg.getImageDownloadMode();
            if (mode == ImageDownloadMode.OFFICIAL_DOWNLOAD_ONLY) {
                /* User only wants to download items with official download option available but it is not available in this case. */
                if (!this.isAccountRequiredForOfficialDownload(br)) {
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
                dllink = displayedImageURL;
            }
        }
        return dllink;
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account, true);
        final boolean isImage = StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_TYPE), "image");
        final String displayedImageURL = link.getStringProperty(PROPERTY_IMAGE_DISPLAY_OR_PREVIEW_URL);
        /* officialDownloadurl can be given while account is not given -> Will lead to error 404 then! */
        final String officialDownloadurl = link.getStringProperty(PROPERTY_OFFICIAL_DOWNLOADURL);
        link.setVerifiedFileSize(-1);
        String dllink = null;
        boolean resume = true;
        if (downloadHTML) {
            link.setVerifiedFileSize(-1);
            resume = false;
            dllink = br.getURL();
        } else if (isImage) {
            final DeviantArtComConfig cfg = PluginJsonConfig.get(DeviantArtComConfig.class);
            final ImageDownloadMode mode = cfg.getImageDownloadMode();
            if (mode == ImageDownloadMode.OFFICIAL_DOWNLOAD_ONLY) {
                /* User only wants to download items with official download option available but it is not available in this case. */
                if (!this.isAccountRequiredForOfficialDownload(br)) {
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
                dllink = displayedImageURL;
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            if (this.looksLikeAccountRequired(br)) {
                throw new AccountRequiredException();
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /* Workaround for old downloadcore bug that can lead to incomplete files */
        /* Disable chunks as we only download pictures or small files */
        br.getHeaders().put("Accept-Encoding", "identity");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            handleServerErrors(dl.getConnection());
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    private void handleServerErrors(final URLConnectionAdapter con) throws PluginException {
        /* Happens sometimes - download should work fine later though. */
        if (con.getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 10 * 60 * 1000l);
        } else if (con.getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 1 * 60 * 1000l);
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
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
                /* 2022-10-31: Normal login process won't work due to their anti DDoS protection -> Only cookie login is possible */
                final boolean allowCookieLoginOnly = true;
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