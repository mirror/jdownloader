//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sexseevideo.net" }, urls = { "http://(www\\.)?sexseevideo\\.net/\\d+(/[a-z0-9\\-]+)?" }, flags = { 0 })
public class SexSeeVideoNet extends PluginForHost {

    public SexSeeVideoNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.sexseevideo.net/static/terms/";
    }

    private String FILENAME = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/user/signup/")) {
            FILENAME = new Regex(link.getDownloadURL(), "sexseevideo\\.net/\\d+/([a-z0-9\\-]+)").getMatch(0);
            if (FILENAME == null) FILENAME = getFUID(link);
        } else {
            FILENAME = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (FILENAME == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(FILENAME.trim()) + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // 2 possible ways, we use the embed way, it's less obvious
        // br.getPage("http://www.sexseevideo.net/modules/video/player/config.php?id=" + getFUID(downloadLink));
        br.getPage("http://www.sexseevideo.net/modules/video/player/config_iframe.php?id=" + getFUID(downloadLink));
        String dllink = br.getRegex("\\'url\\': \\'(http://[^<>\"]*?)\\'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFUID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "sexseevideo\\.net/(\\d+)").getMatch(0);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}