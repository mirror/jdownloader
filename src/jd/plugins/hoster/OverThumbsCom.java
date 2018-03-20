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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "overthumbs.com" }, urls = { "http://(www\\.)?overthumbs\\.com/galleries/[a-z0-9\\-]+" })
public class OverThumbsCom extends PluginForHost {
    private String DLLINK = null;

    public OverThumbsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www.overthumbs.com" + new Regex(link.getDownloadURL(), "(/galleries/.+)").getMatch(0));
    }

    @Override
    public String getAGBLink() {
        return "http://overthumbs.com/notices/2257/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRequest().getHttpConnection().getResponseCode() == 404 || !br.getURL().contains("/galleries/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h2>([^<>\"]*?)<").getMatch(0);
        if (filename.isEmpty() || filename.equals(" ")) {
            filename = br.getRegex("<title>([^<>\"]*?)( On Overthumbs)?</title>").getMatch(0);
        }
        final String vid = br.getRegex("\"/[a-z]+player/playvideo\\.php\\?id=(\\d+)\"").getMatch(0);
        if (filename == null || vid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final boolean new_way = true;
        if (new_way) {
            /* New/Alternative way: jwplayer/playvideo.php?id= */
            br.getPage("http://overthumbs.com/jwplayer/playvideo.php?id=" + vid);
            // --> Unpack js and RegEx finallink
            String js = br.getRegex("eval\\s*\\((function\\(p,a,c,k,e,d\\).*?\\{\\}\\))\\)").getMatch(0);
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            String result = null;
            try {
                engine.eval("var res = " + js);
                result = (String) engine.get("res");
            } catch (final Exception e) {
                e.printStackTrace();
            }
            DLLINK = PluginJSonUtils.getJsonValue(result, "file");
        } else {
            /* Use old way - avoids packed JS althougs js is just packed, nothing else. */
            br.getPage("http://overthumbs.com/flvplayer/player/xml_connect.php?code=" + vid);
            DLLINK = br.getRegex("<urlMOV1>(http://.*?)</urlMOV1>").getMatch(0);
            if (DLLINK == null) {
                DLLINK = br.getRegex("\\'(http://cdn\\.empflix\\.com/empflv/.*?)\\'").getMatch(0);
            }
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Fix old links. */
            final Regex vinfos = new Regex(DLLINK, "http://vdelivery.overthumbs.com/flv/(\\d+)/([^<>]*?)\\.(?:flv|mp4)");
            final String server = vinfos.getMatch(0);
            final String evelse = vinfos.getMatch(1);
            if (server != null && evelse != null) {
                DLLINK = "http://" + server + ".staticvalley.com/mp4/" + server + "/" + evelse + ".mp4";
            }
        }
        if (DLLINK.contains("|")) {
            /* 2 urls given - get highest quality! */
            final String[] dllinks = DLLINK.split("\\|");
            DLLINK = dllinks[dllinks.length - 1];
        }
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ".mp4");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}