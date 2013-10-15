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
import java.util.Locale;
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
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "grooveshark.com" }, urls = { "http://(grooveshark\\.viajd/(s/.*?/\\w+|song/\\d+)|[\\w\\-]+\\.grooveshark\\.com/stream\\.php\\?streamKey=\\w+)" }, flags = { 2 })
public class GrooveShark extends PluginForHost {

    public static class SWFDecompressor {

        public SWFDecompressor() {
            super();
        }

        public byte[] decompress(String s) { // ~2000ms
            byte[] buffer = new byte[512];
            InputStream input = null;
            ByteArrayOutputStream result = null;
            byte[] enc = null;

            try {
                URL url = new URL(s);
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
                    } catch (Throwable e) {
                    }
                    try {
                        result.close();
                    } catch (Throwable e2) {
                    }
                    enc = result.toByteArray();
                }
            } catch (Throwable e3) {
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
        private byte[] strip(byte[] bytes) {
            byte[] compressable = new byte[bytes.length - 8];
            System.arraycopy(bytes, 8, compressable, 0, bytes.length - 8);
            return compressable;
        }

        private byte[] uncompress(byte[] b) {
            Inflater decompressor = new Inflater();
            decompressor.setInput(strip(b));
            ByteArrayOutputStream bos = new ByteArrayOutputStream(b.length - 8);
            byte[] buffer = new byte[1024];

            try {
                while (true) {
                    int count = decompressor.inflate(buffer);
                    if (count == 0 && decompressor.finished()) {
                        break;
                    } else if (count == 0) {
                        return null;
                    } else {
                        bos.write(buffer, 0, count);
                    }
                }
            } catch (Throwable t) {
            } finally {
                decompressor.end();
            }

            byte[] swf = new byte[8 + bos.size()];
            System.arraycopy(b, 0, swf, 0, 8);
            System.arraycopy(bos.toByteArray(), 0, swf, 8, bos.size());
            swf[0] = 70; // F
            return swf;
        }

    }

    public static class StringContainer {
        public String string = null;
    }

    public static StringContainer APPJSURL       = new StringContainer();
    public static StringContainer FLASHURL       = new StringContainer();
    public static StringContainer COUNTRY        = new StringContainer();
    public static StringContainer CLIENTREVISION = new StringContainer();

    public static boolean fetchingKeys(Browser br) throws Exception {
        Browser br2 = br.cloneBrowser();
        br2.getPage(APPJSURL.string); // ~2000ms
        String jsKey = br2.getRegex(reqk(3)).getMatch(1);
        SWFDecompressor swfd = new SWFDecompressor();
        byte[] swfdec = swfd.decompress(FLASHURL.string);
        if (swfdec == null || swfdec.length == 0) { return false; }

        for (int i = 0; i < swfdec.length; i++) {
            if (swfdec[i] < 33 || swfdec[i] > 127) {
                swfdec[i] = 35; // #
            }
        }
        String swfStr = new String(swfdec, "UTF8");
        String flashKey = new Regex(swfStr, reqk(0)).getMatch(0);
        String cKey = new Regex(swfStr, reqk(1)).getMatch(0);
        cKey = cKey == null ? flashKey : cKey;
        flashKey = flashKey == null ? cKey : flashKey;
        if (jsKey == null) return false;

        if (flashKey == null) flashKey = "";
        if (cKey == null) cKey = "";

        SubConfiguration cfg = SubConfiguration.getConfig("grooveshark.com");
        boolean jkey = jsKey.equals(cfg.getStringProperty("jskey"));
        boolean fkey = flashKey.equals(cfg.getStringProperty("flashkey")) | "".equals(flashKey);
        boolean ckey = cKey.equals(cfg.getStringProperty("ckey")) | "".equals(cKey);

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

    public static String getClientVersion(Browser br) {
        if (APPJSURL.string == null) return CLIENTREVISION.string;
        Browser appjs = br.cloneBrowser();
        try {
            appjs.setLoadLimit(2 * 1024 * 1024);
        } catch (final Throwable e) {
        }
        try {
            appjs.getPage(APPJSURL.string);
        } catch (Throwable e) {
            /**
             * FIXME Jiaz, ist das ein Browser Bug? --> jd.http.Browser$BrowserException: Content-length too big 10475091999 >= 1048576
             * 
             * Die Datei ist 1.049.526 Bytes groß
             * 
             * Bsp.: http://grooveshark.com/#!/playlist/Rock+n+Roll+Baby/65695650
             * 
             * .bismarck
             */
            e.printStackTrace();
            return null;
        }
        return appjs.getRegex(appjs.getRegex(reqk(2)).getMatch(0) + reqk(4)).getMatch(0);
    }

    public static boolean getFileVersion(Browser br) {
        if (APPJSURL.string == null || FLASHURL.string == null || CLIENTREVISION.string == null) {
            APPJSURL.string = br.getRegex("appUrl = \'(http://.*?/app_\\d+\\.js)\'").getMatch(0);
            CLIENTREVISION.string = getClientVersion(br);
            FLASHURL.string = br.getRegex("type=\"application\\/x\\-shockwave\\-flash\" data=\"\\/?(.*?)\"").getMatch(0);
            FLASHURL.string = FLASHURL.string == null ? null : LISTEN + FLASHURL.string;
            COUNTRY.string = br.getRegex(",(\"country\":\\{.*?\\}),").getMatch(0);
            if (COUNTRY.string == null) COUNTRY.string = "\"country\":{\"ID\":223,\"CC1\":0,\"CC2\":0,\"CC3\":0,\"CC4\":1073741824,\"DMA\":528,\"IPR\":0}";
        }
        if (APPJSURL.string == null || COUNTRY.string == null || FLASHURL.string == null || CLIENTREVISION.string == null) return false;
        return true;
    }

    private static String reqk(int i) {
        String[] s = new String[5];
        s[0] = "ff8cf9faf900cfe71996b4cedc5046fb2b9c72bb0b5fdbce19195c68103a85d65d88ff6e8c5be634224051bbeb271d2fe07f2ecbc1cd6565a873d3b56c5e356f5bf0ce2e5d7dfece25ae2eea5261";// flashkey
        s[1] = "fc8bfbf6fa00cce41b91b799dc0742fd289d75bc0c53db9d1b1d5b691165828759d8fc3b890ae338221150bdef201922e4212dcdc5c76760ac24d3e4";// ckey
        s[2] = "fd8bfba0fa0acde31bc1b799dd5742fd289271b60958d8c71a4c5f6e103d828c5ad9fe3b8c5de16d224754eeeb7c1d75";
        s[3] = "f980ffa0fb50c8b41f9db79bde5443fa2d9470bd0d0fdccc1e125c69163982865adff8688c00e63e251b54efee261c74e07e";// jskey
        s[4] = "f8dcfff1fe0bceb51b90b2cfda5c46fa";
        JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        return JDHexUtils.toString(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(s[i]));
    }

    private String              DLLINK         = null;
    private String              STREAMKEY      = null;
    private String              TOKEN          = "";
    private String              SID            = null;
    private static final String LISTEN         = "http://grooveshark.com/";
    private static final String USERUID        = UUID.randomUUID().toString().toUpperCase(Locale.ENGLISH);
    public static final String  INVALIDTOKEN   = "\\{\"code\":\\d+,\"message\":\"invalid token\"\\}";
    public static final String  BLOCKEDGERMANY = "Der Zugriff auf \"grooveshark.com\" von Deutschland aus ist nicht mehr möglich!";

    public GrooveShark(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2000l + (long) 1000 * (int) Math.round(Math.random() * 3 + Math.random() * 3));
        String timeZone = System.getProperty("user.timezone");
        if (timeZone != null && (timeZone.contains("Berlin") || timeZone.contains("Argentina"))) {
            setConfigElementsforGermanyUsers();
        }
        setConfigElements();
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
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("http://grooveshark\\.viajd/", LISTEN));
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

    private String getPostParameterString(Browser ajax, String method) throws IOException, PluginException {
        return "{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"" + CLIENTREVISION.string + "\",\"privacy\":0," + COUNTRY.string + ",\"uuid\":\"" + USERUID + "\",\"session\":\"" + SID + "\",\"token\":\"" + getToken(method, getSecretKey(ajax)) + "\"},\"method\":\"" + method + "\",";
    }

    private String getSecretKey(Browser ajax) throws IOException, PluginException {
        try {
            ajax.postPageRaw("https://grooveshark.com/" + "more.php?getCommunicationToken", "{\"parameters\":{\"secretKey\":\"" + JDHash.getMD5(SID) + "\"},\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"" + CLIENTREVISION.string + "\",\"session\":\"" + SID + "\",\"uuid\":\"" + USERUID + "\"},\"method\":\"getCommunicationToken\"}");
        } catch (Throwable e) {
            try {
                org.appwork.utils.logging2.LogSource.exception(logger, e);
            } catch (final Throwable e2) {
                /* does not exist in stable */
            }
            String msg = "Der Aufruf von https-Adressen über einen Proxyserver funktioniert in dieser Version nicht, bitte \"JDownloader 2\" verwenden!";
            if (!isStableEnviroment()) {
                msg = "Der aktuell verwendete Proxyserver unterstützt kein https!";
            }
            logger.severe(msg + " " + e);
            throw new PluginException(LinkStatus.ERROR_FATAL, msg);
        }
        return ajax.getRegex("result\":\"(.*?)\"").getMatch(0);
    }

    private String getToken(String method, String secretKey) {
        String topSecretKey = getPluginConfig().getStringProperty("flashkey");// Flash
        if (method.matches("(getSongFromToken|getTokenForSong)")) {
            topSecretKey = getPluginConfig().getStringProperty("jskey");// Javascript
        }
        topSecretKey = topSecretKey != getPluginConfig().getStringProperty("ckey") ? getPluginConfig().getStringProperty("ckey") : topSecretKey;
        String lastRandomizer = makeNewRandomizer();
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

    private void handleDownload(DownloadLink downloadLink) throws Exception {
        try {
            gsProxy(false);
        } catch (Throwable e) {
            /* does not exist in 09581 */
        }
        if (STREAMKEY == null && DLLINK == null) {
            br.getHeaders().put("Content-Type", "application/json");
            String secretKey = getSecretKey(br);
            String songID = TOKEN.matches("\\d+") ? TOKEN : null;

            if (songID == null) {
                for (int i = 0; i < 2; i++) {
                    br.getHeaders().put("Content-Type", "application/json");
                    songID = "{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"" + CLIENTREVISION.string + "\",\"privacy\":0," + COUNTRY.string + ",\"uuid\":\"" + USERUID + "\",\"session\":\"" + SID + "\",\"token\":\"" + getToken("getSongFromToken", secretKey) + "\"},\"method\":\"getSongFromToken\",\"parameters\":{\"token\":\"" + TOKEN + "\"," + COUNTRY.string + "}}";
                    br.postPageRaw(LISTEN + "more.php?getSongFromToken", songID);

                    if (i == 0 && br.getRegex(INVALIDTOKEN).matches()) {
                        logger.warning("Existing keys are old, looking for new keys.");
                        if (!fetchingKeys(br)) {
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
            for (int i = 0; i < 2; i++) {
                br.getHeaders().put("Content-Type", "application/json");
                STREAMKEY = "{\"method\":\"getStreamKeyFromSongIDEx\",\"parameters\":{\"songID\":" + songID + ",\"mobile\":false," + COUNTRY.string + ",\"prefetch\":false},\"header\":{\"token\":\"" + getToken("getStreamKeyFromSongIDEx", secretKey) + "\",\"uuid\":\"" + USERUID + "\"," + COUNTRY.string + ",\"privacy\":0,\"client\":\"jsqueue\",\"session\":\"" + SID + "\",\"clientRevision\":\"" + CLIENTREVISION.string + "\"}}";
                br.postPageRaw(LISTEN + "more.php?getStreamKeyFromSongIDEx", STREAMKEY);

                if (i == 0 && br.getRegex(INVALIDTOKEN).matches()) {
                    logger.warning("Existing keys are old, looking for new keys.");
                    if (!fetchingKeys(br)) {
                        break;
                    }
                    logger.info("Found new keys. Retrying...");
                } else {
                    break;
                }
            }
            if (br.getRegex(INVALIDTOKEN).matches()) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

            STREAMKEY = br.getRegex("streamKey\":\"(\\w+)\"").getMatch(0);
            if (STREAMKEY == null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP limit reached. Try reconnect!", 30 * 60 * 1000L); }
            STREAMKEY = "streamKey=" + STREAMKEY.replace("_", "%5F");
            String ip = br.getRegex("ip\":\"(.*?)\"").getMatch(0);
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
        try {
            gsProxy(true);
        } catch (Throwable e) {
            /* does not exist in 09581 */
        }
        br.getPage(LISTEN);
        try {
            gsProxy(false);
        } catch (Throwable e) {
            /* does not exist in 09581 */
        }
        if (isGermanyBlocked()) { throw new PluginException(LinkStatus.ERROR_FATAL, BLOCKEDGERMANY); }
        if (!getFileVersion(br)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        String url = downloadLink.getDownloadURL();
        SID = br.getCookie(LISTEN, "PHPSESSID");
        if (SID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "PHPSESSID is null!");
        if (url.matches(LISTEN + "songXXX/\\d+")) {
            // converts from a virtual link to a real link
            // we pass virtual links from decrypter, because we do not want
            // to do a linkcheck when adding the link ... this would do too
            // much requests.
            Browser ajax = br.cloneBrowser();

            for (int i = 0; i < 2; i++) {
                String rawPost = getPostParameterString(ajax, "getTokenForSong") + "\"parameters\":{\"songID\":\"" + downloadLink.getStringProperty("SongID") + "\"," + COUNTRY.string + "}}";
                ajax.getHeaders().put("Content-Type", "application/json");
                ajax.postPageRaw(LISTEN + "more.php?getTokenForSong", rawPost);
                TOKEN = ajax.getRegex("Token\":\"(\\w+)\"").getMatch(0);
                // valid secret key?
                if (i == 0 && ajax.containsHTML(INVALIDTOKEN)) {
                    logger.warning("Existing keys are old, looking for new keys.");
                    if (!fetchingKeys(ajax)) {
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
            String dllink = LISTEN + "s/" + Name + "/" + TOKEN;
            downloadLink.setUrlDownload(dllink);
        } else if (url.matches(LISTEN + ".*?/\\w+")) {
            // direct links..
            TOKEN = url.substring(url.lastIndexOf("/") + 1);
            if (TOKEN.equals("404")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        } else if (new Regex(url, "stream\\.php\\?streamKey=\\w+").matches()) {
            String[] test = url.split("\\?");
            if (test == null || test.length != 2) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            STREAMKEY = test[1];
            DLLINK = test[0];
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDownload(downloadLink);
    }

    private boolean isGermanyBlocked() {
        if (br.containsHTML("Grooveshark den Zugriff aus Deutschland ein")) {
            logger.warning(BLOCKEDGERMANY);
            return true;
        }
        return false;
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

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        try {
            gsProxy(true);
        } catch (Throwable e) {
            /* does not exist in 09581 */
        }
        String url = downloadLink.getDownloadURL();
        if (url.matches(LISTEN + "song\\/\\d+") || new Regex(url, "stream\\.php\\?streamKey=\\w+").matches()) {
            return AvailableStatus.TRUE;
        } else {
            /* 0.95xx comp */
            url = url.contains("%20") ? url.replace("%20", "+") : url;

            br.getPage(url);
            if (br.containsHTML("not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (isGermanyBlocked()) throw new PluginException(LinkStatus.ERROR_FATAL, BLOCKEDGERMANY);
            String[] filenm = br.getRegex("<span itemprop=\"name\">([^<]+)</span>").getColumn(0);
            if (filenm == null || filenm.length != 3) filenm = br.getRegex("<h1>(.*?)\\sby\\s(.*?)</h1>[\r\n\rb]+<h2>on\\s([^<]+)</h2>").getRow(0);
            if (filenm == null || filenm.length != 3) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = filenm[1].trim() + " - " + filenm[2].trim() + " - " + filenm[0].trim() + ".mp3";
            if (filename != null) downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    public static boolean isStableEnviroment() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        final int rev = Integer.parseInt(prev);
        if (rev < 10000) { return true; }
        return false;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "TITLENUMBERING", JDL.L("plugins.hoster.grooveshark.titlenumbering", "Continuous numbering of titles")).setDefaultValue(false));
    }

    private void setConfigElementsforGermanyUsers() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDL.L("plugins.hoster.grooveshark.configlabel", "Proxy Configuration")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "STATUS", JDL.L("plugins.hoster.grooveshark.status", "Use Proxy-Server?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), "PROXYSERVER", JDL.L("plugins.hoster.grooveshark.proxyhost", "Host:")).setDefaultValue("proxy.personalitycores.com"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), "PROXYPORT", JDL.L("plugins.hoster.grooveshark.proxyport", "Port:")).setDefaultValue("8000"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
    }

}