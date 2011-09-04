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

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tvi.iol.pt", "tvi24.iol.pt" }, urls = { "http://(www\\.)?tvi\\.iol\\.pt/mediacenter\\.html\\?(load=\\d+\\&gal_id=\\d+|mul_id=\\d+\\&load=\\d+&pagina=\\d+\\&pos=\\d+)", "http://(www\\.)?tvi24\\.iol\\.pt/aa---videos---[\\w-]+/[\\w-]+/\\d+-\\d+\\.html" }, flags = { PluginWrapper.DEBUG_ONLY, PluginWrapper.DEBUG_ONLY })
public class TviIolPt extends PluginForHost {

    private String clipUrl              = null;
    private String clipNetConnectionUrl = null;

    public TviIolPt(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://fizy.com/about";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String swfUrl = "http://www.tvi.iol.pt/flashplayers/player-52.swf";
        final String dllink = clipNetConnectionUrl + "/" + clipUrl;

        if (dllink.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, dllink);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setPlayPath(clipUrl);
            rtmp.setSwfVfy(swfUrl);
            rtmp.setUrl(clipNetConnectionUrl);
            rtmp.setTimeOut(-1);
            rtmp.setResume(true);

            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        final String id = br.getRegex("createPlayer\\((\\n\\s+)?(\\d+),").getMatch(1);
        if (id == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = null;
        if (br.getHost().contains("tvi24")) {
            filename = br.getRegex("<title>(.*?)>").getMatch(0);
        } else {
            filename = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        }
        if (filename == null) {
            if (br.getHost().contains("tvi24")) {
                filename = br.getRegex("<div class=\"article-promo\">.*?<h1>(.*?)</h1>").getMatch(0);
            } else {
                filename = br.getRegex("<h1><span>.*?-.*?- (.*?)</span></h1>").getMatch(0);
            }
        }
        br.getPage("http://www.tvi.iol.pt/config.html?id=" + id);
        clipUrl = br.getRegex("file\":\"(.*?)\"").getMatch(0);
        clipNetConnectionUrl = br.getRegex("baseURL\":\\s+?\"(.*?)\"").getMatch(0);
        if (filename == null || clipUrl == null || clipNetConnectionUrl == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setFinalFileName(filename.trim() + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
