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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "reverbnation.com" }, urls = { "reverbnationcomid\\d+reverbnationcomartist\\d+" }, flags = { 0 })
public class ReverBnationComHoster extends PluginForHost {

    public ReverBnationComHoster(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.reverbnation.com/main/terms_and_conditions";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        String dllink = getDllink(link);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                return AvailableStatus.TRUE;
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String dllink = getDllink(downloadLink);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDllink(DownloadLink link) throws IOException, PluginException {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-Prototype-Version", "1.6.1");
        Regex infoRegex = new Regex(link.getDownloadURL(), "reverbnationcomid(\\d+)reverbnationcomartist(\\d+)");
        System.out.print("http://www.reverbnation.com/audio_player/add_to_beginning/" + infoRegex.getMatch(0) + "?from_page_object=artist_" + infoRegex.getMatch(1));
        br.postPage("http://www.reverbnation.com/audio_player/add_to_beginning/" + infoRegex.getMatch(0) + "?from_page_object=artist_" + infoRegex.getMatch(1), "");
        String damnString = br.getRegex("from_page_object=String_-(\\d+)\\\\\"").getMatch(0);
        if (damnString == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.reverbnation.com/controller/audio_player/get_tk");
        String crap = br.toString().trim();
        if (crap.length() > 300) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        System.out.print("http://www.reverbnation.com/controller/audio_player/liby/queue_" + infoRegex.getMatch(1) + "?from_page_object=String_-" + damnString + "&" + crap + "&bps=634519254827008");
        br.getPage("http://www.reverbnation.com/controller/audio_player/liby/queue_" + infoRegex.getMatch(1) + "?from_page_object=String_-" + damnString + "&" + crap + "&bps=634519254827008");
        // Hier müsste eigentlich ein redirect auf den finalen Link erfolgen
        System.out.print(br.getRedirectLocation());
        System.out.print(br.toString());
        String finallink = br.getRegex("Navigate\\.go_to_page_url\\(\\'(.*?)\\'").getMatch(0);
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        finallink = finallink.replace("tk%3D", "&tk=");
        return finallink;
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