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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "evilangel.com" }, urls = { "http://(www\\.)?members\\.evilangel.com/en/[A-Za-z0-9\\-_]+/(download/\\d+/\\d+p|film/\\d+)" }, flags = { 2 })
public class EvilAngelCom extends PluginForHost {

    public EvilAngelCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.evilangel.com/en/join");
    }

    @Override
    public String getAGBLink() {
        return "http://www.evilangel.com/en/terms";
    }

    private static final String FILMLINK = "http://(www\\.)?members\\.evilangel.com/en/[A-Za-z0-9\\-_]+/film/\\d+";
    private String              DLLINK   = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            String filename = null;
            login(aa, false);
            if (link.getDownloadURL().matches(FILMLINK)) {
                br.getPage(link.getDownloadURL());
                filename = br.getRegex("<h1 class=\"title\">([^<>\"]*?)</h1>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<h1 class=\"h1_title\">([^<>\"]*?)</h1>").getMatch(0);
                    if (filename == null) filename = br.getRegex("<h2 class=\"h2_title\">([^<>\"]*?)</h2>").getMatch(0);
                }
                if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                filename = Encoding.htmlDecode(filename.trim());
                /** INFO: There are also .wmv versions available but we prefer .mp4 here as 1080p is only available as .mp4 */
                final String[] qualities = { "1080p", "720p", "540p", "480p", "240p" };
                for (final String quality : qualities) {
                    DLLINK = br.getRegex("\"(/en/[A-Za-z0-9\\-_]+/download/\\d+/" + quality + "/download/mp4)\"").getMatch(0);
                    if (DLLINK != null) {
                        filename = filename + "-" + quality + ".mp4";
                        break;
                    }
                    if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    DLLINK = "http://members.evilangel.com" + DLLINK;
                }
            } else {
                DLLINK = link.getDownloadURL();
            }
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    if (filename == null)
                        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                    else
                        link.setFinalFileName(filename);
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }

        } else {
            link.getLinkStatus().setStatusText("Links can only be chacked and downloaded via account!");
            return AvailableStatus.UNCHECKABLE;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "Links can only be chacked and downloaded via account!");
    }

    private static final String MAINPAGE = "http://evilangel.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setCookie(MAINPAGE, "enterSite", "en");
                br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
                br.setCustomCharset("utf-8");
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
                br.setFollowRedirects(false);
                br.getPage("http://www.evilangel.com/en/login");
                if (br.containsHTML(">We are experiencing some problems\\!<")) {
                    final AccountInfo ai = new AccountInfo();
                    ai.setStatus("Your IP is banned. Please re-connect to get a new IP to be able to log-in!");
                    logger.info("Your IP is banned. Please re-connect to get a new IP to be able to log-in!");
                    account.setAccountInfo(ai);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }

                final Date d = new Date();
                SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
                final String date = sd.format(d);
                sd = new SimpleDateFormat("k:mm");
                final String time = sd.format(d);
                final String captcha = br.getRegex("name=\"captcha\\[id\\]\" value=\"([a-z0-9]{32})\"").getMatch(0);
                String postData = "submit=Click+here+to+login&mOffset=1&back&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "=&mDate=" + Encoding.urlEncode(date) + "&mTime=" + Encoding.urlEncode(time);
                // Handly stupid login captcha
                if (captcha != null) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "evilangel.com", "http://evilangel.com", true);
                    final String code = getCaptchaCode("http://www.evilangel.com/en/captcha/" + captcha, dummyLink);
                    postData += "&captcha%5Bid%5D=" + captcha + "&captcha%5Binput%5D=" + Encoding.urlEncode(code);
                }
                br.postPage("http://www.evilangel.com/en/login", postData);
                if (br.containsHTML(">Your account is deactivated for abuse")) {
                    final AccountInfo ai = new AccountInfo();
                    ai.setStatus("Your account is deactivated for abuse. Please re-activate it to use it in JDownloader.");
                    logger.info("Your account is deactivated for abuse. Please re-activate it to use it in JDownloader.");
                    account.setAccountInfo(ai);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getCookie(MAINPAGE, "save_login") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        final AccountInfo ai = new AccountInfo();
        try {
            // Prevent direct login to prevent login captcha
            login(account, false);
            br.getPage("http://members.evilangel.com/");
            if (!br.containsHTML("Welcome back, <strong>")) login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
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
    public void resetDownloadlink(DownloadLink link) {
    }

}