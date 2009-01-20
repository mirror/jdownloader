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

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class BadongoCom extends PluginForHost {

    public BadongoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.badongo.com/toc/";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        br.setCookiesExclusive(true);
        br.setCookie("http://www.badongo.com", "badongoL", "de");
        br.getPage(downloadLink.getDownloadURL().replaceAll("httpviajd\\d+", "http"));

        String filesize = br.getRegex(Pattern.compile("<div class=\"ffileinfo\">Ansichten.*?\\| Dateig.*?:(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String filename = br.getRegex("<div class=\"finfo\">(.*?)</div>").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (downloadLink.getStringProperty("type", "single").equalsIgnoreCase("single")) {
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        } else {

            String parts = Integer.toString(downloadLink.getIntegerProperty("parts", 1));
            if (parts.length() < 2) parts = "01";
            downloadLink.setName(filename.trim() + "." + JDUtilities.fillString(Integer.toString(downloadLink.getIntegerProperty("part", 0)) + "", "0", "", parts.length()));
        }
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        String link = null;
        getFileInformation(downloadLink);
        sleep(7000l, downloadLink);
        br.setDebug(true);
        if (downloadLink.getStringProperty("type", "single").equalsIgnoreCase("split")) {
            String downloadLinks[] = br.getRegex("<a href=\"#\" onclick=\"return doDownload\\('(.*?)'\\);\">").getColumn(0);
            link = downloadLinks[downloadLink.getIntegerProperty("part", 0)];
            br.setCookie("http://www.badongo.com", "bdgDL_f", "4005384");
            br.setCookie("http://www.badongo.com", "uAhist", "23%3A872%7C1%3A170%2C113");
            br.setCookie("http://www.badongo.com", "uAchist", "142%2C2%2C9");
            br.setCookie("http://www.badongo.com", "adF4005384", "yellow");
            br.getPage(link + "/ifr?pr=1&zenc=");
            if (br.containsHTML("Gratis Mitglied Wartezeit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000l);
            if (br.containsHTML("Du hast Deine Download Quote überschritten")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            link = br.getRegex(Pattern.compile("beginDownload.*?window.location.href.*?'(.*?)'", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            link = "http://www.badongo.com" + link;
        }
        br.setFollowRedirects(true);
        br.setDebug(true);
        if (link == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        dl = br.openDownload(downloadLink, link, true, 1);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
