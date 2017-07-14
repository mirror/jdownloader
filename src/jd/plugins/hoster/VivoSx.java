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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vivo.sx" }, urls = { "https?://(www\\.)?vivo\\.sx/[a-z0-9]{10}" })
public class VivoSx extends PluginForHost {

    public VivoSx(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://" + domain + "/terms";
    }

    /* Similar: shared.sx, vivo.sx */
    private static final String domain = "shared.sx";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">The file you have requested does not exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("og:description\" content=\"(Watch|Listen to) (\\&#34;)?([^<>\"]*?)(\\&#34;)? on").getMatch(2);
        if (filename == null) {
            filename = br.getRegex("data\\-type=\"video\">(Watch|Listen to) ([^<>\"]*?)(\\&nbsp;)?<strong>").getMatch(1);
        }
        final String filesize = br.getRegex("<strong>\\((\\d+(\\.\\d{2})? (KB|MB|GB))\\)</strong>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = encodeUnicode(Encoding.htmlDecode(filename.trim()));
        if (filename.endsWith(" (...)")) {
            filename = filename.replace(" (...)", ".mp4");
        }
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            dllink = br.getRegex("(https?://[^<>\"]+/get/[^<>\"]+)").getMatch(0);
            if (dllink == null) {
                /* 2016-10-24 */
                dllink = br.getRegex("Core\\.InitializeStream\\s*\\('([^']+)'").getMatch(0);
                if (dllink != null) {
                    dllink = Encoding.Base64Decode(dllink);
                    dllink = PluginJSonUtils.unescape(dllink);
                    dllink = new Regex(dllink, "(https?://[^<>\"]+/get/[^<>\"]+)").getMatch(0);
                }
            }
            if (dllink == null) {
                final String hash = br.getRegex("type=\"hidden\" name=\"hash\" value=\"([^<>\"]*?)\"").getMatch(0);
                final String expires = br.getRegex("type=\"hidden\" name=\"expires\" value=\"([^<>\"]*?)\"").getMatch(0);
                final String timestamp = br.getRegex("type=\"hidden\" name=\"timestamp\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (hash == null || timestamp == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                String postData = "hash=" + hash + "&timestamp=" + timestamp;
                if (expires != null) {
                    postData += "&expires=" + expires;
                }
                br.postPage(br.getURL(), postData);
                dllink = br.getRegex("class=\"stream-content\" data-url=\"(http[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        try {
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("Accept", "*/*");
            brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            brc.getHeaders().put("", "");
            brc.postPage("/request", "action=view&abs=false&hash=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
        } catch (final Throwable e) {
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
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
        return 4;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}