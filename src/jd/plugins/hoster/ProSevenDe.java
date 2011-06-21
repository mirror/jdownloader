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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "prosieben.de" }, urls = { "http://(www\\.)?prosieben\\.de/tv/[\\w-]+/video(s)?/(clip/[\\w-\\.]+/?|[\\w-]+)" }, flags = { PluginWrapper.DEBUG_ONLY })
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

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String dllink = clipUrl;

        if (dllink.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, dllink);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setSwfVfy("http://www.kabeleins.de/imperia/moveplayer/HybridPlayer.swf");
            rtmp.setUrl(dllink);
            rtmp.setPort(1935);
            rtmp.setProtocol(0);
            rtmp.setResume(true);

            ((RTMPDownload) dl).startDownload();
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
        String jsonString = br.getRegex("json:\\s+\"(.*?)\"\n").getMatch(0);
        jsonString = decodeUnicode(jsonString).replaceAll("\\\\", "");
        try {
            jsonParser(jsonString, "downloadFilename");
        } catch (final Throwable e) {
            return AvailableStatus.UNCHECKABLE;
        }
        if (fileDesc == null || fileDesc.size() < 4) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        clipUrl = fileDesc.get("downloadFilename");
        if (fileDesc.get("show_artist") == null && fileDesc.get("title") == null || clipUrl == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String filename = fileDesc.get("show_artist") + "_" + fileDesc.get("title");
        if (!clipUrl.startsWith("rtmp")) {
            br.getPage("http://www.prosieben.de/static/videoplayer/config/playerConfig.json");
            final String host = br.getRegex(fileDesc.get("geoblocking") + "\" : \"(.*?)\"").getMatch(0);
            if (host != null) {
                clipUrl = String.format(host, fileDesc.get("downloadFilename"));
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        String ext = new Regex(clipUrl, "(\\.\\w{3})$").getMatch(0);
        ext = ext == null ? ".flv" : ext;
        downloadLink.setName(filename.trim() + ext);
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
