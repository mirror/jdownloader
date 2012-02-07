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

package jd.plugins.hoster;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "grooveshark.com" }, urls = { "http://(grooveshark\\.viajd/(s/.*?/\\w+|song/\\d+)|[\\w\\-]+\\.grooveshark\\.com/stream\\.php\\?streamKey=\\w+)" }, flags = { 2 })
public class GrooveShark extends PluginForHost {

    public static class SWFDecompressor {

        public SWFDecompressor() {
            super();
        }

        public byte[] decompress(final String s) { // ~2000ms
            final byte[] buffer = new byte[512];
            InputStream input = null;
            ByteArrayOutputStream result = null;
            byte[] enc = null;

            try {
                final URL url = new URL(s);
                input = url.openStream(); // ~500ms
                result = new ByteArrayOutputStream();

                try {
                    int amount;
                    while ((amount = input.read(buffer)) != -1) { // ~1500ms
                        result.write(buffer, 0, amount);
                    }
                } finally {
                    try {
                        input.close();
                    } catch (final Throwable e) {
                    }
                    try {
                        result.close();
                    } catch (final Throwable e2) {
                    }
                    enc = result.toByteArray();
                }
            } catch (final Throwable e3) {
                return null;
            }
            return uncompress(enc);
        }

        /**
         * Strips the uncompressed header bytes from a swf file byte array
         * 
         * @param bytes
         *            of the swf
         * @return bytes array minus the uncompressed header bytes
         */
        private byte[] strip(final byte[] bytes) {
            final byte[] compressable = new byte[bytes.length - 8];
            System.arraycopy(bytes, 8, compressable, 0, bytes.length - 8);
            return compressable;
        }

        private byte[] uncompress(final byte[] b) {
            final Inflater decompressor = new Inflater();
            decompressor.setInput(strip(b));
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(b.length - 8);
            final byte[] buffer = new byte[1024];

            try {
                while (true) {
                    final int count = decompressor.inflate(buffer);
                    if (count == 0 && decompressor.finished()) {
                        break;
                    } else if (count == 0) {
                        return null;
                    } else {
                        bos.write(buffer, 0, count);
                    }
                }
            } catch (final Throwable t) {
            } finally {
                decompressor.end();
            }

            final byte[] swf = new byte[8 + bos.size()];
            System.arraycopy(b, 0, swf, 0, 8);
            System.arraycopy(bos.toByteArray(), 0, swf, 8, bos.size());
            swf[0] = 70; // F
            return swf;
        }

    }

    public static String APPJSURL;
    public static String FLASHURL;
    public static String COUNTRY;
    public static String CLIENTREVISION;

    public static boolean fetchingKeys(final Browser br, final String jurl, final String furl) throws Exception {
        final Browser br2 = br.cloneBrowser();
        br2.getPage(jurl); // ~2000ms
        final String jsKey = br2.getRegex("revToken:\"(.*?)\"").getMatch(0);
        final SWFDecompressor swfd = new SWFDecompressor();
        final byte[] swfdec = swfd.decompress(furl);
        if (swfdec == null || swfdec.length == 0) { return false; }

        final StringBuffer sb = new StringBuffer();
        for (final byte element : swfdec) {
            if (element < 127) {
                if (element > 32) {
                    sb.append((char) element);
                }
            }
        }

        final String flashKey = new Regex(sb.toString(), "DISABLEHTTPS\\?(.*?)[a-z]\\d+,").getMatch(0);
        final String cKey = new Regex(sb.toString(), "fromCharCode(.*?)userForgotPassword").getMatch(0);
        if (jsKey == null || flashKey == null || cKey == null) { return false; }

        final SubConfiguration cfg = SubConfiguration.getConfig("grooveshark.com");
        final boolean jkey = jsKey.equals(cfg.getStringProperty("jskey"));
        final boolean fkey = flashKey.equals(cfg.getStringProperty("flashkey"));
        final boolean ckey = flashKey.equals(cfg.getStringProperty("ckey"));

        if (!jkey || !fkey || !ckey) {
            if (!jkey) {
                cfg.setProperty("jskey", jsKey);
            }
            if (!fkey) {
                cfg.setProperty("flashkey", flashKey);
            }
            if (!ckey) {
                cfg.setProperty("ckey", cKey);
            }
            cfg.save();
        }
        return true;
    }

    public static boolean getClientVersion(final Browser br) {
        APPJSURL = br.getRegex("app\\.src = \'(http://.*?/app\\.js\\?[0-9\\.]+)\'").getMatch(0);
        CLIENTREVISION = APPJSURL == null ? null : new Regex(APPJSURL, "\\?(\\d+)\\.").getMatch(0);
        FLASHURL = br.getRegex("type=\"application\\/x\\-shockwave\\-flash\" data=\"\\/?(.*?)\"").getMatch(0);
        CLIENTREVISION = CLIENTREVISION == null ? new Regex(FLASHURL, "\\?(\\d+)\\.").getMatch(0) : CLIENTREVISION;
        FLASHURL = FLASHURL == null ? null : LISTEN + FLASHURL;
        COUNTRY = br.getRegex(",(\"country\":\\{.*?\\}),").getMatch(0);
        if (APPJSURL == null || FLASHURL == null || CLIENTREVISION == null) {
            CLIENTREVISION = br.getRegex("/gs/core\\.js\\?(\\d+)\\.").getMatch(0);
            if (CLIENTREVISION == null) {
                CLIENTREVISION = br.getRegex("/themes/themes\\.js\\?(\\d+)\\.").getMatch(0);
                if (CLIENTREVISION == null) {
                    CLIENTREVISION = br.getRegex("uri\\.css\\?(\\d+)\\.").getMatch(0);
                    if (CLIENTREVISION == null) {
                        CLIENTREVISION = br.getRegex("\"coreVersion\":(\\d+)\\.").getMatch(0);
                        if (CLIENTREVISION == null) { return false; }
                    }
                }
            }
            APPJSURL = APPJSURL == null ? "http://static.a.gs-cdn.net/gs/app.js?" + CLIENTREVISION + ".03" : APPJSURL;
            FLASHURL = FLASHURL == null ? LISTEN + "JSQueue.swf?" + CLIENTREVISION + ".01" : FLASHURL;
        }
        if (COUNTRY == null) { return false; }
        return true;
    }

    private String              DLLINK         = null;
    private String              STREAMKEY      = null;
    private String              TOKEN          = "";
    private static final String LISTEN         = "http://grooveshark.com/";
    private static final String USERUID        = UUID.randomUUID().toString().toUpperCase();
    public static String        INVALIDTOKEN   = "\\{\"code\":\\d+,\"message\":\"invalid token\"\\}";
    public static String        BLOCKEDGERMANY = "Der Zugriff auf \"grooveshark.com\" von Deutschland aus ist nicht mehr möglich!";

    public GrooveShark(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2000l + (long) 1000 * (int) Math.round(Math.random() * 3 + Math.random() * 3));
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            setConfigElements();
        }
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
        link.setUrlDownload(link.getDownloadURL().replaceAll("http://grooveshark\\.viajd/", LISTEN));
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
    public String getDescription() {
        return "JDownloader's GrooveShark Plugin helps downloading AudioClips from grooveshark.com. Please set your own proxy server here. It's only needed for german users!";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String getPostParameterString(final Browser ajax, final String method, final String country, final String sid) throws IOException {
        return "{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"" + CLIENTREVISION + "\",\"privacy\":0," + country + ",\"uuid\":\"" + USERUID + "\",\"session\":\"" + sid + "\",\"token\":\"" + getToken(method, getSecretKey(ajax, JDHash.getMD5(sid), sid)) + "\"},\"method\":\"" + method + "\",";
    }

    private String getSecretKey(final Browser ajax, final String token, final String sid) throws IOException {
        ajax.postPageRaw("https://grooveshark.com/" + "more.php?getCommunicationToken", "{\"parameters\":{\"secretKey\":\"" + token + "\"},\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"" + CLIENTREVISION + "\",\"session\":\"" + sid + "\",\"uuid\":\"" + USERUID + "\"},\"method\":\"getCommunicationToken\"}");
        return ajax.getRegex("result\":\"(.*?)\"").getMatch(0);
    }

    private String getToken(final String method, final String secretKey) {
        String topSecretKey = getPluginConfig().getStringProperty("flashkey");// Flash
        if (method.matches("(getSongFromToken|getTokenForSong)")) {
            topSecretKey = getPluginConfig().getStringProperty("jskey");// Javascript
        }
        topSecretKey = topSecretKey != getPluginConfig().getStringProperty("ckey") ? getPluginConfig().getStringProperty("ckey") : topSecretKey;
        final String lastRandomizer = makeNewRandomizer();
        return lastRandomizer + JDHash.getSHA1(method + ":" + secretKey + ":" + topSecretKey + ":" + lastRandomizer);
    }

    private void gsProxy(final boolean b) {
        if (getPluginConfig().getBooleanProperty("STATUS") && "de".equalsIgnoreCase(System.getProperty("user.language"))) {
            /** FIXME Hi Jiaz! Siehe GS Decrypter */
            final org.appwork.utils.net.httpconnection.HTTPProxy proxy = org.appwork.utils.net.httpconnection.HTTPProxy.parseHTTPProxy("http://" + getPluginConfig().getStringProperty("PROXYSERVER") + ":" + getPluginConfig().getIntegerProperty("PROXYPORT"));
            if (b) {
                if (proxy.getHost() != null || proxy.getHost() != "" && proxy.getPort() > 0) {
                    br.setProxy(proxy);
                }
            } else {
                /*
                 * use null, so the plugin uses global set proxy again, setting
                 * it to none will disable global proxy if set
                 */
                br.setProxy(null);
            }
        }
    }

    private void handleDownload(final DownloadLink downloadLink, final String sid) throws IOException, Exception, PluginException {
        try {
            gsProxy(false);
        } catch (final Throwable e) {
            /* does not exist in 09581 */
        }
        if (STREAMKEY == null && DLLINK == null) {
            br.getHeaders().put("Content-Type", "application/json");
            final String secretKey = getSecretKey(br, JDHash.getMD5(sid), sid);
            String songID = TOKEN.matches("\\d+") ? TOKEN : null;

            if (songID == null) {
                for (int i = 0; i < 2; i++) {
                    br.getHeaders().put("Content-Type", "application/json");
                    songID = "{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"" + CLIENTREVISION + "\",\"privacy\":0," + COUNTRY + ",\"uuid\":\"" + USERUID + "\",\"session\":\"" + sid + "\",\"token\":\"" + getToken("getSongFromToken", secretKey) + "\"},\"method\":\"getSongFromToken\",\"parameters\":{\"token\":\"" + TOKEN + "\"," + COUNTRY + "}}";
                    br.postPageRaw(LISTEN + "more.php?getSongFromToken", songID);

                    if (i == 0 && br.getRegex(INVALIDTOKEN).matches()) {
                        logger.warning("Existing keys are old, looking for new keys.");
                        if (!fetchingKeys(br, APPJSURL, FLASHURL)) {
                            break;
                        }
                        logger.info("Found new keys. Retrying...");
                    } else {
                        break;
                    }
                }
                if (br.getRegex(INVALIDTOKEN).matches()) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            }
            songID = songID.matches("\\d+") ? songID : br.getRegex("SongID\":\"(\\d+)\"").getMatch(0);
            // get streamKey
            br.getHeaders().put("Content-Type", "application/json");
            STREAMKEY = "{\"method\":\"getStreamKeyFromSongIDEx\",\"parameters\":{\"songID\":" + songID + ",\"mobile\":false," + COUNTRY + ",\"prefetch\":false},\"header\":{\"token\":\"" + getToken("getStreamKeyFromSongIDEx", secretKey) + "\",\"uuid\":\"" + USERUID + "\"," + COUNTRY + ",\"privacy\":0,\"client\":\"jsqueue\",\"session\":\"" + sid + "\",\"clientRevision\":\"" + CLIENTREVISION + ".32\"}}";
            br.postPageRaw(LISTEN + "more.php?getStreamKeyFromSongIDEx", STREAMKEY);
            STREAMKEY = br.getRegex("streamKey\":\"(\\w+)\"").getMatch(0);
            if (STREAMKEY == null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 300 * 1000L); }
            STREAMKEY = "streamKey=" + STREAMKEY.replace("_", "%5F");
            final String ip = br.getRegex("ip\":\"(.*?)\"").getMatch(0);
            DLLINK = "http://" + ip + "/stream.php";
        }
        // JD v0.9.580: manual change Header or Response 400 Bad request
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        // Chunk: JD stable max. 1 or Chunkerror; JD svn > 1 no Chunkerror

        dl = BrowserAdapter.openDownload(br, downloadLink, DLLINK, STREAMKEY, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
        try {
            gsProxy(true);
        } catch (final Throwable e) {
            /* does not exist in 09581 */
        }
        br.getPage(LISTEN);
        try {
            gsProxy(false);
        } catch (final Throwable e) {
            /* does not exist in 09581 */
        }
        if (isGermanyBlocked()) { throw new PluginException(LinkStatus.ERROR_FATAL, BLOCKEDGERMANY); }
        if (!getClientVersion(br)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        final String url = downloadLink.getDownloadURL();
        final String sid = br.getCookie(LISTEN, "PHPSESSID");
        if (url.matches(LISTEN + "songXXX/\\d+") && sid != null) {
            // converts from a virtual link to a real link
            // we pass virtual links from decrypter, because we do not want
            // to do a linkcheck when adding the link ... this would do too
            // much requests.
            final Browser ajax = br.cloneBrowser();

            for (int i = 0; i < 2; i++) {
                final String rawPost = getPostParameterString(ajax, "getTokenForSong", COUNTRY, sid) + "\"parameters\":{\"songID\":\"" + downloadLink.getStringProperty("SongID") + "\"," + COUNTRY + "}}";
                ajax.getHeaders().put("Content-Type", "application/json");
                ajax.postPageRaw(LISTEN + "more.php?getTokenForSong", rawPost);
                TOKEN = ajax.getRegex("Token\":\"(\\w+)\"").getMatch(0);
                // valid secret key?
                if (i == 0 && ajax.containsHTML(INVALIDTOKEN)) {
                    logger.warning("Existing keys are old, looking for new keys.");
                    if (!fetchingKeys(ajax, APPJSURL, FLASHURL)) {
                        break;
                    }
                    logger.info("Found new keys. Retrying...");
                } else {
                    break;
                }
            }
            if (ajax.containsHTML(INVALIDTOKEN)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

            // Limitations after ~50 Songs
            if (TOKEN == null) {
                if (ajax.containsHTML("attacker") || ajax.containsHTML("\"Token\":false")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 300 * 1000L); }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String Name = downloadLink.getStringProperty("Name");
            Name = cleanNameForURL(Name);
            final String dllink = LISTEN + "s/" + Name + "/" + TOKEN;
            downloadLink.setUrlDownload(dllink);
        } else if (url.matches(LISTEN + ".*?/\\w+")) {
            // direct links..
            TOKEN = url.substring(url.lastIndexOf("/") + 1);
            if (TOKEN.equals("404")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        } else if (new Regex(url, "stream\\.php\\?streamKey=\\w+").matches()) {
            final String[] test = url.split("\\?");
            if (test == null || test.length != 2) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            STREAMKEY = test[1];
            DLLINK = test[0];
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDownload(downloadLink, sid);
    }

    private boolean isGermanyBlocked() {
        if (br.containsHTML("Grooveshark den Zugriff aus Deutschland ein")) {
            logger.warning(BLOCKEDGERMANY);
            return true;
        }
        return false;
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
        try {
            gsProxy(true);
        } catch (final Throwable e) {
            /* does not exist in 09581 */
        }
        String url = downloadLink.getDownloadURL();
        if (url.matches(LISTEN + "song\\/\\d+") || new Regex(url, "stream\\.php\\?streamKey=\\w+").matches()) {
            return AvailableStatus.TRUE;
        } else {
            /* 0.95xx comp */
            url = url.contains("%20") ? url.replace("%20", "+") : url;

            br.getPage(url);
            if (br.containsHTML("not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (isGermanyBlocked()) { throw new PluginException(LinkStatus.ERROR_FATAL, "Grooveshark: Zugriff aus Deutschland blockiert!"); }
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDL.L("plugins.hoster.grooveshark.configlabel", "Proxy Configuration")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "STATUS", JDL.L("plugins.hoster.grooveshark.status", "Use Proxy-Server?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), "PROXYSERVER", JDL.L("plugins.hoster.grooveshark.proxyhost", "Host:")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), "PROXYPORT", JDL.L("plugins.hoster.grooveshark.proxyport", "Port:")));
    }

}