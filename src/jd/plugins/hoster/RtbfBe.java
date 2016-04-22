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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rtbf.be" }, urls = { "http://(www\\.)?rtbf\\.be/(?:video|auvio)/detail_[a-z0-9}\\-_]+\\?id=\\d+" }, flags = { 0 })
public class RtbfBe extends PluginForHost {

    public RtbfBe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.rtbf.be/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String dllink = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        // The regex only takes the short urls but these ones redirect to the real ones to if follow redirects is false the plugin doesn't
        // work at all!
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\\s+(?::|-)\\s+RTBF\\s+(?:Vidéo|Auvio)\"").getMatch(0);
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
        }
        final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        br.getPage("embed/media?id=" + fid + "&autoplay=1");
        String vid_text = br.getRegex("<div class=\"js\\-player\\-embed.*?\" data\\-video=\"(.*?)\">").getMatch(0);
        if (vid_text == null) {
            vid_text = this.br.getRegex("data\\-video=\"(.*?)\"").getMatch(0);
        }
        if (vid_text == null) {
            vid_text = this.br.getRegex("data\\-media=\"(.*?)\"></div>").getMatch(0);
        }
        if (vid_text == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // this is json encoded with htmlentities.
        vid_text = HTMLEntities.unhtmlentities(vid_text);
        // we can get filename here also.
        if (filename == null) {
            filename = PluginJSonUtils.getJson(vid_text, "title");
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dllink = PluginJSonUtils.getJson(vid_text, "downloadUrl");
        if (dllink == null) {
            dllink = PluginJSonUtils.getJson(vid_text, "high");
        }
        if (dllink == null) {
            // audio
            dllink = PluginJSonUtils.getJson(vid_text, "url");
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        String ext = getFileNameExtensionFromString(dllink, ".mp4");
        downloadLink.setFinalFileName(filename + ext);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
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
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
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