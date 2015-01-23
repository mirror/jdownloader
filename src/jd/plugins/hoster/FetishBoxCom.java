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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fetishbox.com", "bondagebox.com" }, urls = { "http://(www\\.)?fetishbox\\.com/videos/[a-z0-9\\-]+\\.html", "http://(www\\.)?bondagebox\\.com/videos/[a-z0-9\\-]+\\.html" }, flags = { 0, 0 })
public class FetishBoxCom extends PluginForHost {

    public FetishBoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fetishbox.com/2257.php";
    }

    /* Using playerConfig script */
    /* Tags: playerConfig.php */

    private static final String defaultExtension = ".mp4";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<div class=\"header\">([^<>]*?)</div>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>]*?) at FetishBox</title>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (br.containsHTML("\\.flv\"")) {
            filename += ".flv";
        } else {
            filename += defaultExtension;
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String playpath = checkDirectLink(downloadLink, "directlink");
        String swfvfy = null;
        String rtmpurl = null;
        if (playpath == null) {
            swfvfy = br.getRegex("new SWFObject\\(\"(http[^<>\"]*?\\.swf)\"").getMatch(0);
            final String playercfgurl = br.getRegex("(http://(www\\.)?[a-z0-9\\.\\-]+/playerConfig\\.php\\?[^<>\"/]+\\.(?:mp4|flv))").getMatch(0);
            if (playercfgurl == null || swfvfy == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(playercfgurl);
            playpath = br.getRegex("defaultVideo:([^<>\"]*?\\.(?:mp4|flv));").getMatch(0);
            rtmpurl = br.getRegex("conn:(rtmp://[^<>\"]*?);").getMatch(0);
            if (playpath == null || rtmpurl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /* Some links are HTTP only, some are RTMP only... */
        if (playpath.startsWith("http")) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, playpath, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setProperty("directlink", playpath);
            dl.startDownload();
        } else {
            final String app = new Regex(rtmpurl, "([a-z0-9]+/)$").getMatch(0);
            if (app == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (downloadLink.getFinalFileName().endsWith(".mp4")) {
                playpath = "mp4:" + playpath;
            } else {
                playpath = "flv:" + playpath;
            }
            try {
                dl = new RTMPDownload(this, downloadLink, rtmpurl);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setUrl(rtmpurl);
            rtmp.setPlayPath(playpath);
            rtmp.setApp(app);
            rtmp.setFlashVer("WIN 16,0,0,287");
            rtmp.setSwfVfy(swfvfy);
            rtmp.setResume(false);
            ((RTMPDownload) dl).startDownload();
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            /* Maybe we have rtmp urls --> Do NOT reuse them! */
            if (!dllink.startsWith("http")) {
                return null;
            }
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

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
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