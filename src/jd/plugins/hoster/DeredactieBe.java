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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deredactie.be" }, urls = { "http://(www\\.)?deredactie\\.be/(permalink/\\d\\.\\d+|cm/vrtnieuws/mediatheek/((programmas|redactietips|nieuws)/)?[\\w\\%]+/\\d\\.\\d+(/\\d\\.\\d+)?(/\\d\\.\\d+)?)" }, flags = { 0 })
public class DeredactieBe extends PluginForHost {

    public DeredactieBe(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK  = null;
    private String JSARRAY = null;

    @Override
    public String getAGBLink() {
        return "http://deredactie.be/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>Pagina \\- niet gevonden<|>De pagina die u zoekt kan niet gevonden worden)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        JSARRAY = br.getRegex("(var vars\\d+.*?\\[\'w\'\\]\\s?=\\s?\'\\d+\';)").getMatch(0);
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
                DLLINK = getMediaUrl("rtmpServer") + "@" + getMediaUrl("rtmpPath");
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

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (DLLINK.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, DLLINK);
            setupRTMPConnection(dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
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
        rtmp.setSwfVfy("http://www.deredactie.be/html/flash/common/streamsense_jwp-v1.swf");
        rtmp.setResume(true);
        rtmp.setTimeOut(-20);
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