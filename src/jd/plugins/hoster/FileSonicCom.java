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

import java.io.File;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesonic.com" }, urls = { "http://[\\w\\.]*?(sharingmatrix|filesonic)\\.(com|net)/.*?file/([0-9]+(/.+)?|[a-z0-9]+/[0-9]+(/.+)?)" }, flags = { 2 })
public class FileSonicCom extends PluginForHost {

    private static final Object LOCK               = new Object();
    private static long         LAST_FREE_DOWNLOAD = 0l;

    public FileSonicCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filesonic.com/premium");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* convert sharingmatrix to filesonic that set english language */
        String id = new Regex(link.getDownloadURL(), "/file/([0-9]+(/.+)?)").getMatch(0);
        if (id == null) {
            id = new Regex(link.getDownloadURL(), "/file/[a-z0-9]+/([0-9]+(/.+)?)").getMatch(0);
        }
        link.setUrlDownload("http://www.filesonic.com/file/" + id);
    }

    private void errorHandling(final DownloadLink downloadLink) throws PluginException {
        if (this.br.containsHTML("Free user can not download files")) {
            //
            throw new PluginException(LinkStatus.ERROR_FATAL, "Free user can not download files over 400MB");
        }
        if (this.br.containsHTML("Download session in progress")) {
            //
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download session in progress", 10 * 60 * 1000l);
        }
        if (this.br.containsHTML("This file is password protected")) {
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
        }
        if (this.br.containsHTML("An Error Occurred")) {

            //
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 20 * 60 * 1000l);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (FileSonicCom.LOCK) {
            AccountInfo ai = new AccountInfo();
            try {
                this.login(account, true);
            } catch (final PluginException e) {
                if (account.getAccountInfo() != null) {
                    ai = account.getAccountInfo();
                }
                if (e.getValue() == PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE) {
                    account.setValid(true);
                } else {
                    account.setValid(false);
                }
                return ai;
            }

            if (account.getAccountInfo() != null) {
                ai = account.getAccountInfo();
            }
            this.br.getPage("http://www.filesonic.com/user/settings");
            final String expiredate = this.br.getRegex("settingsExpireDate\">(.*?)<").getMatch(0);
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
    public String getAGBLink() {
        return "http://www.filesonic.com/contact-us";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        String downloadUrl = null;
        String passCode = null;
        passCode = null;
        this.br.setCookiesExclusive(false);

        this.br.forceDebug(true);
        // we have to enter captcha before we get ip_blocked_state
        // we do this timeing check to avoid this
        final long waited = System.currentTimeMillis() - FileSonicCom.LAST_FREE_DOWNLOAD;
        if (FileSonicCom.LAST_FREE_DOWNLOAD > 0 && waited < 300000) {
            FileSonicCom.LAST_FREE_DOWNLOAD = 0;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 600000 - waited);
        }
        this.requestFileInformation(downloadLink);
        passCode = null;

        final String id = new Regex(downloadLink.getDownloadURL(), "file/(\\d+)").getMatch(0);
        this.br.postPage("http://www.filesonic.com/file/" + id + "?start=1", "");
        if (br.containsHTML("The file that you're trying to download is larger than")) {
            String size = br.getRegex("trying to download is larger than (.*?)\\. <a href=\"").getMatch(0);
            if (size != null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Only premium users can download files larger than " + size.trim() + ".");
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for premium users!");
            }
        }
        this.removeComments();

        if (this.br.containsHTML("href='\\?cancelDownload=1\\&start=1'>")) {
            Plugin.logger.info("Cancel running filesonic downloads");
            this.br.getPage("http://www.filesonic.com/file/" + id + "?cancelDownload=1&start=1");
            // this.br.postPage("http://www.filesonic.com/file/" + id +
            // "?start=1", "");
            this.removeComments();
        }
        this.br.setFollowRedirects(true);
        // download is ready already
        downloadUrl = this.br.getRegex("<p><a href=\"(http://.*?\\.filesonic.com.*?)\"><span>Start download now!</span></a></p>").getMatch(0);
        if (downloadUrl == null) {
            this.errorHandling(downloadLink);
            // downloadUrl =
            // this.br.getRegex("downloadUrl = \"(http://.*?)\"").getMatch(0);
            // if (downloadUrl == null) { throw new
            // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            final String countDownDelay = this.br.getRegex("countDownDelay = (\\d+)").getMatch(0);
            if (countDownDelay != null) {
                /*
                 * we have to wait a little longer than needed cause its not
                 * exactly the same time
                 */
                if (Long.parseLong(countDownDelay) > 300) {

                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(countDownDelay) * 1001l); }
                this.sleep(Long.parseLong(countDownDelay) * 1001, downloadLink);
                this.br.postPage("http://www.filesonic.com/file/" + id + "?start=1", "");
                this.removeComments();
            }
            if (this.br.containsHTML("Recaptcha\\.create")) {
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(this.br);
                rc.parse();
                // rc = rc;
                rc.load();
                final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
                final String code = this.getCaptchaCode(cf, downloadLink);
                rc.setCode(code);
                if (this.br.containsHTML("Recaptcha\\.create")) {

                    // captcha wrong; fs does not return any error message.
                    // just shows the captcha dialog again
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
            if (this.br.containsHTML("This file is password protected")) {
                /* password handling */
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(null, downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                final Form form = this.br.getForm(0);
                form.put("password", Encoding.urlEncode(passCode));
                this.br.submitForm(form);

            }
            downloadUrl = this.br.getRegex("<p><a href=\"(http://.*?\\.filesonic.com.*?)\"><span>Start download now!</span></a></p>").getMatch(0);
        }
        if (downloadUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /*
         * limited to 1 chunk at the moment cause don't know if its a server
         * error that more are possible and resume should also not work ;)
         */
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadUrl, true, 1);
        if (this.dl.getConnection() != null && this.dl.getConnection().getContentType() != null && (this.dl.getConnection().getContentType().contains("html") || this.dl.getConnection().getContentType().contains("unknown"))) {
            this.br.followConnection();

            this.errorHandling(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        this.dl.setFilenameFix(true);
        this.dl.startDownload();
        FileSonicCom.LAST_FREE_DOWNLOAD = System.currentTimeMillis();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String passCode = null;
        this.setBrowserExclusive();
        this.requestFileInformation(downloadLink);
        this.login(account, false);
        final String dllink = downloadLink.getDownloadURL();
        this.br.getPage(dllink);
        final String url = this.br.getRedirectLocation();
        if (url == null) {
            /* no redirect, what the frak */
            Plugin.logger.warning(this.br.toString());
            AccountInfo ai = account.getAccountInfo();
            if (ai == null) {
                ai = new AccountInfo();
                account.setAccountInfo(ai);
            }
            ai.setStatus("ServerProblems(2), will try again in few minutes!");
            account.setProperty("cookies", null);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        this.br.setFollowRedirects(true);
        this.br.setDebug(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, 0);
        /* first download try, without password */
        if (this.dl.getConnection() != null && this.dl.getConnection().getContentType() != null && this.dl.getConnection().getContentType().contains("html")) {
            this.br.followConnection();
            if (this.br.containsHTML("This file is password protected")) {
                /* password handling */
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(null, downloadLink);
                } else {
                    /* get saved password */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                final Form form = this.br.getForm(0);
                form.put("password", Encoding.urlEncode(passCode));
                /* second downloadtry with password */

                // 1 chunk because of bug #2478
                // http://svn.jdownloader.org/issues/2478
                this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, form, true, 1);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (this.dl.getConnection() != null && this.dl.getConnection().getContentType() != null && (this.dl.getConnection().getContentType().contains("html") || this.dl.getConnection().getContentType().contains("unknown"))) {
            this.br.followConnection();
            this.errorHandling(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        this.dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    public void login(final Account account, final boolean force) throws Exception {
        synchronized (FileSonicCom.LOCK) {
            this.setBrowserExclusive();
            this.br.setDebug(true);
            this.br.setFollowRedirects(true);
            try {
                this.br.setCookie("http://www.filesonic.com/", "lang", "en");
                final Object ret = account.getProperty("cookies", null);
                if (ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (cookies.containsKey("role") && account.isValid()) {
                        for (final String key : cookies.keySet()) {
                            this.br.setCookie("http://www.filesonic.com/", key, cookies.get(key));
                        }
                        return;
                    }
                }
                try {
                    this.br.getPage("http://www.filesonic.com/");
                    this.XMLRequest(this.br, "http://www.filesonic.com/user/login", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                } catch (final Exception e) {
                }
                this.br.setFollowRedirects(false);
                final String premCookie = this.br.getCookie("http://www.filesonic.com", "role");
                if (premCookie == null) {
                    AccountInfo ai = account.getAccountInfo();
                    if (ai == null) {
                        ai = new AccountInfo();
                        account.setAccountInfo(ai);
                    }
                    ai.setStatus(null);
                    account.setProperty("cookies", null);
                    if (this.br.containsHTML("You must be logged in") && this.br.containsHTML("If you don")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                    if (this.br.containsHTML("User Does Not Exist With")) {
                        ai.setStatus("User Does Not Exist With This Email Address");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    if (this.br.containsHTML("Provided password does not match")) {
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
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://www.filesonic.com");
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("cookies", cookies);
            } finally {
                this.br.setFollowRedirects(false);
            }
        }
    }

    private void removeComments() throws CharacterCodingException {
        this.br.getRequest().setHtmlCode(this.br.getRequest().getHtmlCode().replaceAll("<\\!\\-\\-.*?\\-\\-\\>", ""));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws Exception {
        if (parameter.getDownloadURL().contains(".net")) {
            this.correctDownloadLink(parameter);
        }

        this.br.setCookie("http://www.filesonic.com/", "lang", "en");
        this.br.getPage(parameter.getDownloadURL());
        if (this.br.getRedirectLocation() != null) {
            this.br.getPage(this.br.getRedirectLocation());
        }
        if (this.br.containsHTML("File not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = this.br.getRegex("Filename: </span> <strong>(.*?)<").getMatch(0);
        String filesize = this.br.getRegex("<span class=\"size\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String otherName = this.br.getRegex("<title>Download (.*?) for").getMatch(0);
        if (otherName != null && otherName.length() > filename.length()) {
            filename = otherName;
        }
        filesize = filesize.replace("&nbsp;", "");
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void XMLRequest(Browser br, final String url, final String post) throws IOException {
        if (br == null) {
            br = this.br;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage(url, post);
        br.getHeaders().put("X-Requested-With", null);
    }
}