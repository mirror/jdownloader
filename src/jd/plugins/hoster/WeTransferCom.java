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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wetransfer.com" }, urls = { "http(s)?://(www\\.)?(wtrns\\.fr/[A-Za-z0-9\\-]+|wetransfer\\.com/dl/[A-Za-z0-9]+/[a-z0-9]+)" }, flags = { 0 })
public class WeTransferCom extends PluginForHost {

    public WeTransferCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        // No TOS/TOSlink found
        return "https://wetransfer.com/";
    }

    private String              HASH             = null;
    private String              CODE             = null;
    private static final String POSTDOWNLOADLINK = "https://crazycatlady3.wetransfer.com/fsdl.php";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String nextLink = br.getRedirectLocation();
        if (nextLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        HASH = new Regex(nextLink, "hash=([a-z0-9]+)").getMatch(0);
        CODE = new Regex(nextLink, "code=([A-Za-z0-9]+)").getMatch(0);
        if (HASH == null || CODE == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openPostConnection(POSTDOWNLOADLINK, "hash=" + HASH + "&flash=WIN%2010%2C3%2C183%2C7&corporate=null&profile=1&code=" + CODE);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(getFileNameFromHeader(con));
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // More chunks are possible for some links but not for all
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, POSTDOWNLOADLINK, "hash=" + HASH + "&flash=WIN%2010%2C3%2C183%2C7&corporate=null&profile=1&code=" + CODE, true, -2);
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
    public void resetDownloadlink(DownloadLink link) {
    }

}