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

package jd.plugins.host;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class DumpRu extends PluginForHost {

    public DumpRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dump.ru/pages/about/";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            setBrowserExclusive();

            br.getPage(downloadLink.getDownloadURL());

            // File not found
            if (br.containsHTML("Запрошенный файл не обнаружен")) {
                logger.warning("File not found");
                return false;
            }

            // Filesize
            String size = br.getRegex("Размер: <span class=\"comment\">(.*?)</span>").getMatch(0);
            downloadLink.setDownloadSize(Regex.getSize(size.replaceAll("Кб", "KB").replaceAll("Mб", "MB")));

            // Filename
            String name = br.getRegex("<span class=\"name_of_file\">(.*?)</span>").getMatch(0).trim();
            downloadLink.setName(name);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        if (!getFileInformation(downloadLink)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        dl = RAFDownload.download(downloadLink, br.createFormRequest(br.getForm(1)));
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert nachprüfen */
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}