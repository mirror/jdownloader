package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "redtube.com" }, urls = { "https?://(www\\.)?(redtube\\.(cn\\.com|com|tv|com\\.br)/|embed\\.redtube\\.(cn\\.com|com|tv|com\\.br)/[^<>\"]*?\\?id=)\\d+" })
public class RedTubeCom extends PluginForHost {

    public RedTubeCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private static final String  ALLOW_MULTIHOST_USAGE           = "ALLOW_MULTIHOST_USAGE";
    private static final boolean default_allow_multihoster_usage = false;

    private String               dllink                          = null;
    private boolean              server_issues                   = false;

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

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload("https://www.redtube.com/" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("https://www.redtube.com", "language", "en");
        br.getPage(link.getDownloadURL().toLowerCase());
        // Offline link
        if (br.containsHTML("is no longer available") || br.containsHTML(">404 Not Found<") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("class=\"video-deleted-info\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Invalid link
        if (br.containsHTML(">Error Page Not Found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fileName = br.getRegex("<h1 class=\"videoTitle[^>]+>(.*?)</h1>").getMatch(0);
        if (fileName == null) {
            fileName = br.getRegex("<title>(.*?) (-|\\|) RedTube[^<]+</title>").getMatch(0);
        }
        br.setFollowRedirects(true);
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
                                if (!con.getContentType().contains("html")) {
                                    link.setDownloadSize(br.getHttpConnection().getLongContentLength());
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
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        String ext = new Regex(dllink, "(\\.flv|\\.mp4).+$").getMatch(0);
        if (fileName != null || ext != null) {
            fileName = Encoding.htmlOnlyDecode(fileName);
            link.setName(fileName.trim() + ext);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

}