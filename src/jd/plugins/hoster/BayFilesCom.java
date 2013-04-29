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
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bayfiles.net", "bayfiles.com" }, urls = { "http://(www\\.)?bayfiles\\.(com|net)/file/[A-Z0-9]+/[A-Za-z0-9]+/.+", "ri9hj59hzjkmv0zkDELETE_MEhgt85hjz09ckmv5ohzj049rghj4" }, flags = { 2, 0 })
public class BayFilesCom extends PluginForHost {

    private static final String CAPTCHAFAILED = "\"Invalid captcha\"";
    private static final String MAINPAGE      = "http://bayfiles.com";
    private static Object       LOCK          = new Object();

    public BayFilesCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://bayfiles.com/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("bayfiles.com/", "bayfiles.net/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Correct previously added links
        correctDownloadLink(link);
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        try {
            br.getPage(link.getDownloadURL());
        } catch (final BrowserException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("(Please check your link\\.<|>Invalid security token\\.|>The link is incorrect<|>The file you requested has been deleted<|>We have messed something up<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("<h2>File:</h2>([\t\n\r ]+)?<p title=\"([^\"\\']+)\">[^\"\\']+, <strong>(.*?)</strong>");
        final String filename = fileInfo.getMatch(1);
        final String filesize = fileInfo.getMatch(2);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // Check if free premium download is available (then we can use
        // unlimited chunks)
        int chunks = 0;
        String dllink = br.getRegex("<div style=\"text\\-align: center;\">[\t\n\r ]+<a class=\"highlighted\\-btn\" href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = getExactDllink();
        if (dllink == null) {
            chunks = 1;
            if (br.containsHTML("(has recently downloaded a file\\.|Upgrade to premium or wait)")) {
                String reconnectWait = br.getRegex("Upgrade to premium or wait (\\d+)").getMatch(0);
                int reconWait = 60;
                if (reconnectWait != null) reconWait = Integer.parseInt(reconnectWait);
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, reconWait * 60 * 1001l);
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("Referer", downloadLink.getDownloadURL());
            final String vFid = br.getRegex("var vfid = (\\d+);").getMatch(0);
            if (vFid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // Can be skipped at the moment
            String waittime = br.getRegex("id=\"countDown\">(\\d+)</").getMatch(0);
            int wait = 10;
            if (waittime != null) wait = Integer.parseInt(waittime);
            Browser brc = br.cloneBrowser();
            brc.getPage("http://bayfiles.net/js/bayfiles.js");
            br.getPage("http://bayfiles.net/ajax_download?_=" + System.currentTimeMillis() + "&action=startTimer&vfid=" + vFid);
            String token = br.getRegex("\"token\":\"(.*?)\"").getMatch(0);
            if (token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            String captcha = brc.getRegex("(/\\*[ \t\r\n]*?captcha.create)").getMatch(0);
            if (captcha == null) {
                /* captchas currently disabled, see bayfiles.js */
                br.postPage("http://bayfiles.net/ajax_captcha", "action=getCaptcha");
                final String reCaptchaID = br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
                if (reCaptchaID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                Form dlForm = new Form();
                dlForm.setMethod(MethodType.POST);
                dlForm.setAction("http://bayfiles.net/ajax_captcha");
                dlForm.put("action", "verifyCaptcha");
                dlForm.put("token", token);
                rc.setForm(dlForm);
                rc.setId(reCaptchaID);
                rc.load();
                for (int i = 0; i <= 5; i++) {
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, downloadLink);
                    // Standard handling does not work here because submit
                    // fields
                    // are
                    // changed
                    dlForm = rc.getForm();
                    dlForm.put("challenge", rc.getChallenge());
                    dlForm.put("response", c);
                    br.submitForm(dlForm);
                    if (br.containsHTML(CAPTCHAFAILED)) {
                        rc.reload();
                        continue;
                    }
                    break;
                }
                if (br.containsHTML(CAPTCHAFAILED)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                token = br.getRegex("\"token\":\"(.*?)\"").getMatch(0);
                if (token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                sleep(wait * 1001l, downloadLink);
            }

            br.postPage("http://bayfiles.net/ajax_download", "action=getLink&vfid=" + vFid + "&token=" + token);
            dllink = br.getRegex("onclick=\"javascript:window\\.location\\.href = \\'(http://.*?)\\'").getMatch(0);
            if (dllink == null) dllink = getExactDllink();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, chunks);
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 2 * 60 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(<div id=\"ol\\-limited\">|class=\"page\\-download|>404 Not Found<)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 2 * 60 * 60 * 1000l);
            logger.warning("Unhandled error happened \n" + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            try {
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
                br.getPage("http://api.bayfiles.net/v1/account/login/" + Encoding.urlEncode(account.getUser()) + "/" + Encoding.urlEncode(account.getPass()));
                if (!br.containsHTML("\"error\":\"\"")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                if (br.getCookie(MAINPAGE, "SESSID") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
                account.setProperty("cookies", null);
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
        br.getPage("http://api.bayfiles.net/v1/account/info");
        if (!br.containsHTML("\"premium\":1")) {
            ai.setStatus("Free accounts are not supported!");
            account.setValid(false);
            return ai;
        }
        final Regex infoRegex = br.getRegex("\"files\":(\\d+),\"storage\":(\\d+),\"premium\":1,\"expires\":(\\d+)");
        String filesNum = infoRegex.getMatch(0);
        if (filesNum != null) ai.setFilesNum(Integer.parseInt(filesNum));
        String space = infoRegex.getMatch(1);
        if (space != null) ai.setUsedSpace(space.trim());
        ai.setUnlimitedTraffic();
        String expire = infoRegex.getMatch(2);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(Long.parseLong(expire) * 1000);
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
        String dllink = br.getRegex("\"(http://s\\d+\\.baycdn\\.net/dl/[a-z0-9]+/[a-z0-9]+/[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("<a class=\"highlighted\\-btn\" href=\"(http://[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getExactDllink() {
        return br.getRegex("(\\'|\")(http://s\\d+\\.baycdn\\.net/dl/[^<>\"]*?)(\\'|\")").getMatch(1);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}