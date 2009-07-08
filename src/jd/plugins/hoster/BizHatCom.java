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
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision="$Revision", interfaceVersion=2, names = { "bizhat.com"}, urls ={ "http://[\\w\\.]*?uploads\\.bizhat\\.com/file/[0-9]+"}, flags = {0})
public class BizHatCom extends PluginForHost {

    public BizHatCom(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    // @Override
    public String getAGBLink() {
        return "http://uploads.bizhat.com/pages/terms.html";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();

        br.getPage(downloadLink.getDownloadURL());
        // if
        //(br.containsHTML("access to the service may be unavailable for a while"
        // ))
        // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        // //TODO: kein Link zu
        String[] infos = br.getRegex("<div style=\"font-size: 12pt;font-weight:bold;\">[\\s]*(.*?) &nbsp; - &nbsp; (.*?)[\\s]*</div>").getRow(0);
        if (infos == null || infos[0] == null || infos[1] == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(infos[0].trim());
        downloadLink.setDownloadSize(Regex.getSize(infos[1].trim()));
        return AvailableStatus.TRUE;
    }

    // @Override
    /* /* public String getVersion() {
        return getVersion("$Revision$");
    } */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String id = new Regex(downloadLink.getDownloadURL(), ".*/(\\d+)").getMatch(0);
        br.postPage(downloadLink.getDownloadURL(), "id=" + Encoding.urlEncode(id) + "&download=" + Encoding.urlEncode("<< Download Now >>"));
        String linkurl = br.getRegex(Pattern.compile("str = \"(http.*?)\"")).getMatch(0);
        String downloadURL = Encoding.htmlDecode(linkurl);
        dl = br.openDownload(downloadLink, downloadURL, true, 0);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return this.getMaxSimultanDownloadNum();
    }

    // @Override
    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
