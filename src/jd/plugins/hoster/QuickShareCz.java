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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "quickshare.cz" }, urls = { "http://[\\w\\.]*?quickshare\\.cz/stahnout-soubor/\\d+:[^\\s]+" }, flags = { 0 })
public class QuickShareCz extends PluginForHost {

    public QuickShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.quickshare.cz/podminky-pouziti";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Takov. soubor neexistuje")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("Název: <strong>(.*?)</strong>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (filename == null) filename = br.getRegex("var ID3 = '(.*?)';").getMatch(0);
        String filesize = br.getRegex("<br>Velikost: <strong>(.*?)<br>").getMatch(0).replaceAll("</strong>", "");
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String ID1 = Encoding.formEncoding(br.getRegex("var ID1 = '(.*?)'").getMatch(0));
        String ID2 = Encoding.formEncoding(br.getRegex("var ID2 = '(.*?)'").getMatch(0));
        String ID3 = Encoding.formEncoding(br.getRegex("var ID3 = '(.*?)'").getMatch(0));
        String ID4 = Encoding.formEncoding(br.getRegex("var ID4 = '(.*?)'").getMatch(0));
        String server = br.getRegex("var server = '(.*?)'").getMatch(0);
        if (ID1 == null || ID2 == null || ID3 == null || ID4 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String param = "ID1=" + ID1 + "&ID2=" + ID2 + "&ID3=" + ID3 + "&ID4=" + ID4;
        br.setFollowRedirects(true);
        // this.sleep(10000, downloadLink); // uncomment when they find a better
        // way to force wait time
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, server + "/download.php", param);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getContentType().contains("text")) {
            String herror = br.getURL();
            if (herror.contains("chyba/1")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.QuickShareCz.alreadyloading", "This IP is already downloading"), 2 * 60 * 1000);
            if (herror.contains("chyba/2")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.QuickShareCz.nofreeslots", "No free slots available"), 60 * 1000);
        } else
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
