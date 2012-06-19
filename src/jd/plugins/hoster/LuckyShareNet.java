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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "luckyshare.net" }, urls = { "http://(www\\.)?luckyshare\\.net/\\d+" }, flags = { 2 })
public class LuckyShareNet extends PluginForHost {

    public LuckyShareNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://luckyshare.net/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://luckyshare.net/termsofservice";
    }

    private static final String MAINPAGE = "http://luckyshare.net/";
    private static final Object LOCK     = new Object();

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(There is no such file available|<title>LuckyShare \\- Download</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1 class=\\'file_name\\'>([^<>\"/]+)</h1>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>LuckyShare \\- ([^<>\"/]+)</title>").getMatch(0);
        String filesize = br.getRegex("<span class=\\'file_size\\'>Filesize: ([^<>\"/]+)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String filesizelimit = br.getRegex(">Files with filesize over ([^<>\"\\'/]+) are available only for Premium Users").getMatch(0);
        if (filesizelimit != null) throw new PluginException(LinkStatus.ERROR_FATAL, "Free users can only download files up to " + filesizelimit);
        final String reconnectWait = br.getRegex("id=\"waitingtime\">(\\d+)</span>").getMatch(0);
        if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWait) * 1001l);
        final String rcID = br.getRegex("Recaptcha\\.create\\(\"([^<>\"/]+)\"").getMatch(0);
        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser ajax = br.cloneBrowser();
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.getPage("http://luckyshare.net/download/request/type/time/file/" + new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
        final String hash = ajax.getRegex("\"hash\":\"([a-z0-9]+)\"").getMatch(0);
        if (hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /** Waittime can still be skipped */
        int wait = 30;
        final String waittime = ajax.getRegex("\"time\":(\\d+)").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        for (int i = 0; i <= 5; i++) {
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            ajax.getPage("http://luckyshare.net/download/verify/challenge/" + rc.getChallenge() + "/response/" + c + "/hash/" + hash);
            if (ajax.containsHTML("(Verification failed|You can renew the verification image by clicking on a corresponding button near the validation input area)")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (ajax.containsHTML("(Verification failed|You can renew the verification image by clicking on a corresponding button near the validation input area)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = ajax.getRegex("\"link\":\"(http:[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
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
                br.postPage("http://luckyshare.net/auth/login", "remember=&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                br.getPage("http://luckyshare.net/account/");
                if (!br.containsHTML(">Account Type:</strong><br /><span>Premium")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://luckyshare.net/account/");
        String filesNum = br.getRegex("<strong>Number of files:</strong><br /><span>(\\d+)</span>").getMatch(0);
        if (filesNum != null) ai.setFilesNum(Integer.parseInt(filesNum));
        String space = br.getRegex("<strong>Storage Used:</strong><br /><span>([^<>\"]*?)</span></td>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<strong>Pro Membership Valid Until:</strong><br /><span>[A-Za-z]+, (\\d{2} [A-Za-z]+ \\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy hh:mm:ss", null));
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
        String dllink = br.getRedirectLocation();
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}