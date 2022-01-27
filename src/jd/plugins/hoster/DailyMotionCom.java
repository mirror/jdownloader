//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.DailyMotionComDecrypter;
import jd.plugins.download.DownloadInterface;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dailymotion.com" }, urls = { "https?://dailymotiondecrypted\\.com/video/\\w+" })
public class DailyMotionCom extends PluginForHost {
    public String getVideosource(final Browser br, final String videoID) throws Exception {
        return jd.plugins.decrypter.DailyMotionComDecrypter.getVideosource(this, br, videoID);
    }

    public static LinkedHashMap<String, String[]> findVideoQualities(final Plugin plugin, final Browser br, final String parameter, String videosource) throws Exception {
        return jd.plugins.decrypter.DailyMotionComDecrypter.findVideoQualities(plugin, br, parameter, videosource);
    }

    public String                dllink                 = null;
    private static final String  REGISTEREDONLYUSERTEXT = "Download only possible for registered users";
    private static final String  COUNTRYBLOCKUSERTEXT   = "This video is not available for your country";
    /** Settings stuff */
    public static final String   ALLOW_SUBTITLE         = "ALLOW_SUBTITLE";
    private static final String  ALLOW_BEST             = "ALLOW_BEST";
    public static final String   ALLOW_HLS              = "ALLOW_HLS_2019_01_18";
    public static final String   ALLOW_MP4              = "ALLOW_MP4_2019_01_18";
    private static final String  ALLOW_144              = "ALLOW_0";
    private static final String  ALLOW_240              = "ALLOW_1";
    private static final String  ALLOW_380              = "ALLOW_2";
    private static final String  ALLOW_480              = "ALLOW_3";
    private static final String  ALLOW_720              = "ALLOW_4";
    private static final String  ALLOW_1080             = "ALLOW_5";
    private static final String  ALLOW_1440             = "ALLOW_6";
    private static final String  ALLOW_2160             = "ALLOW_7";
    private static final String  ALLOW_OTHERS           = "ALLOW_OTHERS";
    private static final String  ALLOW_AUDIO            = "ALLOW_AUDIO";
    private static final String  CUSTOM_DATE            = "CUSTOM_DATE";
    private static final String  CUSTOM_FILENAME        = "CUSTOM_FILENAME";
    private final static String  defaultCustomFilename  = "*videoname*_*quality**ext*";
    private final static String  defaultCustomDate      = "dd.MM.yyyy";
    public final static boolean  default_ALLOW_SUBTITLE = true;
    private final static boolean defaultAllowAudio      = true;
    public static final boolean  default_ALLOW_HLS      = true;
    public static final boolean  default_ALLOW_MP4      = false;

    public DailyMotionCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.dailymotion.com/register");
        setConfigElements();
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("dailymotiondecrypted.com/", "dailymotion.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        br.setFollowRedirects(true);
        br.setCookie("http://www.dailymotion.com", "family_filter", "off");
        br.setCookie("http://www.dailymotion.com", "ff", "off");
        br.setCookie("http://www.dailymotion.com", "lang", "en_US");
        prepBrowser(this.br);
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (downloadLink.getBooleanProperty("countryblock", false)) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.dailymotioncom.countryblocked", COUNTRYBLOCKUSERTEXT));
            return AvailableStatus.TRUE;
        } else if (downloadLink.getBooleanProperty("registeredonly", false)) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.dailymotioncom.registeredonly", REGISTEREDONLYUSERTEXT));
            return AvailableStatus.TRUE;
        }
        if (isHDS(downloadLink)) {
            // br.getPage(downloadLink.getStringProperty("directlink", null));
            downloadLink.getLinkStatus().setStatusText("HDS stream download is not supported (yet)!");
            downloadLink.setFinalFileName(getFormattedFilename(downloadLink));
            return AvailableStatus.FALSE;
        } else if (isHLS(downloadLink)) {
            /* Make sure to follow redirects! */
            this.br.setFollowRedirects(true);
            final String contentURL = downloadLink.getContentUrl();
            final String videoURL;
            if (contentURL != null) {
                videoURL = contentURL;
            } else {
                videoURL = downloadLink.getStringProperty("mainlink", null);
            }
            if (videoURL != null) {
                br.getPage(videoURL);
            }
            final String videoSource = DailyMotionComDecrypter.getVideosource(this, this.br, getVideoID(downloadLink));
            if (videoSource != null) {
                final LinkedHashMap<String, String[]> foundQualities = DailyMotionComDecrypter.findVideoQualities(this, this.br, videoURL, videoSource);
                final String qualityValue = downloadLink.getStringProperty("qualityvalue", null);
                if (foundQualities != null && foundQualities.containsKey(qualityValue)) {
                    downloadLink.setProperty("directlink", Encoding.htmlDecode(foundQualities.get(qualityValue)[0]));
                }
                final String dllink = getDirectlink(downloadLink);
                final Browser brc = br.cloneBrowser();
                brc.getPage(dllink);
                if (brc.getHttpConnection().isOK()) {
                    final List<HlsContainer> hlsBest;
                    try {
                        hlsBest = HlsContainer.findBestVideosByBandwidth(HlsContainer.getHlsQualities(brc));
                    } catch (final Exception e) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
                    }
                    if (hlsBest == null || hlsBest.size() == 0) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        checkFFProbe(downloadLink, "File Checking a HLS Stream");
                        Exception caughtE = null;
                        for (final HlsContainer hlsContainer : hlsBest) {
                            try {
                                final String hlsURL = hlsContainer.getStreamURL();
                                final HLSDownloader downloader = new HLSDownloader(downloadLink, brc, hlsURL);
                                final StreamInfo streamInfo = downloader.getProbe();
                                if (downloadLink.getBooleanProperty("encrypted")) {
                                    throw new PluginException(LinkStatus.ERROR_FATAL, "Encrypted HLS is not supported");
                                }
                                if (streamInfo != null) {
                                    final long estimatedSize = downloader.getEstimatedSize();
                                    if (downloadLink.getKnownDownloadSize() == -1) {
                                        downloadLink.setDownloadSize(estimatedSize);
                                    } else {
                                        downloadLink.setDownloadSize(Math.max(downloadLink.getKnownDownloadSize(), estimatedSize));
                                    }
                                }
                                this.dllink = hlsURL;
                                return AvailableStatus.TRUE;
                            } catch (Exception e) {
                                logger.log(e);
                                if (caughtE == null) {
                                    caughtE = e;
                                }
                            }
                        }
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, caughtE);
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isSubtitle(downloadLink)) {
            final String contentURL = downloadLink.getContentUrl();
            if (contentURL != null) {
                br.getPage(contentURL);
            }
            dllink = getDirectlink(downloadLink);
            if (!checkDirectLink(downloadLink) || dllink == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            final String contentURL = downloadLink.getContentUrl();
            if (contentURL != null) {
                br.getPage(contentURL);
            }
            String mainlink = downloadLink.getStringProperty("mainlink", null);
            logger.info("mainlink: " + mainlink);
            dllink = getDirectlink(downloadLink);
            logger.info("dllink: " + dllink);
            if (dllink == null) {
            } else {
                // System.out.println("DLink FOund");
            }
            /* .m4a links have wrong internal directlinks --> Size check not possible */
            if (!downloadLink.getName().contains(".m4a")) {
                if (!checkDirectLink(downloadLink) || dllink == null) {
                    dllink = findFreshDirectlink(downloadLink);
                    if (dllink == null) {
                        logger.warning("dllink is null...");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    if (!checkDirectLink(downloadLink)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
            }
        }
        downloadLink.setFinalFileName(getFormattedFilename(downloadLink));
        return AvailableStatus.TRUE;
    }

    private String getVideoID(DownloadLink downloadLink) throws Exception {
        final String ret = downloadLink.getStringProperty("plain_videoid", null);
        if (ret == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return ret;
        }
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        if (isHDS(downloadLink)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "HDS stream download is not supported (yet)!");
        } else if (isHLS(downloadLink)) {
            if (this.dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, this.dllink);
            dl.startDownload();
        } else if (dllink.startsWith("rtmp")) {
            downloadLink.setFinalFileName(getFormattedFilename(downloadLink));
            String[] stream = dllink.split("@");
            dl = new RTMPDownload(this, downloadLink, stream[0]);
            setupRTMPConnection(stream, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            downloadDirect(downloadLink);
        }
    }

    private String getDirectlink(final DownloadLink dl) {
        return dl.getStringProperty("directlink", null);
    }

    protected void downloadDirect(DownloadLink downloadLink) throws Exception {
        /* Workaround for old downloadcore bug that can lead to incomplete files */
        br.getHeaders().put("Accept-Encoding", "identity");
        downloadLink.setFinalFileName(getFormattedFilename(downloadLink));
        /*
         * They do allow resume and unlimited chunks but resuming or using more than 1 chunk causes problems, the file will then be
         * corrupted!
         */
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, false, 1);
        /* Their servers usually return a valid size - if not, it's probably a server error */
        final long contentlength = dl.getConnection().getLongContentLength();
        if (contentlength == -1) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String findFreshDirectlink(final DownloadLink dl) throws IOException {
        try {
            dllink = null;
            /* Remove old cookies/Referer */
            this.br = new Browser();
            final String mainlink = dl.getStringProperty("mainlink", null);
            br.setFollowRedirects(true);
            br.getPage(mainlink);
            logger.info("findFreshDirectlink - getPage mainlink has been done, next: getVideosource");
            br.setFollowRedirects(false);
            final String videosource = getVideosource(br, getVideoID(dl));
            if (videosource == null) {
                logger.info("videosource: " + videosource);
                return null;
            }
            LinkedHashMap<String, String[]> foundqualities = findVideoQualities(this, this.br, mainlink, videosource);
            final String qualityvalue = dl.getStringProperty("qualityvalue", null);
            final String directlinkinfo[] = foundqualities.get(qualityvalue);
            dllink = Encoding.htmlDecode(directlinkinfo[0]);
            onNewDirectLink(dl, dllink);
        } catch (final Throwable e) {
            logger.log(e);
            dllink = null;
            return null;
        }
        return dllink;
    }

    protected void onNewDirectLink(DownloadLink dl, String dllink2) {
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        ai.setStatus("Registered (free) User");
        return ai;
    }

    protected boolean checkDirectLink(final DownloadLink link) throws PluginException {
        if (dllink != null) {
            br.setFollowRedirects(false);
            try {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(dllink);
                    if (con.getResponseCode() == 302) {
                        br.followConnection();
                        dllink = br.getRedirectLocation().replace("#cell=core&comment=", "");
                        br.getHeaders().put("Referer", dllink);
                        con = br.openHeadConnection(dllink);
                    } else if (!this.looksLikeDownloadableContent(con)) {
                        return false;
                    }
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            } catch (final Exception e) {
                logger.log(e);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.dailymotion.com/de/legal/terms";
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
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (link.getBooleanProperty("countryblock", false)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, COUNTRYBLOCKUSERTEXT);
        } else if (link.getBooleanProperty("registeredonly", false)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, REGISTEREDONLYUSERTEXT);
        }
        doFree(link);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link);
        if (link.getBooleanProperty("ishds", false)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "HDS stream download is not supported (yet)!");
        } else if (link.getBooleanProperty("countryblock", false)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, COUNTRYBLOCKUSERTEXT);
        }
        doFree(link);
    }

    public void login(final Account account, final boolean verifyCookies) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser(br);
        final Cookies userCookies = Cookies.parseCookiesFromJsonString(account.getPass(), getLogger());
        if (userCookies == null || getAuthorizationCookieValue(userCookies) == null) {
            if (account.getLastValidTimestamp() == -1) {
                showCookieLoginInformation();
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Cookie login required", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        br.setCookies(userCookies);
        if (!verifyCookies) {
            return;
        } else {
            setGraphqlHeaders(br);
            br.postPageRaw("https://graphql.api.dailymotion.com/", "{\"operationName\":\"USER_BIRTHDAY_QUERY\",\"variables\":{},\"query\":\"query USER_BIRTHDAY_QUERY {  me {    id    birthday    __typename  }}\"}");
            try {
                final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final Map<String, Object> data = (Map<String, Object>) root.get("data");
                final Map<String, Object> me = (Map<String, Object>) data.get("me");
                /* Throws Exception on null value */
                final String userID = me.get("id").toString();
                account.setUser(userID);
            } catch (final Throwable e) {
                logger.log(e);
                logger.info("User Cookie login failed");
                if (account.getLastValidTimestamp() == -1) {
                    showCookieLoginInformation();
                    throw new AccountInvalidException("Cookie login failed");
                } else {
                    throw new AccountInvalidException("Cookies expired");
                }
            }
        }
    }

    private String getAuthorizationCookieValue(final Cookies cookies) {
        if (cookies == null) {
            return null;
        } else {
            final Cookie authCookie = cookies.get("access_token");
            if (authCookie == null) {
                return null;
            } else {
                return authCookie.getValue();
            }
        }
    }

    private void setGraphqlHeaders(final Browser br) {
        final String authValue = getAuthorizationCookieValue(br.getCookies(this.getHost()));
        br.getHeaders().put("Authorization", "Bearer " + authValue);
        br.getHeaders().put("Content-Type", "application/json, application/json");
        br.getHeaders().put("Origin", "https://www.dailymotion.com");
        br.getHeaders().put("Referer", "https://www.dailymotion.com");
        br.getHeaders().put("X-DM-AppInfo-Id", "com.dailymotion.neon");
        br.getHeaders().put("X-DM-AppInfo-Type", "website");
        br.getHeaders().put("X-DM-AppInfo-Version", "v2022-01-17T10:14:03.307Z");
        br.getHeaders().put("X-DM-Neon-SSR", "0");
        br.getHeaders().put("X-DM-Preferred-Country", "de");
    }

    private Thread showCookieLoginInformation() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    final String help_article_url = "https://support.jdownloader.org/Knowledgebase/Article/View/account-cookie-login-instructions";
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Dailymotion.com - Login";
                        message += "Hallo liebe(r) Dailymotion NutzerIn\r\n";
                        message += "Um deinen Dailymotion Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "Folge der Anleitung im Hilfe-Artikel:\r\n";
                        message += help_article_url;
                    } else {
                        title = "Dailymotion.com - Login";
                        message += "Hello dear Dailymotion user\r\n";
                        message += "In order to use an account of this service in JDownloader, you need to follow these instructions:\r\n";
                        message += help_article_url;
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(help_article_url);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private boolean isSubtitle(final DownloadLink dl) {
        return dl.getBooleanProperty("type_subtitle", false);
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setUrl(stream[0]);
        rtmp.setSwfVfy(stream[1]);
        rtmp.setResume(true);
    }

    public static Browser prepBrowser(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "de, en-gb;q=0.9, en;q=0.8");
        br.getHeaders().put("Accept-Encoding", "gzip");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.setCookie("http://www.dailymotion.com", "family_filter", "off");
        br.setCookie("http://www.dailymotion.com", "ff", "off");
        br.setCookie("http://www.dailymotion.com", "lang", "en_US");
        br.setAllowedResponseCodes(new int[] { 410 });
        return br;
    }

    private boolean isHDS(final DownloadLink dl) {
        return "hds".equals(dl.getStringProperty("qualityname", null));
    }

    private boolean isHLS(final DownloadLink dl) {
        final String directLink = getDirectlink(dl);
        return StringUtils.contains(directLink, ".m3u8");
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public String getDescription() {
        return "JDownloader's DailyMotion plugin helps downloading Videoclips from dailymotion.com. DailyMotion provides different video formats and qualities.";
    }

    final static String[][] REPLACES = { { "plain_date", "date", "Date when the video was uploaded" }, { "plain_videoid", "videoid", "ID of the video" }, { "plain_channel", "channelname", "The name of the channel/uploader" }, { "plain_ext", "ext", "Extension of the file (usually .mp4)" }, { "qualityname", "quality", "Quality of the video" }, { "plain_videoname", "videoname", "Name of the video" } };

    public static String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("dailymotion.com");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (!formattedFilename.contains("*videoname") && !formattedFilename.contains("*ext*") && !formattedFilename.contains("*videoid*") && !formattedFilename.contains("*channelname*")) {
            formattedFilename = defaultCustomFilename;
        }
        for (final String[] replaceinfo : REPLACES) {
            final String property = replaceinfo[0];
            final String fulltagname = "*" + replaceinfo[1] + "*";
            String tag_data = downloadLink.getStringProperty(property, "-");
            if (fulltagname.equals("*date*")) {
                if (tag_data.equals("-")) {
                    tag_data = "0";
                }
                final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
                SimpleDateFormat formatter = null;
                Date theDate = new Date(Long.parseLong(tag_data));
                if (userDefinedDateFormat != null) {
                    try {
                        formatter = new SimpleDateFormat(userDefinedDateFormat);
                        tag_data = formatter.format(theDate);
                    } catch (Exception e) {
                        // prevent user error killing plugin.
                        tag_data = "-";
                    }
                }
            }
            formattedFilename = formattedFilename.replace(fulltagname, tag_data);
        }
        formattedFilename = correctFilename(formattedFilename);
        return formattedFilename;
    }

    public static String correctFilename(String filename) {
        // Cut filenames if they're too long
        if (filename.length() > 240) {
            final String ext = getFileNameExtensionFromString(filename, "");
            int extLength = ext.length();
            filename = filename.substring(0, 240 - extLength);
            filename += ext;
        }
        return filename;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_SUBTITLE, "Grab subtitle?").setDefaultValue(default_ALLOW_SUBTITLE));
        final ConfigEntry hq = addConfigElementBestOnly();
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_144, JDL.L("plugins.hoster.dailymotioncom.check144", "Grab 144p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_240, JDL.L("plugins.hoster.dailymotioncom.check240", "Grab 240p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_380, JDL.L("plugins.hoster.dailymotioncom.check380", "Grab 380p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480, JDL.L("plugins.hoster.dailymotioncom.check480", "Grab 480p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720, JDL.L("plugins.hoster.dailymotioncom.check720", "Grab 720p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1080, JDL.L("plugins.hoster.dailymotioncom.check1080", "Grab 1080p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1440, JDL.L("plugins.hoster.dailymotioncom.check1440", "Grab 1440p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_2160, JDL.L("plugins.hoster.dailymotioncom.check2160", "Grab 2160p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_AUDIO, JDL.L("plugins.hoster.dailymotioncom.checkaudio", "Allow audio download")).setDefaultValue(defaultAllowAudio));
        /* 2016-06-10: Disabled rtmp and hds - should not be needed anymore! */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_OTHERS, JDL.L("plugins.hoster.dailymotioncom.checkother", "Grab other available qualities (RTMP/OTHERS)?")).setDefaultValue(true).setEnabledCondidtion(hq, false).setEnabled(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HLS, JDL.L("plugins.hoster.dailymotioncom.allowhls", "Grab HLS?")).setDefaultValue(default_ALLOW_HLS));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MP4, JDL.L("plugins.hoster.dailymotioncom.allowmp4", "Grab MP4 HTTP?")).setDefaultValue(default_ALLOW_MP4));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filenames"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.dailymotioncom.customdate", "Define how the date should look.")).setDefaultValue(defaultCustomDate));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*channelname*_*date*_*videoname**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, JDL.L("plugins.hoster.dailymotioncom.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("Explanation of the available tags:<br>");
        for (final String[] replaceinfo : REPLACES) {
            final String fulltagname = "*" + replaceinfo[1] + "*";
            final String tagdescription = replaceinfo[2];
            sb.append(fulltagname + " = " + tagdescription + "<br>");
        }
        sb.append("</html>");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
    }

    public ConfigEntry addConfigElementBestOnly() {
        final ConfigEntry hq = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, JDL.L("plugins.hoster.dailymotioncom.checkbest", "Only grab the best available resolution")).setDefaultValue(false);
        getConfig().addEntry(hq);
        return hq;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}