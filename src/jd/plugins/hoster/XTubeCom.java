//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xtube.com" }, urls = { "https?://(www\\.)?xtube\\.com/(video-watch/|(watch|play_re)\\.php\\?v=)[A-Za-z0-9_\\-]+" })
public class XTubeCom extends PluginForHost {
    private String              DLLINK   = null;
    private static final String MAINPAGE = "http://www.xtube.com";

    public XTubeCom(PluginWrapper wrapper) {
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        if (this.getPluginConfig().getBooleanProperty(ALLOW_MULTIHOST_USAGE, default_allow_multihoster_usage)) {
            return true;
        } else {
            return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        }
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("play_re", "watch"));
    }

    @Override
    public String getAGBLink() {
        return "http://wiki2.xtube.com/index.php?title=Terms_of_Use&action=purge";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        // Offline links should also have nice filenames
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9_\\-]+)$").getMatch(0));
        this.setBrowserExclusive();
        br.setCookie(MAINPAGE, "cookie_warning", "deleted");
        br.setCookie(MAINPAGE, "cookie_warning", "S");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("msg=Invalid+Video+ID") || br.containsHTML(">Video not available<|img/removed_video|>This video has been removed from XTube") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        if (br.getURL().contains("play.php?preview_id=")) {
            filename = br.getRegex("class=\"sectionNoStyleHeader\">([^<>\"]*?)</div>").getMatch(0);
        } else {
            filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
            // For DVD preview links
            if (filename == null) {
                filename = br.getRegex("id=\"videoDetails\">[\t\n\r ]+<p class=\"title\">([^<>\"]*?)</p>").getMatch(0);
            }
        }
        String ownerName = br.getRegex("\\?field_subscribe_user_id=([^<>\"]*?)\"").getMatch(0);
        if (ownerName == null) {
            ownerName = "undefined";
        }
        /* Find highest quality available! */
        for (int i = 2000; i >= 100; i--) {
            DLLINK = this.br.getRegex("\"" + i + "\":\"(http[^<>\"]+)\"").getMatch(0);
            if (DLLINK != null) {
                DLLINK = DLLINK.replace("\\", "");
                break;
            }
        }
        if (DLLINK == null) {
            /* Try the old way. */
            String fileID = new Regex(downloadLink.getDownloadURL(), "xtube\\.com/watch\\.php\\?v=(.+)").getMatch(0);
            if (fileID == null) {
                fileID = br.getRegex("contentId\" value=\"([^\"]+)\"").getMatch(0);
            }
            if (fileID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.postPage("http://www.xtube.com/find_video.php", "user%5Fid=" + Encoding.urlEncode(ownerName) + "&clip%5Fid=&video%5Fid=" + Encoding.urlEncode(fileID));
            DLLINK = br.getRegex("\\&filename=(http.*?)($|\r|\n| )").getMatch(0);
            if (DLLINK == null) {
                DLLINK = br.getRegex("\\&filename=(%2Fvideos.*?hash.+)").getMatch(0);
            }
        }
        if (filename == null || DLLINK == null || DLLINK.length() > 500) {
            logger.info("filename: " + filename + ", DLLINK: " + DLLINK);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (DLLINK.contains("/notfound")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        DLLINK = (Encoding.htmlDecode(DLLINK));
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".mp4");
        br.setDebug(true);
        Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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