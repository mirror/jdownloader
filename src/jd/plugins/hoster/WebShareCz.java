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
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.WebshareCzConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class WebShareCz extends PluginForHost {
    public WebShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://webshare.cz/#/vip-benefits");
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "webshare.cz" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:[a-z0-9]+\\.)?" + buildHostsPatternPart(domains) + "/(\\?fhash=[A-Za-z0-9]+|[A-Za-z0-9]{10}|(#/)?file/[a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "http://webshare.cz/podminky.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return PluginJsonConfig.get(this.getConfigInterface()).getMaxSimultaneousFreeOrFreeAccountDownloads();
    }

    private String getContentURL(final DownloadLink link) {
        return "https://" + this.getHost() + "/file/" + getFID(link) + "/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "([A-Za-z0-9]+)/?$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("https://" + this.getHost() + "/api/file_info/", "wst=&ident=" + getFID(link));
        if (br.containsHTML("(?i)<status>FATAL</status>")) {
            /*
             * E.g. <response><status>FATAL</status><code>FILE_INFO_FATAL_1</code><message>File not
             * found.</message><app_version>29</app_version></response>
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String description = getXMLtagValue("description");
        final String filename = getXMLtagValue("name");
        final String filesize = getXMLtagValue("size");
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename.trim());
        }
        if (!StringUtils.isEmpty(filesize)) {
            link.setVerifiedFileSize(SizeFormatter.getSize(filesize));
        }
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        final String passwordProtected = getXMLtagValue("password");
        if (StringUtils.equals(passwordProtected, "1")) {
            link.setPasswordProtected(true);
        } else {
            link.setPasswordProtected(false);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        pwProtectedErrorhandling(link);
        br.postPage("/api/file_link/", "wst=&ident=" + getFID(link));
        final String dllink = getXMLtagValue("link");
        if (dllink == null) {
            checkErrorsAPI(br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection();
            checkErrorsAPI(br);
            if (br.containsHTML("(?i)(>\\s*Požadovaný soubor nebyl nalezen|>\\s*Requested file not found)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getURL().contains("error=")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    private void pwProtectedErrorhandling(final DownloadLink link) throws PluginException {
        if (link.isPasswordProtected()) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected URLs are not yet supported please contact JDownloader support");
        }
    }

    private void checkErrorsAPI(final Browser br) throws PluginException {
        final String status = getXMLtagValue(br, "status");
        final String code = getXMLtagValue(br, "code");
        final String message = getXMLtagValue(br, "message");
        if (StringUtils.equalsIgnoreCase(status, "FATAL") && !StringUtils.isEmpty(code) && !StringUtils.isEmpty(message)) {
            if (code.equalsIgnoreCase("FILE_LINK_FATAL_4")) {
                /*
                 * <response><status>FATAL</status><code>FILE_LINK_FATAL_4</code><message>File temporarily
                 * unavailable.</message><app_version>29</app_version></response>
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message, 5 * 60 * 1000l);
            } else if (code.equalsIgnoreCase("FILE_LINK_FATAL_5")) {
                /* Should be <message>Too many running downloads on too many devices.</message> */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, message, 3 * 60 * 1000l);
            }
        }
    }

    private String getXMLtagValue(final String tagname) {
        return getXMLtagValue(this.br, tagname);
    }

    private String getXMLtagValue(final Browser br, final String tagname) {
        return br.getRegex("<" + tagname + ">([^<>\"]*?)</" + tagname + ">").getMatch(0);
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            // Load cookies
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Do not verify cookies */
                    return;
                }
                br.postPage("https://" + this.getHost() + "/api/user_data/", "wst=" + getToken(account));
                final String status = getXMLtagValue("status");
                if (StringUtils.equalsIgnoreCase(status, "OK")) {
                    logger.info("Cookie login successful");
                    account.saveCookies(this.br.getCookies(this.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            logger.info("Performing full login");
            br.setFollowRedirects(false);
            br.postPage("https://" + this.getHost() + "/api/salt/", "username_or_email=" + Encoding.urlEncode(account.getUser()) + "&wst=");
            final String lang = System.getProperty("user.language");
            final String salt = br.getRegex("<salt>([^<]*?)</salt>").getMatch(0);
            if (salt == null) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new AccountInvalidException("\r\nUngültige E-Mail Adresse oder Benutzername!");
                } else {
                    throw new AccountInvalidException("\r\nInvalid E-Mail or username!");
                }
            }
            final String password = JDHash.getSHA1(crypt_md5(account.getPass().getBytes("UTF-8"), salt));
            final String digest = Hash.getMD5(account.getUser() + ":Webshare:" + account.getPass());
            /* wst is random 16 char cookie, app().setCookie('wst', ws.strings.randomString(16)) || app().cookie('wst') */
            /*
             * for request, wst is reverse string of wst cookie value, args.wst = app().auth().token() ||
             * ws.strings.subString(app().cookie('wst'), 16, 0);
             */
            br.postPage("/api/login/", "username_or_email=" + Encoding.urlEncode(account.getUser()) + "&password=" + password + "&digest=" + digest + "&keep_logged_in=1&wst=");
            if (br.containsHTML("<code>LOGIN_FATAL_\\d+</code>")) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new AccountInvalidException("\r\nUngültiges Passwort!\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!");
                } else {
                    throw new AccountInvalidException("\r\nInvalid password!\r\nQuick help:\r\nYou're sure that the password you entered is correct?\r\nIf your password contains special characters, change it (remove them) and try again!");
                }
            }
            final String token = getXMLtagValue("token");
            if (StringUtils.isEmpty(token)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setProperty("token", token);
            br.setCookie(br.getHost(), "wst", token);
            account.saveCookies(this.br.getCookies(this.getHost()), "");
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, true);
        if (!br.getURL().contains("/api/user_data/")) {
            br.postPage("/api/user_data/", "wst=" + getToken(account));
        }
        final String status = getXMLtagValue("status");
        if (!StringUtils.equalsIgnoreCase(status, "OK")) {
            final String message = getXMLtagValue("message");
            if (!StringUtils.isEmpty(message)) {
                throw new AccountInvalidException(message);
            } else {
                throw new AccountInvalidException();
            }
        }
        final AccountInfo ai = new AccountInfo();
        final String daysStr = getXMLtagValue("vip_days");
        String statustext;
        if (daysStr == null || "0".equals(daysStr)) {
            final String credits = getXMLtagValue("credits");
            ai.setTrafficLeft(SizeFormatter.getSize((Long.parseLong(credits) * 10) + "MB"));
            if (ai.getTrafficLeft() > 0) {
                account.setType(AccountType.PREMIUM);
                account.setConcurrentUsePossible(true);
                account.setMaxSimultanDownloads(20);
                statustext = account.getType().getLabel() + " | User with credits";
            } else {
                /* Free account or expired premium account */
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(getMaxSimultanFreeDownloadNum());
                account.setConcurrentUsePossible(false);
                statustext = account.getType().getLabel();
            }
        } else {
            ai.setValidUntil(System.currentTimeMillis() + Integer.parseInt(daysStr) * 24 * 60 * 60 * 1000l);
            ai.setUnlimitedTraffic();
            account.setType(AccountType.PREMIUM);
            account.setConcurrentUsePossible(true);
            account.setMaxSimultanDownloads(20);
            statustext = "VIP User";
        }
        statustext += " | Points: " + getXMLtagValue("points");
        ai.setStatus(statustext);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        pwProtectedErrorhandling(link);
        login(account, false);
        final boolean isPremium = AccountType.PREMIUM.equals(account.getType());
        br.postPage("https://" + this.getHost() + "/api/file_link/", "ident=" + this.getFID(link) + "&wst=" + getToken(account));
        final String dllink = getXMLtagValue("link");
        if (StringUtils.isEmpty(dllink)) {
            checkErrorsAPI(br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Only premium users can resume stopped downloads. */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, isPremium, isPremium ? 0 : 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            checkErrorsAPI(br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getToken(final Account account) {
        return account.getStringProperty("token");
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
    private final String magic    = "$1$"; /*
                                            * This string is magic for this algorithm. Having it this way, we can get get better later on
                                            */
    private final int    MD5_SIZE = 16;

    private static void memset(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = 0;
        }
    }

    /*
     * UNIX password
     */
    private String crypt_md5(byte[] pw, String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
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
    public Class<? extends WebshareCzConfig> getConfigInterface() {
        return WebshareCzConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}