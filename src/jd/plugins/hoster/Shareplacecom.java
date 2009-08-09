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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareplace.com" }, urls = { "http://[\\w\\.]*?shareplace\\.com/\\?[\\w]+(/.*?)?" }, flags = { 0 })
public class Shareplacecom extends PluginForHost {

    private String url;

    public Shareplacecom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://shareplace.com/rules.php";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        url = downloadLink.getDownloadURL();
        setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(true);
        br.getPage(url);
        if (br.containsHTML("Your requested file is not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (br.getRedirectLocation() == null) {
            String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("File name: </b>(.*?)<b>", Pattern.CASE_INSENSITIVE)).getMatch(0));
            if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(filename.trim());
            String filesize = null;
            if ((filesize = br.getRegex("File size: </b>(.*)MB<b>").getMatch(0)) != null) {
                downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
            } else if ((filesize = br.getRegex("File size: </b>(.*)KB<b>").getMatch(0)) != null) {
                downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024);
            } else if ((filesize = br.getRegex("File size: </b>(.*)byte<b>").getMatch(0)) != null) {
                downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)));
            }
            return AvailableStatus.TRUE;
        } else
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /* Link holen */
        url = Encoding.UTF8Decode(br.getRegex(Pattern.compile("document.location=\"(.*?)\";", Pattern.CASE_INSENSITIVE)).getMatch(0));

        /* Zwangswarten */
        String waittime = br.getRegex(Pattern.compile("var timeout='([0-9]+)';", Pattern.CASE_INSENSITIVE)).getMatch(0);
        long wait = 10;
        if (waittime != null) wait = Long.parseLong(waittime);
        sleep(wait * 1000l, downloadLink);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url);
        if (dl.getConnection().isContentDisposition()) {
            /* Workaround für fehlerhaften Filename Header */
            String name = Plugin.getFileNameFormHeader(dl.getConnection());
            if (name != null) downloadLink.setFinalFileName(Encoding.urlDecode(name, false));
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}
