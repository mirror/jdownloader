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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dump.ru" }, urls = { "http://[\\w\\.]*?dump\\.ru/file/[0-9]+" }, flags = { 0 })
public class DumpRu extends PluginForHost {

    public DumpRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dump.ru/pages/about/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            setBrowserExclusive();
            br.setCustomCharset("UTF-8");
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            // File not found
            if (br.containsHTML("Запрошенный файл не обнаружен")) {
                logger.warning("File not found");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String name = br.getRegex("name_of_file\">\\s(.*?)</span>").getMatch(0).trim();
            String filesize = br.getRegex("Размер: <span class=\"comment\">(.*?)</span").getMatch(0).trim();
            if (filesize.contains("Мб")) {
                filesize = filesize.replace("Мб", "MB");
            }
            if (filesize.contains("Кб")) {
                filesize = filesize.replace("Кб", "KB");
            }
            if (filesize.contains("&nbsp;")) {
                filesize = filesize.replace("&nbsp;", "");
            }
            if (name == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(name);
            downloadLink.setDownloadSize(Regex.getSize(filesize));

            return AvailableStatus.TRUE;
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form DLForm = br.getForm(1);
        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(DLForm);
        String link = br.getRegex(Pattern.compile("<a href=\"(http://.*?dump\\.ru/file_download/.*?)\">")).getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, -3);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
