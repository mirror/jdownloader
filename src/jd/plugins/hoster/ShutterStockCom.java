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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shutterstock.com" }, urls = { "http://(www\\.)?shutterstock\\.com/pic\\-\\d+/[a-z0-9\\-]+\\.html" }, flags = { 2 })
public class ShutterStockCom extends PluginForHost {

    public ShutterStockCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "http://www.shutterstock.com/website_terms.mhtml";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getPage(link.getDownloadURL().replace("/pic-", "/language.en/pic-"));
        if (br.containsHTML("<div class=\"photo\\-error\">")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("\"title\":\"\\\\\"([^<>\"]*?)\\\\\"\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()) + ".jpg");
        return AvailableStatus.TRUE;
    }

    // Freeusers can only download the thumbnails
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String dllink = br.getRegex("class=\"thumb_image\"  src=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://image\\.shutterstock\\.com/display_pic_with_logo/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fixFilename(downloadLink);
        dl.startDownload();
    }

    private void fixFilename(final DownloadLink downloadLink) {
        final String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        final String newExtension = serverFilename.substring(serverFilename.lastIndexOf("."));
        if (newExtension != null && !downloadLink.getName().endsWith(newExtension)) {
            final String oldExtension = downloadLink.getName().substring(downloadLink.getName().lastIndexOf("."));
            if (oldExtension != null)
                downloadLink.setFinalFileName(downloadLink.getName().replace(oldExtension, newExtension));
            else
                downloadLink.setFinalFileName(downloadLink.getName() + newExtension);
        }
    }

    private static final String MAINPAGE = "http://shutterstock.com";
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
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("http://www.shutterstock.com/login.mhtml");
                if (br.containsHTML("google\\.com/recaptcha/")) {
                    // Handly stupid login captcha
                    final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
                    if (rcID == null) {
                        logger.warning("Expected login captcha is not there!");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.setId(rcID);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "shutterstock.com", "http://shutterstock.com", true);
                    final String c = getCaptchaCode(cf, dummyLink);
                    br.postPage("http://www.shutterstock.com/login.mhtml", "submit=Sign+In&landing_page=%2F&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "");
                    if (br.containsHTML("google\\.com/recaptcha/")) {
                        logger.info("Wrong password, username or captcha, stopping...");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else {
                    br.postPage("http://www.shutterstock.com/login.mhtml", "submit=Sign+In&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                    if (br.containsHTML("You\\'ve entered an incorrect username/password combination")) {
                        logger.info("Wrong password or username, stopping...");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
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
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        long expireTime = 0;
        final Regex hoursMinutes = br.getRegex("<span class=\"detail\"><nobr>(\\d+)h (\\d+)m</nobr><span");
        if (hoursMinutes.getMatches().length != 0) {
            expireTime += (Integer.parseInt(hoursMinutes.getMatch(0)) * 60 * 60 * 1000) + (Integer.parseInt(hoursMinutes.getMatch(1)) * 60 * 1000);
        }
        final String days = br.getRegex("class=\"punctuation\"> \\+ </span><nobr>(\\d+) days?</nobr>").getMatch(0);
        if (days != null) {
            expireTime += Integer.parseInt(days) * 24 * 60 * 60 * 1000;
        }
        if (expireTime == 0) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(System.currentTimeMillis() + expireTime);
        }
        account.setValid(true);
        try {
            // Can request captchas
            account.setConcurrentUsePossible(false);
        } catch (final Exception e) {
            // Not available in old Stable
        }
        final String downloadsLeftToday = br.getRegex("<span class=\"lihp_detail_wrapper\">[\t\n\r ]+<span class=\"detail\">(\\d+)</span>").getMatch(0);
        if (downloadsLeftToday != null) {
            ai.setStatus("Premium User with " + downloadsLeftToday + " downloads left for today.");
        } else {
            ai.setStatus("Premium User");
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(true);
        String dllink = checkDirectLink(link, "premlink");
        if (dllink == null) {
            br.getPage(link.getDownloadURL());
            final Browser br2 = br.cloneBrowser();
            final String[] qualities = { "huge_tiff", "huge_jpg", "medium_jpg", "small_jpg", "vector_eps" };
            String dLink = null;
            for (final String quality : qualities) {
                dLink = br.getRegex("\"(/dl2_lim\\.mhtml\\?id=\\d+\\&size=" + quality + "\\&src=)\"").getMatch(0);
                if (dLink != null) {
                    br2.getPage("http://www.shutterstock.com" + dLink);
                    if (!br2.containsHTML(">Get the freedom of an Enhanced License")) {
                        br.getRequest().setHtmlCode(br2.toString());
                        break;
                    }
                }
            }
            if (dLink == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Link might not be downloadable!");
            // Captcha cannot be skipped without anti captcha method!
            for (int i = 0; i <= 3; i++) {
                final String captchaLink = br.getRegex("\"(show_verify_image_lim\\-\\d+\\.jpg\\?x=\\d+)\"").getMatch(0);
                // Account can get deactivated during requests because of account sharing and maybe also other reasons
                if (br.containsHTML("Switched computers\\?")) {
                    logger.info("Login fail #1");
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Login fail #1");
                }
                if (br.containsHTML("<li id=\"already_a_user_text\">")) {
                    logger.info("Login fail #2");
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Login fail #2");
                }

                if (captchaLink == null) {
                    logger.warning("Captcha fail begin:" + br.toString() + " Captcha fail end");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String code = getCaptchaCode("http://www.shutterstock.com/" + captchaLink, link);
                br.getPage(dLink + "&chosen_subscription=redownload_standard&code=" + code + "&method=download");
                if (br.containsHTML(">Please re-enter the security code<")) continue;
                break;
            }
            if (br.containsHTML(">Please re-enter the security code<")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dllink = br.getRegex("(http://download\\.shutterstock\\.com/gatekeeper/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fixFilename(link);
        link.setProperty("premlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
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
    public void resetDownloadlink(DownloadLink link) {
    }

}