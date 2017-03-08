//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youporn.com" }, urls = { "http://(www\\.)?([a-z]{2}\\.)?youporn\\.com/watch/\\d+/?.+/?|https?://(?:www\\.)?youpornru\\.com/watch/\\d+/?.+/?" })
public class YouPornCom extends PluginForHost {

    /* DEV NOTES */
    /* Porn_plugin */

    String DLLINK = null;

    public YouPornCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private static final String  ALLOW_MULTIHOST_USAGE           = "ALLOW_MULTIHOST_USAGE";
    private static final boolean default_allow_multihoster_usage = false;

    private void setConfigElements() {
        String user_text;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            user_text = "Erlaube den Download von Links dieses Anbieters über Multihoster (nicht empfohlen)?\r\n<html><b>Kann die Anonymität erhöhen, aber auch die Fehleranfälligkeit!</b>\r\nAktualisiere deine(n) Multihoster Account(s) nach dem Aktivieren dieser Einstellung um diesen Hoster in der Liste der unterstützten Hoster deines/r Multihoster Accounts zu sehen (sofern diese/r ihn unterstützen).</html>";
        } else {
            user_text = "Allow links of this host to be downloaded via multihosters (not recommended)?\r\n<html><b>This might improve anonymity but perhaps also increase error susceptibility!</b>\r\nRefresh your multihoster account(s) after activating this setting to see this host in the list of the supported hosts of your multihost account(s) (in case this host is supported by your used multihost(s)).</html>";
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MULTIHOST_USAGE, JDL.L("plugins.hoster." + this.getClass().getName() + ".ALLOW_MULTIHOST_USAGE", user_text)).setDefaultValue(default_allow_multihoster_usage));
    }

    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        if (this.getPluginConfig().getBooleanProperty(ALLOW_MULTIHOST_USAGE, default_allow_multihoster_usage)) {
            return true;
        } else {
            return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        }
    }

    public String getAGBLink() {
        return "http://youporn.com/terms";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        final String fid = getFID(link.getDownloadURL());
        link.setLinkID(fid);
        link.setUrlDownload("http://www.youporn.com/watch/" + fid + "/" + System.currentTimeMillis() + "/");
    }

    private static final String defaultEXT = ".mp4";

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws IOException, PluginException {
        this.DLLINK = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://youporn.com/", "age_verified", "1");
        br.setCookie("http://youporn.com/", "is_pc", "1");
        br.setCookie("http://youporn.com/", "language", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        // Offline link
        if (br.containsHTML("<div id=\"video\\-not\\-found\\-related\"|watchRemoved\"|class=\\'video\\-not\\-found\\'")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Invalid link
        if (br.containsHTML("404 \\- Page Not Found<|id=\"title_404\"") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>(.*?) \\- Free Porn Videos \\- YouPorn</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?) Video \\- Youporn\\.com</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("addthis:title=\"YouPorn \\- (.*?)\"></a>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("\\'video_title\\' : \"([^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("videoTitle: \'([^<>\']*?)\'").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim().replaceAll("   ", "-");
        if (br.getURL().contains("/private/") || br.containsHTML("for=\"privateLogin_password\"")) {
            parameter.getLinkStatus().setStatusText("Password protected links are not yet supported, contact our support!");
            parameter.setName(filename + defaultEXT);
            return AvailableStatus.TRUE;
        }
        /* Find highest quality */
        final String[] qualities = { "1080", "720", "480", "240" };
        for (final String possibleQuality : qualities) {
            DLLINK = br.getRegex("sources\\s*?:.*?" + possibleQuality + "\\s*?:\\s*?\\'([^']+)\\'").getMatch(0);
            if (DLLINK != null) {
                break;
            }
        }
        if (DLLINK == null) {
            DLLINK = br.getRegex("\"(http://[^<>\"\\']+)\">MP4").getMatch(0);
        }
        if (DLLINK == null) {
            DLLINK = br.getRegex("\"(http://videos\\-\\d+\\.youporn\\.com/[^<>\"\\'/]+/save/scene_h264[^<>\"\\']+)\"").getMatch(0);
        }
        if (DLLINK == null) {
            DLLINK = br.getRegex("\"(http://cdn[a-z0-9]+\\.public\\.youporn\\.phncdn\\.com/[^<>\"]*?)\"").getMatch(0);
        }
        if (DLLINK == null) {
            DLLINK = br.getRegex("<ul class=\"downloadList\">.*?href=\"(http://[^\"]+)\">.*?</ul>").getMatch(0);
        }
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = Encoding.htmlDecode(DLLINK);
        parameter.setFinalFileName(filename + defaultEXT);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                parameter.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (br.getURL().contains("/private/") || br.containsHTML("for=\"privateLogin_password\"")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected links are not yet supported, contact our support!");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFID(final String downloadurl) {
        return new Regex(downloadurl, "/watch/(\\d+)").getMatch(0);
    }

    public void reset() {
    }

    public void resetDownloadlink(final DownloadLink link) {
    }

}