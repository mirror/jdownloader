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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "c-span.org" }, urls = { "https?://(?:www\\.)?c\\-span\\.org/video/\\?\\d+(?:\\-\\d+)?/[a-z0-9\\-]+" }) 
public class CspanOrg extends PluginForHost {

    public CspanOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.c-span.org/about/termsAndConditions/";
    }

    private static final String app = "cfx/st";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\\'og:title\\' content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = new Regex(link.getDownloadURL(), "([^/]+)$").getMatch(0);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // http://www.c-span.org/common/services/flashXml.php?programid=424926&version=2014-01-23
        final String progid = this.br.getRegex("name=\\'progid' value=\\'(\\d+)\\'").getMatch(0);
        if (progid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("http://www.c-span.org/common/services/flashXml.php?programid=" + progid);
        final String rtmp_host = this.br.getRegex("name=\"url\">\\$\\(protocol\\)(://[^<>\"]*?):\\$\\(port\\)/cfx/st").getMatch(0);
        final String playpath = this.br.getRegex("name=\"path\">(mp4:[^<>\"]*?)</string>").getMatch(0);
        if (playpath == null || rtmp_host == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String rtmpurl = "rtmp" + rtmp_host + "/" + app;
        try {
            dl = new RTMPDownload(this, downloadLink, rtmpurl);
        } catch (final NoClassDefFoundError e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
        }
        /* Setup rtmp connection */
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPageUrl(downloadLink.getDownloadURL());
        rtmp.setUrl(rtmpurl);
        rtmp.setPlayPath(playpath);
        rtmp.setApp(app);
        rtmp.setFlashVer("WIN 20,0,0,235");
        rtmp.setSwfUrl("http://static.c-span.org/assets/swf/CSPANPlayer.1434395986.swf?programid=" + progid);
        rtmp.setResume(true);
        ((RTMPDownload) dl).startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}