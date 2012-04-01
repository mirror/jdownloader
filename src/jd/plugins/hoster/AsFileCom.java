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
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "asfile.com" }, urls = { "http://(www\\.)?asfile\\.com/file/[A-Za-z0-9]+" }, flags = { 2 })
public class AsFileCom extends PluginForHost {

    public AsFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        this.enablePremium("http://asfile.com/en/index/pay");
    }

    @Override
    public String getAGBLink() {
        return "http://asfile.com/en/page/offer";
    }

    private static final Object LOCK     = new Object();
    private static final String MAINPAGE = "http://asfile.com";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<title>ASfile\\.com</title>|>Page not found<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("Download: <strong>([^<>\"\\']+)</strong><br/> \\(([^<>\"\\']+)\\)");
        String filename = br.getRegex("<meta name=\"title\" content=\"Free download ([^<>\"\\']+)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Free download ([^<>\"\\']+)</title>").getMatch(0);
            if (filename == null) {
                filename = fileInfo.getMatch(0);
            }
        }
        String filesize = fileInfo.getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (!br.containsHTML("/free\\-download/")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String fileID = new Regex(downloadLink.getDownloadURL(), "asfile\\.com/file/(.+)").getMatch(0);
        br.getPage("http://asfile.com/en/free-download/file/" + fileID);
        final String hash = br.getRegex("hash: \\'([a-z0-9]+)\\'").getMatch(0);
        final String storage = br.getRegex("storage: \\'([^<>\"\\']+)\\'").getMatch(0);
        if (hash == null || storage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String waittime = br.getRegex("class=\"orange\">(\\d+)</span>").getMatch(0);
        int wait = 60;
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://asfile.com/en/index/convertHashToLink", "hash=" + hash + "&path=" + fileID + "&storage=" + Encoding.urlEncode(storage) + "&name=" + Encoding.urlEncode(downloadLink.getName()));
        final String correctedBR = br.toString().replace("\\", "");
        String dllink = new Regex(correctedBR, "\"url\":\"(http:[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) dllink = new Regex(correctedBR, "\"(http://s\\d+\\.asfile\\.com/file/free/[a-z0-9]+/\\d+/[A-Za-z0-9]+/[^<>\"\\'/]+)\"").getMatch(0);
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
            // Load cookies
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
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
            br.postPage(MAINPAGE + "/en/login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            br.getPage(MAINPAGE + "/en/");
            if (!br.containsHTML("logout\">Logout ")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            br.getPage("http://asfile.com/en/profile");
            if (!br.containsHTML("<p>Your account:<strong> premium")) {
                logger.info("This is no premium account!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("premium </strong>\\(to (\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2})\\)</p>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy/MM/dd hh:mm", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        if (account.getUser() == null || account.getUser().trim().length() == 0) {
            br.postPage("http://asfile.com/en/index/enterCode", "code=" + account.getPass());
            final String expire = br.getRegex("<p>You have got the premium access to: (\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2})\\)</p>").getMatch(0);
            if (br.getCookie(MAINPAGE, "code") == null || expire == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            account.setValid(true);
            AccountInfo ai = new AccountInfo();
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy/MM/dd hh:mm", null));
            ai.setStatus("Premium User");
            account.setAccountInfo(ai);
        } else {
            login(account, false);
        }
        br.setFollowRedirects(false);
        br.getPage("http://asfile.com/en/premium-download/file/" + new Regex(link.getDownloadURL(), "asfile\\.com/file/(.+)").getMatch(0));
        String dllink = br.getRegex("\"(http://s\\d+\\.asfile\\.com/file/premium/[a-z0-9]+/\\d+/[A-Za-z0-9]+/[^<>\"\\'/]+)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("<p><a href=\"(http://[^<>\"\\'/]+)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML(">404 Not Found<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
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