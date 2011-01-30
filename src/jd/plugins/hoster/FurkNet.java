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
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "furk.net" }, urls = { "http(s)?://[\\w\\.]*?furk\\.net/(.*?\\.html|df/[a-z0-9]+)" }, flags = { 0 })
public class FurkNet extends PluginForHost {

    public FurkNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://furk.net/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("This torrent is not ready for")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "This is NO VALID LINK!"); }
        String filename = br.getRegex("<title>(.*?) :: Furk.net</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("document\\.location\\.href='/registration\\?pfile=(.*?)'\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) :: Furk\\.net</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
                }
            }
        }
        String filesize = br.getRegex("align=\"center\">File size: <b>(.*?)</b><br").getMatch(0);
        if (filesize == null) filesize = br.getRegex("<li>File size: <b>(.*?)</b></li>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        parameter.setName(filename.trim());
        if (filesize != null) parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (br.containsHTML("Slots limit for free downloads")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        if (br.containsHTML("servers for direct downloading:<")) throw new PluginException(LinkStatus.ERROR_FATAL, "Download only possible for registered users");
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.remove(null);
        br.setFollowRedirects(true);
        String waittime = br.getRegex("id=\"free_dl_countdown\">(\\d+)</div>").getMatch(0);
        if (waittime != null) {
            // waittime
            int tt = Integer.parseInt(waittime);
            sleep(tt * 1001l, link);
        } else
            sleep(60001l, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, form, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            if (con.getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.followConnection();
            if (br.containsHTML(">File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("Slots limit for free downloads")) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60000);
            }
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}
