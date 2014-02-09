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
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "file-space.org" }, urls = { "http://(www\\.)?file\\-space\\.org/files/(free|new)?get/[A-Za-z0-9\\-_]+/[^<>\"/]+\\.html" }, flags = { 0 })
public class FileSpaceOrg extends PluginForHost {

    public FileSpaceOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://file-space.org/support.html";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("/(free|new)?get/", "/get/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File Link Error<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("id=\"name\"><nobr>([^<>\"]*?)</nobr>").getMatch(0);
        final String filesize = br.getRegex("<span id=\"size\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        final String md5 = br.getRegex("<span id=\"md5\">([a-f0-9]{32})</span>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            br.getPage(downloadLink.getDownloadURL().replace("/get/", "/freeget/"));
            int wait = 30;
            final String waittime = br.getRegex("var time_load_link = (\\d+);").getMatch(0);
            if (waittime != null) wait = Integer.parseInt(waittime);
            sleep(wait * 1001l, downloadLink);
            final String fid = new Regex(downloadLink.getDownloadURL(), "/get/([A-Za-z0-9\\-_]+)").getMatch(0);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("http://file-space.org/files/getlink/" + fid + ".html");
            dllink = br.getRegex("\\&url=([A-Za-z0-9%=]+)\\&").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = Encoding.Base64Decode(dllink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
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