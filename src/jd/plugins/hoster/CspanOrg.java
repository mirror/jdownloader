//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "c-span.org" }, urls = { "https?://(?:www\\.)?c\\-span\\.org/video/\\?\\d+(?:\\-\\d+)?/[a-z0-9\\-]+" })
public class CspanOrg extends PluginForHost {
    public CspanOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.c-span.org/about/termsAndConditions/";
    }

    private static final String app = "cfx/st";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL().replace("http://", "https://"));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\\'og:title\\' content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = new Regex(link.getDownloadURL(), "([^/]+)$").getMatch(0);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // http://www.c-span.org/common/services/flashXml.php?programid=424926&version=2014-01-23
        final String progid = this.br.getRegex("name=\\'progid' value=\\'(\\d+)\\'").getMatch(0);
        if (progid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final boolean preferMobileHTTP = false;
        if (preferMobileHTTP) {
            long bitrate_max = 0;
            long bitrate_temp = 0;
            String dllink_http = null;
            /* 2017-05-09: Added this code as backup */
            this.br.getPage("https://www.c-span.org/assets/player/ajax-player.php?os=android&html5=program&id=" + progid);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            /* Find highest quality */
            final ArrayList<Object> qualities = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "video/files/{0}/qualities");
            for (final Object fileo : qualities) {
                entries = (LinkedHashMap<String, Object>) fileo;
                bitrate_temp = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "bitrate/#text"), 0);
                if (bitrate_temp > bitrate_max) {
                    dllink_http = (String) JavaScriptEngineFactory.walkJson(entries, "file/#text");
                }
            }
            if (StringUtils.isEmpty(dllink_http)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Important! */
            dllink_http = Encoding.htmlDecode(dllink_http);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink_http, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            br.getPage("https://www.c-span.org/common/services/flashXml.php?programid=" + progid);
            final String rtmp_host = this.br.getRegex("name=\"url\">\\$\\(protocol\\)(://[^<>\"]*?):\\$\\(port\\)/cfx/st").getMatch(0);
            final String playpath = this.br.getRegex("name=\"path\">(mp4:[^<>\"]*?)</string>").getMatch(0);
            if (playpath == null || rtmp_host == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String rtmpurl = "rtmp" + rtmp_host + "/" + app;
            try {
                dl = new RTMPDownload(this, downloadLink, rtmpurl);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setUrl(rtmpurl);
            rtmp.setPlayPath(playpath);
            rtmp.setApp(app);
            rtmp.setFlashVer("WIN 25,0,0,171");
            rtmp.setSwfUrl("https://www.c-span.org/assets/swf/CSPANPlayer.swf?programid=424926" + progid);
            rtmp.setResume(true);
            ((RTMPDownload) dl).startDownload();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}