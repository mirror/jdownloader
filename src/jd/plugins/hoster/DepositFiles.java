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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "depositfiles.com" }, urls = { "https?://(www\\.)?(depositfiles\\.(com|org)|dfiles\\.(eu|ru))(/\\w{1,3})?/files/[\\w]+" })
public class DepositFiles extends antiDDoSForHost {
    private final String                  UA                           = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 Safari/537.36";
    private final String                  FILE_NOT_FOUND               = "Dieser File existiert nicht|Entweder existiert diese Datei nicht oder sie wurde";
    private final String                  downloadLimitReached         = "<strong>Achtung! Sie haben ein Limit|Sie haben Ihre Download Zeitfrist erreicht\\.<";
    private final String                  PATTERN_PREMIUM_FINALURL     = "<div id=\"download_url\".*?<a href=\"(.*?)\"";
    public static AtomicReference<String> MAINPAGE                     = new AtomicReference<String>();
    public static final String            DOMAINS                      = "(depositfiles\\.(com|org)|dfiles\\.(eu|ru))";
    private String                        protocol                     = null;
    public String                         DLLINKREGEX2                 = "<div id=\"download_url\" style=\"display:none;\">.*?<form action=\"(.*?)\" method=\"get";
    private final Pattern                 FILE_INFO_NAME               = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);
    private final Pattern                 FILE_INFO_SIZE               = Pattern.compile(">Datei Gr.*?sse: <b>([^<>\"]*?)</b>");
    private static Object                 PREMLOCK                     = new Object();
    private static Object                 LOCK                         = new Object();
    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger          totalMaxSimultanFreeDownload = new AtomicInteger(20);
    /* don't touch the following! */
    private static AtomicInteger          maxFree                      = new AtomicInteger(1);
    private static AtomicInteger          simultanpremium              = new AtomicInteger(1);
    private static AtomicBoolean          useAPI                       = new AtomicBoolean(true);
    private final String                  SETTING_SSL_CONNECTION       = "SSL_CONNECTION";

    // private final String SETTING_PREFER_SOLVEMEDIA = "SETTING_PREFER_SOLVEMEDIA";
    // @Override
    // public String[] siteSupportedNames() {
    // return new String[] { "depositfiles.com", "depositfiles.org", "dfiles.eu", "dfiles.ru" };
    // }
    public DepositFiles(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("http://depositfiles.com/signup.php?ref=down1");
    }

    public void setMainpage() {
        synchronized (MAINPAGE) {
            if (MAINPAGE.get() == null || userChangedSslSetting()) {
                try {
                    Browser testBr = new Browser();
                    testBr.setFollowRedirects(true);
                    // NOTE: https requests do not trigger redirects
                    // NOTE: http also get filtered by ISP/Government Policy to the incorrect domain! - raztoki20160114
                    testBr.getPage(fixLinkSSL("http://depositfiles.com"));
                    String baseURL = new Regex(testBr.getURL(), "(https?://[^/]+)").getMatch(0);
                    if (baseURL != null && Browser.getHost(baseURL).matches(DOMAINS)) {
                        MAINPAGE.set(baseURL);
                        System.out.println("depositfiles setter MAINPAGE = " + MAINPAGE.get());
                    } else {
                        // Houston we have a problem!
                        System.out.println("despostfiles checker failed, setting failover");
                        MAINPAGE.set(fixLinkSSL("http://depositfiles.com"));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    System.out.println("despostfiles setter failed, setting failover");
                    MAINPAGE.set(fixLinkSSL("http://depositfiles.com"));
                }
            }
            System.out.println("depositfiles setter MAINPAGE = " + MAINPAGE.get());
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
        if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        setMainpage();
        final String currentDomain = Browser.getHost(link.getDownloadURL(), false);
        if (!currentDomain.matches(DOMAINS)) {
            // this is needed to fix old users bad domain name corrections (say hotpspots/gateways)
            String mainPage = MAINPAGE.get();
            if (mainPage != null) {
                mainPage = mainPage.replace("https://", "").replace("http://", "");
                link.setUrlDownload(link.getDownloadURL().replace(currentDomain, mainPage));
            }
        }
        final String newLink = link.getDownloadURL().replaceAll(DOMAINS + "(/.*?)?/files", MAINPAGE.get().replaceAll("https?://(www\\.)?", "") + "/de/files");
        link.setUrlDownload(fixLinkSSL(newLink));
    }

    protected void showFreeDialog(final String domain) {
        if (System.getProperty("org.jdownloader.revision") != null) { /* JD2 ONLY! */
            super.showFreeDialog(domain);
            return;
        }
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
                            if (JOptionPane.OK_OPTION == result) {
                                CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + domain + "&freedialog"));
                            }
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
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
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
        {
            /*
             * ok redirection can happen here due to user using a proxy and our test for 'primary domain is done once!, and not every time
             * access happens'
             */
            String rd = br.getRedirectLocation();
            if (rd != null) {
                final String path1 = new Regex(link, Pattern.quote(Browser.getHost(link)) + "(/.+)$").getMatch(0);
                // redirect location might be relative.
                final String host2 = Browser.getHost(rd = Request.getLocation(rd, br.getRequest()));
                final String path2 = new Regex(rd, Pattern.quote(host2) + "(/.+)$").getMatch(0);
                if (path1 != null && path2 != null && path1.equals(path2)) {
                    br.getPage(rd);
                } else {
                    // problem ?
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        // Datei geloescht?
        if (br.containsHTML(FILE_NOT_FOUND)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(downloadLimitReached)) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.depositfilescom.errors.limitreached", "Download limit reached"));
            return AvailableStatus.TRUE;
        }
        String fileName = br.getRegex(FILE_INFO_NAME).getMatch(0);
        if (fileName == null) {
            String eval = br.getRegex("class=\"info\".*?unescape\\('(.*?)'").getMatch(0);
            if (eval != null) {
                eval = Encoding.unicodeDecode(eval);
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
        if (fixedName != null) {
            fileName = fixedName;
        }
        downloadLink.setName(Encoding.htmlDecode(fileName));
        downloadLink.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(fileSizeString)));
        return AvailableStatus.TRUE;
    }

    public void checkErrors() throws NumberFormatException, PluginException {
        logger.info("Checking errors...");
        if (br.containsHTML("Zugang zur folgenden Datei ist begrenzt oder Datei wurde entfernt|Diese Datei besteht nicht, der Zugang zur folgenden Datei ist begrenzt oder Datei wurde entfernt, wegen der Urheberrechtsverletzung\\.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("This file does not exist, the access to the following file is limited or it has been removed due to infringement of copyright")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("Leider, sind alle Slots f")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Free Downloadslot", 20 * 60 * 1000l);
        }
        if (br.containsHTML("html_download_api-not_exists")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Server under maintenance */
        if (br.containsHTML("(html_download_api-temporary_unavailable|The site is temporarily unavailable for we are making some important upgrades)")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Under maintenance, contact depositfiles support", 30 * 60 * 1000l);
        }
        /* download not available at the moment */
        if (br.containsHTML("Entschuldigung aber im Moment koennen Sie nur diesen Downloadmodus")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
        }
        /* limit reached */
        if (br.containsHTML("(Sie haben ein Limit fuer Downloaden ausgeschoepft|You used up your limit|Please try in|You have reached your download time limit)")) {
            String wait = br.getRegex("html_download_api-limit_interval\">(\\d+)</span>").getMatch(0);
            if (wait != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1000l);
            }
            wait = br.getRegex(">Try in (\\d+) minutes or use GOLD account").getMatch(0);
            if (wait != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(wait) + 1) * 60 * 1000l);
            }
            wait = br.getRegex(">Try in (\\d+) seconds or use GOLD account").getMatch(0);
            if (wait != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
        }
        if (br.containsHTML("(Anschlusslimit|Bitte versuchen Sie in|Sie haben Ihre Download Zeitfrist erreicht\\.<)")) {
            String wait = br.getRegex("versuchen Sie in.*?(\\d+) minu").getMatch(0);
            if (wait != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(wait) + 1) * 60 * 1000l);
            }
            wait = br.getRegex("versuchen Sie in.*?(\\d+) Seku").getMatch(0);
            if (wait != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1000l);
            }
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
        if (br.containsHTML("Von Ihren IP-Addresse werden schon einige")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1001l);
        }
        if (br.containsHTML("You cannot download more than one file in parallel")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1001l);
        }
        /* unknown error, try again */
        final String wait = br.getRegex("Bitte versuchen Sie noch mal nach(.*?)<\\/strong>").getMatch(0);
        if (wait != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, TimeFormatter.getMilliSeconds(wait));
        }
        /* You have exceeded the 15 GB 24-hour limit */
        if (br.containsHTML("GOLD users can download no more than")) {
            logger.info("GOLD users can download no more than 15 GB for the last 24 hours");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if (br.containsHTML("Entweder existiert diese Datei nicht oder sie wurde aufgrund von")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private String getDllink() throws Exception {
        String crap = br.getRegex("document\\.getElementById\\('download_container'\\)\\.innerHTML = '(.*?';)").getMatch(0);
        if (crap == null && br.containsHTML("download_container")) {
            crap = br.getRegex("download_container.*load\\((.*?)\n").getMatch(0);
        } else {
            String finallink = br.getRegex("class=\"download_url\">[\t\n\r ]+<a href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(https?://[a-z0-9\\-]+\\.dfiles\\.eu/auth[^<>\"]*?)\"").getMatch(0);
            }
            return finallink;
        }
        if (crap != null) {
            crap = crap.replaceAll("('| |;|\\+|\\(|\\)|\t|\r|\n)", "");
            final String[] lol = HTMLParser.getHttpLinks(crap, "");
            if (lol == null || lol.length == 0) {
                if (!crap.contains("depositfiles") && crap.contains("php?")) {
                    return MAINPAGE.get() + crap;
                } else {
                    return null;
                }
            }
            return lol[0];
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
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
        logger.info("Free @ Guest :: Web download method in use");
        setConstants();
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        checkShowFreeDialog();
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
                dllink = fixLinkSSL(dllink);
                // handling for txt file downloadlinks, dunno why they made a completely different page for txt files
                dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
                final URLConnectionAdapter con = dl.getConnection();
                if (Plugin.getFileNameFromHeader(con) == null || Plugin.getFileNameFromHeader(con).indexOf("?") >= 0) {
                    con.disconnect();
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                if (!con.isContentDisposition()) {
                    con.disconnect();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
                }
                downloadLink.setProperty("finallink", dllink);
                dl.startDownload();
                return;
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
                        br.setCookie(MAINPAGE.get(), Keks[0], "");
                    }
                    if (Keks.length == 2) {
                        br.setCookie(MAINPAGE.get(), Keks[0], Keks[1]);
                    }
                }
                br.submitForm(form);
                long timeBefore = System.currentTimeMillis();
                checkErrors();
                if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) {
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                if (br.containsHTML("\"file_password\"")) {
                    logger.info("This file seems to be password protected.");
                    if (passCode == null) {
                        passCode = getUserInput(null, downloadLink);
                    }
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
                // wait required here before ajax
                sleep(((wait != null ? Integer.parseInt(wait) : 60) * 1000l) - (System.currentTimeMillis() - timeBefore), downloadLink);
                // // ajax request, here they give you more html && js. Think this is where the captcha type is determined.
                ajaxGetPage("/get_file.php?fid=" + fid);
                // this changes each time you load page...
                final String ck = ajax.getRegex("ACPuzzleKey\\s*=\\s*('|\")(.*?)\\1").getMatch(1);
                if (ck != null) {
                    // lets prefer solvemedia as it can be passed into CES/headless as browser not required
                    final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                    final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                    sm.setChallengeKey(ck);
                    File cf = null;
                    for (int i = 0; i <= 3; i++) {
                        try {
                            cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        } catch (final Exception e) {
                            if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                                throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                            }
                            throw e;
                        }
                        final String code = getCaptchaCode("solvemedia", cf, downloadLink);
                        final String chid = sm.getChallenge(code);
                        if (chid == null) {
                            if (i + 1 != 3) {
                                // wrong answer
                                continue;
                            } else {
                                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                            }
                        }
                        finallink = submitCapthcaStep("fid=" + fid + "&challenge=" + Encoding.urlEncode(chid) + "&response=" + Encoding.urlEncode(code) + "&acpuzzle=1");
                        break;
                    }
                } else {
                    // recaptcha v2.
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LdyfgcTAAAAAArE1fk9cGyExtKfT4a12dWcViye").getToken();
                    finallink = submitCapthcaStep("fid=" + fid + "&challenge=null&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response) + "&response=null");
                }
                if (finallink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        logger.info("finallink = " + finallink);
        finallink = fixLinkSSL(finallink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, finallink, true, 1);
        final URLConnectionAdapter con = dl.getConnection();
        if (Plugin.getFileNameFromHeader(con) == null || Plugin.getFileNameFromHeader(con).indexOf("?") >= 0) {
            if (!con.isContentDisposition()) {
                con.disconnect();
                if (con.getHeaderField("Guest-Limit") != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
                }
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
            if (br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String name = Plugin.getFileNameFromHeader(con);
        if (name != null && name.contains("?")) {
            /* fix invalid filenames */
            String fixedName = new Regex(name, "(.+)\\?").getMatch(0);
            downloadLink.setFinalFileName(fixedName);
        }
        downloadLink.setProperty("finallink", finallink);
        try {
            /* add a download slot */
            controlFree(+1);
            /* start the dl */
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlFree(-1);
        }
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlFree
     *            (+1|-1)
     */
    private synchronized void controlFree(final int num) {
        logger.info("maxFree was = " + maxFree.get());
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxFree.get());
    }

    private final String submitCapthcaStep(final String page) throws Exception {
        // Important! Setup Header
        ajaxGetPage("/get_file.php?" + page);
        if (ajax.containsHTML("(onclick=\"check_recaptcha|load_recaptcha)")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String finallink = ajax.getRegex("\"((?:https?:)?//fileshare\\d+\\." + DOMAINS + "/auth.*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = ajax.getRegex("<form [^>]*action=\"((?:https?:)?//.*?)\"").getMatch(0);
        }
        return finallink;
    }

    private Browser ajax = null;

    private void ajaxGetPage(final String page) throws Exception {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getPage(page);
    }

    private void setConstants() {
        // set session protocol based on user setting and jd version && java version
        protocol = fixLinkSSL("https://");
    }

    // check that can be done pre login based on cached data.
    public boolean isFreeAccountPre(Account acc) {
        if (acc.getType() == AccountType.PREMIUM) {
            return false;
        }
        if (accountData != null && accountData.containsKey("mode")) {
            if ("gold".equalsIgnoreCase(accountData.get("mode").toString())) {
                return false;
            }
        }
        return true;
    }

    // post login check
    public boolean isFreeAccountPost() throws Exception {
        synchronized (LOCK) {
            setLangtoGer();
            if (!br.getURL().contains("/gold/")) {
                final boolean ifr = br.isFollowingRedirects();
                br.setFollowRedirects(true);
                br.getPage(MAINPAGE.get() + "/de/gold/");
                br.setFollowRedirects(ifr);
            }
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
            return ret;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setBrowserExclusive();
        AccountInfo ai = new AccountInfo();
        synchronized (LOCK) {
            // via /api/
            if (useAPI.get()) {
                ai = apiFetchAccountInfo(account);
            } else {
                setMainpage();
                ai = webFetchAccountInfo(account);
            }
            return ai;
        }
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
        if (isFreeAccountPost()) {
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            account.setType(AccountType.FREE);
            account.setValid(true);
            return ai;
        } else {
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
            String expire = br.getRegex("Gold-Zugriff: <b>(.*?)</b></div>").getMatch(0);
            if (expire == null) {
                expire = br.getRegex("Gold Zugriff bis: <b>(.*?)</b></div>").getMatch(0);
                if (expire == null) {
                    expire = br.getRegex("Gold(-| )(Zugriff|Zugang)( bis)?: <b>(.*?)</b></div>").getMatch(3);
                    // russian ip subnets or accounts, seem to not respect url language setting
                    if (expire == null) {
                        expire = br.getRegex("<div class=\"access\">Ваш Gold доступ активен до: <b>(.*?)</b></div>").getMatch(0);
                    }
                }
            }
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
            if (expire == null) {
                ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountbad", "Account expired or not valid."));
                account.setType(AccountType.UNKNOWN);
                account.clearCookies("");
                account.setValid(false);
                return ai;
            }
            account.setValid(true);
            account.setType(AccountType.PREMIUM);
            final Date date;
            try {
                date = dateFormat.parse(expire);
                ai.setValidUntil(date.getTime());
            } catch (final ParseException e) {
                logger.log(e);
            }
        }
        return ai;
    }

    private void webLogin(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                setLangtoGer();
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                // web fail over method
                logger.info("Depositfiles website login method!");
                String uprand = account.getStringProperty("uprand", null);
                if (uprand != null) {
                    br.setCookie(MAINPAGE.get(), "uprand", uprand);
                }
                br.setReadTimeout(3 * 60 * 1000);
                br.getHeaders().put("User-Agent", UA);
                br.setFollowRedirects(true);
                br.getPage(MAINPAGE.get() + "/login.php?return=%2Fde%2F");
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
                login.setAction(MAINPAGE.get() + "/api/user/login");
                login.put("login", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br2 = br.cloneBrowser();
                br2.submitForm(login);
                if (br2.containsHTML("\"error\":\"CaptchaRequired\"") && cid == null) {
                    logger.warning("cid = null, captcha is required to login");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (br2.containsHTML("\"error\":\"CaptchaRequired\"") && cid != null) {
                    DownloadLink dummy = new DownloadLink(null, null, null, null, true);
                    final Recaptcha rc = new Recaptcha(br2, this);
                    rc.setId(cid);
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode("recaptcha", cf, dummy);
                    login.put("recaptcha_challenge_field", rc.getChallenge());
                    login.put("recaptcha_response_field", Encoding.urlEncode(c));
                    br2.submitForm(login);
                    if (br2.containsHTML("\"error\":\"CaptchaInvalid\"")) {
                        logger.info("Invalid Captcha response!");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br = br2.cloneBrowser();
                if (br.getCookie(MAINPAGE.get(), "autologin") == null && br.containsHTML("\"error\":\"InvalidLogIn\"")) {
                    logger.info("Invalid login criteria");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(MAINPAGE.get(), "autologin") == null && !br.containsHTML("\"error\":\"InvalidLogIn\"")) {
                    logger.info("Depositfiles website login method  == failed! Possible plugin error, please report this to JDownloader Development Team");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    logger.info("Depositfiles website login method == success!");
                }
                br.setFollowRedirects(false);
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                account.setProperty("uprand", br.getCookie(MAINPAGE.get(), "uprand"));
            } catch (final PluginException e) {
                account.clearCookies("");
                account.setProperty("uprand", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setConstants();
        requestFileInformation(downloadLink);
        synchronized (PREMLOCK) {
            if (isFreeAccountPre(account)) {
                simultanpremium.set(1);
            } else {
                if (simultanpremium.get() + 1 > 20) {
                    simultanpremium.set(20);
                } else {
                    simultanpremium.incrementAndGet();
                }
            }
        }
        /* 2016-10-08: Free account download does not work via API anymore --> Use web download */
        if (useAPI.get() && account.getType() == AccountType.PREMIUM) {
            apiHandlePremium(downloadLink, account);
        } else {
            webHandlePremium(downloadLink, account);
        }
    }

    private void webHandlePremium(final DownloadLink downloadLink, Account account) throws Exception {
        webLogin(account, false);
        if (account.getType() == AccountType.FREE) {
            logger.info(account.getUser() + " @ Free Account :: Web download method in use");
            br.getPage(downloadLink.getDownloadURL());
            doFree(downloadLink);
        } else {
            logger.info(account.getUser() + " @ Gold Account :: Web download method in use");
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
                if (passCode == null) {
                    passCode = getUserInput(null, downloadLink);
                }
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
                    account.clearCookies("");
                    account.setType(AccountType.UNKNOWN);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            link = fixLinkSSL(link);
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, link, true, 0);
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
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
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
            if (br.containsHTML("\"data\":\\{\"status\":\"UpToDate\"\\}")) {
                return true;
            } else {
                // not sure if we need to bother?? set to true for now.
                return true;
            }
        }
    }

    private HashMap<String, Object> accountData = new HashMap<String, Object>();

    private void saveAccountData(Account account) {
        if (!accountData.isEmpty()) {
            account.setProperty("accountData", accountData);
        } else {
            account.setProperty("accountData", Property.NULL);
        }
    }

    private void setConstants(Account account) {
        @SuppressWarnings("unchecked")
        HashMap<String, Object> tempHM = (HashMap<String, Object>) account.getProperty("accountData", null);
        if (tempHM != null) {
            accountData = tempHM;
        }
    }

    private void apiGetPage(String url) throws Exception {
        br = new Browser();
        apiPrepBr(br);
        br.getPage(fixLinkSSL(url));
    }

    @SuppressWarnings("unchecked")
    private String getToken(Account account) throws Exception {
        String result = null;
        HashMap<String, Object> accountData = (HashMap<String, Object>) account.getProperty("accountData", null);
        if (accountData == null) {
            try {
                apiFetchAccountInfo(account);
            } catch (Exception e) {
            }
            accountData = (HashMap<String, Object>) account.getProperty("accountData", null);
        }
        if (accountData != null && accountData.containsKey("token")) {
            result = accountData.get("token").toString();
        } else {
            // try without api...
            // useAPI.set(false);
            logger.warning("Possible issue with getToken! Please report to JDownloader Development Team.");
            throw new PluginException(LinkStatus.ERROR_RETRY);
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
            apiGetPage("http://depositfiles.com/api/user/login?" + "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&" + apiKeyVal());
            if (br.containsHTML("\"error\":\"CaptchaRequired\"")) {
                for (int i = 0; i <= 2; i++) {
                    DownloadLink dummyLink = new DownloadLink(null, "Account", this.getHost(), MAINPAGE.get(), true);
                    final Recaptcha rc = new Recaptcha(br, this);
                    rc.setId("6LdRTL8SAAAAAE9UOdWZ4d0Ky-aeA7XfSqyWDM2m");
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode("recaptcha", cf, dummyLink);
                    if (c == null || c.equals("")) {
                        logger.warning(this.getHost() + "requires captcha query in order to login, you've entered nothing or blank captcha respsonse. Aborting login sequence");
                        account.setValid(false);
                        return ai;
                    }
                    br.getPage("http://depositfiles.com/api/user/login?recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&" + apiKeyVal() + "&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    if (br.containsHTML("\"error\":\"CaptchaInvalid\"")) {
                        logger.info("Invalid Captcha response!");
                    } else {
                        break;
                    }
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
            String token = PluginJSonUtils.getJsonValue(br, "token");
            if (token != null) {
                token = token.replaceAll("\\\\/", "/");
                token = Encoding.urlEncode(token);
            } else {
                token = br.getCookie(br.getHost(), "autologin");
            }
            if (token == null) {
                logger.warning("Could not find 'token'");
                // useAPI.set(false);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            accountData.put("token", token);
            String passKey = PluginJSonUtils.getJsonValue(br, "member_passkey");
            if (passKey == null) {
                logger.warning("Could not find 'passKey'");
                // useAPI.set(false);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            accountData.put("passKey", passKey);
            String mode = PluginJSonUtils.getJsonValue(br, "mode");
            if (mode == null) {
                logger.warning("Could not find 'mode'");
                // useAPI.set(false);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            accountData.put("mode", mode);
            // "gold_expired":"2015-01-18 14:37:08"
            final String expire = PluginJSonUtils.getJsonValue(br, "gold_expired");
            if ("gold".equalsIgnoreCase(mode)) {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
                if (expire == null) {
                    ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountbad", "Account expired or not valid."));
                    account.setProperty("accountData", Property.NULL);
                    account.setValid(false);
                    return ai;
                }
                try {
                    Date date;
                    date = dateFormat.parse(expire);
                    ai.setValidUntil(date.getTime());
                } catch (final ParseException e) {
                    logger.log(e);
                }
            } else {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
            }
            saveAccountData(account);
            account.setValid(true);
        } catch (PluginException e) {
            account.setProperty("accountData", Property.NULL);
            ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountbad", "Account expired or not valid."));
            account.setValid(false);
            return ai;
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
        if (account.getType() == AccountType.FREE) {
            logger.info(account.getUser() + " @ Free Account :: API download method in use");
            apiResumes = true;
            apiChunks = 1;
        } else {
            logger.info(account.getUser() + " @ Gold Account :: API download method in use");
            apiResumes = true;
            apiChunks = 0;
        }
        // atm they share the same dl routine that I can see. Download program indicates captcha!
        apiGetPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + fuid(downloadLink) + "&" + apiKeyVal() + (pass != null ? "&file_password=" + Encoding.urlEncode(pass) : ""));
        if ("FileIsPasswordProtected".equalsIgnoreCase(getError())) {
            logger.info("This file seems to be password protected.");
            for (int i = 0; i <= 2; i++) {
                if (pass == null) {
                    pass = getUserInput(null, downloadLink);
                }
                if (pass == null || pass.equals("")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download requires valid password");
                }
                apiGetPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + fuid(downloadLink) + "&" + apiKeyVal() + "&file_password=" + pass);
                if ("FilePasswordIsIncorrect".equalsIgnoreCase(getError())) {
                    pass = null;
                    continue;
                } else {
                    downloadLink.setProperty("pass", pass);
                    break;
                }
            }
        }
        handleErrorsApi();
        if (account.getType() == AccountType.FREE) {
            String mode = PluginJSonUtils.getJsonValue(br, "mode");
            String delay = PluginJSonUtils.getJsonValue(br, "delay");
            String dlToken = PluginJSonUtils.getJsonValue(br, "download_token");
            if (delay == null && mode == null && dlToken == null) {
                logger.warning("api epic fail");
                // if (useAPI.getAndSet(false) == true) {
                // return;
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                // }
            }
            // download modes seem to indicate if the user can download as 'gold' or 'free' connection ratios?. User can download there
            // own uploads under gold even though they don't have gold account status.
            if (mode != null && "gold".equalsIgnoreCase(mode) && account.getType() == AccountType.FREE) {
                apiResumes = true;
                apiChunks = 0;
            } else {
                int deley = Integer.parseInt(delay);
                sleep(deley * 1001, downloadLink);
                apiGetPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + fuid(downloadLink) + "&" + apiKeyVal() + (pass != null ? "&file_password=" + Encoding.urlEncode(pass) : "") + "&download_token=" + dlToken);
                if ("CaptchaRequired".equalsIgnoreCase(getError())) {
                    for (int i = 0; i <= 2; i++) {
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.setId("6LdRTL8SAAAAAE9UOdWZ4d0Ky-aeA7XfSqyWDM2m");
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode("recaptcha", cf, downloadLink);
                        if (c == null || c.equals("")) {
                            logger.warning("User aborted/cancelled captcha");
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        apiGetPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + fuid(downloadLink) + "&" + apiKeyVal() + (pass != null ? "&file_password=" + Encoding.urlEncode(pass) : "") + "&download_token=" + dlToken + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                        if ("CaptchaInvalid".equalsIgnoreCase(getError())) {
                            logger.info("Invalid Captcha response!");
                        } else {
                            break;
                        }
                    }
                }
                handleErrorsApi();
            }
        }
        String dllink = PluginJSonUtils.getJsonValue(br, "download_url");
        if (dllink == null) {
            logger.warning("Could not find 'dllink'");
            // if (useAPI.getAndSet(false) == true) {
            // return;
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
        }
        // for now limit to premium accounts
        // if (!account.getBooleanProperty("free", false)) {
        dllink = fixLinkSSL(dllink);
        // }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, apiResumes, apiChunks);
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

    private void handleErrorsApi() throws PluginException {
        final String status = PluginJSonUtils.getJsonValue(br, "status");
        if ("Error".equalsIgnoreCase(status)) {
            final String error = getError();
            if ("FileDoesNotExist".equalsIgnoreCase(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (StringUtils.equalsIgnoreCase(error, "ConnectionLimitHasBeenExhaustedForYourIP")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            } else if ("FilePasswordIsIncorrect".equalsIgnoreCase(error)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "File Password protected!");
            } else if ("CaptchaInvalid".equalsIgnoreCase(error)) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
    }

    private String getError() {
        return PluginJSonUtils.getJsonValue(br, "error");
    }

    private String fuid(DownloadLink downloadLink) {
        String result = new Regex(downloadLink.getDownloadURL(), "files/([^/]+)").getMatch(0);
        return result;
    }

    private void mainpageCookies(Browser ibr) {
        String current_host = new Regex(ibr.getURL(), "(https?://[^/]+)").getMatch(0);
        /** Save cookies */
        final Cookies add = ibr.getCookies(current_host);
        for (final Cookie c : add.getCookies()) {
            br.setCookie(MAINPAGE.get(), c.getKey(), c.getValue());
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
        if (MAINPAGE != null && MAINPAGE.get() != null && (checkSsl() && MAINPAGE.get().startsWith("http://")) || (!checkSsl() && MAINPAGE.get().startsWith("https://"))) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkSsl() {
        return true;// getPluginConfig().getBooleanProperty(SETTING_SSL_CONNECTION, false);
    }

    private String fixLinkSSL(String link) {
        if (checkSsl()) {
            link = link.replace("http://", "https://");
        } else {
            link = link.replace("https://", "http://");
        }
        return link;
    }

    private void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SETTING_SSL_CONNECTION, JDL.L("plugins.hoster.DepositFiles.com.preferSSL", "Use Secure Communication over SSL (HTTPS://)")).setDefaultValue(false));
        // this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SETTING_PREFER_SOLVEMEDIA,
        // JDL.L("plugins.hoster.DepositFiles.com.preferSolvemediaCaptcha",
        // "Prefer solvemedia captcha over reCaptcha V2?")).setDefaultValue(true));
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
        br.setCookie("depositfiles.com", "lang_current", "de");
        br.setCookie("depositfiles.org", "lang_current", "de");
        br.setCookie("dfiles.eu", "lang_current", "de");
        br.setCookie("dfiles.ru", "lang_current", "de");
    }

    @Override
    public String getAGBLink() {
        setMainpage();
        return MAINPAGE.get() + "/en/agreem.html";
    }
}