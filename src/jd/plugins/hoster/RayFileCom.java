//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rayfile.com" }, urls = { "http://[\\w]*?\\.rayfile\\.com/[^/]+/files/[^/]+/" }) 
public class RayFileCom extends PluginForHost {

    private String userAgent = null;
    protected long fileSize  = -1;

    public RayFileCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public void prepBrowser(final Browser br) {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        // br.setCookie("http://rayfile.com", "lang", "english");
        if (userAgent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            userAgent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", userAgent);
        br.setConnectTimeout(2 * 60 * 1000);
        br.setReadTimeout(2 * 60 * 1000);

    }

    @Override
    public String getAGBLink() {
        return "http://dyn.www.rayfile.com/en/copyrights/";
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        // use their regex
        String vid = br.getRegex("var vid = \"(.*?)\"").getMatch(0);
        String vkey = br.getRegex("var vkey = \"(.*?)\"").getMatch(0);

        Browser ajax = this.br.cloneBrowser();
        ajax.getPage("http://www.rayfile.com/zh-cn/files/" + vid + "/" + vkey + "/");

        String downloadUrl = ajax.getRegex("downloads_url = \\['(.*?)'\\]").getMatch(0);
        if (downloadUrl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        br.clearCookies("rayfile.com");
        br.getHeaders().put("Accept-Encoding", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Referer", null);
        br.getHeaders().put("User-Agent", "Grid Service 2.1.10.8366");

        /* setup Range-Header only available for JD2 */
        downloadLink.setProperty("ServerComaptibleForByteRangeRequest", true);

        // IMPORTANT: resuming must be set to false.
        // Range: bytes=resuming bytes - filesize -> not work
        // Range: bytes=resuming bytes - resuming bytes + 5242880 -> work
        // resuming limitations: 1 chunk @ max 5242880 bytes Range!

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, false, 1);

        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        this.dl.startDownload();
    }

    private RAFDownload createHackedDownloadInterface2(PluginForHost plugin, final DownloadLink downloadLink, final Request request) throws IOException, PluginException {
        request.getHeaders().put("Range", "bytes=" + (0) + "-");
        final RAFDownload dl = new RAFDownload(plugin, downloadLink, request) {

            private boolean connected;

            @Override
            public URLConnectionAdapter connect() throws IOException, PluginException {
                if (connected) {
                    return connection;
                }
                logger.finer("Connect...");
                this.connected = true;
                br.openRequestConnection(request, false);
                if (getBrowser().isDebug()) {
                    logger.finest(request.printHeaders());
                }
                connection = request.getHttpConnection();
                if (request.getLocation() != null) {
                    throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED);
                }
                if (connection.getRange() != null) {
                    // Dateigröße wird aus dem Range-Response gelesen
                    if (connection.getRange()[2] > 0) {
                        downloadLink.setDownloadSize(connection.getRange()[2]);
                    }
                } else {
                    if (connection.getLongContentLength() > 0) {
                        downloadLink.setDownloadSize(connection.getLongContentLength());
                    }
                }
                fileSize = downloadLink.getDownloadSize();
                return connection;
            }

        };

        plugin.setDownloadInterface(dl);
        dl.setResume(false);
        dl.setChunkNum(1);
        return dl;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.getPage(link.getDownloadURL());

        if (br.containsHTML("Not HTML Code. Redirect to: ")) {
            String redirectUrl = br.getRequest().getLocation();
            link.setUrlDownload(redirectUrl);
            br.getPage(redirectUrl);
        }

        if (br.containsHTML("page404")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        String filename = br.getRegex("var fname = \"(.*?)\";").getMatch(0);
        String filesize = br.getRegex("formatsize = \"(.*?)\";").getMatch(0);

        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(filename.trim());
        if (filesize != null && !filesize.equals("")) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}