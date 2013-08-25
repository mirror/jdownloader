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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flashx.tv" }, urls = { "http://((www\\.)?flashx\\.tv/video/[A-Z0-9]+/|play\\.flashx\\.tv/player/embed\\.php\\?.+|play\\.flashx\\.tv/player/fxtv\\.php\\?.+)" }, flags = { 0 })
public class FlashxTv extends PluginForHost {

    public FlashxTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://flashx.tv/page/3/terms-of-service";
    }

    private static final String NOCHUNKS = "NOCHUNKS";

    @Override
    public void correctDownloadLink(DownloadLink link) {
        if (link.getDownloadURL().matches(".+play\\.flashx\\.tv/player.+")) {
            // String hash = new Regex(link.getDownloadURL(), "(?i\\-)hash=([A-Z0-9]{12})").getMatch(0);
            String hash = new Regex(link.getDownloadURL(), "\\?hash=([A-Z0-9]{12})").getMatch(0);
            if (hash != null) link.setUrlDownload(link.getDownloadURL().replaceAll("http://play.flashx.tv/player/.+", "http://www.flashx.tv/video/" + hash + "/"));
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Requested page not found|>404 Error<|>Video not found, deleted or abused, sorry\\!<|>Video not found or deleted<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"video_title\">([^<>\"]*?)</div>").getMatch(0);
        if (filename == null) filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        String dllink = null;
        // 1
        String regex = "\"(http://(flashx\\.tv/player/embed_player\\.php|play\\.flashx\\.tv/player/embed\\.php)\\?[^<>\"]*?)\"";
        final String firstlink = br.getRegex(regex).getMatch(0);
        if (firstlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(firstlink);
        String seclinkgk = br.getRegex(fx(0)).getMatch(1);
        // 2
        regex = "\"(http://play\\.flashx\\.tv/player/[^\"]+)\"";
        String seclink = br.getRegex(regex).getMatch(0);
        if (seclink == null) seclink = getPlainData(regex);
        if (seclink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(seclink);
        if (br.containsHTML("We are currently performing maintenance on this server")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This server is under maintenance", 30 * 60 * 1000l);
        // 2.1
        if (seclinkgk == null) seclinkgk = br.getRegex(fx(0)).getMatch(1);
        if (seclinkgk == null) seclinkgk = getPlainData(fx(0));
        if (seclinkgk == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // 3
        regex = "config=(http://play.flashx.tv/nuevo/[^\"]+)\"";
        String thirdLink = br.getRegex(regex).getMatch(0);
        if (thirdLink == null) thirdLink = getPlainData(regex);
        if (thirdLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(seclinkgk);
        br.getPage(thirdLink);
        dllink = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        int chunks = -2;
        if (downloadLink.getBooleanProperty(FlashxTv.NOCHUNKS, false)) {
            chunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">404 \\- Not Found<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(FlashxTv.NOCHUNKS) == false) {
                    downloadLink.setChunksProgress(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(FlashxTv.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(FlashxTv.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    private String fx(final int i) {
        final String[] s = new String[1];
        s[0] = "f8dbfbfafa57cde11f94b695de5042f1299371b7095fdd9a1e185a3b116e82845888fd3e8900e26f211655e8eb771a74e722299bc69a6263a823d6e66e0f373e5af4c82e5827ffcf25a92ebe5261c8e945a78e856ffc9dad998a2a9528657811c6733e016c8b806b391101aa1b30162b03b18a7534a6719d83c0607d4f625dc08a6e4db2cd63d9c7321c08d37306c3b7d933074e56c2b0a81d8739ac6c6775c51d775c0e345d7b121226c64adc65d86d1db07b2042f449930428adf7d6a9520b60d0f0d6";
        return JDHexUtils.toString(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(s[i]));
    }

    private String getPlainData(String regex) {
        String encrypted = br.getRegex("<script language=javascript>(.*?)</script>").getMatch(0);
        String decrypted = null;
        if (encrypted != null) {
            Object result = new Object();
            final ScriptEngineManager manager = new ScriptEngineManager();
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                engine.eval("var decrypted = '';");
                engine.eval("var document = new Object();");
                engine.eval("document.write = function(s) { decrypted += s; }");
                engine.eval(encrypted);
                result = engine.get("decrypted");
            } catch (final Throwable e) {
                return null;
            }
            if (result != null) decrypted = new Regex(result.toString(), regex).getMatch(0);
        }
        return decrypted;
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