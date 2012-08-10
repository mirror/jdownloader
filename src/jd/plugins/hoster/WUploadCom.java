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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wupload.com" }, urls = { "http://(www\\.)?wupload\\.[a-z]{1,5}/file/([0-9]+(/.+)?|[a-z0-9]+/[0-9]+(/.+)?)" }, flags = { 2 })
public class WUploadCom extends PluginForHost {

    private static final Object  LOCK                = new Object();

    private static volatile long LAST_FREE_DOWNLOAD  = 0l;

    private static final String  ua                  = "Mozilla/5.0 (JD; X11; U; Linux i686; en-US; rv:1.9.0.10) Gecko/2009042523 Ubuntu/9.04 (jaunty) Firefox/3.0.10";
    private static final String  uaf                 = "Mozilla/5.0 (JDF; X11; U; Linux i686; en-US; rv:1.9.0.10) Gecko/2009042523 Ubuntu/9.04 (jaunty) Firefox/3.0.10";
    private static final String  uap                 = "Mozilla/5.0 (JDP; X11; U; Linux i686; en-US; rv:1.9.0.10) Gecko/2009042523 Ubuntu/9.04 (jaunty) Firefox/3.0.10";
    private static String        geoDomain           = null;
    private static final String  RECAPTCHATEXT       = "Recaptcha\\.create";
    private static final String  DLYOURFILESUSERTEXT = "You can only download files which YOU uploaded!";
    private static final String  DLYOURFILESTEXT     = "(>Wupload does not allow files to be shared|>If you have uploaded this file yourself, login first in order to download it)";

    public WUploadCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.wupload.com/premium");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (true) {
            downloadLink.getLinkStatus().setStatusText(DLYOURFILESUSERTEXT);
            return AvailableStatus.UNCHECKABLE;
        }
        correctDownloadLink(downloadLink);
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) { return AvailableStatus.UNCHECKED; }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return AvailableStatus.TRUE;
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (true) {
            for (DownloadLink aLink : urls) {
                aLink.getLinkStatus().setStatusText(DLYOURFILESUSERTEXT);
                aLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
            }
            return true;
        }
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            br.setCookie(getDomain(), "lang", "en");
            br.getHeaders().put("User-Agent", ua);
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 80 links at once */
                    if (index == urls.length || links.size() > 80) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("ids=");
                int c = 0;
                for (final DownloadLink dl : links) {
                    if (c > 0) {
                        sb.append(",");
                    }
                    sb.append(getPureID(dl));
                    c++;
                }
                br.postPage("http://api.wupload.com/link?method=getInfo", sb.toString());
                for (final DownloadLink dllink : links) {
                    final String id = getPureID(dllink);
                    final String hit = br.getRegex("link><id>" + id + "(.*?)</link>").getMatch(0);
                    if (hit == null || hit.contains("status>NOT_AVAILABLE")) {
                        dllink.setAvailable(false);
                    } else {
                        String name = new Regex(hit, "filename>(.*?)</filename").getMatch(0);
                        if (name.startsWith("<![CDATA")) {
                            name = new Regex(name, "CDATA\\[(.*?)\\]\\]>").getMatch(0);
                        }
                        final String size = new Regex(hit, "size>(\\d+)</size").getMatch(0);
                        dllink.setAvailable(true);
                        dllink.setFinalFileName(Encoding.htmlDecode(name));
                        if (size != null) {
                            dllink.setDownloadSize(Long.parseLong(size));
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    /* converts id and filename */
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(getDomain() + "/file/" + getID(link));
    }

    // private String downloadAPI(final Browser useBr, final Account account,
    // final DownloadLink link) throws IOException, PluginException {
    // Browser br = useBr;
    // if (br == null) {
    // br = new Browser();
    // }
    // br.getHeaders().put("User-Agent", uap);
    // br.setFollowRedirects(true);
    // String pw = "";
    // final String pwUsw = link.getStringProperty("pass", null);
    // if (pwUsw != null) {
    // pw = "&passwords[" + getPureID(link) + "]=" + Encoding.urlEncode(pwUsw);
    // }
    // final String page =
    // br.getPage("http://api.wupload.com/link?method=getDownloadLink&u=" +
    // Encoding.urlEncode(account.getUser()) + "&p=" +
    // Encoding.urlEncode(account.getPass()) + "&format=xml&ids=" +
    // getPureID(link) + pw);
    // if (page.contains("FSApi_Auth_Exception")) { throw new
    // PluginException(LinkStatus.ERROR_PREMIUM,
    // PluginException.VALUE_ID_PREMIUM_DISABLE); }
    // final String status = br.getRegex("status>(.*?)</status").getMatch(0);
    // if ("NOT_AVAILABLE".equals(status)) { throw new
    // PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
    // return page;
    // }

    private void errorHandling(final DownloadLink downloadLink, final Browser br) throws PluginException {
        if (br.containsHTML("The server is temporarily offline for maintenance")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server Maintenance", 60 * 60 * 1000l); }
        if (br.containsHTML("The file that you're trying to download is larger than")) {
            final String size = br.getRegex("trying to download is larger than (.*?)\\.<").getMatch(0);
            if (size != null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.LF("plugins.hoster.wupload.onlypremiumsize", "Only premium users can download files larger than %s.", size.trim()));
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.wupload.onlypremium", "Only downloadable for premium users!"));
            }
        }
        if (br.containsHTML("only download 1 file at a time")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.wupload.alreadyloading", "This IP is already downloading"), 5 * 60 * 1000l); }
        if (br.containsHTML("Free user can not download files")) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.wupload.largefree", "Free user can not download files over 400MB")); }
        if (br.containsHTML("Download session in progress")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.wupload.inprogress", "Download session in progress"), 10 * 60 * 1000l); }
        if (br.containsHTML("We have detected some suspicious behaviour coming from your IP address")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.wupload.ipabuse", "Hoster blocking IP address due to abuse"), 6 * 60 * 60 * 1000l); }
        if (br.containsHTML("This file is password protected")) {
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
        }
        if (br.containsHTML("((?i)An Error Occurred(?-i)|>404 Not Found<|>The server is temporarily offline for maintenance, please try again later<)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.wupload.servererror", "Server error"), 20 * 60 * 1000l); }
        if (br.containsHTML("This file is available for premium users only\\.")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only file. Buy Premium Account"); }
        if (br.containsHTML("(You can not access this page directly\\. Please use the|>website to start your download</a>|<p>If the problem persists, clear your cookies and try again\\.</p>)")) {
            logger.warning(br.toString());
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
    }

    // @Override
    // public AccountInfo fetchAccountInfo(final Account account) throws
    // Exception {
    // synchronized (LOCK) {
    // final AccountInfo ai = new AccountInfo();
    // try {
    // loginAPI(br, account);
    // } catch (final IOException e) {
    // account.setValid(true);
    // account.setTempDisabled(true);
    // ai.setStatus("ServerError, will retry later");
    // return ai;
    // } catch (final Throwable e) {
    // ai.setStatus("Account invalid! Try Email as Login and don't use special chars in PW!");
    // account.setValid(false);
    // return ai;
    // }
    // final String expiredate =
    // br.getRegex("expiration>(.*?)</premium").getMatch(0);
    // if (expiredate != null) {
    // ai.setStatus("Premium User");
    // account.setValid(true);
    // ai.setUnlimitedTraffic();
    // ai.setValidUntil(TimeFormatter.getMilliSeconds(expiredate.trim(),
    // "yyyy-MM-dd HH:mm:ss", null));
    // return ai;
    // }
    // account.setValid(false);
    // return ai;
    // }
    // }

    @Override
    public String getAGBLink() {
        return "http://www.wupload.com/terms-and-conditions";
    }

    private synchronized String getDomain() {
        if (geoDomain != null) { return geoDomain; }
        final String defaultDomain = "http://www.wupload.com";
        try {
            geoDomain = getDomainAPI();
            if (geoDomain == null) {
                final Browser br = new Browser();
                br.getHeaders().put("User-Agent", ua);
                br.setCookie(defaultDomain, "lang", "en");
                br.setFollowRedirects(false);
                br.getPage(defaultDomain);
                geoDomain = br.getRedirectLocation();
                if (geoDomain == null) {
                    geoDomain = defaultDomain;
                } else {
                    final String domain = new Regex(br.getRedirectLocation(), "https?://.*?(wupload\\..*?)(/|$)").getMatch(0);
                    if (domain == null) {
                        logger.severe("getDomain: failed(2) " + br.getRedirectLocation() + " " + br.toString());
                        geoDomain = defaultDomain;
                    } else {
                        geoDomain = "http://www." + domain;
                    }
                }
            }
        } catch (final Throwable e) {
            logger.info("getDomain: failed(1)" + e.toString());
            geoDomain = defaultDomain;
        }
        return geoDomain;
    }

    private synchronized String getDomainAPI() {
        try {
            final Browser br = new Browser();
            br.getHeaders().put("User-Agent", ua);
            br.setFollowRedirects(true);
            br.getPage("http://api.wupload.com/utility?method=getWuploadDomainForCurrentIp");
            final String domain = br.getRegex("response>.*?wupload(\\..*?)</resp").getMatch(0);
            if (domain != null) { return "http://www.wupload" + domain; }
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        return null;
    }

    public String getID(final DownloadLink link) {
        String id = new Regex(link.getDownloadURL(), "/file/([0-9]+(/.+)?)").getMatch(0);
        if (id == null) {
            id = new Regex(link.getDownloadURL(), "/file/[a-z0-9]+/([0-9]+(/.+)?)").getMatch(0);
        }
        return id;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public String getPureID(final DownloadLink link) {
        String id = new Regex(link.getDownloadURL(), "/file/([0-9]+)").getMatch(0);
        if (id == null) {
            id = new Regex(link.getDownloadURL(), "/file/[a-z0-9]+/([0-9]+)").getMatch(0);
        }
        return id;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        if (true) throw new PluginException(LinkStatus.ERROR_FATAL, DLYOURFILESUSERTEXT);
        String downloadUrl = null;
        String passCode = null;
        passCode = null;
        br.getHeaders().put("User-Agent", uaf);
        br.setCookiesExclusive(false);
        br.forceDebug(true);
        // we have to enter captcha before we get ip_blocked_state
        // we do this timeing check to avoid this
        final long waited = System.currentTimeMillis() - LAST_FREE_DOWNLOAD;
        if (LAST_FREE_DOWNLOAD > 0 && waited < 300000) {
            LAST_FREE_DOWNLOAD = 0;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 600000 - waited);
        }
        requestFileInformation(downloadLink);
        br.setCookie(getDomain(), "lang", "en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        passCode = null;
        if (br.containsHTML("deletedFile\">Sorry, this file has been removed")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String domain = new Regex(br.getURL(), "(http.*?\\..*?/)").getMatch(0);
        String freeDownloadLink = domain + "file/" + new Regex(downloadLink.getDownloadURL(), "/file/([^<>\"/]+)/?").getMatch(0) + "?start=1";
        // this is an ajax call
        Browser ajax = br.cloneBrowser();
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.getHeaders().put("Content-Length", "0");
        ajax.postPage(freeDownloadLink, "");
        ajax.getHeaders().put("Content-Length", null);
        errorHandling(downloadLink, ajax);
        br.setFollowRedirects(true);
        // download is ready already
        final String re = "<p><a href=\"(http://[^<]*?\\.wupload\\..*?[^<]*?)\"><span>";

        downloadUrl = ajax.getRegex(re).getMatch(0);
        if (downloadUrl == null) {

            // downloadUrl =
            // this.br.getRegex("downloadUrl = \"(http://.*?)\"").getMatch(0);
            // if (downloadUrl == null) { throw new
            // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (ajax.containsHTML("This file is available for premium users only\\.")) {

            throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only file. Buy Premium Account"); }
            final String countDownDelay = ajax.getRegex("countDownDelay = (\\d+)").getMatch(0);
            if (countDownDelay != null) {
                /*
                 * we have to wait a little longer than needed cause its not
                 * exactly the same time
                 */
                if (Long.parseLong(countDownDelay) > 300) {

                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(countDownDelay) * 1001l); }
                this.sleep(Long.parseLong(countDownDelay) + 3 * 1000, downloadLink);
                final String tm = ajax.getRegex("name=\\'tm\\' value=\\'(.*?)\\' />").getMatch(0);
                final String tm_hash = ajax.getRegex("name=\\'tm_hash\\' value='(.*?)' />").getMatch(0);
                final Form form = new Form();
                form.setMethod(Form.MethodType.POST);
                form.setAction(downloadLink.getDownloadURL() + "?start=1");
                form.put("tm", tm);
                form.put("tm_hash", tm_hash);
                ajax = br.cloneBrowser();
                ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                ajax.submitForm(form);
                errorHandling(downloadLink, ajax);
            }
            int tries = 0;
            while (ajax.containsHTML("Please Enter Password")) {
                /* password handling */
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(null, downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                final Form form = ajax.getForm(0);
                form.put("passwd", Encoding.urlEncode(passCode));
                ajax = br.cloneBrowser();
                ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                ajax.submitForm(form);
                if (tries++ >= 5) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Password Missing"); }

            }
            if (ajax.containsHTML(RECAPTCHATEXT)) {
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(ajax);
                // TODO: Remove after MAJOR (NIGHTLY) update
                final String id = ajax.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
                rc.setId(id);
                Form rcForm = new Form();
                rcForm.setMethod(MethodType.POST);
                rcForm.setAction(freeDownloadLink);
                rc.setForm(rcForm);
                rc.load();
                for (int i = 0; i <= 5; i++) {
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, downloadLink);
                    rc.setCode(c);
                    if (ajax.containsHTML(RECAPTCHATEXT)) {
                        rc.reload();
                        continue;
                    }
                    break;
                }
                if (ajax.containsHTML(RECAPTCHATEXT)) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            }
            downloadUrl = ajax.getRegex(re).getMatch(0);
            if (downloadUrl == null && ajax.containsHTML("countDownDelay =")) {
                /*
                 * because of a server error, after captcha there is another
                 * countdowndelay
                 */

                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerIssue", 1 * 60 * 1000l);
            }
        }
        if (downloadUrl == null) {
            errorHandling(downloadLink, ajax);
            logger.info(ajax.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /*
         * limited to 1 chunk at the moment cause don't know if its a server
         * error that more are possible and resume should also not work ;)
         */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, 1);
        if (dl.getConnection() != null && dl.getConnection().getContentType() != null && (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("unknown"))) {
            br.followConnection();
            errorHandling(downloadLink, br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.setFilenameFix(true);
        dl.startDownload();
        LAST_FREE_DOWNLOAD = System.currentTimeMillis();
    }

    // @Override
    // public void handlePremium(final DownloadLink downloadLink, final Account
    // account) throws Exception {
    // if (true) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    // setBrowserExclusive();
    // requestFileInformation(downloadLink);
    // String resp = downloadAPI(br, account, downloadLink);
    // String url = new Regex(resp, "CDATA\\[(http://.*?)\\]\\]").getMatch(0);
    // if (url == null) {
    // /* check for needed pw */
    // final String status = new Regex(resp,
    // "status>(.*?)</status").getMatch(0);
    // if ("PASSWORD_REQUIRED".equals(status) ||
    // "WRONG_PASSWORD".equals(status)) {
    // /* wrong pw */
    // final String passCode = Plugin.getUserInput(null, downloadLink);
    // downloadLink.setProperty("pass", passCode);
    // }
    // resp = downloadAPI(br, account, downloadLink);
    // url = new Regex(resp, "CDATA\\[(http://.*?)\\]\\]").getMatch(0);
    // }
    // if (url == null) {
    // final String status = new Regex(resp,
    // "status>(.*?)</status").getMatch(0);
    // if ("PASSWORD_REQUIRED".equals(status) ||
    // "WRONG_PASSWORD".equals(status)) {
    // /* wrong pw */
    // downloadLink.setProperty("pass", null);
    // throw new PluginException(LinkStatus.ERROR_FATAL,
    // "Password missing/wrong");
    // }
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true,
    // 0);
    // if (dl.getConnection() != null && dl.getConnection().getContentType() !=
    // null && (dl.getConnection().getContentType().contains("html") ||
    // dl.getConnection().getContentType().contains("unknown"))) {
    // br.followConnection();
    // errorHandling(downloadLink, br);
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // dl.startDownload();
    // }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    // private String loginAPI(final Browser useBr, final Account account)
    // throws IOException, PluginException {
    // Browser br = useBr;
    // if (br == null) {
    // br = new Browser();
    // }
    // br.getHeaders().put("User-Agent", uap);
    // br.setFollowRedirects(true);
    // final String page =
    // br.getPage("http://api.wupload.com/user?method=getInfo&u=" +
    // Encoding.urlEncode(account.getUser()) + "&p=" +
    // Encoding.urlEncode(account.getPass()) + "&format=xml");
    // System.out.println(br.toString());
    // final String premium = br.getRegex("is_premium>(.*?)</is_").getMatch(0);
    // if (!"1".equals(premium)) { throw new
    // PluginException(LinkStatus.ERROR_PREMIUM,
    // PluginException.VALUE_ID_PREMIUM_DISABLE); }
    // return page;
    // }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            getDomain();
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
                        this.br.setCookie(geoDomain, key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(true);
            br.getHeaders().put("User-Agent", uap);
            Browser loginbr = br.cloneBrowser();
            loginbr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            loginbr.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            loginbr.postPage(geoDomain + "/account/login", "rememberMe=1&redirect=%2F&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            if (!"premium".equals(loginbr.getCookie(geoDomain, "role")) || !loginbr.containsHTML("status\":\"success\"")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(geoDomain);
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
        br.getPage(geoDomain + "/account/settings");
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<li>Premium Membership Valid Until: <strong>([A-Za-z]+ \\d+, \\d+)</strong>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MMMM dd, yyyy", null));
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
        if (br.containsHTML(DLYOURFILESTEXT)) throw new PluginException(LinkStatus.ERROR_FATAL, DLYOURFILESUSERTEXT);
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
    public void resetDownloadlink(final DownloadLink link) {
        correctDownloadLink(link);
    }

    @Override
    public void resetPluginGlobals() {
    }

}