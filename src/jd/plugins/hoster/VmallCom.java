//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookies;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vmall.com" }, urls = { "http://(?:www\\.)?vmalldecrypted\\.com/\\d+" }, flags = { 2 })
public class VmallCom extends PluginForHost {

    public VmallCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE;
    }

    @Override
    public String rewriteHost(String host) {
        if ("dbank.com".equals(getHost())) {
            if (host == null || "dbank.com".equals(host)) {
                return "vmall.com";
            }
        }
        return super.rewriteHost(host);
    }

    private static Object       LOCK     = new Object();
    private static final String MAINPAGE = "http://vmall.com/";

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        String dllink = downloadLink.getStringProperty("mainlink");
        br.getPage(dllink);

        /* Password protected link */
        if (br.getURL().contains("/m_accessPassword.html")) {
            String passCode = null;
            String id = new Regex(br.getURL(), "id=(\\w+)$").getMatch(0);
            id = id == null ? dllink.substring(dllink.lastIndexOf("/") + 1) : id;

            for (int i = 0; i < 3; i++) {

                if (downloadLink.getStringProperty("password", null) == null) {
                    passCode = Plugin.getUserInput(null, downloadLink);
                } else {
                    passCode = downloadLink.getStringProperty("password");
                }

                br.postPage("http://dl.vmall.com/app/encry_resource.php", "id=" + id + "&context=%7B%22pwd%22%3A%22" + passCode + "%22%7D&action=verify");
                if (br.getRegex("\"retcode\":\"0000\"").matches()) {
                    break;
                }
            }
            if (!br.getRegex("\"retcode\":\"0000\"").matches()) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Wrong password!");
            }
            br.getPage(dllink);
        }

        String key = br.getRegex("\"encryKey\":\"([^\"]+)").getMatch(0);
        String downloadurl = null;

        String json = br.getRegex("var globallinkdata = (\\{[^<]+\\});").getMatch(0);
        if (json == null) {
            json = br.getRegex("var globallinkdata = (\\{.*?\\});").getMatch(0);
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        entries = (LinkedHashMap<String, Object>) entries.get("data");
        entries = (LinkedHashMap<String, Object>) entries.get("resource");
        final ArrayList<Object> ressourcelist = (ArrayList) entries.get("files");
        final long thisfid = getLongProperty(downloadLink, "id", -1);
        boolean done = false;
        for (final Object o : ressourcelist) {
            final LinkedHashMap<String, Object> finfomap = (LinkedHashMap<String, Object>) o;
            final String type = (String) finfomap.get("type");
            if (type.equals("File")) {
                final long fid = getLongValue(finfomap.get("id"));
                if (fid == thisfid) {
                    /* get fresh encrypted url string */
                    downloadurl = (String) finfomap.get("downloadurl");
                    done = true;
                    break;
                }
            } else {
                /* Subfolder */
                final ArrayList<Object> ressourcelist_subfolder = (ArrayList) finfomap.get("childList");
                for (final Object filesub : ressourcelist_subfolder) {
                    final LinkedHashMap<String, Object> finfomapsub = (LinkedHashMap<String, Object>) filesub;
                    final long fid = getLongValue(finfomapsub.get("id"));
                    if (fid == thisfid) {
                        /* get fresh encrypted url string */
                        downloadurl = (String) finfomapsub.get("downloadurl");
                        done = true;
                        break;
                    }
                }
            }
            if (done) {
                break;
            }
        }

        /* fail */
        if (downloadurl == null || key == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        downloadLink.setProperty("downloadurl", downloadurl);
        downloadLink.setProperty("encryKey", key);
        return AvailableStatus.TRUE;
    }

    private void doFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getStringProperty("mainlink"));

        String key = downloadLink.getStringProperty("encryKey", null);
        String enc = downloadLink.getStringProperty("downloadurl", null);
        String dllink = decrypt(enc, key);

        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account, false);
        doFree(downloadLink);
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                this.br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /* Always try to re-use cookies to prevent login captcha */
                    this.br.setCookies(this.getHost(), cookies);
                    this.br.getPage("http://www." + this.getHost() + "/");
                    final String cookie_username = this.br.getCookie(MAINPAGE, "name");
                    if (cookie_username != null && !"deleted".equalsIgnoreCase(cookie_username)) {
                        logger.info("Successfully re-used cookies");
                        /* Save new cookie-checked-timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    }
                }
                this.br.getPage("http://www.vmall.com/account/login");
                String postData = "submit=true&loginUrl=https%3A%2F%2Fhwid1.vmall.com%2Foauth2%2Fportal%2Flogin.jsp&service=http%3A%2F%2Fwww.vmall.com%2Faccount%2Fcaslogin&loginChannel=26000000&reqClientType=26&deviceID=&adUrl=&lang=zh-cn&inviterUserID=&inviter=&viewType=0&quickAuth=&userAccount=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember_name=on";
                final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), MAINPAGE, true);
                final String code = getCaptchaCode("https://hwid1.vmall.com/casserver/randomcode", dummyLink);
                postData += "&authcode=" + Encoding.urlEncode(code);

                this.br.postPage("https://hwid1.vmall.com/casserver/remoteLogin", postData);
                if (!"1".equals(br.getCookie(MAINPAGE, "logintype"))) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędny użytkownik/hasło lub kod Captcha wymagany do zalogowania!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private long getLongValue(final Object o) {
        long lo = -1;
        if (o instanceof Long) {
            lo = ((Long) o).longValue();
        } else {
            lo = ((Integer) o).intValue();
        }
        return lo;
    }

    /* Stable workaround */
    public static long getLongProperty(final Property link, final String key, final long def) {
        try {
            return link.getLongProperty(key, def);
        } catch (final Throwable e) {
            try {
                Object r = link.getProperty(key, def);
                if (r instanceof String) {
                    r = Long.parseLong((String) r);
                } else if (r instanceof Integer) {
                    r = ((Integer) r).longValue();
                }
                final Long ret = (Long) r;
                return ret;
            } catch (final Throwable e2) {
                return def;
            }
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void reset() {
    }

    private String decrypt(String enc, String key) {
        if (enc == null || key == null) {
            return null;
        }
        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
        enc = new String(jd.nutils.encoding.Base64.decode(enc));
        String ver = key.substring(0, 2);
        if ("eb".equals(ver)) {
            return decryptA(enc, decryptB(key));
        }
        if ("ed".equals(ver)) {
            return decryptA(enc, JDHash.getMD5(key));
        }
        return enc != null && enc.startsWith("http") ? enc : null;
    }

    private String decryptA(String enc, String key) {
        String ret = "";
        int encLen = enc.length();
        int keyLen = key.length();
        for (int i = 0; i < encLen; i++) {
            ret += String.valueOf((char) (enc.codePointAt(i) ^ key.codePointAt(i % keyLen)));
        }
        return ret;
    }

    private String decryptB(String key) {
        String ret = "";
        try {
            int[] h = new int[256];
            int c = 0, d = 0, e = 0;
            for (int i = 0; i < 256; i++) {
                h[i] = i;
            }
            for (int i = 0; i < 256; i++) {
                d = (d + h[i] + key.codePointAt(i % key.length())) % 256;
                c = h[i];
                h[i] = h[d];
                h[d] = c;
            }
            d = 0;
            for (int i = 0; i < key.length(); i++) {
                e = (e + 1) % 256;
                d = (d + h[e]) % 256;
                c = h[e];
                h[e] = h[d];
                h[d] = c;
                ret += String.valueOf((char) (key.codePointAt(i) ^ h[h[e] + h[d] % 256]));
            }
        } catch (Throwable e) {
            return null;
        }
        return ret.equals("") ? null : ret;
    }

}