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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "slingfile.com" }, urls = { "http://[\\w\\.]*?slingfile\\.com/(file|audio|video)/.+" }, flags = { 0 })
public class SlingFileCom extends PluginForHost {

    public SlingFileCom(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    public String getAGBLink() {
        return "http://www.slingfile.com/pages/tos.html";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        /*
         * cookie settings does not work because hoster checks ip and sets
         * language then so the regex has to match good enough to find info but
         * not use words
         */
        br.getPage(downloadLink.getDownloadURL().replaceAll("video", "file").replaceAll("audio", "file"));
        String filename = br.getRegex(Pattern.compile("<p class=\"info8\">.*?alt=\"arrow\".*?<strong>(.*?)<")).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("<p class=\"info8\">.*?<p class=\"info8\">.*?/>.*? : (.*?)<", Pattern.DOTALL)).getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form downloadForm = br.getFormbyProperty("name", "form1");
        downloadForm.put("download", "1");
        br.submitForm(downloadForm);
        long waittime = 0;
        try {
            waittime = Long.parseLong(br.getRegex("var seconds\\=(\\d+)").getMatch(0)) * 1000l;
        } catch (Exception e) {
        }
        if (waittime > 31000) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
        } else {
            // this.sleep(waittime, downloadLink);
        }

        String downloadUrl = br.getRegex(Pattern.compile("<a class=\"link_v3\" href=\"(.*?)\">here</a>")).getMatch(0);
        if (downloadUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadUrl = Encoding.htmlDecode(downloadUrl);
        this.sleep(waittime, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, 0);
        if (dl.getConnection().getResponseCode() == 410) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
