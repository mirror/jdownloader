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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "depositfiles.com" }, urls = { "https?://[\\w\\.]*?depositfiles\\.com(/\\w{1,3})?/files/[\\w]+" }, flags = { 2 })
public class DepositFiles extends PluginForHost {

    private static final String UA                       = "User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:11.0) Gecko/20100101 Firefox/11.0";
    private static final String FILE_NOT_FOUND           = "Dieser File existiert nicht|Entweder existiert diese Datei nicht oder sie wurde";
    private static final String PATTERN_PREMIUM_FINALURL = "<div id=\"download_url\">.*?<a href=\"(.*?)\"";
    private static final String MAINPAGE                 = "http://depositfiles.com";

    public String               DLLINKREGEX2             = "<div id=\"download_url\" style=\"display:none;\">.*?<form action=\"(.*?)\" method=\"get";
    private final Pattern       FILE_INFO_NAME           = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);
    private final Pattern       FILE_INFO_SIZE           = Pattern.compile(">Datei Gr.*?sse: <b>([^<>\"]*?)</b>");

    private static final Object PREMLOCK                 = new Object();
    private static final Object LOCK                     = new Object();
    private boolean             showCaptcha              = false;

    private static int          simultanpremium          = 1;

    public DepositFiles(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://depositfiles.com/signup.php?ref=down1");
    }

    public void checkErrors() throws NumberFormatException, PluginException {
        logger.info("Checking errors...");
        if (br.containsHTML("Zugang zur folgenden Datei ist begrenzt oder Datei wurde entfernt")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Leider, sind alle Slots f")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Free Downloadslot", 20 * 60 * 1000l);
        /* Server under maintenance */
        if (br.containsHTML("(html_download_api-temporary_unavailable|The site is temporarily unavailable for we are making some important upgrades)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Under maintenance, contact depositfiles support", 30 * 60 * 1000l); }
        /* download not available at the moment */
        if (br.containsHTML("Entschuldigung aber im Moment koennen Sie nur diesen Downloadmodus")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l); }
        /* limit reached */
        if (br.containsHTML("(Sie haben ein Limit fuer Downloaden ausgeschoepft|You used up your limit|Please try in|You have reached your download time limit)")) {
            String wait = br.getRegex("html_download_api-limit_interval\">(\\d+)</span>").getMatch(0);
            if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1000l); }
            System.out.print(br.toString());
            wait = br.getRegex(">Try in (\\d+) minutes or use GOLD account").getMatch(0);
            if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
        }
        if (br.containsHTML("(Anschlusslimit|Bitte versuchen Sie in)")) {
            String wait = br.getRegex("versuchen Sie in.*?(\\d+) minu").getMatch(0);
            if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
        }
        /* county slots full */
        if (br.containsHTML("but all downloading slots for your country")) {
            // String wait =
            // br.getRegex("html_download_api-limit_country\">(\\d+)</span>").getMatch(0);
            // if (wait != null) throw new
            // PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE,
            // Integer.parseInt(wait.trim()) * 1000l);
            // set to one minute according to user request
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.depositfilescom.errors.allslotsbusy", "All download slots for your country are busy"), 1 * 60 * 1000l);
        }
        /* already loading */
        if (br.containsHTML("Von Ihren IP-Addresse werden schon einige")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1001l); }
        if (br.containsHTML("You cannot download more than one file in parallel")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1001l); }
        /* unknown error, try again */
        final String wait = br.getRegex("Bitte versuchen Sie noch mal nach(.*?)<\\/strong>").getMatch(0);
        if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, TimeFormatter.getMilliSeconds(wait)); }
        /* You have exceeded the 15 GB 24-hour limit */
        if (br.containsHTML("GOLD users can download no more than")) {
            logger.info("GOLD users can download no more than 15 GB for the last 24 hours");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if (br.containsHTML("Entweder existiert diese Datei nicht oder sie wurde aufgrund von")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.com(/.*?)?/files", ".com/de/files"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("https://", "http://"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        setBrowserExclusive();
        synchronized (LOCK) {
            try {
                showCaptcha = true;
                login(account, true);
            } catch (final PluginException e) {
                ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountbad", "Account expired or not valid."));
                account.setValid(false);
                return ai;
            } finally {
                showCaptcha = false;
            }
            if (isFreeAccount(account, true)) {
                try {
                    account.setMaxSimultanDownloads(1);
                    account.setConcurrentUsePossible(false);
                } catch (final Throwable e) {
                }
                ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountokfree", "Account is OK.(Free User)"));
                account.setValid(true);
                return ai;
            } else {
                try {
                    account.setMaxSimultanDownloads(-1);
                    account.setConcurrentUsePossible(true);
                } catch (final Throwable e) {
                }
            }
            String expire = br.getRegex("Gold-Zugriff: <b>(.*?)</b></div>").getMatch(0);
            if (expire == null) expire = br.getRegex("Gold Zugriff bis: <b>(.*?)</b></div>").getMatch(0);
            if (expire == null) expire = br.getRegex("Gold(-| )(Zugriff|Zugang)( bis)?: <b>(.*?)</b></div>").getMatch(3);
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
            if (expire == null) {
                ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountbad", "Account expired or not valid."));
                account.setProperty("premium", Property.NULL);
                account.setProperty("cookies", Property.NULL);
                account.setValid(false);
                return ai;
            }
            ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountok", "Account is OK."));
            Date date;
            account.setValid(true);
            try {
                date = dateFormat.parse(expire);
                ai.setValidUntil(date.getTime());
            } catch (final ParseException e) {
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            }
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://depositfiles.com/en/agreem.html";
    }

    private String getDllink() throws Exception {
        String crap = br.getRegex("document\\.getElementById\\(\\'download_container\\'\\)\\.innerHTML = \\'(.*?\\';)").getMatch(0);
        if (crap == null && br.containsHTML("download_container")) {
            crap = br.getRegex("download_container.*load\\((.*?)\n").getMatch(0);
        } else {
            return null;
        }
        if (crap != null) {
            crap = crap.replaceAll("(\\'| |;|\\+|\\(|\\)|\t|\r|\n)", "");
            final String[] lol = HTMLParser.getHttpLinks(crap, "");
            if (lol == null || lol.length == 0) {
                if (!crap.contains("depositfiles") && crap.contains("php?")) {
                    return MAINPAGE + crap;
                } else {
                    return null;
                }
            }
            return lol[0];
        } else {
            String fid = br.getRegex("var fid = '(.*?)'").getMatch(0);
            if (fid != null) { return "http://depositfiles.com/get_file.php?fid=" + fid; }
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        synchronized (PREMLOCK) {
            return simultanpremium;
        }
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 800;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        String passCode = null;
        br.forceDebug(true);
        requestFileInformation(downloadLink);
        String link = downloadLink.getDownloadURL();
        // br.getPage(link);
        if (br.getRedirectLocation() != null) {
            link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
            br.getPage(link);
            // If we can't change the language lets just use the forced language
            // (e.g. links change to "/es/" links)!
            if (br.getRedirectLocation() != null) {
                br.getPage(br.getRedirectLocation());
            }
        }
        checkErrors();
        String dllink = getDllink();
        if (dllink != null && !dllink.equals("")) {
            // handling for txt file downloadlinks, dunno why they made a
            // completely different page for txt files
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            final URLConnectionAdapter con = dl.getConnection();
            if (Plugin.getFileNameFromHeader(con) == null || Plugin.getFileNameFromHeader(con).indexOf("?") >= 0) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (!con.isContentDisposition()) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            }
            dl.startDownload();
        } else {
            logger.info("Entering form-handling.");
            final Form form = new Form();
            form.setMethod(MethodType.POST);
            form.setAction("");
            form.put("gateway_result", "1");
            // Important: Setup Cookie
            final String keks = br.getRegex("(adv_.*?);").getMatch(0);
            if (keks != null) {
                final String[] Keks = keks.split("=");
                if (Keks.length == 1) {
                    br.setCookie(MAINPAGE, Keks[0], "");
                }
                if (Keks.length == 2) {
                    br.setCookie(MAINPAGE, Keks[0], Keks[1]);
                }
            }
            br.submitForm(form);
            checkErrors();
            if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) { throw new PluginException(LinkStatus.ERROR_RETRY); }
            if (br.containsHTML("\"file_password\"")) {
                logger.info("This file seems to be password protected.");
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = getUserInput(null, downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                br.postPage(br.getURL(), "file_password=" + passCode);
                logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
                if (br.containsHTML("(>The file's password is incorrect. Please check your password and try to enter it again\\.<|\"file_password\")")) {
                    logger.info("The entered password (" + passCode + ") was wrong, retrying...");
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            final String fid = br.getRegex("var fid = \\'(.*?)\\';").getMatch(0);
            final String wait = br.getRegex("Please wait (\\d+) sec").getMatch(0);
            final String id = this.br.getRegex("Recaptcha\\.create\\(\\'([^\"\\']+)\\'").getMatch(0);
            dllink = getDllink();
            long timeBefore = System.currentTimeMillis();
            /*
             * seems somethings wrong with waittime parsing so we do wait each time to be sure
             */
            if (fid == null || dllink == null || id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            Form dlForm = new Form();
            dlForm.setMethod(MethodType.GET);
            dlForm.put("fid", fid);
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(dlForm);
            rc.setId(id);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
            int waitThis = 62;
            if (wait != null) waitThis = Integer.parseInt(wait);
            waitThis -= passedTime;
            if (waitThis > 0) this.sleep(waitThis * 1001l, downloadLink);
            // Important! Setup Header
            br.getHeaders().put("Accept-Charset", null);
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Cache-Control", null);
            br.getHeaders().put("Accept", "text/html, */*");
            br.getHeaders().put("Accept-Encoding", "gzip, deflate");
            br.getHeaders().put("Accept-Language", "de");
            br.setFollowRedirects(true);
            br.getPage(dllink);
            br.getPage("http://depositfiles.com/get_file.php?fid=" + fid + "&challenge=" + rc.getChallenge() + "&response=" + Encoding.urlEncode(c));
            if (br.containsHTML("(onclick=\"check_recaptcha|load_recaptcha)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            String finallink = br.getRegex("\"(http://fileshare\\d+\\.depositfiles\\.com/auth.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("<form action=\"(http://.*?)\"").getMatch(0);
            if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 1);
            final URLConnectionAdapter con = dl.getConnection();
            if (Plugin.getFileNameFromHeader(con) == null || Plugin.getFileNameFromHeader(con).indexOf("?") >= 0) {
                if (!con.isContentDisposition()) {
                    con.disconnect();
                    if (con.getHeaderField("Guest-Limit") != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l); }
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            if (!con.isContentDisposition()) {
                if (con.getHeaderField("Guest-Limit") != null) {
                    con.disconnect();
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
                }
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            }
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
            if (con.getContentType().contains("html")) {
                logger.warning("The finallink doesn't lead to a file, following connection...");
                if (con.getHeaderField("Guest-Limit") != null) {
                    con.disconnect();
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
                }
                br.followConnection();
                if (br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l); }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String name = Plugin.getFileNameFromHeader(con);
            if (name != null && name.contains("?")) {
                /* fix invalid filenames */
                String fixedName = new Regex(name, "(.+)\\?").getMatch(0);
                downloadLink.setFinalFileName(fixedName);
            }
            dl.startDownload();
        }
    }

    // TODO: The handleFree supports password protected links, handlePremium not
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        boolean free = false;
        requestFileInformation(downloadLink);
        synchronized (PREMLOCK) {
            showCaptcha = false;
            login(account, false);
            if (isFreeAccount(account, false)) {
                simultanpremium = 1;
                free = true;
            } else {
                if (simultanpremium + 1 > 20) {
                    simultanpremium = 20;
                } else {
                    simultanpremium++;
                }
            }
        }
        if (free) {
            handleFree(downloadLink);
            return;
        }
        String link = downloadLink.getDownloadURL();
        br.getPage(link);
        if (br.getRedirectLocation() != null) {
            link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
            br.getPage(link);
        }
        checkErrors();
        link = br.getRegex(PATTERN_PREMIUM_FINALURL).getMatch(0);
        if (link == null) {
            synchronized (LOCK) {
                account.setProperty("cookies", null);
                account.setProperty("premium", (Boolean) null);
            }
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 0);
        final URLConnectionAdapter con = dl.getConnection();
        if (Plugin.getFileNameFromHeader(con) == null || Plugin.getFileNameFromHeader(con).indexOf("?") >= 0) {
            if (!con.isContentDisposition()) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        if (!con.isContentDisposition()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        String name = Plugin.getFileNameFromHeader(con);
        if (name != null && name.contains("?")) {
            /* fix invalid filenames */
            String fixedName = new Regex(name, "(.+)\\?").getMatch(0);
            downloadLink.setFinalFileName(fixedName);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public boolean isFreeAccount(Account acc, boolean force) throws IOException {
        synchronized (LOCK) {
            Object premium = acc.getBooleanProperty("premium", false);
            if (premium != null && premium instanceof Boolean && !force) return (Boolean) premium;
            setLangtoGer();
            br.getPage(MAINPAGE + "/de/gold/");
            boolean ret = false;
            if (br.containsHTML("Ihre aktuelle Status: Frei - Mitglied</div>")) {
                ret = true;
            } else if (br.containsHTML("So lange haben Sie noch den Gold-Zugriff")) {
                ret = false;
            } else if (br.containsHTML(">Goldmitgliedschaft<")) {
                ret = false;
            } else if (br.containsHTML("noch den Gold-Zugriff")) {
                ret = false;
            } else if (br.containsHTML("haben noch Gold Zugang bis")) {
                ret = false;
            } else if (br.containsHTML("haben Gold Zugang bis")) {
                ret = false;
            } else {
                ret = true;
            }
            acc.setProperty("premium", ret);
            return ret;
        }
    }

    private String getDonloadManagerVersion() {
        Browser brc = br.cloneBrowser();
        try {
            brc.getPage("http://system.depositfiles.com/api/get_downloader_version.php");
        } catch (Throwable e) {
            return null;
        }
        return brc.getRegex("([\\d\\.]+)").getMatch(0);
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                setLangtoGer();
                final Object ret = account.getProperty("cookies", null);
                final Object premium = account.getProperty("premium", (Boolean) null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (premium != null && acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    String dmVersion = account.getStringProperty("dmVersion", null);
                    if (dmVersion != null) {
                        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru; rv:1.8.1.20) DepositFiles/FileManager " + dmVersion);
                    }
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
                String dmVersion = getDonloadManagerVersion();
                String UA = null;
                if (dmVersion == null) {
                    String uprand = account.getStringProperty("uprand", null);
                    if (uprand != null) br.setCookie(MAINPAGE, "uprand", uprand);
                    br.getHeaders().put("User-Agent", UA);
                    br.setReadTimeout(3 * 60 * 1000);
                    br.setFollowRedirects(true);
                    br.getPage(MAINPAGE);
                    Thread.sleep(2000);
                    final Form login = br.getFormBySubmitvalue("Anmelden");
                    if (login != null) {
                        login.setAction("http://depositfiles.com/login.php?return=%2F");
                        login.put("login", Encoding.urlEncode(account.getUser()));
                        login.put("password", Encoding.urlEncode(account.getPass()));
                        br.submitForm(login);
                    } else if (!showCaptcha || !br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                } else {
                    UA = "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru; rv:1.8.1.20) DepositFiles/FileManager " + dmVersion;
                    br.getHeaders().put("User-Agent", UA);
                    br.setFollowRedirects(true);
                    Thread.sleep(2000);
                    br.postPage("http://depositfiles.com/de/login.php", "go=1&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    br.getPage("http://depositfiles.com/gold/");
                }
                br.setFollowRedirects(false);
                if (showCaptcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    DownloadLink dummy = new DownloadLink(null, null, null, null, true);
                    PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    for (int i = 0; i <= 3; i++) {
                        rc.parse();
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode(cf, dummy);
                        rc.getForm().put("login", Encoding.urlEncode(account.getUser()));
                        rc.getForm().put("password", Encoding.urlEncode(account.getPass()));
                        rc.setCode(c);
                        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) continue;
                        break;
                    }
                    if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                        logger.info("Invalid account or wrong captcha!");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final String cookie = br.getCookie(MAINPAGE, "autologin");
                if (cookie == null || br.containsHTML("Benutzername\\-Passwort\\-Kombination")) {
                    logger.info("invalid password/username or captcha required!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                account.setProperty("dmVersion", dmVersion);
                account.setProperty("uprand", br.getCookie(MAINPAGE, "uprand"));
                account.setProperty("UA", UA);
            } catch (final PluginException e) {
                account.setProperty("premium", Property.NULL);
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", UA);
        final String link = downloadLink.getDownloadURL();
        setLangtoGer();
        /* needed so the download gets counted,any referer should work */
        // br.getHeaders().put("Referer", "http://www.google.de");
        br.setFollowRedirects(false);
        br.getPage(link);

        // Datei geloescht?
        if (br.containsHTML(FILE_NOT_FOUND)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (br.containsHTML("<strong>Achtung! Sie haben ein Limit")) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.depositfilescom.errors.limitreached", "Download limit reached"));
            return AvailableStatus.TRUE;
        }
        String fileName = br.getRegex(FILE_INFO_NAME).getMatch(0);
        final String fileSizeString = br.getRegex(FILE_INFO_SIZE).getMatch(0);
        if (fileName == null || fileSizeString == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String fixedName = new Regex(fileName, "(.+)\\?").getMatch(0);
        if (fixedName != null) fileName = fixedName;
        downloadLink.setName(fileName);
        downloadLink.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(fileSizeString)));
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

    public void setLangtoGer() throws IOException {
        br.setCookie(MAINPAGE, "lang_current", "de");
    }
}