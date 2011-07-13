//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fun-vids.org" }, urls = { "http://(www\\.)?fun\\-vids\\.orgdecrypted/hosted/media/.*?,\\d+\\.php" }, flags = { 0 })
public class FunVidsOrg extends PluginForHost {

    public FunVidsOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        // No TOSlink available
        return "http://www.fun-vids.org/";
    }

    private static final String PACKEDJSURL = "http://fvfileserver.com/mediaplayer/encrypt.js";

    public void correctDownloadLink(DownloadLink link) {
        // Links come from a decrypter
        link.setUrlDownload(link.getDownloadURL().replace("fun-vids.orgdecrypted/", "fun-vids.org/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://www.fun-vids.org/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div id=\"mid\">[\t\n\r ]+<div class=\"navi_m_top\">(.*?)</div>").getMatch(0);
        if (filename == null) filename = br.getRegex("<meta name=\"description\" content=\"(.*?)\">").getMatch(0);
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (!br.containsHTML(PACKEDJSURL)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String linkPart = br.getRegex("encr\\(video_path\\) \\+\\'(/.*?)\\'").getMatch(0);
        if (linkPart == null) linkPart = br.getRegex("\\'(/videos/\\d+/show\\d+\\.flv)\\'").getMatch(0);
        br.getPage("http://fvfileserver.com/show.php");
        String lolparam = br.getRegex("var video_path =\"(.*?)\";").getMatch(0);
        if (lolparam == null || linkPart == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String finallink = "http://fvfileserver.com/" + encryptString(lolparam) + linkPart;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String encryptString(String b) {
        String a = "";
        for (int c = 0; c < b.length(); c++) {
            if (b.charAt(c) >= 65 && b.charAt(c) <= 90) {
                a = a + convert_char(13, 65, 90, b.charAt(c));
            } else {
                if (b.charAt(c) >= 97 && b.charAt(c) <= 122) {
                    a = a + convert_char(13, 97, 122, b.charAt(c));
                } else {
                    a = a + b.charAt(c);
                }
            }
        }
        return a;
    }

    private String convert_char(int c, int b, int d, char x) {
        String a = Character.toString(x);
        return fromCharCode(b + (((a.charAt(0) - b) + c) % (c * 2)));
    }

    public static String fromCharCode(int lol) {
        return Character.toString((char) lol);
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
