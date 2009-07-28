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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filebox.ro" }, urls = { "http://[\\w\\.]*?filebox\\.ro/download.php\\?key=[0-9a-z]{32}|http://[\\w\\.]*?fbx\\.ro/[0-9a-z]{16}" }, flags = { 2 })

public class FileboxRo extends PluginForHost {

    public FileboxRo(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    
    // @Overrid
    public void correctDownloadLink(DownloadLink link) throws Exception { 
        this.setBrowserExclusive();
        br.setCookie(link.getDownloadURL(), "filebox_language", "en");
        if (Regex.matches(link.getDownloadURL(), "http://[\\w\\.]*?fbx\\.ro/[0-9a-z]{16}")) {
            br.setFollowRedirects(true);
            br.getPage(link.getDownloadURL());
            String urlpart = "http://www.filebox.ro/download.php?key=";
            String correctUrl = urlpart + br.getRegex("window\\.location\\.href='http://www\\.filebox\\.ro/download\\.php\\?key=(\\w{32})';").getMatch(0);
            link.setUrlDownload(correctUrl);
        }
    }
    
    // @Override
    public String getAGBLink() {
        return "http://www.filebox.ro/disclaimer.php?english=1";
    }
    
    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(downloadLink.getDownloadURL(), "filebox_language", "en");
        br.getPage(downloadLink.getDownloadURL());
        
        if (!(br.containsHTML("File deleted or file lifespan expired") || br.containsHTML("Wrong link") || br.containsHTML("Filebox.ro is temporarily not available."))) {
            String filename = br.getRegex("Name:</span> <strong>(.*?)</strong>").getMatch(0);
            String filesize = br.getRegex("Size:</span> <strong>(.*?)</strong>").getMatch(0);
            if (!(filename == null || filesize  == null)) {
                downloadLink.setName(filename);
                downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
                return AvailableStatus.TRUE;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }
    
    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        if(br.containsHTML("<div id='download_wait_button'>Please wait.*</div>")) {
            String strwait = br.getRegex("Please wait <span id='seconds'>(.*?)</span>").getMatch(0);
            long waittime = Long.parseLong(strwait.trim());
            waittime = (waittime * 1000) +1;
            this.sleep(waittime, downloadLink); 
        }
        String id = getDownloadId(downloadLink);
        br.getPage("http://www.filebox.ro/download.php?key=" + id);
        String prefix = br.getRegex("http://(\\w+)\\.filebox\\.ro/get_file\\.php\\?key=[0-9a-z]{32}").getMatch(0);
        String linkurl = "http://" + prefix + ".filebox.ro/get_file.php?key=" + id;
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = br.openDownload(downloadLink, linkurl, false, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        dl.startDownload();
    }
    
    public String getDownloadId(DownloadLink downloadLink) {
        String[] id = downloadLink.getDownloadURL().split("=");
        return id[1];
    }
    
    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }
    
    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
