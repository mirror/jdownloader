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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wat.tv" }, urls = { "http://(www\\.)?wat\\.tv/video/.*?\\.html" }, flags = { 0 })
public class WatTv extends PluginForHost {

    public WatTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.wat.tv/cgu";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://www.wat.tv/") || br.containsHTML("<title> WAT TV, vidéos replay musique et films, votre média vidéo \\– Wat\\.tv </title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null || filename.equals("")) filename = br.getRegex("<meta name=\"name\" content=\"(.*?)\"").getMatch(0);
        if (filename == null || filename.equals("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim() + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // TODO:Some videosources are RMTP Streams, at the moment this ones
        // can't be downloaded, example:
        // http://www.wat.tv/video/je-suis-belle-je-assume-1zzzj_10zxx_.html
        String finallink = getFinalLink();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl.startDownload();
    }

    public String getFinalLink() throws Exception {
        String videoid = br.getRegex("id=\"media\" value=\"(.*?)\"").getMatch(0);
        if (videoid == null) {
            videoid = br.getRegex("videoId : \"(.*?)\"").getMatch(0);
            if (videoid == null) {
                videoid = br.getRegex("mediaId=(.*?)\\'").getMatch(0);
                if (videoid == null) {
                    videoid = br.getRegex("abuse/video/(.*?)'").getMatch(0);
                }
            }
        }
        if (videoid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser br2 = br.cloneBrowser();
        // Scheinbar unwichtig, war der frühere Weg um an den Downloadlink zu
        // kommen!
        br2.getPage("http://www.wat.tv/interface/contentv3/" + videoid);
        br2.getPage("http://www.wat.tv/get/web/" + videoid + "?token=" + "token4bismarck" + "/" + "tokenTwo4bismarck" + "&domain=www.wat.tv&domain2=www.wat.tv&revision=4.1.017&synd=0&helios=1&context=swf2&pub=5&country=DE&sitepage=WAT%2Ftv%2Fkoh-lanta-9&lieu=wat&playerContext=CONTEXT_WAT&getURL=1&version=WIN%2010,3,183,7");
        String getVideoLink = br2.toString();
        if (!getVideoLink.startsWith("http")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        getVideoLink = Encoding.htmlDecode(getVideoLink.trim());
        URLConnectionAdapter con = br2.openGetConnection(getVideoLink);
        if (con.getResponseCode() == 404 || con.getResponseCode() == 403) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.WatTv.CountryBlocked", "This video isn't available in your country!"));
        br2.followConnection();
        String finallink = br2.toString().trim();
        if (!finallink.startsWith("http://")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return finallink;
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
