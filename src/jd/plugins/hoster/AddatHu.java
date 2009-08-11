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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "addat.hu" }, urls = { "http://[\\w\\.]*?addat.hu/.+/.+" }, flags = { 0 })
public class AddatHu extends PluginForHost {

    public AddatHu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.addat.hu/";
    }

    public String getCoder() {
        return "TnS";
    }

    public void correctDownloadLink(DownloadLink link) {
        String url = link.getDownloadURL();
        Regex regex = new Regex(url, ".*addat.hu/(.*)/");
        String id = regex.getMatch(0);
        link.setUrlDownload("http://addat.hu/" + id + "/freedownload");
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Sajnáljuk, de a keresett fájl nem található\\.")) return AvailableStatus.FALSE;

        String name = br.getRegex(Pattern.compile("<span style=\"font-size: 13px; font-weight: bolder;\">(.*)</span>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        downloadLink.setName(name);

        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        requestFileInformation(downloadLink);
        String link = br.getRegex(Pattern.compile("<a href=\"(.*)\">\\s*<img border=\"0\" src=\"/images/letoltes_btn.jpg\">", Pattern.CASE_INSENSITIVE)).getMatch(0);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1).startDownload();
    }

    public int getTimegapBetweenConnections() {
        return 500;
    }

    public int getMaxConnections() {
        return 1;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
