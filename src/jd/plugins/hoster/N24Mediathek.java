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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "n24.de" }, urls = { "http://(www\\.)?n24\\.de/mediathek/[\\w\\-]+\\.html" }, flags = { 32 })
public class N24Mediathek extends PluginForHost {

    private String DLLINK = null;

    public N24Mediathek(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.n24.de/service/nutzungsbedingungen_1/nutzungsbedingungen.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setUrl(stream[0]);
        rtmp.setPlayPath(stream[1]);
        rtmp.setSwfVfy(stream[2]);
        rtmp.setResume(true);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        String stream[] = DLLINK.split("@");
        if (stream[0].startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, stream[0]);
            setupRTMPConnection(stream, dl);

            ((RTMPDownload) dl).startDownload();

        } else {
            throw new PluginException(LinkStatus.ERROR_FATAL, "N24-Mediathek: " + stream[0]);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());

        if (br.containsHTML(">(Aus lizenzrechtlichen Gründen ist dieses Video momentan nicht auf N24\\.de zu sehen|Die von Ihnen angeforderte Website konnte leider nicht angezeigt werden)\\.<")) {
            logger.info("N24-Mediathek: Not available --> " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        String fValues = br.getRegex("class=\"jsb_ jsb_flash_player\" type=\"hidden\" value=\"\\{(.*?)\\}").getMatch(0);
        if (fValues == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        fValues = fValues.replaceAll("\\&quot;", "@");
        HashMap<String, String> flashValues = new HashMap<String, String>();
        for (String s[] : new Regex(fValues, "@([^@]+)@:@([^@]+)@,").getMatches()) {
            flashValues.put(s[0], decodeUnicode(s[1]).replaceAll("\\\\", ""));
        }

        String playPath = flashValues.get("filename");
        String flashPlayer = flashValues.get("playerUrl");
        String clipId = flashValues.get("clip_id");
        String titleHeader = flashValues.get("header");
        String titleName = flashValues.get("title");
        if (playPath == null || flashPlayer == null || clipId == null || titleHeader == null | titleName == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        Browser flashPlayerJS = br.cloneBrowser();
        flashPlayerJS.getPage("/mediathek/static/js/FlashPlayer.js");
        if (flashPlayerJS.getHttpConnection().getResponseCode() != 200) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String rtmpUrl = flashPlayerJS.getRegex("player_video_item.connectionUrl = \"(rtmp[^\"]+)\"").getMatch(0);
        if (rtmpUrl == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        downloadLink.setFinalFileName(Encoding.htmlDecode((titleHeader + "_" + titleName + ".mp4").trim()));
        DLLINK = rtmpUrl + "@" + playPath + "@" + flashPlayer;

        return AvailableStatus.TRUE;
    }

    private String decodeUnicode(String s) {
        Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        if (res == null) res = s;
        return res;
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