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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vmall.com", "dbank.com" }, urls = { "http://(www\\.)?vmalldecrypted\\.com/\\d+", "vgt5ui6trbevf6ijmnbdli94DELETEME" }, flags = { 2, 0 })
public class DBankCom extends PluginForHost {

    public DBankCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE;
    }

    private static final Object LOCK     = new Object();
    private static final String MAINPAGE = "http://vmall.com/";

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
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
            if (!br.getRegex("\"retcode\":\"0000\"").matches()) { throw new PluginException(LinkStatus.ERROR_FATAL, "Wrong password!"); }
            br.getPage(dllink);
        }

        String key = br.getRegex("\"encryKey\":\"([^\"]+)").getMatch(0);
        String linkList = br.getRegex("\"files\":\\[(.*?)\\]\\}").getMatch(0);
        String fileId = downloadLink.getStringProperty("id");
        String currentJsonString = null;
        for (String s : new Regex(linkList == null ? "NPE" : linkList, "\\{(.*?)\\}").getColumn(0)) {
            if (new Regex(s, "\"id\":" + fileId).matches()) currentJsonString = s;
        }

        /* get fresh encrypted url string */
        Regex linkInfo = new Regex(currentJsonString, "\"downloadurl\":\"([^<>\"]+)");
        /* fail */
        if (linkInfo.getMatches().length == 0 || key == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        downloadLink.setProperty("downloadurl", linkInfo.getMatch(0));
        downloadLink.setProperty("encryKey", key);
        return AvailableStatus.TRUE;
    }

    private void doFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getStringProperty("mainlink"));

        String key = downloadLink.getStringProperty("encryKey", null);
        String enc = downloadLink.getStringProperty("downloadurl", null);
        String dllink = decrypt(enc, key);

        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
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

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage("http://login.vmall.com/accounts/loginAuth", "userDomain.rememberme=true&ru=http%3A%2F%2Fwww.vmall.com%2FServiceLogin.action&forward=&relog=&client=&userDomain.user.email=" + Encoding.urlEncode(account.getUser()) + "&userDomain.user.password=" + Encoding.urlEncode(account.getPass()));
                if (!"normal".equals(br.getCookie(MAINPAGE, "login_type"))) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
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
        if (enc == null || key == null) return null;
        enc = new String(jd.nutils.encoding.Base64.decode(enc));
        String ver = key.substring(0, 2);
        if ("eb".equals(ver)) { return decryptA(enc, decryptB(key)); }
        if ("ed".equals(ver)) { return decryptA(enc, JDHash.getMD5(key)); }
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