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

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "grooveshark.com" }, urls = { "http://(listen\\.)?grooveshark\\.com/(#/)?((album|artist|playlist|s|user)/.*?/([a-zA-z0-9]+|\\d+)(/music/favorites|/similar)?|popular)" }, flags = { 0 })
public class GrvShrkCm extends PluginForDecrypt {

    private static final String     LISTEN = "http://grooveshark.com/";
    private static final String     USERID = UUID.randomUUID().toString().toUpperCase();
    private String                  userID;
    private String                  userName;
    private ArrayList<DownloadLink> decryptedLinks;

    public GrvShrkCm(final PluginWrapper wrapper) {
        super(wrapper);
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
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replaceAll("\\?src=\\d+", "");
        if (parameter.endsWith("similar")) {
            logger.warning("Link format is not supported: " + parameter);
            return null;
        }
        if (parameter.contains("http://listen.")) {
            parameter = parameter.replace("listen.", "");
        }
        /* single */
        if (parameter.contains("/s/")) {
            parameter = parameter.replaceFirst("grooveshark\\.com", "grooveshark.viajd");
            parameter = parameter.replace("#/", "");
            final DownloadLink dlLink = createDownloadlink(parameter);
            dlLink.setProperty("type", "single");
            decryptedLinks.add(dlLink);
            return decryptedLinks;
        }
        if (!parameter.contains("/#/")) {
            parameter = parameter.replaceFirst(LISTEN, LISTEN + "#/");
        }

        br.getPage(parameter);
        /* favourites */
        if (new Regex(parameter, LISTEN + "#/user/.*?/\\d+/music/favorites").matches()) {
            getFavourites(parameter, progress);
        }

        /* playlists */
        if (new Regex(parameter, LISTEN + "#/playlist/.*?/\\d+").matches()) {
            getPlaylists(parameter, progress);
        }

        /* album/artist/popular */
        if (new Regex(parameter, LISTEN + "#/((album|artist)/.*?/\\d+|popular)").matches()) {
            getAlbum(parameter, progress);
        }

        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private void getAlbum(final String parameter, final ProgressController progress) throws IOException {
        final String country = br.getRegex(Pattern.compile("\"country(.*?)}", Pattern.UNICODE_CASE)).getMatch(-1);
        final HashMap<String, String> titleContent = new HashMap<String, String>();
        if (parameter.contains("artist") || parameter.contains("album") || parameter.contains("popular")) {
            final String ID = parameter.substring(parameter.lastIndexOf("/") + 1);
            String method = new Regex(parameter, "#/(.*?)/").getMatch(0);
            if (method == null) {
                method = "popular";
            }
            final String parameterMethod = method;
            method = method.equals("artist") ? method + "GetSongsEx" : method + "GetSongs";
            final String rawPost = getPostParameterString(parameter, method, country);
            Boolean type = false;
            String t1 = null;
            for (int i = 1; i <= 2; i++) {
                String rawPostTrue;
                if (parameterMethod.equals("album")) {
                    rawPostTrue = rawPost + "\"parameters\":{\"" + parameterMethod + "ID\":\"" + ID + "\",\"isVerified\":" + type.toString() + ",\"offset\":0}}";
                } else if (parameterMethod.equals("artist")) {
                    rawPostTrue = rawPost + "\"parameters\":{\"" + parameterMethod + "ID\":\"" + ID + "\",\"isVerifiedOrPopular\":true}}";
                } else {
                    rawPostTrue = rawPost + "\"parameters\":{\"type\":\"daily\"}}";
                    i = 2;
                }
                br.getHeaders().put("Content-Type", "application/json");
                br.postPageRaw(LISTEN + "more.php?" + method, rawPostTrue);
                if (type) {
                    // Regex broken?
                    if (t1 == null || t1.length() == 0) {
                        break;
                    }
                    t1 = t1 + "," + br.getRegex("(result|Songs|songs)\":\\[(.*?)\\]\\}?").getMatch(1);
                    break;
                }
                t1 = br.getRegex("(result|Songs|songs)\":\\[(.*?)(\\]\\}|\\],)").getMatch(1);
                type = true;
            }
            if (t1 != null && t1.length() > 0) {
                t1 = decodeUnicode(t1.replace("#", "-").replace("},{", "}#{"));
                final String[] t2 = t1.split("#");
                progress.setRange(t2.length);
                for (final String t3 : t2) {
                    final String[] line = t3.replace("null", "\"null\"").replace("\",\"", "\"#\"").replaceAll("\\{|\\}", "").split("#");
                    for (final String t4 : line) {
                        final String[] finalline = t4.replace("\":\"", "\"#\"").replace("\"", "").split("#");
                        if (finalline.length < 2) {
                            continue;
                        }
                        titleContent.put(finalline[0], finalline[1]);
                    }
                    final DownloadLink dlLink = createDownloadlink("http://grooveshark.viajd/song/" + titleContent.get("SongID"));
                    for (final Entry<String, String> next : titleContent.entrySet()) {
                        dlLink.setProperty(next.getKey(), next.getValue());
                    }
                    final String filename = dlLink.getProperty("ArtistName", "UnknownArtist") + " - " + dlLink.getProperty("AlbumName", "UnkownAlbum") + " - " + dlLink.getProperty("Name", "UnknownSong") + ".mp3";
                    dlLink.setName(decodeUnicode(filename.trim()));
                    try {
                        dlLink.setDownloadSize(Integer.parseInt(dlLink.getStringProperty("EstimateDuration")) * (128 / 8) * 1024);
                    } catch (final Exception e) {
                    }
                    final String ArtistName = titleContent.get("ArtistName");
                    final String AlbumName = titleContent.get("AlbumName");
                    String fpName = ArtistName + "_" + AlbumName;
                    if (method.contains("popular")) {
                        fpName = "Grooveshark daily popular";
                    }
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName.trim());
                    fp.add(dlLink);
                    decryptedLinks.add(dlLink);
                    progress.increase(1);
                    // we do no linkcheck.. so this should be fast
                }
            }
        }
    }

    private void getFavourites(final String parameter, final ProgressController progress) throws IOException {
        userID = new Regex(parameter, "grooveshark.com/#/user/.*?/(\\d+)/").getMatch(0);
        userName = new Regex(parameter, "grooveshark.com/#/user/(.*?)/(\\d+)/").getMatch(0);
        final String country = br.getRegex(Pattern.compile("\"country(.*?)}", Pattern.UNICODE_CASE)).getMatch(-1);
        final String method = "getFavorites";
        final String paramString = getPostParameterString(LISTEN, method, country) + "\"parameters\":{\"userID\":" + userID + ",\"ofWhat\":\"Songs\"}}";
        br.getHeaders().put("Content-Type", "application/json");
        br.postPageRaw(LISTEN + "more.php?" + method, paramString);
        final String result = br.getRegex("\"result\"\\:\\[(.*)\\]").getMatch(0);
        final String[][] songs = new Regex(result, "\\{.*?\\}").getMatches();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(userName + "'s GrooveShark Favourites");
        progress.increase(1);
        for (final String[] s : songs) {
            final HashMap<String, String> ret = new HashMap<String, String>();
            for (final String[] ss : new Regex(s[0], "\"(.*?)\"\\s*:\\s*\"(.*?)\"").getMatches()) {
                ret.put(ss[0], ss[1]);

            }
            final DownloadLink dlLink = createDownloadlink("http://grooveshark.viajd/song/" + ret.get("SongID"));
            for (final Entry<String, String> next : ret.entrySet()) {
                dlLink.setProperty(next.getKey(), next.getValue());
            }
            final String filename = dlLink.getProperty("ArtistName", "UnknownArtist") + " - " + dlLink.getProperty("AlbumName", "UnkownAlbum") + " - " + dlLink.getProperty("Name", "UnknownSong") + ".mp3";
            dlLink.setName(decodeUnicode(filename.trim()));
            try {
                dlLink.setDownloadSize(Integer.parseInt(dlLink.getStringProperty("EstimateDuration")) * (128 / 8) * 1024);
            } catch (final Exception e) {
            }
            decryptedLinks.add(dlLink);
            fp.add(dlLink);
        }
        progress.doFinalize();
    }

    private void getPlaylists(final String parameter, final ProgressController progress) throws IOException {
        final String playlistID = parameter.substring(parameter.lastIndexOf("/") + 1);
        final String country = br.getRegex(Pattern.compile("\"country(.*?)}", Pattern.UNICODE_CASE)).getMatch(-1);
        final String method = "playlistGetSongs";
        final String paramString = getPostParameterString(LISTEN, method, country) + "\"parameters\":{\"playlistID\":" + playlistID + "}}";
        br.getHeaders().put("Content-Type", "application/json");
        br.postPageRaw(LISTEN + "more.php?" + method, paramString);
        final String result = br.getRegex("\"result\"\\:.*?\\[(.*)\\]").getMatch(0);
        final String[][] songs = new Regex(result, "\\{.*?\\}").getMatches();
        // setup fp-Title
        br.getPage(parameter.replace("#/", ""));
        String title = br.getRegex("<title>(.*?)\\s\\|").getMatch(0);
        if (title == null) {
            title = new Regex(parameter, LISTEN + "#/playlist/(.*?)/").getMatch(0);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName("GrooveShark Playlists - " + Encoding.htmlDecode(title));
        progress.increase(1);
        for (final String[] s : songs) {
            final HashMap<String, String> ret = new HashMap<String, String>();
            for (final String[] ss : new Regex(s[0], "\"(.*?)\"\\s*:\\s*\"(.*?)\"").getMatches()) {
                ret.put(ss[0], ss[1]);
            }
            final DownloadLink dlLink = createDownloadlink("http://grooveshark.viajd/song/" + ret.get("SongID"));
            for (final Entry<String, String> next : ret.entrySet()) {
                dlLink.setProperty(next.getKey(), next.getValue());
            }
            final String filename = dlLink.getProperty("ArtistName", "UnknownArtist") + " - " + dlLink.getProperty("AlbumName", "UnkownAlbum") + " - " + dlLink.getProperty("Name", "UnknownSong") + ".mp3";
            dlLink.setName(decodeUnicode(filename.trim()));
            try {
                dlLink.setDownloadSize(Integer.parseInt(dlLink.getStringProperty("EstimateDuration")) * (128 / 8) * 1024);
            } catch (final Exception e) {
            }
            decryptedLinks.add(dlLink);
            fp.add(dlLink);
        }
        progress.doFinalize();
    }

    private String getPostParameterString(final String parameter, final String method, final String country) throws IOException {
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("Referer", parameter);
        final String sid = br.getCookie(parameter, "PHPSESSID");
        final String secretKey = getSecretKey(br, JDHash.getMD5(sid), sid);
        final String str = "{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"20110722\",\"privacy\":0," + country + ",\"uuid\":\"" + USERID + "\",\"session\":\"" + sid + "\",\"token\":\"" + getToken(method, secretKey) + "\"},\"method\":\"" + method + "\",";
        return str;
    }

    private String getSecretKey(final Browser ajax, final String token, final String sid) throws IOException {
        ajax.getHeaders().put("Content-Type", "application/json");
        String secretKey = "{\"parameters\":{\"secretKey\":\"" + token + "\"},\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"20110722.08\",\"session\":\"" + sid + "\",\"uuid\":\"" + USERID + "\"},\"method\":\"getCommunicationToken\"}";
        ajax.postPageRaw("https://grooveshark.com/more.php?getCommunicationToken", secretKey);
        secretKey = ajax.getRegex("result\":\"(.*?)\"").getMatch(0);
        return secretKey;
    }

    private String getToken(final String method, final String secretKey) {
        final String topSecretKey = Encoding.Base64Decode("bmV2ZXJHb25uYUdpdmVZb3VVcA==");// static.a.gs-cdn.net/gs/app.js
        final String lastRandomizer = makeNewRandomizer();
        final String z = lastRandomizer + JDHash.getSHA1(method + ":" + secretKey + ":" + topSecretKey + ":" + lastRandomizer);
        return z;
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

}
