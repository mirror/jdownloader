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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class TwoSharedCom extends PluginForHost {

    public TwoSharedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.2shared.com/terms.jsp";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        br.setCookiesExclusive(true);
        br.getPage(downloadLink.getDownloadURL());
        String filesize = br.getRegex(Pattern.compile("<span class=.*?>File size:</span>(.*?)&nbsp; &nbsp;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String filename = br.getRegex("<title>2shared - download(.*?)</title>").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim().replaceAll(",|\\.", "")));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 3529 $");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String link = br.getRegex(Pattern.compile("function startDownload.*?window.location = \"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (link == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        dl = br.openDownload(downloadLink, link);
        if (dl.getConnection().getContentType().contains("text")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Session limit reached", 10 * 60 * 1000l);
        }
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
