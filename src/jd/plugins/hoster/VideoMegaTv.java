//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videomega.tv" }, urls = { "http://(www\\.)?videomega\\.tv/(?:(?:(?:iframe|cdn|view|genembedv2)\\.php)?\\?ref=|validatehash\\.php\\?hashkey=)[A-Za-z0-9]+" }, flags = { 0 })
public class VideoMegaTv extends antiDDoSForHost {

    public VideoMegaTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://videomega.tv/terms.html";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("http://videomega.tv/view.php?ref=" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
        link.setContentUrl("http://videomega.tv/genembedv2.php?ref=" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    private String               fuid              = null;
    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        br = new Browser();
        fuid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        // Check filename from decrypter first, if any
        if (link.getFinalFileName() == null) {
            link.setFinalFileName(fuid + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        // lets set all referrer to home page
        br.getHeaders().put("Referer", "http://videomega.tv/");
        getPage("http://videomega.tv/view.php?ref=" + fuid);
        String dllink = br.getRegex("<source src=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink != null) {
            URLConnectionAdapter con = br.openGetConnection(dllink);
            Long size = con.getContentLength();
            link.setDownloadSize(SizeFormatter.getSize(String.valueOf(size)));
            if (size < 88000) {
                logger.info("ContentLength too small: " + size + ", broken video.");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            con.disconnect();
        }
        // br is empty here, re-get the page
        getPage("http://videomega.tv/view.php?ref=" + fuid);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        String dllink = null;
        requestFileInformation(downloadLink);
        /* New way */
        dllink = br.getRegex("<source src=\"(http://[^<>\"]*?)\"").getMatch(0);
        final String id = br.getRegex("id: \"(\\d+)\"").getMatch(0);
        if (dllink == null || id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // br.getHeaders().put("Accept", "*/*");
        // br.getHeaders().put("Accept-Encoding", "identity;q=1, *;q=0");
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (dl.getConnection().getLongContentLength() < 10000l) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: File is too small", 60 * 60 * 1000l);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}