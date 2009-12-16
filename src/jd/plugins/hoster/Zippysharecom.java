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
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zippyshare.com" }, urls = { "http://www\\d{0,}\\.zippyshare\\.com/(v/\\d+/file\\.html|.*?key=\\d+)" }, flags = { 0 })
public class Zippysharecom extends PluginForHost {

    private Pattern linkIDPattern = Pattern.compile(".*?zippyshare\\.com/v/([0-9]+)/file.html");
    private Pattern fileExtPattern = Pattern.compile("pong = 'fckhttp.*?(\\.\\w+)';");

    public Zippysharecom(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    @Override
    public String getAGBLink() {
        return "http://www.zippyshare.com/terms.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.zippyshare.com", "ziplocale", "en");
        br.getPage(downloadLink.getDownloadURL().replaceAll("locale=..", "locale=en"));
        if (br.containsHTML("<title>Zippyshare.com - File does not exist</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex(Pattern.compile("<strong>Size: </strong>(.*?)</font><br", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String name = br.getRegex(Pattern.compile("<title>Zippyshare.com -(.*?)</title>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (name.endsWith("...")) {
            String linkID = new Regex(downloadLink.getDownloadURL(), linkIDPattern).getMatch(0);
            String fileExt = br.getRegex(fileExtPattern).getMatch(0);
            if (linkID != null) name = (name.substring(0, name.length() - 3) + "_" + linkID);
            if (fileExt != null) name = name + fileExt;
        }
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
//        String dllink = br.getRegex("ziptime = 0; tuw\\(\\);.*?var.*?= '(http.*?)';").getMatch(0);
        String dllink = br.getRegex("var pong = '(http.*?)';").getMatch(0);
        if (dllink == null) dllink = br.getRegex("<script language=\"Javascript\">.*?(http.*?)';").getMatch(0);
        logger.info("Encoded the zippyshare link...");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = Encoding.urlDecode((dllink), true);
        if (!br.containsHTML("name=\"mp3player\"") || br.containsHTML("replace")) {
            String[] replacessuck = br.getRegex("(\\(.*?\\.replace\\(.*?,.*?\\))").getColumn(0);
            if (replacessuck != null) {
                for (String fckU : replacessuck) {
                    String rpl1 = new Regex(fckU, "replace\\((.*?),.*?\\)").getMatch(0).replace("/", "");
                    String rpl2 = new Regex(fckU, "replace\\(.*?, \"(.*?)\"\\)").getMatch(0);
                    if (!rpl1.equals("www")) {
                        dllink = dllink.replace(rpl1, rpl2);
                        logger.info("Replace(s) done");
                    }
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
