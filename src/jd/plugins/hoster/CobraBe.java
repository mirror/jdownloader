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

import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

/*
 * vrt.be network
 * old content handling --> var vars12345 = Array();
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cobra.be" }, urls = { "http://(www\\.)?cobra\\.be/(permalink/\\d\\.\\d+|cm/(vrtnieuws|cobra)([^/]+)?/(mediatheek|videozone).+)" }, flags = { 0 })
public class CobraBe extends PluginForHost {

    public CobraBe(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK  = null;
    private String JSARRAY = null;

    @Override
    public String getAGBLink() {
        return "http://www.vrt.be/privacy-beleid";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("/cm/vrtnieuws/mediatheek/[^/]+/[^/]+/[^/]+/([0-9\\.]+)(.+)?", "/permalink/$1"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("/cm/vrtnieuws([^/]+)?/mediatheek(\\w+)?/([0-9\\.]+)(.+)?", "/permalink/$3"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        // Link offline
        if (br.containsHTML("(>Pagina \\- niet gevonden<|>De pagina die u zoekt kan niet gevonden worden)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        JSARRAY = br.getRegex("(var vars\\d+.*?\\[\'w\'\\]\\s?=\\s?\'\\d+\';)").getMatch(0);
        if (JSARRAY == null) makeJsArray();

        if (JSARRAY == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        JSARRAY = JSARRAY.replaceAll("vars\\d+", "vars12345");

        DLLINK = getMediaUrl("src");
        final String filename = getMediaUrl("title");
        if (DLLINK == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) ext = ".flv";
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()).replaceAll("\"", "") + ext);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                DLLINK = getMediaUrl("rtmpServer");
                DLLINK = DLLINK != null && getMediaUrl("rtmpPath") != null ? DLLINK + "@" + getMediaUrl("rtmpPath") : null;
                if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    private void makeJsArray() {
        String sporza = br.getRegex("(data\\-video\\-id=.*?data\\-video\\-width\\s?=\\s?\"\\d+\")").getMatch(0);
        if (sporza != null) {
            sporza = sporza.replace("rtmp-server", "rtmpServer").replace("rtmp-path", "rtmpPath");
            sporza = sporza.replaceAll("data\\-video-([^=]+)\\s?=\\s?\"([^\"]+)?\"", "vars12345['$1'] = '$2';");
            if (sporza.contains("vars12345")) JSARRAY = "var vars12345 = Array();\n" + sporza;
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (DLLINK.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, DLLINK);
            setupRTMPConnection(dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, false, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private String getMediaUrl(String s) {
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        JSARRAY += "\nvar out = vars12345['" + s + "'];";
        try {
            engine.eval(JSARRAY);
            return String.valueOf(engine.get("out"));
        } catch (final Throwable e) {
        }
        return null;
    }

    private void setupRTMPConnection(final DownloadInterface dl) {
        final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPlayPath(DLLINK.split("@")[1]);
        rtmp.setUrl(DLLINK.split("@")[0]);
        rtmp.setSwfVfy("http://www.cobra.be/html/flash/common/player.5.10.swf");
        rtmp.setResume(true);
        rtmp.setRealTime();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}