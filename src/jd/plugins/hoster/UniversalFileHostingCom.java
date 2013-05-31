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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "universalfilehosting.com" }, urls = { "http://(www\\.)?universalfilehosting\\.com/dl/[A-Za-z0-9]+" }, flags = { 2 })
public class UniversalFileHostingCom extends PluginForHost {

    public UniversalFileHostingCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.universalfilehosting.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "http://www.universalfilehosting.com/terms-of-use";
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://www.universalfilehosting.com/lang/en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Invalid link\\.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("<p>Click to download : <strong>([^<>\"]*?)</strong>").getMatch(0);
        String fileSize = br.getRegex("File size : (.*?)<br").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (fileSize != null) link.setDownloadSize(SizeFormatter.getSize(fileSize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        final String dllink = br.getRegex("\"(http://(www\\.)?universalfilehosting\\.com/download/[a-z0-9]{32})\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String  MAINPAGE = "http://universalfilehosting.com";
    private static Object        LOCK     = new Object();
    private static AtomicInteger maxPrem  = new AtomicInteger(1);

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
                br.getPage("http://www.universalfilehosting.com/lang/en");
                br.getPage("http://www.universalfilehosting.com/login");
                final String session = br.getCookie(MAINPAGE, "session");
                if (session == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.postPage("http://www.universalfilehosting.com/login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                final String newSession = br.getCookie(MAINPAGE, "session");
                if (newSession == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Session cookie hasn't changed->Account isn't valid
                if (newSession.equals(session)) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://www.universalfilehosting.com/lang/en");
        br.getPage("http://www.universalfilehosting.com/my-account");
        if (br.containsHTML("<th>Daily bandwidth usage</th>[\t\n\r ]+<td id=\"bar\">[\t\n\r ]+[\t\n\r ]+<span>Unlimited</span>")) {
            ai.setUnlimitedTraffic();
            account.setProperty("nolimits", true);
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                // not available in old Stable 0.9.581
            }
        } else {
            account.setProperty("nolimits", false);
            try {
                maxPrem.set(1);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                // not available in old Stable 0.9.581
            }
            final Regex trafficStuff = br.getRegex("<div class=\"pull\\-left\" style=\"margin\\-left: 10px\">[\t\n\r ]+<span>(\\d+(\\.\\d+)? [A-Za-z]{1,5}) / (\\d+(\\.\\d+)? [A-Za-z]{1,5})");
            if (trafficStuff.getMatches().length != 1) {
                account.setValid(false);
                return ai;
            }
            ai.setTrafficMax(SizeFormatter.getSize(trafficStuff.getMatch(2)));
            ai.setTrafficLeft(SizeFormatter.getSize(trafficStuff.getMatch(2)) - SizeFormatter.getSize(trafficStuff.getMatch(0)));
        }
        String space = br.getRegex("<span id=\"used\\-storage\">(\\d+(\\.\\d+)? [A-Za-z]{1,5})</span>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        account.setValid(true);
        final String accType = br.getRegex("<th>Account type</th>[\t\n\r ]+<td>[\t\n\r ]+<div class=\"span\" style=\"margin\\-left: 0px\">([^<>\"/]*?)</div>").getMatch(0);
        if (accType != null) {
            ai.setStatus(accType.trim() + " User");
        } else {
            ai.setStatus("Unknown accounttype!");
            account.setValid(false);
            return ai;
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        boolean resume = false;
        if (account.getBooleanProperty("nolimits")) resume = true;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), resume, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}