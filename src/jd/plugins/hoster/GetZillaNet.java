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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "getzilla.net" }, urls = { "http://(www\\.)?getzilla\\.net/files/\\d+/.*?\\.html" }, flags = { 0 })
public class GetZillaNet extends PluginForHost {

    public GetZillaNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.getzilla.net/page/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Файл не найден<|<title>Бесплатный хостинг файлов Getzilla\\.net файлообменник позволяет хранить и передавать файлы</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Программы</a> \\\\ <span>(.*?)</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1>Скачать файл\\&nbsp;<span>\\&laquo;(.*?)\\&raquo;</span></h1>").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>[\t\n\r ]+Getzilla\\.net (.*?) скачать бесплатно</title>").getMatch(0);
        }
        String filesize = br.getRegex("class=\"filesize ([a-z0-9]{1,50})?\">(.*?)<div").getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        filesize = Encoding.htmlDecode(filesize);
        filesize = filesize.replace(",", ".");
        filesize = filesize.replace("Г", "G");
        filesize = filesize.replace("М", "M");
        filesize = filesize.replaceAll("(к|К)", "k");
        filesize = filesize.replaceAll("(Б|б)", "");
        filesize = filesize + "b";
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL().replace("/files/", "/files/get/"));
        int wait = 5000;
        String waittime = br.getRegex("var download_wait_time = \"(\\d+)\";").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait + 200, downloadLink);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage(downloadLink.getDownloadURL().replace("/files/", "/files/getUrl/") + "?gold=false&_dc=" + System.currentTimeMillis());
        String dllink = br.toString();
        if (dllink == null || !dllink.startsWith("http") || dllink.contains("/files/") || dllink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}