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
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.StringUtils;

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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "romhustler.org" }, urls = { "https?://(?:www\\.)?romhustler\\.(?:net|org)/(?:file|download)/\\d+/[A-Za-z0-9/\\+=%]+" })
public class RomHustler extends PluginForHost {
    public RomHustler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://romhustler.net/disclaimer.php";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private static AtomicReference<String> agent = new AtomicReference<String>();

    public Browser prepBrowser(Browser prepBr) {
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", agent.get());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        prepBr.setCustomCharset("utf-8");
        prepBr.setFollowRedirects(true);
        return prepBr;
    }

    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser(br);
        final String decrypterLink = downloadLink.getStringProperty("decrypterLink", null);
        if (decrypterLink == null) {
            /* This should never happen. */
            // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.getPage(downloadLink.getDownloadURL());
            String filename = br.getRegex("itemprop=\"name\">([^<>]+)</h1>").getMatch(0);
            if (filename != null) {
                downloadLink.setName(filename + " " + System.currentTimeMillis());
            }
        } else {
            br.getPage(decrypterLink);
        }
        String jslink = br.getRegex("\"(/js/cache[a-z0-9\\-]+\\.js)\"").getMatch(0);
        if (jslink != null) {
            try {
                br.cloneBrowser().getPage("https://" + this.getHost() + jslink);
            } catch (final Exception e) {
            }
        }
        // don't worry about filename... set within decrypter should be good until download starts.
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        boolean skipWaittime = true;
        if (!skipWaittime) {
            int wait = 8;
            final String waittime = br.getRegex("start=\"(\\d+)\"></span>").getMatch(0);
            if (waittime != null) {
                wait = Integer.parseInt(waittime);
            }
            sleep(wait * 1001l, link);
        }
        final String fuid = new Regex(link.getDownloadURL(), "/(\\d+)/").getMatch(0);
        if (fuid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String ddlink = null;
        if (true) {
            Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
            br2.getPage("/link/" + fuid + "?_=" + System.currentTimeMillis());
            ddlink = PluginJSonUtils.getJson(br2, "hashed");
        }
        if (StringUtils.isEmpty(ddlink) || !ddlink.startsWith("http") || ddlink.length() > 500) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        ddlink = ddlink.replace("\\", "");
        if (link.getBooleanProperty("splitlink", false)) {
            ddlink += "/1";
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, ddlink, true, -4);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = getFileNameFromHeader(dl.getConnection());
        filename = Encoding.htmlDecode(filename);
        link.setFinalFileName(filename);
        dl.startDownload();
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {
    }
}