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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "motherless.com" }, urls = { "http://([\\w\\.]*?|members\\.)(motherless\\.com/(movies|thumbs).*|motherlesspictures\\.com/[a-zA-Z0-9/.]+)" }, flags = { 0 })
public class MotherLessCom extends PluginForHost {

    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2500l);
    }

    public String getAGBLink() {
        return "http://motherless.com/terms";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("motherlesspictures", "motherless"));
    }

    public AvailableStatus requestFileInformation(DownloadLink parameter) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.openGetConnection(parameter.getDownloadURL());
        if (br.containsHTML("Not Available") || br.containsHTML("not found") || br.containsHTML("You will be redirected to")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(Plugin.getFileNameFromHeader(br.getHttpConnection()));
        parameter.setDownloadSize(br.getHttpConnection().getLongContentLength());
        br.getHttpConnection().disconnect();
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink link) throws Exception {
        if (!link.getDownloadURL().contains("/img/")) requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
