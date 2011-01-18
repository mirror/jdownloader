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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filehoster.ru" }, urls = { "http://[\\w\\.]*?filehoster\\.ru/files/[a-z0-9]+" }, flags = { 0 })
public class FileHosterRu extends PluginForHost {

    public FileHosterRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filehoster.ru/rules/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("windows-1251");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Файлообменник временно не работает")) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.FileHosterRu.errors.hostertemporaryunavailable", "This hoster is temporary not available!"));
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.containsHTML("(Запрашиваемый вами файл не существует|К файлу долгое время не было обращений)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>Файлообменник Filehoster\\.ru - скачать файл(.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class='file1link' href='.*?'>(.*?)</a>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("name=\"description\" content=\"Файлообменник: скачать файл(.*?)\">").getMatch(0);
            }

        }
        String filesize = br.getRegex("</a>\\&nbsp;\\&nbsp;\\((.*?)\\)</span>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        if (filesize != null) {
            filesize = filesize.replace("Г", "G");
            filesize = filesize.replace("М", "M");
            filesize = filesize.replace("к", "k");
            filesize = filesize.replaceAll("(Б|б)", "");
            filesize = filesize + "b";
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Файлообменник временно не работает")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.FileHosterRu.errors.hostertemporaryunavailable", "This hoster is temporary not available!"));
        String dllink = br.getRegex("class='file1link' href='(http.*?)'>").getMatch(0);
        if (dllink == null) dllink = br.getRegex("'(http://dl[0-9]+\\.filehoster\\.ru/files/.*?)'>").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -1);
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