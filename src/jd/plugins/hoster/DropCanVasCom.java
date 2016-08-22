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
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dropcanvas.com" }, urls = { "https?://(?:www\\.)?dropcanvas\\.com/[a-z0-9]+/\\d+" })
public class DropCanVasCom extends antiDDoSForHost {

    public DropCanVasCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dropcanvas.com/terms-and-conditions-of-service";
    }

    private String  dllink            = null;
    private boolean isNotDownloadable = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = openAntiDDoSRequestConnection(br, br.createGetRequest(link.getDownloadURL()));
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                dllink = link.getDownloadURL();
                return AvailableStatus.TRUE;
            } else {
                br.followConnection();
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (br.containsHTML(">The file you requested could not be found|class=\"canvasNotAvailable\"|>File not found\\.\\.\\.</div>") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex(">Please wait while we fetch your download</h3>([^<>\"]*?)<br />").getMatch(0);
        if (filename == null) {
            isNotDownloadable = true;
            filename = br.getRegex("<div class=\"fileNameView\">\\s*(.*?)\\s*&nbsp;").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = filename.trim();
        if (filename.equals("")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setName(Encoding.htmlDecode(filename));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (dllink == null) {
            dllink = checkDirectLink(downloadLink, "directlink");
            if (dllink == null) {
                if (isNotDownloadable) {
                    // images etc...
                    dllink = br.getRegex("<img\\s+class=\"big dragout\"\\s+src=\"(.*?)\"").getMatch(0);
                }
                if (dllink == null) {
                    final String minwait = br.getRegex("queueMinDrop = (\\d+);").getMatch(0);
                    final String beginqueue = br.getRegex("queue = (\\d+);").getMatch(0);
                    if (minwait == null || beginqueue == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final int minWait = Integer.parseInt(minwait);
                    final int beginQueue = Integer.parseInt(beginqueue);
                    final int wait = beginQueue * minWait;
                    sleep(1001 * wait, downloadLink);
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    final Regex dlInfo = new Regex(downloadLink.getDownloadURL(), "dropcanvas\\.com/([a-z0-9]+)/(\\d+)");
                    br.postPage("http://dropcanvas.com/download/getDownloadLink", "albumId=" + dlInfo.getMatch(0) + "&indx=" + dlInfo.getMatch(1));
                    dllink = PluginJSonUtils.getJsonValue(br, "downloadLink");
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("\"error\":\"Unknown error\"")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Waittime error, please contact our support!");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
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