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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filebox.ro" }, urls = { "(http://[\\w\\.]*?(filebox|fbx)\\.ro/(download|down)\\.php\\?key=[0-9a-z]{16})|(http://[\\w\\.]*?(filebox|fbx)\\.ro/video/play_video\\.php\\?key=[0-9a-z]{16})|(http://[\\w\\.]*?fbx\\.ro/[0-9a-z]{16})|(http://[\\w\\.]*?fbx\\.ro/v/[0-9a-z]{16})" }, flags = { 0 })
public class FileboxRo extends PluginForHost {

    private boolean isVideo = false;

    public FileboxRo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setCookie(link.getDownloadURL(), "filebox_language", "en");
        if (!Regex.matches(link.getDownloadURL(), "http://[\\w\\.]*?filebox\\.ro/download\\.php\\?key=[0-9a-z]{16}")) {
            if (!Regex.matches(link.getDownloadURL(), ".+(/video/|/v/).+")) {
                br.setFollowRedirects(true);
                br.getPage(link.getDownloadURL());
                String urlpart = "http://www.filebox.ro/download.php?key=";
                String correctUrl = urlpart + br.getRegex("window\\.location\\.href='http://www\\.filebox\\.ro/download\\.php\\?key=([0-9a-z]{16})';").getMatch(0);
                link.setUrlDownload(correctUrl);
            }
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.filebox.ro/disclaimer.php?english=1";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (Regex.matches(downloadLink.getDownloadURL(), ".+(/video/|/v/).+")) {
            this.isVideo = true;
            String filename = br.getRegex("<h2>(.*?)</h2>.*?<div id=\"player\">").getMatch(0);
            if (filename != null) {
                downloadLink.setFinalFileName(filename + ".flv");
                return AvailableStatus.TRUE;
            }
        } else {
            String redirect = br.getRegex("window.location.href='(.*?)'").getMatch(0);
            br.setCookie(redirect, "filebox_language", "en");
            br.getPage(redirect);
            if (!(br.containsHTML("File deleted or file lifespan expired") || br.containsHTML("Wrong link") || br.containsHTML("Filebox.ro is temporarily not available."))) {
                String filename = br.getRegex("<h3>(.*?)</h3>.*?<hr />").getMatch(0);
                String filesize = br.getRegex("Size:</span>(.*?)</li>").getMatch(0);

                if (!(filename == null || filesize == null)) {
                    downloadLink.setName(filename);
                    downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
                    return AvailableStatus.TRUE;
                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String linkurl = null;
        if (isVideo) {
            String urlpart = br.getRegex("s1.addVariable\\(\"source_script\",\"(.*?)\"\\);").getMatch(0);
            String key = br.getRegex("s1.addVariable\\(\"key\",\"(.*?)\"\\);").getMatch(0);
            linkurl = urlpart + "?file=" + key + "&start=0";
        } else {
            br.setFollowRedirects(true);
            String id = new Regex(downloadLink.getDownloadURL(), ".+key=([0-9a-z]{16})").getMatch(0);
            br.getPage("http://www.filebox.ro/js/wait.js.php?key=" + id);
            String strwait = br.getRegex("wait=(\\d+)").getMatch(0);

            if (strwait != null && !br.containsHTML("Start download")) {
                long waittime = Long.parseLong(strwait.trim());
                waittime = (waittime * 1000) + 1;
                this.sleep(waittime, downloadLink);
            }
            br.getPage("http://www.filebox.ro/download.php?key=" + id);
            linkurl = br.getRegex("(http://\\w+\\.filebox.ro/get_file.php\\?key=[0-9a-z]{32})").getMatch(0);
        }

        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, false, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        if (!dl.startDownload()) {
            /* workaround for buggy server, there is always 12 bytes missing */
            if (downloadLink.getDownloadSize() == downloadLink.getDownloadCurrent() + 13) {
                downloadLink.getLinkStatus().reset();
                downloadLink.getLinkStatus().addStatus(LinkStatus.FINISHED);
            }
        }
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

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
