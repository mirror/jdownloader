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
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.YoutubeITAG;
import jd.plugins.components.YoutubeVariant;
import jd.plugins.components.YoutubeVariantInterface;
import jd.plugins.decrypter.YoutubeHelper;
import jd.plugins.download.DownloadInterface;
import jd.utils.locale.JDL;

import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dailymotion.com" }, urls = { "http://dailymotiondecrypted\\.com/video/\\w+" }, flags = { 2 })
public class DailyMotionCom extends PluginForHost {
    private static String getQuality(final String quality, final String videosource) {
        return new Regex(videosource, "\"" + quality + "\":\"(http[^<>\"\\']+)\"").getMatch(0);
    }

    /* Sync the following functions in hoster- and decrypterplugin */
    public static String getVideosource(final Browser br) {
        String videosource = br.getRegex("\"sequence\":\"([^<>\"]*?)\"").getMatch(0);
        if (videosource == null) {
            videosource = br.getRegex("%2Fsequence%2F(.*?)</object>").getMatch(0);
        }
        if (videosource == null) {
            videosource = br.getRegex("name=\"flashvars\" value=\"(.*?)\"/></object>").getMatch(0);
        }
        if (videosource != null) {
            videosource = Encoding.htmlDecode(videosource).replace("\\", "");
        }
        return videosource;
    }

    public static LinkedHashMap<String, String[]> findVideoQualities(final Browser br, final String parameter, String videosource) throws IOException {
        LinkedHashMap<String, String[]> QUALITIES = new LinkedHashMap<String, String[]>();
        final String[][] qualities = { { "hd1080URL", "1" }, { "hd720URL", "2" }, { "hqURL", "3" }, { "sdURL", "4" }, { "ldURL", "5" }, { "video_url", "5" } };
        for (final String quality[] : qualities) {
            final String qualityName = quality[0];
            final String qualityNumber = quality[1];
            final String currentQualityUrl = getQuality(qualityName, videosource);
            if (currentQualityUrl != null) {
                final String[] dlinfo = new String[4];
                dlinfo[0] = currentQualityUrl;
                dlinfo[1] = null;
                dlinfo[2] = qualityName;
                dlinfo[3] = qualityNumber;
                QUALITIES.put(qualityNumber, dlinfo);
            }
        }
        // List empty or only 1 link found -> Check for (more) links
        if (QUALITIES.isEmpty() || QUALITIES.size() == 1) {
            final String manifestURL = new Regex(videosource, "\"autoURL\":\"(http://[^<>\"]*?)\"").getMatch(0);
            if (manifestURL != null) {
                /** HDS */
                final String[] dlinfo = new String[4];
                dlinfo[0] = manifestURL;
                dlinfo[1] = "hds";
                dlinfo[2] = "autoURL";
                dlinfo[3] = "7";
                QUALITIES.put("7", dlinfo);
            }

            // Try to avoid HDS
            br.getPage("http://www.dailymotion.com/embed/video/" + new Regex(parameter, "([A-Za-z0-9\\-_]+)$").getMatch(0));
            videosource = br.getRegex("var info = \\{(.*?)\\},").getMatch(0);
            if (videosource != null) {
                videosource = Encoding.htmlDecode(videosource).replace("\\", "");
                final String[][] embedQualities = { { "stream_h264_ld_url", "5" }, { "stream_h264_url", "4" }, { "stream_h264_hq_url", "3" }, { "stream_h264_hd_url", "2" }, { "stream_h264_hd1080_url", "1" } };
                for (final String quality[] : embedQualities) {
                    final String qualityName = quality[0];
                    final String qualityNumber = quality[1];
                    final String currentQualityUrl = getQuality(qualityName, videosource);
                    if (currentQualityUrl != null) {
                        final String[] dlinfo = new String[4];
                        dlinfo[0] = currentQualityUrl;
                        dlinfo[1] = null;
                        dlinfo[2] = qualityName;
                        dlinfo[3] = qualityNumber;
                        QUALITIES.put(qualityNumber, dlinfo);
                    }
                }
            }
            // if (FOUNDQUALITIES.isEmpty()) {
            // String[] values =
            // br.getRegex("new SWFObject\\(\"(http://player\\.grabnetworks\\.com/swf/GrabOSMFPlayer\\.swf)\\?id=\\d+\\&content=v([0-9a-f]+)\"").getRow(0);
            // if (values == null || values.length != 2) {
            // /** RTMP */
            // final DownloadLink dl = createDownloadlink("http://dailymotiondecrypted.com/video/" + System.currentTimeMillis() + new
            // Random(10000));
            // dl.setProperty("isrtmp", true);
            // dl.setProperty("mainlink", PARAMETER);
            // dl.setFinalFileName(FILENAME + "_RTMP.mp4");
            // fp.add(dl);
            // decryptedLinks.add(dl);
            // return decryptedLinks;
            // }
            // }
        }
        return QUALITIES;
    }

    public String               dllink                 = null;
    private static final String MAINPAGE               = "http://www.dailymotion.com/";
    private static final String REGISTEREDONLYUSERTEXT = "Download only possible for registered users";
    private static final String COUNTRYBLOCKUSERTEXT   = "This video is not available for your country";
    /** Settings stuff */
    private static final String ALLOW_BEST             = "ALLOW_BEST";
    private static final String ALLOW_LQ               = "ALLOW_LQ";
    private static final String ALLOW_SD               = "ALLOW_SD";
    private static final String ALLOW_HQ               = "ALLOW_HQ";
    private static final String ALLOW_720              = "ALLOW_720";
    private static final String ALLOW_1080             = "ALLOW_1080";
    private static final String ALLOW_OTHERS           = "ALLOW_OTHERS";
    private static final String ALLOW_HDS              = "ALLOW_HDS";

    public DailyMotionCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.dailymotion.com/register");
        setConfigElements();
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("dailymotiondecrypted.com/", "dailymotion.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.setCookie("http://www.dailymotion.com", "family_filter", "off");
        br.setCookie("http://www.dailymotion.com", "ff", "off");
        br.setCookie("http://www.dailymotion.com", "lang", "en_US");
        prepBrowser();
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
            downloadLink.getLinkStatus().setStatusText("HDS stream download is not supported (yet)!");
            downloadLink.setFinalFileName(downloadLink.getStringProperty("directname", null));
            return AvailableStatus.TRUE;
        } else if (downloadLink.getBooleanProperty("isrtmp", false)) {
            getRTMPlink();
        } else {
            dllink = downloadLink.getStringProperty("directlink", null);
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
        return AvailableStatus.TRUE;
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        if (isHDS(downloadLink)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "HDS stream download is not supported (yet)!");
        } else if (dllink.startsWith("rtmp")) {
            String[] stream = dllink.split("@");
            dl = new RTMPDownload(this, downloadLink, stream[0]);
            setupRTMPConnection(stream, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            // They do allow resume and unlimited chunks but resuming or using
            // more
            // than 1 chunk causes problems, the file will then b corrupted!
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
            // Their servers usually return a valid size - if not, it's probably a server error
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
    }

    private String findFreshDirectlink(final DownloadLink dl) throws IOException {
        try {
            dllink = null;
            br.setFollowRedirects(true);
            br.getPage(dl.getStringProperty("mainlink", null));
            br.setFollowRedirects(false);
            final String videosource = getVideosource(this.br);
            if (videosource == null) {
                return null;
            }
            LinkedHashMap<String, String[]> foundqualities = findVideoQualities(this.br, dl.getDownloadURL(), videosource);
            final String qualityvalue = dl.getStringProperty("qualityvalue", null);
            final String directlinkinfo[] = foundqualities.get(qualityvalue);
            dllink = Encoding.htmlDecode(directlinkinfo[0]);
        } catch (final Throwable e) {
            dllink = null;
            return null;
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, this.br);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered (free) User");
        return ai;
    }

    private boolean checkDirectLink(final DownloadLink downloadLink) {
        if (dllink != null) {
            br.setFollowRedirects(false);
            try {
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(dllink);
                    if (con.getResponseCode() == 302) {
                        br.followConnection();
                        dllink = br.getRedirectLocation().replace("#cell=core&comment=", "");
                        br.getHeaders().put("Referer", dllink);
                        con = br.openGetConnection(dllink);
                    }
                    if (con.getResponseCode() == 410 || con.getContentType().contains("html")) {
                        return false;
                    }
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            } catch (final Exception e) {
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getBooleanProperty("countryblock", false)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, COUNTRYBLOCKUSERTEXT);
        }
        if (downloadLink.getBooleanProperty("registeredonly", false)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, REGISTEREDONLYUSERTEXT);
        }
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, this.br);
        requestFileInformation(link);
        if (link.getBooleanProperty("ishds", false)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "HDS stream download is not supported (yet)!");
        } else if (link.getBooleanProperty("countryblock", false)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, COUNTRYBLOCKUSERTEXT);
        }
        doFree(link);
    }

    public void login(final Account account, final Browser br) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-Prototype-Version", "1.6.1");
        br.postPage("http://www.dailymotion.com/pageitem/login", "form_name=dm_pageitem_login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&login_submit=Login");
        if (br.getCookie(MAINPAGE, "sid") == null || br.getCookie(MAINPAGE, "sdx") == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    private void getRTMPlink() throws IOException, PluginException {
        final String[] values = br.getRegex("new SWFObject\\(\"(http://player\\.grabnetworks\\.com/swf/GrabOSMFPlayer\\.swf)\\?id=\\d+\\&content=v([0-9a-f]+)\"").getRow(0);
        if (values == null || values.length != 2) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser rtmp = br.cloneBrowser();
        rtmp.getPage("http://content.grabnetworks.com/v/" + values[1] + "?from=" + dllink);
        dllink = rtmp.getRegex("\"url\":\"(rtmp[^\"]+)").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink + "@" + values[0];
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setUrl(stream[0]);
        rtmp.setSwfVfy(stream[1]);
        rtmp.setResume(true);
    }

    private void prepBrowser() {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:28.0) Gecko/20100101 Firefox/28.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "de, en-gb;q=0.9, en;q=0.8");
        br.getHeaders().put("Accept-Encoding", "gzip");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
    }

    private boolean isHDS(final DownloadLink dl) {
        return "hds".equals(dl.getStringProperty("qualityname", null));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public String getDescription() {
        return "JDownloader's DailyMotion Plugin helps downloading Videoclips from dailymotion.com. DailyMotion provides different video formats and qualities.";
    }

    private void setConfigElements() {
        final ConfigEntry hq = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, JDL.L("plugins.hoster.dailymotioncom.checkbest", "Only grab the best available resolution")).setDefaultValue(false);
        getConfig().addEntry(hq);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_LQ, JDL.L("plugins.hoster.dailymotioncom.checkLQ", "Grab LQ/LD [320x240]?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_SD, JDL.L("plugins.hoster.dailymotioncom.checkSD", "Grab SD/HQ [512x384]?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HQ, JDL.L("plugins.hoster.dailymotioncom.checkHQ", "Grab HQ/HD [848x480]?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720, JDL.L("plugins.hoster.dailymotioncom.check720", "Grab [1280x720]?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1080, JDL.L("plugins.hoster.dailymotioncom.check1080", "Grab [1920x1080]?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_OTHERS, JDL.L("plugins.hoster.dailymotioncom.checkother", "Grab other available qualities (RTMP/OTHERS)?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HDS, JDL.L("plugins.hoster.dailymotioncom.checkhds", "Grab hds (not downloadable yet!)?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
    }

    public static abstract class Replacer {

        private String[] tags;

        public String[] getTags() {
            return tags;
        }

        abstract public String getDescription();

        public Replacer(String... tags) {
            this.tags = tags;
        }

        public String replace(String name, YoutubeHelper helper, DownloadLink link) {
            for (String tag : tags) {
                String mod = new Regex(name, "\\*" + tag + "\\[(.+?)\\]\\*").getMatch(0);
                if (mod != null) {

                    name = name.replaceAll("\\*" + tag + "(\\[[^\\]]+\\])\\*", getValue(link, helper, mod));
                }
                if (name.contains("*" + tag + "*")) {
                    String v = getValue(link, helper, null);
                    name = name.replace("*" + tag + "*", v == null ? "" : v);
                }

            }
            return name;
        }

        abstract protected String getValue(DownloadLink link, YoutubeHelper helper, String mod);

        public boolean isExtendedRequired() {
            return false;
        }

        public static enum DataSource {
            WEBSITE,
            API_VIDEOS,
            API_USERS
        }

        public DataSource getDataSource() {
            return DataSource.WEBSITE;
        }

        public boolean matches(String checkName) {
            for (String tag : tags) {
                if (checkName.contains("*" + tag + "*")) {
                    return true;
                }
                if (Pattern.compile("\\*" + tag + "\\[(.+?)\\]\\*", Pattern.CASE_INSENSITIVE).matcher(checkName).find()) {
                    return true;
                }

            }
            return false;
        }

    }

    public static LogSource      LOGGER   = LogController.getInstance().getLogger(YoutubeHelper.class.getName());
    public static List<Replacer> REPLACER = new ArrayList<Replacer>();
    static {
        REPLACER.add(new Replacer("group") {

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                String var = link.getStringProperty(YoutubeHelper.YT_VARIANT, "");
                YoutubeVariantInterface variant = helper.getVariantById(var);
                try {
                    return variant.getGroup().getLabel();
                } catch (Throwable e) {
                    // old variant
                    LOGGER.log(e);
                    return "[INVALID LINK!]";
                }
            }

            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_group();
            }

        });
        REPLACER.add(new Replacer("variant") {

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_VARIANT, "");
            }

            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_variantid();
            }

        });
        REPLACER.add(new Replacer("quality") {
            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_quality();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                String var = link.getStringProperty(YoutubeHelper.YT_VARIANT, "");
                YoutubeVariantInterface variant = helper.getVariantById(var);
                try {
                    return variant.getQualityExtension();
                } catch (Throwable e) {
                    // old variant
                    LOGGER.log(e);
                    return "[INVALID LINK!]";
                }
            }

        });
        REPLACER.add(new Replacer("videoid", "id") {
            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_id();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_ID, "");
            }

        });
        REPLACER.add(new Replacer("ext", "extension") {
            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_extension();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_EXT, "unknown");
            }

        });

        REPLACER.add(new Replacer("agegate", "age") {

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getBooleanProperty(YoutubeHelper.YT_AGE_GATE, false) + "";
            }

            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_age();
            }

        });
        REPLACER.add(new Replacer("username", "user") {

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_USER, "");
            }

            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_user();
            }

        });
        REPLACER.add(new Replacer("channel", "channelname") {

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_CHANNEL, "");
            }

            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_channel();
            }

        });

        REPLACER.add(new Replacer("videoname", "title") {

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_TITLE, "");
            }

            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_title();
            }

        });
        REPLACER.add(new Replacer("date") {
            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_date();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                // date

                DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG, TranslationFactory.getDesiredLocale());

                if (StringUtils.isNotEmpty(mod)) {
                    try {
                        formatter = new SimpleDateFormat(mod);
                    } catch (Throwable e) {
                        LOGGER.log(e);

                    }
                }
                long timestamp = link.getLongProperty(YoutubeHelper.YT_DATE, -1);
                return timestamp > 0 ? formatter.format(timestamp) : "";
            }

        });
        REPLACER.add(new Replacer("date_time") {
            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_date_accurate();
            }

            public DataSource getDataSource() {
                return DataSource.API_VIDEOS;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                // date

                DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, TranslationFactory.getDesiredLocale());

                if (StringUtils.isNotEmpty(mod)) {
                    try {
                        formatter = new SimpleDateFormat(mod);
                    } catch (Throwable e) {
                        LOGGER.log(e);

                    }
                }
                long timestamp = link.getLongProperty(YoutubeHelper.YT_DATE, -1);
                String ret = timestamp > 0 ? formatter.format(timestamp) : "";

                return ret;
            }

        });
        // REPLACER.add(new Replacer("date_update") {
        // @Override
        // public String getDescription() {
        // return _GUI._.YoutubeHelper_getDescription_date_accurate();
        // }
        //
        // public DataSource getDataSource() {
        // return DataSource.API_VIDEOS;
        // }
        //
        // @Override
        // protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
        // // date
        //
        // DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, TranslationFactory.getDesiredLocale());
        //
        // if (StringUtils.isNotEmpty(mod)) {
        // try {
        // formatter = new SimpleDateFormat(mod);
        // } catch (Throwable e) {
        // LOGGER.log(e);
        //
        // }
        // }
        // long timestamp = link.getLongProperty(YoutubeHelper.YT_DATE_UPDATE, -1);
        // return timestamp > 0 ? formatter.format(timestamp) : "";
        // }
        //
        // });
        REPLACER.add(new Replacer("videoCodec") {
            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_videoCodec();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                // date
                String var = link.getStringProperty(YoutubeHelper.YT_VARIANT, "");
                YoutubeVariantInterface variant = helper.getVariantById(var);

                try {

                    if (variant instanceof YoutubeVariant) {
                        return ((YoutubeVariant) variant).getVideoCodec();
                    }
                    return "";
                } catch (Throwable e) {
                    // old variant
                    LOGGER.log(e);
                    return "[INVALID LINK!]";
                }
            }

        });
        REPLACER.add(new Replacer("resolution") {
            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_resolution();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                // date
                String var = link.getStringProperty(YoutubeHelper.YT_VARIANT, "");
                YoutubeVariantInterface variant = helper.getVariantById(var);

                try {

                    if (variant instanceof YoutubeVariant) {
                        return ((YoutubeVariant) variant).getResolution();
                    }
                    return "";
                } catch (Throwable e) {
                    // old variant
                    LOGGER.log(e);
                    return "[INVALID LINK!]";
                }
            }

        });
        REPLACER.add(new Replacer("bestResolution") {
            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_resolution_best();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                // date
                String var = link.getStringProperty(YoutubeHelper.YT_BEST_VIDEO, "");

                try {
                    return YoutubeITAG.valueOf(var).getQualityVideo();

                } catch (Throwable e) {
                    // old variant
                    LOGGER.log(e);
                    return "[INVALID LINK!]";
                }
            }

        });
        REPLACER.add(new Replacer("audioCodec") {
            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_audioCodec();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                // date
                String var = link.getStringProperty(YoutubeHelper.YT_VARIANT, "");
                YoutubeVariantInterface variant = helper.getVariantById(var);

                try {

                    if (variant instanceof YoutubeVariant) {
                        return ((YoutubeVariant) variant).getAudioCodec();
                    }
                    return "";
                } catch (Throwable e) {
                    // old variant
                    LOGGER.log(e);
                    return "[INVALID LINK!]";
                }
            }

        });

        REPLACER.add(new Replacer("audioQuality") {
            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_audioQuality();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                // date
                String var = link.getStringProperty(YoutubeHelper.YT_VARIANT, "");
                YoutubeVariantInterface variant = helper.getVariantById(var);

                try {

                    if (variant instanceof YoutubeVariant) {
                        return ((YoutubeVariant) variant).getAudioQuality();
                    }
                    return "";
                } catch (Throwable e) {
                    // old variant
                    LOGGER.log(e);
                    return "[INVALID LINK!]";
                }
            }

        });

        REPLACER.add(new Replacer("videonumber") {
            @Override
            public String getDescription() {
                return _GUI._.YoutubeHelper_getDescription_videonumber();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {

                // playlistnumber

                if (StringUtils.isEmpty(mod)) {
                    mod = "0000";
                }
                DecimalFormat df;
                try {
                    df = new DecimalFormat(mod);
                } catch (Throwable e) {
                    LOGGER.log(e);
                    df = new DecimalFormat("0000");
                }
                int playlistNumber = link.getIntegerProperty(YoutubeHelper.YT_PLAYLIST_INT, -1);
                return playlistNumber >= 0 ? df.format(playlistNumber) : "";
            }

        });
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