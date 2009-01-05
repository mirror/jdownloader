//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class ShareBombCom extends PluginForHost {

    public ShareBombCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www1.sharebomb.com/tos";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.set_LAST_PAGE_ACCESS_identifier(this.getHost());
        br.set_PAGE_ACCESS_exclusive(false);
        br.set_WAIT_BETWEEN_PAGE_ACCESS(1000l);
        String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        for (int i = 1; i < 3; i++) {
            try {
                br.getPage(url);
            } catch (Exception e) {
                continue;
            }
            downloadName = Encoding.htmlDecode(br.getRegex(Pattern.compile("Name:</strong> (.*?)<", Pattern.CASE_INSENSITIVE)).getMatch(0));
            downloadSize = br.getRegex(Pattern.compile("Size:</strong> (.*?)<", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (!(downloadName == null || downloadSize == null)) {
                downloadLink.setName(downloadName);
                downloadLink.setDownloadSize(Regex.getSize(downloadSize.replaceAll(",", "\\.")));
                return true;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        /* Link holen */
        String url = new Regex(br, Pattern.compile("<a href=\"/?(files/.*)\">", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String linkurl = "http://www1.sharebomb.com/" + Encoding.htmlDecode(url);
        /* Datei herunterladen */
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, linkurl);
        dl.startDownload();
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 1000;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}