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
import javax.script.ScriptException;

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

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cliphunter.com" }, urls = { "http://(www\\.)?cliphunter\\.com/w/\\d+/\\w+" }, flags = { 0 })
public class ClipHunterCom extends PluginForHost {

    private String DLLINK = null;

    public ClipHunterCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public String decryptUrl(final String fun, final String value) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(fun);
            result = inv.invokeFunction("decrypt", value);
        } catch (final ScriptException e) {
            e.printStackTrace();
        }
        return (String) result;
    }

    @Override
    public String getAGBLink() {
        return "http://www.cliphunter.com/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, false, 0);
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
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("error/missing")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        String filename = br.getRegex("<title>(.*?) -.*?</title>").getMatch(0);
        final String jsUrl = br.getRegex("<script.*src=\"(http://s\\.gexo.*?player\\.js)\"").getMatch(0);
        final String encryptedUrl = br.getRegex("var pl_fiji_p = '(.*?)'").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1 style=\"font-size: 2em;\">(.*?) </h1>").getMatch(0);
        }
        if (filename == null || jsUrl == null || encryptedUrl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        // parse decryptalgo
        final Browser br2 = br.cloneBrowser();
        br2.getPage(jsUrl);
        String decryptAlgo = new Regex(br2, "decrypt\\:function(.*?)\\}\\;\\$\\(document").getMatch(0);
        if (decryptAlgo == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        decryptAlgo = "function decrypt" + decryptAlgo + ";";

        DLLINK = decryptUrl(decryptAlgo, encryptedUrl);
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
