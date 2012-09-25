//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adrive.com" }, urls = { "https?://(www\\.)?adrive\\.com/public/[0-9a-zA-Z]+" }, flags = { 0 })
public class AdriveCom extends PluginForHost {

    public AdriveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.adrive.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String goToLink = br.getRegex("<b>Please go to <a href=\"(/.*?)\"").getMatch(0);
        if (goToLink != null) br.getPage("http://www.adrive.com" + goToLink);
        if (br.containsHTML("The file you are trying to access is no longer available publicly\\.") || br.getURL().equals("https://www.adrive.com/login")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String linkurl = Encoding.htmlDecode(new Regex(br, Pattern.compile("<a href=\"(.*?)\">here</a>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = br2.openGetConnection(linkurl);
        if (con.getContentType().contains("html")) {
            br2.followConnection();
            if (br.containsHTML("File overlimit")) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.adrivecom.errors.toomanyconnections", "Too many connections"), 10 * 60 * 1000l);
            } else {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        downloadLink.setFinalFileName(AdriveCom.getFileNameFromHeader(con));
        downloadLink.setDownloadSize(con.getLongContentLength());
        con.disconnect();
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        /* Link holen */
        String linkurl = Encoding.htmlDecode(new Regex(br, Pattern.compile("<a href=\"(.*?)\">here</a>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* Datei herunterladen */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("File overlimit")) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.adrivecom.errors.toomanyconnections", "Too many connections"), 10 * 60 * 1000l);
            }
        }
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
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