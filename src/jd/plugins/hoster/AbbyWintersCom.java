//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Cookie;
import jd.http.Cookies;
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

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "abbywinters.com" }, urls = { "http://(www\\.)?abbywinters\\.com/shoot/[a-z0-9\\-_]+/(images/stills/[a-z0-9\\-_]+|videos/video/clip)" }, flags = { 2 })
public class AbbyWintersCom extends PluginForHost {

    public AbbyWintersCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.abbywinters.com/tour");
    }

    @Override
    public String getAGBLink() {
        return "http://www.abbywinters.com/about/termsandconditions";
    }

    private static final String PICTURELINK = "http://(www\\.)?abbywinters\\.com/shoot/[a-z0-9\\-_]+/images/stills/[a-z0-9\\-_]+";

    // private static final String VIDEOLINK = "http://(www\\.)?abbywinters\\.com/shoot/[a-z0-9\\-_]+/videos/video/clip";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // Login required to check/download
        final Account aa = AccountController.getInstance().getValidAccount(this);
        // This shouldn't happen
        if (aa == null) {
            link.getLinkStatus().setStatusText("Only downlodable/checkable via account!");
            return AvailableStatus.UNCHECKABLE;
        }
        login(aa, false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("404 Page not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = null;
        if (link.getDownloadURL().matches(PICTURELINK)) {
            final Regex fInfo = br.getRegex("<title>([^<>\"]*?)\\| Image (\\d+) of \\d+</title>");
            if (fInfo.getMatches().length != 1) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final DecimalFormat df = new DecimalFormat("0000");
            filename = Encoding.htmlDecode(fInfo.getMatch(0).trim()) + "_" + df.format(Integer.parseInt(fInfo.getMatch(1))) + ".jpg";
        } else {
            String username = br.getRegex("title=\"View profile: ([^<>\"]*?)\"").getMatch(0);
            if (username == null) username = br.getRegex("</span>([^<>\"]*?)<span class=\"icon_videoclip\">").getMatch(0);
            final String videoName = br.getRegex("<title>([^<>\"]*?): .*?</title>").getMatch(0);
            if (username != null && videoName != null) filename = Encoding.htmlDecode(username) + " - " + Encoding.htmlDecode(videoName) + ".mp4";
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downlodable/checkable via account!");
    }

    private static final String MAINPAGE = "http://abbywinters.com";
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
                br.setFollowRedirects(false);
                br.getPage("http://www.abbywinters.com/");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                if (br.containsHTML("for=\"captcha\">Security Check</label>")) {
                    String captchaPublic = br.getRegex("name=\"captchapublic\" class=\"hide\" value=\"([^<>\"]*?)\"").getMatch(0);
                    if (captchaPublic == null) {
                        logger.warning("Login captcha handling failed!");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    captchaPublic = Encoding.urlEncode(captchaPublic);
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "abbywinters.com", "http://abbywinters.com", true);
                    final String code = getCaptchaCode("http://www.abbywinters.com/captcha?code=" + captchaPublic, dummyLink);
                    br.postPage("http://www.abbywinters.com/rpc/login", "remember=on&form-action=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&captcha=" + Encoding.urlEncode(code) + "&captchapublic=" + captchaPublic);
                } else {
                    br.postPage("http://www.abbywinters.com/rpc/login", "remember=on&form-action=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                }
                if (br.containsHTML("\"result\":\"failed\"") || !br.containsHTML("\"result\":\"ok\"") || br.getCookie(MAINPAGE, "user") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://www.abbywinters.com/myaccount");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("<th>Rebill date:</th><td>([^<>\"]*?)</td>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink;
        if (link.getDownloadURL().matches(PICTURELINK)) {
            dllink = br.getRegex("\"(http://[^<>\"]*?)\" class=\"viewXLarge\"").getMatch(0);
        } else {
            dllink = br.getRegex("class=\"download_icon_ok\"><a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        }
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