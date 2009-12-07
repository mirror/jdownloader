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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "up-file.com" }, urls = { "http://[\\w\\.]*?up-file\\.com/download/[a-z0-9\\.]+" }, flags = { 0 })
public class UpFileCom extends PluginForHost {

    public UpFileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://up-file.com/page/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://up-file.com", "country", "EN");
        br.setCookie("http://up-file.com", "lang", "EN");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("The requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1><span>Fil.*?me.*?>(.*?)<").getMatch(0);
        String filesize = br.getRegex("<h1><span>Fil.*?me.*?>.*?<h1><span>Fil.*?size.*?>(.*?)<").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Downloading is in process from your")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        Form forms[] = br.getForms();
        /*
         * because every country has different number of possible payment
         * solutions
         */
        Form form = forms[forms.length - 1];
        br.submitForm(form);
        String dllink;
        dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink.contains("free.php")) br.getPage("http://up-file.com/free-link.php");
        if (br.containsHTML("please wait")) {
            String waittime = br.getRegex("please wait -.*?(\\d+).*?se").getMatch(0);
            int wait = 65;
            if (waittime != null) wait = Integer.parseInt(waittime.trim()) + 10;
            sleep(wait * 1000l, downloadLink);
            br.getPage("http://up-file.com/free-link.php");
        }
        dllink = br.getRegex("href='(http:.*?)'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
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
