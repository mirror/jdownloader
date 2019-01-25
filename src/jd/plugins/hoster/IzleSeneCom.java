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
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "izlesene.com" }, urls = { "https?://(?:www\\.)?izlesene\\.com/video/[a-z0-9\\-]+/\\d+" })
public class IzleSeneCom extends PluginForHost {
    private String DLLINK = null;

    public IzleSeneCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.izlesene.com/yardim/kullanim";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.izlesene.com/player_xml/izlesene/" + new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
        final String name_url = new Regex(downloadLink.getDownloadURL(), "/video/([a-z0-9\\-]+)/").getMatch(0);
        if (!br.containsHTML("videoname") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<videoname>(.*?)</videoname>").getMatch(0);
        if (filename == null) {
            /* Fallback! */
            filename = name_url;
        }
        filename = Encoding.htmlDecode(filename).trim();
        downloadLink.setFinalFileName(filename + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final boolean useOldWay = false;
        if (useOldWay) {
            DLLINK = br.getRegex("<videosec>(.*?)</videosec>").getMatch(0);
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            DLLINK = "http://dcdn.nokta.com/" + DLLINK + "_1_2_1.xml";
            br.getPage(DLLINK);
            DLLINK = br.getRegex("<server durl=\"(https?://.*?)\"").getMatch(0);
            if (DLLINK == null) {
                DLLINK = br.getRegex("\"(https?://istr\\d+\\.izlesene\\.com/data/videos/\\d+/\\d+\\-\\d+.{1,5})\"").getMatch(0);
            }
        } else {
            /* Since 2016-05-31 */
            /*
             * E.g. https://istr.izlesene.com/data/videos/9351/9351615-360_2-135k.mp4?token=QQkqNta87oqV-AAUEliyzw&ts=1464792401 (token is
             * NEEDED!)
             */
            this.br.getPage(downloadLink.getDownloadURL());
            final String media = br.getRegex("\"media\"\\s*:\\s*(\\{\\s*\"defaultQuality\".*?\\})\\s*,\\s*\"").getMatch(0);
            if (media != null) {
                final Map<String, Object> map = JSonStorage.restoreFromString(media, TypeRef.HASHMAP);
                final List<Map<String, Object>> levels = (List<Map<String, Object>>) map.get("level");
                if (levels != null) {
                    int quality = -1;
                    for (final Map<String, Object> level : levels) {
                        final Number value = JavaScriptEngineFactory.toLong(level.get("value"), -1);
                        if (value.intValue() > quality) {
                            quality = value.intValue();
                            DLLINK = (String) level.get("source");
                        }
                    }
                }
            } else {
                DLLINK = PluginJSonUtils.getJsonValue(this.br, "streamurl");
            }
        }
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String ext = ".mp4";
        if (br.containsHTML("flv")) {
            ext = ".flv";
        }
        downloadLink.setFinalFileName(downloadLink.getName().replace(downloadLink.getName().substring(downloadLink.getName().length() - 4, downloadLink.getName().length()), ext));
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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