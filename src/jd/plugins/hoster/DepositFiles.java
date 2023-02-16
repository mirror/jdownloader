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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.proxy.AbstractProxySelectorImpl;
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
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DepositFiles extends antiDDoSForHost {
    private final String                           UA                       = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36";
    private final String                           downloadLimitReached     = "<strong>Achtung! Sie haben ein Limit|Sie haben Ihre Download Zeitfrist erreicht\\.<";
    private final String                           PATTERN_PREMIUM_FINALURL = "<div id=\"download_url\".*?<a href=\"(.*?)\"";
    public static AtomicReference<String>          MAINPAGE                 = new AtomicReference<String>();
    public static final String                     DOMAINS                  = "(depositfiles\\.(com|org)|dfiles\\.(eu|ru))";
    private String                                 protocol                 = null;
    public String                                  DLLINKREGEX2             = "<div id=\"download_url\" style=\"display:none;\">.*?<form action=\"(.*?)\" method=\"get";
    private final Pattern                          FILE_INFO_NAME           = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);
    private final Pattern                          FILE_INFO_SIZE           = Pattern.compile(">Datei Gr.*?sse: <b>([^<>\"]*?)</b>");
    /* don't touch the following! */
    private static Map<Account, Set<DownloadLink>> RUNNING                  = new WeakHashMap<Account, Set<DownloadLink>>();
    private static AtomicBoolean                   useAPI                   = new AtomicBoolean(true);
    private final String                           SETTING_SSL_CONNECTION   = "SSL_CONNECTION";

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        final String[] extraNames = { "dfiles", "depositfiles" };
        final String[] officiallySupportedNames = buildSupportedNames(getPluginDomains());
        String[] finalSupportedNames = new String[officiallySupportedNames.length + extraNames.length];
        System.arraycopy(officiallySupportedNames, 0, finalSupportedNames, 0, officiallySupportedNames.length);
        System.arraycopy(extraNames, 0, finalSupportedNames, officiallySupportedNames.length, extraNames.length);
        return finalSupportedNames;
    }

    public static String[] getAnnotationUrls() {
        return DepositFiles.buildAnnotationUrls(getPluginDomains());
    }

    public static final String getDefaultAnnotationPatternPart() {
        return "(/\\w{1,3})?/files/[\\w]+";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + DepositFiles.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "depositfiles.com", "depositfiles.org", "dfiles.eu", "dfiles.ru" });
        return ret;
    }

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

    @Override
    public String buildExternalDownloadURL(DownloadLink downloadLink, PluginForHost buildForThisPlugin) {
        if (StringUtils.equals(getHost(), buildForThisPlugin.getHost())) {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        } else {
            final String fileID = new Regex(downloadLink.getPluginPatternMatcher(), "/files/([\\w]+)").getMatch(0);
            return "https://dfiles.eu/files/" + fileID;
        }
    }

    public void setMainpage() {
        synchronized (MAINPAGE) {
            if (MAINPAGE.get() == null || userChangedSslSetting()) {
                try {
                    Browser testBr = new Browser();
                    testBr.setFollowRedirects(true);
                    // NOTE: https requests do not trigger redirects
                    // NOTE: http also get filtered by ISP/Government Policy to the incorrect domain! - raztoki20160114
                    testBr.getPage(fixLinkSSL("https://depositfiles.com"));
                    String baseURL = new Regex(testBr.getURL(), "(https?://[^/]+)").getMatch(0);
                    if (baseURL != null && Browser.getHost(baseURL).matches(DOMAINS)) {
                        MAINPAGE.set(baseURL);
                        System.out.println("depositfiles setter MAINPAGE = " + MAINPAGE.get());
                    } else {
                        // Houston we have a problem!
                        System.out.println("despostfiles checker failed, setting failover");
                        MAINPAGE.set(fixLinkSSL("https://depositfiles.com"));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    System.out.println("despostfiles setter failed, setting failover");
                    MAINPAGE.set(fixLinkSSL("https://depositfiles.com"));
                }
            }
            System.out.println("depositfiles setter MAINPAGE = " + MAINPAGE.get());
        }
    }

    @Override
    public boolean hasAutoCaptcha() {
        return true;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        } else if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        setMainpage();
        final String currentDomain = Browser.getHost(link.getPluginPatternMatcher(), false);
        if (!currentDomain.matches(DOMAINS)) {
            // this is needed to fix old users bad domain name corrections (say hotpspots/gateways)
            String mainPage = MAINPAGE.get();
            if (mainPage != null) {
                mainPage = mainPage.replaceFirst("https://", "").replace("http://", "");
                link.setUrlDownload(link.getPluginPatternMatcher().replace(currentDomain, mainPage));
            }
        }
        final String newLink = link.getPluginPatternMatcher().replaceAll(DOMAINS + "(/.*?)?/files", MAINPAGE.get().replaceAll("https?://(www\\.)?", "") + "/de/files");
        link.setUrlDownload(fixLinkSSL(newLink));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        // correctlink fixes https|not https, and sets mainpage! no need to duplicate in other download areas!.
        correctDownloadLink(link);
        setBrowserExclusive();
        if (account != null) {
            this.webLogin(account, false);
        } else {
            br.getHeaders().put("User-Agent", UA);
        }
        setLangtoGer(br);
        /* Needed so the download gets counted,any referer should work */
        // br.getHeaders().put("Referer", "http://www.google.de");
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("class=\"no_download_msg\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(downloadLimitReached)) {
            link.getLinkStatus().setStatusText("Download limit reached");
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
        if (fileName == null && fileSizeString != null) {
            fileName = new Regex(link, "files/([\\w]+)").getMatch(0);
        }
        String fixedName = new Regex(fileName, "(.+)\\?").getMatch(0);
        if (fixedName != null) {
            fileName = fixedName;
        }
        if (fileName != null) {
            link.setName(Encoding.htmlDecode(fileName).trim());
        }
        if (fileSizeString != null) {
            link.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(fileSizeString)));
        }
        return AvailableStatus.TRUE;
    }

    public void checkErrorsWebsite(final Browser br) throws NumberFormatException, PluginException {
        logger.info("Checking website errors...");
        /* Check for offline */
        if (br.containsHTML("Zugang zur folgenden Datei ist begrenzt oder Datei wurde entfernt|Diese Datei besteht nicht, der Zugang zur folgenden Datei ist begrenzt oder Datei wurde entfernt, wegen der Urheberrechtsverletzung\\.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("This file does not exist, the access to the following file is limited or it has been removed due to infringement of copyright")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class=\"no_download_msg\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Check for "no free slots" */
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

    private String getDllink(final Browser br) throws Exception {
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
    public int getTimegapBetweenConnections() {
        return 800;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        logger.info("Free @ Guest :: Web download method in use");
        setConstants();
        webHandleDownload(link, null);
    }

    private void webHandleDownload(final DownloadLink link, final Account account) throws Exception {
        String finallink = checkDirectLink(link);
        if (finallink == null) {
            requestFileInformation(link, account);
            checkErrorsWebsite(br);
            String downloadURL = getDllink(br);
            if (!StringUtils.isEmpty(downloadURL)) {
                /* Direct download without pre-download waittime and/or captcha */
                downloadURL = fixLinkSSL(downloadURL);
                // handling for txt file downloadlinks, dunno why they made a completely different page for txt files
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, downloadURL, true, 1);
                if (!looksLikeDownloadableContent(dl.getConnection())) {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    handleDownloadError(br, dl.getConnection(), link);
                }
                handleDownload(dl.getConnection(), link, "finallink", downloadURL);
                try {
                    /* add a download slot */
                    controlRunningDownloads(account, link, true);
                    /* start the dl */
                    dl.startDownload();
                } finally {
                    /* remove download slot */
                    controlRunningDownloads(account, link, false);
                }
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
                checkErrorsWebsite(br);
                if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) {
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                if (br.containsHTML("\"file_password\"")) {
                    logger.info("This file seems to be password protected.");
                    link.setPasswordProtected(true);
                    String passCode = link.getDownloadPassword();
                    if (passCode == null) {
                        passCode = getUserInput(null, link);
                    }
                    br.postPage(br.getURL(), "file_password=" + passCode);
                    logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
                    if (br.containsHTML("(>The file's password is incorrect. Please check your password and try to enter it again\\.<|\"file_password\")")) {
                        logger.info("The entered password (" + passCode + ") was wrong, retrying...");
                        link.setDownloadPassword(null);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                    }
                    link.setDownloadPassword(passCode);
                }
                String fid = br.getRegex("var\\s*fid\\s*=\\s*\\'(.*?)\\'").getMatch(0);
                if (fid == null) {
                    fid = br.getRegex("php\\?fid=([a-z0-9A-Z]+)").getMatch(0);
                    if (fid == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                final String waitStr = br.getRegex("(?i)Please wait (\\d+) sec").getMatch(0);
                /* Wait time minus time user needed to enter download-password */
                sleep(Long.parseLong(waitStr) * 1001, link);
                // // ajax request, here they give you more html && js. Think this is where the captcha type is determined.
                ajaxGetPage("/get_file.php?abspeed=0&fid=" + fid);
                if (ajax.containsHTML("But currently no free download slots for")) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 5 * 60 * 1000l);
                }
                // this changes each time you load page...
                final String ck = ajax.getRegex("ACPuzzleKey\\s*=\\s*('|\")(.*?)\\1").getMatch(1);
                if (ck != null) {
                    // lets prefer solvemedia as it can be passed into CES/headless as browser not required
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
                        final String code = getCaptchaCode("solvemedia", cf, link);
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
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, finallink, true, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            handleDownloadError(br, dl.getConnection(), link);
        }
        handleDownload(dl.getConnection(), link, "finallink", finallink);
        try {
            /* add a download slot */
            controlRunningDownloads(account, link, true);
            /* start the dl */
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlRunningDownloads(account, link, false);
        }
    }

    private void handleDownload(final URLConnectionAdapter connection, final DownloadLink link, String finalLinkProperty, String finalLink) {
        final String name = Plugin.getFileNameFromHeader(connection);
        if (name != null && name.contains("?") && link.getFinalFileName() == null) {
            /* fix invalid filenames */
            final String fixedName = new Regex(name, "(.+)\\?").getMatch(0);
            link.setFinalFileName(fixedName);
        }
        if (finalLinkProperty != null) {
            link.setProperty(finalLinkProperty, finalLink);
        }
    }

    protected void handleDownloadError(final Browser br, final URLConnectionAdapter con, final DownloadLink link) throws PluginException {
        final String downloadErrorHeader = dl.getConnection().getHeaderField("Download-Error");
        if (StringUtils.isNotEmpty(downloadErrorHeader)) {
            if (StringUtils.equalsIgnoreCase("No such file", downloadErrorHeader)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, downloadErrorHeader);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error:" + downloadErrorHeader);
            }
        }
        if (br.containsHTML("(?i)File does't exist")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (dl.getConnection().getHeaderField("Guest-Limit") != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        } else if (br.containsHTML("(<title>\\s*404 Not Found\\s*</title>|<h1>\\s*404 Not Found\\s*</h1>)")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, final Account account, AbstractProxySelectorImpl proxy) {
        final int max = super.getMaxSimultanDownload(link, account, proxy);
        if (account == null) {
            final int running;
            synchronized (RUNNING) {
                final Set<DownloadLink> set = RUNNING.get(account);
                running = set != null ? set.size() : 0;
            }
            final int ret = Math.min(running + 1, max);
            return ret;
        } else {
            return max;
        }
    }

    protected void controlRunningDownloads(final Account account, final DownloadLink link, boolean startFlag) {
        synchronized (RUNNING) {
            Set<DownloadLink> set = RUNNING.get(account);
            if (set == null) {
                set = new HashSet<DownloadLink>();
                RUNNING.put(account, set);
            }
            final int before = set.size();
            if (startFlag) {
                set.add(link);
            } else {
                set.remove(link);
            }
            logger.info("Running(" + link.getName() + ")|max:" + getMaxSimultanDownload(link, account, null) + "|before:" + before + "|after:" + set.size());
        }
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
    public boolean isFreeAccount(final Account acc) {
        if (acc.getType() == AccountType.PREMIUM) {
            return false;
        } else {
            /* Rely on cached data from API. */
            final Map<String, Object> accountData = getAccountData(acc);
            if (accountData != null && accountData.containsKey("mode")) {
                if ("gold".equalsIgnoreCase(accountData.get("mode").toString())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai;
        synchronized (account) {
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

    private AccountInfo webFetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        webLogin(account, true);
        setLangtoGer(br);
        if (!br.getURL().contains("/gold/")) {
            final boolean followRedirectsBefore = br.isFollowingRedirects();
            try {
                br.setFollowRedirects(true);
                br.getPage(MAINPAGE.get() + "/de/gold/");
            } finally {
                br.setFollowRedirects(followRedirectsBefore);
            }
        }
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
        if (expire == null) {
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            account.setType(AccountType.FREE);
            return ai;
        } else {
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
            account.setType(AccountType.PREMIUM);
            final Date date = dateFormat.parse(expire);
            ai.setValidUntil(date.getTime());
        }
        return ai;
    }

    private void webLogin(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                setLangtoGer(br);
                final Cookies cookies = account.loadCookies("");
                String uprand = account.getStringProperty("uprand", null);
                if (uprand != null) {
                    br.setCookie(MAINPAGE.get(), "uprand", uprand);
                }
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        return;
                    }
                    logger.info("Validating login cookies");
                    if (this.isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(this.br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(null);
                    }
                }
                // web fail over method
                logger.info("Performing full login");
                br.setReadTimeout(3 * 60 * 1000);
                br.getHeaders().put("User-Agent", UA);
                br.setFollowRedirects(true);
                br.getPage(MAINPAGE.get() + "/login.php?return=%2Fde%2F");
                Thread.sleep(1000);
                final Form login = br.getFormBySubmitvalue("Eingeben");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.setAction(MAINPAGE.get() + "/api/user/login");
                login.put("login", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                if (br.containsHTML("\"error\":\"CaptchaRequired\"")) {
                    if (getDownloadLink() == null) {
                        final DownloadLink dummyLink = new DownloadLink(null, "Account", this.getHost(), MAINPAGE.get(), true);
                        setDownloadLink(dummyLink);
                    }
                    final String c = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LdyfgcTAAAAAArE1fk9cGyExtKfT4a12dWcViye").getToken();
                    if (c == null || c.equals("")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    login.put("g-recaptcha-response", Encoding.urlEncode(c));
                    br.submitForm(login);
                    if (br.containsHTML("\"error\":\"CaptchaInvalid\"")) {
                        /* This should never happen */
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
                br.getPage("/");
                if (!isLoggedIN(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(this.br.getCookies(br.getHost()), "");
                account.setProperty("uprand", br.getCookie(br.getHost(), "uprand"));
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                    account.setProperty("uprand", Property.NULL);
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("logout\\.php") && br.getCookie(br.getHost(), "autologin", Cookies.NOTDELETEDPATTERN) != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        setConstants();
        /* 2016-10-08: Free account download does not work via API anymore --> Use web download */
        if (useAPI.get() && account.getType() == AccountType.PREMIUM) {
            apiHandlePremium(link, account);
        } else {
            webHandlePremium(link, account);
        }
    }

    private void webHandlePremium(final DownloadLink link, Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            logger.info(account.getUser() + " @ Free Account :: Web download method in use");
            webHandleDownload(link, account);
        } else {
            logger.info(account.getUser() + " @ Gold Account :: Web download method in use");
            requestFileInformation(link);
            webLogin(account, false);
            String url = link.getPluginPatternMatcher();
            br.setFollowRedirects(false);
            br.getPage(url);
            if (br.getRedirectLocation() != null) {
                url = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
                br.getPage(url);
            }
            checkErrorsWebsite(br);
            if (br.containsHTML("\"file_password\"")) {
                logger.info("This file seems to be password protected.");
                link.setPasswordProtected(true);
                String passCode = link.getDownloadPassword();
                if (passCode == null) {
                    passCode = getUserInput(null, link);
                }
                br.postPage(br.getURL().replaceFirst("https?://", protocol), "file_password=" + passCode);
                logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
                if (br.containsHTML("(>The file's password is incorrect. Please check your password and try to enter it again\\.<|\"file_password\")")) {
                    logger.info("The entered password (" + passCode + ") was wrong, retrying...");
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else {
                    link.setDownloadPassword(passCode);
                }
            }
            checkErrorsWebsite(br);
            url = br.getRegex(PATTERN_PREMIUM_FINALURL).getMatch(0);
            if (url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            url = fixLinkSSL(url);
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, url, true, 0);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                handleDownloadError(br, dl.getConnection(), link);
            }
            handleDownload(dl.getConnection(), link, null, url);
            try {
                /* add a download slot */
                controlRunningDownloads(account, link, true);
                /* start the dl */
                dl.startDownload();
            } finally {
                /* remove download slot */
                controlRunningDownloads(account, link, false);
            }
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

    private void saveAccountData(Map<String, Object> accountData, Account account) {
        synchronized (account) {
            if (accountData != null && !accountData.isEmpty()) {
                account.setProperty("accountData", accountData);
            } else {
                account.setProperty("accountData", Property.NULL);
            }
        }
    }

    private Map<String, Object> getAccountData(Account account) {
        synchronized (account) {
            final Object ret = account.getProperty("accountData", null);
            if (ret instanceof Map) {
                return (Map<String, Object>) ret;
            } else {
                return null;
            }
        }
    }

    private void apiGetPage(String url) throws Exception {
        br = new Browser();
        apiPrepBr(br);
        br.getPage(fixLinkSSL(url));
    }

    private String getToken(Account account) throws Exception {
        String result = null;
        Map<String, Object> accountData = getAccountData(account);
        if (accountData == null) {
            try {
                apiFetchAccountInfo(account);
            } catch (Exception e) {
            }
            accountData = getAccountData(account);
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
        // this shouldn't be needed!
        // if (versionCheck()) {
        try {
            apiGetPage("http://depositfiles.com/api/user/login?" + "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&" + apiKeyVal());
            if (br.containsHTML("\"error\":\"CaptchaRequired\"")) {
                for (int i = 0; i <= 2; i++) {
                    if (getDownloadLink() == null) {
                        final DownloadLink dummyLink = new DownloadLink(null, "Account", this.getHost(), MAINPAGE.get(), true);
                        setDownloadLink(dummyLink);
                    }
                    final String c = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LdyfgcTAAAAAArE1fk9cGyExtKfT4a12dWcViye").getToken();
                    if (c == null || c.equals("")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    br.getPage("http://depositfiles.com/api/user/login?recaptcha_challenge_field=null&g-recaptcha-response=" + Encoding.urlEncode(c) + "&" + apiKeyVal() + "&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
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
                logger.info("Invalid Captcha response! Exhausted retry count");
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
            final Map<String, Object> accountData = new HashMap<String, Object>();
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
                if (expire == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                try {
                    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
                    final Date date = dateFormat.parse(expire);
                    ai.setValidUntil(date.getTime());
                } catch (final ParseException e) {
                    logger.log(e);
                }
            } else {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
            }
            saveAccountData(accountData, account);
            account.setRefreshTimeout(5 * 60 * 60 * 1000l);
        } catch (PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.setProperty("accountData", Property.NULL);
            }
            throw e;
        }
        // }
        return ai;
    }

    // defaults do not change
    private boolean apiResumes = true;
    private int     apiChunks  = 0;

    private void apiHandlePremium(final DownloadLink link, final Account account) throws Exception {
        String passCode = link.getDownloadPassword();
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
        apiGetPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + getFID(link) + "&" + apiKeyVal() + (passCode != null ? "&file_password=" + Encoding.urlEncode(passCode) : ""));
        if ("FileIsPasswordProtected".equalsIgnoreCase(getError())) {
            link.setPasswordProtected(true);
            logger.info("This file seems to be password protected.");
            for (int i = 0; i <= 2; i++) {
                if (passCode == null) {
                    passCode = getUserInput(null, link);
                }
                apiGetPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + getFID(link) + "&" + apiKeyVal() + "&file_password=" + passCode);
                if ("FilePasswordIsIncorrect".equalsIgnoreCase(getError())) {
                    passCode = null;
                    continue;
                } else {
                    link.setDownloadPassword(passCode);
                    break;
                }
            }
        }
        handleErrorsApi(link, account);
        if (account.getType() == AccountType.FREE) {
            String mode = PluginJSonUtils.getJsonValue(br, "mode");
            String delayStr = PluginJSonUtils.getJsonValue(br, "delay");
            String dlToken = PluginJSonUtils.getJsonValue(br, "download_token");
            if (delayStr == null && mode == null && dlToken == null) {
                logger.warning("api epic fail");
                // if (useAPI.getAndSet(false) == true) {
                // return;
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Bad API response");
                // }
            }
            // download modes seem to indicate if the user can download as 'gold' or 'free' connection ratios?. User can download there
            // own uploads under gold even though they don't have gold account status.
            if (mode != null && "gold".equalsIgnoreCase(mode) && account.getType() == AccountType.FREE) {
                apiResumes = true;
                apiChunks = 0;
            } else {
                int delay = Integer.parseInt(delayStr);
                sleep(delay * 1001, link);
                apiGetPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + getFID(link) + "&" + apiKeyVal() + (passCode != null ? "&file_password=" + Encoding.urlEncode(passCode) : "") + "&download_token=" + dlToken);
                if ("CaptchaRequired".equalsIgnoreCase(getError())) {
                    for (int i = 0; i <= 2; i++) {
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.setId("6LdRTL8SAAAAAE9UOdWZ4d0Ky-aeA7XfSqyWDM2m");
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode("recaptcha", cf, link);
                        if (c == null || c.equals("")) {
                            logger.warning("User aborted/cancelled captcha");
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        apiGetPage("http://depositfiles.com/api/download/file?token=" + getToken(account) + "&file_id=" + getFID(link) + "&" + apiKeyVal() + (passCode != null ? "&file_password=" + Encoding.urlEncode(passCode) : "") + "&download_token=" + dlToken + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                        if ("CaptchaInvalid".equalsIgnoreCase(getError())) {
                            logger.info("Invalid Captcha response!");
                        } else {
                            break;
                        }
                    }
                }
                handleErrorsApi(link, account);
            }
        }
        String downloadURL = PluginJSonUtils.getJsonValue(br, "download_url");
        if (downloadURL == null) {
            logger.warning("Could not find 'dllink'");
            // if (useAPI.getAndSet(false) == true) {
            // return;
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Bad API response: Failed to find final downloadurl");
            // }
        }
        downloadURL = fixLinkSSL(downloadURL);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, downloadURL, apiResumes, apiChunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            handleDownloadError(br, dl.getConnection(), link);
        }
        handleDownload(dl.getConnection(), link, "finallink", downloadURL);
        try {
            /* add a download slot */
            controlRunningDownloads(account, link, true);
            /* start the dl */
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlRunningDownloads(account, link, false);
        }
    }

    private void handleErrorsApi(final DownloadLink link, final Account account) throws PluginException, IOException {
        final String status = PluginJSonUtils.getJsonValue(br, "status");
        if ("Error".equalsIgnoreCase(status)) {
            final String error = getError();
            if ("FileDoesNotExist".equalsIgnoreCase(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (StringUtils.equalsIgnoreCase(error, "ConnectionLimitHasBeenExhaustedForYourIP")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            } else if ("FilePasswordIsIncorrect".equalsIgnoreCase(error)) {
                link.setPasswordProtected(true);
                throw new PluginException(LinkStatus.ERROR_FATAL, "File Password protected!");
            } else if ("CaptchaInvalid".equalsIgnoreCase(error)) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                /* Unknown error -> Should never happen */
                /* E.g. 2020-12-22: {"status":"Error","status_code":0,"error":"","error_code":0} */
                if (link == null) {
                    throw new AccountUnavailableException("Unknown API error: " + error, 5 * 60 * 1000l);
                } else {
                    /*
                     * 2021-01-04: It won't work this way as we'd have to login via website first in order to see that offline status
                     * immediately.
                     */
                    // logger.info("Special offline check");
                    // br.getPage(link.getPluginPatternMatcher());
                    // this.checkErrorsWebsite();
                    // /* No idea what happened? Hmm throw Exception anyways */
                    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API error: " + error);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        }
    }

    private String getError() {
        return PluginJSonUtils.getJsonValue(br, "error");
    }

    private String getFID(final DownloadLink link) {
        String result = new Regex(link.getPluginPatternMatcher(), "files/([^/]+)").getMatch(0);
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
        final String finallink = downloadLink.getStringProperty("finallink", null);
        if (finallink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                final URLConnectionAdapter con = br2.openGetConnection(finallink);
                try {
                    if (looksLikeDownloadableContent(con)) {
                        if (con.getCompleteContentLength() > 0) {
                            downloadLink.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                        return finallink;
                    } else {
                        throw new IOException();
                    }
                } finally {
                    con.disconnect();
                }
            } catch (Exception e) {
                logger.log(e);
                downloadLink.setProperty("finallink", Property.NULL);
                return null;
            }
        }
        return null;
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

    public void setLangtoGer(final Browser br) throws IOException {
        for (final String[] domainlist : getPluginDomains()) {
            for (final String domain : domainlist) {
                br.setCookie(domain, "lang_current", "de");
            }
        }
    }

    @Override
    public String getAGBLink() {
        setMainpage();
        return MAINPAGE.get() + "/en/agreem.html";
    }
}