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

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "speedshare.org" }, urls = { "http://[\\w\\.]*?speedshare\\.org/download\\.php\\?id=[\\w]+" }, flags = { 0 })
public class SpeedShareOrg extends PluginForHost {

    private String url;

    public SpeedShareOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.speedshare.org/rules.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex(Pattern.compile("<title>SpeedShare - Download (.*)</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        String filesize = br.getRegex(Pattern.compile("\\((.*?)\\) angefordert\\.</b></div></td></td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        url = downloadLink.getDownloadURL();
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);

        /* Downloadlimit erreicht */
        if (br.containsHTML("<span>Entschuldigung")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l); }

        /* DownloadLink holen, thx @dwd */
        String all = br.getRegex("eval\\(unescape\\(.*?\"\\)\\)\\);").getMatch(-1);
        String dec = br.getRegex("loadfilelink\\.decode\\(\".*?\"\\);").getMatch(-1);
        Context cx = ContextFactory.getGlobal().enter();
        Scriptable scope = cx.initStandardObjects();
        String fun = "function f(){ " + all + "\nreturn " + dec + "} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        url = Context.toString(result);
        Context.exit();

        /* 15 seks warten */
        sleep(15000l, downloadLink);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = jd.plugins.BrowserAdapter.openDownload(br,downloadLink, url);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
