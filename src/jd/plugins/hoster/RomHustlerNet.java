//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "romhustler.net" }, urls = { "http://(www\\.)?romhustler\\.net/download/\\d+/[A-Za-z0-9/\\+=%]+" }, flags = { 0 })
public class RomHustlerNet extends PluginForHost {

    public RomHustlerNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://romhustler.net/disclaimer.php";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private static StringContainer agent = new StringContainer();

    public static class StringContainer {
        public String string = null;
    }

    public Browser prepBrowser(Browser prepBr) {
        if (agent.string == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.string = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent.string);
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        prepBr.setCustomCharset("utf-8");
        prepBr.setFollowRedirects(true);
        return prepBr;
    }

    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        prepBrowser(br);
        if (downloadLink.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String decrypterLink = downloadLink.getStringProperty("decrypterLink", null);
        br.getPage(decrypterLink);
        String jslink = br.getRegex("\"(/js/cache[a-z0-9\\-]+\\.js)\"").getMatch(0);
        if (jslink != null) br.cloneBrowser().getPage("http://romhustler.net" + jslink);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">404 \\- Page got lost")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // don't worry about filename... set within decrypter should be good until download starts.
        return AvailableStatus.TRUE;
    }

    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        boolean skipWaittime = true;
        if (!skipWaittime) {
            int wait = 8;
            final String waittime = br.getRegex("start=\"(\\d+)\"></span>").getMatch(0);
            if (waittime != null) wait = Integer.parseInt(waittime);
            sleep(wait * 1001l, downloadLink);
        }

        String fuid = new Regex(downloadLink.getDownloadURL(), "/(\\d+)/").getMatch(0);
        if (fuid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String ddlink = null;
        if (true) {
            Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
            br2.getPage("/link/" + fuid + "?_=" + System.currentTimeMillis());
            ddlink = br2.getRegex("(https?://romhustler\\.net/file/" + fuid + "/[A-Za-z0-9/\\+=%]+)").getMatch(0);
        }

        if (ddlink == null || !ddlink.startsWith("http") || ddlink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, ddlink, true, -4);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        String filename = dl.getConnection().getURL().toString();
        filename = Encoding.htmlDecode(filename.substring(filename.lastIndexOf("/") + 1));
        if (filename != null && filename.contains(downloadLink.getName())) {
            downloadLink.setFinalFileName(filename);
        }
        dl.startDownload();
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {
    }

}