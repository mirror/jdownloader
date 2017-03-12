//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.utils.JDHexUtils;
import jd.utils.locale.JDL;

import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "trilulilu.ro" }, urls = { "http://(www\\.)?trilulilu\\.ro/(?!video|canal|profil|artist|embed|grup|[^<>\"/]+/(fisiere|profil).+)[A-Za-z0-9_\\-]+/[A-Za-z0-9_\\-]+" })
public class TriLuLiLuRo extends antiDDoSForHost {

    private String              dllink                     = null;
    private static Object       LOCK                       = new Object();
    private static final String VIDEOPLAYER                = "<meta property=\"og:video\"";
    private static final String LIMITREACHED               = ">Ai atins limita de 5 ascultări de piese audio pe zi. Te rugăm să intri in cont ca să poţi";
    private static final String COUNTRYBLOCK               = "\t+Fişierul nu este disponibil pentru vizionare în ţara dumneavoastră";
    private static final String COUNTRYBLOCKUSERTEXT       = JDL.L("plugins.hoster.triluliluro.country", "This file is not downloadable in your country.");
    private static final String ONLYFORREGISTEREDUSERS     = "\t+Trebuie să.*?intri în cont</a> ca să poţi accesa acest fişier";
    private static final String ONLYFORREGISTEREDUSERSTEXT = JDL.L("plugins.hoster.triluliluro.user", "This file is only downloadable for registered users!");
    private String              NONSTANDARDMD5             = null;

    // private static final String INVALIDLINKS =
    // "http://(www\\.)?trilulilu\\.ro/((video|canal|profil|artist|grup).+|[^<>\"/]+/(fisiere|profil).+)";
    private static final String TYPE_IMAGE                 = "http://(www\\.)?trilulilu\\.ro/imagini\\-.+";
    private static final String TYPE_MUSIC                 = "http://(www\\.)?trilulilu\\.ro/muzica\\-.+";

    public TriLuLiLuRo(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.trilulilu.ro");
        if (!"ro".equalsIgnoreCase(System.getProperty("user.language"))) {
            setConfigElements();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        // THIS TYPE OF FUNCTION WITHIN A HOSTER PLUGIN, PREVENTS DEEP ANALYSE FROM WORKING! BEST NOT TO USE IT!
        // if (downloadLink.getDownloadURL().matches(INVALIDLINKS)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        br.setFollowRedirects(true);
        br.addAllowedResponseCodes(500);
        getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Link offline
        if (br.getURL().equals("http://www.trilulilu.ro/") || br.getURL().contains("no_file") || br.getURL().contains("ref=404") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(COUNTRYBLOCK)) {
            // try {
            // localProxy(true);
            // } catch (final Throwable e) {
            // /* does not exist in 09581 */
            // }
            downloadLink.getLinkStatus().setStatusText(COUNTRYBLOCKUSERTEXT);
            return AvailableStatus.TRUE;
        }
        if (br.containsHTML("(Fişierul căutat nu există|Contul acestui utilizator a fost dezactivat|>Acest fişier nu mai este disponibil)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Invalid link
        if (br.containsHTML(">Trilulilu 404<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(LIMITREACHED)) {
            return AvailableStatus.TRUE;
        }
        String filename = null;
        if (downloadLink.getDownloadURL().matches(TYPE_IMAGE) || br.getURL().matches(TYPE_IMAGE)) {
            filename = br.getRegex("<title>([^<>\"]*?)\\- [\\w\\s-]+</title>").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = br.getRegex("<div class=\"img_player\">[\t\n\r ]+<img src=\"(http://[^<>\"]*?\\.jpg)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<img id=\"player_img\" src=\"(http://[^<>\"]*?\\.jpg)").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink != null) {
                dllink += "?size=original";
            }
            filename = Encoding.htmlDecode(filename.trim()) + ".jpg";
            downloadLink.setFinalFileName(filename);
        } else if (isTypeMusic(downloadLink)) {
            filename = br.getRegex("<meta name=\"title\" content=\"([^<>\"]*?)\\- Muzică.*? \\- Trilulilu\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?)\\s*-\\s*Muzică\\s*-\\s*Trilulilu</title>").getMatch(0);
            }
            filename = br.getRegex("<meta name=\"title\" content=\"([^<>\"]*?)\\- Muzică.*? \\- Trilulilu\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h1[^<>]*>\\s*(.*?)\\s*</h1>").getMatch(0);
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        } else {
            if (br.containsHTML("<title> \\- Trilulilu</title>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<div class=\"file_description floatLeft\">[\r\t\n ]+<h1>(.*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<meta name=\"title\" content=\"Trilulilu \\- (.*?) - Muzică Diverse\" />").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<title>Trilulilu \\- (.*?) - Muzică Diverse</title>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("<div class=\"music_demo\">[\n\t\r ]+<h3>(.*?)\\.mp3 \\(demo 30 de secunde\\)</h3").getMatch(0);
                        if (filename == null) {
                            filename = br.getRegex("<div class=\"hentry\">[\t\n\r ]+<h1>(.*?)</h1>").getMatch(0);
                        }
                    }
                }
            }
            if (filename == null) {
                filename = br.getRegex("property=\"og:title\"[\t\n\r ]+content=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename.trim());
            if (br.containsHTML(VIDEOPLAYER)) {
                downloadLink.setFinalFileName(filename + ".mp4");
            } else {
                downloadLink.setFinalFileName(filename + ".mp3");
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean isTypeMusic(final DownloadLink dl) {
        return (dl.getDownloadURL().matches(TYPE_MUSIC) || br.containsHTML("\"isMP3\":\"true\""));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Free Account");
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.trilulilu.ro/termeni-conditii";
    }

    @Override
    public String getDescription() {
        return "JDownloader's Trilulilu Plugin helps downloading content from trilulilu.ro. Please set your own proxy server here.";
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            try {
                setBrowserExclusive();
                br.setFollowRedirects(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(br.getHost(), key, value);
                        }
                        return;
                    }
                }
                String u = account.getUser();
                String p = account.getPass();
                String s = " + ";

                br.getPage("http://www.trilulilu.ro/login");
                Form loginform = br.getFormbyProperty("id", "login_form");
                String n = loginform.getRegex("<input id=\"login_nonce\" type=\"hidden\" value=\"([0-9a-f]+)\"").getMatch(0);

                String ditati = br.getRegex("<script src=\"(http://static\\.trilulilu\\.ro/\\w+/ditati\\.js\\?\\d+)\"").getMatch(0);
                Browser md5fn = br.cloneBrowser();
                md5fn.getPage(ditati == null ? "http://static.trilulilu.ro/compiled/ditati.js" : ditati);

                NONSTANDARDMD5 = md5fn.getRegex("(var hexcase.*?)String\\.prototype\\.htmlentities").getMatch(0);
                if (n == null || NONSTANDARDMD5 == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }

                String postdata = "action=login";
                postdata += "&username=" + u;
                postdata += "&remember=0";
                postdata += "&PAS5WORD=" + md5hex(p + md5hex(u) + s + n + s);
                postdata += "&PAS5W0RD=" + md5hex(p + s + md5hex(u + s + n));
                postdata += "&PA55WORD=" + md5hex(s + md5hex(n + u) + s + p);
                postdata += "&PASSW0RD=" + md5hex(md5hex(p) + s + n + s + u);
                postdata += "&PA5SW0RD=" + md5hex(md5hex(s + u + p) + s + n);
                postdata += "&PA5SWORD=" + md5hex(u + s + md5hex(n + s + p));
                postdata += "&PA55W0RD=" + md5hex(n + md5hex(s + p + u + s));
                postdata += "&PASSWORD=" + n;

                br.postPage("/login", postdata);
                if (!br.containsHTML("ok")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(br.getHost());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    private String md5hex(String s) {
        Object result = new Object();
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(NONSTANDARDMD5);
            result = inv.invokeFunction("hex_md5", s);
        } catch (final Throwable e) {
            return null;
        }
        return result != null ? result.toString() : "";
    }

    private void getDownloadUrl(DownloadLink downloadLink) throws PluginException, IOException {
        Browser br2 = br.cloneBrowser();
        dllink = br.getRegex("<param name=\"flashvars\" value=\"song=(http.*?\\.mp3)").getMatch(0);
        if (dllink == null) {
            String sig = null, exp = null;
            String server = br.getRegex("server\":\"(\\d+)\"").getMatch(0);
            String hash = br.getRegex("\"hash\":\"(.*?)\"").getMatch(0);
            String username = br.getRegex("\"file_username\" value=\"(.*?)\"").getMatch(0);
            username = username == null ? br.getRegex("\"userid\":\"(.*?)\"").getMatch(0) : username;
            if (username == null || hash == null) {
                logger.warning("username or hash is null!");
                return;
            }
            if (server == null) {
                Browser amf = br.cloneBrowser();
                amf.getHeaders().put("Content-Type", "application/x-amf");
                amf.postPageRaw("/amf", createAMFRequest(username, hash));
                HashMap<String, String> amfResult = new HashMap<String, String>();
                amfResult = AMFParser(amf);
                if (amfResult == null) {
                    return;
                }
                server = amfResult.get("server");
                sig = amfResult.get("sig");
                exp = amfResult.get("exp");
            }
            if (server == null) {
                logger.warning("server is null!");
                return;
            }
            if (br.containsHTML(VIDEOPLAYER)) {
                br2.getPage("http://fs" + server + ".trilulilu.ro/" + hash + "/video-formats2");
                String format = br2.getRegex("<format>(.*?)</format>").getMatch(0);
                if (format == null) {
                    logger.warning("format is null!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (br2.containsHTML("mp4-720p")) {
                    format = "mp4-720p";
                }
                dllink = "http://fs" + server + ".trilulilu.ro/stream.php?type=video&source=site&hash=" + hash + "&username=" + username + "&key=ministhebest&format=" + format + "y&start=";
            } else {
                String key = br.getRegex("key=([a-z0-9]+)\"").getMatch(0);
                key = key == null ? br.getRegex("\"key\":\"([a-z0-9]+)\"").getMatch(0) : key;
                dllink = "http://fs" + server + ".trilulilu.ro/stream.php?type=audio&source=site&hash=" + hash + "&username=" + username + "&key=" + (key == null ? "" : key) + "&sig=" + (sig == null ? "" : sig) + "&exp=" + exp;
            }
        }
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
        }
    }

    private String createAMFRequest(String arg0, String arg1) {
        String data = "0A000000030200";
        data += getHexLength(arg0) + JDHexUtils.getHexString(arg0) + "0200";
        data += getHexLength(arg1) + JDHexUtils.getHexString(arg1) + "020004706C6179";
        return JDHexUtils.toString("000300000001000F66696C652E617564696F5F696E666F00022F31000000" + getHexLength(JDHexUtils.toString(data)) + data);
    }

    private String getHexLength(final String s) {
        String result = Integer.toHexString(s.length());
        return result.length() % 2 > 0 ? "0" + result : result;
    }

    private HashMap<String, String> AMFParser(final Browser amf) {
        /* Parsing key/value pairs from binary amf0 response message to HashMap */
        String t = amf.toString();
        /* workaround for browser in stable version */
        t = t.replaceAll("\r\n", "\n");
        char[] content = null;
        try {
            content = t.toCharArray();
        } catch (Throwable e) {
            return null;
        }
        HashMap<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < content.length; i++) {
            if (content[i] != 3) {
                continue;// Object 0x03
            }
            i = i + 2;
            for (int j = i; j < content.length; j++) {
                int keyLen = content[j];
                if (keyLen == 0 || keyLen + j >= content.length) {
                    i = content.length;
                    break;
                }
                String key = "";
                int k;
                for (k = 1; k <= keyLen; k++) {
                    key = key + content[k + j];
                }
                String value = "";
                int v = j + k;
                int vv = 0;
                if (content[v] == 2) {// String 0x02
                    v = v + 2;
                    int valueLen = content[v];
                    if (valueLen == 0) {
                        value = null;
                    }
                    for (vv = 1; vv <= valueLen; vv++) {
                        value = value + content[v + vv];
                    }
                } else if (content[v] == 0) {// Number 0x00
                    String r;
                    for (vv = 1; vv <= 8; vv++) {
                        r = Integer.toHexString(content[v + vv]);
                        r = r.length() % 2 > 0 ? "0" + r : r;
                        value = value + r;
                    }
                    /*
                     * Encoded as 64-bit double precision floating point number IEEE 754 standard
                     */
                    value = value != null ? String.valueOf((int) Double.longBitsToDouble(new BigInteger(value, 16).longValue())) : value;
                } else {
                    continue;
                }
                j = v + vv;
                result.put(key, value);
            }
        }
        return result;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doDownload(downloadLink);
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account, false);
        if (br.containsHTML(ONLYFORREGISTEREDUSERS)) {
            br.getPage(downloadLink.getDownloadURL());
        }
        doDownload(downloadLink);
    }

    private void doDownload(DownloadLink downloadLink) throws Exception {
        if (br.containsHTML(LIMITREACHED)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        if (br.containsHTML(COUNTRYBLOCK)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, COUNTRYBLOCKUSERTEXT);
        }
        if (br.containsHTML(ONLYFORREGISTEREDUSERS)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, ONLYFORREGISTEREDUSERSTEXT);
        }
        if (downloadLink.getDownloadURL().matches(TYPE_IMAGE) || br.getURL().matches(TYPE_IMAGE)) {
        } else {
            getDownloadUrl(downloadLink);
        }
        if (isTypeMusic(downloadLink)) {
            String embed = br.getRegex("og:audio\" content=\"(http://[^<>\"]*?/audio-[^<>\"]*?)\"").getMatch(0);
            getPage(embed);
            dllink = br.getRegex("file: \"([^<>\"]+)\"").getMatch(0);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            try {
                con = openConnection(br2, dllink);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        int maxchunks = -2;
        // Videos have chunk-limits!
        if (br.containsHTML(VIDEOPLAYER)) {
            maxchunks = 1;
        }
        br.getHeaders().put("Accept-Charset", null);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
        try {
            localProxy(false);
        } catch (final Throwable e) {
            /* does not exist in 09581 */
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private void localProxy(final boolean b) {
        if (getPluginConfig().getBooleanProperty("STATUS")) {
            String server = getPluginConfig().getStringProperty("PROXYSERVER", null);
            int port = getPluginConfig().getIntegerProperty("PROXYPORT", -1);
            if (isEmpty(server) || port < 0) {
                return;
            }
            server = new Regex(server, "^[0-9a-zA-Z]+://").matches() ? server : "http://" + server;
            final org.appwork.utils.net.httpconnection.HTTPProxy proxy = org.appwork.utils.net.httpconnection.HTTPProxy.parseHTTPProxy(server + ":" + port);
            if (b) {
                if (proxy.getHost() != null || proxy.getHost() != "" && proxy.getPort() > 0) {
                    br.setProxy(proxy);
                    return;
                }
            }
        }
        br.setProxy(br.getThreadProxy());
    }

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        return br.openHeadConnection(directlink);
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDL.L("plugins.hoster.trilulilu.configlabel", "Proxy Configuration")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "STATUS", JDL.L("plugins.hoster.trilulilu.status", "Use Proxy-Server?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), "PROXYSERVER", JDL.L("plugins.hoster.trilulilu.proxyhost", "Host:")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), "PROXYPORT", JDL.L("plugins.hoster.trilulilu.proxyport", "Port:")));
    }

}