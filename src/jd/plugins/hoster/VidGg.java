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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vidgg.to", "vid.gg" }, urls = { "http://(?:www\\.)?(?:vid\\.gg|vidgg\\.to)/(?:video/|embed\\?id=)[a-z0-9]+", "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" })
public class VidGg extends PluginForHost {

    private static final String DOMAIN = "vidgg.to";

    public VidGg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.vid.gg/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String rewriteHost(String host) {
        if ("vid.gg".equals(getHost())) {
            if (host == null || "vid.gg".equals(host)) {
                return "vidgg.to";
            }
        }
        return super.rewriteHost(host);
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("http://www.vidgg.to/video/" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
    }

    /* Similar plugins: NovaUpMovcom, VideoWeedCom, NowVideoEu, MovShareNet, VidGg */
    @SuppressWarnings("deprecation")
    // This plugin is 99,99% copy the same as the DivxStageNet plugin, if this
    // gets broken please also check the other one!
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        setBrowserExclusive();
        /* Make sure to fix old urls too! */
        correctDownloadLink(downloadLink);
        br.getHeaders().put("Accept-Encoding", "identity");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(The file is beeing transfered to our other servers|This file no longer exists on our servers)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("Title: </strong>(.*?)</td>( <td>)?").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Watch ([^<>\"]*?) online \\| vidgg\\.to</title>").getMatch(0);
        }
        if (br.containsHTML("<h5[^<>]+>Untitled</h5>") && filename == null) {
            filename = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = filename.trim();
        if (filename.equals("Untitled") || filename.equals("Title")) {
            downloadLink.setFinalFileName("Video " + new Regex(downloadLink.getDownloadURL(), "vid\\.gg/video/(.+)$").getMatch(0) + ".mp4");
        } else {
            downloadLink.setFinalFileName(filename + (!filename.endsWith(".mp4") ? ".mp4" : ""));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /* 2016-12-12: Crypto stuff (and unwise js function) has been removed */
        String dllink = this.br.getRegex("<source[^>]*?src=\"(http[^\"]+)\"[^>]*?type=\\'video/mp4\\'[^>]*?>").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 410) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
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

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_VideoHosting;
    }

}