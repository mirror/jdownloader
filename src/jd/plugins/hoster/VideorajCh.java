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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videoraj.to" }, urls = { "http://(www\\.)?videoraj\\.(?:ch|to)/(v/|embed\\.php\\?id=)[a-z0-9]+" })
public class VideorajCh extends PluginForHost {

    public VideorajCh(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.videoraj.to/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("http://www.videoraj.to/v/" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
    }

    /* API available but not enough for us: http://www.videoraj.ch/api-doc.php */
    /* Similar plugins: NovaUpMovcom, VideoWeedCom, NowVideoEu, MovShareNet, VidGg */
    @SuppressWarnings("deprecation")
    // This plugin is 99,99% copy the same as the DivxStageNet plugin, if this
    // gets broken please also check the other one!
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        setBrowserExclusive();
        br.getHeaders().put("Accept-Encoding", "identity");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("The Video no longer exists")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"title\">([^<>\"]*?)<").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = filename.trim();
        if (filename.equals("Untitled") || filename.equals("Title")) {
            downloadLink.setFinalFileName("Video " + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + ".mp4");
        } else {
            downloadLink.setFinalFileName(filename + (!filename.endsWith(".mp4") ? ".mp4" : ""));
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String fid = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().lastIndexOf("/") + 1);
        br.getPage("/embed.php?id=" + fid + "&autoplay=1");
        String cid2 = br.getRegex("flashvars\\.cid2=\"(\\d+)\";").getMatch(0);
        String key = br.getRegex("flashvars\\.filekey=\"(.*?)\"").getMatch(0);
        if (key == null && br.containsHTML("w,i,s,e")) {
            String result = unWise();
            key = new Regex(result, "(\"\\d+{1,3}\\.\\d+{1,3}\\.\\d+{1,3}\\.\\d+{1,3}-[a-f0-9]{32})\"").getMatch(0);
        }
        if (key == null) {
            key = br.getRegex("key:[\t\n\r ]+\"([^<>\"]*?)\"").getMatch(0);
        }
        if (key == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (cid2 == null) {
            cid2 = "undefined";
        }
        String lastdllink = null;
        boolean success = false;
        for (int i = 0; i <= 3; i++) {
            if (i > 0) {
                br.getPage("/api/player.api.php?user=undefined&errorUrl=" + Encoding.urlEncode(lastdllink) + "&pass=undefined&cid3=undefined&errorCode=404&cid=1&cid2=" + cid2 + "&key=" + key + "&file=" + fid + "&numOfErrors=" + i);
            } else {
                br.getPage("/api/player.api.php?cid2=" + cid2 + "&numOfErrors=0&user=undefined&cid=1&pass=undefined&key=" + key + "&file=" + fid + "&cid3=undefined");
            }
            String dllink = br.getRegex("url=(http.*?)\\&title").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.htmlDecode(dllink);
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
                if (!dl.getConnection().getContentType().contains("html")) {
                    success = true;
                    break;
                } else {
                    lastdllink = dllink;
                    continue;
                }
            } finally {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        if (!success) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 410) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private String unWise() {
        String result = null;
        String fn = br.getRegex("eval\\((function\\(.*?\'\\))\\);").getMatch(0);
        if (fn == null) {
            return null;
        }
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval("var res = " + fn);
            result = (String) engine.get("res");
            result = new Regex(result, "eval\\((.*?)\\);$").getMatch(0);
            engine.eval("res = " + result);
            result = (String) engine.get("res");
            String res[] = result.split(";\\s;");
            engine.eval("res = " + new Regex(res[res.length - 1], "eval\\((.*?)\\);$").getMatch(0));
            result = (String) engine.get("res");
        } catch (final Exception e) {
            logger.log(e);
            return null;
        }
        return result;
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

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_VideoHosting;
    }

}