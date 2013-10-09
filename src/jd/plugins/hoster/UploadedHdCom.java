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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.Formatter;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision: 20294 $", interfaceVersion = 2, names = { "uploadedhd.com" }, urls = { "http://(www\\.)?uploadedhd\\.com/[a-f0-9]{16}" }, flags = { 2 })
public class UploadedHdCom extends PluginForHost {

    private final String MAINPAGE = "http://www.uploadedhd.com";

    public UploadedHdCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/register.html");
    }

    // DTemplate modified heavily by raztoki
    // protocol: no https
    // captchatype: null
    // mods: everything

    private void setConstants(final Account account) {
        if (account != null && account.getBooleanProperty("free")) {
            // free account
            chunks = 1;
            resumes = true;
            acctype = "Free Account";
            directlinkproperty = "freelink2";
        } else if (account != null && !account.getBooleanProperty("free")) {
            // prem account
            chunks = 0; // chunk issues
            resumes = true;
            acctype = "Premium Account";
            directlinkproperty = "premlink";
        } else {
            // non account
            chunks = 1;
            resumes = true;
            acctype = "Non Account";
            directlinkproperty = "freelink";
        }
    }

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

    public boolean hasAutoCaptcha() {
        return false;
    }

    private final String preDlPass     = "<input type=\"password\"[^>]+name=\"filePassword\"";
    private final String premiumFile   = ">You must register for a premium account to download files of this size";
    private final String premiumSize   = "You+must+register+for+a+premium+account+to+download+files+of+this+size";
    private final String waitBetweenDl = "You+must+wait+10+minutes+between+downloads";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // English language, some reason you can't set this via cookie.
        br.getPage(MAINPAGE + "/index.php?_t=English+%28en%29");
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().matches(".+/register\\.(php|html).*?") || br.containsHTML(premiumFile)) {
            ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(this.getHost());
            Account account = null;
            if (accounts != null && accounts.size() != 0) {
                Iterator<Account> it = accounts.iterator();
                while (it.hasNext()) {
                    Account n = it.next();
                    if (n.isEnabled() && n.isValid()) {
                        account = n;
                        break;
                    }
                }
            }
            if (account != null) {
                login(account, false);
                // images send without html even within free accounts...
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(downloadLink.getDownloadURL());
                    if (con.getContentType().contains("html")) {
                        br.followConnection();
                    } else {
                        // is file... ?
                        downloadLink.setFinalFileName(getFileNameFromHeader(con));
                        downloadLink.setVerifiedFileSize(con.getLongContentLength());
                        isDownloadable = true;
                        return AvailableStatus.TRUE;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            } else {
                downloadLink.getLinkStatus().setStatusText(linkCheckAcc);
                try {
                    downloadLink.setComment(linkCheckAcc);
                } catch (Throwable e) {
                }
                return AvailableStatus.UNCHECKABLE;
            }
        }
        if (br.containsHTML(preDlPass)) {
            for (int i = 0; i != 2; i++) {
                Form password = null;
                Form[] forms = br.getForms();
                for (Form form : forms) {
                    if (form.containsHTML(preDlPass)) {
                        password = form;
                    }
                }
                String pass = downloadLink.getStringProperty("pass");
                if (pass == null) {
                    pass = Plugin.getUserInput("Pre Download Password Protection!", downloadLink);
                    if (pass == null || pass.equals("")) {
                        logger.info("User aborted/entered blank password");
                        return AvailableStatus.UNCHECKABLE;
                    }
                }
                password.put("filePassword", pass);
                br.submitForm(password);
                if (br.containsHTML(preDlPass)) {
                    continue;
                } else {
                    downloadLink.getStringProperty("pass", pass);
                    break;
                }
            }
        }
        if (br.getURL().contains(waitBetweenDl)) {
            downloadLink.getLinkStatus().setStatusText("Cannot check links when a downloadlimit is reached");
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.getURL().contains(premiumSize)) {
            downloadLink.getLinkStatus().setStatusText("");
            return AvailableStatus.TRUE;
        }
        if (br.getURL().matches(".+/(error|index)\\.(php|html).*?") || br.containsHTML(">File has been removed\\.<") || br.getRequest().getHttpConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fInfo = br.getRegex("<th class=\"descr\">[\t\n\r ]+<strong>([^<>\"]*?) \\((\\d+(\\.\\d+)? (KB|MB|GB))\\)<br/>");
        String filename = fInfo.getMatch(0);
        String filesize = fInfo.getMatch(1);
        // Maybe a multimedia file (audio/video)
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) \\- UploadedHD</title>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("Tamaño De Archivo:[\t\n\r ]+</td>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        setConstants(null);
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        if (isDownloadable) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), resumes, chunks);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                handleErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            // Happens if the user isn't logged in
            if (br.getURL().equals("http://www.uploadedhd.com/register.php")) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by registered/premium users");
            }
            // Happens when the user is logged in but isn't allowed to download a specified link
            if (br.getURL().contains(premiumSize)) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by registered/premium users");
            }
            if (br.getURL().contains(waitBetweenDl)) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            boolean captcha = false;
            int wait = 60;
            final String waittime = br.getRegex("\\$\\('\\.download-timer-seconds'\\)\\.html\\((\\d+)\\);").getMatch(0);
            if (waittime != null) wait = Integer.parseInt(waittime);
            sleep(wait * 1001l, downloadLink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL() + "?d=1", resumes, chunks);
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                handleErrors();
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    final String captchaAction = br.getRegex("<div class=\"captchaPageTable\">[\t\n\r ]+<form method=\"POST\" action=\"(http://[^<>\"]*?)\"").getMatch(0);
                    final String rcID = br.getRegex("recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
                    if (rcID == null || captchaAction == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.setId(rcID);
                    rc.load();
                    for (int i = 0; i <= 5; i++) {
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode(cf, downloadLink);
                        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaAction, "submit=continue&submitted=1&d=1&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c, resumes, chunks);
                        if (!dl.getConnection().isContentDisposition()) {
                            br.followConnection();
                            rc.reload();
                            continue;
                        }
                        break;
                    }
                    captcha = true;
                }
            }
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                handleErrors();
                if (captcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
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
                br.postPage(MAINPAGE + "/login.php", "loginUsername=" + Encoding.urlEncode(account.getUser()) + "&loginPassword=" + Encoding.urlEncode(account.getPass()) + "&submit=Login&submitme=1");
                // English language, some reason you can't set this via cookie and it switches after login again.. stupid site.
                if (br.getRedirectLocation() != null && !br.getRedirectLocation().contains("/account_home.php")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("/upgrade.php");
        ai.setUnlimitedTraffic(); // jdownloader
        final String expire = br.getRegex("(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expire != null) {
            // far as I'm aware all premium accounts have a expire date.. else we should also confirm via html
            ai.setStatus("Premium Account");
            account.setProperty("free", false);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd/MM/yyyy hh:mm:ss", null));
        } else {
            ai.setStatus("Free Account");
            account.setProperty("free", true);
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        setConstants(account);
        requestFileInformation(link);
        login(account, false);
        if (account.getBooleanProperty("free")) {
            br.getPage(link.getDownloadURL());
            doFree(link);
        }
        String dllink = link.getDownloadURL();
        if (!isDownloadable) {
            dllink = br.getRegex("(m4v|mp3): \"(http://[^<>\"]*?)\"").getMatch(1);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumes, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleErrors() throws PluginException {
        if (br.containsHTML(">Error: Could not open file for reading.<")) {
            logger.warning("Hoster Error : Could not open file for reading.");
            throw new PluginException(LinkStatus.ERROR_FATAL);
        } else if (br.getURL().contains("/error.html") || br.getURL().contains("/error.php")) {
            logger.warning("Hoster Error : Uncaught error by our plugin, please report this to JDownloader Development Team.");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    // ***************************************************************************************************** //
    // Components below don't require coder interaction, or configuration !

    private String        acctype            = null;
    private String        directlinkproperty = null;

    private final String  linkCheckAcc       = "Account required to check files online status";

    private int           chunks             = 1;

    private boolean       resumes            = false;
    private boolean       isDownloadable     = false;

    private static Object LOCK               = new Object();

    public void showAccountDetailsDialog(final Account account) {
        setConstants(account);
        AccountInfo ai = account.getAccountInfo();
        String message = "";
        message += "Account type: " + acctype + "\r\n";
        if (ai.getUsedSpace() != -1) message += "  Used Space: " + Formatter.formatReadable(ai.getUsedSpace()) + "\r\n";
        if (ai.getPremiumPoints() != -1) message += "Premium Points: " + ai.getPremiumPoints() + "\r\n";

        jd.gui.UserIO.getInstance().requestMessageDialog(this.getHost() + " Account", message);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("//uploadedhd", "//www.uploadedhd"));
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/terms.html";
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}