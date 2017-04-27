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

import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "acapellas4u.co.uk" }, urls = { "http://(www\\.)?acapellas4u\\.co\\.uk/\\d+\\-[a-z0-9\\-_]+" })
public class AcapellasFourYouCoUk extends antiDDoSForHost {

    private static final String MAINPAGE = "http://www.acapellas4u.co.uk/";

    private static Object       LOCK     = new Object();

    private static final String USERTEXT = "Only downloadable for registered users!";

    public AcapellasFourYouCoUk(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.acapellas4u.co.uk/ucp.php?mode=register");
    }

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
    public String getAGBLink() {
        return "http://www.acapellas4u.co.uk/ucp.php?mode=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.acapellas4youcouk.only4registered", USERTEXT));
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        getPage(link.getDownloadURL());
        String hash = br.getRegex("filehash=([a-z0-9]+)").getMatch(0);
        if (hash == null) {
            hash = br.getRegex("\"unique_id\" : \"(.*?)\"").getMatch(0);
            if (hash == null) {
                hash = br.getRegex("var idcomments_post_id = \\'([a-z0-9]+)\\';").getMatch(0);
                if (hash == null) {
                    hash = br.getRegex("name=\"filehash\" type=\"hidden\" value=\"(.*?)\"").getMatch(0);
                }
            }
        }
        if (hash == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, "http://www.acapellas4u.co.uk/download_file.php?&filehash=" + hash + "&confirm=1/", false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            br.setCookiesExclusive(true);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) {
                acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            }
            if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (cookies.containsKey("acas4u_sevul_u") && account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    /* 2017-04-27: Always check if existing cookies work to avoid unnecessary login captchas! */
                    if (this.br.containsHTML("mode=logout")) {
                        return;
                    }
                }
            }
            getPage("http://www.acapellas4u.co.uk/");
            String sid = br.getRegex("name=\"sid\" value=\"([a-z0-9]{10,})\"").getMatch(0);
            if (sid == null) {
                sid = br.getCookie(MAINPAGE, "acas4u_sevulx_sid");
            }
            final Form loginform = this.br.getFormbyKey("password");
            if (loginform != null) {
                /* 2017-04-27: Prefer form handling */
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                if (sid != null) {
                    loginform.put("sid", sid);
                }
                String captchaurl = loginform.getRegex("\"(https?://(?:www\\.)?[^/]+/ucp\\.php\\?mode=confirm[^<>\"]+)\"").getMatch(0);
                if (captchaurl != null) {
                    /* 2017-04-27: New; login captcha (does not happen every attempt) */
                    captchaurl = Encoding.htmlDecode(captchaurl);
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), "http://" + this.getHost(), true);
                    final String code = this.getCaptchaCode(captchaurl, dummyLink);
                    loginform.put("confirm_code", Encoding.urlEncode(code));
                }
                submitForm(loginform);
            } else {
                String loginurl = "http://www.acapellas4u.co.uk/ucp.php?mode=login";
                String postdata = "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&redirect=index.php&login=Login&autologin=on";
                if (sid != null) {
                    loginurl += "&sid=" + sid;
                    postdata += "&sid=" + sid;
                }

                postPage(loginurl, postdata);
            }
            if ((br.getCookie(MAINPAGE, "acas4u_sevulx_u") == null || "1".equals(br.getCookie(MAINPAGE, "acas4u_sevulx_u"))) && (br.getCookie(MAINPAGE, "acas4u_sevul_u") == null || "1".equals(br.getCookie(MAINPAGE, "acas4u_sevul_u")))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", fetchCookies(MAINPAGE));
        }
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.acapellas4youcouk.only4registered", USERTEXT));
            return AvailableStatus.TRUE;
        }
        login(aa, false);
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.getURL().contains("/download_list.php") || br.containsHTML("<title>ACAPELLAS4U \\&bull; Browse Artists</title>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>ACAPELLAS4U \\&bull; File Download \\&bull; (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"title\" : \"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("var idcomments_post_title = \\'(.*?)\\';").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<p><b>File Name: ([^\"\\']+)</b></p>").getMatch(0);
                }
            }
        }
        String filesize = br.getRegex("File Size: ([^<>\"\\']+)").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}