package jd.plugins.hoster;

import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.RedtubeConfig;
import org.jdownloader.plugins.components.config.RedtubeConfig.PreferredStreamQuality;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "redtube.com" }, urls = { "https?://(?:www\\.|[a-z]{2}\\.)?(?:redtube\\.(?:cn\\.com|com|tv|com\\.br)/|embed\\.redtube\\.(?:cn\\.com|com|tv|com\\.br)/[^<>\"]*?\\?id=)(\\d{4,})" })
public class RedTubeCom extends PluginForHost {
    public RedTubeCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private static final String  ALLOW_MULTIHOST_USAGE           = "ALLOW_MULTIHOST_USAGE";
    private final String         PROPERTY_USERNAME               = "username";
    private static final boolean default_allow_multihoster_usage = false;
    private String               dllink                          = null;
    private boolean              server_issues                   = false;
    private boolean              private_video                   = false;

    private void setConfigElements() {
        String user_text;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            user_text = "Erlaube den Download von Links dieses Anbieters über Multihoster (nicht empfohlen)?\r\n<html><b>Kann die Anonymität erhöhen, aber auch die Fehleranfälligkeit!</b>\r\nAktualisiere deine(n) Multihoster Account(s) nach dem Aktivieren dieser Einstellung um diesen Hoster in der Liste der unterstützten Hoster deines/r Multihoster Accounts zu sehen (sofern diese/r ihn unterstützen).</html>";
        } else {
            user_text = "Allow links of this host to be downloaded via multihosters (not recommended)?\r\n<html><b>This might improve anonymity but perhaps also increase error susceptibility!</b>\r\nRefresh your multihoster account(s) after activating this setting to see this host in the list of the supported hosts of your multihost account(s) (in case this host is supported by your used multihost(s)).</html>";
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MULTIHOST_USAGE, JDL.L("plugins.hoster." + this.getClass().getName() + ".ALLOW_MULTIHOST_USAGE", user_text)).setDefaultValue(default_allow_multihoster_usage));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        if (this.getPluginConfig().getBooleanProperty(ALLOW_MULTIHOST_USAGE, default_allow_multihoster_usage)) {
            return true;
        } else {
            return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.redtube.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload("https://www.redtube.com/" + getFID(link));
    }

    @SuppressWarnings("unchecked")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".mp4");
        }
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie(this.getHost(), "language", "en");
        br.getPage(link.getPluginPatternMatcher().toLowerCase());
        // Offline link
        if (br.containsHTML("is no longer available") || br.containsHTML(">\\s*404 Not Found<") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class=\"video-deleted-info\"") || br.containsHTML("class=\"unavailable_text\"") || br.containsHTML(">\\s*This video has been removed")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Removed video");
        } else if (br.containsHTML(">\\s*Video has been flagged for verification")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Flagged video");
        }
        // Invalid link
        if (br.containsHTML(">Error Page Not Found|<title>Kostenlose Porno Sexvideos")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String channelname = br.getRegex("class=\"video-infobox-link\" href=\"/(?:users|channels)/([^\"/]+)").getMatch(0);
        if (channelname != null) {
            /* Packagizer property */
            link.setProperty(PROPERTY_USERNAME, Encoding.htmlDecode(channelname).trim());
        }
        private_video = this.br.containsHTML("class=\"private_video_text\"");
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        br.setFollowRedirects(true);
        final String playervars = br.getRegex("playervars: (.+?\\}),\n").getMatch(0);
        if (playervars != null) {
            final Map<String, Object> values = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(playervars);
            List<Map<String, Object>> list = (List<Map<String, Object>>) values.get("mediaDefinitions");
            for (Map<String, Object> entry : list) {
                final String videoUrl = (String) entry.get("videoUrl");
                final String format = (String) entry.get("format");
                if (StringUtils.isEmpty(videoUrl)) {
                    continue;
                } else if (StringUtils.equals("mp4", format)) {
                    final Browser brc = br.cloneBrowser();
                    brc.getPage(videoUrl);
                    list = (List<Map<String, Object>>) restoreFromString(brc.toString(), TypeRef.OBJECT);
                    break;
                }
            }
            final String userPreferredQuality = getPreferredStreamQuality();
            if (list != null) {
                int qualityMax = 0;
                for (Object entry : list) {
                    Map<String, Object> video = (Map<String, Object>) entry;
                    final String videoUrl = (String) video.get("videoUrl");
                    final Object quality = video.get("quality");
                    if (StringUtils.isEmpty(videoUrl)) {
                        continue;
                    } else if (quality == null) {
                        continue;
                    }
                    final Number fileSize = (Number) video.get("videoSize");
                    final String qualityTempStr = (String) quality;
                    if (StringUtils.equals(qualityTempStr, userPreferredQuality)) {
                        logger.info("Found user preferred quality: " + userPreferredQuality);
                        if (fileSize != null) {
                            link.setDownloadSize(fileSize.longValue());
                        }
                        dllink = videoUrl;
                        break;
                    }
                    final int qualityTemp = Integer.parseInt(qualityTempStr);
                    if (qualityTemp > qualityMax) {
                        if (fileSize != null) {
                            link.setDownloadSize(fileSize.longValue());
                        }
                        qualityMax = qualityTemp;
                        dllink = videoUrl;
                    }
                }
                if (dllink != null) {
                    final Browser brc = br.cloneBrowser();
                    URLConnectionAdapter con = null;
                    try {
                        con = brc.openHeadConnection(dllink);
                        if (looksLikeDownloadableContent(con)) {
                            if (con.getCompleteContentLength() > 0) {
                                link.setDownloadSize(con.getCompleteContentLength());
                            }
                        } else {
                            server_issues = true;
                        }
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }
                }
            }
        }
        if (dllink == null) {
            // old handling
            dllink = br.getRegex("source src=\"(http.*?)(\"|%3D%22)").getMatch(0);
            if (dllink != null && dllink.contains("&amp;")) {
                dllink = dllink.replace("&amp;", "&");
            }
            if (dllink == null) {
                dllink = br.getRegex("flv_h264_url=(http.*?)(\"|%3D%22)").getMatch(0);
                if (dllink == null) {
                    final String json = PluginJSonUtils.getJsonNested(br, "sources");
                    if (json != null) {
                        URLConnectionAdapter con = null;
                        String[] qualities = { "1080", "720", "480", "240" };
                        for (String quality : qualities) {
                            dllink = PluginJSonUtils.getJsonValue(json, quality);
                            // logger.info("dllink: " + dllink);
                            if (dllink != null) {
                                dllink = Encoding.urlDecode(dllink, true);
                                if (dllink.startsWith("//")) {
                                    dllink = "http:" + dllink;
                                }
                                try {
                                    con = br.openHeadConnection(dllink);
                                    if (this.looksLikeDownloadableContent(con)) {
                                        if (con.getCompleteContentLength() > 0) {
                                            link.setDownloadSize(con.getCompleteContentLength());
                                        }
                                        break;
                                    } else if (quality == "240") {
                                        server_issues = true;
                                    }
                                } finally {
                                    try {
                                        con.disconnect();
                                    } catch (final Throwable e) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (dllink == null && br.containsHTML("<source src=\"\" type=\"video/mp4\">")) {
            /* 2017-03-11 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            String ext = new Regex(dllink, "(\\.flv|\\.mp4).+$").getMatch(0);
            if (ext == null) {
                ext = ".mp4";
            }
            link.setFinalFileName(filename + ext);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (private_video) {
            throw new AccountRequiredException("Private video");
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    private String getPreferredStreamQuality() {
        final RedtubeConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final PreferredStreamQuality quality = cfg.getPreferredStreamQuality();
        switch (quality) {
        case BEST:
        default:
            return null;
        case Q2160P:
            return "2160";
        case Q1080P:
            return "1080";
        case Q720P:
            return "720";
        case Q480P:
            return "480";
        case Q360P:
            return "360";
        case Q240P:
            return "240";
        }
    }

    @Override
    public Class<? extends RedtubeConfig> getConfigInterface() {
        return RedtubeConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}