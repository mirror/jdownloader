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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "avgle.com" }, urls = { "https?://(?:www\\.)?avgle\\.com/video/\\d+" })
public class AvgleCom extends PluginForHost {
    public AvgleCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags: porn plugin
    // protocol: no https
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String default_extension = ".mp4";
    /* Connection stuff */
    // private static final boolean free_resume = true;
    // private static final int free_maxchunks = 0;
    private static final int    free_maxdownloads = -1;
    private String              dllink            = null;
    private boolean             server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://avgle.com/static/terms";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains("/video")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        dllink = decodeKode();
        if (dllink == null) {
            /* 2017-07-21: Files are usually hosted on Googlevideo */
            dllink = PluginJSonUtils.getJson(this.br, "s3");
            if (dllink != null) {
                dllink = Encoding.Base64Decode(dllink);
            }
            if (dllink != null && !dllink.startsWith("http")) {
                dllink = null;
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = default_extension;
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (!StringUtils.isEmpty(dllink)) {
            link.setFinalFileName(filename);
            checkFFProbe(link, "Download a HLS Stream");
            final HLSDownloader downloader = new HLSDownloader(link, br, dllink);
            final StreamInfo streamInfo = downloader.getProbe();
            if (streamInfo == null) {
                // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                server_issues = true;
            } else {
                final long estimatedSize = downloader.getEstimatedSize();
                if (estimatedSize > 0) {
                    link.setDownloadSize(estimatedSize);
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    private String decodeKode() throws Exception {
        String kode = br.getRegex("(var kode=\".+?\"),").getMatch(0);
        if (kode == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(kode);
        sb.append(";");
        sb.append("for(;-1===kode.indexOf(\"getElementById('K_ID')\");)eval(kode);");
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval(sb.toString());
            kode = (String) engine.get("kode");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        String[] videoInfo = new Regex(kode, "data-hash=\\\\\"(.+?)\\\\\" data-ts=\\\\\"(.+?)\\\\\" data-vid=\\\\\"(.+?)\\\\\"").getRow(0);
        if (videoInfo == null || videoInfo.length != 3) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        Browser ajax = br.cloneBrowser();
        ajax.getPage("/video-url.php?hash=" + videoInfo[0] + "&ts=" + videoInfo[1] + "&vid=" + videoInfo[2]);
        return PluginJSonUtils.getJson(ajax, "url");
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
