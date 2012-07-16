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
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "azushare.net" }, urls = { "http://(www\\.)?azushare\\.net/[A-Za-z0-9]+/" }, flags = { 0 })
public class AzuShareNet extends PluginForHost {

    private static final String MAINPAGE = "http://azushare.net";
    private static final Object LOCK     = new Object();

    public AzuShareNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://azushare.net/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://azushare.net/premium";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setCookie(MAINPAGE, "lang", "en");
        br.getPage(link.getDownloadURL());
        final String filename = br.getRegex("<div class=\"down\\-file\">([^<>\"]*?)</div>").getMatch(0);
        final String filesize = br.getRegex("<div class=\"file\\-properties\">[\t\n\r ]+File size: ([^<>\"]*?) \\| Upload date:").getMatch(0);
        if ("".equals(filename)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final Regex limit = br.getRegex(">For next free download you have to wait <strong>(\\d+):(\\d+)s</strong>");
        if (limit.getMatches().length == 1) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(limit.getMatch(0)) * 60 + Integer.parseInt(limit.getMatch(1))) * 1001l);
        final String rcID = br.getRegex("noscript\\?k=([^<>\"]*?)\"").getMatch(0);
        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, downloadLink);
        br.postPage(br.getURL(), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.setFollowRedirects(false);
        String dllink = br.getRegex("<h2 class=\"grey\\-brake\"><a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://s\\d+\\.azushare\\.net/dl/[a-z0-9]+/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
                br.setFollowRedirects(false);
                br.setCookie(MAINPAGE, "lang", "en");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("http://azushare.net/ajax/register.php", "log=1&loginV=" + Encoding.urlEncode(account.getUser()) + "&passV=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "p") == null || !br.containsHTML("\"Login successful")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://azushare.net/settings");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("<br/> Premium: (\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        final String dllink = br.getRedirectLocation();
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
        // Only had one link to test
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}