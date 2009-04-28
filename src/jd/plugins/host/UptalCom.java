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

package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class UptalCom extends PluginForHost {

    public UptalCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(100l);
    }

    //@Override
    public String getAGBLink() {
        return "http://www.uptal.com/faq.php";
    }

    //@Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File name:</b></td>\\s+<td[^>]+>(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("File size:</b></td>\\s+<td[^>]+>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return true;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        String getlink;
        String filename = downloadLink.getName();
        String previousLink = downloadLink.getStringProperty("directLink", null);
        if (previousLink == null) {
            getFileInformation(downloadLink);
            br.setFollowRedirects(true);
            getlink = br.getRegex("document\\.location=\"(.*?)\"").getMatch(0);
            if (getlink == null) getlink = br.getRegex("name=downloadurl value=\"(.*?)\"").getMatch(0);
            if (getlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            br.setFollowRedirects(true);
            getlink = getlink.replaceAll(" ", "%20");
            downloadLink.setProperty("directLink", getlink);
        } else {
            getlink = previousLink;
        }
        // this.sleep(3000, downloadLink); // uncomment when they find a better
        // way to force wait time
        dl = br.openDownload(downloadLink, getlink, true, 1);
        downloadLink.setFinalFileName(filename);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isOK()) {
            if (previousLink != null) {
                downloadLink.setProperty("directLink", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
        }
        dl.startDownload();
    }

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    //@Override
    public void reset() {
    }

    //@Override
    public void resetPluginGlobals() {
    }

    //@Override
    public void reset_downloadlink(DownloadLink link) {
        link.setProperty("directLink", null);
    }
}