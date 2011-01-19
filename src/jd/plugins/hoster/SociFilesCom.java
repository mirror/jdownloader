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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "socifiles.com" }, urls = { "http://(www\\.)?socifiles\\.com/d/[0-9a-z]+" }, flags = { 0 })
public class SociFilesCom extends PluginForHost {

    public SociFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://about.socifiles.com/terms-of-service";
    }

    private static final String PASSWORDPROTECTED = ">This file is protected by password\\.<";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(or was removed|is not existed)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Filename:</span>(.*?)</li>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Download(.*?)- SociFiles\\.com</title>").getMatch(0);
        String filesize = br.getRegex(">Size:</span>(.*?)</li>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        if (br.containsHTML(PASSWORDPROTECTED)) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.socifilescom.passwordprotected", "This link is password protected"));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String passCode = null;
        if (br.containsHTML(PASSWORDPROTECTED)) {
            logger.info("This link seems to be password protected...");
            String fileID = new Regex(downloadLink.getDownloadURL(), "socifiles\\.com/d/(.+)").getMatch(0);
            if (fileID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            br.postPage(downloadLink.getDownloadURL(), "password=" + passCode + "&id=" + fileID + "&submit=Submit");
            if (br.containsHTML(PASSWORDPROTECTED)) {
                logger.info("The user entered a wrong password, retrying...");
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password!");
            }
        }
        Form dlform = br.getFormbyProperty("id", "download-form");
        if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(dlform);
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
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