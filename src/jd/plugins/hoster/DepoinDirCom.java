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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "depoindir.com" }, urls = { "http://(www\\.)?depoindir\\.com/files/get/[A-Za-z0-9]+" }, flags = { 0 })
public class DepoinDirCom extends PluginForHost {

    public DepoinDirCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.depoindir.com/legal/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.postPage(downloadLink.getDownloadURL().replace("/files/get/", "/files/gen/") + "/" + downloadLink.getName(), "pass=&waited=1");
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // Should never happen
            if (br.containsHTML("(>HTTP 403 Sayfa Giriş izni yok\\.<|http-equiv=\\'refresh\\' content=\\'10; url=http://www\\.tutsakhosting|>Başlangıç sayfasına gitmek için aşağıdaki linke tıklayın)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 3 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCustomCharset("UTF-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(> Dosya Link Hatası<|<title>Dosya: Bulunamadı Depoindir\\.com \\- Dosya upload , File upload , Dosya Depoindir\\.com \\- Dosya upload</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(">Dosya ismi:</b></td><td nowrap>none<|>Dosya boyutu:</b></td><td>none Kb")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<span id=\"name\">[\t\n\r ]+<nobr>(.*?) <img").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Dosya indir  Depoindir\\.com - Dosya upload , File upload , (.*?) Depoindir\\.com \\- Dosya upload , File upload , Dosya Depoindir\\.com \\- Dosya upload</title>").getMatch(0);
        String filesize = br.getRegex("<span id=\"size\">(.*?)</span><br").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}