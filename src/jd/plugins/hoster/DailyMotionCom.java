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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
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
import jd.plugins.decrypter.DailyMotionComDecrypter;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dailymotion.com" }, urls = { "https?://dailymotion\\.com/video/\\w+" })
public class DailyMotionCom extends PluginForHost {
    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING, LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
    }

    public String getVideosource(final Browser br, final String videoID) throws Exception {
        return DailyMotionComDecrypter.getVideosource(this, br, videoID);
    }

    public static LinkedHashMap<String, String[]> findVideoQualities(final Plugin plugin, final Browser br, final String parameter, String videosource) throws Exception {
        return DailyMotionComDecrypter.findVideoQualities(plugin, br, parameter, videosource);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String videoid = getVideoID(link);
        if (videoid != null) {
            return "dailymotioncom" + videoid + "_" + getQualityHeight(link);
        } else {
            return super.getLinkID(link);
        }
    }

    /** Settings stuff */
    public static final String   ALLOW_SUBTITLE          = "ALLOW_SUBTITLE";
    public static final String   ALLOW_BEST              = "ALLOW_BEST";
    public static final String   ALLOW_240               = "ALLOW_1";
    public static final String   ALLOW_380               = "ALLOW_2";
    public static final String   ALLOW_480               = "ALLOW_3";
    public static final String   ALLOW_720               = "ALLOW_4";
    public static final String   ALLOW_1080              = "ALLOW_5";
    public static final String   ALLOW_1440              = "ALLOW_6";
    public static final String   ALLOW_2160              = "ALLOW_7";
    public static final String   ALLOW_AUDIO             = "ALLOW_AUDIO";
    private static final String  CUSTOM_DATE             = "CUSTOM_DATE";
    private static final String  CUSTOM_FILENAME         = "CUSTOM_FILENAME";
    private final static String  defaultCustomFilename   = "*videoname*_*quality**ext*";
    private final static String  defaultCustomDate       = "dd.MM.yyyy";
    public static final String   API_BASE_GRAPHQL        = "https://graphql.api.dailymotion.com/";
    public final static boolean  default_ALLOW_SUBTITLE  = true;
    private final static boolean defaultAllowAudio       = true;
    public static final boolean  default_ALLOW_HLS       = true;
    public static final boolean  default_ALLOW_MP4       = false;
    public static final String   PROPERTY_HLS_MASTER     = "hls_master";
    public static final String   PROPERTY_DIRECTURL      = "directurl";
    public static final String   PROPERTY_TITLE          = "plain_videoname";
    public static final String   PROPERTY_CONTENT_URL    = "mainlink";
    public static final String   PROPERTY_VIDEO_ID       = "plain_videoid";
    public static final String   PROPERTY_QUALITY_NAME   = "qualityname";
    public static final String   PROPERTY_QUALITY_HEIGHT = "height";
    public static final String   PROPERTY_DATE_TIMESTAMP = "plain_date";
    public static final String   PROPERTY_CHANNEL        = "plain_channel";
    public static final String   PROPERTY_TYPE           = "type";
    public static final String   TYPE_AUDIO              = "audio";
    public static final String   TYPE_VIDEO              = "video";
    public static final String   TYPE_SUBTITLE           = "subtitle";

    public DailyMotionCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.dailymotion.com/register");
        setConfigElements();
    }

    private String getVideoID(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_VIDEO_ID);
    }

    public static boolean isAudio(final DownloadLink link) {
        final String type = getType(link);
        if (StringUtils.equals(type, TYPE_AUDIO)) {
            return true;
        } else {
            /* Legacy */
            final String legacy_plain_ext = link.getStringProperty("plain_ext");
            if (StringUtils.equalsIgnoreCase(legacy_plain_ext, ".m4a")) {
                return true;
            } else if (StringUtils.endsWithCaseInsensitive(link.getName(), ".m4a")) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean isVideo(final DownloadLink link) {
        final String type = getType(link);
        if (StringUtils.equals(type, TYPE_VIDEO)) {
            return true;
        } else {
            /* Legacy */
            final String legacy_plain_ext = link.getStringProperty("plain_ext");
            if (StringUtils.equalsIgnoreCase(legacy_plain_ext, ".mp4")) {
                return true;
            } else if (StringUtils.endsWithCaseInsensitive(link.getName(), ".mp4")) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean isSubtitle(final DownloadLink link) {
        final String type = getType(link);
        if (StringUtils.equals(type, TYPE_SUBTITLE)) {
            return true;
        } else {
            /* Legacy */
            return link.getBooleanProperty("type_subtitle", false);
        }
    }

    public static String getType(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_TYPE);
    }

    /**
     * Returns height of this item. </br>
     * -1 = Fallback / audio.
     */
    public static int getQualityHeight(final DownloadLink link) {
        final int height = link.getIntegerProperty(PROPERTY_QUALITY_HEIGHT, -1);
        if (height != -1) {
            return height;
        } else {
            /* For older items. */
            final String legacyQualityNumberStr = link.getStringProperty("qualitynumber");
            if (legacyQualityNumberStr != null && legacyQualityNumberStr.matches("\\d+")) {
                final int legacyQualityNumber = Integer.parseInt(legacyQualityNumberStr);
                switch (legacyQualityNumber) {
                case 1:
                    return 240;
                case 2:
                    return 380;
                case 3:
                    return 480;
                case 4:
                    return 720;
                case 5:
                    return 1080;
                case 6:
                    return 1440;
                case 7:
                    return 2160;
                default:
                    return -1;
                }
            } else {
                /* This should never happen. */
                return -1;
            }
        }
    }

    private String getHlsMaster(final DownloadLink link) {
        final String hlsMaster = link.getStringProperty(PROPERTY_HLS_MASTER);
        if (hlsMaster != null) {
            return hlsMaster;
        } else {
            /* For older items */
            return link.getStringProperty("directlink");
        }
    }

    public static String getDirectlink(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_DIRECTURL);
    }

    private String getContentURL(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_CONTENT_URL);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "family_filter", "off");
        br.setCookie(this.getHost(), "ff", "off");
        br.setCookie(this.getHost(), "lang", "en_US");
        prepBrowser(this.br);
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (link.getBooleanProperty("countryblock", false)) {
            return AvailableStatus.TRUE;
        } else if (link.getBooleanProperty("registeredonly", false)) {
            return AvailableStatus.TRUE;
        }
        if (isHLS(link)) {
            /* Make sure to follow redirects! */
            this.br.setFollowRedirects(true);
            String directurl = getDirectlink(link);
            if (directurl == null) {
                directurl = this.findFreshDirectlink(link);
            }
            checkFFmpeg(link, "Check a HLS Stream");
            final Browser brc = br.cloneBrowser();
            final HLSDownloader downloader = new HLSDownloader(link, brc, directurl);
            final StreamInfo streamInfo = downloader.getProbe();
            if (link.getBooleanProperty("encrypted")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Encrypted HLS is not supported");
            }
            if (streamInfo != null) {
                final long estimatedSize = downloader.getEstimatedSize();
                if (link.getKnownDownloadSize() == -1) {
                    link.setDownloadSize(estimatedSize);
                } else {
                    link.setDownloadSize(Math.max(link.getKnownDownloadSize(), estimatedSize));
                }
            }
        } else if (isSubtitle(link)) {
            /* Do not check - assume that subtitle URLs are always online. */
            final String directurl = getDirectlink(link);
            if (directurl == null) {
                /* This should never happen. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            /* Old/unsupported HTTP URLs. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setFinalFileName(getFormattedFilename(link));
        return AvailableStatus.TRUE;
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (link.getBooleanProperty("countryblock", false)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This video is not available for your country");
        }
        final String directurl = getDirectlink(link);
        if (directurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (isHLS(link)) {
            /* HLS download */
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, directurl);
            dl.startDownload();
        } else {
            /* HTTP download */
            downloadDirect(link);
        }
    }

    protected void downloadDirect(final DownloadLink link) throws Exception {
        /* Workaround for old downloadcore bug that can lead to incomplete files */
        final String directurl = getDirectlink(link);
        if (directurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Accept-Encoding", "identity");
        link.setFinalFileName(getFormattedFilename(link));
        /*
         * They do allow resume and unlimited chunks but resuming or using more than 1 chunk causes problems, the file will then be
         * corrupted!
         */
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, directurl, false, 1);
        /* Their servers usually return a valid size - if not, it's probably a server error */
        final long contentlength = dl.getConnection().getLongContentLength();
        if (contentlength == -1) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String findFreshDirectlink(final DownloadLink link) throws Exception {
        if (isSubtitle(link)) {
            /* Subtitle directurls should be static so this should never be called for subtitle files. */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String contentURL = this.getContentURL(link);
        String dllink = null;
        final DailyMotionComDecrypter crawler = (DailyMotionComDecrypter) this.getNewPluginForDecryptInstance(this.getHost());
        final ArrayList<DownloadLink> results = crawler.crawlSingleVideo(new CryptedLink(contentURL), contentURL, null, true);
        DownloadLink fresh = null;
        for (final DownloadLink result : results) {
            if (StringUtils.equals(this.getLinkID(result), this.getLinkID(link))) {
                fresh = result;
                break;
            }
        }
        if (fresh == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        onNewDirectLink(link, dllink);
        link.setProperties(fresh.getProperties());
        return getDirectlink(link);
    }

    protected void onNewDirectLink(DownloadLink dl, String freshDirectlink) {
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "https://www.dailymotion.com/de/legal/terms";
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
        if (link.getBooleanProperty("registeredonly", false)) {
            throw new AccountRequiredException();
        }
        handleDownload(link, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link);
        handleDownload(link, account);
    }

    public void login(final Account account, final boolean verifyCookies) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser(br);
        final Cookies userCookies = account.loadUserCookies();
        if (userCookies == null || getAuthorizationCookieValue(userCookies) == null) {
            if (!account.hasEverBeenValid()) {
                showCookieLoginInfo();
            }
            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
        }
        br.setCookies(userCookies);
        if (!verifyCookies) {
            return;
        } else {
            setGraphqlHeaders(br);
            br.postPageRaw(API_BASE_GRAPHQL, "{\"operationName\":\"USER_BIRTHDAY_QUERY\",\"variables\":{},\"query\":\"query USER_BIRTHDAY_QUERY {  me {    id    birthday    __typename  }}\"}");
            try {
                final Map<String, Object> root = restoreFromString(br.toString(), TypeRef.MAP);
                final Map<String, Object> data = (Map<String, Object>) root.get("data");
                final Map<String, Object> me = (Map<String, Object>) data.get("me");
                /* Throws Exception on null value */
                final String userID = me.get("id").toString();
                account.setUser(userID);
            } catch (final Throwable e) {
                logger.log(e);
                logger.info("User Cookie login failed");
                if (account.hasEverBeenValid()) {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                } else {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
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

    private boolean isHLS(final DownloadLink link) {
        final String directlink = getDirectlink(link);
        if (directlink != null && StringUtils.containsIgnoreCase(directlink, ".m3u8")) {
            return true;
        } else if (getHlsMaster(link) != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        /* Allow only downloads with original plugin. */
        return link.getHost().equalsIgnoreCase(plugin.getHost());
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_240, "Grab 240p?").setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_380, "Grab 380p?").setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480, "Grab 480p?").setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720, "Grab 720p?").setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1080, "Grab 1080p?").setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1440, "Grab 1440p?").setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_2160, "Grab 2160p?").setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_AUDIO, "Allow audio download").setDefaultValue(defaultAllowAudio));
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