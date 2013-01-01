//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filebeam.com" }, urls = { "http://(www\\.)?(filebeam\\.com/[a-z0-9]{32}|fbe\\.am/[A-Za-z0-9]+)" }, flags = { 0 })
public class FileBeamCom extends PluginForHost {

    public FileBeamCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filebeam.com/index.php?page=tos";
    }

    private static final String PASSWORDTEXT = ">Password Protected File<";
    private static final String SHORTLINK    = "http://(www\\.)?fbe\\.am/[A-Za-z0-9]+";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (link.getDownloadURL().matches(SHORTLINK)) {
            link.setUrlDownload(br.getURL());
        }
        if (br.containsHTML("(>This file does not exist|This file was either delete by the uploader|or the file was never uploaded\\.<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(PASSWORDTEXT)) {
            link.getLinkStatus().setStatusText("This file is password protected.");
            link.setName(link.getDownloadURL().substring(link.getDownloadURL().lastIndexOf("/")));
            return AvailableStatus.TRUE;
        }
        String filename = br.getRegex(">File Download Area</center></h1><center><h3>(.*?)</h3>").getMatch(0);
        String filesize = br.getRegex("<center>File Size \\- ([^<>]+) <br").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String passCode = downloadLink.getStringProperty("pass", null);
        requestFileInformation(downloadLink);
        if (br.containsHTML(PASSWORDTEXT)) {
            if (passCode == null) passCode = Plugin.getUserInput("Password?", downloadLink);
            br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode));
            if (br.containsHTML(PASSWORDTEXT)) throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
        }
        String dllink = br.getRegex("class=tablebcolor><tr><td><b><font size=5><a href=(http://[^<>\"]+)>").getMatch(0);
        if (dllink == null) dllink = br.getRegex("(http://filebeam\\.com/download2\\.php\\?a=[a-z0-9]{32}\\&b=[a-z0-9]{32})").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}