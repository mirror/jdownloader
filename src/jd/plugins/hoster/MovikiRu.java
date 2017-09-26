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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "moviki.ru" }, urls = { "https?://(?:www\\.)?moviki\\.ru/(?:embed|videos)/\\d+(.+)?" })
public class MovikiRu extends PluginForHost {
    public MovikiRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Extension which will be used if no correct extension is found */
    private static final String default_extension = ".mp4";
    private String              dllink            = null;
    private boolean             server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://www.moviki.ru/terms.php";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        final String fid = new Regex(link.getDownloadURL(), "moviki\\.ru/[^/]+/(\\d+)").getMatch(0);
        link.setUrlDownload("http://www.moviki.ru/videos/" + fid + "/x/");
        link.setLinkID(fid);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        final String fid = new Regex(downloadLink.getDownloadURL(), "moviki\\.ru/(?:embed|videos)/(\\d+)").getMatch(0);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("404.php") || br.containsHTML(">window\\.location=\\'/404\\.php\\'|Это личное видео")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"block_header\">([^<>\"]*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div class=\"headline\">[\t\n\r ]*?<h2>([^<>\"]+)</h2>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("([^<>\"]+)").getMatch(0);
        }
        if (filename == null) {
            filename = fid;
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim());
        if (br.containsHTML("Это личное видео пользователя <a")) {
            downloadLink.getLinkStatus().setStatusText("Private video!");
            downloadLink.setName(filename + ".mp4");
            return AvailableStatus.TRUE;
        }
        dllink = jd.plugins.hoster.CamwhoresTv.getDllinkCrypted(this.br);
        String ext = null;
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.htmlDecode(dllink);
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        }
        if (ext == null) {
            ext = default_extension;
        }
        downloadLink.setFinalFileName(filename + ext);
        if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Это личное видео пользователя <a")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        /* 2017-04-28: The final downloadlink is only valid once - we have to create a new one to be able to download the file. */
        this.br.clearCookies(this.br.getURL());
        br.getPage(downloadLink.getDownloadURL());
        dllink = jd.plugins.hoster.CamwhoresTv.getDllinkCrypted(this.br);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KernelVideoSharing;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
