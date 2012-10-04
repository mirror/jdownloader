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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tempfile.ru" }, urls = { "http://(www\\.)?tempfile\\.ru/file/\\d+" }, flags = { 0 })
public class TempFileRu extends PluginForHost {

    public TempFileRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://tempfile.ru/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Файл удален|>Более подробный ответ Вы можете найти в разделе<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("style=\"text\\-align:center;\"><a href=\"/file/\\d+\">([^<>\"]*?)</a>").getMatch(0);
        String filesize = br.getRegex(">Размер файла:</td><td align=\"left\">([^<>\"]*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filesize = filesize.replace("Г", "G");
        filesize = filesize.replace("М", "M");
        filesize = filesize.replaceAll("(к|К)", "k");
        filesize = filesize.replaceAll("(Б|б)", "");
        filesize = filesize + "b";
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String passCode = downloadLink.getStringProperty("pass", null);
        final String rCode = br.getRegex("name=\"robot_code\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (rCode == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String postData = "robot_code=" + Encoding.urlEncode(rCode);
        if (br.containsHTML(">Внимание\\! Файл защищен паролем")) {
            if (passCode == null) passCode = Plugin.getUserInput("Password?", downloadLink);
            postData += "&password=" + Encoding.urlEncode(passCode);
        }
        br.postPage(br.getURL(), "robot_code=" + postData);
        if (passCode != null && br.containsHTML(">Внимание\\! Файл защищен паролем")) throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
        String dllink = br.getRegex("Скачать файл можно по этой ссылке:</h2>[\t\n\r ]+<b><a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://tempfile\\.ru/download/[a-z0-9]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -4);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}