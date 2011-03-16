//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.parser.html.HTMLParser;
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

/**
 * TODO: Support für andere Linkcards(bestimmte Anzahl Downloads,unlimited usw)
 * einbauen
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megashares.com" }, urls = { "http://[\\w\\.]*?(d[0-9]{2}\\.)?megashares\\.com/(.*\\?d[0-9]{2}=[0-9a-zA-Z]{7}|dl/[0-9a-zA-Z]{7}/)" }, flags = { 2 })
public class MegasharesCom extends PluginForHost {

    private final String UserAgent = "JD_" + "$Revision$";
    private static final Object LOCK = new Object();

    public MegasharesCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.megashares.com/lc_order.php?tid=sasky");
    }

    @SuppressWarnings("unchecked")
    private void login(Account account) throws IOException, PluginException {
        synchronized (LOCK) {
            this.setBrowserExclusive();
            br.getHeaders().put("User-Agent", UserAgent);
            br.setFollowRedirects(true);
            Object ret = account.getProperty("cookies", null);
            if (ret != null && ret instanceof HashMap<?, ?>) {
                logger.info("Use cookie login");
                /* use saved cookies */
                HashMap<String, String> cookies = (HashMap<String, String>) ret;
                for (String key : cookies.keySet()) {
                    br.setCookie("http://megashares.com/", key, cookies.get(key));
                }
            } else {
                logger.info("Use website login");
                /* get new cookies */
                String pw = account.getPass();
                if (pw.length() > 32) {
                    pw = pw.substring(0, 32);
                }
                br.getPage("http://d01.megashares.com/");
                br.postPage("http://d01.megashares.com/myms_login.php", "mymslogin_name=" + Encoding.urlEncode(account.getUser()) + "&mymspassword=" + Encoding.urlEncode(pw) + "&myms_login=Login");
            }
            if (br.getCookie("http://megashares.com", "myms") == null) {
                /* invalid account */
                account.setProperty("cookies", null);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                /* valid account */
                HashMap<String, String> cookies = new HashMap<String, String>();
                Cookies add = br.getCookies("http://megashares.com/");
                for (Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("cookies", cookies);
            }
        }

    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        synchronized (LOCK) {
            try {
                login(account);
            } catch (PluginException e) {
                account.setProperty("cookies", null);
                account.setValid(false);
                if (br.containsHTML("PIN is incorrect")) {
                    ai.setStatus("Error during login - PIN is incorrect");
                } else if (br.containsHTML("Invalid Username")) {
                    ai.setStatus("Check username or if you using linkcard for login then the card may expired or has no more links");
                } else {
                    ai.setStatus("Please use *My Megashares* Account!Create one and link with your linkcard");
                }
                return ai;
            }
            if (br.getURL() == null || !br.getURL().endsWith("myms.php")) br.getPage("http://d01.megashares.com/myms.php");
            String validUntil = br.getRegex("premium_info_box\">Period Ends:(.*?)<").getMatch(0);
            if (validUntil == null) {
                account.setProperty("cookies", null);
                ai.setStatus("Account invalid");
                account.setValid(false);
            } else {
                account.setValid(true);
                ai.setStatus("Account ok");
                /* the whole day valid? */
                ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil.trim(), "MMM dd, yyyy", null) + (1000l * 60 * 60 * 24));
            }
            /* TODO: there can be many different kind of linkcards */
            return ai;
        }
    }

    public void loadpage(String url) throws IOException {
        boolean tmp = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        br.getPage(url);
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        br.setFollowRedirects(tmp);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws IOException {
        String url = link.getDownloadURL();
        String id = null;
        if (url.contains("/dl/")) {
            id = new Regex(url, "/dl/([a-zA-Z0-9]+)").getMatch(0);
        } else {
            id = new Regex(url, "\\?d[0-9]{2}=([0-9a-zA-Z]+)").getMatch(0);
        }
        if (id != null) {
            url = "http://d01.megashares.com/?d01=" + id;
        }
        link.setUrlDownload(url);
    }

    private String findDownloadUrl() {
        String url = br.getRegex("<div>\\s*?<a href=\"(http://.*?megashares.*?)\">").getMatch(0);
        if (url == null) url = br.getRegex("<div id=\"show_download_button(_\\d+)?\".*?>[^<]*?<a href=\"(http://.*?megashares.*?)\">").getMatch(1);
        return url;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        synchronized (LOCK) {
            login(account);
            // Password protection
            loadpage(downloadLink.getDownloadURL());
            if (!checkPassword(downloadLink)) { return; }
            if (br.containsHTML("All download slots for this link are currently filled")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            if (br.containsHTML("Your Download Passport is") || br.containsHTML("Your Passport needs to be reactivated.") || br.containsHTML("You have reached.*?maximum download limit") || br.containsHTML("You already have the maximum")) {
                /* invalid account? try again */
                logger.info("No Premium? try again");
                account.setProperty("cookies", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        String url = findDownloadUrl();
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, -10);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.getHttpConnection().getLongContentLength() == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
            /*
             * seems like megashares sends empty page when last download was
             * some secs ago
             */
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
            /* maybe we have to fix link */
            correctDownloadLink(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleErrors(DownloadLink link) throws PluginException {
        // Sie laden gerade eine datei herunter
        if (br.containsHTML("You already have the maximum")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000l); }
        String[] dat = br.getRegex("Your download passport will renew.*?in.*?(\\d+).*?:.*?(\\d+).*?:.*?(\\d+)</strong>").getRow(0);
        if (br.containsHTML("You have reached.*?maximum download limit")) {
            long wait = Long.parseLong(dat[1]) * 60000l + Long.parseLong(dat[2]) * 1000l;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait);
        }
        if (br.containsHTML("All download slots for this link are currently filled")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.megasharescom.errors.allslotsfilled", "Cannot check, because all slots filled"), 10 * 60 * 1000l);

    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // Password protection
        if (!checkPassword(downloadLink)) { return; }
        handleErrors(downloadLink);
        // Reconnet/wartezeit check

        // Captchacheck
        if (br.containsHTML("Your Passport needs to be reactivated.")) {
            String captchaAddress = br.getRegex("then hit the \"Reactivate Passport\" button\\.</dt>.*?<dd><img src=\"(.*?)\"").getMatch(0);
            if (captchaAddress == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            HashMap<String, String> input = HTMLParser.getInputHiddenFields(br + "");

            String code = getCaptchaCode(captchaAddress, downloadLink);
            String geturl = downloadLink.getDownloadURL() + "&rs=check_passport_renewal&rsargs[]=" + code + "&rsargs[]=" + input.get("random_num") + "&rsargs[]=" + input.get("passport_num") + "&rsargs[]=replace_sec_pprenewal&rsrnd=" + System.currentTimeMillis();
            br.getPage(geturl);
            requestFileInformationInternal(downloadLink);
            if (!checkPassword(downloadLink)) { return; }
            handleErrors(downloadLink);
        }
        // Downloadlink
        String url = findDownloadUrl();
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Dateigröße holen
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            /*
             * seems like megashares sends empty page when last download was
             * some secs ago
             */
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
            if (br.getHttpConnection().getLongContentLength() == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
            if (br.getHttpConnection().toString().contains("Get a link card now")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
            if (br.getHttpConnection().toString().contains("Your Passport needs to")) throw new PluginException(LinkStatus.ERROR_RETRY);
            /* maybe we have to fix link */
            correctDownloadLink(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dl.startDownload()) {
            downloadLink.getLinkStatus().setRetryCount(0);
        }
    }

    private boolean checkPassword(DownloadLink link) throws Exception {

        if (br.containsHTML("This link requires a password")) {
            Form form = br.getFormBySubmitvalue("Validate+Password");
            String pass = link.getStringProperty("pass");
            if (pass != null) {
                form.put("passText", pass);
                br.submitForm(form);
                if (!br.containsHTML("This link requires a password")) { return true; }
            }
            int i = 0;
            while ((i++) < 5) {
                pass = Plugin.getUserInput(JDL.LF("plugins.hoster.passquestion", "Link '%s' is passwordprotected. Enter password:", link.getName()), link);
                if (pass != null) {
                    form.put("passText", pass);
                    br.submitForm(form);
                    if (!br.containsHTML("This link requires a password")) {
                        link.setProperty("pass", pass);
                        return true;
                    }
                }
            }
            link.setProperty("pass", null);
            link.getLinkStatus().addStatus(LinkStatus.ERROR_FATAL);
            link.getLinkStatus().setErrorMessage("Link password wrong");
            return false;
        }
        return true;
    }

    @Override
    public String getAGBLink() {
        return "http://d01.megashares.com/tos.php";
    }

    public void renew(Browser br, int buttonID) throws IOException {
        Browser brc = br.cloneBrowser();
        String renew[] = br.getRegex("\"renew\\('(.*?)','(.*?)'").getRow(0);
        String post = br.getRegex("renew\\.php'.*?\\{(.*?):").getMatch(0);
        if (post != null) post = post.trim();
        if (renew == null || renew.length != 2) return;
        if (buttonID <= 0) {
            brc.postPage("/renew.php", post + "=" + renew[0]);
        } else {
            brc.postPage("/renew.php", post + "=" + renew[0] + "&button_id=" + buttonID);
        }
        br.getPage(renew[1]);
        brc = null;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setDebug(true);
        return requestFileInformationInternal(downloadLink);
    }

    private AvailableStatus requestFileInformationInternal(DownloadLink downloadLink) throws IOException, PluginException {
        br.getHeaders().put("User-Agent", UserAgent);
        loadpage(downloadLink.getDownloadURL());
        /* new filename, size regex */
        String fln = br.getRegex("FILE Download.*?>.*?>(.*?)<").getMatch(0);
        String dsize = br.getRegex("FILE Download.*?>.*?>.*?>(\\d+.*?)<").getMatch(0);
        try {
            renew(br, 0);
            if (br.containsHTML("class=\"order_push_box_left(_2)?\">")) {
                renew(br, 1);
            }
            if (br.containsHTML("Invalid link")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("You already have the maximum")) {
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.megasharescom.errors.alreadyloading", "Cannot check, because aready loading file"));
                return AvailableStatus.UNCHECKABLE;
            }
            if (br.containsHTML("All download slots for this link are currently filled")) {
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.megasharescom.errors.allslotsfilled", "Cannot check, because all slots filled"));
                return AvailableStatus.UNCHECKABLE;
            }
            if (br.containsHTML("This link requires a password")) {
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.megasharescom.errors.passwordprotected", "Password protected download"));
                return AvailableStatus.UNCHECKABLE;
            }
            if (br.containsHTML("This link is currently offline")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This link is currently offline for scheduled maintenance, please try again later", 60 * 60 * 1000l);
            /* fallback regex */
            if (dsize == null) dsize = br.getRegex("Filesize:</span></strong>(.*?)<br />").getMatch(0);
            if (fln == null) fln = br.getRegex("download page link title.*?<h1 class=.*?>(.*?)<").getMatch(0);
            if (dsize == null || fln == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String betterfln = br.getRegex("download page link title.*?class=.*?style=.*?title=\"(.*?)\"").getMatch(0);
            if ((fln.endsWith("...") && betterfln != null) || (betterfln != null && betterfln.length() >= fln.length())) {
                fln = betterfln;
            }
            return AvailableStatus.TRUE;
        } finally {
            if (dsize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(dsize));
            if (fln != null) downloadLink.setName(fln.trim());
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
