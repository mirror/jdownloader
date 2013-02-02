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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "omploader.org" }, urls = { "http://(www\\.)?(ompldr|omploader)\\.org/[A-Za-z0-9]+" }, flags = { 0 })
public class OmpLoaderOrg extends PluginForHost {

    public OmpLoaderOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://ompldr.org/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("ompldr.org/", "omploader.org/"));
    }

    private String DLLINK = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            if (con.getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (!con.getContentType().contains("html")) {
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                link.setDownloadSize(con.getLongContentLength());
                DLLINK = link.getDownloadURL();
                return AvailableStatus.TRUE;
            } else {
                br.followConnection();
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Exception e) {
            }
        }
        if (br.containsHTML(">Nothing to pee here") || br.containsHTML("<title>omploader</title>") || !br.containsHTML("omploader")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("class=\"container\"><a href=\"[A-Za-z0-9]+/([^<>\"/]*?)\"").getMatch(0);
        final String filesize = br.getRegex("<strong>Size:</strong></div><div class=\"right\">([^<>\"]*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (DLLINK == null) {
            DLLINK = br.getRegex("<strong>Name:</strong></div><div class=\"right\"><a href=\"([A-Za-z0-9]+/[^<>\"/]*?)\"").getMatch(0);
            if (DLLINK != null) DLLINK = "http://ompldr.org/" + DLLINK;
        }
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}