//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.HashMap;
import java.util.Iterator;

import jd.PluginWrapper;
import jd.network.rtmp.url.RtmpUrlConnection;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "prosieben.de" }, urls = { "http://(www.\\)?prosieben\\.de/tv/[\\w-]+/video/[\\w-]+)" }, flags = { PluginWrapper.DEBUG_ONLY })
public class ProSevenDe extends PluginForHost {

    private HashMap<String, String> fileDesc;
    private String                  clipUrl = null;

    public ProSevenDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.prosieben.de/service/nutzungsbedingungen/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String dllink = clipUrl;

        if (dllink.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, dllink);
            final RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setSwfVfy("http://www.kabeleins.de/imperia/moveplayer/HybridPlayer.swf");
            rtmp.setUrl(dllink);
            rtmp.setResume(true);

            ((RTMPDownload) dl).startDownload();
        }
    }

    private void jsonParser(final String json, final String path) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode rootNode = mapper.readTree(json);
        final Iterator<JsonNode> iter = rootNode.get("categoryList").iterator();
        while (iter.hasNext()) {
            final Iterator<JsonNode> iter1 = iter.next().path("clipList").iterator();
            while (iter1.hasNext()) {
                final JsonNode t8 = iter1.next();
                final JsonNode t9 = t8.path("metadata");
                fileDesc = new HashMap<String, String>();
                if (t8.path("title") != null) {
                    fileDesc.put("title", t8.path("title").getTextValue());
                }
                if (t9.path(path) != null) {
                    fileDesc.put(path, t9.path(path).getTextValue());
                }
                if (t9.path("show_artist") != null) {
                    fileDesc.put("show_artist", t9.path("show_artist").getTextValue());
                }
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        final String jsonString = br.getRegex("json:\\s+\"(.*?)\"\n").getMatch(0).replaceAll("\\\\", "");
        jsonParser(jsonString, "downloadFilename");
        if (fileDesc == null || fileDesc.size() < 3) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        clipUrl = fileDesc.get("downloadFilename");
        if (fileDesc.get("show_artist") == null && fileDesc.get("title") == null || clipUrl == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String filename = fileDesc.get("show_artist") + "_" + fileDesc.get("title");
        downloadLink.setName(filename.trim() + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
