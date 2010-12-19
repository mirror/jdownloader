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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "grooveshark.com", "grooveshark.com" }, urls = { "http://listen\\.grooveshark\\.viajd/(#/)?.+", "http://grooveshark\\.viajd/song/\\d+" }, flags = { 0, 0 })
public class GrooveShark extends PluginForHost {

    private static final String LISTEN  = "http://listen.grooveshark.com/";
    private static final String COWBELL = "http://cowbell.grooveshark.com/";
    private static final String USERID  = UUID.randomUUID().toString().toUpperCase();

    public GrooveShark(final PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("http://listen.grooveshark.com/#/");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.viajd", ".com"));
    }

    // private void login(Account account) throws PluginException, IOException {
    // this.setBrowserExclusive();
    // br.setDebug(true);
    // this.br.getPage(GrooveShark.LISTEN);
    // final String country =
    // this.br.getRegex(Pattern.compile("\"country(.*?)}",
    // Pattern.UNICODE_CASE)).getMatch(-1);
    // br.postPage("https://listen.grooveshark.com/empty.php", "username=" +
    // account.getUser() + "&password=" + account.getPass());
    //
    // Browser ajax = br.cloneBrowser();
    // // get SongID
    // ajax.getHeaders().put("Content-Type", "application/json");
    // ajax.getHeaders().put("Referer",
    // "http://listen.grooveshark.com/JSQueue.swf?20101203.14");
    // ajax.getHeaders().put("x-flash-version", "10,1,53,64");
    //
    // final String sid = br.getCookie(GrooveShark.LISTEN, "PHPSESSID");
    // // https://cowbell.grooveshark.com/more.php?authenticateUser
    //
    // String loginJSon = "{\"parameters\":{\"savePassword\":0,\"password\":\""
    // + account.getPass() + "\",\"username\":\"" + account.getUser() +
    // "\"},\"header\":" + "{\"token\":\"token\"," +
    // "\"clientRevision\":\"20101012.36\"," + "\"client\":\"htmlshark\"," +
    // "\"session\":\"" + sid + "\",\"privacy\":0," + country + ",\"uuid\":\"" +
    // GrooveShark.USERID + "\"},\"method\":\"authenticateUser\"}";
    //
    // // this.br.getHeaders().put("x-flash-version", "10,1,53,64");
    // final String token = this.getRequestToken("authenticateUser",
    // JDHash.getMD5(sid), sid);
    //
    // ajax.postPageRaw("https://cowbell.grooveshark.com/more.php?authenticateUser",
    // loginJSon.replace("\"token\":\"token\"", "\"token\":\"" + token + "\""));
    // userID = ajax.getRegex("\"userID\"\\:(\\d+)").getMatch(0);
    // userName = ajax.getRegex("\"username\"\\:\"(.*?)\"").getMatch(0);
    // isPremium =
    // "1".equals(ajax.getRegex("\"isPremium\"\\:\"(\\d)\"").getMatch(0));
    // authCode = ajax.getRegex("\"authToken\"\\:\"(.*?)\"").getMatch(0);
    // if (userID == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    //
    // }
    // }

    @Override
    public String getAGBLink() {
        return "http://grooveshark.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String getPostParameterString(Browser ajax, final String parameter, final String method, final String country) throws IOException {

        ajax.getHeaders().put("Content-Type", "application/json");
        ajax.getHeaders().put("Referer", parameter);
        final String sid = this.br.getCookie(parameter, "PHPSESSID");
        final String token = this.getSecretToken(ajax, method, JDHash.getMD5(sid), sid);
        final String str = "{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":20100831," + country + ",\"uuid\":\"" + USERID + "\",\"session\":\"" + sid + "\",\"token\":\"" + token + "\"},\"method\":\"" + method + "\",";
        return str;
    }

    private String getSecretToken(Browser ajax, final String method, final String token, final String sid) throws IOException {
        ajax.getHeaders().put("Content-Type", "application/json");
        String secretKey = "{\"parameters\":{\"secretKey\":\"" + token + "\"},\"header\":{\"client\":\"jsqueue\",\"clientRevision\":\"20100831.08\",\"session\":\"" + sid + "\",\"uuid\":\"" + USERID + "\"},\"method\":\"getCommunicationToken\"}";
        ajax.postPageRaw("https://cowbell.grooveshark.com/service.php", secretKey);
        secretKey = ajax.getRegex("result\":\"(.*?)\"").getMatch(0);
        final String lastRandomizer = this.makeNewRandomizer();
        final String z = lastRandomizer + JDHash.getSHA1(method + ":" + secretKey + ":quitStealinMahShit:" + lastRandomizer);
        return z;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.requestFileInformation(downloadLink);
        String url = downloadLink.getDownloadURL();
        if (new Regex(url, "grooveshark\\.com\\/song\\/\\d+").matches()) {
            // converts from a virtual link to a real link
            // we pass virtual links from decrypter, because we do not want to
            // do a linkcheck when adding the link ... this would do too much
            // requests.
            this.br.getPage(GrooveShark.LISTEN);

            Browser ajax = br;

            String sid = this.br.getCookie(GrooveShark.LISTEN, "PHPSESSID");
            String country = this.br.getRegex(Pattern.compile("\"country(.*?)}", Pattern.UNICODE_CASE)).getMatch(-1);
            String rawPost = this.getPostParameterString(ajax, LISTEN, "getTokenForSong", country);
            rawPost = rawPost + "\"parameters\":{\"songID\":\"" + downloadLink.getStringProperty("SongID") + "\"," + country + "}}";
            ajax.getHeaders().put("Content-Type", "application/json");
            ajax.postPageRaw(LISTEN + "more.php?getTokenForSong", rawPost);

            String token = ajax.getRegex("Token\":\"(\\w+)\"").getMatch(0);
            final String Name = downloadLink.getStringProperty("Name").replace(" ", "_").replaceAll("\\?", "");
            String dllink = LISTEN + "s/" + Name + "/" + token;

            downloadLink.setUrlDownload(dllink);
            token = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().lastIndexOf("/") + 1);

            this.br.getHeaders().put("Content-Type", "application/json");
            this.br.getHeaders().put("Referer", downloadLink.getDownloadURL());

            handleDownload(downloadLink, country, sid, token);
        } else {
            // direct links..
            // countrystring
            this.br.getPage(GrooveShark.LISTEN);
            final String country = this.br.getRegex(Pattern.compile("\"country(.*?)}", Pattern.UNICODE_CASE)).getMatch(-1);
            // get SongID
            this.br.getHeaders().put("Content-Type", "application/json");
            this.br.getHeaders().put("Referer", downloadLink.getDownloadURL());
            final String sid = this.br.getCookie(GrooveShark.LISTEN, "PHPSESSID");
            final String Token = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().lastIndexOf("/") + 1);
            handleDownload(downloadLink, country, sid, Token);
        }
    }

    private void handleDownload(final DownloadLink downloadLink, final String country, final String sid, final String token) throws IOException, Exception, PluginException {
        String songID = "{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":20100831," + country + ",\"uuid\":\"" + GrooveShark.USERID + "\",\"session\":\"" + sid + "\",\"token\":\"" + this.makeNewRandomizer() + JDHash.getSHA1(sid) + "\"},\"method\":\"getSongFromToken\",\"parameters\":{\"token\":\"" + token + "\"," + country + "}}";
        this.br.postPageRaw(GrooveShark.LISTEN + "more.php?getSongFromToken", songID);
        songID = this.br.getRegex("SongID\":\"(\\d+)\"").getMatch(0);
        // get streamKey
        final String mysecretKey = this.getSecretToken(br, "getStreamKeyFromSongIDEx", JDHash.getMD5(sid), sid);
        this.br.getHeaders().put("Content-Type", "application/json");
        this.br.getHeaders().put("Referer", "http://listen.grooveshark.com/JSQueue.swf?20101203.14");
        this.br.getHeaders().put("x-flash-version", "10,1,53,64");
        String streamKey = "{\"parameters\":{" + country + ",\"prefetch\":true,\"songID\":" + songID + ",\"mobile\":false},\"header\":{\"privacy\":0,\"token\":\"" + mysecretKey + "\",\"session\":\"" + sid + "\"," + country + ",\"uuid\":\"" + GrooveShark.USERID + "\",\"client\":\"jsqueue\",\"clientRevision\":\"20101012.36\"},\"method\":\"getStreamKeyFromSongIDEx\"}";
        this.br.postPageRaw(GrooveShark.COWBELL + "more.php?getStreamKeyFromSongIDEx", streamKey);
        streamKey = "streamKey=" + this.br.getRegex("streamKey\":\"(\\w+)\"").getMatch(0).replace("_", "%5F");
        final String ip = this.br.getRegex("ip\":\"(.*?)\"").getMatch(0);
        final String dllink = "http://" + ip + "/stream.php";

        // JD v0.9.580: manual change Header or Response 400 Bad request
        this.br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        // Chunk: JD stable max. 1 or Chunkerror; JD svn > 1 no Chunkerror
        this.dl = BrowserAdapter.openDownload(this.br, downloadLink, dllink, streamKey, true, 1);
        if (this.dl.getConnection().getContentType().contains("html")) {
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
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
        String url = downloadLink.getDownloadURL();
        if (new Regex(url, "grooveshark\\.com\\/song\\/\\d+").matches()) {

            return AvailableStatus.TRUE;
        } else {
            this.br.getPage(url);
            if (this.br.containsHTML("not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            String[] filenm = br.getRegex("<h1 class=\"song\">(.*?)by\\s+<a href=\".*?\" rel=\"artist parent\" rev=\"child\">(.*?)</a>.*?on.*?<a href=.*? rel=\"album parent\" rev=\"child\">(.*?)</a>.*?</h1>").getRow(0);
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