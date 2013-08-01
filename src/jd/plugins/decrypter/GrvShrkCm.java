//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
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
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "grooveshark.com" }, urls = { "http://((listen|retro)\\.)?grooveshark\\.com/([#!/%21]+)?((album|artist|playlist|s|user)/.*?/([a-zA-z0-9]+|\\d+)(/music/favorites|/similar)?|popular)" }, flags = { 0 })
public class GrvShrkCm extends PluginForDecrypt {

    private String                  LISTEN           = "http://grooveshark.com/";
    private static String           USERUID          = UUID.randomUUID().toString().toUpperCase(Locale.ENGLISH);
    private String                  USERID;
    private String                  USERNAME;
    private ArrayList<DownloadLink> decryptedLinks;
    private static final String     UNSUPPORTEDLINKS = "http://grooveshark\\.com/(/popular|#/playlist/Unknown/.*?)";

    public GrvShrkCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String decodeUnicode(String s) {
        Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    private boolean exists(DownloadLink dl) {
        for (DownloadLink dLink : decryptedLinks) {
            if (dl.getDownloadURL().equals(dLink.getDownloadURL())) return true;
        }
        return false;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        JDUtilities.getPluginForHost("grooveshark.com");
        decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replaceAll("\\?src=\\d+", "");
        if (parameter.endsWith("similar") || parameter.matches(UNSUPPORTEDLINKS)) {
            logger.warning("Link format is not supported: " + parameter);
            return decryptedLinks;
        }
        /* get Clientrevision */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
        try {
            gsProxy(true);
        } catch (Throwable e) {
        }
        br.getPage(LISTEN);
        if (br.containsHTML("Grooveshark den Zugriff aus Deutschland ein")) {
            logger.warning(jd.plugins.hoster.GrooveShark.BLOCKEDGERMANY);
            return decryptedLinks;
        }
        try {
            gsProxy(false);
        } catch (Throwable e) {
        }
        if (!jd.plugins.hoster.GrooveShark.getFileVersion(br)) {
            logger.warning("Failed parsing!");
            return null;
        }

        parameter = parameter.replace("http://listen.", "http://").replace("/#%21/", "/#!/").replaceAll("/#/", "/");
        /* single */
        if (parameter.contains("/s/")) {
            parameter = parameter.replaceFirst(LISTEN, "http://grooveshark.viajd/");
            parameter = parameter.replace("#!/", "");
            DownloadLink dlLink = createDownloadlink(parameter);
            decryptedLinks.add(dlLink);
            return decryptedLinks;
        }
        if (!parameter.contains("/#!/")) {
            parameter = parameter.replaceFirst(LISTEN, LISTEN + "#!/");
        }

        try {
            gsProxy(true);
        } catch (Throwable e) {
        }
        br.getPage(LISTEN);
        if (br.containsHTML("Grooveshark den Zugriff aus Deutschland ein")) {
            logger.warning(jd.plugins.hoster.GrooveShark.BLOCKEDGERMANY);
            return decryptedLinks;
        }
        try {
            gsProxy(false);
        } catch (Throwable e) {
        }

        /* favourites */
        if (parameter.matches(LISTEN + "#!/user/.*?/\\d+/music/favorites")) {
            getFavourites(parameter, progress);
        }

        /* playlists */
        if (parameter.matches(LISTEN + "#!/playlist/.*?/\\d+")) {
            getPlaylists(parameter, progress);
        }

        /* album/artist/popular */
        if (parameter.matches(LISTEN + "#!/((album|artist)/.*?/\\d+|popular)")) {
            getAlbum(parameter, progress);
        }

        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }

        if (getPluginConfig().getBooleanProperty("TITLENUMBERING")) {
            String format = "%0" + String.valueOf(decryptedLinks.size()).length() + "d";
            for (DownloadLink dl : decryptedLinks) {
                String trackNumber = dl.getStringProperty("TrackNum");
                dl.setName(String.format(format, trackNumber == null ? decryptedLinks.indexOf(dl) + 1 : Integer.parseInt(trackNumber)) + "." + dl.getName());
            }
        }

        return decryptedLinks;
    }

    private void getAlbum(String parameter, ProgressController progress) throws Exception {
        HashMap<String, String> titleContent = new HashMap<String, String>();
        if (parameter.contains("/artist") || parameter.contains("/album") || parameter.contains("/popular")) {
            String ID = parameter.substring(parameter.lastIndexOf("/") + 1);
            String method = new Regex(parameter, "#!/(.*?)/").getMatch(0);
            if (method == null) {
                method = "popular";
            }
            String parameterMethod = method;

            if (method.equals("artist")) {
                method += "GetAllSongsEx";
            } else if (method.equals("album")) {
                method += "GetAllSongs";
            } else {
                method += "GetSongs";
            }
            String t1 = null, rawPostTrue;

            String rawPost = getPostParameterString(parameter, method);
            boolean type = false;
            for (int i = 1; i < 3; i++) {
                for (int j = 0; j < 2; j++) {
                    if (parameterMethod.equals("album")) {
                        rawPostTrue = rawPost + "\"parameters\":{\"" + parameterMethod + "ID\":\"" + ID + "\",\"isVerified\":" + Boolean.toString(type) + ",\"offset\":0}}";
                    } else if (parameterMethod.equals("artist")) {
                        rawPostTrue = rawPost + "\"parameters\":{\"" + parameterMethod + "ID\":\"" + ID + "\",\"isVerifiedOrPopular\":true}}";
                    } else {
                        rawPostTrue = rawPost + "\"parameters\":{\"type\":\"daily\"}}";
                        i = 3;
                    }
                    br.getHeaders().put("Content-Type", "application/json");
                    br.postPageRaw(LISTEN + "more.php?" + method, rawPostTrue);

                    if (j == 0 && br.containsHTML(jd.plugins.hoster.GrooveShark.INVALIDTOKEN)) {
                        logger.warning("Existing keys are old, looking for new keys.");
                        if (!jd.plugins.hoster.GrooveShark.fetchingKeys(br)) break;
                        rawPost = getPostParameterString(parameter, method);
                        logger.info("Found new keys. Retrying...");
                    } else {
                        break;
                    }
                }
                if (br.containsHTML(jd.plugins.hoster.GrooveShark.INVALIDTOKEN)) { return; }

                if (type) {
                    // Regex broken?
                    if (t1 == null || t1.length() == 0) { return; }
                    t1 = t1 + "," + br.getRegex("(result|Songs|songs)\":\\[(.*?)\\]\\}?").getMatch(1);
                    break;
                }
                t1 = br.getRegex("(result|Songs|songs)\":\\[(.*?)(\\]\\}|\\],)").getMatch(1);
                type = true;
            }

            t1 = decodeUnicode(t1.replace("#", "-").replace("},{", "}#{"));
            String[] t2 = t1.split("#");
            progress.setRange(t2.length);
            HashMap<String, FilePackage> fpMap = new HashMap<String, FilePackage>();
            String fixedFpName = null;
            for (String t3 : t2) {
                String[] line = t3.replace("null", "\"null\"").replace("\",\"", "\"#\"").replaceAll("\\{|\\}", "").split("#");
                for (String t4 : line) {
                    String[] finalline = t4.replace("\":\"", "\"#\"").replace("\"", "").split("#");
                    if (finalline.length < 2) {
                        continue;
                    }
                    titleContent.put(finalline[0], finalline[1]);
                }
                DownloadLink dlLink = createDownloadlink("http://grooveshark.viajd/song/" + titleContent.get("SongID"));
                for (Entry<String, String> next : titleContent.entrySet()) {
                    dlLink.setProperty(next.getKey(), next.getValue());
                }
                String filename = dlLink.getProperty("ArtistName", "UnknownArtist") + " - " + dlLink.getProperty("AlbumName", "UnkownAlbum") + " - " + dlLink.getProperty("Name", "UnknownSong") + ".mp3";
                dlLink.setName(decodeUnicode(filename.trim()));
                try {
                    dlLink.setDownloadSize(Integer.parseInt(dlLink.getStringProperty("EstimateDuration")) * (128 / 8) * 1024);
                } catch (Exception e) {
                }
                String ArtistName = titleContent.get("ArtistName");
                String AlbumName = titleContent.get("AlbumName");
                String fpName = ArtistName + "_" + AlbumName;
                if (method.contains("popular")) {
                    fpName = "Grooveshark daily popular";
                }
                fpName = fpName.trim();
                if (fixedFpName == null) fixedFpName = fpName;
                FilePackage fp = fpMap.get(fpName != fixedFpName ? fixedFpName : fpName);
                if (fp == null) {
                    fp = FilePackage.getInstance();
                    fp.setName(fpName);
                    fpMap.put(fpName, fp);
                }
                if (!exists(dlLink)) {
                    decryptedLinks.add(dlLink);
                    fp.add(dlLink);
                    progress.increase(1);
                }
                // we do no linkcheck.. so this should be fast
            }
        }
    }

    private void getFavourites(String parameter, ProgressController progress) throws Exception {
        USERID = new Regex(parameter, "/#!/user/.*?/(\\d+)/").getMatch(0);
        USERNAME = new Regex(parameter, "/#!/user/(.*?)/(\\d+)/").getMatch(0);
        String method = "getFavorites";

        for (int j = 0; j < 2; j++) {
            String paramString = getPostParameterString(LISTEN, method) + "\"parameters\":{\"userID\":" + USERID + ",\"ofWhat\":\"Songs\"}}";
            br.getHeaders().put("Content-Type", "application/json");
            br.postPageRaw(LISTEN + "more.php?" + method, paramString);

            if (j == 0 && br.containsHTML(jd.plugins.hoster.GrooveShark.INVALIDTOKEN)) {
                logger.warning("Existing keys are old, looking for new keys.");
                if (!jd.plugins.hoster.GrooveShark.fetchingKeys(br)) break;
                logger.info("Found new keys. Retrying...");
            } else {
                break;
            }
        }
        if (br.containsHTML(jd.plugins.hoster.GrooveShark.INVALIDTOKEN)) { return; }

        String result = br.getRegex("\"result\"\\:\\[(.*)\\]").getMatch(0);
        String[][] songs = new Regex(result, "\\{.*?\\}").getMatches();
        FilePackage fp = FilePackage.getInstance();
        fp.setName(USERNAME + "'s GrooveShark Favourites");
        progress.increase(1);
        for (String[] s : songs) {
            HashMap<String, String> ret = new HashMap<String, String>();
            for (String[] ss : new Regex(s[0], "\"(.*?)\"\\s*:\\s*\"(.*?)\"").getMatches()) {
                ret.put(ss[0], ss[1]);

            }
            DownloadLink dlLink = createDownloadlink("http://grooveshark.viajd/song/" + ret.get("SongID"));
            for (Entry<String, String> next : ret.entrySet()) {
                dlLink.setProperty(next.getKey(), next.getValue());
            }
            String filename = dlLink.getProperty("ArtistName", "UnknownArtist") + " - " + dlLink.getProperty("AlbumName", "UnkownAlbum") + " - " + dlLink.getProperty("Name", "UnknownSong") + ".mp3";
            dlLink.setName(decodeUnicode(filename.trim()));
            try {
                dlLink.setDownloadSize(Integer.parseInt(dlLink.getStringProperty("EstimateDuration")) * (128 / 8) * 1024);
            } catch (Exception e) {
            }
            if (!exists(dlLink)) {
                decryptedLinks.add(dlLink);
                fp.add(dlLink);
            }
        }
        progress.doFinalize();
    }

    private void getPlaylists(String parameter, ProgressController progress) throws Exception {
        String playlistID = parameter.substring(parameter.lastIndexOf("/") + 1);
        String method = "playlistGetSongs";

        for (int j = 0; j < 2; j++) {
            String paramString = getPostParameterString(LISTEN, method) + "\"parameters\":{\"playlistID\":" + playlistID + "}}";
            br.getHeaders().put("Content-Type", "application/json");
            br.postPageRaw(LISTEN + "more.php?" + method, paramString);

            if (j == 0 && br.containsHTML(jd.plugins.hoster.GrooveShark.INVALIDTOKEN)) {
                logger.warning("Existing keys are old, looking for new keys.");
                if (!jd.plugins.hoster.GrooveShark.fetchingKeys(br)) break;
                logger.info("Found new keys. Retrying...");
            } else {
                break;
            }
        }
        if (br.containsHTML(jd.plugins.hoster.GrooveShark.INVALIDTOKEN)) { return; }

        String result = br.getRegex("\"result\"\\:.*?\\[(.*)\\]").getMatch(0);
        String[][] songs = new Regex(result, "\\{.*?\\}").getMatches();
        // setup fp-Title
        try {
            gsProxy(true);
        } catch (Throwable e) {
        }
        br.getPage(parameter);
        try {
            gsProxy(false);
        } catch (Throwable e) {
        }
        String title = br.getRegex("<title>(.*?)(\\||-).*?</title>").getMatch(0);
        title = title.trim();
        if (title == null || "Grooveshark".equals(title)) {
            title = new Regex(parameter, "/playlist/(.*?)/").getMatch(0);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName("GrooveShark Playlists - " + Encoding.htmlDecode(title));
        progress.increase(1);
        for (String[] s : songs) {
            HashMap<String, String> ret = new HashMap<String, String>();
            if (!s[0].endsWith(",")) s[0] = s[0].concat(",");
            for (String[] ss : new Regex(s[0], "\"(.*?)\":\"?(.*?)\"?,").getMatches()) {
                ret.put(ss[0], ss[1]);
            }
            DownloadLink dlLink = createDownloadlink("http://grooveshark.viajd/song/" + ret.get("SongID"));
            for (Entry<String, String> next : ret.entrySet()) {
                dlLink.setProperty(next.getKey(), next.getValue());
            }
            String filename = dlLink.getProperty("ArtistName", "UnknownArtist") + " - " + dlLink.getProperty("AlbumName", "UnkownAlbum") + " - " + dlLink.getProperty("Name", "UnknownSong") + ".mp3";
            dlLink.setName(decodeUnicode(filename.trim()));
            try {
                dlLink.setDownloadSize(Integer.parseInt(dlLink.getStringProperty("EstimateDuration")) * (128 / 8) * 1024);
            } catch (Exception e) {
            }
            if (!exists(dlLink)) {
                decryptedLinks.add(dlLink);
                fp.add(dlLink);
            }
        }
        progress.doFinalize();
    }

    private String getPostParameterString(String parameter, String method) throws IOException {
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("Referer", parameter);
        String sid = br.getCookie(parameter, "PHPSESSID");
        if (sid == null) return null;
        String secretKey = getSecretKey(br, sid);
        return "{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"" + jd.plugins.hoster.GrooveShark.CLIENTREVISION.string + "\",\"privacy\":0," + jd.plugins.hoster.GrooveShark.COUNTRY.string + ",\"uuid\":\"" + USERUID + "\",\"session\":\"" + sid + "\",\"token\":\"" + getToken(method, secretKey) + "\"},\"method\":\"" + method + "\",";
    }

    private String getSecretKey(Browser ajax, String sid) throws IOException {
        try {
            ajax.postPageRaw("https://grooveshark.com/" + "more.php?getCommunicationToken", "{\"parameters\":{\"secretKey\":\"" + JDHash.getMD5(sid) + "\"},\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"" + jd.plugins.hoster.GrooveShark.CLIENTREVISION.string + "\",\"session\":\"" + sid + "\",\"uuid\":\"" + USERUID + "\"},\"method\":\"getCommunicationToken\"}");
        } catch (Throwable e) {
            try {
                org.appwork.utils.logging2.LogSource.exception(logger, e);
            } catch (final Throwable e2) {
                /* does not exist in stable */
            }
            String msg = "Der Aufruf von https-Adressen über einen Proxyserver funktioniert in dieser Version nicht, bitte \"JDownloader 2\" verwenden!";
            if (!jd.plugins.hoster.GrooveShark.isStableEnviroment()) {
                msg = "Der aktuell verwendete Proxyserver unterstützt kein https!";
            }
            logger.severe(msg + " " + e);
        }
        return ajax.getRegex("result\":\"(.*?)\"").getMatch(0);
    }

    private String getToken(String method, String secretKey) {
        SubConfiguration cfg = SubConfiguration.getConfig("grooveshark.com");
        String topSecretKey = cfg.getStringProperty("jskey");
        String lastRandomizer = makeNewRandomizer();
        topSecretKey = topSecretKey != cfg.getStringProperty("ckey") ? cfg.getStringProperty("ckey") : topSecretKey;
        return lastRandomizer + JDHash.getSHA1(method + ":" + secretKey + ":" + topSecretKey + ":" + lastRandomizer);
    }

    private void gsProxy(boolean b) {
        if (getPluginConfig().getBooleanProperty("STATUS")) {
            String server = getPluginConfig().getStringProperty("PROXYSERVER", null);
            int port = getPluginConfig().getIntegerProperty("PROXYPORT", -1);
            if (isEmpty(server) || port < 0) return;
            server = new Regex(server, "^[0-9a-zA-Z]+://").matches() ? server : "http://" + server;
            org.appwork.utils.net.httpconnection.HTTPProxy proxy = org.appwork.utils.net.httpconnection.HTTPProxy.parseHTTPProxy(server + ":" + port);
            if (b && proxy != null && proxy.getHost() != null) {
                br.setProxy(proxy);
                return;
            }
        }
        br.setProxy(br.getThreadProxy());
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    private String makeNewRandomizer() {
        String charList = "0123456789abcdef";
        char[] chArray = new char[6];
        Random random = new Random();
        int i = 0;
        do {
            chArray[i] = charList.toCharArray()[random.nextInt(16)];
            i++;
        } while (i <= 5);
        return new String(chArray);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}