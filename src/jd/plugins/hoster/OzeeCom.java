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

import org.appwork.utils.IO;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.cryptojs.CryptoJS;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision: 36028 $", interfaceVersion = 3, names = { "ozee.com" }, urls = { "https?://(?:www\\.)?ozee\\.com/(videos/[^/]+/|shows/[^/]+/video/)[a-z0-9\\-]+\\.html" })
public class OzeeCom extends PluginForHost {

    public OzeeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://www.ozee.com/terms-and-conditions";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("class=\"error\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "([^/]+)\\.html$").getMatch(0);
        String filename = PluginJSonUtils.getJsonValue(this.br, "name");
        if (filename == null) {
            filename = url_filename;
        }
        final String result = processJS();
        if (result != null) {
            dllink = result;
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
        link.setFinalFileName(filename);
        if (!(Thread.currentThread() instanceof SingleDownloadController)) {
            final HLSDownloader downloader = new HLSDownloader(link, br, this.dllink);
            final StreamInfo streamInfo = downloader.getProbe();
            if (link.getBooleanProperty("encrypted")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Encrypted HLS is not supported");
            }
            if (streamInfo != null) {
                final long estimatedSize = downloader.getEstimatedSize();
                if (link.getKnownDownloadSize() == -1) {
                    link.setDownloadSize(estimatedSize);
                } else {
                    link.setDownloadSize(Math.max(link.getKnownDownloadSize(), estimatedSize));
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getPage(dllink);
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        final String dllink = hlsbest.getDownloadurl();
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, dllink);
        dl.startDownload();
    }

    private String processJS() {
        String result = null;
        try {
            final String json_crypted = this.br.getRegex("hlsplayurl\\s*?=\\s*?\\'([^<>\\']+)\\'").getMatch(0);
            String user = br.getRegex("var dailytoday\\s*?=\\s*?\"([^<>\"]+)\";").getMatch(0);
            if (user == null || json_crypted == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String js3 = "var newLocation=JSON.parse(CryptoJS.AES.decrypt(link, user, {format: CryptoJSAesJson}).toString(CryptoJS.enc.Utf8));";
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            engine.put("link", json_crypted);
            engine.put("user", user);
            engine.eval(IO.readURLToString(CryptoJS.class.getResource("aes.js")));
            engine.eval(IO.readURLToString(CryptoJS.class.getResource("aes-json-format.js")));
            engine.eval(js3);
            result = (String) engine.get("newLocation");
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return result;
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
