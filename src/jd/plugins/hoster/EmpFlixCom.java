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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "empflix.com" }, urls = { "https?://(?:www\\.)?empflix\\.com/(?:view\\.php\\?id=\\d+|videos/.*?\\-\\d+\\.html|.+/video\\d+)|https?://(?:www\\.)?empflix\\.com/embedding_player/embedding_feed\\.php\\?viewkey=[a-z0-9]+|https?://player\\.empflix\\.com/video/\\d+" })
public class EmpFlixCom extends PluginForHost {

    /* DEV NOTES */
    /* Porn_plugin */

    private String              dllink                = null;

    private static final String TYPE_NORMAL           = "https?://(?:www\\.)?empflix\\.com/(view_video\\.php\\?viewkey=[a-z0-9]+|.*?video\\d+)";
    private static final String TYPE_embed            = "https?://player\\.empflix\\.com/video/\\d+";
    private static final String TYPE_embedding_player = "https?://(?:www\\.)?empflix\\.com/embedding_player/embedding_feed\\.php\\?viewkey=[a-z0-9]+";

    public EmpFlixCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.empflix.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        final String addedlink = link.getDownloadURL();
        if (addedlink.matches(TYPE_embed)) {
            final String fid = new Regex(addedlink, "(\\d+)$").getMatch(0);
            link.setUrlDownload("http://www.empflix.com/videos/xyz-" + fid + ".html");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        if (!downloadLink.isNameSet()) {
            /* Offline urls should have ok-filenames too. */
            downloadLink.setName(getFid(downloadLink));
        }
        br.setFollowRedirects(true);
        if (downloadLink.getDownloadURL().matches(TYPE_embedding_player)) {
            /* Convert embed urls --> Original urls */
            br.getPage(downloadLink.getDownloadURL().replace("http://", "https://"));
            String videoID = br.getRegex("start_thumb>https?://static\\.empflix\\.com/thumbs/[a-z0-9\\-_]+/[a-z0-9]+_(\\d+)l\\.jpg<").getMatch(0);
            if (videoID == null) {
                videoID = br.getRegex("<start_thumb><\\!\\[CDATA\\[https?://static\\.empflix\\.com/thumbs/[a-z0-9\\-_]+/[a-z0-9]+_(\\d+)l\\.jpg\\]\\]></start_thumb>").getMatch(0);
            }
            if (videoID == null) {
                /* Either plugin broken or link offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String newlink = "http://www.empflix.com/cum-videos/" + System.currentTimeMillis() + "/video" + videoID;
            downloadLink.setUrlDownload(newlink);
        }
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(Error: Sorry, the movie you requested was not found|Check this hot video instead:</div>)") || this.br.getURL().matches(".+\\.com/?$")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        boolean directURL = false;
        String filename = br.getRegex("<title>(.*?), Free Streaming Porn</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"title\" name=\"title\" value=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null) {
            /* 2016-11-03 */
            filename = br.getRegex("property=\"og:title\" content=\"([^<>]+)\"").getMatch(0);
        }
        dllink = br.getRegex("addVariable\\(\\'config\\', \\'(http://.*?)\\'\\)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("(\\'|\")(http://cdn\\.empflix\\.com/empflv(\\d+)?/.*?)(\\'|\")").getMatch(1);
        }
        if (dllink == null) {
            dllink = br.getRegex("id=\"config\" name=\"config\" value=\"(http://.*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("flashvars\\.config = escape\\(\"(.*?)\"\\)").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("config\" value=\"(.*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("config\\s*=\\s*('|\")(.*?)\1").getMatch(1);
        }
        if (dllink == null) {
            /* 2016-11-03 */
            dllink = br.getRegex("itemprop=\"contentUrl\" content=\"(https?:[^<>\"]+)\"").getMatch(0);
            if (dllink != null) {
                directURL = true;
            }
        }
        if (dllink == null) {
            /* raztoki20161124 */
            final String[] lis = br.getRegex("<\\s*li\\s+[^>]+").getColumn(-1);
            if (lis != null) {
                String vid = null, nk = null, vk = null, th = null;
                for (final String li : lis) {
                    if (li.contains("data-vid") && li.contains("data-nk") && li.contains("data-vk") && li.contains("data-th") && li.contains("data-name")) {
                        vid = new Regex(li, "data-vid\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
                        nk = new Regex(li, "data-nk\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
                        vk = new Regex(li, "data-vk\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
                        th = new Regex(li, "data-th\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
                        filename = new Regex(li, "data-name\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
                        break;
                    }
                }
                if (vid == null || nk == null || vk == null || th == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String url = "https://cdn-fck.empflix.com/empflix/" + vk + "-1.fid?key=" + nk + "&VID=" + vid + "&nomp4=1&catID=0&rollover=1&startThumb=" + th + "&embed=0&utm_source=0&multiview=0&premium=1&country=0user=0&vip=1&cd=0&ref=0&alpha";
                final Browser br = this.br.cloneBrowser();
                br.getHeaders().put("Accept", "*/*");
                br.getPage(url);
                dllink = br.getRegex("\\[CDATA\\[(.*?)\\]{2}").getMatch(0);
                if (dllink != null) {
                    directURL = true;
                }
            }
        }
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!directURL) {
            br.getPage(Encoding.htmlDecode(dllink));
            dllink = br.getRegex("<res>480p</res>\\s+<videoLink><\\!\\[CDATA\\[(.*?)\\]\\]></videoLink>").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<(file|videoLink)><?(\\!\\[CDATA\\[)?(.*?)(\\]\\])?>?</(file|videoLink)>").getMatch(2);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.htmlDecode(dllink);
            directURL = true;
        }
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ".mp4");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFid(final DownloadLink dl) {
        final String fid;
        if (dl.getDownloadURL().endsWith(".html")) {
            fid = new Regex(dl.getDownloadURL(), "([a-z0-9]+)\\.html$").getMatch(0);
        } else {
            fid = new Regex(dl.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        }
        return fid;
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