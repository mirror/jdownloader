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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "online.nolife-tv.com" }, urls = { "http://(www\\.)?online\\.nolife\\-tv\\.com/index\\.php\\?id=\\d+" }, flags = { 0 })
public class OnlineNoLifeTvCom extends PluginForHost {

    public OnlineNoLifeTvCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://online.nolife-tv.com/";
    }

    private String              DLLINK              = null;
    private static final String ONLYPREMIUMUSERTEXT = "Only downloadable for premium members";
    private boolean             notDownloadable     = false;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://online.nolife-tv.com/index.php") || br.containsHTML("<title>Nolife Online</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div id=\"ligne_titre_big\" style=\"margin\\-top:10px;\">(.*?)</div><div").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Nolife Online \\- (.*?)( \\- [A-Za-z]+ \\d+)?</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = Encoding.htmlDecode(filename);
        if (!br.containsHTML("flashvars")) {
            notDownloadable = true;
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.onlinenolifetvcom.only4premium", ONLYPREMIUMUSERTEXT));
            downloadLink.setName(filename + ".mp4");
            return AvailableStatus.TRUE;
        } else {
            br.postPage("http://online.nolife-tv.com/_info.php", "identity=null&hq=0&id%5Fnlshow=" + new Regex(downloadLink.getDownloadURL(), "online\\.nolife\\-tv\\.com/index\\.php\\?id=(\\d+)").getMatch(0));
            DLLINK = br.getRegex("url=(.*?)\\&hq=0\\&preview").getMatch(0);
            if (DLLINK == null || DLLINK.equals("")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = nolifeDecode(Encoding.htmlDecode(DLLINK));
            if (DLLINK == null || !DLLINK.startsWith("http")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = Encoding.htmlDecode(DLLINK);
            filename = filename.trim();
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext == null) ext = ".mp4";
            downloadLink.setFinalFileName(filename + ext);
            Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html"))
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
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (notDownloadable) throw new PluginException(LinkStatus.ERROR_FATAL, ONLYPREMIUMUSERTEXT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String nolifeDecode(final String input) {
        String result = "";
        final String key = "s8_dg2x-5sd1";
        int i = 0;
        int j = 0;
        while (j < input.length()) {
            i = input.codePointAt(j) ^ key.codePointAt(j % key.length());
            result += String.valueOf((char) i);
            j += 1;
        }
        return result;
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