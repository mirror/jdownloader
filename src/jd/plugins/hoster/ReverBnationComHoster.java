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

import java.util.Map;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "reverbnation.com" }, urls = { "https?://(?:www\\.)?reverbnation\\.com/([^/]+)/song/(\\d+)-([a-z0-9\\-]+)" })
public class ReverBnationComHoster extends PluginForHost {
    @SuppressWarnings("deprecation")
    public ReverBnationComHoster(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.reverbnation.com/main/terms_and_conditions";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    public static final String PROPERTY_DIRECTURL = "directlink";
    public static final String PROPERTY_POSITION  = "position";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("var config\\s*=\\s*(\\{.*?\\});").getMatch(0);
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json);
        final Map<String, Object> song = (Map<String, Object>) entries.get("PRIMARY_SONG");
        parseFileInfo(link, song);
        return AvailableStatus.TRUE;
    }

    public static void parseFileInfo(final DownloadLink link, final Map<String, Object> song) {
        final Map<String, Object> artist = (Map<String, Object>) song.get("artist");
        final String title = (String) song.get("name");
        // final String access = (String) song.get("access"); // e.g. "streaming_only"
        // Field "protected" == DRM protected flag??
        final String url = (String) song.get("url");
        if (!StringUtils.isEmpty(url)) {
            link.setProperty(PROPERTY_DIRECTURL, url);
        }
        link.setFinalFileName(artist.get("name") + title + ".mp3");
        /* Set estimated filesize */
        final long durationSeconds = ((Number) song.get("duration")).longValue();
        final long bitrate = ((Number) song.get("bitrate")).longValue();
        link.setDownloadSize((durationSeconds * bitrate * 1024) / 8);
        link.setAvailable(true);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
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
    public void resetDownloadlink(final DownloadLink link) {
    }
}