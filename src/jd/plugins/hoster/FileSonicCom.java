//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesonic.com" }, urls = { "http://[\\w\\.]*?(sharingmatrix|filesonic)\\.(com|net)/.*?file/([0-9]+(/.+)?|[a-z0-9]+/[0-9]+(/.+)?)" }, flags = { 2 })
public class FileSonicCom extends PluginForHost {

    private static final Object LOCK = new Object();
    private static final HashMap<String, String> freecookies = new HashMap<String, String>();
    private static String thisUrl = null;
    private static String nextUrl = null;
    private static String nextPass = null;

    public FileSonicCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filesonic.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.filesonic.com/contact-us";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        /* convert sharingmatrix to filesonic that set english language */
        String id = new Regex(link.getDownloadURL(), "/file/([0-9]+(/.+)?)").getMatch(0);
        if (id == null) id = new Regex(link.getDownloadURL(), "/file/[a-z0-9]+/([0-9]+(/.+)?)").getMatch(0);
        link.setUrlDownload("http://www.filesonic.com/file/" + id);
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @SuppressWarnings("unchecked")
    public void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            this.setBrowserExclusive();
            br.setDebug(true);
            br.setFollowRedirects(true);
            try {
                br.setCookie("http://www.filesonic.com/", "lang", "en");
                Object ret = account.getProperty("cookies", null);
                if (ret != null && ret instanceof HashMap<?, ?> && !force) {
                    HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (cookies.containsKey("role") && account.isValid()) {
                        for (String key : cookies.keySet()) {
                            br.setCookie("http://www.filesonic.com/", key, cookies.get(key));
                        }
                        return;
                    }
                }
                try {
                    br.getPage("http://www.filesonic.com/");
                    XMLRequest(br, "http://www.filesonic.com/user/login", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                } catch (Exception e) {                    
                }
                br.setFollowRedirects(false);
                String premCookie = br.getCookie("http://www.filesonic.com", "role");
                if (premCookie == null) {
                    AccountInfo ai = account.getAccountInfo();
                    if (ai == null) {
                        ai = new AccountInfo();
                        account.setAccountInfo(ai);
                    }
                    account.setProperty("cookies", null);
                    if (br.containsHTML("User Does Not Exist With")) {
                        ai.setStatus("User Does Not Exist With This Email Address");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    if (br.containsHTML("Provided password does not match")) {
                        ai.setStatus("Provided password does not match");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        ai.setStatus("ServerProblems(1), will try again in few minutes!");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                }
                if (premCookie.equalsIgnoreCase("free")) {
                    account.setProperty("cookies", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                HashMap<String, String> cookies = new HashMap<String, String>();
                Cookies add = br.getCookies("http://www.filesonic.com");
                for (Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("cookies", cookies);
            } finally {
                br.setFollowRedirects(false);
            }
        }
    }

    private void XMLRequest(Browser br, String url, String post) throws IOException {
        if (br == null) br = this.br;
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage(url, post);
        br.getHeaders().put("X-Requested-With", null);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        synchronized (LOCK) {
            AccountInfo ai = new AccountInfo();
            try {
                login(account, true);
            } catch (PluginException e) {
                if (account.getAccountInfo() != null) ai = account.getAccountInfo();
                if (e.getValue() == PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE) {
                    account.setValid(true);
                } else {
                    account.setValid(false);
                }
                return ai;
            }
            if (account.getAccountInfo() != null) ai = account.getAccountInfo();
            br.getPage("http://www.filesonic.com/user/settings");
            String expiredate = br.getRegex("settingsExpireDate\">(.*?)<").getMatch(0);
            if (expiredate != null) {
                ai.setStatus("Premium User");
                // it seems expire date is still wrong for many users
                // ai.setValidUntil(Regex.getMilliSeconds(expiredate,
                // "yyyy-MM-dd HH:mm:ss", null));
                ai.setValidUntil(-1);
                account.setValid(true);
                return ai;
            }
            account.setValid(false);
            return ai;
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(downloadLink);
        login(account, false);
        String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        String url = br.getRedirectLocation();
        if (url == null) {
            /* no redirect, what the frak */
            logger.warning(br.toString());
            AccountInfo ai = account.getAccountInfo();
            if (ai == null) {
                ai = new AccountInfo();
                account.setAccountInfo(ai);
            }
            ai.setStatus("ServerProblems(2), will try again in few minutes!");
            account.setProperty("cookies", null);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        /* first download try, without password */
        if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("This file is password protected")) {
                /* password handling */
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = getUserInput(null, downloadLink);
                } else {
                    /* get saved password */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                Form form = br.getForm(0);
                form.put("password", Encoding.urlEncode(passCode));
                /* second downloadtry with password */
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, 0);
            } else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.getConnection() != null && dl.getConnection().getContentType() != null && (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("unknown"))) {
            br.followConnection();
            errorHandling(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        if (parameter.getDownloadURL().contains(".net")) correctDownloadLink(parameter);
        this.setBrowserExclusive();
        br.setCookie("http://www.filesonic.com/", "lang", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Filename: </span> <strong>(.*?)<").getMatch(0);
        String filesize = br.getRegex("<span class=\"size\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String otherName = br.getRegex("<title>Download (.*?) for").getMatch(0);
        if (otherName != null && otherName.length() > filename.length()) {
            filename = otherName;
        }
        filesize = filesize.replace("&nbsp;", "");
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    private void errorHandling(DownloadLink downloadLink) throws PluginException {
        if (br.containsHTML("Free user can not download files")) throw new PluginException(LinkStatus.ERROR_FATAL, "Free user can not download files over 400MB");
        if (br.containsHTML("Download session in progress")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download session in progress", 10 * 60 * 1000l);
        if (br.containsHTML("This file is password protected")) {
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
        }
        if (br.containsHTML("An Error Occurred")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 20 * 60 * 1000l); }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        String downloadUrl = null;
        String passCode = null;
        boolean triedSavedURL = false;
        synchronized (freecookies) {
            if (freecookies.size() > 0 && nextUrl != null) {
                this.setBrowserExclusive();
                for (String key : freecookies.keySet()) {
                    br.setCookie("http://www.filesonic.com/", key, freecookies.get(key));
                }
                downloadUrl = nextUrl;
                nextUrl = null;
                freecookies.clear();
                passCode = nextPass;
                nextPass = null;
            }
            if (!downloadLink.getDownloadURL().equalsIgnoreCase(thisUrl)) {
                downloadUrl = null;
            }
            thisUrl = null;
        }
        br.forceDebug(true);
        if (downloadUrl == null) {
            requestFileInformation(downloadLink);
            passCode = null;
            String id = new Regex(downloadLink.getDownloadURL(), "file/(\\d+)").getMatch(0);
            br.setFollowRedirects(true);
            br.getPage("http://www.filesonic.com/download-free/" + id);
            errorHandling(downloadLink);
            if (br.containsHTML("This file is password protected")) {
                /* password handling */
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = getUserInput(null, downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                Form form = br.getForm(0);
                form.put("password", Encoding.urlEncode(passCode));
                br.submitForm(form);

            }

            downloadUrl = br.getRegex("downloadUrl = \"(http://.*?)\"").getMatch(0);
            if (downloadUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String countDownDelay = br.getRegex("countDownDelay = (\\d+)").getMatch(0);
            if (countDownDelay != null && freecookies.size() == 0) {
                /*
                 * we have to wait a little longer than needed cause its not
                 * exactly the same time
                 */
                if (Long.parseLong(countDownDelay) > 300) {
                    Cookies add = br.getCookies("http://www.filesonic.com");
                    synchronized (freecookies) {
                        for (Cookie c : add.getCookies()) {
                            freecookies.put(c.getKey(), c.getValue());
                        }
                        nextUrl = downloadUrl;
                        nextPass = passCode;
                        thisUrl = downloadLink.getDownloadURL();
                    }
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Long.parseLong(countDownDelay) + 120) * 1001l);
                }
                this.sleep((Long.parseLong(countDownDelay)) * 1001, downloadLink);
            }
        } else {
            triedSavedURL = true;
        }
        /*
         * limited to 1 chunk at the moment cause don't know if its a server
         * error that more are possible and resume should also not work ;)
         */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, 1);
        if (dl.getConnection() != null && dl.getConnection().getContentType() != null && (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("unknown"))) {
            br.followConnection();
            if (triedSavedURL) throw new PluginException(LinkStatus.ERROR_RETRY);
            errorHandling(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}