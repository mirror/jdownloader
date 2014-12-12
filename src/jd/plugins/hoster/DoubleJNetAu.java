//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "doublej.net.au" }, urls = { "http://(www\\.)?doublej\\.net\\.au/programs/[a-z0-9\\-]+/.+" }, flags = { 0 })
public class DoubleJNetAu extends PluginForHost {

    // raztoki embed video player template.

    private String[] links = null;
    private Browser  ajax  = null;
    private Browser  m3u   = null;

    /**
     * @author raztoki
     */
    public DoubleJNetAu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.doublej.net.au/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public class AudioStream implements Storable {
        private String arid;

        public String getArid() {
            return arid;
        }

        public void setArid(String arid) {
            this.arid = arid;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getType() {
            return type;
        }

        public void setType(String tyoe) {
            this.type = tyoe;
        }

        private String url;
        private String type;

        private AudioStream(/* storable */) {
        }
    }

    public class Enity implements Storable {
        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        private Enity(/* storable */) {
        }
    }

    public class StreamInfo implements Storable {
        private Enity published_entity;

        public Enity getPublished_entity() {
            return published_entity;
        }

        public void setPublished_entity(Enity published_entity) {
            this.published_entity = published_entity;
        }

        public AudioStream[] getAudio_streams() {
            return audio_streams;
        }

        public void setAudio_streams(AudioStream[] audio_streams) {
            this.audio_streams = audio_streams;
        }

        private AudioStream[] audio_streams;

        private StreamInfo(/* storable */) {
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br = new Browser();

        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", "Opera/9.80 (Windows NT 6.1; Win64; x64) Presto/2.12.388 Version/12.17");
        // first get
        br.getPage(downloadLink.getDownloadURL());
        // we want to grab the uid, for json requests. (case sensitive, need to grab the one in bracket!
        final String fuid = br.getRegex("-\\s*On-demand\\s*\\(([A-Za-z0-9]+)\\)</a></h2></div>").getMatch(0);
        if (fuid == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // get associated json crapola
        getAjax("http://program.abcradio.net.au/api/v1/on_demand/" + fuid + ".json");
        StreamInfo si = JSonStorage.restoreFromString(ajax.toString(), new TypeRef<StreamInfo>() {
        });

        //

        String url1 = null;
        for (AudioStream asNode : si.getAudio_streams()) {
            if ("HLS".equals(asNode.getType())) {
                // HDS doesn't work... you get 503
                url1 = asNode.getUrl();
            }
        }

        downloadLink.setFinalFileName(si.getPublished_entity().getTitle() + ".m4a");
        // set m3u stuff
        getM3u(url1);
        // url string
        String url2 = m3u.getRegex("https?://[^\r\n\t]+").getMatch(-1);
        //
        url2 = url2 + "";
        //
        downloadLink.setProperty("m3uUrl", url2);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        if (links == null || links.length == 0) {
            requestFileInformation(downloadLink);
        }
        dl = new HLSDownloader(downloadLink, br, downloadLink.getStringProperty("m3uUrl"));
        dl.startDownload();

    }

    private void getAjax(final String url) throws IOException {
        // no cookie session
        ajax = new Browser();
        ajax.getHeaders().put("User-Agent", br.getHeaders().get("User-Agent"));
        ajax.getHeaders().put("Accept-Language", "en-AU,en;q=0.9");
        ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        ajax.getHeaders().put("Origin", "http://doublej.net.au");
        ajax.getHeaders().put("Accept-Charset", null);
        ajax.getHeaders().put("Cache-Control", null);
        ajax.getHeaders().put("Pragma", null);
        ajax.getPage(url);
    }

    private void getM3u(String url) throws IOException {
        // cookie _alid_ gets set on first request.
        if (m3u == null) {
            m3u = new Browser();
        }
        m3u.getHeaders().put("User-Agent", br.getHeaders().get("User-Agent"));
        m3u.getHeaders().put("Accept-Language", "en-AU,en;q=0.9");
        m3u.getHeaders().put("Accept", "text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/webp, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1");
        m3u.getHeaders().put("Referer", "http://www.abc.net.au/radio/player/beta/scripts/vendor/jwplayer/jwplayer.flash.swf");
        m3u.getHeaders().put("Accept-Charset", null);
        m3u.getHeaders().put("Cache-Control", null);
        m3u.getHeaders().put("Pragma", null);
        m3u.getPage(url);
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