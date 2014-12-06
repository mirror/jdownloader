//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "audiomack.com" }, urls = { "http://(www\\.)?audiomack\\.com/(song/[a-z0-9\\-_]+/[a-z0-9\\-_]+|api/music/url/album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/\\d+)" }, flags = { 0 })
public class AudioMa extends PluginForHost {

    public AudioMa(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.audiomack.com/about/terms-of-service";
    }

    private static final String  TYPE_API       = "http://(www\\.)?audiomack\\.com/api/music/url/album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/\\d+";
    private static final boolean use_oembed_api = true;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename;
        if (use_oembed_api) {
            br.getPage("http://www.audiomack.com/oembed?format=json&url=" + Encoding.urlEncode(link.getDownloadURL()));
            if (br.containsHTML(">Did not find any music with url")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String artist = getJson("author_name");
            final String songname = getJson("title");
            if (artist == null || songname == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = artist + " - " + songname;
        } else if (link.getDownloadURL().matches(TYPE_API)) {
            br.getPage(link.getStringProperty("mainlink", null));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = link.getStringProperty("plain_filename", null);
        } else {
            br.getPage(link.getDownloadURL());
            if (br.containsHTML(">Page not found<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<aside class=\"span2\">[\t\n\r ]+<h1>([^<>\"]*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^<>\"]*?) \\- download and stream \\| AudioMack</title>").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("name=\"twitter:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* Access real link here in case we used the oembed API above */
        if (use_oembed_api) {
            br.getPage(downloadLink.getDownloadURL());
        }
        /* Prefer downloadlink --> Higher quality version */
        String dllink = br.getRegex("\"(http://(www\\.)?music\\.audiomack\\.com/tracks/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            String apilink;
            if (downloadLink.getDownloadURL().matches(TYPE_API)) {
                apilink = downloadLink.getDownloadURL();
            } else {
                apilink = br.getRegex("\"(http://(www\\.)?audiomack\\.com/api/[^<>\"]*?)\"").getMatch(0);
                if (apilink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            br.getPage(apilink);
            dllink = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replace("\\", "");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
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