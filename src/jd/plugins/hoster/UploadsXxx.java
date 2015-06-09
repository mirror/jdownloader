//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;

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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploads.xxx" }, urls = { "http://(www\\.)?uploads\\.xxx/video/[A-Za-z0-9\\-_]+\\-[A-Z0-9]+/" }, flags = { 0 })
public class UploadsXxx extends PluginForHost {

    public UploadsXxx(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 1;

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        this.br.setCookie(this.getHost(), "xxx_lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">404 - File not found|>Sorry, but the specified file may have been deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String format = br.getRegex(">Format: <b>([^<>\"]*?)</b>").getMatch(0);
        String filename = br.getRegex("class=\"title\">[\t\n\r ]+<h\\d+>([^<>\"]*?)</h\\d+>").getMatch(0);
        String filesize = br.getRegex(">Size: <b>([^<>\"]+)<").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (format == null) {
            format = "MP4";
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + "." + format);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings({ "deprecation" })
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            final String fid = new Regex(downloadLink.getDownloadURL(), "\\-([A-Z0-9]+)/").getMatch(0);
            br.getPage("/play/" + fid + "/");
            final String streamlink = br.getRegex("file: \\'(http://[^<>\"\\']*?)\\'").getMatch(0);
            /* video/xxx.mp4 = conversion in progress, video/lock.mp4 = IP_blocked */
            if (streamlink == null || streamlink.contains("video/xxx.mp4") || streamlink.contains("video/lock.mp4")) {
                // if (br.containsHTML("/img/streaming\\.jpg")) {
                // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Converting video in progress ...");
                // }
                this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("/ajax.php?a=getDownloadForFree", "id=" + fid + "&_go=");
                if (br.containsHTML("\"message\":\"error\"")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                }
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setId("6LeVtQcTAAAAAMTtLxcMjlQZan7EXS69aPVS35YT");
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                br.postPage("/ajax.php?a=getDownloadLink", "captcha1=" + Encoding.urlEncode(rc.getChallenge()) + "&captcha2=" + Encoding.urlEncode(c) + "&id=" + fid + "&_go=");
                if (br.containsHTML("\"message\":\"error\"")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                dllink = getJson("location");
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                /* Prefer streams as we can avoid the captcha though the quality does not match the originally uploaded content. */
                dllink = streamlink;
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                if (isJDStable()) {
                    con = br2.openGetConnection(dllink);
                } else {
                    con = br2.openHeadConnection(dllink);
                }
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

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}