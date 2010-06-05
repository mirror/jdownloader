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
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "addat.hu" }, urls = { "http://[\\w\\.]*?addat.hu/.+/.+" }, flags = { 0 })
public class AddatHu extends PluginForHost {

    private String id;

    public AddatHu(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.addat.hu/";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        String url = link.getDownloadURL();
        Regex regex = new Regex(url, ".*addat.hu/(.*)/");
        id = regex.getMatch(0);
        link.setUrlDownload("http://addat.hu/" + id + "/freedownload");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Sajnáljuk, de a keresett fájl nem található.")) return AvailableStatus.FALSE;
        String name = br.getRegex(Pattern.compile("<span style=\"font-size: 13px; font-weight: bolder;\">(.*)</span>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        downloadLink.setName(name);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        requestFileInformation(downloadLink);
        String dllink = br.getRegex(Pattern.compile("<a href=\"(.*)\">\\s*<img border=\"0\" src=\"/images/letoltes_btn.jpg\">", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Just to bypass their little protection
        String activation = "http://addat.hu/stmch.php?id=" + id;
        br.getPage(activation);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl.startDownload();
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public int getMaxConnections() {
        return 1;
    }

    /**
     * TODO: This could be set to -1 but the problem is that i dunnu why i can
     * start that much dls in the browser but not in jd!
     */
    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
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