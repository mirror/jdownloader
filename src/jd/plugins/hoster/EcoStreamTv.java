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
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ecostream.tv" }, urls = { "http://(www\\.)?ecostream\\.tv/(stream|embed)/[a-z0-9]{32}\\.html" }, flags = { 0 })
public class EcoStreamTv extends PluginForHost {

    private final static AtomicBoolean use_js = new AtomicBoolean(true);

    public EcoStreamTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/embed/", "/stream/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.ecostream.tv/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String NOCHUNKS = "NOCHUNKS";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">File not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setFinalFileName(getfid(downloadLink) + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String tmp = br.getRegex("var [A-Za-z0-9]+=\\'([^<>\"]*?)\\';([\t\n\r ]+)?</script>[\t\n\r ]+<script src=\"/js/spin\\.js\"").getMatch(0);
        String noSenseForThat = br.getRegex("var superslots=\\'([^<>\"]*?)\\';").getMatch(0);
        if (tmp == null || noSenseForThat == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String postpage = null;
        Browser br2 = br.cloneBrowser();
        try {
            br2.getHeaders().put("Accept", "*/*");
            br2.getPage("/js/ecoss.js");
            postpage = br2.getRegex("\\$\\.post\\('(/xhr/videos/\\w+)',").getMatch(0);
        } catch (final Throwable e) {
        }

        if (postpage == null || !use_js.get()) {
            postpage = "/xhr/videos/w0Iri0";
        }
        br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");

        this.sleep(2 * 1000l, downloadLink);

        br2.postPage(postpage, "id=" + getfid(downloadLink) + "&tpm=" + tmp + noSenseForThat);
        String finallink = br2.getRegex("\"url\":\"(/[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            if (!use_js.get() && br2.getHttpConnection().getResponseCode() == 404) {
                use_js.set(false);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Blocked JD", 15 * 60 * 1000l);
            }
        }
        int maxChunks = 0;
        if (downloadLink.getBooleanProperty(EcoStreamTv.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(EcoStreamTv.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(EcoStreamTv.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(EcoStreamTv.NOCHUNKS, false) == false) {
                downloadLink.setProperty(EcoStreamTv.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }

            throw e;
        }
    }

    private String getfid(final DownloadLink downloadLink) {
        return new Regex(downloadLink.getDownloadURL(), "ecostream\\.tv/stream/([a-z0-9]{32})\\.html").getMatch(0);
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