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
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.FacebookConfig;
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
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.decrypter.FaceBookComGallery;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FaceBookComVideos extends PluginForHost {
    private final boolean enforceCookieLogin = true;

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if (enforceCookieLogin) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING, LazyPlugin.FEATURE.IMAGE_HOST, LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
        } else {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING, LazyPlugin.FEATURE.IMAGE_HOST, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
        }
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        FaceBookComGallery.prepBR(br);
        return br;
    }

    private static final String PATTERN_PHOTO                          = "(?i)https?://[^/]+/(?:photo\\.php|photo/)\\?fbid=(\\d+)";
    private static final String PATTERN_PHOTO_PART_OF_ALBUM            = "(?i)https?://[^/]+/[^/]+/photos/a\\.\\d+/(\\d+)";
    /* Allow parameter 'v' to be anywhere in that URL. */
    private static final String PATTERN_VIDEO_WATCH                    = "(?i)https?://[^/]+/watch/(?:live/)?\\?.*v=(\\d+)";
    private static final String PATTERN_VIDEO_WITH_UPLOADER_NAME       = "(?i)https://[^/]+/([^/]+)/videos/(\\d+).*";
    // private static final String TYPE_SINGLE_VIDEO_ALL = "https?://(www\\.)?facebook\\.com/video\\.php\\?v=\\d+";
    public static final String  PROPERTY_DATE_FORMATTED                = "date_formatted";
    public static final String  PROPERTY_TITLE                         = "title";
    /* Real uploader name */
    public static final String  PROPERTY_UPLOADER                      = "uploader";
    /* Uploader name inside URL (slug, shortened variant of uploaders' name.) */
    public static final String  PROPERTY_UPLOADER_URL                  = "uploader_url";
    public static final String  PROPERTY_CONTENT_ID                    = "content_id";
    @Deprecated
    public static final String  PROPERTY_DIRECTURL_OLD                 = "directurl";
    public static final String  PROPERTY_DIRECTURL_LAST                = "directurl_last";
    public static final String  PROPERTY_DIRECTURL_LOW                 = "directurl_low";
    public static final String  PROPERTY_DIRECTURL_HD                  = "directurl_hd";
    private static final String PROPERTY_IS_CHECKABLE_VIA_PLUGIN_EMBED = "is_checkable_via_plugin_embed";
    public static final String  PROPERTY_ACCOUNT_REQUIRED              = "account_required";
    public static final String  PROPERTY_RUNTIME_MILLISECONDS          = "runtime_milliseconds";
    public static final String  PROPERTY_DESCRIPTION                   = "description";
    public static final String  PROPERTY_TYPE                          = "type";
    public static final String  TYPE_VIDEO                             = "video";
    public static final String  TYPE_PHOTO                             = "photo";
    public static final String  TYPE_THUMBNAIL                         = "thumbnail";
    private String              downloadURL                            = null;

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "facebook.com" });
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
            /* Special: No pattern: URLs will be added via crawler */
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    public FaceBookComVideos(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.facebook.com/r.php");
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid + "/" + getType(link);
        } else {
            return super.getLinkID(link);
        }
    }

    public static String getFID(final DownloadLink link) {
        final String storedContentID = link.getStringProperty(PROPERTY_CONTENT_ID);
        if (storedContentID != null) {
            return storedContentID;
        } else {
            return new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
        }
    }

    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    private String getAndCheckDownloadURL(final DownloadLink link, final boolean throwExceptionIfNoDirecturlIsAvailable) throws Exception {
        final String urlLast = link.getStringProperty(PROPERTY_DIRECTURL_LAST);
        final String urlVideoLow = link.getStringProperty(PROPERTY_DIRECTURL_LOW);
        final String urlVideoHD = link.getStringProperty(PROPERTY_DIRECTURL_HD);
        final String urlOld = link.getStringProperty(PROPERTY_DIRECTURL_OLD);
        if (urlLast == null && urlVideoLow == null && urlVideoHD == null && urlOld == null) {
            if (throwExceptionIfNoDirecturlIsAvailable) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                return null;
            }
        }
        String ret = checkDirecturlFromPropertyAndSetFilesize(link, PROPERTY_DIRECTURL_LAST);
        if (ret == null && (PluginJsonConfig.get(this.getConfigInterface()).isPreferHD() || urlVideoLow == null)) {
            ret = checkDirecturlFromPropertyAndSetFilesize(link, PROPERTY_DIRECTURL_HD);
        }
        if (ret == null) {
            ret = checkDirecturlFromPropertyAndSetFilesize(link, PROPERTY_DIRECTURL_LOW);
            if (ret == null) {
                ret = checkDirecturlFromPropertyAndSetFilesize(link, PROPERTY_DIRECTURL_OLD);
            }
        }
        return ret;
    }

    /** Returns directurl without checking it. */
    private static String getDirecturl(final DownloadLink link) {
        String ret = link.getStringProperty(PROPERTY_DIRECTURL_LAST);
        final String urlVideoLow = link.getStringProperty(PROPERTY_DIRECTURL_LOW);
        if (ret == null && (PluginJsonConfig.get(FacebookConfig.class).isPreferHD() || urlVideoLow == null)) {
            ret = link.getStringProperty(PROPERTY_DIRECTURL_HD);
        }
        if (ret == null) {
            ret = urlVideoLow;
            if (ret == null) {
                ret = link.getStringProperty(PROPERTY_DIRECTURL_OLD);
            }
        }
        return ret;
    }

    private int getMaxChunks(final DownloadLink link) {
        if (isVideo(link)) {
            return 0;
        } else {
            return 1;
        }
    }

    public static boolean isVideo(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(PATTERN_PHOTO) || link.getPluginPatternMatcher().matches(PATTERN_PHOTO_PART_OF_ALBUM)) {
            /* Legacy handling */
            return false;
        } else {
            final String type = link.getStringProperty(PROPERTY_TYPE);
            if (type == null) {
                /* Old video URL (legacy handling) */
                return true;
            } else {
                if (StringUtils.equalsIgnoreCase(type, TYPE_VIDEO)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public static boolean isPhoto(final DownloadLink link) {
        final String type = getType(link);
        if (StringUtils.equals(type, TYPE_PHOTO)) {
            return true;
        } else {
            return false;
        }
    }

    public static String getType(final DownloadLink link) {
        final String type = link.getStringProperty(PROPERTY_TYPE);
        if (type != null) {
            return type;
        } else if (isVideo(link)) {
            return TYPE_VIDEO;
        } else {
            /* Legacy handling */
            return TYPE_PHOTO;
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        downloadURL = null;
        if (account != null) {
            this.login(account, false);
        }
        downloadURL = getAndCheckDownloadURL(link, false);
        if (downloadURL != null) {
            /* This is what we want! */
            logger.info("Availablecheck only via directurl done:" + downloadURL);
            return AvailableStatus.TRUE;
        } else {
            // final boolean fastLinkcheck = PluginJsonConfig.get(this.getConfigInterface()).isEnableFastLinkcheck();
            // 2024-04-05: Re-evaluate if this is still needed; Facebook direct-URLs should be static!
            logger.info("Trying to refresh directurl");
            final FaceBookComGallery crawler = (FaceBookComGallery) this.getNewPluginForDecryptInstance(this.getHost());
            final ArrayList<DownloadLink> results = crawler.crawl(new CryptedLink(link.getContainerUrl()), account);
            DownloadLink hit = null;
            for (final DownloadLink result : results) {
                if (StringUtils.equals(result.getLinkID(), link.getLinkID())) {
                    hit = result;
                    break;
                }
            }
            if (hit == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadURL = getAndCheckDownloadURL(hit, true);
            if (downloadURL == null) {
                /* E.g. final downloadurl doesn't lead to video-file. */
                logger.warning("Failed to refresh directurl");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?");
            }
            logger.info("Successfully refreshed directurl | New: " + downloadURL);
            link.setProperties(hit.getProperties());
            setFilename(link);
            return AvailableStatus.TRUE;
        }
    }

    public static String getUploaderURL(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(PATTERN_VIDEO_WITH_UPLOADER_NAME)) {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_VIDEO_WITH_UPLOADER_NAME).getMatch(0);
        } else {
            return link.getStringProperty(PROPERTY_UPLOADER_URL);
        }
    }

    public static String getUploaderNameAny(final DownloadLink link) {
        final String uploader = link.getStringProperty(PROPERTY_UPLOADER);
        final String uploaderURL = getUploaderURL(link);
        if (uploader != null) {
            return uploader;
        } else {
            return uploaderURL;
        }
    }

    /** Sets filename based on previously set DownloadLink properties. */
    public static void setFilename(final DownloadLink link) {
        /* Some filename corrections */
        String filename = "";
        final String title = link.getStringProperty(PROPERTY_TITLE);
        final String dateFormatted = link.getStringProperty(PROPERTY_DATE_FORMATTED);
        if (dateFormatted != null) {
            filename += dateFormatted + "_";
        }
        final String uploaderNameForFilename = getUploaderNameAny(link);
        if (!StringUtils.isEmpty(uploaderNameForFilename)) {
            filename += uploaderNameForFilename + "_";
        }
        if (!StringUtils.isEmpty(title)) {
            filename += title.replaceAll("\\s*\\| Facebook\\s*$", "");
            if (!filename.contains(getFID(link))) {
                filename = filename + "_" + getFID(link);
            }
        } else {
            /* No title given at all -> use fuid only */
            filename += getFID(link);
        }
        String ext = null;
        if (isVideo(link)) {
            ext = ".mp4";
        } else {
            final String directurl = getDirecturl(link);
            if (directurl != null) {
                ext = Plugin.getFileNameExtensionFromURL(directurl);
            }
        }
        if (ext != null) {
            filename += ext;
        }
        link.setFinalFileName(filename);
    }

    private String checkDirecturlFromPropertyAndSetFilesize(final DownloadLink link, final String propertyName) throws IOException, PluginException {
        final String url = link.getStringProperty(propertyName);
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(url);
            if (this.looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                return url;
            } else {
                try {
                    br.followConnection(true);
                } catch (IOException ignore) {
                    logger.log(ignore);
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        link.removeProperty(propertyName);
        return null;
    }

    public static String getUploaderNameFromVideoURL(final String videourl) {
        if (videourl == null) {
            return null;
        } else {
            return new Regex(videourl, "(?i)https?://[^/]+/([^/]+)/videos/.*").getMatch(0);
        }
    }

    private void checkErrors(final DownloadLink link) throws PluginException {
        if (br.getURL().contains("/login.php") || br.getURL().contains("/login/?next=")) {
            /*
             * 2021-03-01: Login required: There are videos which are only available via account but additionally it seems like FB randomly
             * enforces the need of an account for other videos also e.g. by country/IP.
             */
            throw new AccountRequiredException();
        } else if (link.getPluginPatternMatcher().matches(PATTERN_VIDEO_WATCH) && !br.getURL().contains(getFID(link))) {
            /*
             * Specific type of URL will redirect to other URL/mainpage on offline --> Check for that E.g.
             * https://www.facebook.com/watch/?v=2739449049644930
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, true);
        final AccountInfo ai = new AccountInfo();
        ai.setStatus("Valid Facebook account is active");
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "https://www.facebook.com/terms.php";
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (downloadURL == null) {
            if (isAccountRequired(link)) {
                /*
                 * If this happens while an account is active this means that the user is either missing the rights to access that item or
                 * the item is offline.
                 */
                throw new AccountRequiredException();
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, true, this.getMaxChunks(link));
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken file?");
        } else {
            link.setProperty(PROPERTY_DIRECTURL_LAST, downloadURL);
        }
        dl.startDownload();
    }

    private boolean isAccountRequired(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_ACCOUNT_REQUIRED)) {
            return true;
        } else {
            return false;
        }
    }

    public void login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                /* 2020-10-9: Experimental login/test */
                final Cookies userCookies = account.loadUserCookies();
                if (enforceCookieLogin && userCookies == null) {
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                if (userCookies != null) {
                    logger.info("Trying to login via user-cookies");
                    br.setCookies(userCookies);
                    if (!validateCookies) {
                        /* Do not validate cookies */
                        return;
                    }
                    if (verifyCookies(account, userCookies, br)) {
                        /* Save cookies to save new valid cookie timestamp */
                        logger.info("User-cookie login successful");
                        /*
                         * Try to make sure that username in JD is unique because via cookie login, user can enter whatever he wants into
                         * username field! 2020-11-16: Username can be "" (empty) for some users [rare case].
                         */
                        final String username = PluginJSonUtils.getJson(br, "username");
                        if (!StringUtils.isEmpty(username)) {
                            logger.info("Found username in json: " + username);
                            account.setUser(username);
                        } else {
                            logger.info("Failed to find username in json (rarec case)");
                        }
                        return;
                    } else {
                        logger.info("User-Cookie login failed");
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    if (!validateCookies) {
                        /* Do not validate cookies */
                        return;
                    }
                    if (verifyCookies(account, cookies, br)) {
                        /* Save cookies to save new valid cookie timestamp */
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        /* Get rid of old cookies / headers */
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Full login required");
                br.setFollowRedirects(true);
                final boolean prefer_mobile_login = true;
                // better use the website login. else the error handling below might be broken.
                if (prefer_mobile_login) {
                    /* Mobile login = no crypto crap */
                    br.getPage("https://m.facebook.com/");
                    final Form loginForm = br.getForm(0);
                    if (loginForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginForm.remove(null);
                    loginForm.put("email", Encoding.urlEncode(account.getUser()));
                    loginForm.put("pass", Encoding.urlEncode(account.getPass()));
                    br.submitForm(loginForm);
                    br.getPage("https://www.facebook.com/");
                } else {
                    br.getPage("https://www.facebook.com/login.php");
                    final String lang = System.getProperty("user.language");
                    final Form loginForm = br.getForm(0);
                    if (loginForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginForm.remove("persistent");
                    loginForm.put("persistent", "1");
                    loginForm.remove(null);
                    loginForm.remove("login");
                    loginForm.remove("trynum");
                    loginForm.remove("profile_selector_ids");
                    loginForm.remove("legacy_return");
                    loginForm.remove("enable_profile_selector");
                    loginForm.remove("display");
                    String _js_datr = br.getRegex("\"_js_datr\"\\s*,\\s*\"([^\"]+)").getMatch(0);
                    br.setCookie("https://facebook.com", "_js_datr", _js_datr);
                    br.setCookie("https://facebook.com", "_js_reg_fb_ref", Encoding.urlEncode("https://www.facebook.com/login.php"));
                    br.setCookie("https://facebook.com", "_js_reg_fb_gate", Encoding.urlEncode("https://www.facebook.com/login.php"));
                    loginForm.put("email", Encoding.urlEncode(account.getUser()));
                    loginForm.put("pass", Encoding.urlEncode(account.getPass()));
                    br.submitForm(loginForm);
                }
                /**
                 * Facebook thinks we're an unknown device, now we prove we're not ;)
                 */
                if (br.containsHTML(">Your account is temporarily locked")) {
                    final String nh = br.getRegex("name=\"nh\" value=\"([a-z0-9]+)\"").getMatch(0);
                    final String dstc = br.getRegex("name=\"fb_dtsg\" value=\"([^<>\"]*?)\"").getMatch(0);
                    if (nh == null || dstc == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&submit%5BContinue%5D=Continue");
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "facebook.com", "http://facebook.com", true);
                    String achal = br.getRegex("name=\"achal\" value=\"([a-z0-9]+)\"").getMatch(0);
                    final String captchaPersistData = br.getRegex("name=\"captcha_persist_data\" value=\"([^<>\"]*?)\"").getMatch(0);
                    if (captchaPersistData == null || achal == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    // Normal captcha handling
                    for (int i = 1; i <= 3; i++) {
                        String captchaLink = br.getRegex("\"(https?://(www\\.)?facebook\\.com/captcha/tfbimage\\.php\\?captcha_challenge_code=[^<>\"]*?)\"").getMatch(0);
                        if (captchaLink == null) {
                            break;
                        }
                        captchaLink = Encoding.htmlDecode(captchaLink);
                        String code;
                        try {
                            code = getCaptchaCode(captchaLink, dummyLink);
                        } catch (final Exception e) {
                            continue;
                        }
                        br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&captcha_response=" + Encoding.urlEncode(code) + "&achal=" + achal + "&submit%5BSubmit%5D=Submit");
                    }
                    // reCaptcha handling
                    for (int i = 1; i <= 3; i++) {
                        final String rcID = br.getRegex("\"recaptchaPublicKey\":\"([^<>\"]*?)\"").getMatch(0);
                        if (rcID == null) {
                            break;
                        }
                        final String extraChallengeParams = br.getRegex("name=\"extra_challenge_params\" value=\"([^<>\"]*?)\"").getMatch(0);
                        final String captchaSession = br.getRegex("name=\"captcha_session\" value=\"([^<>\"]*?)\"").getMatch(0);
                        if (extraChallengeParams == null || captchaSession == null) {
                            break;
                        }
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.setId(rcID);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c;
                        try {
                            c = getCaptchaCode("recaptcha", cf, dummyLink);
                        } catch (final Exception e) {
                            continue;
                        }
                        br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&captcha_session=" + Encoding.urlEncode(captchaSession) + "&extra_challenge_params=" + Encoding.urlEncode(extraChallengeParams) + "&recaptcha_type=password&recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&captcha_response=" + Encoding.urlEncode(c) + "&achal=1&submit%5BSubmit%5D=Submit");
                    }
                    for (int i = 1; i <= 3; i++) {
                        if (br.containsHTML(">To confirm your identity, please enter your birthday")) {
                            achal = br.getRegex("name=\"achal\" value=\"([a-z0-9]+)\"").getMatch(0);
                            if (achal == null) {
                                break;
                            }
                            String birthdayVerificationAnswer;
                            try {
                                birthdayVerificationAnswer = getUserInput("Enter your birthday (dd:MM:yyyy)", dummyLink);
                            } catch (final Exception e) {
                                continue;
                            }
                            final String[] bdSplit = birthdayVerificationAnswer.split(":");
                            if (bdSplit == null || bdSplit.length != 3) {
                                continue;
                            }
                            int bdDay = 0, bdMonth = 0, bdYear = 0;
                            try {
                                bdDay = Integer.parseInt(bdSplit[0]);
                                bdMonth = Integer.parseInt(bdSplit[1]);
                                bdYear = Integer.parseInt(bdSplit[2]);
                            } catch (final Exception e) {
                                continue;
                            }
                            br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&birthday_captcha_month=" + bdMonth + "&birthday_captcha_day=" + bdDay + "&birthday_captcha_year=" + bdYear + "&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&achal=" + achal + "&submit%5BSubmit%5D=Submit");
                        } else {
                            break;
                        }
                    }
                    if (br.containsHTML("/captcha/friend_name_image\\.php\\?")) {
                        // unsupported captcha challange.
                        logger.warning("Unsupported captcha challenge.");
                    }
                } else if (br.containsHTML("/checkpoint/")) {
                    br.getPage("https://www.facebook.com/checkpoint/");
                    final String postFormID = br.getRegex("name=\"post_form_id\" value=\"(.*?)\"").getMatch(0);
                    final String nh = br.getRegex("name=\"nh\" value=\"(.*?)\"").getMatch(0);
                    if (nh == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&submit%5BContinue%5D=Weiter&nh=" + nh);
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&submit%5BThis+is+Okay%5D=Das+ist+OK&nh=" + nh);
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&machine_name=&submit%5BDon%27t+Save%5D=Nicht+speichern&nh=" + nh);
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&machine_name=&submit%5BDon%27t+Save%5D=Nicht+speichern&nh=" + nh);
                } else if (br.getURL().contains("/login/save-device")) {
                    /* 2020-10-29: Challenge kinda like "Trust this device" */
                    final Form continueForm = br.getFormbyActionRegex(".*/login/device-based/.*");
                    if (continueForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.submitForm(continueForm);
                    br.getPage("https://" + this.getHost() + "/");
                    br.followRedirect();
                }
                if (!isLoggedinHTML(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* Save cookies */
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (PluginException e) {
                if (e.getLinkStatus() == PluginException.VALUE_ID_PREMIUM_DISABLE) {
                    account.removeProperty("");
                }
                throw e;
            }
        }
    }

    protected boolean verifyCookies(final Account account, final Cookies cookies, final Browser br) throws Exception {
        br.setCookies(this.getHost(), cookies);
        final boolean follow = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            br.getPage("https://" + this.getHost() + "/");
        } finally {
            br.setFollowRedirects(follow);
        }
        if (this.isLoggedinHTML(br)) {
            logger.info("Successfully logged in via cookies");
            return true;
        } else {
            logger.info("Cookie login failed");
            br.clearCookies(br.getHost());
            return false;
        }
    }

    private boolean isLoggedinHTML(final Browser br) {
        final boolean brContainsSecondaryLoggedinHint = br.containsHTML("settings_dropdown_profile_picture");
        final String logout_hash = PluginJSonUtils.getJson(br, "logout_hash");
        logger.info("logout_hash = " + logout_hash);
        logger.info("brContainsSecondaryLoggedinHint = " + brContainsSecondaryLoggedinHint);
        return !StringUtils.isEmpty(logout_hash) && brContainsSecondaryLoggedinHint;
    }

    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        /* No not allow multihost plugins to handle Facebook URLs! */
        return link.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public Class<? extends FacebookConfig> getConfigInterface() {
        return FacebookConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            link.removeProperty(PROPERTY_DIRECTURL_LAST);
            link.removeProperty(PROPERTY_DIRECTURL_OLD);
            link.removeProperty(PROPERTY_DIRECTURL_HD);
            link.removeProperty(PROPERTY_DIRECTURL_LOW);
            link.removeProperty(PROPERTY_IS_CHECKABLE_VIA_PLUGIN_EMBED);
            link.removeProperty(PROPERTY_ACCOUNT_REQUIRED);
            link.removeProperty(PROPERTY_TITLE);
            link.removeProperty(PROPERTY_UPLOADER);
            link.removeProperty(PROPERTY_UPLOADER_URL);
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* Only login captcha sometimes */
        return false;
    }
}