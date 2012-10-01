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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nzbload.com" }, urls = { "http://(www\\.)?nzbloaddecrypted\\.com/en/download/[a-z0-9]+/\\d+" }, flags = { 2 })
public class NzbLoadCom extends PluginForHost {

    public NzbLoadCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.nzbload.com/en/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.nzbload.com/en/legal/terms-of-service";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("nzbloaddecrypted.com/", "nzbload.com/"));
    }

    private static final String MAINPAGE = "http://nzbload.com";
    private static final Object LOCK     = new Object();

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "text/plain, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final Regex params = new Regex(link.getDownloadURL(), "http://(www\\.)?nzbload\\.com/en/download/([a-z0-9]+)/(\\d+)");
        br.getPage("http://www.nzbload.com/data/download.json?t=" + System.currentTimeMillis() + "&sub=" + params.getMatch(1) + "&params[0]=" + params.getMatch(2));
        if (br.containsHTML("\"filename\":null,\"filesize\":null")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = get("filename");
        String filesize = get("filesize");
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final Regex params = new Regex(downloadLink.getDownloadURL(), "http://(www\\.)?nzbload\\.com/en/download/([a-z0-9]+)/(\\d+)");
        final Browser br2 = br.cloneBrowser();
        br2.getPage("http://www.nzbload.com/tpl/download/" + params.getMatch(1) + ".js?version=1.050");
        final String sleep = br2.getRegex("updateTimer = setTimeout\\(\\'checkProgress\\(\\);\\', (\\d+)\\);").getMatch(0);
        final String rcID = br2.getRegex("Recaptcha\\.create\\(\\'([^<>\"]*?)\\'").getMatch(0);
        if (rcID == null || sleep == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.nzbload.com/data/download.json?overwrite=start-download&t=" + System.currentTimeMillis() + "&sub=" + params.getMatch(1) + "&params[0]=" + params.getMatch(2));
        if (br.containsHTML("Free users can download")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
        final String expiry = get("expiry");
        final String hash = get("hash");
        if (expiry == null || hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(Long.parseLong(sleep) + 500, downloadLink);
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        rc.load();
        for (int i = 0; i <= 5; i++) {
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            br.postPage("http://www.nzbload.com/action/download.json?act=verify_captcha&t=" + System.currentTimeMillis(), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
            if (br.containsHTML("\"success\":false")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (br.containsHTML("\"success\":false")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.getPage("http://www.nzbload.com/data/download.json?overwrite=get_url&t=" + System.currentTimeMillis() + "&sub=" + params.getMatch(1) + "&params[0]=" + params.getMatch(2) + "&params[1]=" + hash + "&params[2]=" + expiry + "&params[3]=" + downloadLink.getName());
        if (br.containsHTML("Free users can download 1 file at the same time")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l);
        String dllink = get("url");
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
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("http://www.nzbload.com/action/login.json?t=" + System.currentTimeMillis() + "&act=submit", "remember-me=1&lastloc=%2Fen%2Flogout&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "loggedin") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        final String expire = get("expiry");
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(expire));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link);
        String dllink = get("url");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -4);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 3;
    }

    private String get(final String parameter) {
        String output = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        if (output == null) output = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        return output;
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