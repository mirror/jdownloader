//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import javax.script.Invocable;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cliphunter.com" }, urls = { "http://(www\\.)?cliphunter\\.com/w/\\d+/\\w+" }, flags = { 0 })
public class ClipHunterCom extends PluginForHost {

    private String DLLINK = null;

    public ClipHunterCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public String decryptUrl(final String fun, final String value) {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(fun);
            result = inv.invokeFunction("decrypt", value);
        } catch (final Throwable e) {
            return null;
        }
        return result != null ? result.toString() : null;
    }

    @Override
    public String getAGBLink() {
        return "http://www.cliphunter.com/terms/";
    }

    private String getHighestQuality(final String[] enc, final String dec) {
        String tmpSr, tmpUrl, out = null;
        int sr = -1;
        for (final String s : enc) {
            tmpUrl = decryptUrl(dec, s);
            tmpSr = new Regex(tmpUrl, "sr=(\\d+)").getMatch(0);
            if (tmpSr == null) {
                continue;
            }
            if (sr > Integer.parseInt(tmpSr)) {
                continue;
            }
            sr = Integer.parseInt(tmpSr);
            out = tmpUrl;
        }
        return out;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("cliphunter.com", "qchange", "h");
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("error/missing") || br.containsHTML("(>Ooops, This Video is not available|>This video was removed and is no longer available at our site|<title></title>)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<title>(.*?) -.*?</title>").getMatch(0);
        final String jsUrl = br.getRegex("<script.*src=\"(http://s\\.gexo.*?player\\.js)\"").getMatch(0);
        final String[] encryptedUrls = br.getRegex("var pl_fiji(_p|_i)? = '(.*?)'").getColumn(1);
        if (filename == null) {
            filename = br.getRegex("<h1 style=\"font-size: 2em;\">(.*?) </h1>").getMatch(0);
        }
        if (filename == null || jsUrl == null || encryptedUrls == null || encryptedUrls.length == 0) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        // parse decryptalgo
        final Browser br2 = br.cloneBrowser();
        br2.getPage(jsUrl);
        String decryptAlgo = new Regex(br2, "decrypt\\:\\s?function(.*?\\})(,|;)").getMatch(0);
        if (decryptAlgo == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        decryptAlgo = "function decrypt" + decryptAlgo + ";";
        DLLINK = getHighestQuality(encryptedUrls, decryptAlgo);
        if (DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
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
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
