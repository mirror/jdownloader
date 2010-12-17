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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 12797 $", interfaceVersion = 2, names = { "grooveshark.com" }, urls = { "http://listen\\.grooveshark\\.com/(#/)?.+" }, flags = { 0 })
public class GrvShrkCm extends PluginForDecrypt {

    private static final String LISTEN = "http://listen.grooveshark.com/";
    private static final String USERID = UUID.randomUUID().toString().toUpperCase();
    private String              userID;
    private String              userName;

    public GrvShrkCm(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.contains("search/song") || (parameter.endsWith("similar") && parameter.contains("artist"))) {
            this.logger.warning("Link format is not supported: " + parameter);
            return null;
        }

        // single
        if (parameter.contains("/s/")) {
            parameter = parameter.replaceFirst("grooveshark\\.com", "grooveshark.viajd");
            parameter = parameter.replace("#/", "");
            final DownloadLink dlLink = this.createDownloadlink(Encoding.htmlDecode(parameter));
            dlLink.setProperty("type", "single");
            decryptedLinks.add(dlLink);
            return decryptedLinks;
        }

        // favourites
        // login

        br.setDebug(true);
        // this.br.getPage(GrooveShark.LISTEN);
        if (new Regex(param, ".*?listen.grooveshark.com/\\#/user/.*?/\\d+/music/favorites").matches()) { return decryptFavourites(param, progress); }

        // album
        this.br.getPage(parameter);
        final String country = this.br.getRegex(Pattern.compile("\"country(.*?)}", Pattern.UNICODE_CASE)).getMatch(-1);
        final HashMap<String, String> titleContent = new HashMap<String, String>();
        if (parameter.contains("artist") || parameter.contains("album") || parameter.contains("popular")) {
            final String ID = parameter.substring(parameter.lastIndexOf("/") + 1);
            String method = new Regex(parameter, "#/(.*?)/").getMatch(0);
            if (method == null) {
                method = "popular";
            }
            String rawPost = this.getPostParameterString(parameter, method + "GetSongs", country);
            Boolean type = true;
            String t1 = null;
            for (int i = 1; i <= 2; i++) {
                String rawPostTrue;
                if (method.equals("artist") || method.equals("album")) {
                    rawPostTrue = rawPost + "\"parameters\":{\"" + method + "ID\":" + ID + ",\"isVerified\":" + type.toString() + ",\"offset\":0}}";
                } else {
                    rawPostTrue = rawPost + "\"parameters\":{\"type\":\"daily\"}}";
                    i = 2;
                }
                this.br.getHeaders().put("Content-Type", "application/json");
                this.br.postPageRaw(GrvShrkCm.LISTEN + "more.php?" + method + "GetSongs", rawPostTrue);
                if (!type) {
                    if (t1.length() != 0) {
                        t1 = t1.concat(",");
                    }
                    t1 = t1 + this.br.getRegex("songs\":\\[(.*?)\\],\"hasMore").getMatch(0);
                    i = 2;
                } else {
                    t1 = this.br.getRegex("songs\":\\[(.*?)\\](,\"hasMore|}})").getMatch(0);
                    type = false;
                }
            }
            t1 = this.decodeUnicode(t1.replace("#", "-").replace("},{", "}#{"));
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

                final DownloadLink dlLink = this.createDownloadlink("http://grooveshark.viajd/song/" + titleContent.get("SongID"));
                for (Iterator<Entry<String, String>> it = titleContent.entrySet().iterator(); it.hasNext();) {
                    Entry<String, String> next = it.next();
                    dlLink.setProperty(next.getKey(), decodeUnicode(next.getValue()));
                }
                final String filename = dlLink.getProperty("ArtistName", "UnknownArtist") + " - " + dlLink.getProperty("AlbumName", "UnkownAlbum") + " - " + dlLink.getProperty("Name", "UnknownSong") + ".mp3";
                dlLink.setName(filename.trim());
                try {
                    dlLink.setDownloadSize(Integer.parseInt(dlLink.getStringProperty("EstimateDuration")) * (128 / 8) * 1024);
                } catch (Exception e) {
                }

                final String ArtistName = titleContent.get("ArtistName");
                final String AlbumName = titleContent.get("AlbumName");
                String fpName = ArtistName + "_" + AlbumName;
                if (method == "popular") {
                    fpName = "popular_Top_Ten";
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                dlLink.setFilePackage(fp);
                decryptedLinks.add(dlLink);
                progress.increase(1);
                if ((method == "popular") && (decryptedLinks.size() > 9)) {
                    break;
                }
            }
        }

        if (decryptedLinks.size() == 0) {
            this.logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptFavourites(CryptedLink param, ProgressController progress) throws IOException {

        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        userID = new Regex(parameter, "listen.grooveshark.com/#/user/.*?/(\\d+)/").getMatch(0);
        this.userName = new Regex(parameter, "listen.grooveshark.com/#/user/(.*?)/(\\d+)/").getMatch(0);
        this.br.getPage(parameter);
        final String country = this.br.getRegex(Pattern.compile("\"country(.*?)}", Pattern.UNICODE_CASE)).getMatch(-1);
        String method = "getFavorites";
        String paramString = this.getPostParameterString(LISTEN, method, country) + "\"parameters\":{\"userID\":" + this.userID + ",\"ofWhat\":\"Songs\"}}";
        this.br.getHeaders().put("Content-Type", "application/json");
        this.br.postPageRaw(GrvShrkCm.LISTEN + "more.php?" + method, paramString);
        String result = br.getRegex("\"result\"\\:\\[(.*)\\]").getMatch(0);
        String[][] songs = new Regex(result, "\\{.*?\\}").getMatches();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(userName + "'s GrooveShark Favourites");
        progress.increase(1);
        for (String[] s : songs) {
            HashMap<String, String> ret = new HashMap<String, String>();
            for (String[] ss : new Regex(s[0], "\"(.*?)\"\\s*:\\s*\"(.*?)\"").getMatches()) {
                ret.put(ss[0], ss[1]);

            }

            final DownloadLink dlLink = this.createDownloadlink("http://grooveshark.viajd/song/" + ret.get("SongID"));
            for (Iterator<Entry<String, String>> it = ret.entrySet().iterator(); it.hasNext();) {
                Entry<String, String> next = it.next();
                dlLink.setProperty(next.getKey(), decodeUnicode(next.getValue()));
            }
            final String filename = dlLink.getProperty("ArtistName", "UnknownArtist") + " - " + dlLink.getProperty("AlbumName", "UnkownAlbum") + " - " + dlLink.getProperty("Name", "UnknownSong") + ".mp3";
            dlLink.setName(filename.trim());
            try {
                dlLink.setDownloadSize(Integer.parseInt(dlLink.getStringProperty("EstimateDuration")) * (128 / 8) * 1024);
            } catch (Exception e) {
            }

            decryptedLinks.add(dlLink);
            dlLink.setFilePackage(fp);

        }
        return decryptedLinks;

    }

    // private boolean login(Account account) throws IOException {
    // this.br.getPage(LISTEN);
    // final String c = this.br.getRegex(Pattern.compile("\"country(.*?)}",
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
    // final String sid = br.getCookie("http://listen.grooveshark.com/",
    // "PHPSESSID");
    // // https://cowbell.grooveshark.com/more.php?authenticateUser
    //
    // String loginJSon = "{\"parameters\":{\"savePassword\":0,\"password\":\""
    // + account.getPass() + "\",\"username\":\"" + account.getUser() +
    // "\"},\"header\":" + "{\"token\":\"token\"," +
    // "\"clientRevision\":\"20101012.36\"," + "\"client\":\"htmlshark\"," +
    // "\"session\":\"" + sid + "\",\"privacy\":0," + c + ",\"uuid\":\"" +
    // USERID + "\"},\"method\":\"authenticateUser\"}";
    //
    // // this.br.getHeaders().put("x-flash-version", "10,1,53,64");
    // final String token = this.getSecretToken("authenticateUser",
    // JDHash.getMD5(sid), sid);
    //
    // ajax.postPageRaw("https://cowbell.grooveshark.com/more.php?authenticateUser",
    // loginJSon.replace("\"token\":\"token\"", "\"token\":\"" + token + "\""));
    // userID = ajax.getRegex("\"userID\"\\:(\\d+)").getMatch(0);
    // userName = ajax.getRegex("\"username\"\\:\"(.*?)\"").getMatch(0);
    // isPremium =
    // "1".equals(ajax.getRegex("\"isPremium\"\\:\"(\\d)\"").getMatch(0));
    // authCode = ajax.getRegex("\"authToken\"\\:\"(.*?)\"").getMatch(0);
    // return userID != null;
    // }

    private String getPostParameterString(final String parameter, final String method, final String country) throws IOException {
        this.br.getHeaders().put("Content-Type", "application/json");
        this.br.getHeaders().put("Referer", parameter);
        final String sid = this.br.getCookie(parameter, "PHPSESSID");
        final String token = this.getSecretToken(method, JDHash.getMD5(sid), sid);
        final String str = "{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":20100831," + country + ",\"uuid\":\"" + GrvShrkCm.USERID + "\",\"session\":\"" + sid + "\",\"token\":\"" + token + "\"},\"method\":\"" + method + "\",";
        return str;
    }

    private String getSecretToken(final String method, final String token, final String sid) throws IOException {
        this.br.getHeaders().put("Content-Type", "application/json");
        String secretKey = "{\"parameters\":{\"secretKey\":\"" + token + "\"},\"header\":{\"client\":\"jsqueue\",\"clientRevision\":\"20100831.08\",\"session\":\"" + sid + "\",\"uuid\":\"" + GrvShrkCm.USERID + "\"},\"method\":\"getCommunicationToken\"}";
        this.br.postPageRaw("https://cowbell.grooveshark.com/service.php", secretKey);
        secretKey = this.br.getRegex("result\":\"(.*?)\"").getMatch(0);
        final String lastRandomizer = this.makeNewRandomizer();
        final String z = lastRandomizer + JDHash.getSHA1(method + ":" + secretKey + ":quitStealinMahShit:" + lastRandomizer);
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

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }
}