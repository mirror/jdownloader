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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "promptfile.com" }, urls = { "http://(www\\.)?promptfile\\.com/l/[A-Z0-9]+\\-[A-Z0-9]+" }, flags = { 0 })
public class PromptFileCom extends PluginForHost {

    public PromptFileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.promptfile.com/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().equals("http://www.promptfile.com/?404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fInfo = br.getRegex("<span style=\"text\\-decoration:none;cursor:text;\" title=\"([^<>\"]*?)\">([^<>\"]*?) \\((\\d+(\\.\\d{1,2})? [A-Za-z]{2,5})\\)</span>");
        final String filename = fInfo.getMatch(1);
        final String filesize = fInfo.getMatch(2);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // if (isStable()) throw new PluginException(LinkStatus.ERROR_FATAL, "Only supported in the JDownloader 2 BETA!");
        final String cHash = br.getRegex("name=\"chash\" value=\"([a-z0-9]+)\"").getMatch(0);
        if (cHash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage(br.getURL(), "chash=" + cHash);
        final String dllink = br.getRegex("clip: \\{\\s+url: \\'(http://(www\\.)?promptfile\\.com/file/[A-Za-z0-9=]+)(\\'|\")").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            br.followConnection();
            if (br.containsHTML("file unavailable")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: File unavailable", 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private boolean isStable() {
        if (System.getProperty("jd.revision.jdownloaderrevision") == null)
            return true;
        else
            return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}