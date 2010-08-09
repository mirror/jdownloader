//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "upfull.com" }, urls = { "http://[\\w\\.]*?upfull\\.com/files/get/[a-z0-9]+" }, flags = { 0 })
public class UpFullCom extends PluginForHost {

    public UpFullCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.upfull.com/legal/tos";
    }

    private static final String PASSWORDTEXT = "name=\"pass\"";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(> File Link Error</h2>|Your file could not be found\\. Please check the download link\\.</p>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(PASSWORDTEXT)) {
            String filename = br.getRegex("<form action=\"http://(www\\.)?upfull\\.com/files/get/.*?/(.*?)\"").getMatch(1);
            if (filename != null) link.setName(filename.trim());
            link.getLinkStatus().setStatusText("This file is password protected");
        } else {
            String filename = br.getRegex("class=\"h1namefile\">(.*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("alt=\\'Up Book - Download (.*?)\\'").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("\\&amp;title=(.*?)\"").getMatch(0);
                }
            }
            String filesize = br.getRegex("id=\"size\">(.*?)</span>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setName(filename.trim());
            if (filesize != null) link.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String passCode = "";
        if (br.containsHTML(PASSWORDTEXT)) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            br.postPage(downloadLink.getDownloadURL(), "pass=" + passCode);
            if (br.containsHTML(">The Password you submited was incorrect\\.<")) throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        String md5 = br.getRegex("id=\"md5\">(.*?)</span>").getMatch(0);
        if (md5 != null) downloadLink.setMD5Hash(md5.trim());
        br.setFollowRedirects(false);
        br.postPage(downloadLink.getDownloadURL().replace("/get/", "/gen/"), "pass=" + passCode + "&waited=1");
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null && passCode.length() != 0) {
            downloadLink.setProperty("pass", passCode);
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