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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "minhateca.com.br" }, urls = { "http://minhatecadecrypted\\.com\\.br/\\d+" }, flags = { 0 })
public class MinhatecaComBr extends PluginForHost {

    public MinhatecaComBr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://minhateca.com.br/termosecondicoes.aspx";
    }

    private static boolean pluginloaded = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getStringProperty("mainlink", null));
        if (br.containsHTML("class=\"noFile\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = link.getStringProperty("plain_filename", null);
        final String filesize = link.getStringProperty("plain_filesize", null);
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String req_token = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String fid = downloadLink.getStringProperty("plain_fid", null);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            if (req_token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://minhateca.com.br/action/License/Download", "fileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(req_token));
            if (dllink == null) dllink = br.getRegex("\"redirectUrl\":\"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = unescape(dllink);
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

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) throw new IllegalStateException("youtube plugin not found!");
            pluginloaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
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