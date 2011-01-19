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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dynyoo.com" }, urls = { "http://(www\\.)?dynyoo\\.com/\\?goto=dl\\&id=[a-z0-9]{32}" }, flags = { 0 })
public class DynYooCom extends PluginForHost {

    public DynYooCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dynyoo.com/?goto=faq";
    }

    private static final String FILEIDREGEX = "dynyoo\\.com/\\?goto=dl\\&id=(.+)";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // We use an API
        // Problems with the API ? Contact info@dynyoo.com
        br.getPage("http://dynyoo.com/api/status.php?id=" + new Regex(link.getDownloadURL(), FILEIDREGEX).getMatch(0));
        Regex info = br.getRegex("(.*?);(.*?);(.*?)");
        if (br.containsHTML("invalid")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (info.getMatch(0) == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!info.getMatch(0).equals("online")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = info.getMatch(1);
        String filesize = info.getMatch(2);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, "http://dynyoo.com/downloadnow.php?id=" + new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The finallink doesn't seem to be a file...");
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