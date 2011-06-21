//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "data.cod.ru" }, urls = { "http://[a-zA-Z.]{0,}data.cod.ru/[0-9]{1,}" }, flags = { 0 })
public class DatacodRu extends PluginForHost {

    public DatacodRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://data.cod.ru/terms/";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; chrome://global/locale/intl.properties; rv:1.8.1.12) Gecko/2008102920  Firefox/3.0.0 YB/4.2.0");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<title>404</title>") || br.containsHTML("���� �� ������") || br.containsHTML("���������� ���� �������� �����\\.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("������ �� ������ ������� ������ ��� ������������� ���������")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE);
        String name = br.getRegex(Pattern.compile("����: <b title=\"(.*?)\">")).getMatch(0);
        if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileSize = br.getRegex(Pattern.compile("������: <b>(.*?)</b></li>")).getMatch(0);
        if (fileSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // 15.58 ��
        fileSize = fileSize.replaceAll("�", "G");
        fileSize = fileSize.replaceAll("�", "M");
        fileSize = fileSize.replaceAll("�", "k");
        fileSize = fileSize.replaceAll("�", "k");
        fileSize = fileSize.replaceAll("�", "");
        fileSize = fileSize + "b";
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String dlLink = br.getRegex(Pattern.compile("<a href=\"(http://files.*?)\" class=\"button\">������� ����</a>")).getMatch(0);
        if (dlLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("the dllink doesn't seem to be a file, following the connection...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlLink, true, 1);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
