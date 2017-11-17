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

import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "androidfilehost.com" }, urls = { "https?://(www\\.)?androidfilehost\\.com/\\?fid=\\d+" })
public class AndroidFileHostCom extends antiDDoSForHost {
    public AndroidFileHostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.androidfilehost.com/terms-of-use.php";
    }

    public void correctDownloadLink(final DownloadLink link) {
        /* Forced https */
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.containsHTML(">file not found|404 not found") || br.getHttpConnection().getResponseCode() == 404 || (br.containsHTML("<h3>access denied</h3>") && br.containsHTML("you don't have permission to access this folder\\."))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("id=\"filename\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("name=\"file_size\" id=\"file_size\" value=\"(\\d+)\"").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(Long.parseLong(filesize));
        final String md5 = br.getRegex("<h4>md5</h4>[\t\n\r ]+<p><code>([a-f0-9]{32})</code>").getMatch(0);
        if (md5 != null) {
            link.setMD5Hash(md5.trim());
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* Re-usage of old links disabled for testing. */
        // String dllink = checkDirectLink(downloadLink, "directlink");
        String dllink = null;
        if (dllink == null) {
            /* Old handling removed AFTER revision 26995 */
            final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
            // sleep(10 * 1001l, downloadLink);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            postPage("/libs/otf/mirrors.otf.php", "submit=submit&action=getdownloadmirrors&fid=" + fid);
            final String[] mirrors = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getColumn(0);
            if (mirrors == null || mirrors.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            URLConnectionAdapter con = null;
            boolean success = false;
            for (final String mirror : mirrors) {
                dllink = mirror.replace("\\", "");
                try {
                    con = br.openGetConnection(dllink);
                    con.disconnect();
                    if (con.getResponseCode() == 404) {
                        continue;
                    }
                    success = true;
                    break;
                } catch (final Exception e) {
                    dllink = null;
                }
            }
            if (!success) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000l);
            }
        }
        // Disabled chunks and resume because different downloadserver = different connection limits
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            /* Check again for server error 404 just to make sure... */
            if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
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