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
import java.util.Random;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "24video.xxx" }, urls = { "http://(?:www\\.)?24video\\.(?:net|xxx|sex|adult)/video/view/\\d+" })
public class TwenteeFourVideoXxx extends PluginForHost {
    public TwenteeFourVideoXxx(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("24video\\.(net|xxx|sex)/", "24video.adult/"));
        link.setContentUrl(link.getDownloadURL().replaceFirst("24video\\.(net|xxx|sex)/", "24video.adult/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.24video.adult/staticPage/view/agreement_en";
    }

    @Override
    public String rewriteHost(String host) {
        if ("24video.net".equals(getHost())) {
            if (host == null || "24video.net".equals(host)) {
                return "24video.xxx";
            }
        }
        return super.rewriteHost(host);
    }

    private Browser ajax = null;

    private void ajaxGetPage(final String string) throws IOException {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.getPage(string);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        /* Fix old urls */
        correctDownloadLink(link);
        br.setFollowRedirects(true);
        // are you 18 ?
        br.setCookie(Browser.getHost(link.getDownloadURL()), "plus18-1", "true");
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<video><error")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        ajaxGetPage("/video/xml/" + getFID(link) + "?mode=init");
        String filename = ajax.getRegex("txt='([^<>]*?)'").getMatch(0);
        // final String filesize = ajax.getRegex("filesize='(\\d+)'").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = encodeUnicode(Encoding.htmlDecode(filename.trim()) + ".mp4");
        link.setFinalFileName(filename);
        // link.setDownloadSize(Long.parseLong(filesize));
        String dllink = ajax.getRegex("<video url=('|\")(http[^<>\"]*?)\\1").getMatch(1);
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (!con.getContentType().contains("html") && con.getLongContentLength() != 0) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setProperty("directlink", Property.NULL);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            final String fid = getFID(downloadLink);
            ajaxGetPage("/auth/setSession?id=" + br.getCookie(Browser.getHost(downloadLink.getDownloadURL()), "JSESSIONID"));
            // lets place some random time in here
            sleep(new Random().nextInt(10) * 1001l, downloadLink);
            ajaxGetPage("/video/xml/" + fid + "?mode=play");
            dllink = ajax.getRegex("<video url=('|\")(http[^<>\"]*?)\\1").getMatch(1);
            if (dllink == null) {
                dllink = ajax.getRegex("\"(http://dl\\.24video[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.htmlDecode(dllink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String getFID(final DownloadLink dl) {
        final String fid = new Regex(dl.getDownloadURL(), "(\\d+)$").getMatch(0);
        return fid;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
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