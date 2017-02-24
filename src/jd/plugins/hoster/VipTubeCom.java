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
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "viptube.com" }, urls = { "http://(www\\.)?viptube\\.com/(video|embed)/\\d+" })
public class VipTubeCom extends PluginForHost {

    public VipTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String            dllink    = null;
    private static AtomicLong lastCheck = new AtomicLong(System.currentTimeMillis());

    @Override
    public String getAGBLink() {
        return "http://www.viptube.com/static/terms";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("http://www.viptube.com/video/" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
    }

    /* Similar sites: drtuber.com, proporn.com, viptube.com, tubeon.com, winporn.com, nuvid.com */
    /*
     * IMPORTANT: If the crypto stuff fails, use the mobile version of the sites to get uncrypted finallinks! Also, registered users can see
     * uncrypted normal streamlinks!
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        /*
         * download and linkcheck can effectively request at the same time!! If you hit multiple links in short succession it will report
         * some as offline!
         */
        synchronized (lastCheck) {
            try {
                long sum;
                // small wait to prevent issue. -raztoki20160112
                while ((sum = System.currentTimeMillis() - lastCheck.get()) < 5000) {
                    // can't use sleep(long, downloadlink) because NPE will occur due to DownloadLink.getDownloadLinkController() line: 511
                    Thread.sleep(5000l + new Random().nextInt(2000));
                }
                dllink = null;
                br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
                final String url_filename = new Regex(downloadLink.getDownloadURL(), "viptube\\.com/(.+)").getMatch(0).replace("/", "_");
                this.setBrowserExclusive();
                br.setFollowRedirects(true);
                br.getPage(downloadLink.getDownloadURL());
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                String filename = br.getRegex("\"title\":\"([^<>\"]*?)\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("class=\"watch\"><div class=\"base\"><div class=\"headline\"><h2>(.*?)</h2").getMatch(0);
                    if (filename == null) {
                        filename = url_filename;
                    }
                }
                getDllink();
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = Encoding.htmlDecode(dllink);
                filename = filename.trim();
                downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + getFileNameExtensionFromString(dllink, ".mp4"));
                br.getHeaders().put("Accept", "*/*"); // In case the link redirects to the finallink
                URLConnectionAdapter con = null;
                try {
                    try {
                        con = br.openGetConnection(dllink);
                        if (con.getResponseCode() == 404) {
                            /*
                             * Small workaround for buggy servers that redirect and fail if the Referer is wrong then. Examples: hdzog.com
                             */
                            final String redirect_url = con.getRequest().getUrl();
                            con = br.openGetConnection(redirect_url);
                        }
                    } catch (final BrowserException e) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
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
            } finally {
                lastCheck.set(System.currentTimeMillis());
            }
        }
    }

    private void getDllink() throws IOException {
        dllink = this.br.getRegex("src=\"(http://[^/]+/mp4/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            final boolean preferMobile = false;
            if (preferMobile) {
                /* Usually we'll get a .mp4 here, quality is lower than via website. */
                final String videoid = new Regex(this.br.getURL(), "/video/(\\d+)").getMatch(0);
                if (videoid != null) {
                    this.br.getPage("http://m." + this.getHost() + "/video/" + videoid);
                    dllink = this.br.getRegex("<source src=\"(http[^<>\"]+\\.(?:mp4|flv)[^<>\"]+)").getMatch(0);
                }
            } else {
                /* 2017-01-25: pkey generation is wrong - fallback to mobile version of the website! */
                final String h = this.br.getRegex("h=([a-f0-9]{32})\\'").getMatch(0);
                final String t = this.br.getRegex("params\\s*?\\+=\\s*?\\'%26t=(\\d+)\\';").getMatch(0);
                String vkey = this.br.getRegex("vkey=([a-z0-9]+)").getMatch(0);
                if (vkey == null) {
                    /* 2017-01-23 */
                    vkey = this.br.getRegex("vkey=\\'\\s*?\\+\\s*?\\'([a-z0-9]+)\\'").getMatch(0);
                }
                if (h != null && t != null && vkey != null) {
                    br.getPage("http://www." + this.getHost() + "/player_config/?h=" + h + "&check_speed=1&t=" + t + "&vkey=" + vkey + "&pkey=" + JDHash.getMD5(vkey + Encoding.Base64Decode("RXdxT0JRbUpETUpScmdYWg==")) + "&aid=&domain_id=");
                    dllink = getXmlDllink(this.br);
                }
            }
        }
    }

    public static String getXmlDllink(final Browser br) {
        final String[] possibleQualities = { "hq_video_file", "video_file" };
        String dllink = null;
        for (final String possibleQuality : possibleQualities) {
            dllink = br.getRegex("<" + possibleQuality + ">(http://.*?)</" + possibleQuality + ">").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<" + possibleQuality + "><\\!\\[CDATA\\[(http://.*?)\\]\\]></" + possibleQuality + ">").getMatch(0);
            }
            if (dllink != null) {
                break;
            }
        }
        return dllink;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript9;
    }
}
