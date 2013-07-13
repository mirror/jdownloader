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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filefeltolto.hu" }, urls = { "http://(www\\.)?filefeltolto\\.hu/(letoltes/[a-z0-9]+/[^<>\"/]+|x/[a-z0-9]+|xvidplayer_gateway\\.php\\?fid=\\d+)" }, flags = { 0 })
public class FileFeltoltoHu extends PluginForHost {

    public FileFeltoltoHu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filefeltolto.hu/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getDownloadURL().matches("http://(www\\.)?filefeltolto\\.hu/(letoltes/[a-z0-9]+/[^<>\"/]+|x/[a-z0-9]+)")) {
            br.getPage(link.getDownloadURL());
            if (br.containsHTML("A fájl nem létezik, vagy törölve lett szerzői jogsértő tartalom miatt")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.getURL().equals("http://filefeltolto.hu/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = br.getRegex("class=\"left\">Fájlnév</td><td>([^<>\"]*?)</td></tr>").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) letöltés :: FileFeltolto\\.hu</title>").getMatch(0);
            String filesize = br.getRegex("\\((\\d+ KB)\\)").getMatch(0);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setName(Encoding.htmlDecode(filename.trim()));
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        } else {
            Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(link.getDownloadURL());
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con).trim()));
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = downloadLink.getDownloadURL();
        int wait = 10;
        String waittime = br.getRegex("var limit=\\'0:(\\d+)\\';").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        // Activate waittime
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://filefeltolto.hu/ajax/set.php", "hash=" + new Regex(downloadLink.getDownloadURL(), "filefeltolto\\.hu/letoltes/([a-z0-9]+)/").getMatch(0));
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}