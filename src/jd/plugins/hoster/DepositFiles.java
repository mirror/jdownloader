//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
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
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "depositfiles.com" }, urls = { "https?://(www\\.)?(depositfiles\\.(com|org)|dfiles\\.(eu|ru))(/\\w{1,3})?/files/[\\w]+" }, flags = { 2 })
public class DepositFiles extends PluginForHost {

    public static class StringContainer {
        public String string = null;

        @Override
        public String toString() {
            return string;
        }
    }

    private final String          UA                       = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
    private final String          FILE_NOT_FOUND           = "Dieser File existiert nicht|Entweder existiert diese Datei nicht oder sie wurde";
    private final String          PATTERN_PREMIUM_FINALURL = "<div id=\"download_url\".*?<a href=\"(.*?)\"";
    public static StringContainer MAINPAGE                 = new StringContainer();
    public static final String    DOMAINS                  = "(depositfiles\\.(com|org)|dfiles\\.(eu|ru))";

    private String                protocol                 = null;

    public String                 DLLINKREGEX2             = "<div id=\"download_url\" style=\"display:none;\">.*?<form action=\"(.*?)\" method=\"get";
    private final Pattern         FILE_INFO_NAME           = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);
    private final Pattern         FILE_INFO_SIZE           = Pattern.compile(">Datei Gr.*?sse: <b>([^<>\"]*?)</b>");

    private static Object         PREMLOCK                 = new Object();
    private static Object         LOCK                     = new Object();

    private static AtomicInteger  simultanpremium          = new AtomicInteger(1);
    private static AtomicBoolean  useAPI                   = new AtomicBoolean(true);

    private final String          SSL_CONNECTION           = "SSL_CONNECTION";

    public DepositFiles(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("http://depositfiles.com/signup.php?ref=down1");
    }

    public void setMainpage() {
        if (MAINPAGE == null || MAINPAGE.string == null || userChangedSslSetting()) {
            try {
                Browser testBr = new Browser();
                testBr.setFollowRedirects(true);
                // NOTE: https requests do not trigger redirects
                testBr.getPage(fixLinkSSL("http://depositfiles.com"));
                String baseURL = new Regex(testBr.getURL(), "(https?://[^/]+)").getMatch(0);
                StringContainer main = new StringContainer();
                main.string = baseURL;
                if (baseURL != null) MAINPAGE = main;
                System.out.println("depositfiles setter MAINPAGE = " + MAINPAGE.string);
            } catch (Throwable e) {
                e.printStackTrace();
                try {
                    System.out.println("despostfiles setter failed, setting failover");
                    StringContainer main = new StringContainer();
                    main.string = fixLinkSSL("http://depositfiles.com");
                    MAINPAGE = main;
                    System.out.println("depositfiles setter MAINPAGE = " + MAINPAGE.string);
                } catch (final Throwable e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
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

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        setMainpage();
        final String newLink = link.getDownloadURL().replaceAll(DOMAINS + "(/.*?)?/files", MAINPAGE.string.replaceAll("https?://(www\\.)?", "") + "/de/files");
        link.setUrlDownload(fixLinkSSL(newLink));
    }

    private static void showFreeDialog(final String domain) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        String tab = "                        ";
                        if ("de".equalsIgnoreCase(lng)) {
                            title = domain + " Free Download";
                            message = "Du lädst im kostenlosen Modus von " + domain + ".\r\n";
                            message += "Wie bei allen anderen Hostern holt JDownloader auch hier das Beste für dich heraus!\r\n\r\n";
                            message += tab + "  Falls du allerdings mehrere Dateien\r\n" + "          - und das möglichst mit Fullspeed und ohne Unterbrechungen - \r\n" + "             laden willst, solltest du dir den Premium Modus anschauen.\r\n\r\nUnserer Erfahrung nach lohnt sich das - Aber entscheide am besten selbst. Jetzt ausprobieren?  ";
                        } else {
                            title = domain + " Free Download";
                            message = "You are using the " + domain + " Free Mode.\r\n";
                            message += "JDownloader always tries to get the best out of each hoster's free mode!\r\n\r\n";
                            message += tab + "   However, if you want to download multiple files\r\n" + tab + "- possibly at fullspeed and without any wait times - \r\n" + tab + "you really should have a look at the Premium Mode.\r\n\r\nIn our experience, Premium is well worth the money. Decide for yourself, though. Let's give it a try?   ";
                        }
                        if (CrossSystem.isOpenBrowserSupported()) {
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                            if (JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + domain + "&freedialog"));
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private void checkShowFreeDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("premAdShown", Boolean.FALSE) == false) {
                if (config.getProperty("premAdShown2") == null) {
                    File checkFile = JDUtilities.getResourceFile("tmp/dpsfiles");
                    if (!checkFile.exists()) {
                        checkFile.mkdirs();
                        showFreeDialog("depositfiles.com");
                    }
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("premAdShown", Boolean.TRUE);
                config.setProperty("premAdShown2", "shown");
                config.save();
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        // correctlink fixes https|not https, and sets mainpage! no need to duplicate in other download areas!.
        correctDownloadLink(downloadLink);
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
        if (fileName == null) {
            String eval = br.getRegex("class=\"info\".*?unescape\\('(.*?)'").getMatch(0);
            if (eval != null) {
                JDUtilities.getPluginForHost("youtube.com");
                eval = jd.plugins.hoster.Youtube.unescape(eval);
                fileName = new Regex(eval, FILE_INFO_NAME).getMatch(0);
            }
        }
        final String fileSizeString = br.getRegex(FILE_INFO_SIZE).getMatch(0);
        if (fileName == null || fileSizeString == null) {
            if (fileSizeString != null) {
                fileName = new Regex(link, "files/([\\w]+)").getMatch(0);
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        String fixedName = new Regex(fileName, "(.+)\\?").getMatch(0);
        if (fixedName != null) fileName = fixedName;
        downloadLink.setName(Encoding.htmlDecode(fileName));
        downloadLink.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(fileSizeString)));
        return AvailableStatus.TRUE;
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
        /* country slots full */
        if (br.containsHTML("but all downloading slots for your country")) {
            // String wait = br.getRegex("html_download_api-limit_country\">(\\d+)</span>").getMatch(0);
            // if (wait != null) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, Integer.parseInt(wait.trim()) *
            // 1000l);
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
                    return MAINPAGE.string + crap;
                } else {
                    return null;
                }
            }
            return lol[0];
        } else {
            String fid = br.getRegex("var fid = '(.*?)'").getMatch(0);
            if (fid != null) { return MAINPAGE.string + "/get_file.php?fid=" + fid; }
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
            return simultanpremium.get();
        }
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 800;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        setConstants();
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        checkShowFreeDialog();
        br.forceDebug(true);
        String passCode = downloadLink.getStringProperty("pass", null);
        String finallink = checkDirectLink(downloadLink);
        if (finallink == null) {
            String link = downloadLink.getDownloadURL();
            if (br.getRedirectLocation() != null) {
                link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
                br.getPage(link);
                // If we can't change the language lets just use the forced language (e.g. links change to "/es/" links)!
                if (br.getRedirectLocation() != null) {
                    br.getPage(br.getRedirectLocation());
                }
            }
            checkErrors();
            String dllink = getDllink();
            if (dllink != null && !dllink.equals("")) {
                // handling for txt file downloadlinks, dunno why they made a completely different page for txt files
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
                form.setAction(br.getURL().replaceFirst("https?://", protocol));
                form.put("gateway_result", "1");
                // Important: Setup Cookie
                final String keks = br.getRegex("(adv_.*?);").getMatch(0);
                if (keks != null) {
                    final String[] Keks = keks.split("=");
                    if (Keks.length == 1) {
                        br.setCookie(MAINPAGE.string, Keks[0], "");
                    }
                    if (Keks.length == 2) {
                        br.setCookie(MAINPAGE.string, Keks[0], Keks[1]);
                    }
                }
                br.submitForm(form);
                checkErrors();
                if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) { throw new PluginException(LinkStatus.ERROR_RETRY); }
                if (br.containsHTML("\"file_password\"")) {
                    logger.info("This file seems to be password protected.");
                    if (passCode == null) passCode = getUserInput(null, downloadLink);
                    br.postPage(br.getURL().replaceFirst("https?://", protocol), "file_password=" + passCode);
                    logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
                    if (br.containsHTML("(>The file's password is incorrect. Please check your password and try to enter it again\\.<|\"file_password\")")) {
                        logger.info("The entered password (" + passCode + ") was wrong, retrying...");
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                    downloadLink.setProperty("pass", passCode);
                }
                final String fid = br.getRegex("var fid = \\'(.*?)\\';").getMatch(0);
                final String wait = br.getRegex("Please wait (\\d+) sec").getMatch(0);
                final String id = this.br.getRegex("Recaptcha\\.create\\(\\'([^\"\\']+)\\'").getMatch(0);
                dllink = getDllink();
                long timeBefore = System.currentTimeMillis();
                /*
                 * seems something wrong with wait time parsing so we do wait each time to be sure
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
                br.getHeaders().put("Accept", "*/*");
                br.getHeaders().put("Accept-Encoding", "gzip, deflate");
                br.getHeaders().put("Accept-Language", "de");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.setFollowRedirects(true);
                br.getPage(dllink);
                br.getPage(MAINPAGE.string + "/get_file.php?fid=" + fid + "&challenge=" + rc.getChallenge() + "&response=" + Encoding.urlEncode(c));
                if (br.containsHTML("(onclick=\"check_recaptcha|load_recaptcha)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                finallink = br.getRegex("\"(https?://fileshare\\d+\\." + DOMAINS + "/auth.*?)\"").getMatch(0);
                if (finallink == null) finallink = br.getRegex("<form action=\"(https?://.*?)\"").getMatch(0);
                if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
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
        if (passCode != null) downloadLink.setProperty("pass", passCode);
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
        downloadLink.setProperty("finallink", finallink);
        dl.startDownload();
    }

    private void setConstants() {
        // set session protocol based on user setting and jd version && java version
        protocol = fixLinkSSL("https://");
        if (isJava7nJDStable()) protocol = "http://";
    }

    public boolean isFreeAccount(Account acc, boolean force) throws IOException {
        synchronized (LOCK) {
            Object free = acc.getBooleanProperty("free", false);
            if (free != null && free instanceof Boolean && !force) return (Boolean) free;
            if (accountData != null && accountData.containsKey("mode")) {
                if ("gold".equalsIgnoreCase(accountData.get("mode").toString()))
                    return true;
                else
                    return false;
            }
            setLangtoGer();
            if (!br.getURL().contains("/gold/")) br.getPage(MAINPAGE.string + "/de/gold/");
            boolean ret = false;
            if (br.containsHTML("Ihre aktuelle Status: Frei - Mitglied</div>")) {
                ret = false;
            } else if (br.containsHTML("So lange haben Sie noch den Gold-Zugriff")) {
                ret = true;
            } else if (br.containsHTML(">Goldmitgliedshchaft<")) {
                ret = true;
            } else if (br.containsHTML("noch den Gold-Zugriff")) {
                ret = true;
            } else if (br.containsHTML("haben noch Gold Zugang bis")) {
                ret = true;
            } else if (br.containsHTML("haben Gold Zugang bis")) {
                ret = true;
            } else {
                ret = false;
            }
            acc.setProperty("free", ret);
            return ret;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        // removeable dead properties ** remove in a month
        account.setProperty("premium", Property.NULL);
        account.setProperty("dmVersion", Property.NULL);
        account.setProperty("UA", Property.NULL);
        // eo dead stuff

        setBrowserExclusive();
        setMainpage();
        AccountInfo ai = new AccountInfo();
        synchronized (LOCK) {
            // via /api/
            if (useAPI.get()) {
                ai = apiFetchAccountInfo(account);
            }
            if (!useAPI.get()) {
                ai = webFetchAccountInfo(account);
            }
        }
        return ai;
    }

    private AccountInfo webFetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            webLogin(account, true);
        } catch (final PluginException e) {
            ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountbad", "Account expired or not valid."));
            account.setValid(false);
            return ai;
        }
        if (isFreeAccount(account, true)) {
            try {
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
            }
            ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountokfree", "Free Account is ok"));
            account.setValid(true);
            return ai;
        } else {
            try {
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
            String expire = br.getRegex("Gold-Zugriff: <b>(.*?)</b></div>").getMatch(0);
            if (expire == null) expire = br.getRegex("Gold Zugriff bis: <b>(.*?)</b></div>").getMatch(0);
            if (expire == null) expire = br.getRegex("Gold(-| )(Zugriff|Zugang)( bis)?: <b>(.*?)</b></div>").getMatch(3);
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
            if (expire == null) {
                ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountbad", "Account expired or not valid."));
                account.setProperty("free", Property.NULL);
                account.setProperty("cookies", Property.NULL);
                account.setValid(false);
                return ai;
            }
            ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountok", "Premium Account is ok"));
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

    @SuppressWarnings("unchecked")
    private void webLogin(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                setLangtoGer();
                /** Load cookies */
                final Object ret = account.getProperty("cookies", null);
                final Object free = account.getProperty("free", (Boolean) null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (free != null && acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE.string, key, value);
                        }
                        return;
                    }
                }
                // web fail over method
                logger.info("Depositfiles website login method!");
                String uprand = account.getStringProperty("uprand", null);
                if (uprand != null) br.setCookie(MAINPAGE.string, "uprand", uprand);
                br.setReadTimeout(3 * 60 * 1000);
                br.getHeaders().put("User-Agent", UA);
                br.setFollowRedirects(true);
                br.getPage(MAINPAGE.string + "/login.php?return=%2Fde%2F");
                String captchaJs = br.getRegex("(http[^\"']+js/base2\\.js)").getMatch(0);
                Browser br2 = br.cloneBrowser();
                String cid = null;
                if (captchaJs != null) {
                    br2.getPage(captchaJs);
                    cid = br2.getRegex("window\\.recaptcha_public_key = '([^']+)").getMatch(0);
                }
                Thread.sleep(2000);
                final Form login = br.getFormBySubmitvalue("Eingeben");
                if (login == null) {
                    logger.warning("Couldn't find login form");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.setAction(MAINPAGE.string + "/api/user/login");
                login.put("login", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br2 = br.cloneBrowser();
                br2.submitForm(login);

                if (br2.containsHTML("\"error\":\"CaptchaRequired\"") && cid == null) {
                    logger.warning("cid = null, captcha is required to login");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (br2.containsHTML("\"error\":\"CaptchaRequired\"") && cid != null) {
                    DownloadLink dummy = new DownloadLink(null, null, null, null, true);
                    PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.setId(cid);
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, dummy);
                    login.put("recaptcha_challenge_field", rc.getChallenge());
                    login.put("recaptcha_response_field", Encoding.urlEncode(c));
                    br2.submitForm(login);
                    if (br2.containsHTML("\"error\":\"CaptchaInvalid\"")) {
                        logger.info("Invalid Captcha response!");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br = br2.cloneBrowser();
                if (br.getCookie(MAINPAGE.string, "autologin") == null && br.containsHTML("\"error\":\"InvalidLogIn\"")) {
                    logger.info("Invalid login criteria");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(MAINPAGE.string, "autologin") == null && !br.containsHTML("\"error\":\"InvalidLogIn\"")) {
                    logger.info("Depositfiles website login method  == failed! Possible plugin error, please report this to JDownloader Development Team");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    logger.info("Depositfiles website login method == success!");
                }
                br.setFollowRedirects(false);

                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE.string);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                account.setProperty("uprand", br.getCookie(MAINPAGE.string, "uprand"));
            } catch (final PluginException e) {
                account.setProperty("free", Property.NULL);
                account.setProperty("cookies", Property.NULL);
                account.setProperty("uprand", Property.NULL);
                if (e instanceof PluginException && e.getLinkStatus() == 4194304) useAPI.set(true);
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setConstants();
        requestFileInformation(downloadLink);
        synchronized (PREMLOCK) {
            if (isFreeAccount(account, false)) {
                simultanpremium.set(1);
            } else {
                if (simultanpremium.get() + 1 > 20) {
                    simultanpremium.set(20);
                } else {
                    simultanpremium.incrementAndGet();
                }
            }
        }
        if (useAPI.get()) {
            try {
                apiHandlePremium(downloadLink, account);
            } catch (PluginException e) {
                if (e instanceof PluginException && e.getLinkStatus() == 4194304)
                    useAPI.set(false);
                else
                    throw e;
            }
        }
        if (!useAPI.get()) {
            webHandlePremium(downloadLink, account);
        }
    }

    private void webHandlePremium(final DownloadLink downloadLink, Account account) throws Exception {
        webLogin(account, false);
        if (isFreeAccount(account, true)) {
            br.getPage(downloadLink.getDownloadURL());
            doFree(downloadLink);
        } else {
            String link = downloadLink.getDownloadURL();
            br.getPage(link);
            if (br.getRedirectLocation() != null) {
                link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
                br.getPage(link);
            }
            checkErrors();
            String passCode = downloadLink.getStringProperty("pass", null);
            if (br.containsHTML("\"file_password\"")) {
                logger.info("This file seems to be password protected.");
                if (passCode == null) passCode = getUserInput(null, downloadLink);
                br.postPage(br.getURL().replaceFirst("https?://", protocol), "file_password=" + passCode);
                logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
                if (br.containsHTML("(>The file's password is incorrect. Please check your password and try to enter it again\\.<|\"file_password\")")) {
                    logger.info("The entered password (" + passCode + ") was wrong, retrying...");
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                downloadLink.setProperty("pass", passCode);
            }
            checkErrors();
            link = br.getRegex(PATTERN_PREMIUM_FINALURL).getMatch(0);
            if (link == null) {
                synchronized (LOCK) {
                    account.setProperty("cookies", null);
                    account.setProperty("free", (Boolean) null);
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
            if (passCode != null) downloadLink.setProperty("pass", passCode);
            dl.startDownload();
        }
    }

    private String newAppVersion = "2114";

    private String apiKeyVal() {
        return "key" + (int) (Math.random() * 10000.0D) + "=val" + new Date().getTime();
    }

    private final AtomicBoolean newVC = new AtomicBoolean(false);

    @SuppressWarnings("unused")
    private boolean versionCheck() throws Exception {
        if (newVC.get()) {
            return true;
        } else {
            br.getHeaders().put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
            br.getPage("http://depositfiles.com/api/update/check?" + apiKeyVal() + "&appkey=dfmanager2&version=" + newAppVersion);
            br.getHeaders().put("Cache-Control", "no-cache");
            if (br.containsHTML("\"data\":\\{\"status\":\"UpToDate\"\\}"))
                return true;
            else
                // not sure if we need to bother?? set to true for now.
                return true;
        }
    }

    private HashMap<String, Object> accountData = new HashMap<String, Object>();

    private void saveAccountData(Account account) {
        if (!accountData.isEmpty())
            account.setProperty("accountData", accountData);
        else
            account.setProperty("accountData", Property.NULL);
    }

    private void setConstants(Account account) {
        @SuppressWarnings("unchecked")
        HashMap<String, Object> tempHM = (HashMap<String, Object>) account.getProperty("accountData", null);
        if (tempHM != null) accountData = tempHM;
    }

    private void getPage(String url) throws Exception {
        br = new Browser();
        apiPrepBr(br);
        br.getPage(fixLinkSSL(url));
    }

    @SuppressWarnings("unchecked")
    private String getToken(Account account) {
        String result = null;
        HashMap<String, Object> accountData = (HashMap<String, Object>) account.getProperty("accountData");
        if (accountData != null && accountData.containsKey("token")) {
            result = accountData.get("token").toString();
        }
        return result.replace("%2F", "/");
    }

    private Browser apiPrepBr(Browser ibr) {
        ibr.getHeaders().put("User-Agent", "Java/" + System.getProperty("java.version"));
        ibr.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        ibr.getHeaders().put("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
        ibr.getHeaders().put("Accept-Language", null);
        ibr.getHeaders().put("Accept-Charset", null);
        ibr.getHeaders().put("Accept-Encoding", null);
        ibr.getHeaders().put("Pragma", null);
        return ibr;
    }

    /**
     * new AccountInfo method for /api/
     * 
     * @author raztoki
     */
    private AccountInfo apiFetchAccountInfo(final Account account) throws Exception {
        logger.info("apiFetchAccountInfo method in use!");
        AccountInfo ai = new AccountInfo();
        setConstants(account);
        // this shouldn't be needed!
        // if (versionCheck()) {
        try {
            getPage("http://depositfiles.com/api/user/login?" + "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&" + apiKeyVal());
            if (br.containsHTML("\"error\":\"CaptchaRequired\"")) {
                for (int i = 0; i <= 2; i++) {
                    DownloadLink dummy = new DownloadLink(null, null, null, null, true);
                    PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.setId("6LdRTL8SAAAAAE9UOdWZ4d0Ky-aeA7XfSqyWDM2m");
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, dummy);
                    if (c == null || c.equals("")) {
                        logger.warning(this.getHost() + "requires captcha query in order to login, you've entered nothing or blank captcha respsonse. Aborting login sequence");
                        account.setValid(false);
                        return ai;
                    }
                    br.getPage("http://depositfiles.com/api/user/login?recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&" + apiKeyVal() + "&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    if (br.containsHTML("\"error\":\"CaptchaInvalid\""))
                        logger.info("Invalid Captcha response!");
                    else
                        break;
                }
            }
            if (br.containsHTML("\"error\":\"LoginInvalid\"")) {
                logger.warning("Invalid Login (user:password)!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (br.containsHTML("\"error\":\"CaptchaInvalid\"")) {
                logger.info("Invalid Captcha response! Excausted retry count");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            String token = getJson("token");
            if (token != null) {
                token = token.replaceAll("\\\\/", "/");
                token = Encoding.urlEncode(token);
            } else
                token = br.getCookie(br.getHost(), "autologin");
            if (token == null) {
                logger.warning("Could not find 'token'");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            accountData.put("token", (Object) token);
            String passKey = getJson("member_passkey");
            if (passKey == null) {
                logger.warning("Could not find 'passKey'");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            accountData.put("passKey", (Object) passKey);
            String mode = getJson("mode");
            if (mode == null) {
                logger.warning("Could not find 'mode'");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            accountData.put("mode", (Object) mode);
            if ("gold".equalsIgnoreCase(mode)) {
                account.setProperty("free", false);
                try {
                    account.setMaxSimultanDownloads(-1);
                    account.setConcurrentUsePossible(true);
                } catch (Throwable e) {
                }
                // no expire date shown by API within login... kinda lame I know... && cookie session is set with login
                br.setFollowRedirects(true);
                // we need to save cookies from browser into mainpage, otherwise cookies session wont send with request!
                mainpageCookies(br);
                br.getPage(MAINPAGE.string + "/de/gold/");
                String expire = br.getRegex("Gold-Zugriff: <b>(.*?)</b></div>").getMatch(0);
                if (expire == null) expire = br.getRegex("Gold Zugriff bis: <b>(.*?)</b></div>").getMatch(0);
                if (expire == null) expire = br.getRegex("Gold(-| )(Zugriff|Zugang)( bis)?: <b>(.*?)</b></div>").getMatch(3);
                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
                if (expire == null) {
                    ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountbad", "Account expired or not valid."));
                    account.setProperty("premium", Property.NULL);
                    account.setProperty("accountData", Property.NULL);
                    account.setValid(false);
                    return ai;
                }
                try {
                    Date date;
                    date = dateFormat.parse(expire);
                    ai.setValidUntil(date.getTime());
                } catch (final ParseException e) {
                    logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                }
            } else {
                account.setProperty("free", true);
                try {
                    account.setMaxSimultanDownloads(1);
                    account.setConcurrentUsePossible(false);
                } catch (Throwable e) {
                }
            }
            saveAccountData(account);
            ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountok", "Premium Account is ok"));
            account.setValid(true);
        } catch (PluginException e) {
            account.setProperty("accountData", Property.NULL);
            account.setProperty("free", Property.NULL);
            if (e instanceof PluginException && e.getLinkStatus() == 4194304)
                useAPI.set(false);
            else {
                ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountbad", "Account expired or not valid."));
                account.setValid(false);
                return ai;
            }
        }
        // }
        return ai;
    }

    // defaults do not change
    private boolean apiResumes = true;
    private int     apiChunks  = 0;

    private void apiHandlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setConstants(account);
        String pass = downloadLink.getStringProperty("pass", null);
        if (account.getBooleanProperty("free")) {
            logger.info(account.getUser() + " @ Free Account :: API download method in use");
            apiResumes = true;
            apiChunks = 1;
        } else {
            logger.info(account.getUser() + " @ Gold Account :: API download method in use");
            apiResumes = true;
            apiChunks = 0;
        }
        // atm they share the same dl routine that I can see. Download program indicates captcha!
        getPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + fuid(downloadLink) + "&" + apiKeyVal() + (pass != null ? "&file_password=" + Encoding.urlEncode(pass) : ""));
        if (br.containsHTML("\"error\":\"FileIsPasswordProtected\"")) {
            logger.info("This file seems to be password protected.");
            for (int i = 0; i <= 2; i++) {
                if (pass == null) pass = getUserInput(null, downloadLink);
                if (pass == null || pass.equals("")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download requires valid password");
                getPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + fuid(downloadLink) + "&" + apiKeyVal() + "&file_password=" + pass);
                if ("FilePasswordIsIncorrect".equalsIgnoreCase(getJson("error"))) {
                    pass = null;
                    continue;
                } else {
                    downloadLink.setProperty("pass", pass);
                    break;
                }
            }
        }
        if ("FilePasswordIsIncorrect".equalsIgnoreCase(getJson("error"))) throw new PluginException(LinkStatus.ERROR_FATAL, "File Password protected!");
        if (account.getBooleanProperty("free", false)) {
            String mode = getJson("mode");
            String delay = getJson("delay");
            String dlToken = getJson("download_token");
            if (delay == null && mode == null && dlToken == null) {
                logger.warning("api epic fail");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // download modes seem to indicate if the user can download as 'gold' or 'free' connection ratios?. User can download there
            // own uploads under gold even though they don't have gold account status.
            if (mode != null && "gold".equalsIgnoreCase(mode) && account.getBooleanProperty("free", false)) {
                apiResumes = true;
                apiChunks = 0;
            } else {
                int deley = Integer.parseInt(delay);
                sleep(deley * 1001, downloadLink);
                getPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + fuid(downloadLink) + "&" + apiKeyVal() + (pass != null ? "&file_password=" + Encoding.urlEncode(pass) : "") + "&download_token=" + dlToken);
                if (br.containsHTML("\"error\":\"CaptchaRequired\"")) {
                    for (int i = 0; i <= 2; i++) {
                        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        rc.setId("6LdRTL8SAAAAAE9UOdWZ4d0Ky-aeA7XfSqyWDM2m");
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode(cf, downloadLink);
                        if (c == null || c.equals("")) {
                            logger.warning("User aborted/cancelled captcha");
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        getPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + fuid(downloadLink) + "&" + apiKeyVal() + (pass != null ? "&file_password=" + Encoding.urlEncode(pass) : "") + "&download_token=" + dlToken + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                        if (br.containsHTML("\"error\":\"CaptchaInvalid\""))
                            logger.info("Invalid Captcha response!");
                        else
                            break;
                    }
                }
                if (br.containsHTML("\"error\":\"CaptchaInvalid\"")) {
                    logger.info("Exausted tries");
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
        }
        String dllink = getJson("download_url");
        if (dllink == null) {
            logger.warning("Could not find 'dllink'");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replaceAll("\\\\/", "/");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, apiResumes, apiChunks);
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

    private String fuid(DownloadLink downloadLink) {
        String result = new Regex(downloadLink.getDownloadURL(), "files/([^/]+)").getMatch(0);
        return result;
    }

    private String getJson(String object) {
        String result = br.getRegex("\"" + object + "\":\"([^\"]+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + object + "\":(\\d+)").getMatch(0);
        return result;
    }

    private void mainpageCookies(Browser ibr) {
        String current_host = new Regex(ibr.getURL(), "(https?://[^/]+)").getMatch(0);
        /** Save cookies */
        final Cookies add = ibr.getCookies(current_host);
        for (final Cookie c : add.getCookies()) {
            br.setCookie(MAINPAGE.string, c.getKey(), c.getValue());
        }
    }

    private String checkDirectLink(DownloadLink downloadLink) {
        String finallink = downloadLink.getStringProperty("finallink", null);
        if (finallink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(finallink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty("finallink", Property.NULL);
                    finallink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty("finallink", Property.NULL);
                finallink = null;
            }
        }
        return finallink;
    }

    private boolean userChangedSslSetting() {
        if (MAINPAGE != null && MAINPAGE.string != null && (checkSsl() && MAINPAGE.string.startsWith("http://")) || (!checkSsl() && MAINPAGE.string.startsWith("https://")))
            return true;
        else
            return false;
    }

    private boolean checkSsl() {
        return getPluginConfig().getBooleanProperty(SSL_CONNECTION, false);
    }

    private String fixLinkSSL(String link) {
        if (checkSsl())
            link = link.replace("http://", "https://");
        else
            link = link.replace("https://", "http://");
        return link;
    }

    private boolean isJava7nJDStable() {
        if (System.getProperty("jd.revision.jdownloaderrevision") == null && System.getProperty("java.version").matches("1\\.[7-9].+"))
            return true;
        else
            return false;
    }

    private static AtomicBoolean stableSucks = new AtomicBoolean(false);

    public static void showSSLWarning(final String domain) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        boolean xSystem = CrossSystem.isOpenBrowserSupported();
                        if ("de".equalsIgnoreCase(lng)) {
                            title = domain + " :: Java 7+ && HTTPS Post Requests.";
                            message = "Wegen einem Bug in in Java 7+ in dieser JDownloader version koennen wir keine HTTPS Post Requests ausfuehren.\r\n";
                            message += "Wir haben eine Notloesung ergaenzt durch die man weiterhin diese JDownloader Version nutzen kann.\r\n";
                            message += "Bitte bedenke, dass HTTPS Post Requests als HTTP gesendet werden. Nutzung auf eigene Gefahr!\r\n";
                            message += "Falls du keine unverschluesselten Daten versenden willst, update bitte auf JDownloader 2!\r\n";
                            if (xSystem)
                                message += "JDownloader 2 Installationsanleitung und Downloadlink: Klicke -OK- (per Browser oeffnen)\r\n ";
                            else
                                message += "JDownloader 2 Installationsanleitung und Downloadlink:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                        } else {
                            title = domain + " :: Java 7+ && HTTPS Post Requests.";
                            message = "Due to a bug in Java 7+ when using this version of JDownloader, we can not succesfully send HTTPS Post Requests.\r\n";
                            message += "We have added a work around so you can continue to use this version of JDownloader...\r\n";
                            message += "Please be aware that HTTPS Post Requests are sent as HTTP. Use at your own discretion.\r\n";
                            message += "If you do not want to send unecrypted data, please upgrade to JDownloader 2!\r\n";
                            if (xSystem)
                                message += "Jdownloader 2 install instructions and download link: Click -OK- (open in browser)\r\n ";
                            else
                                message += "JDownloader 2 install instructions and download link:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                        }
                        int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.CLOSED_OPTION, JOptionPane.CLOSED_OPTION);
                        if (xSystem && JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=37365"));
                        stableSucks.set(true);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SSL_CONNECTION, JDL.L("plugins.hoster.HotFileCom.com.preferSSL", "Use Secure Communication over SSL (HTTPS://)")).setDefaultValue(false));
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
        br.setCookie(MAINPAGE.string, "lang_current", "de");
    }

    @Override
    public String getAGBLink() {
        setMainpage();
        return MAINPAGE.string + "/en/agreem.html";
    }

}