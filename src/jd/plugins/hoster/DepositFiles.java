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

    private static final String   UA                       = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.57 Safari/537.17";
    private static final String   FILE_NOT_FOUND           = "Dieser File existiert nicht|Entweder existiert diese Datei nicht oder sie wurde";
    private static final String   PATTERN_PREMIUM_FINALURL = "<div id=\"download_url\">.*?<a href=\"(.*?)\"";
    public static StringContainer MAINPAGE                 = new StringContainer();
    public static final String    DOMAINS                  = "(depositfiles\\.(com|org)|dfiles\\.(eu|ru))";

    public String                 DLLINKREGEX2             = "<div id=\"download_url\" style=\"display:none;\">.*?<form action=\"(.*?)\" method=\"get";
    private final Pattern         FILE_INFO_NAME           = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);
    private final Pattern         FILE_INFO_SIZE           = Pattern.compile(">Datei Gr.*?sse: <b>([^<>\"]*?)</b>");

    private static Object         PREMLOCK                 = new Object();
    private static Object         LOCK                     = new Object();

    private static AtomicInteger  simultanpremium          = new AtomicInteger(1);
    private static AtomicBoolean  useWebLogin              = new AtomicBoolean(false);

    private static final String   SSL_CONNECTION           = "SSL_CONNECTION";
    private boolean               PREFERSSL                = false;

    public DepositFiles(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("http://depositfiles.com/signup.php?ref=down1");
    }

    public void setMainpage() {
        if (MAINPAGE == null || MAINPAGE.string == null) {
            try {
                Browser testBr = new Browser();
                testBr.setFollowRedirects(true);
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        setBrowserExclusive();
        setMainpage();
        synchronized (LOCK) {
            try {
                login(account, true);
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

    @Override
    public String getAGBLink() {
        setMainpage();
        return MAINPAGE.string + "/en/agreem.html";
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
        setMainpage();
        String finallink = checkDirectLink(downloadLink);
        checkShowFreeDialog();
        setBrowserExclusive();
        String passCode = downloadLink.getStringProperty("pass", null);
        br.forceDebug(true);
        requestFileInformation(downloadLink);
        if (finallink == null) {
            String link = fixLinkSSL(downloadLink.getDownloadURL());
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
                form.setAction("");
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

    // TODO: The handleFree supports password protected links, handlePremium not
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        synchronized (PREMLOCK) {
            login(account, false);
            if (isFreeAccount(account, false)) {
                simultanpremium.set(1);
                handleFree(downloadLink);
                return;
            } else {
                if (simultanpremium.get() + 1 > 20) {
                    simultanpremium.set(20);
                } else {
                    simultanpremium.incrementAndGet();
                }
            }
        }

        String link = downloadLink.getDownloadURL();
        br.getPage(fixLinkSSL(link));
        if (br.getRedirectLocation() != null) {
            link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
            br.getPage(link);
        }
        checkErrors();
        String passCode = downloadLink.getStringProperty("pass", null);
        if (br.containsHTML("\"file_password\"")) {
            logger.info("This file seems to be password protected.");
            if (passCode == null) passCode = getUserInput(null, downloadLink);
            br.postPage(br.getURL(), "file_password=" + passCode);
            logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
            if (br.containsHTML("(>The file's password is incorrect. Please check your password and try to enter it again\\.<|\"file_password\")")) {
                logger.info("The entered password (" + passCode + ") was wrong, retrying...");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
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
        if (passCode != null) downloadLink.setProperty("pass", passCode);
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
            if (!br.getURL().contains("/gold/")) br.getPage(MAINPAGE.string + "/de/gold/");
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
            brc.setFollowRedirects(true);
            // I assume they don't redirect for this subdomain, just incase...
            brc.getPage(fixLinkSSL("http://system.depositfiles.com/api/get_downloader_version.php"));
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
                br.getHeaders().put("User-Agent", UA);
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
                            this.br.setCookie(MAINPAGE.string, key, value);
                        }
                        return;
                    }
                }
                String dmVersion = getDonloadManagerVersion();
                if (dmVersion != null) {
                    // depositfiles download program login method.
                    logger.info("Depositfiles download program login method!");
                    br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru; rv:1.8.1.20) DepositFiles/FileManager " + dmVersion);
                    br.setFollowRedirects(true);
                    Thread.sleep(2000);
                    br.postPage(MAINPAGE.string + "/de/login.php", "go=1&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    if (br.getCookie(MAINPAGE.string, "autologin") == null && br.containsHTML(">Ihr Passwort oder Login ist falsch<")) {
                        logger.info("Invalid login criteria");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if (br.getCookie(MAINPAGE.string, "autologin") == null && !br.containsHTML(">Ihr Passwort oder Login ist falsch<")) {
                        logger.info("Depositfiles download program login method  == failed! Possible plugin error, please report this to JDownloader Development Team");
                        useWebLogin.set(true);
                    } else {
                        logger.info("Depositfiles download program login method  == success!");
                    }
                }
                if (dmVersion == null || useWebLogin.get() == true) {
                    // web fail over method
                    logger.info("Depositfiles website login method!");
                    String uprand = account.getStringProperty("uprand", null);
                    if (uprand != null) br.setCookie(MAINPAGE.string, "uprand", uprand);
                    br.setReadTimeout(3 * 60 * 1000);
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
                }
                br.setFollowRedirects(false);

                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE.string);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                if (dmVersion != null) account.setProperty("dmVersion", dmVersion);
                account.setProperty("uprand", br.getCookie(MAINPAGE.string, "uprand"));
                account.setProperty("UA", br.getHeaders().get("User-Agent"));
            } catch (final PluginException e) {
                account.setProperty("premium", Property.NULL);
                account.setProperty("cookies", Property.NULL);
                account.setProperty("UA", Property.NULL);
                account.setProperty("uprand", Property.NULL);
                useWebLogin.set(false);
                throw e;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setMainpage();
        correctDownloadLink(downloadLink);
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", UA);
        final String link = downloadLink.getDownloadURL();
        setLangtoGer();
        /* needed so the download gets counted,any referer should work */
        // br.getHeaders().put("Referer", "http://www.google.de");
        br.setFollowRedirects(false);
        br.getPage(fixLinkSSL(link));

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

    private void checkSsl() {
        PREFERSSL = getPluginConfig().getBooleanProperty(SSL_CONNECTION, false);
        if (oldStyle() == true) PREFERSSL = false;
    }

    private String fixLinkSSL(String link) {
        checkSsl();
        if (PREFERSSL)
            link = link.replace("http://", "https://");
        else
            link = link.replace("https://", "http://");
        return link;
    }

    private boolean oldStyle() {
        String style = System.getProperty("ftpStyle", null);
        if ("new".equalsIgnoreCase(style)) return false;
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        int rev = Integer.parseInt(prev);
        if (rev < 10000) return true;
        return false;
    }

    @SuppressWarnings("deprecation")
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