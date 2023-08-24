//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "e-hentai.org" }, urls = { "https?://(?:[a-z0-9\\-]+\\.)?(?:e-hentai\\.org|exhentai\\.org)/(?:s/[a-f0-9]{10}/\\d+-\\d+|mpv/\\d+/[a-f0-9]{10}/#page\\d+)|ehentaiarchive://\\d+/[a-z0-9]+" })
public class EHentaiOrg extends PluginForHost {
    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    public EHentaiOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://exhentai.org/");
        setConfigElements();
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = new Browser();
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        if (new Regex(link.getPluginPatternMatcher(), TYPE_ARCHIVE).patternFind()) {
            return -8;
        } else {
            /* Small file/image -> Limit chunks to 1 */
            return 1;
        }
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    private static final int    free_maxdownloads                  = -1;
    private String              dllink                             = null;
    private final boolean       ENABLE_RANDOM_UA                   = true;
    public static final String  PREFER_ORIGINAL_QUALITY            = "PREFER_ORIGINAL_QUALITY";
    public static final String  PREFER_ORIGINAL_FILENAME           = "PREFER_ORIGINAL_FILENAME";
    public static final String  SETTING_DOWNLOAD_ZIP               = "DOWNLOAD_ZIP";
    private static final String TYPE_EXHENTAI                      = "exhentai\\.org";
    private static final String TYPE_ARCHIVE                       = "(?i)ehentaiarchive://(\\d+)/([a-z0-9]+)";
    private static final String TYPE_SINGLE_IMAGE_MULTI_PAGE_VIEW  = "(?i)https?://[^/]+/mpv/(\\d+)/([a-f0-9]{10})/#page(\\d+)";
    private static final String TYPE_SINGLE_IMAGE                  = "(?i)https?://[^/]+/s/([a-f0-9]{10})/(\\d+)-(\\d+)";
    public static final String  PROPERTY_GALLERY_URL               = "gallery_url";
    public static final String  PROPERTY_MPVKEY                    = "mpvkey";
    public static final String  PROPERTY_IMAGEKEY                  = "imagekey";
    private final String        PROPERTY_ACCOUNT_IMAGE_POINTS_LEFT = "image_points_left";

    @Override
    public String getAGBLink() {
        return "https://e-hentai.org/tos.php";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(TYPE_SINGLE_IMAGE)) {
            final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), TYPE_SINGLE_IMAGE);
            return this.getHost() + "://" + urlinfo.getMatch(1) + "_" + urlinfo.getMatch(2);
        } else if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(TYPE_SINGLE_IMAGE_MULTI_PAGE_VIEW)) {
            final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), TYPE_SINGLE_IMAGE_MULTI_PAGE_VIEW);
            return this.getHost() + "://" + urlinfo.getMatch(0) + "_" + urlinfo.getMatch(2);
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (requiresAccount(link) && account == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    private boolean requiresAccount(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), TYPE_ARCHIVE).matches() || new Regex(link.getPluginPatternMatcher(), TYPE_EXHENTAI).matches();
    }

    private String getDirecturlproperty(final Account account) {
        if (account != null && this.getPluginConfig().getBooleanProperty(PREFER_ORIGINAL_QUALITY, default_PREFER_ORIGINAL_QUALITY)) {
            return "directurl_original";
        } else {
            return "directurl";
        }
    }

    /**
     * Take account from download candidate! </br>
     * 2021-01-18: There is an API available but it is only returning the metadata: https://ehwiki.org/wiki/API
     *
     * @param link
     * @param account
     * @return
     * @throws Exception
     */
    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        // nullification
        dllink = null;
        if (account != null) {
            login(this.br, account, false);
        } else {
            if (this.requiresAccount(link)) {
                throw new AccountRequiredException();
            }
            if (ENABLE_RANDOM_UA) {
                /* Be sure only to use random UA when an account is not used! */
                /*
                 * Using a different UA for every download might be a bit obvious but at the moment, this fixed the error-server responses
                 * as it tricks it into thinking that we re a lot of users and not only one.
                 */
                br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
            }
        }
        final String directurlproperty = getDirecturlproperty(account);
        final boolean preferOriginalQuality = this.getPluginConfig().getBooleanProperty(PREFER_ORIGINAL_QUALITY, default_PREFER_ORIGINAL_QUALITY);
        if (new Regex(link.getPluginPatternMatcher(), TYPE_ARCHIVE).patternFind()) {
            /* Account archive download */
            if (account == null) {
                /* Cannot check without account */
                throw new AccountRequiredException();
            }
            final String galleryid = new Regex(link.getPluginPatternMatcher(), "(\\d+)/([a-z0-9]+)$").getMatch(0);
            final String galleryhash = new Regex(link.getPluginPatternMatcher(), "(\\d+)/([a-z0-9]+)$").getMatch(1);
            final String host; // e-hentai.org or exhentai.org
            if (link.hasProperty(PROPERTY_GALLERY_URL)) {
                host = Browser.getHost(link.getStringProperty(PROPERTY_GALLERY_URL));
            } else {
                /* Fallback for revision 45332 and prior */
                host = this.getHost();
            }
            /* Check if gallery still exists. */
            br.getPage("https://" + host + "/g/" + galleryid + "/" + galleryhash);
            if (isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (isDownload) {
                /*
                 * 2022-01-10: Depending on account settings, some galleries won't be displayed by default for some users. They have to
                 * click on a "View anyways" button to continue.
                 */
                final String skipContentWarningURL = br.getRegex("(?i)\"(https?://[^/]+/g/\\d+/[a-f0-9]+/\\?nw=session)\"[^>]*>\\s*View Gallery").getMatch(0);
                if (skipContentWarningURL != null) {
                    logger.info("Skipping content warning via URL: " + skipContentWarningURL);
                    br.getPage(skipContentWarningURL);
                }
                String continue_url = br.getRegex("popUp\\('(https?://[^/]+/archiver\\.php\\?[^<>\"\\']+)'").getMatch(0);
                if (continue_url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                continue_url = Encoding.htmlDecode(continue_url);
                br.getPage(continue_url);
                /* Another step */
                final String continue_url2 = br.getRegex("document\\.getElementById\\(\"continue\"\\).*?document\\.location\\s*=\\s*\"((?:/|http)[^\"]+)\"").getMatch(0);
                /**
                 * 2022-01-07: Two types can be available: "Original Archive" and "Resample Archive". </br>
                 * We prefer best quality --> "Original Archive"
                 */
                final Form continueForm = br.getFormByInputFieldKeyValue("dltype", "org");
                if (continue_url2 != null) {
                    /* Old way */
                    br.getPage(continue_url2);
                } else if (continueForm != null) {
                    br.submitForm(continueForm);
                }
                final String continue3 = br.getRegex("id=\"continue\"[^>]*>\\(<a href=\"(https?://[^\"]+)").getMatch(0);
                if (continue3 != null) {
                    br.getPage(continue3);
                }
                dllink = br.getRegex("document\\.location\\s*=\\s*\"((?:/|http)[^\"]+)\"").getMatch(0);
                if (dllink == null) {
                    /* 2022-01-07 */
                    dllink = br.getRegex("(?i)href=\"([^\"]+)\"[^>]*>\\s*Click Here To Start Downloading").getMatch(0);
                }
                if (dllink == null && br.containsHTML("name=\"dlcheck\"[^<>]*value=\"Insufficient Funds\"")) {
                    /* 2020-05-20: E.g. not enough credits for archive downloads but enough to download single images. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Insufficient funds for downloading archives");
                }
            }
            return AvailableStatus.TRUE;
        } else if (new Regex(link.getPluginPatternMatcher(), TYPE_SINGLE_IMAGE_MULTI_PAGE_VIEW).patternFind()) {
            /* 2020-05-21: New linktype "Multi Page View" */
            br.setFollowRedirects(true);
            final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), TYPE_SINGLE_IMAGE_MULTI_PAGE_VIEW);
            final String galleryid = urlinfo.getMatch(0);
            final String page = urlinfo.getMatch(2);
            /*
             * 2020-05-21: TODO: Check if this ever expires - it would be easy to renew this but would require additional http requests
             * which slows down the process of downloading a lot of small files significantly!
             */
            final String mpvkey = link.getStringProperty(PROPERTY_MPVKEY);
            final String imagekey = link.getStringProperty(PROPERTY_IMAGEKEY);
            if (galleryid == null || page == null || mpvkey == null || imagekey == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> postData = new HashMap<String, Object>();
            postData.put("method", "imagedispatch");
            postData.put("gid", galleryid);
            postData.put("page", page);
            postData.put("imgkey", imagekey);
            postData.put("mpvkey", mpvkey);
            final String host;
            if (link.getPluginPatternMatcher().contains("exhentai")) {
                host = "exhentai.org";
                br.postPageRaw("https://exhentai.org/api.php", JSonStorage.serializeToJson(postData));
            } else {
                host = "e-hentai.org";
                br.postPageRaw("https://api.e-hentai.org/api.php", JSonStorage.serializeToJson(postData));
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final String filesizeStr;
            final String lowResInfo = (String) entries.get("d");
            final String origInfo = (String) entries.get("o");
            /* 2020-05-21: Only registered users can download originals! */
            boolean downloadRequiresImagePoints = false;
            if (account != null && preferOriginalQuality) {
                /* Download original file */
                filesizeStr = new Regex(origInfo, "(\\d+\\.\\d{1,2} [A-Za-z]+)").getMatch(0);
                this.dllink = (String) entries.get("lf");
                if (!this.dllink.startsWith("http") && !this.dllink.startsWith("/")) {
                    this.dllink = "https://" + host + "/" + this.dllink;
                }
                downloadRequiresImagePoints = true;
            } else {
                /* Download "lower quality" file */
                filesizeStr = new Regex(lowResInfo, "(\\d+\\.\\d{1,2} [A-Za-z]+)").getMatch(0);
                this.dllink = (String) entries.get("i");
            }
            /* Only perform linkcheck if filesize is not given as text! */
            if (filesizeStr != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
            }
            if (filesizeStr == null && !downloadRequiresImagePoints && !isDownload) {
                URLConnectionAdapter con = null;
                try {
                    final Browser brc = br.cloneBrowser();
                    con = brc.openHeadConnection(dllink);
                    if (this.looksLikeDownloadableContent(con)) {
                        if (con.getCompleteContentLength() > 0) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        }
                        link.setProperty(directurlproperty, con.getURL().toString());
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        } else if (link.getPluginPatternMatcher().matches(TYPE_SINGLE_IMAGE)) {
            /* TYPE_SINGLE_IMAGE e-hentai.org and exhentai.org */
            br.getPage(link.getPluginPatternMatcher());
            if (isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getRequest().getHtmlCode().length() <= 1) {
                /**
                 * 2020-05-23: Empty page: Most likely exhentai.org URL with account that does not have permissions to access it or exhentai
                 * login failure.
                 */
                final String errorDetailsText = "Received blank page";
                if (account != null) {
                    throw new AccountUnavailableException("exhentai login failure: " + errorDetailsText, 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Website problem: " + errorDetailsText);
                }
            } else if (br.containsHTML("Your IP address has been temporarily banned for excessive pageloads")) {
                final String errortext = "Your IP address has been temporarily banned for excessive pageloads";
                long waitMillis = 30 * 60 * 1000;
                if (account == null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errortext, waitMillis);
                } else {
                    throw new AccountUnavailableException(errortext, waitMillis);
                }
            } else if (br.getRequest().getHtmlCode().length() <= 100) {
                /* This should never happen! */
                final String errorDetailsText = "Got page with max 100 characters";
                if (account != null) {
                    throw new AccountUnavailableException("Login failure: " + errorDetailsText, 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorDetailsText);
                }
            }
            String ext = null;
            String originalFileName = br.getRegex("(?i)<div>([^<>]*\\.(jpe?g|png|gif))\\s*::\\s*\\d+").getMatch(0);
            final String extDefault = ".jpg";
            if (originalFileName != null) {
                originalFileName = Encoding.htmlDecode(originalFileName).trim();
                ext = getFileNameExtensionFromString(originalFileName, extDefault);
            }
            if (link.getForcedFileName() != null) {
                /* Special handling: Package customizer altered, or user altered value, we need to update this value. */
                link.setForcedFileName(this.correctOrApplyFileNameExtension(link.getForcedFileName(), ext));
            } else {
                final String namepart = getFileTitle(br, link);
                /* Set filename based on user setting */
                final boolean preferOriginalFilename = getPluginConfig().getBooleanProperty(EHentaiOrg.PREFER_ORIGINAL_FILENAME, EHentaiOrg.default_PREFER_ORIGINAL_FILENAME);
                if (StringUtils.isNotEmpty(originalFileName) && preferOriginalFilename) {
                    link.setFinalFileName(originalFileName);
                } else {
                    /* crawler might not set file extension. */
                    link.setFinalFileName(namepart + ext);
                }
            }
            final boolean userPrefersOriginalDownloadurl = this.getPluginConfig().getBooleanProperty(PREFER_ORIGINAL_QUALITY, default_PREFER_ORIGINAL_QUALITY);
            final Regex originalDownloadRegex = br.getRegex("href=\"(https?://(?:(?:g\\.)?e\\-hentai|exhentai)\\.org/fullimg\\.php[^<>\"]*?)\">\\s*Download original \\d+ x \\d+ ([^<>\"]*?) source\\s*</a>");
            String originalImageDownloadurl = originalDownloadRegex.getMatch(0);
            if (originalImageDownloadurl != null) {
                originalImageDownloadurl = Encoding.htmlDecode(originalImageDownloadurl);
            } else {
                if (userPrefersOriginalDownloadurl) {
                    /* Some [exhentai?] .gif files are not downloadable as original e.g.: https://exhentai.org/s/f448e4f296/2652144-204 */
                    logger.info("User prefers original image but failed to find original image downloadurl");
                }
            }
            if (account != null && originalImageDownloadurl != null) {
                dllink = originalImageDownloadurl;
                final String filesizeStrOriginalImage = originalDownloadRegex.getMatch(1);
                if (filesizeStrOriginalImage != null) {
                    link.setDownloadSize(SizeFormatter.getSize(filesizeStrOriginalImage));
                }
            } else {
                try {
                    this.dllink = getNormalmageDownloadurl(link, account, isDownload);
                } catch (final PluginException e) {
                    /* Limit reached */
                    final String host = br.getHost();
                    if (isLimitError && host.equals(host_exhentai) && link.getPluginPatternMatcher().contains(host_exhentai) && isDownload) {
                        final String newurl = link.getPluginPatternMatcher().replaceFirst(Pattern.quote(host_exhentai), host_ehentai);
                        logger.info("Attempting workaround via ehentai website: " + newurl);
                        br.getPage(newurl);
                        try {
                            this.dllink = getNormalmageDownloadurl(link, account, isDownload);
                        } catch (final Exception ignore) {
                            logger.log(ignore);
                            logger.info("Failed to get around limit");
                            /* Throw initial Exception */
                            throw e;
                        }
                    } else {
                        throw e;
                    }
                }
                if (this.dllink != null) {
                    link.setProperty(getDirecturlproperty(account), this.dllink);
                }
                final String filesizeStrNormalImage = br.getRegex(":: ([^:<>\"]+)</div><div class=\"sn\"").getMatch(0);
                if (filesizeStrNormalImage != null) {
                    link.setDownloadSize(SizeFormatter.getSize(filesizeStrNormalImage));
                }
            }
        } else {
            /* This should never happen */
            logger.warning("Unsupported URL");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return AvailableStatus.TRUE;
    }

    /** Returns whether or not a gallery is offline. */
    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.getRequest().getHtmlCode().matches("(?i)^Invalid page\\.?")) {
            return true;
        } else {
            return false;
        }
    }

    /** Ugly public variable to try to avoid the need to add a custom Exception-type. */
    private boolean isLimitError = false;

    /** Returns direct downloadable URL to normal image (not original image). */
    private String getNormalmageDownloadurl(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        isLimitError = false;
        final String html = br.getRequest().getHtmlCode();
        String cleanup = new Regex(html, "<iframe[^>]*>(.*?)<iframe").getMatch(0);
        if (cleanup == null) {
            cleanup = new Regex(html, "<div id=\"i3\">(.*?)</div").getMatch(0);
        }
        String dllink = new Regex(cleanup, "<img [^>]*src=(\"|\\')([^\"\\'<>]+)\\1").getMatch(1);
        if (dllink == null) {
            /* 2017-01-30: Until now only jp(e)g was allowed, now also png. */
            dllink = new Regex(html, "(?i)<img [^>]*src=(\"|')([^\"\\'<>]{30,}(?:\\.jpe?g|png|gif))\\1").getMatch(1);
        }
        if (dllink == null) {
            logger.info("Failed to find final downloadurl");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (StringUtils.containsIgnoreCase(dllink, "509.gif")) {
            /* E.g. https://ehgt.org/g/509.gif or https://exhentai.org/img/509.gif */
            final String errorMessageOnLimitReached = "Error 509: Downloadlimit reached";
            final boolean allowAdditionalPointsCheck = false;
            if (account != null && allowAdditionalPointsCheck) {
                /* URL to current image */
                final String targetURL = br.getURL();
                this.sleep(3000l, link);
                /*
                 * script we require!
                 */
                // final Browser brc = br.cloneBrowser();
                br.setFollowRedirects(true);
                br.getPage(MAINPAGE_ehentai + "/home.php");
                logger.info("Credits before:");
                int[] creditsLeftInfo = getImagePointsLeftInfo(br); // prints credits left (logs them)
                br.getPage(MAINPAGE_ehentai + "/hathperks.php");
                br.getPage(MAINPAGE_ehentai + "/home.php");
                logger.info("Credits AFTER:");
                creditsLeftInfo = getImagePointsLeftInfo(br);
                if (creditsLeftInfo != null && creditsLeftInfo[0] >= creditsLeftInfo[1]) {
                    logger.info("Confirmed limit reached according to remaining credits");
                    exceptionLimitReached(account, errorMessageOnLimitReached);
                } else {
                    // br.getPage(targetURL);
                    exceptionLimitReached(account, "Unknown error in downloadlimit handling occured: Looks like limit reached but it hasn't been reached?");
                }
            }
            isLimitError = true;
            exceptionLimitReached(account, errorMessageOnLimitReached);
        }
        return dllink;
    }

    private void exceptionLimitReached(final Account account, final String errormessage) throws PluginException {
        if (account == null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errormessage, 5 * 60 * 1000);
        } else {
            /* 2020-03-03: This should not be required anymore --> Lead to timeouts --> No idea what it was good for */
            // br.getPage("http://exhentai.org/home.php");
            // account.saveCookies(br.getCookies(MAINPAGE), "");
            throw new AccountUnavailableException(errormessage, 5 * 60 * 1000);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private boolean requiresAccount(final String url) {
        return url != null && StringUtils.containsIgnoreCase(url, "/img/kokomade.jpg");
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        final String directurlproperty = getDirecturlproperty(account);
        final String storedDirecturl = link.getStringProperty(directurlproperty);
        if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl: " + storedDirecturl);
            this.dllink = storedDirecturl;
        } else {
            requestFileInformation(link, account, true);
            if (StringUtils.isEmpty(dllink)) {
                /* This should never happen! */
                logger.warning("Failed to find final downloadurl");
                this.handleErrorsLastResort(link, account, this.br);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), getMaxChunks(link, account));
        long expectedFilesize = link.getView().getBytesTotal();
        if (expectedFilesize > 1000) {
            /*
             * Allow content to be up to 1KB smaller than expected filesize --> All to prevent downloading static images e.g. when trying to
             * download after randomly being logged-out.
             */
            expectedFilesize -= 1000;
        }
        try {
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                final String errorNotEnoughPoints1 = "Downloading original files (during peak hours )?requires GP, and you do not have enough\\.";
                final String errorNotEnoughPoints2 = "Downloading original files of this gallery (during peak hours )?requires GP, and you do not have enough\\.";
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else if (br.containsHTML("﻿(?i)You have exceeded your image viewing limits")) {
                    exceptionLimitReached(account, "You have exceeded your image viewing limits");
                } else if (br.getURL().contains("bounce_login.php")) {
                    /* Account required / re-login required */
                    if (account != null) {
                        throw new AccountUnavailableException("Account / Re-login required", 1 * 60 * 1000l);
                    } else {
                        /* This should never happen */
                        throw new AccountRequiredException();
                    }
                } else if (br.containsHTML(Pattern.quote(errorNotEnoughPoints1))) {
                    exceptionLimitReached(account, "Downloading original files (during peak hours) requires GP, and you do not have enough.");
                } else if (br.containsHTML(Pattern.quote(errorNotEnoughPoints2))) {
                    exceptionLimitReached(account, "Downloading original files of this gallery (during peak hours) requires GP, and you do not have enough.");
                } else if (br.getRequest().getHtmlCode().length() <= 150 && !br.getRequest().getHtmlCode().startsWith("<html")) {
                    /* No html error but plaintext -> Looks like an errormessage we don't know */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Unknown error: " + br.getRequest().getHtmlCode());
                } else {
                    this.handleErrorsLastResort(link, account, this.br);
                }
            } else if (dl.getConnection().getResponseCode() != 206 && dl.getConnection().getCompleteContentLength() > 0 && expectedFilesize > 0 && dl.getConnection().getCompleteContentLength() < expectedFilesize) {
                /* Don't jump into this for response code 206 Partial Content (when download is resumed). */
                br.followConnection(true);
                /* Rare error: E.g. "403 picture" is smaller than 1 KB but is still downloaded content (picture). */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - file is too small:" + dl.getConnection().getCompleteContentLength(), 2 * 60 * 1000l);
            } else if (requiresAccount(dl.getConnection().getURL().toString())) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directurlproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired");
            } else {
                throw e;
            }
        }
        /* Store directurl to be able to re-use it later. */
        link.setProperty(getDirecturlproperty(account), dl.getConnection().getURL().toString());
        final String headerFilename = Plugin.getFileNameFromHeader(dl.getConnection());
        final String finalFilename = link.getFinalFileName();
        final String extByMimeType = Plugin.getExtensionFromMimeTypeStatic(dl.getConnection().getContentType());
        if (headerFilename != null && finalFilename != null) {
            final String newExt = Plugin.getFileNameExtensionFromString(headerFilename);
            if (newExt != null) {
                final String newFilename = this.correctOrApplyFileNameExtension(finalFilename, newExt);
                if (!newFilename.equals(finalFilename)) {
                    logger.info("Corrected file-extension before download by header | New filename: " + newFilename);
                    link.setFinalFileName(newFilename);
                }
            }
        } else if (extByMimeType != null) {
            final String newFilename = this.correctOrApplyFileNameExtension(finalFilename, "." + extByMimeType);
            if (!newFilename.equals(finalFilename)) {
                logger.info("Corrected file-extension before download by Content-Type | New filename: " + newFilename);
                link.setFinalFileName(newFilename);
            }
        }
        dl.startDownload();
    }

    private final String host_ehentai      = "e-hentai.org";
    private final String MAINPAGE_ehentai  = "https://" + host_ehentai;
    private final String host_exhentai     = "exhentai.org";
    private final String MAINPAGE_exhentai = "https://" + host_exhentai;

    /** 2019-11-26: Alternative way to login: https://e-hentai.org/bounce_login.php?b=d&bt=1-1 */
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            /* About 'hathperks.php': */
            /*
             * 2020-03-04: About 'hathperks.php': Workaround for serverside bug: Without doing this, accounts with higher credit limits per
             * day (usually >5000), all accounts can be stuck with the daily 5000 limit. Accessing this page first and then '/home.php'
             * fixes this. Accessing hathperks will set two additional cookies: 'sk' and 'hath_perks'
             */
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            final Cookies userCookies = account.loadUserCookies();
            if (userCookies != null) {
                setCookies(br, userCookies);
                if (!force) {
                    /* We trust these cookies --> Do not check them */
                    return;
                }
                if (verifyCookies(account, false)) {
                    /* Success */
                    return;
                } else {
                    if (account.hasEverBeenValid()) {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                    } else {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                    }
                }
            }
            if (cookies != null) {
                setCookies(br, cookies);
                if (!force) {
                    /* We trust these cookies --> Do not check them */
                    return;
                }
                if (verifyCookies(account, false)) {
                    /* Success */
                    return;
                }
            }
            /* Login page with params to redirect to /home.php */
            logger.info("Performing full login");
            br.getPage(MAINPAGE_ehentai + "/bounce_login.php?b=d&bt=1-1");
            /* 2020-03-04: --> Will redirect to forums.* */
            // br.getPage("https://forums.e-hentai.org/index.php?act=Login");
            for (int i = 0; i <= 1; i++) {
                final Form loginform = br.getFormbyKey("CookieDate");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("UserName", account.getUser());
                loginform.put("PassWord", Encoding.urlEncode(account.getPass()));
                if (i > 0 && CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(br)) {
                    /*
                     * First login attempt failed and we get a captcha --> Does not necessarily mean that user entered wrong logindata -
                     * captchas may happen!
                     */
                    final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
                    final String recaptchaV2Response = rc2.getToken();
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                } else if (i > 0) {
                    logger.info("No captcha on 2nd login attempt --> Probably invalid logindata");
                    break;
                }
                br.submitForm(loginform);
                if (isLoggedInEhentai(br)) {
                    logger.info("Stepping out of login loop because: Success");
                    break;
                }
            }
            if (!isLoggedInEhentai(br)) {
                throw new AccountInvalidException();
            }
            /* Double-check */
            if (!verifyCookies(account, false)) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(MAINPAGE_ehentai), "");
        }
    }

    private void setCookies(final Browser br, final Cookies cookies) {
        br.setCookies(MAINPAGE_ehentai, cookies);
        br.setCookies(MAINPAGE_exhentai, cookies);
    }

    /** Sets given cookies and checks if we can login with them. */
    protected boolean verifyCookies(final Account account, final boolean throwExceptionOnFailure) throws Exception {
        // getPage(br, MAINPAGE_ehentai + "/index.php?");
        br.getPage(MAINPAGE_ehentai + "/hathperks.php");
        if (this.isLoggedInEhentai(br)) {
            br.getPage(MAINPAGE_ehentai + "/home.php");
            if (this.isLoggedInEhentai(br)) {
                logger.info("e-hentai.org: Successfully logged in via cookies -> Checking exhentai.org login");
                /* Get- and save exhentai.org cookies too */
                /**
                 * Important! Get- and save exhentai cookies: First time this will happen: </br>
                 * exhentai.org -> forums.e-hentai.org/remoteapi.php?ex= -> exhentai.org/?poni= -> exhentai.org
                 */
                br.getPage(MAINPAGE_exhentai);
                if (this.isLoggedInEhentaiOrExhentai(br)) {
                    logger.info("Successfully logged in exhentai.org");
                } else {
                    logger.warning("Failed to login in exhentai.org");
                    if (throwExceptionOnFailure) {
                        throw new AccountInvalidException("exhentai.org login failed");
                    }
                }
                return true;
            }
        }
        logger.info("Failed to login via cookies");
        br.clearCookies(null);
        return false;
    }

    private boolean isLoggedInEhentai(final Browser br) {
        return br.getCookie(MAINPAGE_ehentai, "ipb_pass_hash", Cookies.NOTDELETEDPATTERN) != null;
    }

    private boolean isLoggedInExhentai(final Browser br) {
        return br.getCookie(MAINPAGE_exhentai, "ipb_pass_hash", Cookies.NOTDELETEDPATTERN) != null;
    }

    private boolean isLoggedInEhentaiOrExhentai(final Browser br) {
        return isLoggedInEhentai(br) || isLoggedInExhentai(br);
    }

    /** Checks for logged in state if account is present and throws plugin_Defect otherwise. */
    private void handleErrorsLastResort(final DownloadLink link, final Account account, final Browser br) throws PluginException {
        if (account != null && !isLoggedInEhentaiOrExhentai(br)) {
            throw new AccountUnavailableException("Session expired?", 5 * 60 * 1000);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(this.br, account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(true);
        br.getPage(MAINPAGE_ehentai + "/home.php");
        final int[] imagePointsLeftInfo = this.getImagePointsLeftInfo(br);
        if (imagePointsLeftInfo != null) {
            account.setProperty(PROPERTY_ACCOUNT_IMAGE_POINTS_LEFT, imagePointsLeftInfo[1] - imagePointsLeftInfo[0]);
            ai.setStatus(String.format(AccountType.FREE.getLabel() + " [Used %d / %d items]", imagePointsLeftInfo[0], imagePointsLeftInfo[1]));
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                /* For development purposes: Set fake trafficlimit so developer can see the limit visually. */
                final long dummyTrafficUsed = SizeFormatter.getSize(imagePointsLeftInfo[0] + "TiB");
                final long dummyTrafficMax = SizeFormatter.getSize(imagePointsLeftInfo[1] + "TiB");
                ai.setTrafficLeft(dummyTrafficMax - dummyTrafficUsed);
                ai.setTrafficMax(dummyTrafficMax);
            } else {
                ai.setUnlimitedTraffic();
            }
        } else {
            logger.warning("Failed to find items_downloadedStr or items_maxStr:" + imagePointsLeftInfo);
            account.removeProperty(PROPERTY_ACCOUNT_IMAGE_POINTS_LEFT);
            ai.setStatus(AccountType.FREE.getLabel() + " Failed to find credits left info");
            ai.setUnlimitedTraffic();
        }
        if (imagePointsLeftInfo != null && imagePointsLeftInfo[0] >= imagePointsLeftInfo[1]) {
            logger.info("Account does not have any credits left --> Set remaining traffic to 0");
            ai.setTrafficLeft(0);
        }
        account.setRefreshTimeout(10 * 60 * 1000l);
        synchronized (account) {
            displayImagePointsUsageInformation(account, imagePointsLeftInfo);
        }
        return ai;
    }

    private int getImagePointsLeft(final Account account) {
        return account.getIntegerProperty(PROPERTY_ACCOUNT_IMAGE_POINTS_LEFT, -1);
    }

    private final String PROPERTY_ACCOUNT__ORIGINAL_IMAGE_DOWNLOAD_IMAGE_POINTS_USAGE_INFORMATION_HAS_BEEN_DISPLAYED = "original_image_download_image_points_usage_information_has_been_displayed";

    private void displayImagePointsUsageInformation(final Account account, final int[] creditsLeftInfo) {
        if (userWantsOriginalImageDownload() && account.getBooleanProperty(PROPERTY_ACCOUNT__ORIGINAL_IMAGE_DOWNLOAD_IMAGE_POINTS_USAGE_INFORMATION_HAS_BEEN_DISPLAYED, false) == false) {
            final Thread thread = new Thread() {
                public void run() {
                    try {
                        String message = "";
                        final String title;
                        title = "E-Hentai - Information about image points usage for downloads of original images";
                        message += "Hello " + account.getUser();
                        message += "\r\nYou prefer to download original quality images from this website.";
                        message += "\r\nDownloading original images counts towards your 'image limits'.";
                        if (creditsLeftInfo != null) {
                            message += "\r\nCurrently used image points: " + creditsLeftInfo[0] + " of " + creditsLeftInfo[1];
                        } else {
                            /* This should never happen. */
                            message += "\r\nCurrently used image points: Unknown";
                        }
                        message += "\r\nYou can also see your remaining 'image points' here: e-hentai.org/home.php";
                        message += "\r\nIf you do not want E-Hentai image downloads via JDownloader to use up your image points, you can disable the download of original images here:";
                        message += "\r\nSettings -> Plugins -> e-hentai.org";
                        message += "\r\nThis dialog is shown once per added E-Hentai account and only for users who prefer to download original images via their plugin settings.";
                        final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                        dialog.setTimeout(300 * 1000);
                        final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                        ret.throwCloseExceptions();
                    } catch (final Throwable e) {
                        getLogger().log(e);
                    }
                };
            };
            thread.setDaemon(true);
            thread.start();
            account.setProperty(PROPERTY_ACCOUNT__ORIGINAL_IMAGE_DOWNLOAD_IMAGE_POINTS_USAGE_INFORMATION_HAS_BEEN_DISPLAYED, true);
        }
    }

    private boolean userWantsOriginalImageDownload() {
        return this.getPluginConfig().getBooleanProperty(PREFER_ORIGINAL_QUALITY, default_PREFER_ORIGINAL_QUALITY);
    }

    /**
     * Access e-hentai.org/home.php before calling this! </br>
     * Returns array of numbers with: </br>
     * [0] = number of items downloaded / used from limit </br>
     * [1] = max limit for this account </br>
     * [1] minus [0] = points left
     */
    private int[] getImagePointsLeftInfo(final Browser br) {
        if (!br.getURL().endsWith("/home.php")) {
            logger.warning("!Developer! You did not access '/home.php' before calling this! It will most likely fail!");
        }
        final String items_downloadedStr = br.getRegex("(?i)You are currently at <strong>(\\d+)</strong>").getMatch(0);
        final String items_maxStr = br.getRegex("(?i)towards a limit of <strong>(\\d+)</strong>").getMatch(0);
        if (items_downloadedStr != null && items_maxStr != null) {
            logger.info("Credits: Used: " + items_downloadedStr + " Max: " + items_maxStr);
            return new int[] { Integer.parseInt(items_downloadedStr), Integer.parseInt(items_maxStr) };
        } else {
            /* Assume true as we can't check */
            logger.warning("Failed to find remaining credits");
            return null;
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    private String getFileTitle(final Browser br, final DownloadLink link) throws PluginException {
        final String filenamePartFromCrawler = link.getStringProperty("namepart");
        if (filenamePartFromCrawler != null) {
            return filenamePartFromCrawler;
        }
        // link has added in a single manner outside of crawler, so no title is given
        final DecimalFormat df = new DecimalFormat("0000");
        // we can do that based on image part
        final String[] uidPart = new Regex(link.getPluginPatternMatcher(), "/(\\d+)-(\\d+)$").getRow(0);
        final String fpName = getTitle(br);
        if (fpName == null || uidPart == null || uidPart.length != 2) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String title = HTMLEntities.unhtmlentities(fpName) + "_" + uidPart[0] + "-" + df.format(Integer.parseInt(uidPart[1]));
        return title;
    }

    public String getTitle(final Browser br) {
        final String title = br.getRegex("<title>([^<>\"]*?)(?:\\s*-\\s*E-Hentai Galleries|\\s*-\\s*ExHentai\\.org)?</title>").getMatch(0);
        return title;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    public static final boolean default_PREFER_ORIGINAL_QUALITY  = true;
    public static final boolean default_PREFER_ORIGINAL_FILENAME = false;
    public static final boolean default_ENABLE_DOWNLOAD_ZIP      = true;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_ORIGINAL_QUALITY, "Account only: Prefer original quality (bigger filesize, higher resolution, reaches limit faster)?").setDefaultValue(default_PREFER_ORIGINAL_QUALITY));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_ORIGINAL_FILENAME, "Prefer original file name?").setDefaultValue(default_PREFER_ORIGINAL_FILENAME));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_DOWNLOAD_ZIP, "Add .zip file containing all pictures of a gallery?").setDefaultValue(default_ENABLE_DOWNLOAD_ZIP));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
