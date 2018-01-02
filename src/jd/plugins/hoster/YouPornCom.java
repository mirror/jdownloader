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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youporn.com" }, urls = { "https?://(www\\.)?([a-z]{2}\\.)?youporn\\.com/watch/\\d+/?.+/?|https?://(?:www\\.)?youpornru\\.com/watch/\\d+/?.+/?" })
public class YouPornCom extends PluginForHost {
    /* DEV NOTES */
    /* Porn_plugin */
    String          dllink             = null;
    private boolean server_issues      = false;
    private boolean temporarilyBlocked = false;

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
        link.setUrlDownload("https://www.youporn.com/watch/" + fid + "/" + System.currentTimeMillis() + "/");
    }

    private static final String defaultEXT = ".mp4";

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws IOException, PluginException {
        this.dllink = null;
        this.server_issues = false;
        this.temporarilyBlocked = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://youporn.com/", "age_verified", "1");
        br.setCookie("http://youporn.com/", "is_pc", "1");
        br.setCookie("http://youporn.com/", "language", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        if (br.containsHTML("<div id=\"video\\-not\\-found\\-related\"|watchRemoved\"|class=\\'video\\-not\\-found\\'")) {
            /* Offline link */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("404 \\- Page Not Found<|id=\"title_404\"") || this.br.getHttpConnection().getResponseCode() == 404) {
            /* Invalid link */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("onload=\"go\\(\\)\"")) {
            /* 2017-07-26: TODO: Maybe follow that js redirect */
            logger.info("Temporarily blocked because of too many requests");
            this.temporarilyBlocked = true;
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = br.getRegex("<title>(.*?) \\- Free Porn Videos[^<>]+</title>").getMatch(0);
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
        int qualityMax = 0;
        int qualityTemp = 0;
        String qualityTempStr;
        /* Must not be present */
        String filesize = null;
        final String[] htmls = br.getRegex("class='callBox downloadOption[^~]*?downloadVideoLink clearfix'([^~]*?)</span>").getColumn(0);
        for (final String html : htmls) {
            qualityTempStr = new Regex(html, "(\\d+)p_\\d+k").getMatch(0);
            if (qualityTempStr == null) {
                continue;
            }
            qualityTemp = Integer.parseInt(qualityTempStr);
            if (qualityTemp > qualityMax) {
                qualityMax = qualityTemp;
                dllink = new Regex(html, "(https?://[^'\"]+\\d+p[^'\"]+\\.mp4[^\\'\"\\|]+)").getMatch(0);
                if (dllink != null) {
                    /* Only attempt to grab filesize if it corresponds to the current videoquality! */
                    filesize = new Regex(html, "class=\\'downloadsize\\'>\\((\\d+[^<>\"]+)\\)").getMatch(0);
                }
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://[^<>\"\\']+)\">MP4").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://videos\\-\\d+\\.youporn\\.com/[^<>\"\\'/]+/save/scene_h264[^<>\"\\']+)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://cdn[a-z0-9]+\\.public\\.youporn\\.phncdn\\.com/[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("<ul class=\"downloadList\">.*?href=\"(https?://[^\"]+)\">.*?</ul>").getMatch(0);
        }
        if (dllink != null) {
            /* Do NOT htmldecode! */
            dllink = dllink.replace("&amp;", "&");
        }
        parameter.setFinalFileName(filename + defaultEXT);
        if (filesize != null) {
            parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        } else if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    parameter.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (br.getURL().contains("/private/") || br.containsHTML("for=\"privateLogin_password\"")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected links are not yet supported, contact our support!");
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (this.temporarilyBlocked) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Temporarily blocked because of too many requests", 5 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
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