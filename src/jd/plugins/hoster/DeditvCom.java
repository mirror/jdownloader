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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deditv.com" }, urls = { "http://(www\\.)?deditv\\.com/play\\.php\\?v=[0-9a-f]+" }, flags = { 32 })
public class DeditvCom extends PluginForHost {

    private String DLLINK;

    public DeditvCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://deditv.com/term.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPlayPath("mp4:" + stream[1]);
        rtmp.setUrl(stream[0]);
        rtmp.setSwfVfy(stream[2]);
        rtmp.setPageUrl(stream[3]);
        rtmp.setTimeOut(10);
        rtmp.setResume(true);
    }

    private void download(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String[] stream = DLLINK.split("@");
        if (DLLINK.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, stream[0] + stream[1]);
            setupRTMPConnection(stream, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        if (br.containsHTML(">404 error , this file do not exist any more") || br.getURL().endsWith("removed.html")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        br.getPage(dllink);

        String flashUrl = br.getRegex("new SWFObject\\(\'([\\./]+)?(\\d+\\.swf)\',").getMatch(1);
        String fileName = br.getRegex("addVariable\\(\'file\',\'(.*?)\'\\)").getMatch(0);
        if (flashUrl == null || fileName == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        flashUrl = "http://" + br.getHost() + "/" + flashUrl;
        try {
            DLLINK = getRtmpUrl(flashUrl);
        } catch (Throwable e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Developer Version of JD or JD2Beta needed!");
        }
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = DLLINK + "@" + fileName + "@" + flashUrl + "@" + dllink;
        downloadLink.setName(fileName);
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

    private String getRtmpUrl(String flashurl) throws Exception {
        String[] args = { "-abc", flashurl };
        // disassemble abc
        String asasm = flash.swf.tools.SwfxPrinter.main(args);
        String doABC = new Regex(asasm, "<doABC2>(.*)</doABC2>").getMatch(0);
        for (String method : new Regex(doABC, "(function.+?Traits Entries)").getColumn(0)) {
            if (method.startsWith("function com.longtailvideo.jwplayer.utils:Configger::com.longtailvideo.jwplayer.utils:Configger()")) {
                if (method.contains("streamer")) { return new Regex(method, "\"(rtmp.*?/)\"").getMatch(0); }
            }
        }
        return null;
    }

}