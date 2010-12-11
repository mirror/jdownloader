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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mammutmail.com" }, urls = { "http://(www\\.)?mammutmail\\.com/\\?action=download\\&sid=[a-z0-9]{32}" }, flags = { 0 })
public class MammutMailCom extends PluginForHost {

    public MammutMailCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.mammutmail.com/en/?page=felhasznalasi";
    }

    private static final String INFOREGEX = "<p><a href=\"(http://.*?)\"><span class=\"a_blue\">Letöltés: (.*?) \\(([0-9\\.]+ [A-Za-z]+)\\)</span>";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("iso-8859-2");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(A fájl már nem található a szerveren\\!<br|A fájl a feladó vagy a címzett által törölve lett<br|Egyéb ok miatt törölve lett a rendszerből<br)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(INFOREGEX).getMatch(1);
        if (filename == null) filename = br.getRegex("\\&file_name=(.*?)\"").getMatch(0);
        String filesize = br.getRegex(INFOREGEX).getMatch(2);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (!br.containsHTML("download_do\\.php\\?sid=")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(false);
        String dllink = "http://mammutmail.com/download_do.php?sid=" + new Regex(downloadLink.getDownloadURL(), "\\&sid=([a-z0-9]{32})").getMatch(0) + "&file_name=" + downloadLink.getName();
        if (dllink.contains("null")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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