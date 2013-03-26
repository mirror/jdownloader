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
import java.util.Locale;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharedbit.net" }, urls = { "https?://(www\\.)?sharedbit\\.net/(file/\\d+|[a-z0-9]+)" }, flags = { 2 })
public class SharedBitNet extends PluginForHost {

    public SharedBitNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://sharedbit.net/premium");
    }

    public void correctDownloadLink(DownloadLink link) {
        // They force https anyways
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    @Override
    public String getAGBLink() {
        return "https://sharedbit.net/terms";
    }

    private static final String MAINPAGE                  = "http://sharedbit.net";
    private static Object       LOCK                      = new Object();
    private static final String PREMIUMONLY               = "Free downloading restricted by uploader";
    private static final String PREMIUMONLYUSERTEXT       = JDL.L("plugins.hoster.sharedbitnet.only4premium", "Only downloadable for premium users");
    private static final String PRIVATE                   = "class=\"msg private bad\"";
    private static final String PRIVATEUSERTEXT           = JDL.L("plugins.hoster.sharedbitnet.privatelink", "This is a private link!");
    private static final String PASSWORDPROTECTED         = ">Download Link Password:<";
    private static final String PASSWORDPROTECTEDUSERTEXT = JDL.L("plugins.hoster.sharedbitnet.passwordprotected", "This link is password protected!");
    private Browser             AJAX                      = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        // Convert old to new links
        if (link.getDownloadURL().matches("https?://(www\\.)?sharedbit\\.net/file/\\d+")) {
            final String newLink = br.getRedirectLocation();
            if (newLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (newLink.equals("https://sharedbit.net/error404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            link.setUrlDownload(newLink);
            br.getPage(link.getDownloadURL());
        }
        if (br.containsHTML(">Page is not Found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(PREMIUMONLY)) {
            link.getLinkStatus().setStatusText(PREMIUMONLYUSERTEXT);
        } else if (br.containsHTML(PRIVATE)) {
            link.getLinkStatus().setStatusText(PRIVATEUSERTEXT);
            link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
            return AvailableStatus.TRUE;
        } else if (br.containsHTML(PASSWORDPROTECTED)) {
            link.getLinkStatus().setStatusText(PASSWORDPROTECTEDUSERTEXT);
        }
        String filename = br.getRegex("<span class=\"name\">([^<>\"]*?)</span>").getMatch(0);
        String filesize = br.getRegex("<span class=\"size\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String passCode = null;
        final String fid = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        AJAX = br.cloneBrowser();

        if (br.containsHTML(PASSWORDPROTECTED)) {
            passCode = handlePassword(downloadLink, fid);
        }
        if (br.containsHTML(PREMIUMONLY)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
        } else if (br.containsHTML(PRIVATE)) throw new PluginException(LinkStatus.ERROR_FATAL, PRIVATEUSERTEXT);
        String code = null;
        for (int i = 0; i <= 3; i++) {
            final String captchaLink = br.getRegex("\"(/captcha/\\d+/dl[^<>\"]*?)\"").getMatch(0);
            if (captchaLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            code = getCaptchaCode("https://sharedbit.net" + captchaLink, downloadLink);
            ajaxPost("https://sharedbit.net/dl-process/link", "dlid=&hash=" + fid + "&captcha=" + code);
            if (AJAX.containsHTML("\"error\":\"\",\"captcha\":1")) continue;
            break;
        }
        if (AJAX.containsHTML("\"error\":\"\",\"captcha\":1")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (AJAX.containsHTML("\"error\":\"other\"")) {
            logger.warning("FATAL unknown error happened");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (AJAX.containsHTML("\"error\":\"bound\"")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        if (AJAX.containsHTML("\"error\":\"parallel\"")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Parallel downloads are not allowed for freeusers", 3 * 60 * 1000l);
        final String dlid = AJAX.getRegex("\"dlid\":\"([^<>\"]*?)\"").getMatch(0);
        if (dlid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        waitTime(System.currentTimeMillis(), downloadLink);
        ajaxPost("https://sharedbit.net/dl-process/link", "dlid=" + dlid + "&hash=" + fid + "&captcha=" + code);

        String dllink = AJAX.getRegex("\"link\":\"(http:[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        dl.startDownload();
    }

    private void ajaxPost(final String url, final String postData) throws IOException, PluginException {
        AJAX.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        AJAX.postPage(url, postData);
        if (AJAX.containsHTML("blocked\":1")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Your IP is banned!", 60 * 80 * 1000l);
    }

    private void waitTime(long timeBefore, final DownloadLink downloadLink) throws PluginException {
        int wait = 60;
        final String waittime = AJAX.getRegex("\"timer\":\"(\\d+)\"").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 2;
        /** Ticket Time */
        wait -= passedTime;
        if (wait > 0) sleep(wait * 1000l, downloadLink);
    }

    private String handlePassword(final DownloadLink thelink, final String fid) throws IOException, PluginException {
        String passCode = thelink.getStringProperty("pass", null);
        if (passCode == null) passCode = Plugin.getUserInput("Password?", thelink);
        String postData = "captcha=&hash=" + fid + "&pass=" + Encoding.urlEncode(passCode);
        // Not yet finished
        // final String passCaptcha =
        // br.getRegex("\"(/captcha/\\d+/filepass)\"").getMatch(0);
        // if (passCaptcha != null) {
        // final String code = getCaptchaCode("https://sharedbit.net" +
        // passCaptcha, thelink);
        // postData += "&captcha=" + Encoding.urlEncode(code);
        // }
        AJAX.postPage("https://sharedbit.net/dl-process/pass", postData);
        if (AJAX.containsHTML("\"name\":\"eauth\"")) throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
        // if (AJAX.containsHTML("\"captcha\":\"ecaptcha\"")) throw new
        // PluginException(LinkStatus.ERROR_CAPTCHA);
        br.getPage(thelink.getDownloadURL());
        return passCode;
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
                final Browser ajax = br.cloneBrowser();
                ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                ajax.postPage("https://sharedbit.net/post/auth", "type=mini&landing=%2F&captcha=&name=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (ajax.containsHTML("\"pass\":\"eauth\"") || ajax.getCookie(MAINPAGE, "usid") == null || ajax.getCookie(MAINPAGE, "uex") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                if (!ajax.containsHTML("\"premium\":1")) {
                    logger.info("This is no premium account, aborting...");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // ajax.postPage("https://sharedbit.net/post/cookie", "");
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
        br.getPage("https://sharedbit.net/account");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("class=\"until\">valid until \\&#151; <b>([^<>\"]*?)</b></span>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd, MMMM yyyy", Locale.ENGLISH));
        }
        final String usedSpace = getData("Storage Used");
        if (usedSpace != null) ai.setUsedSpace(SizeFormatter.getSize(usedSpace));
        final String filesnum = getData("Number of files");
        if (filesnum != null) ai.setFilesNum(Integer.parseInt(filesnum));
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    private String getData(final String parameter) {
        return br.getRegex("<td>" + parameter + "</td>[\t\n\r ]+<td class=\"val\"><b>([^<>\"]*?)</b></td>").getMatch(0);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (br.containsHTML(PASSWORDPROTECTED)) {
            final String fid = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            AJAX = br.cloneBrowser();
            handlePassword(link, fid);
            br.getPage(link.getDownloadURL());
            dllink = br.getRedirectLocation();
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, -5);
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