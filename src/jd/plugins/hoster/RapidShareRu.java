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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidshare.ru" }, urls = { "http://[\\w\\.]*?rapidshare\\.ru/[0-9]+" }, flags = { 0 })
public class RapidShareRu extends PluginForHost {

    public RapidShareRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.rapidshare.ru/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Guest or Admin Password")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Полное имя файла:(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("Полное имя файла.*?\">(.*?)</nobr").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("'http://dl[0-9]+\\.rapidshare\\.ru/.*?/.*?/(.*?)\"").getMatch(0);
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        Regex filesizereg = br.getRegex("объемом <b><span class=.*?>(.*?)</span>(.*?)</b>");
        String filesize = null;
        if (filesizereg.getMatch(0) != null && filesizereg.getMatch(1) != null) filesize = filesizereg.getMatch(0) + filesizereg.getMatch(1);
        // Filesizehandling is complicated so only set the filesize if it is
        // regexed correctly
        if (filesize != null && filesize.contains("мегабайт")) {
            filesize = filesize.replace("мегабайт", "MB");
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Все слоты бесплатного скачивания заняты. Пожалуйста попробуйте еще раз через некоторое время")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available", 30 * 60 * 1000l);
        String dllink = br.getRegex("innerHTML=' <a href=\"'\\+'(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("'(http://dl[0-9]+\\.rapidshare\\.ru/.*?/.*?/[a-zA-Z0-9.-_]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setDebug(true);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            if (dl.getConnection().isOK()) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "ServerError", 5 * 60 * 1000l);
            }
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