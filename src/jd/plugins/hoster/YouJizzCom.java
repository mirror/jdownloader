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
import java.util.ArrayList;
import java.util.LinkedHashMap;

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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.YouJizzComConfig;
import org.jdownloader.plugins.components.config.YouJizzComConfig.PreferredStreamQuality;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youjizz.com" }, urls = { "https?://(?:www\\.)?youjizz\\.com/videos/(embed/\\d+|.*?\\-\\d+\\.html)" })
public class YouJizzCom extends PluginForHost {
    /* DEV NOTES */
    /* Porn_plugin */
    private String dllink = null;

    public YouJizzCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.youjizz.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().contains("/embed/")) {
            link.setUrlDownload("https://www.youjizz.com/videos/x-" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) + ".html");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // if (!br.containsHTML("flvPlayer\\.swf")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        if (filename == null || filename.trim().length() == 0) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        if (filename == null || filename.trim().length() == 0) {
            filename = br.getRegex("title1\">(.*?)</").getMatch(0);
        }
        if (filename == null) {
            /* Fallback */
            filename = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/(.+)").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = filename.trim();
        final String embed = br.getRegex("src=('|\"|&#x22;)(https?://(?:www\\.)?youjizz\\.com/videos/embed/[0-9]+)\\1").getMatch(1);
        if (embed == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(Encoding.htmlOnlyDecode(embed));
        // 20170717
        final String filter = br.getRegex("dataEncodings\\s*=\\s*(\\[.*?\\]);").getMatch(0);
        if (filter != null) {
            final ArrayList<Object> results = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(filter);
            // mobile has mp4 and non mobile is hls
            int qualityMax = 0;
            final String preferredQuality = getPreferredStreamQuality();
            for (final Object resultz : results) {
                final LinkedHashMap<String, Object> result = (LinkedHashMap<String, Object>) resultz;
                final Object q = result.get("quality");
                final String d = (String) result.get("filename");
                if (q == null || d == null) {
                    continue;
                } else if (q instanceof String && !((String) q).matches("\\d+")) {
                    continue;
                } else if (d.contains(".m3u8")) {
                    /* Skip HLS */
                    continue;
                }
                final String qualityTmpStr = (String) q;
                if (StringUtils.equals(qualityTmpStr, preferredQuality)) {
                    logger.info("Found user preferred quality: " + qualityTmpStr);
                    dllink = d;
                    break;
                }
                final int qualityTmp = Integer.parseInt(qualityTmpStr);
                if (qualityTmp > qualityMax) {
                    qualityMax = Integer.parseInt((String) q);
                    dllink = d;
                }
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("addVariable\\(\"file\"\\s*,.*?\"(https?://.*?\\.flv(\\?.*?)?)\"").getMatch(0);
            if (dllink == null) {
                // 02.dec.2016
                dllink = br.getRegex("<source src=\"([^\"]+)").getMatch(0);
            }
            if (dllink == null) {
                // 02.dec.2016
                dllink = br.getRegex("newLink\\.setAttribute\\('href'\\s*,\\s*'([^']+)").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://(mediax|cdn[a-z]\\.videos)\\.youjizz\\.com/[A-Z0-9]+\\.flv(\\?.*?)?)\"").getMatch(0);
                if (dllink == null) {
                    // class="buttona" >Download This Video</
                    dllink = br.getRegex("\"(http://im\\.[^<>\"]+)\"").getMatch(0);
                }
                if (dllink == null) {
                    String playlist = br.getRegex("so\\.addVariable\\(\"playlist\"\\s*,\\s*\"(https?://(www\\.)?youjizz\\.com/playlist\\.php\\?id=\\d+)").getMatch(0);
                    if (playlist == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    Browser br2 = br.cloneBrowser();
                    br2.getPage(playlist);
                    // multiple qualities (low|med|high) grab highest for now, decrypter will be needed for others.
                    dllink = br2.getRegex("<level bitrate=\"\\d+\" file=\"(https?://(\\w+\\.){1,}youjizz\\.com/[^\"]+)\" ?></level>[\r\n\t]+</levels>").getMatch(0);
                    if (dllink != null) {
                        dllink = dllink.replace("%252", "%2");
                    }
                }
            }
        }
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlOnlyDecode(dllink);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (con.isOK() && !con.getContentType().contains("html")) {
                String ext = getFileNameFromHeader(con).substring(getFileNameFromHeader(con).lastIndexOf("."));
                link.setFinalFileName(Encoding.htmlDecode(filename) + ext);
                link.setDownloadSize(con.getLongContentLength());
            } else {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
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
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("text")) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getPreferredStreamQuality() {
        final YouJizzComConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final PreferredStreamQuality quality = cfg.getPreferredStreamQuality();
        switch (quality) {
        case Q2160P:
            return "2160";
        case Q1080P:
            return "1080";
        case Q720P:
            return "720";
        case Q480P:
            return "480";
        case Q360P:
            return "360";
        case Q240P:
            return "240";
        case BEST:
        default:
            return null;
        }
    }

    @Override
    public Class<? extends YouJizzComConfig> getConfigInterface() {
        return YouJizzComConfig.class;
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