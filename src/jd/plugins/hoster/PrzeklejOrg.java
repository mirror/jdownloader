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

import org.appwork.utils.formatter.SizeFormatter;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "przeklej.org" }, urls = { "https?" + "://(www\\.)?(?:przeklej\\.org|easypaste\\.org)/file/[A-Za-z0-9]+" })
public class PrzeklejOrg extends PluginForHost {

    public PrzeklejOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String domains = "(?:przeklej\\.org|easypaste\\.org)";

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "przeklej.org", "easypaste.org" };
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("przeklej.org/", "easypaste.org/").replaceAll("http://", "https://"));
    }

    @Override
    public String getAGBLink() {
        return "https://www.easypaste.org/terms-of-use?lang=en";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        correctDownloadLink(link);
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(link.getPluginPatternMatcher() + "?lang=en");
        if (br.containsHTML(">\\s*file was removed\\s*<") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String jsredirect = br.getRegex("<script>location\\.href=\"(https?://(www\\.)?" + domains + "/file/[^<>\"]*?)\"</script>").getMatch(0);
        if (jsredirect != null) {
            br.getPage(jsredirect);
        }
        String filename = br.getRegex("\"name\":\\s*\"(.*?)\",").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>\\s*([^<>\"]*?)\\s*-\\s*(?:file on\\s*)" + domains + "\\s*</title>").getMatch(0);
        }
        final String filesize = br.getRegex("\"contentSize\":\\s*\"(\\d+)\",").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(encodeUnicode(Encoding.htmlDecode(filename.trim())));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            dllink = br.getRegex("<a href=\"(/file/download/\\w+.*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, false, 1);
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