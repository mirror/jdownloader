//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fleon.me" }, urls = { "http://(www\\.)?fleon\\.me/[a-z]+\\.php\\?Id=[0-9a-f]+" }, flags = { 32 })
public class FleonMe extends PluginForHost {

    private String DLLINK;

    public FleonMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("[a-z]+\\.php\\?Id=", "videos.php?Id="));
    }

    @Override
    public String getAGBLink() {
        return "http://fleon.me/tos.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        // Set name so even if its offline it has a nicer name
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "([0-9a-f]+)$").getMatch(0) + ".mp4");
        br.setFollowRedirects(true);
        String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        if (br.containsHTML(">404 File was removed") || br.getURL().endsWith("404.php") || !br.containsHTML("new SWFObject")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String flashUrl = br.getRegex("new SWFObject\\(\'/?([^\']+\\d+\\.swf)\',").getMatch(0);
        String fileName = br.getRegex("addVariable\\(\'file\',\'(.*?)\'\\)").getMatch(0);
        if (flashUrl == null || fileName == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!flashUrl.startsWith("http://")) flashUrl = "http://" + br.getHost() + "/" + flashUrl;
        try {
            DLLINK = getRtmpUrl(flashUrl);
        } catch (Throwable e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "JD2 BETA needed!");
        }
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = DLLINK + "@" + fileName + "@" + flashUrl + "@" + dllink;
        if (downloadLink.getBooleanProperty("MOVIE2K", false)) {
            if (!downloadLink.getName().endsWith(".mp4")) downloadLink.setName(downloadLink.getName() + ".mp4");
        } else {
            downloadLink.setName(fileName);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        download(downloadLink);
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPlayPath("mp4:" + stream[1]);
        rtmp.setUrl(stream[0]);
        rtmp.setSwfVfy(stream[2]);
        rtmp.setPageUrl(stream[3]);
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