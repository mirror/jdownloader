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

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ecostream.tv" }, urls = { "http://(www\\.)?ecostream\\.tv/(stream|embed)/[a-z0-9]{32}\\.html" }, flags = { 0 })
public class EcoStreamTv extends PluginForHost {

    public EcoStreamTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/embed/", "/stream/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.ecostream.tv/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String NOCHUNKS = "NOCHUNKS";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(getfid(downloadLink) + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String tmp = br.getRegex("var anlytcs=\\'([^<>\"]*?)\\';").getMatch(0);
        final String noSenseForThat = br.getRegex("var adslotid=\\'([^<>\"]*?)\\';").getMatch(0);
        if (tmp == null || noSenseForThat == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://www.ecostream.tv/xhr/video/vidurl", "id=" + getfid(downloadLink) + "&tpm=" + tmp + noSenseForThat);
        String finallink = br.getRegex("\"url\":\"(/[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        finallink = "http://www.ecostream.tv" + finallink;
        int maxChunks = 0;
        if (downloadLink.getBooleanProperty(EcoStreamTv.NOCHUNKS, false)) maxChunks = 1;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(EcoStreamTv.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(EcoStreamTv.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(EcoStreamTv.NOCHUNKS, false) == false) {
                downloadLink.setProperty(EcoStreamTv.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    private String getfid(final DownloadLink downloadLink) {
        return new Regex(downloadLink.getDownloadURL(), "ecostream\\.tv/stream/([a-z0-9]{32})\\.html").getMatch(0);
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