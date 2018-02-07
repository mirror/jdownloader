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

import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videobox.com" }, urls = { "http://(www\\.)?videoboxdecrypted\\.com/decryptedscene/\\d+" })
public class VideoBoxCom extends PluginForHost {
    public VideoBoxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.videobox.com/tour/home/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.videobox.com/public/terms-of-service";
    }

    private String           dllink           = null;
    public static final long trust_cookie_age = 300000l;

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText("Only checkable with enabled premium account");
            return AvailableStatus.UNCHECKABLE;
        }
        return requestFileInformation(link, aa);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        login(account, false, this.br);
        final String sessionID = br.getCookie("http://videobox.com/", "JSESSIONID");
        dllink = checkDirectLink(link, "directlink");
        br.setFollowRedirects(true);
        final String orginalurl = link.getStringProperty("originalurl", null);
        if (orginalurl == null) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Delete link and re-add it!");
        }
        br.getPage(orginalurl);
        if (dllink == null) {
            br.getPage("http://www.videobox.com/content/download/options/" + link.getStringProperty("sceneid", null) + ".json?x-user-name=" + Encoding.urlEncode(account.getUser()) + "&x-session-key=" + sessionID + "&callback=metai.buildDownloadLinks");
            dllink = getSpecifiedQuality(link.getStringProperty("quality", null));
        }
        link.setFinalFileName(link.getStringProperty("finalname", null));
        link.setDownloadSize(SizeFormatter.getSize(link.getStringProperty("plainfilesize", null)));
        return AvailableStatus.TRUE;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private String getSpecifiedQuality(final String quality) throws PluginException {
        final String qualityInfo = br.getRegex("(\"res\" : \"" + quality + "\".*?)\\}").getMatch(0);
        if (qualityInfo == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return qualityInfo;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private static final String MAINPAGE = "http://videobox.com";
    private static Object       LOCK     = new Object();

    public void login(final Account account, final boolean force, final Browser br) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(account.getHoster(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    /* TODO: Check if loggedIn, via HTML code */
                    return;
                }
                br.setFollowRedirects(true);
                br.getPage("https://www." + account.getHoster() + "/login");
                String postData = "login-page=login-page&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember_me=true&x=0&y=0";
                /* TODO: 2018-02-07: Fix this (status unclear but login without captcha should work fine) */
                if (false) {
                    if (this.getDownloadLink() == null) {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), "https://" + account.getHoster(), true);
                        this.setDownloadLink(dummyLink);
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    postData += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                }
                br.postPage(br.getURL(), postData);
                if (br.containsHTML("Your account expired on")) {
                    account.getAccountInfo().setExpired(true);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account expired", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(MAINPAGE, "SPRING_SECURITY_REMEMBER_ME_COOKIE") == null || br.getURL().contains("videobox.com/auth-fail")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true, this.br);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("https://www.videobox.com/billing/account");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex(">next statement</div>[\t\n\r ]+<div class=\"value\">([^<>\"]*?)</div>").getMatch(0);
        if (expire == null && !br.containsHTML("<strong>Account status:</strong>[\t\n\r ]+ACTIVE")) {
            account.setValid(false);
            return ai;
        } else if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "MM/dd/yyyy", Locale.ENGLISH));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}