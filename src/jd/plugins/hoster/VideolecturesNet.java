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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videolectures.net" }, urls = { "http://[\\w\\.]*?videolectures\\.net/.+" }, flags = { 32 })
public class VideolecturesNet extends PluginForHost {

    private String clipUrl              = null;
    private String clipNetConnectionUrl = null;

    public VideolecturesNet(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://videolectures.net/site/terms_of_use/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String swfUrl = br.getRegex("flowplayer_swf:\\s\"(.*?)\",").getMatch(0);
        final String app = clipNetConnectionUrl.substring(clipNetConnectionUrl.lastIndexOf("/") + 1);
        if (swfUrl == null || app == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        final String dllink = clipNetConnectionUrl + "/" + clipUrl;

        if (dllink.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, dllink);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setPlayPath(clipUrl);
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setSwfVfy(swfUrl);
            rtmp.setFlashVer("WIN 10,1,102,64");
            rtmp.setApp(app);
            rtmp.setUrl(clipNetConnectionUrl);
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
        String filename = br.getRegex("<title>(.*?)\\s-.*?</title>").getMatch(0).trim();
        if (filename == null) {
            filename = br.getRegex("name=\"title\"\\scontent=\"(.*?)\"").getMatch(0);
        }
        clipUrl = br.getRegex("\\s+clip.url\\s=\\s\"(.*?)\"").getMatch(0);
        clipNetConnectionUrl = br.getRegex("clip.netConnectionUrl\\s=\\s\"(.*?)\"").getMatch(0);
        if (filename == null || clipUrl == null || clipNetConnectionUrl == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setFinalFileName(filename + ".flv");
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