//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Locale;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dizzcloud.com" }, urls = { "http://(www\\.)?dizzcloud\\.com/dl/(?!robots)[a-z0-9]+" }, flags = { 2 })
public class DizzCloudCom extends PluginForHost {

    public DizzCloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://dizzcloud.com/upgrade");
    }

    @Override
    public String getAGBLink() {
        return "http://dizzcloud.com/tos";
    }

    private final String PREMIUMONLY = ">Only premium users can download this file|disabled the ability to free download a file larger than  \\d+ Mb\\.<|>File owner has disabled<";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File not found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<span>File: </span>([^<>\"]*?)</div>").getMatch(0);
        if (filename == null) filename = br.getRegex("file-name\">([^<>\"]*?)\\[").getMatch(0);
        if (filename == null) filename = br.getRegex(">File: </div>([^<>\"]*?) <span").getMatch(0);
        if (filename == null) filename = br.getRegex("class=\"name\">([^<>\"]*?)</div>").getMatch(0);
        String filesize = br.getRegex("id=\"file\\-size\"><span>Size: </span>([^<>\"]*?)</div>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("file-name\">[^<>\"]*?\\[([^<>\"]*?)\\]").getMatch(0);
        if (filesize == null) filesize = br.getRegex("line-height: 26px;\">\\[([^<>\"]*?)\\]").getMatch(0);
        if (filesize == null) filesize = br.getRegex("id=\"file-size\">([^<>\"]*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            if (br.containsHTML(PREMIUMONLY)) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
            }
            final String waittime = br.getRegex(">Next free download from your ip will be available in <b>(\\d+) minutes</p>").getMatch(0);
            if (waittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 60 * 1001l);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            final String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            rc.setId(id);
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(cf, downloadLink);
            br.getPage("http://dizzcloud.com/dl/" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + "?type=recaptcha&challenge=" + rc.getChallenge() + "&capture=" + Encoding.urlEncode(c));
            if (br.containsHTML("\"Entered digits are incorrect")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dllink = br.getRegex("\"href\":\"(http:[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = dllink.replace("\\", "");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
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

    private static final String MAINPAGE = "http://dizzcloud.com";
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
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.postPage("http://dizzcloud.com/login", "email=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "auth_hash") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
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
            throw e;
        }
        br.getPage("http://dizzcloud.com/");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex(">Premium till.*?(\\d+\\.\\d+\\.\\d+).*?\\&nbsp;\\&nbsp;</span>").getMatch(0);
        if (expire == null) {
            logger.info("JD could not detect account expire time, your account has been determined as a free account");
            account.setProperty("free", true);
            ai.setStatus("Free User");
        } else {
            account.setProperty("free", false);
            if ("11.11.2222".equals(expire)) {
                ai.setValidUntil(-1);
                ai.setStatus("Lifetime Premium User");
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy", Locale.ENGLISH));
                ai.setStatus("Premium User");
            }
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account, false);
        if (account.getBooleanProperty("free")) {
            br.getPage(downloadLink.getDownloadURL());
            // if the cached cookie expired, relogin.
            if (br.getCookie(MAINPAGE, "auth_hash") == null) {
                synchronized (LOCK) {
                    account.setProperty("cookies", Property.NULL);
                    // if you retry, it can use another account...
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            doFree(downloadLink);
        } else {
            br.setFollowRedirects(false);
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML("class=\"daydllimit\"")) {
                logger.info("daily limit reached, temp disabling premium");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            String dllink = br.getRedirectLocation();
            if (dllink == null) dllink = br.getRegex("\"(http://[a-z0-9\\-]+\\.cloudstoreservice\\.net/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://[^<>\"]*?)\" class=\"orange\\-btn\">DOWNLOAD</a>").getMatch(0);
            if (dllink == null) {
                br.postPage(downloadLink.getDownloadURL(), "getlnk=1");
                dllink = br.getRegex("msg\":\"(http.*?)\"").getMatch(0);
                if (dllink != null) dllink = dllink.replace("\\", "");
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, Encoding.htmlDecode(dllink), true, -10);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                if (br.containsHTML("Dynamic ip error")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                // Premium traffic is gone or expired -> Reverts to free account
                // -> Disable it
                if (br.containsHTML(PREMIUMONLY)) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}