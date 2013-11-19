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
import java.util.Random;

import jd.PluginWrapper;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dodane.pl" }, urls = { "http://(www\\.)?dodane\\.pl/file/\\d+/.{1}" }, flags = { 0 })
public class DodanePl extends PluginForHost {

    public DodanePl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dodane.pl/regulations";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Strona o podanym adresie nie istnieje")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("var currentFileName = \"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) filename = br.getRegex("<div class=\"filetitle\"><h1>([^<>\"]*?)</h1>").getMatch(0);
        String filesize = br.getRegex("Wielkość: <span>([^<>\"]*?)</span").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String fileid = new Regex(downloadLink.getDownloadURL(), "/file/(\\d+)").getMatch(0);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("http://dodane.pl/file/download/" + fileid + "/" + JDHash.getMD5(Long.toString(System.currentTimeMillis() + new Random().nextLong())));
        final String id = br.getRegex("\"id\":(\\d+)").getMatch(0);
        final String token = br.getRegex("\"sessionToken\":\"([^<>\"]*?)\"").getMatch(0);
        final String server = br.getRegex("\"downloadServerAddr\":\"([^<>\"]*?)\"").getMatch(0);
        if (id == null || token == null || server == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://" + server + "/download/" + fileid + "/" + id + "/" + token;
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}