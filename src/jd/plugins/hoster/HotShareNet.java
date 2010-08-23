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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hotshare.net" }, urls = { "http://[\\w\\.]*?hotshare\\.net/(.+/)?(file|audio|video)/.+" }, flags = { 0 })
public class HotShareNet extends PluginForHost {

    public HotShareNet(PluginWrapper wrapper) {
        super(wrapper);

    }

    @Override
    public String getAGBLink() {
        return "http://www.hotshare.net/pages/tos.html";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        String part = new Regex(link.getDownloadURL(), "(file/.+|audio/.+|video/.+)").getMatch(0);
        link.setUrlDownload("http://www.hotshare.net/en/" + part);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        br.setFollowRedirects(true);
        this.setBrowserExclusive();
        br.setCookie("http://www.hotshare.net/", "language", "english");
        br.getPage(downloadLink.getDownloadURL().replaceAll("video", "file").replaceAll("audio", "file"));
        String filename = br.getRegex(Pattern.compile("<h1 class=\"top_title_downloading\"><strong>(.*?)</strong> </h1>")).getMatch(0);
        if (filename == null) {
            // if old regular expression won't work anymore, use new one
            filename = br.getRegex(Pattern.compile("</strong> \\((.*?)\\) </h1>")).getMatch(0);
        }
        String filesize = br.getRegex(Pattern.compile("<span class=\"arrow1\">Size: <b>(.*?)</b></span> ")).getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form downloadForm = br.getFormbyProperty("name", "form1");
        downloadForm.put("download", "1");
        downloadForm.put("imageField.x", "149");
        downloadForm.put("imageField.y", "14");
        br.submitForm(downloadForm);
        String downloadUrl = br.getRegex(Pattern.compile("<a class=\"link_v3\" href=\"(.*?)\">")).getMatch(0);
        sleep(5000l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("File not")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
        if (downloadLink.getDownloadCurrent() == 15) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
