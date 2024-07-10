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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "video.haberturk.com" }, urls = { "https?://video\\.haberturk\\.com/haber/video/[a-z0-9\\-_]+/\\d+|https?://(?:www\\.)?haberturk\\.com/video/haber/izle/[a-z0-9\\-_]+/\\d+" })
public class VideoHaberturkCom extends PluginForHost {
    public VideoHaberturkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "https://video.haberturk.com/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.dllink = null;
        final String extDefault = ".mp4";
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            /* Set fallback filename */
            link.setName(fid + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<h2 class=\"baslik\">([^<>\"]*?)</h2>").getMatch(0);
        if (title == null) {
            title = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (title == null) {
                title = br.getRegex("<title>([^<>\"]*?) \\- Haber Videoları \\- Habertürk Video</title>").getMatch(0);
            }
        }
        dllink = br.getRegex("og:video:url\" content=\"(https?://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("file:\\'(http://[^<>\"]*?)\\'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\\&path=(http://[^<>\"]*?)\\&").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("url: \"(http:[^<>\"]*?)\"").getMatch(0);
                    if (dllink != null) {
                        dllink = dllink.replaceAll("\\\\/", "/");
                    }
                }
            }
        }
        if (dllink == null) {
            final String[] mp4s = br.getRegex("\"(https?:[^\"]+\\.mp4)").getColumn(0);
            if (mp4s != null && mp4s.length > 0) {
                this.dllink = PluginJSonUtils.unescape(mp4s[0]);
            }
        }
        // hls
        if (dllink == null) {
            final String json = br.getRegex("<div class='htplay_video' data-ht='(\\{.*?\\})' style=").getMatch(0);
            if (json != null) {
                processJavascript(json);
            }
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            link.setFinalFileName(this.applyFilenameExtension(title, extDefault));
        }
        return AvailableStatus.TRUE;
    }

    private void processJavascript(String json) {
        try {
            final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            final List<Object> test = (List<Object>) entries.get("ht_files");
            // we are still in the order from the website, lets shuffle
            Collections.shuffle(test);
            // ok there are hls and web links, lets prefer the web links!
            // there are also multiple servers. we just want one entry.
            final String[] keys = new String[] { "mp4", "m3u8" };
            for (final String key : keys) {
                for (final Object a : test) {
                    final Map<String, Object> yay1 = (Map<String, Object>) a;
                    if (!yay1.containsKey(key)) {
                        continue;
                    }
                    // another array
                    final List<Object> yay2 = (List<Object>) yay1.get(key);
                    int p = 0;
                    String file = null;
                    for (final Object b : yay2) {
                        final Map<String, Object> yay3 = (Map<String, Object>) b;
                        // multiple qualities.
                        final String tmpfile = (String) yay3.get("file");
                        final String name = (String) yay3.get("name");
                        if (keys[0].equals(key) && name != null && name.matches("\\d+") && file != null) {
                            final Integer tmpP = Integer.parseInt(name);
                            // best handling
                            if (tmpP > p) {
                                file = tmpfile;
                                p = tmpP;
                            }
                        } else if ("m3u8".equals(key) && file == null) {
                            // hls just has master file?
                            file = tmpfile;
                            break;
                        }
                    }
                    if (file != null) {
                        dllink = file;
                        return;
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlOnlyDecode(dllink);
        if (dllink.contains(".m3u8")) {
            // hls has multiple qualities....
            final Browser br2 = br.cloneBrowser();
            br2.getPage(dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(br2));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = hlsbest.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
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
}
