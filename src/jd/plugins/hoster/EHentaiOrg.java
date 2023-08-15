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
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
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
        prepBR(br, null);
        return br;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    private Browser prepBR(final Browser br, final DownloadLink link) {
        br.setReadTimeout(3 * 60 * 1000);
        // br.setConnectTimeout(3 * 60 * 1000);
        /* TODO: 2020-12-14: What does this do? */
        if (link != null) {
            br.setCookie(Browser.getHost(link.getPluginPatternMatcher()), "nw", "1");
        }
        return br;
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Limit chunks to 1 as we only download small files */
    private static final int    free_maxchunks                    = 1;
    private static final int    free_maxdownloads                 = -1;
    private String              dllink                            = null;
    private final boolean       ENABLE_RANDOM_UA                  = true;
    public static final String  PREFER_ORIGINAL_QUALITY           = "PREFER_ORIGINAL_QUALITY";
    public static final String  PREFER_ORIGINAL_FILENAME          = "PREFER_ORIGINAL_FILENAME";
    public static final String  SETTING_DOWNLOAD_ZIP              = "DOWNLOAD_ZIP";
    private static final String TYPE_EXHENTAI                     = "exhentai\\.org";
    private static final String TYPE_ARCHIVE                      = "ehentaiarchive://\\d+/[a-z0-9]+";
    private static final String TYPE_SINGLE_IMAGE_MULTI_PAGE_VIEW = "https://[^/]+/mpv/(\\d+)/([a-f0-9]{10})/#page(\\d+)";
    private static final String TYPE_SINGLE_IMAGE                 = "https?://[^/]+/s/([a-f0-9]{10})/(\\d+)-(\\d+)";
    public static final String  PROPERTY_GALLERY_URL              = "gallery_url";
    private final String        PROPERTY_DIRECTURL                = "directurl";

    @Override
    public String getAGBLink() {
        return "http://g.e-hentai.org/tos.php";
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
            return super.canHandle(link, account);
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
        } else if (ENABLE_RANDOM_UA) {
            /* Be sure only to use random UA when an account is not used! */
            /*
             * Using a different UA for every download might be a bit obvious but at the moment, this fixed the error-server responses as it
             * tricks it into thinking that we re a lot of users and not only one.
             */
            br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        }
        final String directurlproperty = getDirecturlproperty(account);
        prepBR(br, link);
        final boolean preferOriginalQuality = this.getPluginConfig().getBooleanProperty(PREFER_ORIGINAL_QUALITY, default_PREFER_ORIGINAL_QUALITY);
        /* from manual 'online check', we don't want to 'try' as it uses up quota... */
        if (new Regex(link.getPluginPatternMatcher(), TYPE_ARCHIVE).matches()) {
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
            br.getPage("https://" + host + "/g/" + galleryid + "/" + galleryhash);
            if (isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (isDownload) {
                /*
                 * 2022-01-10: Depending on account settings, some galleries won't be displayed by default for some users. They have to
                 * click on a "View anyways" button to continue.
                 */
                final String skipContentWarningURL = br.getRegex("\"(https?://[^/]+/g/\\d+/[a-f0-9]+/\\?nw=session)\"[^>]*>\\s*View Gallery\\s*<").getMatch(0);
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
                    dllink = br.getRegex("(?i)href=\"([^\"]+)\"[^>]*>Click Here To Start Downloading").getMatch(0);
                }
                if (dllink == null && br.containsHTML("name=\"dlcheck\"[^<>]*value=\"Insufficient Funds\"")) {
                    /* 2020-05-20: E.g. not enough credits for archive downloads but enough to download single images. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Insufficient funds for downloading archives");
                }
            }
            return AvailableStatus.TRUE;
        } else if (new Regex(link.getPluginPatternMatcher(), TYPE_SINGLE_IMAGE_MULTI_PAGE_VIEW).matches()) {
            /* 2020-05-21: New linktype "Multi Page View" */
            br.setFollowRedirects(true);
            final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), TYPE_SINGLE_IMAGE_MULTI_PAGE_VIEW);
            final String galleryid = urlinfo.getMatch(0);
            final String page = urlinfo.getMatch(2);
            /*
             * 2020-05-21: TODO: Check if this ever expires - it would be easy to renew this but would require additional http requests
             * which slows down the process of downloading a lot of small files significantly!
             */
            final String mpvkey = link.getStringProperty("mpvkey", null);
            final String imagekey = link.getStringProperty("imagekey", null);
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
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
            final String filesizeStr;
            final String lowResInfo = (String) entries.get("d");
            final String origInfo = (String) entries.get("o");
            /* 2020-05-21: Only registered users can download originals! */
            boolean downloadRequiresPoints = false;
            if (account != null && preferOriginalQuality) {
                /* Download original file */
                filesizeStr = new Regex(origInfo, "(\\d+\\.\\d{1,2} [A-Za-z]+)").getMatch(0);
                this.dllink = (String) entries.get("lf");
                if (!this.dllink.startsWith("http") && !this.dllink.startsWith("/")) {
                    this.dllink = "https://" + host + "/" + this.dllink;
                }
                downloadRequiresPoints = true;
            } else {
                /* Download "lower quality" file */
                filesizeStr = new Regex(lowResInfo, "(\\d+\\.\\d{1,2} [A-Za-z]+)").getMatch(0);
                this.dllink = (String) entries.get("i");
            }
            /* Only perform linkcheck if filesize is not given as text! */
            if (filesizeStr != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
            }
            if (filesizeStr == null && !downloadRequiresPoints) {
                URLConnectionAdapter con = null;
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(true);
                    con = br.openHeadConnection(dllink);
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
            /* TYPE_SINGLE_IMAGE */
            if (this.requiresAccount(link) && account == null) {
                throw new AccountRequiredException();
            }
            /* TYPE_SINGLE_IMAGE e-hentai.org and exhentai.org */
            String dllinkOriginalFile = null;
            br.setFollowRedirects(true);
            br.getPage(link.getPluginPatternMatcher());
            final String urlpart = new Regex(link.getPluginPatternMatcher(), TYPE_SINGLE_IMAGE).getMatch(0);
            /*
             * 2020-12-01: Workaround attempt: Some users randomly always get the "cookie redirect" for exhentai.org which should only
             * happen when accessing it for the first time. It redirects them to the main page.
             */
            if (link.getPluginPatternMatcher().contains("exhentai") && !this.canHandle(br.getURL()) && !br.getURL().contains(urlpart)) {
                logger.info("Redirect to mainpage? Accessing gallery URL again ...");
                br.getPage(link.getPluginPatternMatcher());
                if (!this.canHandle(br.getURL()) && !br.getURL().contains(urlpart)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Redirect to mainpage? Login failure?", 3 * 60 * 1000l);
                }
            }
            if (br.toString().length() <= 100) {
                /* 2020-05-23: Empty page: Most likely exhentai.org URL with account that does not have permissions to access it. */
                throw new AccountRequiredException();
            } else if (br.getRequest().getHtmlCode().matches("Your IP address has been temporarily banned for excessive pageloads.+")) {
                if (account == null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Your IP address has been temporarily banned for excessive pageloads");
                }
                String tmpYears = new Regex(br, "(\\d+)\\s+years?").getMatch(0);
                String tmpdays = new Regex(br, "(\\d+)\\s+days?").getMatch(0);
                String tmphrs = new Regex(br, "(\\d+)\\s+hours?").getMatch(0);
                String tmpmin = new Regex(br, "(\\d+)\\s+minutes?").getMatch(0);
                String tmpsec = new Regex(br, "(\\d+)\\s+seconds?").getMatch(0);
                long years = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
                if (StringUtils.isEmpty(tmpYears)) {
                    years = Integer.parseInt(tmpYears);
                }
                if (StringUtils.isEmpty(tmpdays)) {
                    days = Integer.parseInt(tmpdays);
                }
                if (StringUtils.isEmpty(tmphrs)) {
                    hours = Integer.parseInt(tmphrs);
                }
                if (StringUtils.isEmpty(tmpmin)) {
                    minutes = Integer.parseInt(tmpmin);
                }
                if (StringUtils.isEmpty(tmpsec)) {
                    seconds = Integer.parseInt(tmpsec);
                }
                long expireS = ((years * 86400000 * 365) + (days * 86400000) + (hours * 3600000) + (minutes * 60000) + (seconds * 1000)) + System.currentTimeMillis();
                throw new AccountUnavailableException("Your IP address has been temporarily banned for excessive pageloads", expireS);
            } else if (isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filesizeStrOriginalImage = null;
            if (account != null && this.getPluginConfig().getBooleanProperty(PREFER_ORIGINAL_QUALITY, default_PREFER_ORIGINAL_QUALITY)) {
                /* Try to get fullsize (original) image. */
                final Regex fulllinkinfo = br.getRegex("href=\"(https?://(?:(?:g\\.)?e\\-hentai|exhentai)\\.org/fullimg\\.php[^<>\"]*?)\">Download original \\d+ x \\d+ ([^<>\"]*?) source</a>");
                dllinkOriginalFile = fulllinkinfo.getMatch(0);
                filesizeStrOriginalImage = fulllinkinfo.getMatch(1);
            }
            String ext = null;
            String originalFileName = br.getRegex("(?i)<div>([^<>]*\\.(jpe?g|png|gif))\\s*::\\s*\\d+").getMatch(0);
            final String extDefault = ".jpg";
            if (originalFileName != null) {
                originalFileName = Encoding.htmlDecode(originalFileName).trim();
                ext = getFileNameExtensionFromString(originalFileName, extDefault);
            } else if (dllink != null) {
                ext = Plugin.getFileNameExtensionFromURL(dllink, extDefault);
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
                    // decrypter doesn't set file extension.
                    link.setFinalFileName(namepart + ext);
                }
            }
            if (dllinkOriginalFile != null) {
                dllinkOriginalFile = Encoding.htmlDecode(dllinkOriginalFile);
                /* Filesize is already set via html_filesize, we have our full (original) resolution downloadlink and our file extension! */
                dllink = dllinkOriginalFile;
                if (filesizeStrOriginalImage != null) {
                    link.setDownloadSize(SizeFormatter.getSize(filesizeStrOriginalImage));
                }
            } else {
                /* 2020-03-06: Sometimes needed for exhentai URLs but not that important. */
                this.dllink = getNormalmageDownloadurl(link, account, isDownload);
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
        return br.getHttpConnection().getResponseCode() == 404;
    }

    /** Returns direct downloadable URL to normal image (not original image). */
    private String getNormalmageDownloadurl(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        // g.e-hentai.org = free non account
        // error
        // <div id="i3"><a onclick="return load_image(94, '00ea7fd4e0')" href="http://g.e-hentai.org/s/00ea7fd4e0/348501-94"><img id="img"
        // src="http://ehgt.org/g/509.gif" style="margin:20px auto" /></a></div>
        // working
        // <div id="i3"><a onclick="return load_image(94, '00ea7fd4e0')" href="http://g.e-hentai.org/s/00ea7fd4e0/348501-94"><img id="img"
        // src="http://153.149.98.104:65000/h/40e8a3da0fac1b0ec40b5c58489f7b8d46b1a2a2-436260-1200-1600-jpg/keystamp=1469074200-e1ec68e0ef/093.jpg"
        // style="height:1600px;width:1200px" /></a></div>
        // error (no div id=i3, no a onclick either...) Link; 0957971887641.log; 57438449; jdlog://0957971887641
        // <a href="http://g.e-hentai.org/s/4bf901e9e6/957224-513"><img src="http://ehgt.org/g/509.gif" style="margin:20px auto" /></a>
        // working
        // ...
        // exhentai.org = account
        // error
        // <div id="i3"><a onclick="return load_image(26, '2fb043446a')" href="http://exhentai.org/s/2fb043446a/706165-26"><img id="img"
        // src="http://exhentai.org/img/509.gif" style="margin:20px auto" /></a></div>
        // working
        // <div id="i3"><a onclick="return load_image(54, 'cd7295ee9c')" href="http://exhentai.org/s/cd7295ee9c/940613-54"><img id="img"
        // src="http://130.234.205.178:25565/h/f21818f4e9d04169de22f31407df68da84f30719-935516-1273-1800-jpg/keystamp=1468656900-b9873b14ab/ow_013.jpg"
        // style="height:1800px;width:1273px" /></a></div>
        // best solution is to apply cleanup?
        /* 2020-03-05: I've created this workaround but it is not required anymore --> Just keep counter_max set to 0 then it'll be fine! */
        boolean looksLikeLimitReached = false;
        int counter = 0;
        int counter_max = 2;
        /* URL to current image */
        final String targetURL = br.getURL();
        String dllink = null;
        do {
            counter++;
            logger.info(String.format("Getdllink attempt %d / %d", counter, counter_max));
            if (looksLikeLimitReached) {
                this.sleep(3000l, link);
                /*
                 * script we require!
                 */
                // final Browser brc = br.cloneBrowser();
                br.setFollowRedirects(true);
                br.getPage(MAINPAGE_ehentai + "/home.php");// before, debugging
                br.getPage(MAINPAGE_ehentai + "/hathperks.php");
                logger.info("Credits before:");
                int[] creditsLeftInfo = getCreditsLeftInfo(br); // prints credits left (logs them)
                /*
                 * 2021-01-15: In browser a re-login (using the still existing e-hentai cookies) worked fine and removed that limit but it
                 * didn't help here.
                 */
                // /* Enforce to get new exhentai cookies */
                // br.clearCookies(MAINPAGE_exhentai);
                // this.getPage(br, MAINPAGE_exhentai);
                // if (!this.isLoggedInExhentai(br)) {
                // throw new AccountUnavailableException("Exhentai login failure", 5 * 60 * 1000);
                // }
                // getPage(br, MAINPAGE_ehentai + "/uiconfig.php");
                br.getPage(MAINPAGE_ehentai + "/home.php");
                logger.info("Credits AFTER:");
                creditsLeftInfo = getCreditsLeftInfo(br);
                if (creditsLeftInfo != null && creditsLeftInfo[0] >= creditsLeftInfo[1]) {
                    logger.info("Confirmed limit reached according to remaining credits");
                    exceptionLimitReached(account);
                }
                br.getPage(targetURL);
            }
            final String html = br.getRequest().getHtmlCode();
            String cleanup = new Regex(html, "<iframe[^>]*>(.*?)<iframe").getMatch(0);
            if (cleanup == null) {
                cleanup = new Regex(html, "<div id=\"i3\">(.*?)</div").getMatch(0);
            }
            dllink = new Regex(cleanup, "<img [^>]*src=(\"|\\')([^\"\\'<>]+)\\1").getMatch(1);
            if (dllink == null) {
                /* 2017-01-30: Until now only jp(e)g was allowed, now also png. */
                dllink = new Regex(html, "(?i)<img [^>]*src=(\"|')([^\"\\'<>]{30,}(?:\\.jpe?g|png|gif))\\1").getMatch(1);
            }
            if (dllink == null) {
                logger.info("Failed to find final downloadurl");
                break;
            }
            /* E.g. https://ehgt.org/g/509.gif */
            if (StringUtils.contains(dllink, "509.gif")) {
                looksLikeLimitReached = true;
            } else {
                looksLikeLimitReached = false;
                break;
            }
            if (!isDownload) {
                /* This function has been called during linkcheck -> We don't want to waste time here. */
                break;
            }
        } while (looksLikeLimitReached && counter < counter_max);
        if (looksLikeLimitReached) {
            logger.info("Failed to get around limit - limit is definitely reached!");
            exceptionLimitReached(account);
        }
        return dllink;
    }

    private void maybeLoginFailure(final Account account) throws PluginException {
        if (account != null) {
            throw new AccountUnavailableException("Unexpected logout happened?", 5 * 60 * 1000);
        } else {
            throw new AccountRequiredException();
        }
    }

    private void exceptionLimitReached(final Account account) throws PluginException {
        if (account == null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000);
        } else {
            /* 2020-03-03: This should not be required anymore --> Lead to timeouts --> No idea what it was good for */
            // br.getPage("http://exhentai.org/home.php");
            // account.saveCookies(br.getCookies(MAINPAGE), "");
            throw new AccountUnavailableException("Downloadlimit reached", 5 * 60 * 1000);
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
                logger.warning("Failed to find final downloadurl");
                this.handleErrorsLastResort(link, account, this.br);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), free_maxchunks);
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
                final String errorNotEnoughGP = "Downloading original files during peak hours requires GP, and you do not have enough.";
                final String errorNotEnoughGP2 = "Downloading original files of this gallery requires GP, and you do not have enough.";
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else if (br.containsHTML("﻿(?i)You have exceeded your image viewing limits")) {
                    exceptionLimitReached(account);
                } else if (br.getURL().contains("bounce_login.php")) {
                    /* Account required / re-login required */
                    if (account != null) {
                        throw new AccountUnavailableException("Account / Re-login required", 1 * 60 * 1000l);
                    } else {
                        /* This should never happen */
                        throw new AccountRequiredException();
                    }
                } else if (br.containsHTML(Pattern.quote(errorNotEnoughGP))) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorNotEnoughGP, 10 * 60 * 1000l);
                } else if (br.containsHTML(Pattern.quote(errorNotEnoughGP2))) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorNotEnoughGP2, 10 * 60 * 1000l);
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
                maybeLoginFailure(account);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directurlproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired");
            } else {
                throw e;
            }
        }
        link.setProperty(getDirecturlproperty(account), dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private static final String MAINPAGE_ehentai  = "https://e-hentai.org";
    private static final String MAINPAGE_exhentai = "https://exhentai.org";

    /** 2019-11-26: Alternative way to login: https://e-hentai.org/bounce_login.php?b=d&bt=1-1 */
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            /* About 'hathperks.php': */
            /*
             * 2020-03-04: About 'hathperks.php': Workaround for serverside bug: Without doing this, accounts with higher credit limits per
             * day (usually >5000), all accounts can be stuck with the daily 5000 limit. Accessing this page first and then '/home.php'
             * fixes this. Accessing hathperks will set two additional cookies: 'sk' and 'hath_perks'
             */
            final boolean followRedirects = br.isFollowingRedirects();
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies != null) {
                    br.setCookies(MAINPAGE_ehentai, userCookies);
                    if (!force) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    if (verifyCookies(account, true)) {
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
                    br.setCookies(MAINPAGE_ehentai, cookies);
                    final Cookies cookies2 = account.loadCookies("exhentai");
                    if (cookies2 != null) {
                        br.setCookies(MAINPAGE_exhentai, cookies2);
                    }
                    if (!force) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    if (verifyCookies(account, false)) {
                        /* Success */
                        return;
                    }
                }
                boolean failed = true;
                /* Login page with params to redirect to /home.php */
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
                    failed = !isLoggedInEhentai(br);
                    if (!failed) {
                        logger.info("Stepping out of login loop");
                        break;
                    }
                }
                if (failed) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* This will set two more important cookies! */
                br.getPage(MAINPAGE_ehentai + "/hathperks.php");
                account.saveCookies(br.getCookies(MAINPAGE_ehentai), "");
                /*
                 * Important! Get- and save exhentai cookies: First time this will happen: exhentai.org ->
                 * forums.e-hentai.org/remoteapi.php?ex= -> exhentai.org/?poni= -> exhentai.org
                 */
                br.getPage(MAINPAGE_exhentai);
                if (this.isLoggedInEhentaiOrExhentai(br)) {
                    logger.info("Successfully logged in exhentai -> Saving cookies");
                    account.saveCookies(br.getCookies(MAINPAGE_exhentai), "exhentai");
                } else {
                    logger.info("Failed to login in exhentai -> Ignoring cookies");
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                    account.clearCookies("exhentai");
                }
                throw e;
            } finally {
                br.setFollowRedirects(followRedirects);
            }
        }
    }

    /** Sets given cookies and checks if we can login with them. */
    protected boolean verifyCookies(final Account account, final boolean isUserCookies) throws Exception {
        // getPage(br, MAINPAGE_ehentai + "/index.php?");
        br.getPage(MAINPAGE_ehentai + "/hathperks.php");
        if (this.isLoggedInEhentai(br)) {
            br.getPage(MAINPAGE_ehentai + "/home.php");
            if (this.isLoggedInEhentai(br)) {
                final String items_downloadedStr = br.getRegex("You are currently at <strong>(\\d+)</strong>").getMatch(0);
                final String items_maxStr = br.getRegex("towards a limit of <strong>(\\d+)</strong>").getMatch(0);
                logger.info("Successfully logged in via cookies:" + items_downloadedStr + "/" + items_maxStr);
                if (!isUserCookies) {
                    account.saveCookies(br.getCookies(MAINPAGE_ehentai), "");
                }
                /* Get- and save exhentai cookies too */
                br.getPage(MAINPAGE_exhentai);
                if (this.isLoggedInEhentaiOrExhentai(br)) {
                    logger.info("Successfully logged in exhentai -> Saving cookies");
                    if (!isUserCookies) {
                        account.saveCookies(br.getCookies(MAINPAGE_exhentai), "exhentai");
                    }
                } else {
                    logger.info("Failed to login in exhentai -> Ignoring cookies");
                }
                return true;
            }
        }
        logger.info("Failed to login via cookies");
        br.clearAll();
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
        final int[] creditsLeftInfo = this.getCreditsLeftInfo(br);
        if (creditsLeftInfo != null) {
            ai.setStatus(String.format(AccountType.FREE.getLabel() + " [Used %d / %d items]", creditsLeftInfo[0], creditsLeftInfo[1]));
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                /* For development purposes: Set fake trafficlimit so developer can see the limit visually. */
                final long dummyTrafficUsed = SizeFormatter.getSize(creditsLeftInfo[0] + "TiB");
                final long dummyTrafficMax = SizeFormatter.getSize(creditsLeftInfo[1] + "TiB");
                ai.setTrafficLeft(dummyTrafficMax - dummyTrafficUsed);
                ai.setTrafficMax(dummyTrafficMax);
            }
        } else {
            logger.warning("Failed to find items_downloadedStr or items_maxStr:" + creditsLeftInfo);
            ai.setStatus(AccountType.FREE.getLabel() + " Failed to find credits left info");
            ai.setUnlimitedTraffic();
        }
        if (creditsLeftInfo != null && creditsLeftInfo[0] >= creditsLeftInfo[1]) {
            logger.info("Account does not have any credits left --> Set remaining traffic to 0");
            ai.setTrafficLeft(0);
        }
        /* 2020-11-30: Experimental */
        account.setRefreshTimeout(10 * 60 * 1000l);
        return ai;
    }

    /**
     * Access e-hentai.org/home.php before calling this! </br>
     * Returns array of numbers with: </br>
     * [0] = number of items downloaded / used from limit </br>
     * [1] = max limit for this account </br>
     * [1] minus [0] = points left
     */
    private int[] getCreditsLeftInfo(final Browser br) {
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
