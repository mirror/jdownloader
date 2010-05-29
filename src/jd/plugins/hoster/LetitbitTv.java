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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "letitbit.tv" }, urls = { "http://[\\w\\.]*?letitbit\\.tv/files/\\d+/.+" }, flags = { 0 })
public class LetitbitTv extends PluginForHost {

    public LetitbitTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.letitbit.ru/page/agreement";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Файл не найден<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"orange\">\\&laquo;(.*?)\\&raquo;</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"\">(.*?)</span>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"file-property\">Файл</td><td>(.*?)</td>").getMatch(0);
            }
        }
        String filesize = br.getRegex(">Size:</span>(.*?)</li>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("class=\"img arch\"></div><span>(.*?)</span>").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = Encoding.htmlDecode(filename);
        filesize = filesize.replace("&nbsp;", "");
        filesize = filesize.replace(",", ".");
        filesize = filesize.replace("Г", "G");
        filesize = filesize.replace("М", "M");
        filesize = filesize.replace("к", "k");
        filesize = filesize.replaceAll("(Б|б)", "");
        filesize = filesize.replace("байт", "byte");
        filesize = filesize + "b";
        link.setFinalFileName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String addedLink = downloadLink.getDownloadURL();
        br.getPage(addedLink.replace("files/", "files/get/"));
        // Ticket Time
        int tt = 45000;
        String ttt = br.getRegex("var download_wait_time = \"(\\d+)\";").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt + " milliseconds from now on...");
            tt = Integer.parseInt(ttt);
        }
        sleep(tt + 1000, downloadLink);
        br.getPage(addedLink.replace("files/", "files/getUrl/"));
        String dllink = br.toString();
        if (!dllink.trim().startsWith("http")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<title>404 Not Found</title")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
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