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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "host.hackerbox.org" }, urls = { "http://[\\w\\.]*?host\\.hackerbox\\.org/download\\.php\\?file=[a-z0-9]+" }, flags = { 0 })
public class HostHackerboxOrg extends PluginForHost {

    public HostHackerboxOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://host.hackerbox.org/?page=tos";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<h1>Invalid download link")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Password Protected: <input")) {
            link.getLinkStatus().setStatusText("This file is password protected");
            if (link.getStringProperty("pass", null) != null) {
                handlePassword(link.getStringProperty("pass", null), link);
            } else
                return AvailableStatus.TRUE;
        }
        String filename = br.getRegex("<d2>File Name[ ]+:(.*?)</d2>").getMatch(0);
        String filesize = br.getRegex("<d2>File Size[ ]+:(.*?)</d2>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String passCode = null;
        if (br.containsHTML("Password Protected: <input")) {
            handlePassword(passCode, downloadLink);
        }
        String dllink = br.getRegex("document\\.getElementById\\(\"dl\"\\)\\.innerHTML = '<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://host\\.hackerbox\\.org/download2\\.php\\?a=[0-9a-z]+\\&b=[0-9a-z]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public String handlePassword(String passCode, DownloadLink downloadLink) throws IOException, PluginException {
        for (int i = 0; i <= 3; i++) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }

            br.postPage(downloadLink.getDownloadURL(), "pass=" + passCode);
            logger.info("File is password protected, password = " + passCode);
            if (br.containsHTML("Password Protected: <input")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                downloadLink.setProperty("pass", null);
                continue;
            }
            break;
        }
        if (br.containsHTML("Password Protected: <input")) {
            logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        return passCode;
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