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

//This plugin only takes decrypted links from the livedrive decrypter
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livedrive.com" }, urls = { "http://[a-z0-9]+\\.livedrivedecrypted\\.com/item/[a-z0-9]{32}" }, flags = { 0 })
public class LiveDriveCom extends PluginForHost {

    public LiveDriveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("livedrivedecrypted.com/", "livedrive.com/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.livedrive.com/terms-of-use";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        final String filename = br.getRegex("<div id=\"Preview\">[\t\n\r ]+<img src=\"/Content/Images/filetypes/180x230/[^<>\"/]*?\" alt=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String liveDriveUrlUserPart = new Regex(downloadLink.getDownloadURL(), "(.*?)\\.livedrive\\.com").getMatch(0);
        liveDriveUrlUserPart = liveDriveUrlUserPart.replaceAll("(http://|www\\.)", "");
        final String aid = br.getRegex("\\'(\\d+)\\'\\);\">Download</a>").getMatch(0);
        if (aid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String dllink = "http://" + liveDriveUrlUserPart + ".livedrive.com/IO/DownloadSharedFile?NodeID=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]{32})$").getMatch(0) + "&AID=" + aid;
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
    public void resetDownloadlink(DownloadLink link) {
    }

}