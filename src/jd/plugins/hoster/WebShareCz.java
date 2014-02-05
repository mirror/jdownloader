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
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "webshare.cz" }, urls = { "https?://(www\\.)?webshare\\.cz/(\\?fhash=[A-Za-z0-9]+|[A-Za-z0-9]{10}|(#/)?file/[a-z0-9]+)" }, flags = { 2 })
public class WebShareCz extends PluginForHost {

    public WebShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://webshare.cz/#/vip-benefits");
    }

    @Override
    public String getAGBLink() {
        return "http://webshare.cz/podminky.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://webshare.cz/file/" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0) + "/");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("https://webshare.cz/api/file_info/", "wst=&ident=" + getFID(link));
        if (br.containsHTML("<status>FATAL</status>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = getXMLtagValue("name");
        final String filesize = getXMLtagValue("size");
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.postPage("https://webshare.cz/api/file_link/", "wst=&ident=" + getFID(downloadLink));
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getHeaders().put("Referer", null);
        br.getHeaders().put("X-Requested-With", null);
        final String dllink = getXMLtagValue("link");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(>Požadovaný soubor nebyl nalezen|>Requested file not found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.getURL().contains("error=")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getXMLtagValue(final String tagname) {
        return br.getRegex("<" + tagname + ">([^<>\"]*?)</" + tagname + ">").getMatch(0);
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "file/([A-Za-z0-9]+)/").getMatch(0);
    }

    private static final String MAINPAGE = "http://webshare.cz";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
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
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                final String lang = System.getProperty("user.language");
                br.setFollowRedirects(false);
                br.postPage("https://webshare.cz/api/salt/", "username_or_email=" + Encoding.urlEncode(account.getUser()) + "&wst=");
                final String salt = br.getRegex("<salt>([^<>\"]*?)</salt>").getMatch(0);
                if (salt == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                String pw = crypt_md5(account.getPass().getBytes("UTF-8"), salt);
                pw = JDHash.getSHA1(pw);
                br.postPage("https://webshare.cz/api/login/", "username_or_email=" + Encoding.urlEncode(account.getUser()) + "&password=" + pw + "&keep_logged_in=true&wst=");
                final String token = getXMLtagValue("token");
                if (token == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.containsHTML("<code>LOGIN_FATAL_\\d+</code>")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.setProperty("token", token);
                br.setCookie(MAINPAGE, "wst", token);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
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
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.postPage("http://webshare.cz/api/user_data/", "wst=" + getToken(account));
        final String space = getXMLtagValue("private_space");
        final String filesnum = getXMLtagValue("files");
        // TODO: Wait for admin changes for more precise values
        final String credits = getXMLtagValue("credits");
        ai.setTrafficLeft(SizeFormatter.getSize((Long.parseLong(credits) * 10) + "MB"));
        ai.setFilesNum(Long.parseLong(filesnum));
        ai.setUsedSpace(space.trim());
        final String days = getXMLtagValue("vip_days");
        if (days == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(System.currentTimeMillis() + Integer.parseInt(days) * 24 * 60 * 60 * 1000l);
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.postPage("http://webshare.cz/api/file_link/", "ident=" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)/?$").getMatch(0) + "&wst=" + getToken(account));
        final String dllink = getXMLtagValue("link");
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getToken(final Account acc) {
        return acc.getStringProperty("token");
    }

    /*
     * Copyright (c) 1999 University of California. All rights reserved.
     * 
     * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions
     * are met: 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
     * disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
     * disclaimer in the documentation and/or other materials provided with the distribution. 3. Neither the name of the author nor the
     * names of any co-contributors may be used to endorse or promote products derived from this software without specific prior written
     * permission.
     * 
     * THIS SOFTWARE IS PROVIDED BY CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
     * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL CONTRIBUTORS BE LIABLE FOR ANY
     * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
     * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
     * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
     * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
     * 
     * $FreeBSD: src/lib/libcrypt/misc.c,v 1.1 1999/09/20 12:45:49 markm Exp $
     */

    static char[] itoa64 = /* 0 ... 63 => ascii - 64 */
                         "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static String cryptTo64(long v, int n) {
        StringBuilder result = new StringBuilder();
        while (--n >= 0) {
            result.append(itoa64[(int) v & 0x3f]);
            v >>= 6;
        }
        return result.toString();
    }

    /*
     * ---------------------------------------------------------------------------- "THE BEER-WARE LICENSE" (Revision 42):
     * <phk@login.dknet.dk> wrote this file. As long as you retain this notice you can do whatever you want with this stuff. If we meet some
     * day, and you think this stuff is worth it, you can buy me a beer in return. Poul-Henning Kamp
     * ----------------------------------------------------------------------------
     * 
     * $FreeBSD: src/lib/libcrypt/crypt-md5.c,v 1.5 1999/12/17 20:21:45 peter Exp $
     */
    private static String magic    = "$1$"; /*
                                             * This string is magic for this algorithm. Having it this way, we can get get better later on
                                             */
    private static int    MD5_SIZE = 16;

    private static void memset(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = 0;
        }
    }

    /*
     * UNIX password
     */
    private static String crypt_md5(byte[] pw, String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        StringBuilder passwd = new StringBuilder();
        String sp, ep;
        byte[] finalState = new byte[MD5_SIZE];

        int sl, pl, i;
        MessageDigest ctx = MessageDigest.getInstance("MD5");
        MessageDigest ctx1 = MessageDigest.getInstance("MD5");
        long l;

        /* Refine the Salt first */
        sp = salt;

        /* If it starts with the magic string, then skip that */
        if (sp.startsWith(magic)) {
            sp = sp.substring(magic.length() - 1);
        }
        byte[] saltBytes = sp.getBytes("UTF8");

        /* It stops at the first '$', max 8 chars */
        ep = sp;
        if (ep != null) {
            int end_salt = ep.indexOf('$');
            if (end_salt == -1) {
                sl = ep.length();
            } else if ((end_salt >= 0) && (end_salt <= 7)) {
                sl = end_salt + 1;
            } else {
                sl = 8;
            }
        } else {
            sl = 0;
        }

        ctx.reset();
        /* The password first, since that is what is most unknown */
        ctx.update(pw, 0, pw.length);
        /* Then our magic string */
        ctx.update(magic.getBytes("UTF8"), 0, magic.length());
        /* Then the raw salt */
        ctx.update(saltBytes, 0, sl);

        /* Then just as many characters of the MD5(pw,salt,pw) */
        ctx1.reset();
        ctx1.update(pw, 0, pw.length);
        ctx1.update(saltBytes, 0, sl);
        ctx1.update(pw, 0, pw.length);
        finalState = ctx1.digest();

        for (pl = pw.length; pl > 0; pl -= MD5_SIZE) {
            ctx.update(finalState, 0, pl > MD5_SIZE ? MD5_SIZE : pl);
        }

        /* Don't leave anything around in vm they could use. */
        memset(finalState);

        /* Then something really weird... */
        for (i = pw.length; i != 0; i >>>= 1) {
            if ((i & 1) != 0) {
                ctx.update(finalState, 0, 1);
            } else {
                ctx.update(pw, 0, 1);
            }
        }

        /* Now make the output string */
        passwd.append(magic);
        passwd.append(sp.substring(0, sl));
        passwd.append("$");

        finalState = ctx.digest();

        /*
         * and now, just to make sure things don't run too fast On a 60 Mhz Pentium this takes 34 msec, so you would need 30 seconds to
         * build a 1000 entry dictionary...
         */
        for (i = 0; i < 1000; i++) {
            ctx1.reset();

            if ((i & 1) != 0) {
                ctx1.update(pw, 0, pw.length);
            } else {
                ctx1.update(finalState, 0, MD5_SIZE);
            }

            if ((i % 3) != 0) {
                ctx1.update(saltBytes, 0, sl);
            }

            if ((i % 7) != 0) {
                ctx1.update(pw, 0, pw.length);
            }

            if ((i & 1) != 0) {
                ctx1.update(finalState, 0, MD5_SIZE);
            } else {
                ctx1.update(pw, 0, pw.length);
            }

            finalState = ctx1.digest();
        }

        l = (byteToUnsigned(finalState[0]) << 16) | (byteToUnsigned(finalState[6]) << 8) | byteToUnsigned(finalState[12]);
        passwd.append(cryptTo64(l, 4));
        l = (byteToUnsigned(finalState[1]) << 16) | (byteToUnsigned(finalState[7]) << 8) | byteToUnsigned(finalState[13]);
        passwd.append(cryptTo64(l, 4));
        l = (byteToUnsigned(finalState[2]) << 16) | (byteToUnsigned(finalState[8]) << 8) | byteToUnsigned(finalState[14]);
        passwd.append(cryptTo64(l, 4));
        l = (byteToUnsigned(finalState[3]) << 16) | (byteToUnsigned(finalState[9]) << 8) | byteToUnsigned(finalState[15]);
        passwd.append(cryptTo64(l, 4));
        l = (byteToUnsigned(finalState[4]) << 16) | (byteToUnsigned(finalState[10]) << 8) | byteToUnsigned(finalState[5]);
        passwd.append(cryptTo64(l, 4));
        l = byteToUnsigned(finalState[11]);
        passwd.append(cryptTo64(l, 2));

        /* Don't leave anything around in vm they could use. */
        memset(finalState);

        return passwd.toString();
    }

    private static int byteToUnsigned(byte aByte) {
        return aByte & 0xFF;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}