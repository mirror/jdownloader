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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "prosieben.de" }, urls = { "http://(www\\.)?(prosieben\\.de/tv/[\\w\\-]+|the\\-voice\\-of\\-germany\\.de)/videos?/(clip/[\\w\\-\\.]+/?|[\\w\\-]+)" }, flags = { 32 })
public class ProSevenDe extends PluginForHost {

    private HashMap<String, String> fileDesc;
    private String                  clipUrl = null;

    public ProSevenDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    @Override
    public String getAGBLink() {
        return "http://www.prosieben.de/service/nutzungsbedingungen/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void setupRTMPConnection(DownloadInterface dl, DownloadLink downloadLink, String[] stream) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

        rtmp.setApp(stream[1] + stream[2].substring(stream[2].indexOf("?")));
        rtmp.setUrl(stream[0] + stream[1]);
        rtmp.setPlayPath("mp4:" + stream[2]);
        rtmp.setSwfVfy("http://is.myvideo.de/player/GP/2.6.3/player.swf");
        rtmp.setPageUrl(downloadLink.getDownloadURL());
        rtmp.setResume(true);
    }

    private void download(DownloadLink downloadLink, String[] stream) throws Exception {
        dl = new RTMPDownload(this, downloadLink, clipUrl);
        setupRTMPConnection(dl, downloadLink, stream);
        ((RTMPDownload) dl).startDownload();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        clipUrl = clipUrl.replace("mp4:", "");
        clipUrl = clipUrl.replaceAll("\\\\", "");
        String[] stream = new Regex(clipUrl, "(rtmp.?://[0-9a-z]+\\.fplive\\.net/)([0-9a-z]+/[\\w\\-]+/\\d+)/(.*?)$").getRow(0);
        if (clipUrl.startsWith("rtmp")) {
            if (stream != null && stream.length == 3) {
                download(downloadLink, stream);
            }
        }
    }

    private void jsonParser(final String json, final String path) throws Exception {
        final org.codehaus.jackson.map.ObjectMapper mapper = new org.codehaus.jackson.map.ObjectMapper();
        final org.codehaus.jackson.JsonNode rootNode = mapper.readTree(json);
        final Iterator<org.codehaus.jackson.JsonNode> catIter = rootNode.get("categoryList").iterator();
        while (catIter.hasNext()) {
            final Iterator<org.codehaus.jackson.JsonNode> clipIter = catIter.next().path("clipList").iterator();
            while (clipIter.hasNext()) {
                final org.codehaus.jackson.JsonNode ta = clipIter.next();
                final org.codehaus.jackson.JsonNode tb = ta.path("metadata");
                fileDesc = new HashMap<String, String>();
                if (ta.path("title") != null) {
                    fileDesc.put("title", ta.path("title").getTextValue());
                }
                if (ta.path("id") != null) {
                    fileDesc.put("id", ta.path("id").getTextValue());
                }
                if (tb.path(path) != null) {
                    fileDesc.put(path, tb.path(path).getTextValue());
                }
                if (tb.path("show_artist") != null) {
                    fileDesc.put("show_artist", tb.path("show_artist").getTextValue());
                }
                if (tb.path("geoblocking") != null) {
                    fileDesc.put("geoblocking", tb.path("geoblocking").getTextValue());
                }
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String jsonString = br.getRegex("\"json\",\\s+\"(.*?)\"\n").getMatch(0);
        if (jsonString == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        jsonString = decodeUnicode(jsonString).replaceAll("\\\\", "");
        try {
            jsonParser(jsonString, "downloadFilename");
        } catch (final Throwable e) {
            return AvailableStatus.UNCHECKABLE;
        }
        if (fileDesc == null || fileDesc.size() < 5) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        for (Entry<String, String> next : fileDesc.entrySet()) {
            if (next.getValue() == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        clipUrl = fileDesc.get("downloadFilename");

        if (!clipUrl.startsWith("rtmp")) {
            if (!"".equals("")) {// old handling
                br.getPage("http://www.prosieben.de/static/videoplayer/config/playerConfig.json");
                String host = br.getRegex(fileDesc.get("geoblocking") + "\" : \"(.*?)\"").getMatch(0);
                if (host != null) {
                    host = host.replaceAll("clips:/", "mp4:");
                    clipUrl = String.format(host, fileDesc.get("downloadFilename"));
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else {
                br.getPage("http://ws.vtc.sim-technik.de/video/video.jsonp?clipid=" + fileDesc.get("id") + "&app=moveplayer&method=2&callback=SIMVideoPlayer.FlashPlayer.jsonpCallback");
                clipUrl = br.getRegex("\"VideoURL\"\\s?:\\s?\"(rtmp[^\"]+)").getMatch(0);
            }
        }
        if (clipUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String ext = new Regex(clipUrl, "(\\.\\w{3})$").getMatch(0);
        ext = ext == null ? ".mp4" : ext;
        downloadLink.setName(Encoding.htmlDecode((fileDesc.get("show_artist") + "_" + fileDesc.get("title")).trim()) + ext);
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