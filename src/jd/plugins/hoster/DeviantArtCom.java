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
import java.net.URLEncoder;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/art/[\\w\\-]+|https?://[\\w\\.\\-]*?\\.deviantart\\.com/status/\\d+|https?://[\\w\\.\\-]*?deviantartdecrypted\\.com/journal/[\\w\\-]+" })
public class DeviantArtCom extends PluginForHost {
    private boolean             DOWNLOADS_STARTED                = false;
    private String              DLLINK                           = null;
    private final String        COOKIE_HOST                      = "http://www.deviantart.com";
    private static final String NICE_HOST                        = "deviantart.com";
    private static final String NICE_HOSTproperty                = "deviantartcom";
    private final String        INVALIDLINKS                     = "https?://(www\\.)?forum\\.deviantart\\.com/art/general";
    private final String        MATURECONTENTFILTER              = ">Mature Content Filter<";
    private static Object       LOCK                             = new Object();
    public static String        FASTLINKCHECK_2                  = "FASTLINKCHECK_2";
    public static String        FORCEHTMLDOWNLOAD                = "FORCEHTMLDOWNLOAD";
    public static String        CRAWL_GIVEN_OFFSETS_INDIVIDUALLY = "CRAWL_GIVEN_OFFSETS_INDIVIDUALLY";
    private static final String GENERALFILENAMEREGEX             = "<title>([^<>\"]*?) on deviantART</title>";
    private static final String DLLINK_REFRESH_NEEDED            = "https?://(www\\.)?deviantart\\.com/download/.+";
    private static final String TYPE_DOWNLOADALLOWED_GENERAL     = "\"label\">\\s*Download";
    private static final String TYPE_DOWNLOADALLOWED_HTML        = "class=\"text\">HTML download</span>";
    private static final String TYPE_DOWNLOADFORBIDDEN_HTML      = "<div class=\"grf\\-indent\"";
    private static final String TYPE_DOWNLOADFORBIDDEN_SWF       = "class=\"flashtime\"";
    private static final String TYPE_ACCOUNTNEEDED               = "has limited the viewing of this artwork<";
    private boolean             HTMLALLOWED                      = false;
    private static final String LINKTYPE_ART                     = "https?://[\\w\\.\\-]*?deviantart\\.com/art/[^<>\"/]+";
    private static final String LINKTYPE_JOURNAL                 = "https?://[\\w\\.\\-]*?deviantart\\.com/journal/[\\w\\-]+";
    private static final String LINKTYPE_STATUS                  = "https?://[\\w\\.\\-]*?\\.deviantart\\.com/status/\\d+";
    private static final String TYPE_BLOG_OFFLINE                = "https?://[\\w\\.\\-]*?deviantart\\.com/blog/.+";

    /**
     * @author raztoki
     */
    @SuppressWarnings("deprecation")
    public DeviantArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
        this.enablePremium(COOKIE_HOST.replace("http://", "https://") + "/join/");
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("deviantartdecrypted.com/", "deviantart.com/"));
    }

    @Override
    public void init() {
        super.init();
        Browser.setRequestIntervalLimitGlobal(getHost(), 1500);
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBR(this.br);
        if (link.getDownloadURL().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Workaround for a strange bug - DLLINK is not null at the beginning so if multiple links are to be checked they will all get the
        // same filenames
        DLLINK = null;
        br.setFollowRedirects(true);
        if (this.getPluginConfig().getBooleanProperty(FASTLINKCHECK_ALL, default_FASTLINKCHECK_ALL) && !DOWNLOADS_STARTED) {
            return AvailableStatus.UNCHECKABLE;
        }
        boolean loggedIn = false;
        final Account acc = AccountController.getInstance().getValidAccount(this);
        if (acc != null) {
            try {
                login(this.br, acc, false);
                loggedIn = true;
            } catch (final Exception e) {
            }
        }
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("/error\\-title\\-oops\\.png\\)") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Motionbooks are not supported (yet)
        if (br.containsHTML(",target: \\'motionbooks/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Redirects can lead to unsupported/offline links/linktypes */
        if (br.getURL().matches(TYPE_BLOG_OFFLINE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        // String filename_server = null;
        if (!getPluginConfig().getBooleanProperty(FilenameFromServer, false) && !link.getDownloadURL().matches(LINKTYPE_STATUS)) {
            filename = br.getRegex(GENERALFILENAMEREGEX).getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String fid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
            filename = Encoding.htmlDecode(filename.trim()) + "_" + fid;
        }
        if (link.getDownloadURL().matches(LINKTYPE_STATUS)) {
            filename = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename.trim());
        }
        String ext = null;
        String filesize = null;
        /* Check if either user wants to download the html code or if we have a linktype which needs this. */
        if (this.getPluginConfig().getBooleanProperty(FORCEHTMLDOWNLOAD, false) || link.getDownloadURL().matches(LINKTYPE_JOURNAL) || link.getDownloadURL().matches(LINKTYPE_STATUS)) {
            HTMLALLOWED = true;
            DLLINK = br.getURL();
            if (filename == null) { // Config FilenameFromServer is enabled
                filename = findServerFilename(filename); // Depends on DLLINK
            }
            ext = "html";
        } else if (br.containsHTML(TYPE_DOWNLOADALLOWED_GENERAL)) {
            final String ret[] = getDownloadURL();
            if (ret != null) {
                DLLINK = ret[0];
                ext = new Regex(ret[1], "span class=\"text\">([A-Za-z0-9]{1,5})\\s*download").getMatch(0);
                filesize = new Regex(ret[1], "span class=\"text\">([A-Za-z0-9]{1,5})\\s*download\\s*([0-9]+ [KM]B)").getMatch(1);
                if (ext == null) {
                    ext = new Regex(ret[1], ">Download</span>[\t\n\r ]+<span class=\"text\">([A-Za-z0-9]{1,5}),? ([^<>\"]*?)</span>").getMatch(0);
                }
            }
            /* Download for other extensions */
            // filesize = fInfo.getMatch(1);
            if (ext == null || DLLINK == null) {
                logger.info("ext: " + ext + ", DLLINK: " + DLLINK);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (filename == null) { // Config FilenameFromServer is enabled
                filename = findServerFilename(filename); // Depends on DLLINK
            }
        } else if (br.containsHTML(TYPE_DOWNLOADFORBIDDEN_SWF)) {
            filesize = getImageSize();
            String url_swf_sandbox = br.getRegex("class=\"flashtime\" src=\"(https?://sandbox\\.deviantart\\.com[^<>\"]*?)\"").getMatch(0);
            if (url_swf_sandbox == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Fix that ... */
            url_swf_sandbox = url_swf_sandbox.replace("?", "/?");
            br.getPage(url_swf_sandbox);
            this.DLLINK = br.getRegex("<param name=\"movie\" value=\"(https?://[^<>\"]*?\\.swf)\"").getMatch(0);
            if (this.DLLINK == null) {
                this.DLLINK = br.getRegex("id=\"sandboxembed\" src=\"(http[^<>\"]+\\.swf)\"").getMatch(0);
            }
            if (filename == null) { // Config FilenameFromServer is enabled
                filename = findServerFilename(filename); // Depends on DLLINK
            }
            ext = "swf";
        } else if (br.containsHTML(TYPE_DOWNLOADALLOWED_HTML)) {
            HTMLALLOWED = true;
            if (filename == null) { // Config FilenameFromServer is enabled
                filename = findServerFilename(filename); // Depends on DLLINK
            }
            ext = "html";
            filesize = getfileSize();
        } else if (br.containsHTML(TYPE_DOWNLOADFORBIDDEN_HTML)) {
            HTMLALLOWED = true;
            // Download whole html site
            DLLINK = br.getURL();
            if (filename == null) { // Config FilenameFromServer is enabled
                filename = findServerFilename(filename); // Depends on DLLINK
            }
            filesize = getfileSize();
            ext = "html";
            if (br.containsHTML(MATURECONTENTFILTER) && !loggedIn) {
                link.getLinkStatus().setStatusText("Mature content can only be downloaded via account");
                link.setName(filename + "." + ext);
                if (filesize != null) {
                    link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
                }
                return AvailableStatus.TRUE;
            }
        } else if (br.containsHTML(TYPE_ACCOUNTNEEDED)) {
            /* Account needed to view/download */
            if (filename == null) { // Config FilenameFromServer is enabled
                filename = findServerFilename(filename); // Depends on DLLINK
            }
            filesize = getfileSize();
            ext = "html";
        } else {
            filesize = getImageSize();
            // Maybe its a video
            if (filesize == null) {
                filesize = getfileSize();
            }
            if (br.containsHTML(MATURECONTENTFILTER) && !loggedIn) {
                link.getLinkStatus().setStatusText("Mature content can only be downloaded via account");
                link.setName(filename);
                if (filesize != null) {
                    link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
                }
                return AvailableStatus.TRUE;
            }
            ext = br.getRegex("<strong>Download Image</strong><br><small>([A-Za-z0-9]{1,5}),").getMatch(0);
            if (ext == null) {
                ext = new Regex(filename, "\\.([A-Za-z0-9]{1,5})$").getMatch(0);
            }
            /* Workaround for invalid domain(s) e.g. "laur-.deviantart.com" */
            final String cookie = this.br.getCookie(null, "userinfo");
            if (cookie != null) {
                br.setCookie(this.getHost(), "userinfo", cookie);
            }
            if (ext == null || ext.length() > 5) {
                final String dllink = getCrippledDllink();
                if (dllink != null) {
                    ext = getFileNameExtensionFromURL(dllink);
                }
            }
            if (ext == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (filename == null) { // Config FilenameFromServer is enabled
                filename = findServerFilename(filename); // Depends on DLLINK
            }
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        } else {
            final Browser br2 = br.cloneBrowser();
            /* Workaround for old downloadcore bug that can lead to incomplete files */
            br2.getHeaders().put("Accept-Encoding", "identity");
            URLConnectionAdapter con = null;
            final String dlLink = getDllink();
            if (dlLink != null) {
                try {
                    con = br2.openGetConnection(dlLink);
                    if (con.getContentType().contains("html") && !HTMLALLOWED) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        link.setDownloadSize(con.getLongContentLength());
                        // link.setContentUrl("dlLink"); // Gives Oops, 404 not found after some time.
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        }
        ext = ext.toLowerCase().trim();
        if (!filename.endsWith(ext)) {
            if (ext.startsWith(".")) {
                // getFileNameExtensionFromURL
                filename += ext;
            } else {
                filename += "." + ext;
            }
        }
        filename = filename.replaceAll(" +", " ");
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    private String[] getDownloadURL() {
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

    private String getfileSize() {
        String filesize = br.getRegex("<label>File Size:</label>([^<>\"]*?)<br/>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<dt>File Size</dt><dd>([^<>\"]*?)</dd>").getMatch(0);
        }
        return filesize;
    }

    private String getImageSize() {
        String imagesize = br.getRegex("<label>Image Size:</label>([^<>\"]*?)<br>").getMatch(0);
        if (imagesize == null) {
            imagesize = br.getRegex("<dt>Image Size</dt><dd>([^<>\"]*?)</dd>").getMatch(0);
        }
        return imagesize;
    }

    private String getCrippledDllink() {
        String crippleddllink = null;
        try {
            final String linkWithExt = getDllink();
            final String toRemove = new Regex(linkWithExt, "(\\?token=.+)").getMatch(0);
            if (toRemove != null) {
                crippleddllink = linkWithExt.replace(toRemove, "");
            } else {
                crippleddllink = linkWithExt;
            }
        } catch (final Exception e) {
        }
        return crippleddllink;
    }

    private String findServerFilename(final String oldfilename) {
        // Try to get server filename, if not possible, return old one
        String newfilename = null;
        final String dllink = getCrippledDllink();
        if (dllink != null) {
            newfilename = new Regex(dllink, "/([^<>\"/]+)$").getMatch(0);
        } else {
            newfilename = oldfilename;
        }
        return newfilename;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        DOWNLOADS_STARTED = true;
        requestFileInformation(downloadLink);
        if (br.containsHTML(TYPE_ACCOUNTNEEDED) || br.containsHTML(MATURECONTENTFILTER)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        final String dlLink = getDllink();
        if (dlLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Workaround for old downloadcore bug that can lead to incomplete files */
        /* Disable chunks as we only download pictures or small files */
        br.getHeaders().put("Accept-Encoding", "identity");
        boolean resume = true;
        if (HTMLALLOWED) {
            downloadLink.setVerifiedFileSize(-1);
            resume = false;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlLink, resume, 1);
        if (dl.getConnection().getContentType().contains("html") && !HTMLALLOWED) {
            handleServerErrors(downloadLink);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        DOWNLOADS_STARTED = true;
        /* This will also log in */
        requestFileInformation(downloadLink);
        final String dlLink = getDllink();
        if (dlLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Workaround for old downloadcore bug that can lead to incomplete files */
        br.getHeaders().put("Accept-Encoding", "identity");
        /* Disable chunks as we only download pictures */
        boolean resume = true;
        if (HTMLALLOWED) {
            downloadLink.setVerifiedFileSize(-1);
            resume = false;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlLink, resume, 1);
        if (dl.getConnection().getContentType().contains("html") && !HTMLALLOWED) {
            handleServerErrors(downloadLink);
            br.followConnection();
            if (br.containsHTML("><title>Redirection</title>")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: unknown redirect", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleServerErrors(final DownloadLink dlink) throws PluginException {
        /* Happens sometimes - download should work fine later though. */
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 10 * 60 * 1000l);
        }
        if (dl.getConnection().getResponseCode() == 404) {
            logger.info(NICE_HOST + ": 404servererror");
            int timesFailed = dlink.getIntegerProperty(NICE_HOSTproperty + "timesfailed_404servererror", 0);
            dlink.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                dlink.setProperty(NICE_HOSTproperty + "timesfailed_404servererror", timesFailed);
                logger.info(NICE_HOST + ": timesfailed_404servererror -> Retrying in 30 minutes");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
            } else {
                dlink.setProperty(NICE_HOSTproperty + "timesfailed_404servererror", Property.NULL);
                logger.info(NICE_HOST + ": timesfailed_404servererror - Download must be broken!");
                throw new PluginException(LinkStatus.ERROR_FATAL, "Download broken (deviantart server issue 404)!");
            }
        }
    }

    private String getDllink() throws PluginException {
        if (DLLINK == null) {
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
                DLLINK = bestURL;
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
                DLLINK = dllink;
            }
        }
        return DLLINK;
    }

    private String getHQpic() {
        final String hqurl = br.getRegex("class=\"dev\\-content\\-normal[^\"]*?\">[\t\n\r ]+<img collect_rid=\"[0-9:]+\" src=\"(https?://[^<>\"]*?)\"").getMatch(0);
        return hqurl;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setStatus("Free Registered User");
        return ai;
    }

    private boolean isCookieSet(Browser br, String key) {
        final String value = br.getCookie(getHost(), key);
        return StringUtils.isNotEmpty(value) && !StringUtils.equalsIgnoreCase(value, "deleted");
    }

    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean requestMain = true;
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    br.getPage("https://www.deviantart.com/");
                    requestMain = false;
                    if (!isCookieSet(br, "userinfo") || !isCookieSet(br, "auth") || !isCookieSet(br, "auth_secure")) {
                        br.clearCookies(getHost());
                    } else {
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    }
                }
                if (requestMain) {
                    br.getPage("https://www.deviantart.com/");
                }
                br.getPage("https://www.deviantart.com/users/login"); // Not allowed to go directly to /users/login/
                if (br.containsHTML("Please confirm you are human")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                if (false && (br.containsHTML("Please confirm you are human") || (br.containsHTML("px-blocked") && br.containsHTML("g-recaptcha")))) {
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
                final Form loginform = br.getFormbyKey("username");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember_me", "1");
                br.submitForm(loginform);
                if (!isCookieSet(br, "userinfo") || !isCookieSet(br, "auth") || !isCookieSet(br, "auth_secure") || br.getFormbyKey("username") != null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == PluginException.VALUE_ID_PREMIUM_DISABLE) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    public static void prepBR(final Browser br) {
        /* Needed to view mature content */
        br.setCookie("deviantart.com", "agegate_state", "1");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0");
    }

    @Override
    public String getDescription() {
        return "JDownloader's Deviantart Plugin helps downloading data from deviantart.com.";
    }

    private static final boolean default_FASTLINKCHECK_2                  = true;
    private static final boolean default_FASTLINKCHECK_ALL                = false;
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
            fastlinkchecktext = "Schnelle Linküberprüfung aktivieren? (Dateiname und -größe werden nicht korrekt angezeigt)";
            fastlinkcheck_all_text = "Schnelle Linküberprüfung für ALLE Links aktivieren?\r\nBedenke, dass der online-status bis zum Downloadstart nicht aussagekräftig ist!";
            forcehtmldownloadtext = "HTML Code statt dem eigentlichen Inhalt (Dateien/Bilder) laden?";
            decryptOffsetsIndividually = "Bei gegebenem 'offset=XX' im Link nur dieses Crawlen, statt ab diesem bis zum Ende zu crawlen?";
            sbinfo.append("Bitte beachten: solltest Du nur Seite 1 einer Gallerie sammeln wollen, so stelle sicher, dass \"?offset=0\" am Ende der URL steht.\r\n");
            sbinfo.append("Du kannst auch zu einer anderen Seite wechseln, auf Seite 1 klicken und deren URL einfügen.");
        } else {
            fastlinkchecktext = "Enable fast link check? (file name and size won't be shown correctly until downloadstart)";
            fastlinkcheck_all_text = "Enable fast linkcheck for ALL links?\r\nNote that this means that you can't see the real online/offline status until the download is started!";
            forcehtmldownloadtext = "Download html code instead of the media (files/pictures)?";
            decryptOffsetsIndividually = "On given 'offset=XX', crawl only this offset instead of crawling from this offset until the end?";
            sbinfo.append("Please note: if you wanted to grab only page 1 of a gallery, please make sure that \"?offset=0\" is added to its URL.\r\n");
            sbinfo.append("You can also switch to another page, click on page 1 and grab its URL.");
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK_2, fastlinkchecktext).setDefaultValue(default_FASTLINKCHECK_2));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK_ALL, fastlinkcheck_all_text).setDefaultValue(default_FASTLINKCHECK_ALL));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "FilenameFromServer", "Choose file name from download link with unique identifier?").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FORCEHTMLDOWNLOAD, forcehtmldownloadtext).setDefaultValue(default_FORCEHTMLDOWNLOAD));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CRAWL_GIVEN_OFFSETS_INDIVIDUALLY, decryptOffsetsIndividually).setDefaultValue(default_CRAWL_GIVEN_OFFSETS_INDIVIDUALLY));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbinfo.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}