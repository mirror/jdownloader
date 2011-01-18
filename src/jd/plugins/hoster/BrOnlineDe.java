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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "br-online.de" }, urls = { "http://[\\w\\.]*?br-online\\.de/(?!podcast).*?/.*?\\.xml" }, flags = { 0 })
public class BrOnlineDe extends PluginForHost {

    public BrOnlineDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.br-online.de/unternehmen/kontakt/br-ansprechpartner-DID1236676889527/index.xml";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"topheadline\">.*?<em>.*?</em>(.*?)</h3>").getMatch(0);
        if (filename == null) filename = br.getRegex("class=\"videoText\"><p><strong>.*?</strong>(.*?)<span>").getMatch(0);
        dllink = br.getRegex("player\\.avaible_url\\['flashmedia'\\]\\['(1|2)'\\] = \"(http://.*?\\.mp3)\";").getMatch(1);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://gffstream\\.vo\\.llnwd\\.net/o\\d+/br/mir-live/[a-zA-Z0-9]+/[a-zA-Z0-9]+/[a-zA-Z0-9]+/iLCpbHJG/[a-zA-Z0-9]+/.*?\\.mp3)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("player\\.(avaible_url|dl_url)\\['microsoftmedia'\\]\\['(1|2)'\\] = \"(http://.*?\\.wmv)\"").getMatch(2);
                if (dllink == null) dllink = br.getRegex("\"(http://gffstream\\.vo\\.llnwd\\.net/o\\d+/br/.*?/[a-zA-Z0-9]+/[a-zA-Z0-9]+/[a-zA-Z0-9]+/[a-zA-Z0-9]+/[a-zA-Z0-9]+/.*?\\.wmv)\"").getMatch(0);
            }
        }
        if (filename == null || dllink == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = filename.trim().replaceAll("(\\?|\\&quot;)", "");
        if (!dllink.endsWith(".mp3"))
            downloadLink.setFinalFileName(filename + ".wmv");
        else
            downloadLink.setFinalFileName(filename + ".mp3");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = br2.openGetConnection(dllink);
        if (!con.getContentType().contains("html"))
            downloadLink.setDownloadSize(con.getLongContentLength());
        else
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
