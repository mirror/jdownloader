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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/(?:[^/]+/)?art/[\\w\\-]+|https?://[\\w\\.\\-]*?\\.deviantart\\.com/status/\\d+|https?://[\\w\\.\\-]*?deviantartdecrypted\\.com/(?:[^/]+/)?journal/[\\w\\-]+" })
public class DeviantArtCom extends PluginForHost {
    private String             dllink                           = null;
    private final String       NICE_HOST                        = "deviantart.com";
    private final String       NICE_HOSTproperty                = "deviantartcom";
    private final String       INVALIDLINKS                     = "https?://(www\\.)?forum\\.deviantart\\.com/(?:[^/]+/)?art/general";
    public static final String FASTLINKCHECK_2                  = "FASTLINKCHECK_2";
    public static final String FORCEHTMLDOWNLOAD                = "FORCEHTMLDOWNLOAD";
    public static final String CRAWL_GIVEN_OFFSETS_INDIVIDUALLY = "CRAWL_GIVEN_OFFSETS_INDIVIDUALLY";
    private final String       GENERALFILENAMEREGEX             = "<title[^<>]*>([^<>\"]*?) on deviantART</title>";
    // private static final String DLLINK_REFRESH_NEEDED = "https?://(www\\.)?deviantart\\.com/download/.+";
    private final String       TYPE_DOWNLOADALLOWED_HTML        = "(?i)class=\"text\">HTML download</span>";
    private final String       TYPE_DOWNLOADFORBIDDEN_HTML      = "<div class=\"grf\\-indent\"";
    private final String       TYPE_DOWNLOADFORBIDDEN_SWF       = "class=\"flashtime\"";
    private boolean            downloadHTML                     = false;
    private final String       PATTERN_ART                      = "https?://[^/]+/(?:[^/]+/)?art/[^<>\"/]+";
    private final String       LINKTYPE_JOURNAL                 = "https?://[^/]+/(?:[^/]+/)?journal/[\\w\\-]+";
    private final String       LINKTYPE_STATUS                  = "https?://[^/]+/status/\\d+";
    private final String       TYPE_BLOG_OFFLINE                = "https?://[^/]+/(?:[^/]+/)?blog/.+";

    /**
     * @author raztoki
     */
    @SuppressWarnings("deprecation")
    public DeviantArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
        this.enablePremium("https://www." + this.getHost() + "/join/");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + this.getHost() + "/about/policy/service/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getPluginPatternMatcher().replace("deviantartdecrypted.com/", "deviantart.com/"));
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (link.getPluginPatternMatcher().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        prepBR(this.br);
        // Workaround for a strange bug - DLLINK is not null at the beginning so if multiple links are to be checked they will all get the
        // same filenames
        dllink = null;
        br.setFollowRedirects(true);
        if (this.getPluginConfig().getBooleanProperty(FASTLINKCHECK_ALL, default_FASTLINKCHECK_ALL) && !isDownload) {
            return AvailableStatus.UNCHECKABLE;
        }
        if (account != null) {
            login(account, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("/error\\-title\\-oops\\.png\\)") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(",target: \\'motionbooks/")) {
            logger.info("Motionbooks are not supported (yet)");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Redirects can lead to unsupported/offline links/linktypes */
        if (br.getURL().matches(TYPE_BLOG_OFFLINE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        // String filename_server = null;
        if (!getPluginConfig().getBooleanProperty(FilenameFromServer, false) && !link.getPluginPatternMatcher().matches(LINKTYPE_STATUS)) {
            filename = br.getRegex(GENERALFILENAMEREGEX).getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String fid = new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
            filename = Encoding.htmlDecode(filename.trim()) + "_" + fid;
        }
        if (link.getPluginPatternMatcher().matches(LINKTYPE_STATUS)) {
            filename = new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename.trim());
        }
        String ext = null;
        boolean setOfficialDownloadFilesize = false;
        final String officialDownloadFilesize = br.getRegex("(?i)>\\s*Image size\\s*</div><div [^>]*>\\d+x\\d+px\\s*(\\d+[^>]+)</div>").getMatch(0);
        // final boolean accountNeededForOfficialDownload = br.containsHTML("(?i)Log in to download");
        final String officialDownloadurl = br.getRegex("data-hook=\"download_button\"[^>]*href=\"(https?://[^\"]+)").getMatch(0);
        final String displayedImageURL = HTMLSearch.searchMetaTag(br, "og:image");
        /* Check if either user wants to download the html code or if we have a linktype which needs this. */
        if (this.getPluginConfig().getBooleanProperty(FORCEHTMLDOWNLOAD, false) || link.getPluginPatternMatcher().matches(LINKTYPE_JOURNAL) || link.getPluginPatternMatcher().matches(LINKTYPE_STATUS)) {
            downloadHTML = true;
            dllink = br.getURL();
            ext = ".html";
        } else if (officialDownloadurl != null) {
            this.dllink = officialDownloadurl.replace("&amp;", "&");
            setOfficialDownloadFilesize = true;
        } else if (displayedImageURL != null) {
            this.dllink = displayedImageURL;
        } else if (br.containsHTML(TYPE_DOWNLOADFORBIDDEN_SWF)) {
            String url_swf_sandbox = br.getRegex("class=\"flashtime\" src=\"(https?://sandbox\\.deviantart\\.com[^<>\"]*?)\"").getMatch(0);
            if (url_swf_sandbox == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Fix that ... */
            url_swf_sandbox = url_swf_sandbox.replace("?", "/?");
            br.getPage(url_swf_sandbox);
            this.dllink = br.getRegex("<param name=\"movie\" value=\"(https?://[^<>\"]*?\\.swf)\"").getMatch(0);
            if (this.dllink == null) {
                this.dllink = br.getRegex("id=\"sandboxembed\" src=\"(http[^<>\"]+\\.swf)\"").getMatch(0);
            }
            ext = ".swf";
        } else if (br.containsHTML(TYPE_DOWNLOADALLOWED_HTML) || br.containsHTML(TYPE_DOWNLOADFORBIDDEN_HTML)) {
            downloadHTML = true;
            dllink = br.getURL();
            ext = ".html";
        } else if (isAccountRequiredUploaderDecision(br)) {
            /* Account needed to view/download */
            ext = ".html";
        } else {
            /* Workaround for invalid domain(s) e.g. "laur-.deviantart.com" */
            final String cookie = this.br.getCookie(null, "userinfo", Cookies.NOTDELETEDPATTERN);
            if (cookie != null) {
                br.setCookie(this.getHost(), "userinfo", cookie);
            }
        }
        if (filename == null && this.dllink != null) { // Config FilenameFromServer is enabled
            filename = Plugin.getFileNameFromURL(new URL(this.dllink));
        }
        if (ext == null) {
            ext = Plugin.getFileNameExtensionFromURL(this.dllink);
        }
        if (StringUtils.isEmpty(this.dllink)) {
            this.dllink = getDllink();
            // if (StringUtils.isEmpty(this.dllink)) {
            // this.dllink = this.getDirecturl();
            // }
        }
        link.setVerifiedFileSize(-1); // reset this every time as user can change settings
        if (downloadHTML) {
            try {
                link.setDownloadSize(br.getRequest().getHtmlCode().getBytes("UTF-8").length);
            } catch (final UnsupportedEncodingException ignore) {
                ignore.printStackTrace();
            }
        } else if (officialDownloadFilesize != null && setOfficialDownloadFilesize) {
            link.setDownloadSize(SizeFormatter.getSize(officialDownloadFilesize.replace(",", "")));
        } else if (this.getPluginConfig().getBooleanProperty(SKIP_FILESIZECHECK, default_SKIP_FILESIZECHECK) == false && !isDownload && !StringUtils.isEmpty(this.dllink)) {
            final Browser br2 = br.cloneBrowser();
            /* Workaround for old downloadcore bug that can lead to incomplete files */
            br2.getHeaders().put("Accept-Encoding", "identity");
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(this.dllink);
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
        filename = Encoding.htmlDecode(filename).trim();
        filename = this.correctOrApplyFileNameExtension(filename, ext);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    private boolean isAccountRequired(final Browser br) {
        if (isAccountRequiredUploaderDecision(br) || isAccountRequiredMatureContent(br)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAccountRequiredUploaderDecision(final Browser br) {
        if (br.containsHTML("(?i)has limited the viewing of this artwork\\s*<")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAccountRequiredMatureContent(final Browser br) {
        if (br.containsHTML("(?i)>\\s*This content is intended for mature audiences")) {
            return true;
        } else {
            return false;
        }
    }

    /* TODO: Fix- or delete this */
    private String[] getDirecturl() {
        String ret[] = null;
        final String downloadURLs1[][] = br.getRegex("dev-page-download\"[\t\n\r ]*?href=\"(https?://(?:www\\.)?deviantart\\.com/download/[^<>\"]*?)\"(.*?)</a").getMatches();
        if (downloadURLs1 != null) {
            int best = -1;
            for (final String downloadURL[] : downloadURLs1) {
                final String height = new Regex(downloadURL[1], "Download\\s*\\d+\\s*&#215;\\s*(\\d+)\\s*<").getMatch(0);
                if (height != null) {
                    if (best == -1 || Integer.parseInt(height) > best) {
                        ret = downloadURL;
                        best = Integer.parseInt(height);
                    }
                }
            }
        }
        if (ret == null) {
            final String downloadURLs2[][] = br.getRegex("data-download_url=\"(https?://(?:www\\.)?deviantart\\.com/download/[^<>\"]*?)\"(.*?)</a").getMatches();
            if (downloadURLs2 != null) {
                int best = -1;
                for (final String downloadURL[] : downloadURLs2) {
                    final String height = new Regex(downloadURL[1], "Download\\s*\\d+\\s*&#215;\\s*(\\d+)\\s*<").getMatch(0);
                    if (height != null) {
                        if (best == -1 || Integer.parseInt(height) > best) {
                            ret = downloadURL;
                            best = Integer.parseInt(height);
                        }
                    }
                }
            }
            if (ret == null) {
                if (downloadURLs1 != null && downloadURLs1.length > 0) {
                    ret = downloadURLs1[downloadURLs1.length - 1];
                } else if (downloadURLs2.length > 0) {
                    ret = downloadURLs2[downloadURLs2.length - 1];
                }
            }
        }
        if (ret != null) {
            ret[0] = Encoding.htmlDecode(ret[0]);
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account, true);
        if (this.isAccountRequired(br)) {
            throw new AccountRequiredException();
        } else if (StringUtils.isEmpty(this.dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Workaround for old downloadcore bug that can lead to incomplete files */
        /* Disable chunks as we only download pictures or small files */
        br.getHeaders().put("Accept-Encoding", "identity");
        boolean resume = true;
        if (downloadHTML) {
            link.setVerifiedFileSize(-1);
            resume = false;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, this.dllink, resume, 1);
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

    /* TODO: Fix- or delete this */
    private String getDllink() throws PluginException {
        if (dllink == null) {
            final String videoStreamURLs[][] = br.getRegex("\"src\":\"(https?:[^<>\"]*?mp4)\"(.*?)\\}").getMatches();
            if (videoStreamURLs != null && videoStreamURLs.length > 0) {
                int best = -1;
                String bestURL = null;
                for (final String videoStreamURL[] : videoStreamURLs) {
                    final String height = new Regex(videoStreamURL[1], "height\":\\s*(\\d+)").getMatch(0);
                    if (height != null) {
                        if (best == -1 || Integer.parseInt(height) > best) {
                            bestURL = videoStreamURL[0];
                            best = Integer.parseInt(height);
                        }
                    }
                }
                if (bestURL == null) {
                    bestURL = videoStreamURLs[0][0];
                }
                bestURL = bestURL.replace("\\", "");
                dllink = bestURL;
                return bestURL;
            }
            String dllink = null;
            /* First try to get downloadlink, if that doesn't exist, try to get the link to the picture which is displayed in browser */
            /*
             * NEVER open up this RegEx as sometimes users link downloadlinks in the description --> Open RegEx will lead to plugin errors
             * in some rare cases
             */
            if (dllink == null) {
                dllink = br.getRegex("dev-page-download\"[\t\n\r ]*?href=\"(https?://(www\\.)?deviantart\\.com/download/[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                if (br.containsHTML(">Mature Content</span>")) {
                    /* Prefer HQ */
                    dllink = getHQpic();
                    if (dllink == null) {
                        dllink = br.getRegex("data\\-gmiclass=\"ResViewSizer_img\".*?src=\"(htts?://[^<>\"]*?)\"").getMatch(0);
                    }
                    if (dllink == null) {
                        dllink = br.getRegex("<img collect_rid=\"\\d+:\\d+\" src=\"(https?://[^\"]+)").getMatch(0);
                    }
                } else {
                    /* Prefer HQ */
                    dllink = getHQpic();
                    if (dllink == null) {
                        final String images[] = br.getRegex("<img collect_rid=\"[0-9:]+\" src=\"(https?[^<>\"]*?)\"").getColumn(0);
                        if (images != null && images.length > 0) {
                            String org = null;
                            for (String image : images) {
                                if (image.contains("/pre/") || image.contains("//pre")) {
                                    continue;
                                } else {
                                    org = image;
                                    break;
                                }
                            }
                            if (org == null) {
                                dllink = images[0];
                            } else {
                                dllink = org;
                            }
                        } else {
                            dllink = br.getRegex("(name|property)=\"og:image\" content=\"(https?://[^<>\"]*?)\"").getMatch(1);
                        }
                    }
                }
            }
            if (dllink != null) {
                dllink = dllink.replace("\\", "");
                dllink = Encoding.htmlDecode(dllink);
            }
        }
        return dllink;
    }

    /* TODO: Fix- or delete this */
    private String getHQpic() {
        final String hqurl = br.getRegex("class=\"dev\\-content\\-normal[^\"]*?\">\\s*<img collect_rid=\"[0-9:]+\" src=\"(https?://[^<>\"]*?)\"").getMatch(0);
        return hqurl;
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
        final String loggedINProfileURL = br.getRegex("data-userid=\"\\d+\" data-useruuid=\"[^\"]+\" href=\"(https?://[^\"]+)\"").getMatch(0);
        if (loggedINProfileURL != null) {
            return true;
        } else if (br.containsHTML("/users/logout")) {
            return true;
        } else {
            return false;
        }
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

    private static final boolean default_FASTLINKCHECK_2                  = true;
    private static final boolean default_FASTLINKCHECK_ALL                = false;
    private static final String  SKIP_FILESIZECHECK                       = "SKIP_FILESIZECHECK";
    private static final boolean default_SKIP_FILESIZECHECK               = true;
    private static final boolean default_FORCEHTMLDOWNLOAD                = false;
    public static final boolean  default_CRAWL_GIVEN_OFFSETS_INDIVIDUALLY = false;
    private static final String  FASTLINKCHECK_ALL                        = "FASTLINKCHECK_ALL";
    private static final String  FilenameFromServer                       = "FilenameFromServer";

    public void setConfigElements() {
        final StringBuilder sbinfo = new StringBuilder();
        String fastlinkchecktext = null;
        String fastlinkcheck_all_text = null;
        String forcehtmldownloadtext = null;
        String decryptOffsetsIndividually = null;
        final String lang = System.getProperty("user.language");
        if ("de".equalsIgnoreCase(lang)) {
            fastlinkchecktext = "Schnelle Linküberprüfung aktivieren? (finaler Dateiname- und Größe werden nicht sofort angezeigt!)?";
            fastlinkcheck_all_text = "Schnelle Linküberprüfung für ALLE Links aktivieren?\r\nBedenke, dass der online-status bis zum Downloadstart nicht aussagekräftig ist!";
            forcehtmldownloadtext = "HTML Code statt dem eigentlichen Inhalt (Dateien/Bilder) laden?";
            decryptOffsetsIndividually = "Bei gegebenem 'offset=XX' im Link nur dieses Crawlen, statt ab diesem bis zum Ende zu crawlen?";
            sbinfo.append("Bitte beachten: solltest Du nur Seite 1 einer Gallerie sammeln wollen, so stelle sicher, dass \"?offset=0\" am Ende der URL steht.\r\n");
            sbinfo.append("Du kannst auch zu einer anderen Seite wechseln, auf Seite 1 klicken und deren URL einfügen.");
        } else {
            fastlinkchecktext = "Enable fast link check? (final file name- and size won't be shown until downloadstart!)?";
            fastlinkcheck_all_text = "Enable fast linkcheck for ALL links?\r\nNote that this means that you can't see the real online/offline status until the download is started!";
            forcehtmldownloadtext = "Download html code instead of the media (files/pictures)?";
            decryptOffsetsIndividually = "On given 'offset=XX', crawl only this offset instead of crawling from this offset until the end?";
            sbinfo.append("Please note: if you wanted to grab only page 1 of a gallery, please make sure that \"?offset=0\" is added to its URL.\r\n");
            sbinfo.append("You can also switch to another page, click on page 1 and grab its URL.");
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK_2, fastlinkchecktext).setDefaultValue(default_FASTLINKCHECK_2));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK_ALL, fastlinkcheck_all_text).setDefaultValue(default_FASTLINKCHECK_ALL));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SKIP_FILESIZECHECK, "Avoid additional requests to check file size?").setDefaultValue(default_SKIP_FILESIZECHECK));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "FilenameFromServer", "Choose file name from download link with unique identifier?").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FORCEHTMLDOWNLOAD, forcehtmldownloadtext).setDefaultValue(default_FORCEHTMLDOWNLOAD));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CRAWL_GIVEN_OFFSETS_INDIVIDUALLY, decryptOffsetsIndividually).setDefaultValue(default_CRAWL_GIVEN_OFFSETS_INDIVIDUALLY));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbinfo.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
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