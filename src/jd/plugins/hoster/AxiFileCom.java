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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "axifile.com" }, urls = { "http://[\\w\\.]*?axifile\\.com/(\\?|mydownload\\.php\\?file=)\\d+" }, flags = { 0 })
public class AxiFileCom extends PluginForHost {
    public AxiFileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.axifile.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Download shared file \\| (.*?)</TITLE>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex(">You have request \"(.*?)\" file").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("content=\"FAST AND SAFE DOWNLOAD (.*?)\">").getMatch(0);
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String filesize = br.getRegex(">You have request \".*?\" file \\((.*?)\\)</DIV>").getMatch(0);
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String link = (downloadLink.getDownloadURL());
        String link2 = link.replace(".com/?", ".com/mydownload.php?file=");
        br.getPage(link2);
        // password protected-links-handling
        if (br.containsHTML("This file is password protected")) {
            String passCode = null;
            Form pwform = br.getForm(0);
            if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            pwform.put("pwd", passCode);
            br.submitForm(pwform);
            if (br.containsHTML("This file is password protected")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
        }
        String dllink = br.getRegex("pnlLink1\"><b>.*?</b><br> <A href=\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://dl\\d+\\.axifile\\.com/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -3);
        /*
         * hoster supported wahlweise 3 files mit 1 chunk oder 1 file mit 3
         * chunks
         */
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
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
