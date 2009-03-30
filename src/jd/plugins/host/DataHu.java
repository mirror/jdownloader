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
import jd.plugins.PluginForHost;

public class DataHu extends PluginForHost {

    public DataHu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://data.hu/adatvedelem.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        System.out.println(br.getPage(downloadLink.getDownloadURL()));
        String[] dat = br.getRegex("<div class=\"download_filename\">(.*?)<\\/div>.*\\:(.*?)<div class=\"download_not_start\">").getRow(0);
        long length = Regex.getSize(dat[1].trim());
        downloadLink.setDownloadSize(length);
        downloadLink.setName(dat[0].trim());
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        getFileInformation(downloadLink);

        if (br.containsHTML("A let.*?shez v.*?rnod kell:")) {
            long wait = (Long.parseLong(br.getRegex(Pattern.compile("<div id=\"counter\" class=\"countdown\">([0-9]+)</div>")).getMatch(0)) * 1000);
            sleep(wait, downloadLink);
        }
        br.getPage(downloadLink.getDownloadURL());
        String link = br.getRegex(Pattern.compile("download_it\"><a href=\"(http://.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        br.openDownload(downloadLink, link, true, 1).startDownload();

    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public int getMaxConnections() {
        return 1;
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
}