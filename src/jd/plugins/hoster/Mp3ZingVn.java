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
/**
 * @author noone2407
 */
package jd.plugins.hoster;

import java.util.LinkedHashMap;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

/**
 * @author noone2407
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mp3.zing.vn" }, urls = { "https?://mp3\\.zing\\.vn/bai-hat/(\\S+)\\.html$" })
public class Mp3ZingVn extends PluginForHost {
    private String dllink = null;

    public Mp3ZingVn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://mp3.zing.vn/huong-dan/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        br.setFollowRedirects(true);
        br.getPage(url);
        if (isOffline()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>(.*?)( \\| Zing MP3)?</title>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(filename.replaceFirst("\\.{3}$", "").replace(":", "-") + ".mp3");
        final String datacode = br.getRegex("<a\\s+(?:[^>]*?\\s+)?data-code=\"(.*?)\"").getMatch(0);
        final String json_source = br.getPage("https://mp3.zing.vn/xhr/media/get-url-download?type=audio&panel=.fn-tab-panel-service&code=" + datacode + "&group=.fn-tab-panel");
        if (isOffline()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json_source);
        if ("Không thể download bài hát này vì yêu cầu từ nhà sở hữu bản quyền.".equals(entries.get("msg"))) {
            // from google translate
            // Unable to download this song because the request from the copyright owner.
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("Không thể download bài hát này vì yêu cầu từ nhà sở hữu bản quyền.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // final String datalink = (String) JavaScriptEngineFactory.walkJson(entries, "data/128/link");
        // final Integer datasize = (Integer) JavaScriptEngineFactory.walkJson(entries, "data/128/size");
        // final Boolean vip = (Boolean) JavaScriptEngineFactory.walkJson(entries, "data/128/vip");
        final String free_json = new Regex(json_source, "\"128\":(\\{.*?\\})").getMatch(0);
        final String size = PluginJSonUtils.getJsonValue(free_json, "size");
        final String link = PluginJSonUtils.getJsonValue(free_json, "link");
        // logger.info("free_json: " + free_json + " " + size + " " + link);
        dllink = link;
        if (size == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setDownloadSize(Long.valueOf(size));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("json")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private boolean isOffline() {
        return br.containsHTML("title-404|vip_required|Bài hát này không nghe") || br.getHttpConnection().getResponseCode() == 404;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
