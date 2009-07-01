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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(name = "slingfile.com", urls = "http://[\\w\\.]*?slingfile\\.com/(file|audio|video)/.+", flags = 0)
public class SlingFileCom extends PluginForHost {

    public SlingFileCom(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.slingfile.com/pages/tos.html";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {

        this.setBrowserExclusive();
        br.setCookie("http://www.slingfile.com/", "language", "deutsch");
        br.getPage(downloadLink.getDownloadURL().replaceAll("video", "file").replaceAll("audio", "file"));
        String filename = br.getRegex(Pattern.compile("Dateiname : <strong>(.*?)</strong>")).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("Dateigr&ouml;&szlig;e : (.*?)</p>")).getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form downloadForm = br.getFormbyProperty("name", "form1");
        downloadForm.put("download", "1");
        br.submitForm(downloadForm);
        String downloadUrl = br.getRegex(Pattern.compile("<a class=\"link_v3\" href=\"(.*?)\">here</a>")).getMatch(0);

        dl = br.openDownload(downloadLink, downloadUrl, true, 0);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return this.getMaxSimultanDownloadNum();
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
    }
}
