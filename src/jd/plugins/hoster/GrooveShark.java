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

import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "grooveshark.com" }, urls = { "http://grooveshark\\.viajd/(s/.*?/\\w+|song/\\d+)" }, flags = { 0 })
public class GrooveShark extends PluginForHost {

    private String              CLIENTREVISION = null;
    private static final String LISTEN         = "http://grooveshark.com/";
    private static final String USERUID        = UUID.randomUUID().toString().toUpperCase();
    private static String       TOKEN          = "";

    public GrooveShark(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2000l + (long) 1000 * (int) Math.round(Math.random() * 3 + Math.random() * 3));
    }

    private String cleanNameForURL(String name) {
        if (name == null) { return null; }
        name = name.replace("&", " and ");
        name = name.replace("#", " number ");
        name = name.replaceAll("[^\\w]", "_");
        name = decodeUnicode(name);
        name = name.trim().replaceAll("\\s", "_");
        name = name.replaceAll("__+", "_");
        name = Encoding.urlEncode(name);
        name = name.replace("%5F", "+");
        name = name.replace("_", "+");
        return name;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.viajd", ".com"));
    }

    private String decodeUnicode(final String s) {
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
        return "http://grooveshark.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String getPostParameterString(final Browser ajax, final String parameter, final String method, final String country) throws IOException {
        // ajax.getHeaders().put("Content-Type", "application/json");
        // ajax.getHeaders().put("Referer", parameter);
        final String sid = br.getCookie(parameter, "PHPSESSID");
        final String secretKey = getSecretKey(ajax, JDHash.getMD5(sid), sid);
        return "{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"" + CLIENTREVISION + "\",\"privacy\":0," + country + ",\"uuid\":\"" + USERUID + "\",\"session\":\"" + sid + "\",\"token\":\"" + getToken(method, secretKey) + "\"},\"method\":\"" + method + "\",";
    }

    private String getSecretKey(final Browser ajax, final String token, final String sid) throws IOException {
        ajax.postPageRaw(LISTEN + "more.php?getCommunicationToken", "{\"parameters\":{\"secretKey\":\"" + token + "\"},\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"" + CLIENTREVISION + "\",\"session\":\"" + sid + "\",\"uuid\":\"" + USERUID + "\"},\"method\":\"getCommunicationToken\"}");
        return ajax.getRegex("result\":\"(.*?)\"").getMatch(0);
    }

    private String getToken(final String method, final String secretKey) {
        String topSecretKey = Encoding.Base64Decode("c2l0QW5kRmFydEluTXlEdWNr");
        if (method.matches("(getSongFromToken|getTokenForSong)")) {
            topSecretKey = Encoding.Base64Decode("YmV3YXJlT2ZUaGVHaW5nZXJBcG9jYWx5cHNl");
        }
        final String lastRandomizer = makeNewRandomizer();
        return lastRandomizer + JDHash.getSHA1(method + ":" + secretKey + ":" + topSecretKey + ":" + lastRandomizer);
    }

    private void handleDownload(final DownloadLink downloadLink, final String country, final String sid) throws IOException, Exception, PluginException {
        // pro Titel ein secretKey
        // pro Request ein neuer Tokenhash(getToken)
        br.getHeaders().put("Content-Type", "application/json");
        final String secretKey = getSecretKey(br, JDHash.getMD5(sid), sid);
        // get songID
        br.getHeaders().put("Content-Type", "application/json");
        String songID = "{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"" + CLIENTREVISION + "\",\"privacy\":0," + country + ",\"uuid\":\"" + USERUID + "\",\"session\":\"" + sid + "\",\"token\":\"" + getToken("getSongFromToken", secretKey) + "\"},\"method\":\"getSongFromToken\",\"parameters\":{\"token\":\"" + TOKEN + "\"," + country + "}}";
        br.postPageRaw(LISTEN + "more.php?getSongFromToken", songID);

        if (br.getRegex("\\{\"code\":\\d+,\"message\":\"invalid token\"\\}").matches()) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        songID = br.getRegex("SongID\":\"(\\d+)\"").getMatch(0);
        // get streamKey
        br.getHeaders().put("Content-Type", "application/json");
        String streamKey = "{\"parameters\":{" + country + ",\"prefetch\":true,\"songID\":" + songID + ",\"mobile\":false},\"header\":{\"privacy\":0,\"token\":\"" + getToken("getStreamKeyFromSongIDEx", secretKey) + "\",\"session\":\"" + sid + "\"," + country + ",\"uuid\":\"" + USERUID + "\",\"client\":\"jsqueue\",\"clientRevision\":\"" + CLIENTREVISION + "\"},\"method\":\"getStreamKeyFromSongIDEx\"}";
        br.postPageRaw(LISTEN + "more.php?getStreamKeyFromSongIDEx", streamKey);
        streamKey = br.getRegex("streamKey\":\"(\\w+)\"").getMatch(0);
        if (streamKey == null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 300 * 1000L); }
        streamKey = "streamKey=" + streamKey.replace("_", "%5F");
        final String ip = br.getRegex("ip\":\"(.*?)\"").getMatch(0);
        final String dllink = "http://" + ip + "/stream.php";

        // JD v0.9.580: manual change Header or Response 400 Bad request
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        // Chunk: JD stable max. 1 or Chunkerror; JD svn > 1 no Chunkerror
        dl = BrowserAdapter.openDownload(br, downloadLink, dllink, streamKey, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getHeaders().put("Content-Type", "application/json");
        br.getPage(LISTEN);
        CLIENTREVISION = downloadLink.getStringProperty("clientrev");
        CLIENTREVISION = CLIENTREVISION == null ? "20110906" : CLIENTREVISION;
        final String url = downloadLink.getDownloadURL();
        final String sid = br.getCookie(LISTEN, "PHPSESSID");
        final String country = br.getRegex(Pattern.compile("\"country(.*?)}", Pattern.UNICODE_CASE)).getMatch(-1);
        if (new Regex(url, "grooveshark\\.com/song/\\d+").matches()) {
            // converts from a virtual link to a real link
            // we pass virtual links from decrypter, because we do not want
            // to do a linkcheck when adding the link ... this would do too
            // much requests.
            final Browser ajax = br;
            String rawPost = getPostParameterString(ajax, LISTEN, "getTokenForSong", country);
            rawPost = rawPost + "\"parameters\":{\"songID\":\"" + downloadLink.getStringProperty("SongID") + "\"," + country + "}}";
            ajax.getHeaders().put("Content-Type", "application/json");
            ajax.postPageRaw(LISTEN + "more.php?getTokenForSong", rawPost);
            TOKEN = ajax.getRegex("Token\":\"(\\w+)\"").getMatch(0);
            // valid secret key?
            if (TOKEN == null && ajax.containsHTML("\"message\":\"invalid token\"")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            // Limitations after ~20 Songs
            if (TOKEN == null) {
                if (ajax.containsHTML("attacker") || ajax.containsHTML("\"Token\":false")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 300 * 1000L); }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String Name = downloadLink.getStringProperty("Name");
            Name = cleanNameForURL(Name);
            final String dllink = LISTEN + "s/" + Name + "/" + TOKEN;
            downloadLink.setUrlDownload(dllink);
        } else if (new Regex(url, "grooveshark\\.com/.*?/\\w+").matches()) {
            // direct links..
            TOKEN = url.substring(url.lastIndexOf("/") + 1);
            if (TOKEN.equals("404")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDownload(downloadLink, country, sid);
    }

    private String makeNewRandomizer() {
        final String charList = "0123456789abcdef";
        final char[] chArray = new char[6];
        final Random random = new Random();
        int i = 0;
        do {
            chArray[i] = charList.toCharArray()[random.nextInt(16)];
            i++;
        } while (i <= 5);
        return new String(chArray);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        if (new Regex(url, "grooveshark\\.com\\/song\\/\\d+").matches()) {
            return AvailableStatus.TRUE;
        } else {
            /* 0.95xx comp */
            url = url.contains("%20") ? url.replace("%20", "+") : url;

            br.getPage(url);
            if (br.containsHTML("not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            String[] filenm = br.getRegex("<h1 class=\"song\">(.*?)by\\s+<a href=\".*?\" rel=\"artist parent\" rev=\"child\">(.*?)</a>.*?on.*?<a href=.*? rel=\"album parent\" rev=\"child\">(.*?)</a>.*?</h1>").getRow(0);
            if (filenm == null || filenm.length != 3) {
                filenm = br.getRegex("<meta name=\"title\" content=\"(.*?)\\sby\\s(.*?)\\son\\s(.*?)\"").getRow(0);
                if (filenm == null || filenm.length != 3) {
                    filenm = br.getRegex("<title>(.*?)\\sby\\s(.*?)\\son\\s(.*?)\\s\\- Free Music Streaming\\, Online Music\\, Videos \\- Grooveshark</title>").getRow(0);
                }
            }
            if (filenm == null || filenm.length != 3) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            final String filename = filenm[1].trim() + " - " + filenm[2].trim() + " - " + filenm[0].trim() + ".mp3";
            if (filename != null) {
                downloadLink.setName(filename.trim());
            }
        }
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