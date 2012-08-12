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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tudou.com" }, urls = { "http://(www\\.)?tudou\\.com/programs/view/[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class TuDouCom extends PluginForHost {

    private String dllink = null;

    public TuDouCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.tudou.com/about/agreement.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("\"notfound\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(",kw = \"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"vcate_title\">(.*?)</span>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("id=\"playerBox\"><h1>(.*?)</h1><div class=\"player\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("id=\"videoDes\">(.*?)</div></div>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("\"http://so\\.tudou\\.com/isearch\\.do\\?type=relative\\&kw=(.*?)\"").getMatch(0);
                    }
                }
            }
        }
        String videoID = new Regex(downloadLink.getDownloadURL(), "tudou\\.com/programs/view/(.+)").getMatch(0);
        String iid = br.getRegex("var iid = (\\d+)").getMatch(0);
        if (iid == null) iid = br.getRegex("supermail\\.tudou\\.com/supermail/index\\.action\\?iid=(\\d+)\"").getMatch(0);
        if (iid == null) iid = br.getRegex("var pageID.*?,iid = (\\d+)").getMatch(0);
        if (videoID == null || iid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String xmllink = "http://v2.tudou.com/v?vn=02&refurl=http://www.tudou.com/programs/view/" + videoID + "/&it=" + iid + "&noCache=&ui=0&st=1,2&si=sp&tAg=";
        br.getPage(xmllink);
        System.out.print(br.toString());
        dllink = br.getRegex("</f><f st=\"2\" s1=\"[a-z0-9]+\" bc=\"10\" brt=\"2\">(http://.*?)</f></v><\\!--pageview_candidate-->").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("brt=\"2\".*?(http://.*?)<").getMatch(0);
            if (dllink == null) dllink = br.getRegex("brt=\"1\".*?(http://.*?)<").getMatch(0);
        }
        if (filename == null || dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".flv");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html") && !con.getContentType().contains("text"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}