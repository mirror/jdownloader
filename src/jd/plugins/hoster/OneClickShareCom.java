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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1-clickshare.com" }, urls = { "http://(www\\.)?1\\-clickshare\\.com/(\\d+|download2\\.php\\?a=\\d+)" }, flags = { 0 })
public class OneClickShareCom extends PluginForHost {

    public OneClickShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.1-clickshare.com/index.php?page=tos";
    }

    private static final String PASSWORDTEXT = "(>Password Protected|name=\"codefile\")";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("download2\\.php\\?a=", ""));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Invalid download link")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(PASSWORDTEXT)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.oneclicksharecom.passwordprotected", "This link is password protected"));
            return AvailableStatus.TRUE;
        }
        Regex linkInfo = br.getRegex("<br /><h1>File: (.*?) \\- Size: (.*?)</h1>");
        String filename = linkInfo.getMatch(0);
        String filesize = linkInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String passCode = downloadLink.getStringProperty("pass", null);
        if (br.containsHTML(PASSWORDTEXT)) {
            if (passCode == null) passCode = Plugin.getUserInput("Password?", downloadLink);
            br.getPage("http://www.1-clickshare.com/download.php?codefile=" + new Regex(downloadLink.getDownloadURL(), "1\\-clickshare\\.com/(\\d+)").getMatch(0) + "&pass=" + Encoding.urlEncode(passCode));
            if (br.containsHTML(PASSWORDTEXT)) {
                logger.info("User entered wrong password...");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
        }
        String dllink = br.getRegex("document\\.getElementById\\(\"dl\"\\)\\.innerHTML = \\'<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://(www\\.)?1\\-clickshare\\.com/download2\\.php\\?a=\\d+\\&b=[a-z0-9]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
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