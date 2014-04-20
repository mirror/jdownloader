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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "droplr.com" }, urls = { "https?://(www\\.)?(droplr\\.com|d\\.pr)/(f/[A-Za-z0-9]+|[A-Za-z0-9]+/[A-Za-z0-9]+/[A-Za-z0-9]+)" }, flags = { 0 })
public class DroplrCom extends PluginForHost {

    public DroplrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://help.droplr.com/customer/portal/articles/989945-terms-conditions";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("d.pr/", "droplr.com/"));
    }

    private static final String INVALIDLINKS = "https?://droplr\\.com/join/d/.+";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getDownloadURL().matches(INVALIDLINKS)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">The password you entered for the drop was incorrect") || br.getHttpConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("data\\-filename=\"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("class=\"file\\-size\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dtoken = br.getRegex("data\\-dtoken=\"([a-z0-9]+)\"").getMatch(0);
        String dllink = br.getRegex("data\\-link=\"(http[^<>\"]*?)\" id=\"download\"").getMatch(0);
        if (dllink == null || dtoken == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = downloadLink.getDownloadURL() + "/download?link=" + dllink;
        dllink += "&filename=" + Encoding.urlEncode(downloadLink.getName());
        dllink += "&dtoken=" + dtoken;
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage(dllink);
        dllink = br.getRegex("\"signed_link\":\"(http[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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