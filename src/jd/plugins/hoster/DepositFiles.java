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

import java.io.IOException;
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

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.http.Browser;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DepositFiles extends antiDDoSForHost {
    public static AtomicReference<String>          MAINPAGE = new AtomicReference<String>();
    /* don't touch the following! */
    private static Map<Account, Set<DownloadLink>> RUNNING  = new WeakHashMap<Account, Set<DownloadLink>>();
    private static AtomicBoolean                   useAPI   = new AtomicBoolean(true);
    private final String                           API_BASE = "https://depositfiles.com/api";

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

    /* Keep this updated and make use of it [in the future]. */
    protected List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("dfiles.ru");
        return deadDomains;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 800;
    }

    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public DepositFiles(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://dfiles.eu/signup.php?ref=down1");
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink link, PluginForHost buildForThisPlugin) {
        if (StringUtils.equals(getHost(), buildForThisPlugin.getHost())) {
            return super.buildExternalDownloadURL(link, buildForThisPlugin);
        } else {
            return "https://dfiles.eu/files/" + this.getFID(link);
        }
    }

    public void setMainpage() {
        synchronized (MAINPAGE) {
            if (MAINPAGE.get() == null) {
                try {
                    final String hostsPattern = buildHostsPatternPart(getPluginDomains().get(0));
                    Browser testBr = this.createNewBrowserInstance();
                    // NOTE: https requests do not trigger redirects
                    // NOTE: http also get filtered by ISP/Government Policy to the incorrect domain! - raztoki20160114
                    testBr.getPage(adjustProtocol("https://depositfiles.com"));
                    String baseURL = new Regex(testBr.getURL(), "(https?://[^/]+)").getMatch(0);
                    if (baseURL != null && Browser.getHost(baseURL).matches(hostsPattern)) {
                        MAINPAGE.set(baseURL);
                        System.out.println("depositfiles setter MAINPAGE = " + MAINPAGE.get());
                    } else {
                        // Houston we have a problem!
                        System.out.println("despostfiles checker failed, setting failover");
                        MAINPAGE.set(adjustProtocol("https://depositfiles.com"));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    System.out.println("despostfiles setter failed, setting failover");
                    MAINPAGE.set(adjustProtocol("https://depositfiles.com"));
                }
            }
            System.out.println("depositfiles setter MAINPAGE = " + MAINPAGE.get());
        }
    }

    @Override
    public boolean hasAutoCaptcha() {
        return false;
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

    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    private String getContentURL(final DownloadLink link) {
        setMainpage();
        String url = link.getPluginPatternMatcher();
        final List<String> deadDomains = this.getDeadDomains();
        final String domainFromURL = Browser.getHost(url, false);
        final String hostsPattern = buildHostsPatternPart(getPluginDomains().get(0));
        if (!domainFromURL.matches(hostsPattern) || deadDomains.contains(domainFromURL)) {
            // this is needed to fix old users bad domain name corrections (say hotpspots/gateways)
            String mainPage = MAINPAGE.get();
            if (mainPage != null) {
                mainPage = mainPage.replaceFirst("(?i)https://", "").replace("http://", "");
                url = url.replace(domainFromURL, mainPage);
            }
        }
        url = url.replaceAll(hostsPattern + "(/.*?)?/files", MAINPAGE.get().replaceAll("(?i)https?://(www\\.)?", "") + "/de/files");
        return url;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        setBrowserExclusive();
        if (account != null) {
            this.webLogin(account, false);
        }
        setLangtoGer(br);
        br.getPage(this.getContentURL(link));
        if (br.containsHTML("class=\"no_download_msg\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)<strong>\\s*Achtung! Sie haben ein Limit|Sie haben Ihre Download Zeitfrist erreicht\\.\\s*<")) {
            /* Filename- and filesize are not visible in this state. */
            link.getLinkStatus().setStatusText("Download limit reached");
            return AvailableStatus.TRUE;
        }
        final String filenameregex = "(?s)Dateiname: <b title=\"(.*?)\">.*?</b>";
        String fileName = br.getRegex(filenameregex).getMatch(0);
        if (fileName == null) {
            String eval = br.getRegex("class=\"info\".*?unescape\\('(.*?)'").getMatch(0);
            if (eval != null) {
                eval = Encoding.unicodeDecode(eval);
                fileName = new Regex(eval, filenameregex).getMatch(0);
            }
        }
        final String fileSizeString = br.getRegex("(?i)>\\s*Datei Gr.*?sse: <b>([^<]+)</b>").getMatch(0);
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
        if (br.containsHTML("(?i)but all downloading slots for your country")) {
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
        final String wait = br.getRegex("(?i)Bitte versuchen Sie noch mal nach(.*?)<\\/strong>").getMatch(0);
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

    private String getDllinkWebsite(final Browser br) throws Exception {
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

    public String getDirectlinkproperty(final Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return "free_directurl";
        } else {
            return "account_ " + acc.getType() + "_directurl";
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        logger.info("Free @ Guest :: Web download method in use");
        webHandleDownload(link, null);
    }

    private void webHandleDownload(final DownloadLink link, final Account account) throws Exception {
        final String directurlproperty = this.getDirectlinkproperty(account);
        final String storedDirecturl = link.getStringProperty(directurlproperty);
        tryStoredDirecturl: if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl");
            try {
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, adjustProtocol(storedDirecturl), true, getMaxChunks(account));
                handleErrorsAfterDownloadstart(br, link);
            } catch (final Throwable e) {
                logger.log(e);
                logger.info("Failed to re-use stored directurl");
                break tryStoredDirecturl;
            }
            handlePreDownloadStuff(dl.getConnection(), link, account, storedDirecturl);
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
        }
        requestFileInformation(link, account);
        checkErrorsWebsite(br);
        String finallink = getDllinkWebsite(br);
        if (StringUtils.isEmpty(finallink)) {
            logger.info("Entering form-handling");
            final Form form = new Form();
            form.setMethod(MethodType.POST);
            form.setAction(this.adjustProtocol(br.getURL()));
            form.put("gateway_result", "1");
            form.put("asm", "0");
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
            handleDownloadPasswordWebsite(br, link);
            checkErrorsWebsite(br);
            String fid = br.getRegex("var\\s*fid\\s*=\\s*\\'(.*?)\\'").getMatch(0);
            if (fid == null) {
                fid = br.getRegex("php\\?fid=([a-z0-9A-Z]+)").getMatch(0);
                if (fid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            final String waitStr = br.getRegex("setTimeout\\('show_url\\((\\d+)").getMatch(0);
            long preDownloadWaittimeMillis = waitStr != null ? Long.parseLong(waitStr) * 1000 : 0;
            if (preDownloadWaittimeMillis > 120000) {
                /* Long wait -> IP limit reached */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, preDownloadWaittimeMillis);
            }
            final CaptchaHelperHostPluginRecaptchaV2 rc = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LdyfgcTAAAAAArE1fk9cGyExtKfT4a12dWcViye");
            if (preDownloadWaittimeMillis > rc.getSolutionTimeout()) {
                final long prePrePreDownloadWait = preDownloadWaittimeMillis - rc.getSolutionTimeout();
                logger.info("Waittime is higher than interactive captcha timeout --> Waiting a part of it before solving captcha to avoid timeouts");
                logger.info("Pre-pre download waittime seconds: " + (prePrePreDownloadWait / 1000));
                this.sleep(prePrePreDownloadWait, link);
                preDownloadWaittimeMillis = preDownloadWaittimeMillis - rc.getSolutionTimeout();
            }
            String recaptchaV2Response = null;
            final boolean needsCaptcha = account == null || account.getType() != AccountType.PREMIUM;
            final boolean allowCaptchaSolvingBeforeFinalWaitStep = true;
            if (allowCaptchaSolvingBeforeFinalWaitStep && needsCaptcha) {
                final long timeBefore = Time.systemIndependentCurrentJVMTimeMillis();
                recaptchaV2Response = rc.getToken();
                final long timePassed = Time.systemIndependentCurrentJVMTimeMillis() - timeBefore;
                preDownloadWaittimeMillis = preDownloadWaittimeMillis - timePassed;
            }
            if (preDownloadWaittimeMillis > 0) {
                sleep(Long.parseLong(waitStr) * 1001, link);
            }
            final Browser ajax = br.cloneBrowser();
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            ajax.getHeaders().put("Accept", "*/*");
            // // ajax request, here they give you more html && js. Think this is where the captcha type is determined.
            ajax.getPage("/get_file.php?abspeed=0&fid=" + fid);
            if (ajax.containsHTML("(?i)But currently no free download slots for")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 5 * 60 * 1000l);
            }
            if (recaptchaV2Response == null && needsCaptcha) {
                /* Captcha hasn't been requested before -> Do it now. */
                recaptchaV2Response = rc.getToken();
            }
            ajax.getPage("/get_file.php?fid=" + fid + "&challenge=null&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response) + "&response=null");
            if (ajax.containsHTML("(onclick=\"check_recaptcha|load_recaptcha)") || CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(ajax)) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            finallink = ajax.getRegex("\"((?:https?:)?//fileshare\\d+\\.[^/]+/auth.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = ajax.getRegex("<form [^>]*action=\"((?:https?:)?//.*?)\"").getMatch(0);
            }
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        logger.info("finallink = " + finallink);
        finallink = adjustProtocol(finallink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, finallink, true, getMaxChunks(account));
        handleErrorsAfterDownloadstart(br, link);
        handlePreDownloadStuff(dl.getConnection(), link, account, finallink);
        /* add a download slot */
        controlRunningDownloads(account, link, true);
        try {
            /* start the dl */
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlRunningDownloads(account, link, false);
        }
    }

    private void handleDownloadPasswordWebsite(final Browser br, final DownloadLink link) throws PluginException, IOException {
        if (br.containsHTML("\"file_password\"")) {
            logger.info("This file seems to be password protected.");
            link.setPasswordProtected(true);
            String passCode = link.getDownloadPassword();
            if (passCode == null) {
                passCode = getUserInput("Password?", link);
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
    }

    private void handlePreDownloadStuff(final URLConnectionAdapter connection, final DownloadLink link, final Account account, String finalLink) {
        final String finalLinkProperty = this.getDirectlinkproperty(account);
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

    protected void handleErrorsAfterDownloadstart(final Browser br, final DownloadLink link) throws PluginException, IOException {
        if (this.looksLikeDownloadableContent(dl.getConnection())) {
            /* No errors */
            return;
        }
        br.followConnection(true);
        final String downloadErrorTextFromHeader = dl.getConnection().getHeaderField("Download-Error");
        if (StringUtils.isNotEmpty(downloadErrorTextFromHeader)) {
            if (StringUtils.equalsIgnoreCase("No such file", downloadErrorTextFromHeader)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, downloadErrorTextFromHeader);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download-Error: " + downloadErrorTextFromHeader);
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
        return -1;
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account, AbstractProxySelectorImpl proxy) {
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
        String expire = br.getRegex("(?i)Gold-Zugriff:\\s*<b>(.*?)</b></div>").getMatch(0);
        if (expire == null) {
            expire = br.getRegex("(?i)Gold Zugriff bis:\\s*<b>(.*?)</b></div>").getMatch(0);
            if (expire == null) {
                expire = br.getRegex("Gold(-| )(Zugriff|Zugang)( bis)?: <b>(.*?)</b></div>").getMatch(3);
                // russian ip subnets or accounts, seem to not respect url language setting
                if (expire == null) {
                    expire = br.getRegex("(?i)<div class=\"access\">Ваш Gold доступ активен до: <b>(.*?)</b></div>").getMatch(0);
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
            ai.setValidUntil(date.getTime(), br);
        }
        return ai;
    }

    private void webLogin(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                setLangtoGer(br);
                final Cookies cookies = account.loadCookies("");
                String uprand = account.getStringProperty("uprand");
                if (uprand != null) {
                    br.setCookie(MAINPAGE.get(), "uprand", uprand);
                }
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        return;
                    }
                    logger.info("Validating login cookies");
                    br.getPage(MAINPAGE.get());
                    if (this.isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(null);
                    }
                }
                logger.info("Performing full login");
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
                    account.removeProperty("uprand");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("(?i)logout\\.php") && br.getCookie(br.getHost(), "autologin", Cookies.NOTDELETEDPATTERN) != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* 2016-10-08: Free account download does not work via API anymore --> Use web download */
        if (useAPI.get() && account.getType() == AccountType.PREMIUM) {
            apiHandlePremium(link, account);
        } else {
            webHandleDownload(link, account);
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
            final Object ret = account.getProperty("accountData");
            if (ret instanceof Map) {
                return (Map<String, Object>) ret;
            } else {
                return null;
            }
        }
    }

    private void apiGetPage(final String url) throws Exception {
        apiPrepBr(br);
        br.getPage(adjustProtocol(url));
    }

    private String getToken(final Account account) throws Exception {
        final Map<String, Object> accountData = getAccountData(account);
        if (accountData != null && accountData.containsKey("token")) {
            final String result = accountData.get("token").toString();
            return result.replace("%2F", "/");
        }
        return null;
    }

    private Browser apiPrepBr(final Browser ibr) {
        ibr.getHeaders().put("User-Agent", "Java/" + System.getProperty("java.version"));
        ibr.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        ibr.getHeaders().put("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
        ibr.getHeaders().put("Accept-Language", null);
        ibr.getHeaders().put("Accept-Charset", null);
        ibr.getHeaders().put("Accept-Encoding", null);
        ibr.getHeaders().put("Pragma", null);
        ibr.setFollowRedirects(true);
        return ibr;
    }

    private AccountInfo apiFetchAccountInfo(final Account account) throws Exception {
        logger.info("apiFetchAccountInfo method in use!");
        final AccountInfo ai = new AccountInfo();
        try {
            // final String storedToken = this.getToken(account);
            // if (storedToken != null) {
            // // TODO: Maybe re-use this token?
            // }
            final UrlQuery query = new UrlQuery();
            query.add("login", Encoding.urlEncode(account.getUser()));
            query.add("password", Encoding.urlEncode(account.getPass()));
            apiGetPage(API_BASE + "/user/login?" + query.toString());
            Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            String error = (String) entries.get("error");
            if (StringUtils.equalsIgnoreCase(error, "CaptchaRequired")) {
                final String c = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LdyfgcTAAAAAArE1fk9cGyExtKfT4a12dWcViye").getToken();
                if (c == null || c.equals("")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                query.add("recaptcha_challenge_field", "null");
                query.add("g-recaptcha-response", Encoding.urlEncode(c));
                apiGetPage(API_BASE + "/user/login?" + query.toString());
                entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            }
            error = (String) entries.get("error");
            if (error != null) {
                throw new AccountInvalidException(error);
            }
            final Map<String, Object> data = (Map<String, Object>) entries.get("data");
            String token = (String) data.get("token");
            final String passKey = (String) data.get("member_passkey");
            if (token == null) {
                token = br.getCookie(br.getHost(), "autologin", Cookies.NOTDELETEDPATTERN);
            }
            final String mode = (String) data.get("mode");
            if (StringUtils.isEmpty(token) || StringUtils.isEmpty(passKey) || StringUtils.isEmpty(mode)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Map<String, Object> accountData = new HashMap<String, Object>();
            accountData.put("token", token);
            accountData.put("passKey", passKey);
            accountData.put("mode", mode);
            final String expire = (String) data.get("gold_expired");
            if ("gold".equalsIgnoreCase(mode)) {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
                if (StringUtils.isEmpty(expire)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
                final Date date = dateFormat.parse(expire);
                ai.setValidUntil(date.getTime());
            } else {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
            }
            saveAccountData(accountData, account);
            /**
             * We can't validate logins so we always need to generate a fresh token. </br>
             * API will request login captchas if we're doing this too frequently.
             */
            account.setRefreshTimeout(5 * 60 * 60 * 1000l);
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.removeProperty("accountData");
            }
            throw e;
        }
        return ai;
    }

    private void apiHandlePremium(final DownloadLink link, final Account account) throws Exception {
        String passCode = link.getDownloadPassword();
        final boolean resume = true;
        final int premiumChunklimit = 0;
        int maxchunks;
        if (account.getType() == AccountType.FREE) {
            logger.info(account.getUser() + " @ Free Account :: API download method in use");
            maxchunks = 1;
        } else {
            logger.info(account.getUser() + " @ Gold Account :: API download method in use");
            maxchunks = premiumChunklimit;
        }
        final UrlQuery query = new UrlQuery();
        query.add("token", getToken(account));
        query.add("file_id", getFID(link));
        if (passCode != null) {
            query.add("file_password", Encoding.urlEncode(passCode));
        }
        apiGetPage(API_BASE + "/download/file?" + query.toString());
        final String errorPasswordProtected = "FileIsPasswordProtected";
        Map<String, Object> root = this.handleErrorsApi(link, account, errorPasswordProtected);
        if (errorPasswordProtected.equalsIgnoreCase((String) root.get("error"))) {
            /* File is password protected */
            link.setPasswordProtected(true);
            passCode = getUserInput("Password?", link);
            query.addAndReplace("file_password", Encoding.urlEncode(passCode));
            apiGetPage(API_BASE + "/download/file?" + query.toString());
            root = this.handleErrorsApi(link, account);
            /* No exception? User must have entered correct password. */
            link.setDownloadPassword(passCode);
        }
        Map<String, Object> root_data = (Map<String, Object>) root.get("data");
        if (account.getType() == AccountType.FREE) {
            final String mode = (String) root_data.get("mode");
            final Number delay = (Number) root_data.get("delay");
            final String dlToken = (String) root_data.get("download_token");
            if (delay == null && StringUtils.isEmpty(mode) && StringUtils.isEmpty(dlToken)) {
                /* This should never happen. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Bad API response");
            }
            /*
             * download modes seem to indicate if the user can download as 'gold' or 'free' connection ratios?. User can download their own
             * uploads under gold even though they don't have gold account status.
             */
            if ("gold".equalsIgnoreCase(mode)) {
                // resume = true;
                maxchunks = premiumChunklimit;
            } else {
                /* mode == "free" */
                /* 2023-07-17: This will most likely fail. Looks like they've disabled free downloads in API mode. */
                sleep(delay.intValue() * 1001, link);
                // final CaptchaHelperHostPluginRecaptchaV2 rc = new CaptchaHelperHostPluginRecaptchaV2(this, br,
                // "6LdyfgcTAAAAAArE1fk9cGyExtKfT4a12dWcViye");
                // final String recaptchaV2Response = rc.getToken();
                // query.add("challenge", "null");
                // query.add("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                // query.add("response", "null");
                query.add("download_token", Encoding.urlEncode(dlToken));
                apiGetPage(API_BASE + "/download/file?" + query.toString());
                root = handleErrorsApi(link, account);
                root_data = (Map<String, Object>) root.get("data");
            }
        }
        String directurl = (String) root_data.get("download_url");
        if (StringUtils.isEmpty(directurl)) {
            /* This should never happen. */
            if ("Guest".equals(root_data.get("mode"))) {
                /* API response looks like free/anonymous download mode although we're supposed to be in premium mode */
                throw new AccountUnavailableException("Account not in use-VPN/Proxy blocked?", 15 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Bad API response: Failed to find final downloadurl");
            }
        }
        directurl = adjustProtocol(directurl);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, directurl, resume, maxchunks);
        handleErrorsAfterDownloadstart(br, link);
        handlePreDownloadStuff(dl.getConnection(), link, account, directurl);
        /* Add a download slot */
        controlRunningDownloads(account, link, true);
        try {
            /* start the dl */
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlRunningDownloads(account, link, false);
        }
    }

    private Map<String, Object> handleErrorsApi(final DownloadLink link, final Account account) throws PluginException, IOException {
        return handleErrorsApi(link, account);
    }

    private Map<String, Object> handleErrorsApi(final DownloadLink link, final Account account, final String errorStrIgnore) throws PluginException, IOException {
        Map<String, Object> entries = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } catch (final JSonMapperException ignore) {
            /* This should never happen. */
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid API response", 60 * 1000l);
            } else {
                throw new AccountUnavailableException("Invalid API response", 60 * 1000);
            }
        }
        handleErrorsApi(entries, link, account, errorStrIgnore);
        return entries;
    }

    private void handleErrorsApi(final Map<String, Object> entries, final DownloadLink link, final Account account) throws PluginException, IOException {
        handleErrorsApi(entries, link, account);
    }

    private void handleErrorsApi(final Map<String, Object> entries, final DownloadLink link, final Account account, final String errorStrIgnore) throws PluginException, IOException {
        final String status = entries.get("status").toString();
        final String error = (String) entries.get("error");
        if ("OK".equalsIgnoreCase(status)) {
            /* No error */
            return;
        } else if (StringUtils.equalsIgnoreCase(error, errorStrIgnore)) {
            /* Ignore this specific error */
            return;
        }
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
                throw new PluginException(LinkStatus.ERROR_FATAL, error);
            }
        }
    }

    private String getFID(final DownloadLink link) {
        String result = new Regex(link.getPluginPatternMatcher(), "(?i)/files/([^/]+)").getMatch(0);
        return result;
    }

    private boolean preferHTTPS() {
        /* 2023-07-06: Removed https setting as website is enforcing https */
        return true;// getPluginConfig().getBooleanProperty(SETTING_SSL_CONNECTION, true);
    }

    private String adjustProtocol(String link) {
        if (preferHTTPS()) {
            link = link.replaceFirst("(?i)http://", "https://");
        } else {
            link = link.replaceFirst("(?i)https://", "http://");
        }
        return link;
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