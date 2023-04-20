//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class UlozTo extends PluginForHost {
    private static final String  QUICKDOWNLOAD                = "https?://[^/]+/quickDownload/\\d+";
    /* 2017-01-02: login API seems to be broken --> Use website as workaround */
    private static final boolean use_login_api                = false;
    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(20);
    /* don't touch the following! */
    private static AtomicInteger freeRunning                  = new AtomicInteger(0);

    public UlozTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://uloz.to/kredit");
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getContentURL(final DownloadLink link) {
        return link.getPluginPatternMatcher().replaceAll("https?://[^/]+/", "https://" + this.getHost() + "/");
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        /**
         * ulozto.net = the English version of the site </br>
         * Important: Each language version has it's own beginning URL-structure e.g.: </br>
         * https://ulozto.net/file/<fid>/<slug> English website does not work with "/soubory/"! </br>
         * * https://uloz.to/soubory/<fid>/<slug> </br>
         */
        ret.add(new String[] { "uloz.to", "ulozto.sk", "ulozto.cz", "ulozto.net", "zachowajto.pl" });
        ret.add(new String[] { "pornfile.cz", "pornfile.ulozto.net" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:file|soubory)/([\\!a-zA-Z0-9]+)(/([\\w\\-]+))?");
        }
        return ret.toArray(new String[0]);
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public String getAGBLink() {
        return "https://ulozto.net/tos/terms-of-service";
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public Browser prepBR(final Browser br) {
        br.setCustomCharset("utf-8");
        br.setAllowedResponseCodes(new int[] { 400, 401, 410, 451 });
        br.setCookie(this.getHost(), "adblock_detected", "false");
        br.setCookie(this.getHost(), "maturity", "adult");
        return br;
    }

    private String finalDirectDownloadURL = null;

    /**
     * 2019-09-16: They are GEO-blocking several countries including Germany. Error 451 will then be returned! This can be avoided via their
     * Android-App: https://uloz.to/androidapp
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        synchronized (freeRunning) {
            return requestFileInformation(link, null, false);
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            /* Set weak filename */
            final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
            final String fid = urlinfo.getMatch(0);
            final String urlSlug = urlinfo.getMatch(2);
            if (urlSlug != null) {
                final String[] segments = urlSlug.split("-");
                String weakFilename;
                if (segments.length >= 2) {
                    /* Assume that file extension is last part of slug. */
                    weakFilename = urlSlug.substring(0, urlSlug.lastIndexOf("-")) + "." + segments[segments.length - 1];
                } else {
                    weakFilename = urlSlug;
                }
                weakFilename = weakFilename.replace("-", " ").trim();
                link.setName(weakFilename);
            } else {
                link.setName(fid);
            }
        }
        prepBR(this.br);
        if (account != null) {
            login(account, false);
        }
        br.setFollowRedirects(false);
        final String contentURL = getContentURL(link);
        if (contentURL.matches(QUICKDOWNLOAD)) {
            return AvailableStatus.TRUE;
        } else if (contentURL.matches("(?i)https?://[^/]+/(podminky|tos)/[^/]+")) {
            return AvailableStatus.FALSE;
        }
        finalDirectDownloadURL = handleDownloadUrl(link, isDownload);
        if (finalDirectDownloadURL != null) {
            return AvailableStatus.TRUE;
        } else if (this.isPrivateFile(br)) {
            /* File is online but we can't get any information about the file. */
            return AvailableStatus.TRUE;
        }
        checkErrors(br, link, account);
        handleAgeRestrictedRedirects();
        responseCodeOfflineCheck(br);
        // Wrong links show the mainpage so here we check if we got the mainpage or not
        if (br.containsHTML("(multipart/form\\-data|Chybka 404 \\- požadovaná stránka nebyla nalezena<br>|<title>Ulož\\.to</title>|<title>404 \\- Page not found</title>)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (handleLimitExceeded(link, br, isDownload)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Limit exceeded");
        }
        final boolean passwordProtected = this.isPasswordProtected(br);
        link.setPasswordProtected(passwordProtected);
        if (!passwordProtected && !this.br.containsHTML("class=\"jsFileTitle[^\"]*")) {
            /* Seems like whatever url the user added, it is not a downloadurl. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^\"]+)").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("itemprop=\"name\" content=\"([^\"]+)").getMatch(0);
        }
        // For video links
        String filesize = br.getRegex("<span id=\"fileSize\">(\\d{2}:\\d{2}(:\\d{2})? \\| )?(\\d+(\\.\\d{2})? [A-Za-z]{1,5})</span>").getMatch(2);
        if (filesize == null) {
            filesize = br.getRegex("id=\"fileVideo\".+class=\"fileSize\">\\d{2}:\\d{2} \\| ([^<>\"]*?)</span>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("(?i)<span>\\s*Velikost\\s*</span>([^<>\"]+)<").getMatch(0);
                // For file links
                if (filesize == null) {
                    filesize = br.getRegex("<span id=\"fileSize\">.*?\\|([^<>]*?)</span>").getMatch(0); // 2015-08-08
                    if (filesize == null) {
                        filesize = br.getRegex("<span id=\"fileSize\">([^<>\"]*?)</span>").getMatch(0);
                    }
                }
            }
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            /* Remove some stuff that is present for streams such as " | on-line video | Ulož.to Disk" or " | on-line video". */
            filename = filename.replaceFirst(" \\| on-line video.*", "");
            link.setName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    public void handleAgeRestrictedRedirects() throws Exception {
        /* For age restricted links */
        final String ageFormToken = br.getRegex("id=\"frm-askAgeForm-_token_\" value=\"([^<>\"]*?)\"").getMatch(0);
        String urlBefore = br.getURL();
        if (ageFormToken != null) {
            /* 2016-05-24: This might be outdated */
            br.postPage(br.getURL(), "agree=Confirm&do=askAgeForm-submit&_token_=" + Encoding.urlEncode(ageFormToken));
            handleRedirect();
        } else if (br.containsHTML("value=\"pornDisclaimer-submit\"")) {
            /* 2016-05-24: This might be outdated */
            String currenturlpart = new Regex(br.getURL(), "https?://[^/]+(/.+)").getMatch(0);
            currenturlpart = Encoding.urlEncode(currenturlpart);
            br.postPage("/porn-disclaimer/?back=" + currenturlpart, "agree=Souhlas%C3%ADm&_do=pornDisclaimer-submit");
            /* Only follow redirects going back to our initial URL */
            if (br.getRedirectLocation() != null && br.getRedirectLocation().equalsIgnoreCase(urlBefore)) {
                br.getPage(br.getRedirectLocation());
            }
        } else if (br.containsHTML("id=\"frm\\-askAgeForm\"")) {
            /*
             * 2016-05-24: Uloz.to recognizes porn files and moves them from uloz.to to pornfile.cz (usually with the same filename- and
             * link-ID.
             */
            /* Agree to redirect from uloz.to to pornfile.cz */
            br.postPage(this.br.getURL(), "agree=Souhlas%C3%ADm&do=askAgeForm-submit");
            /* Only follow redirects going back to our initial URL */
            if (br.getRedirectLocation() != null && br.getRedirectLocation().equalsIgnoreCase(urlBefore)) {
                br.getPage(br.getRedirectLocation());
            }
            /* Agree to porn disclaimer */
            final String currenturlpart = new Regex(br.getURL(), "https?://[^/]+(/.+)").getMatch(0);
            br.postPage("/porn-disclaimer/?back=" + Encoding.urlEncode(currenturlpart), "agree=Souhlas%C3%ADm&do=pornDisclaimer-submit");
            /* Only follow redirects going back to our initial URL */
            if (br.getRedirectLocation() != null && br.getRedirectLocation().equalsIgnoreCase(urlBefore)) {
                br.getPage(br.getRedirectLocation());
            }
        }
    }

    /**
     * Accesses downloadurl and checks for content. </br>
     * Returns final downloadurl.
     */
    private String handleDownloadUrl(final DownloadLink link, final boolean isDownload) throws Exception {
        br.getPage(this.getContentURL(link));
        int i = 0;
        while (br.getRedirectLocation() != null) {
            if (i == 10) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Redirect loop");
            }
            logger.info("Getting redirect-page");
            final URLConnectionAdapter con = br.openRequestConnection(br.createRedirectFollowingRequest(br.getRequest()));
            if (looksLikeDownloadableContent(con)) {
                con.disconnect();
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String fileName = getFileNameFromDispositionHeader(con);
                if (fileName != null) {
                    link.setFinalFileName(fileName);
                }
                link.setAvailable(true);
                return con.getRequest().getUrl();
            }
            br.followConnection();
            handleLimitExceeded(link, br, isDownload);
            i++;
        }
        responseCodeOfflineCheck(br);
        return null;
    }

    private boolean handleLimitExceeded(DownloadLink link, final Browser br, final boolean isDownload) throws Exception {
        if (br.containsHTML("/limit-exceeded") || StringUtils.containsIgnoreCase(br.getURL(), "/limit-exceeded")) {
            if (!isDownload) {
                logger.info("\"limit exceeded\" captcha skipped");
                /* Don't ask for captchas during availablecheck */
                return true;
            } else {
                logger.info("\"limit exceeded\" captcha required");
                final Form f = br.getFormbyActionRegex(".*/limit-exceeded.*");
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                f.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                br.submitForm(f);
                if (br.containsHTML("/limit-exceeded") || StringUtils.containsIgnoreCase(br.getURL(), "/limit-exceeded")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private void responseCodeOfflineCheck(final Browser br) throws PluginException {
        final int responseCode = br.getHttpConnection().getResponseCode();
        if (responseCode == 400 || responseCode == 410 || responseCode == 451) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    /** Handles special redirects e.g. after submitting 'Age restricted' Form. */
    private void handleRedirect() throws Exception {
        for (int i = 0; i <= i; i++) {
            final String continuePage = br.getRegex("(?i)<p><a href=\"(http://.*?)\">Please click here to continue</a>").getMatch(0);
            if (continuePage != null) {
                br.getPage(continuePage);
            } else {
                break;
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        doFree(link, null);
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink link, final Account account) throws Exception {
        AvailableStatus status = requestFileInformation(link, account, true);
        final String contentURL = this.getContentURL(link);
        if (contentURL.matches(QUICKDOWNLOAD)) {
            throw new AccountRequiredException();
        }
        this.checkErrors(br, link, account);
        if (AvailableStatus.UNCHECKABLE.equals(status)) {
            /* TODO: 2020-08-28 Check if this is still required at this place */
            final Form form = br.getFormbyActionRegex("limit-exceeded");
            if (form == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(form);
            status = requestFileInformation(link);
        }
        if (AvailableStatus.UNCHECKABLE.equals(status)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        br.setFollowRedirects(true);
        String dllink = checkDirectLink(link, "directlink_free");
        if (dllink == null) {
            boolean captcha_failed = false;
            /* 2019-05-15: New: Free download without captcha */
            dllink = br.getRegex("(/[^\"<>]+\\?do=slowDirectDownload[^\"<>]*?)\"").getMatch(0);
            if (dllink == null) {
                /* 2020-03-09: New */
                dllink = br.getRegex("(/slowDownload[^\"<>]+)\"").getMatch(0);
            }
            if (dllink == null) {
                if (link.isPasswordProtected()) {
                    handlePassword(link);
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                Browser cbr = null;
                for (int i = 0; i <= 5; i++) {
                    if (i == 5) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    /* 2020-03-06: url_free_dialog is new */
                    final String url_free_dialog = br.getRegex("(/download-dialog/free[^<>\"]+)").getMatch(0);
                    if (url_free_dialog == null) {
                        logger.info("url_free_dialog is null");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.getPage(url_free_dialog);
                    final String redirect = PluginJSonUtils.getJson(br, "redirectDialogContent");
                    if (redirect != null) {
                        /* Check for extra captcha */
                        br.getPage(redirect);
                        final Form form = br.getFormbyActionRegex("limit-exceeded");
                        if (form != null) {
                            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                            form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                            br.submitForm(form);
                        }
                    }
                    Form captchaForm = br.getFormbyProperty("id", "frm-downloadDialog-freeDownloadForm");
                    if (captchaForm == null) {
                        captchaForm = br.getFormbyProperty("id", "frm-download-freeDownloadTab-freeDownloadForm");
                    }
                    if (captchaForm == null) {
                        captchaForm = br.getFormbyProperty("id", "frm-freeDownloadForm-form");
                    }
                    if (captchaForm == null) {
                        logger.info("Failed to find captchaForm --> Assume captcha is not needed");
                        dllink = getFinalDownloadurl(br);
                        break;
                    }
                    captcha_failed = true;
                    cbr = br.cloneBrowser();
                    cbr.getPage("/reloadXapca.php?rnd=" + System.currentTimeMillis());
                    if (cbr.getRequest().getHttpConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                    }
                    final String hash = PluginJSonUtils.getJsonValue(cbr, "hash");
                    final String timestamp = PluginJSonUtils.getJsonValue(cbr, "timestamp");
                    final String salt = PluginJSonUtils.getJsonValue(cbr, "salt");
                    String captchaUrl = PluginJSonUtils.getJsonValue(cbr, "image");
                    if (captchaForm == null || captchaUrl == null || hash == null || timestamp == null || salt == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    URLConnectionAdapter con = null;
                    File file = null;
                    String filename = null;
                    try {
                        final Browser brCaptcha = br.cloneBrowser();
                        con = brCaptcha.openGetConnection(captchaUrl);
                        if (con.isOK()) {
                            if (con.isContentDisposition()) {
                                filename = Plugin.getFileNameFromDispositionHeader(con);
                            } else {
                                filename = JDHash.getMD5(captchaUrl);
                            }
                            file = JDUtilities.getResourceFile("captchas/ulozto/" + filename);
                            if (file == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            file.deleteOnExit();
                            brCaptcha.downloadConnection(file, con);
                        } else {
                            /* 2019-07-31: E.g. redirect to 'https://xapca6.uloz.to/blocked' (response 404) --> Possible GEO-block?? */
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Cannot load captcha");
                        }
                    } finally {
                        try {
                            con.disconnect();
                        } catch (final Throwable e) {
                        }
                    }
                    if (file == null || !file.exists()) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String code = getCaptchaCode(file, link);
                    if (code == null) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    captchaForm.put("captcha_value", Encoding.urlEncode(code));
                    captchaForm.remove(null);
                    captchaForm.remove("freeDownload");
                    captchaForm.put("timestamp", timestamp);
                    captchaForm.put("salt", salt);
                    captchaForm.put("hash", hash);
                    /* 2018-08-30: Wait some seconds or we might run into an error. */
                    this.sleep(3000, link);
                    br.submitForm(captchaForm);
                    // If captcha fails, throrotws exception
                    // If in automatic mode, clears saved data
                    final boolean isError2020 = br.containsHTML("formErrorContent");
                    if (br.containsHTML("\"errors\"\\s*:\\s*\\[\\s*\"(Error rewriting the text|Rewrite the text from the picture|Text je opsán špatně|An error ocurred while)") || br.containsHTML("\"new_captcha_data\"") || isError2020) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    final String redirectToSecondCaptcha = PluginJSonUtils.getJson(br, "redirectDialogContent");
                    if (redirectToSecondCaptcha != null) {
                        /**
                         * 2021-02-11: Usually: /download-dialog/free/limit-exceeded?fileSlug=<FUID>&repeated=0&nocaptcha=0 </br>
                         * This can happen after downloading some files. The user is allowed to download more but has to solve two captchas
                         * in a row to do so!
                         */
                        br.getPage(redirectToSecondCaptcha);
                        final Form f = br.getFormbyActionRegex(".*limit-exceeded.*");
                        if (f != null) {
                            if (f.containsHTML("class=\"g-recaptcha\"")) {
                                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                                f.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                            }
                            br.submitForm(f);
                        } else {
                            /* This should never happen */
                            logger.warning("Limit reached and failed to find Form for 2nd captcha");
                            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                        }
                    }
                    dllink = getFinalDownloadurl(br);
                    if (StringUtils.isEmpty(dllink)) {
                        break;
                    }
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
                    if (this.looksLikeDownloadableContent(dl.getConnection())) {
                        captcha_failed = false;
                        break;
                    } else {
                        br.followConnection(true);
                        if (account != null) {
                            if (br.containsHTML("Pro rychlé stažení") || br.containsHTML("You do not have  enough") || br.containsHTML("Nie masz wystarczającego")) {
                                throw new AccountUnavailableException("Not enough premium traffic available", 1 * 60 * 60 * 1000l);
                            }
                        }
                        if (br.containsHTML("Stránka nenalezena")) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        } else if (br.containsHTML("dla_backend/uloz\\.to\\.overloaded\\.html")) {
                            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 10 * 60 * 1000l);
                        } else if (br.containsHTML("Chyba při ověření uživatele|Nastala chyba při odeslání textu\\. Znovu opiš text z obrázku\\.")) {
                            if (account != null) {
                                synchronized (account) {
                                    account.clearCookies("");
                                }
                                throw new PluginException(LinkStatus.ERROR_RETRY);
                            } else {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                        br.clearCookies("ulozto.net");
                        br.clearCookies("uloz.to");
                        dllink = handleDownloadUrl(link, true);
                        if (dllink != null) {
                            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
                            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                                captcha_failed = false;
                                break;
                            } else {
                                br.followConnection(true);
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                        continue;
                    }
                }
            }
            if (account != null) {
                if (br.containsHTML("Pro rychlé stažení") || br.containsHTML("You do not have  enough") || br.containsHTML("Nie masz wystarczającego")) {
                    throw new AccountUnavailableException("Not enough premium traffic available", 1 * 60 * 60 * 1000l);
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (dllink.contains("/error404/?fid=file_not_found")) {
                logger.info("The user entered the correct captcha but this file is offline...");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (captcha_failed) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        if (dl == null) {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The finallink doesn't seem to be a file: " + dllink);
            try {
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
            if (account != null) {
                if (br.containsHTML("Pro rychlé stažení") || br.containsHTML("You do not have  enough") || br.containsHTML("Nie masz wystarczającego")) {
                    throw new AccountUnavailableException("Not enough premium traffic available", 1 * 60 * 60 * 1000l);
                }
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 503 && br.getHttpConnection().getHeaderField("server") != null && br.getHttpConnection().getHeaderField("server").toLowerCase(Locale.ENGLISH).contains("nginx")) {
                // 503 with nginx means no more connections allow, it doesn't mean server error!
                synchronized (freeRunning) {
                    final int maxFreeBefore = totalMaxSimultanFreeDownload.get();
                    totalMaxSimultanFreeDownload.set(Math.min(Math.max(1, freeRunning.get()), maxFreeBefore));
                    logger.info("maxFreeRunning(" + link.getName() + ")|running:" + freeRunning.get() + "|before:" + maxFreeBefore + "|after:" + totalMaxSimultanFreeDownload.get());
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.setFilenameFix(true);
        link.setProperty("directlink_free", dl.getConnection().getURL().toString());
        /* Add a download slot */
        controlMaxFreeDownloads(account, link, +1);
        try {
            /* start the dl */
            dl.startDownload();
        } finally {
            /* Remove download slot */
            controlMaxFreeDownloads(account, link, -1);
        }
    }

    private boolean isPrivateFile(final Browser br) {
        return br.containsHTML(">\\s*(Soubor je označen jako soukromý|The file has been marked as private)");
    }

    private String getFinalDownloadurl(Browser br) {
        String dllink = PluginJSonUtils.getJsonValue(br, "url");
        if (StringUtils.isEmpty(dllink)) {
            dllink = PluginJSonUtils.getJsonValue(br, "downloadLink");
            if (StringUtils.isEmpty(dllink)) {
                /* 2020-08-27: pornfile.cz */
                dllink = PluginJSonUtils.getJsonValue(br, "slowDownloadLink");
            }
        }
        return dllink;
    }

    /**
     * @author raztoki
     * @param link
     * @throws Exception
     */
    private void handlePassword(final DownloadLink link) throws Exception {
        final boolean ifr = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            final boolean preferFormHandling = true;
            if (preferFormHandling) {
                /* 2016-12-07: Prefer this way to prevent failures due to wrong website language! */
                final Form pwform = br.getFormbyKey("password_send");
                if (pwform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                String passCode = link.getDownloadPassword();
                if (StringUtils.isEmpty(passCode)) {
                    passCode = getUserInput("Password?", link);
                }
                pwform.put("password", Encoding.urlEncode(passCode));
                br.submitForm(pwform);
                if (this.isPasswordProtected(br)) {
                    // failure
                    logger.info("Incorrect password was entered: " + passCode);
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else {
                    logger.info("Correct password has been entered: " + passCode);
                    link.setDownloadPassword(passCode);
                    return;
                }
            } else {
                String passCode = link.getDownloadPassword();
                if (StringUtils.isEmpty(passCode)) {
                    passCode = getUserInput("Password?", link);
                }
                br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode) + "&password_send=Send&do=passwordProtectedForm-submit");
                if (this.isPasswordProtected(br)) {
                    // failure
                    logger.info("Incorrect password was entered");
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else {
                    logger.info("Correct password has been entered");
                    link.setDownloadPassword(passCode);
                    return;
                }
            }
        } finally {
            br.setFollowRedirects(ifr);
        }
    }

    private boolean isPasswordProtected(final Browser br) {
        final boolean isPasswordProtectedAccordingToResponseCode = br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 401;
        final boolean isPasswordProtectedAccordingToHTML = br.containsHTML("\"frm\\-passwordProtectedForm\\-password\"");
        return isPasswordProtectedAccordingToHTML || isPasswordProtectedAccordingToResponseCode;
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            /* Free Account */
            doFree(link, account);
        } else {
            /* Premium Account */
            requestFileInformation(link, account, true);
            this.checkErrors(br, link, account);
            final String contentURL = this.getContentURL(link);
            String dllink = finalDirectDownloadURL;
            if (dllink == null) {
                if (contentURL.matches(QUICKDOWNLOAD)) {
                    dllink = contentURL;
                } else {
                    if (link.isPasswordProtected()) {
                        handlePassword(link);
                    }
                    dllink = br.getRegex("(/quickDownload/[^<>\"\\']+)\"").getMatch(0);
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection();
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (br.containsHTML("Pro rychlé stažení") || br.containsHTML("You do not have  enough") || br.containsHTML("Nie masz wystarczającego")) {
                    throw new AccountUnavailableException("Not enough premium traffic available", 1 * 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl.startDownload();
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc != null && acc.getType() == AccountType.PREMIUM) {
            /* Premium account -> No captchas expected */
            return false;
        } else {
            return true;
        }
    }

    private void loginAPI(Account account, final AccountInfo aa) throws Exception {
        synchronized (account) {
            try {
                final AccountInfo ai = aa != null ? aa : account.getAccountInfo();
                setBrowserExclusive();
                br.setFollowRedirects(true);
                prepBR(this.br);
                br.getHeaders().put("Accept", "text/html, */*");
                br.getHeaders().put("Accept-Encoding", "identity");
                br.getHeaders().put("User-Agent", "UFM 1.5");
                br.getPage("http://api.uloz.to/login.php?kredit=1&uzivatel=" + Encoding.urlEncode(account.getUser()) + "&heslo=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("ERROR")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final String trafficleft = br.toString().trim();
                if (trafficleft != null) {
                    ai.setTrafficLeft(SizeFormatter.getSize(trafficleft + " KB"));
                }
                if (aa == null) {
                    account.setAccountInfo(ai);
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            } finally {
                setBasicAuthHeader(account);
            }
        }
    }

    private void checkErrors(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        if (this.isPrivateFile(br)) {
            throw new AccountRequiredException();
        }
        this.checkGeoBlocked(br, account);
        if (br.containsHTML("(?i)The file is not available at this moment, please, try it later")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The file is not available at this moment, please, try it later", 15 * 60 * 1000l);
        }
    }

    private void checkGeoBlocked(final Browser br, final Account account) throws PluginException {
        if (StringUtils.containsIgnoreCase(br.getURL(), "/blocked")) {
            if (account != null) {
                throw new AccountUnavailableException("Geoblocked", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Geoblocked", 24 * 60 * 60 * 1000l);
            }
        }
    }

    private void loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                setBrowserExclusive();
                br.setFollowRedirects(true);
                prepBR(this.br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    this.br.getPage("https://" + account.getHoster());
                    checkGeoBlocked(br, account);
                    handleAgeRestrictedRedirects();
                    if (isLoggedIn(br)) {
                        logger.info("Cookie login successful");
                        /* Re-new cookie timestamp */
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                this.br.getPage("https://" + account.getHoster() + "/login");
                checkGeoBlocked(br, account);
                handleAgeRestrictedRedirects();
                final Form loginform = br.getFormbyKey("username");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (loginform.hasInputFieldByName("remember")) {
                    loginform.remove("remember");
                }
                loginform.put("remember", "on");
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (!isLoggedIn(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIn(final Browser br) {
        final boolean isLoggedinByHTML = br.containsHTML("do=web-logout");
        boolean isLoggedinByCookie = false;
        for (final Cookie cookie : br.getCookies(br.getHost()).getCookies()) {
            if (cookie.getKey() != null && cookie.getKey().matches("permanentLogin.*") && cookie.getValue() != null && cookie.getValue().matches(Cookies.NOTDELETEDPATTERN)) {
                isLoggedinByCookie = true;
                break;
            }
        }
        if (isLoggedinByHTML || isLoggedinByCookie) {
            return true;
        } else {
            return false;
        }
    }

    private void setBasicAuthHeader(final Account account) {
        this.br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
    }

    private void login(final Account account, final boolean force) throws Exception {
        if (use_login_api) {
            this.loginAPI(account, null);
        } else {
            this.loginWebsite(account, force);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (use_login_api) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginAPI(account, ai);
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        String trafficleftStr = br.getRegex("data-remaining-credit=\"([^\"]+)").getMatch(0);
        if (trafficleftStr == null) {
            trafficleftStr = br.getRegex("class=\"t-credit-amount\"[^>]*>([^<]+)").getMatch(0);
        }
        ai.setTrafficRefill(false);
        long trafficleft = -1;
        if (trafficleftStr != null) {
            trafficleft = SizeFormatter.getSize(trafficleftStr);
        }
        this.setTrafficLeft(account, ai, trafficleft);
        final Regex spaceMaxAndUsed = br.getRegex("class=\"t-space\" max=\"(\\d+)\" value=\"(\\d+)\"");
        // final String spaceMaxBytesStr = spaceMaxAndUsed.getMatch(0);
        final String spaceUsedBytesStr = spaceMaxAndUsed.getMatch(1);
        if (spaceUsedBytesStr != null) {
            /* Prefer precise values. */
            ai.setUsedSpace(Long.parseLong(spaceUsedBytesStr));
        } else {
            final String usedSpaceStr = br.getRegex("class=\"t-disk-space-used\"[^>]*>([^<]+)<").getMatch(0);
            if (usedSpaceStr != null) {
                ai.setUsedSpace(SizeFormatter.getSize(usedSpaceStr));
            }
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /**
             * 2023-04-18: Debug test </br>
             * This is their Web-API. It provides slightly less information than the website via html and the API key may change at any
             * time.
             */
            br.getPage("/p-api/get-api-current-user-token");
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final String token = entries.get("token").toString();
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("Accept", "application/json");
            brc.getHeaders().put("x-auth-token", "}p^YyPpxkIT2MB)#!MHE"); // API key
            brc.getHeaders().put("x-user-token", token);
            brc.getPage("https://apis.uloz.to/v5/me");
            final Map<String, Object> usermap = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> user = (Map<String, Object>) usermap.get("user");
            final Number trafficleftO = (Number) user.get("credit");
            this.setTrafficLeft(account, ai, trafficleftO.longValue() * 1024);
        }
        return ai;
    }

    private void setTrafficLeft(final Account account, final AccountInfo ai, final long trafficleft) {
        if (trafficleft > 0) {
            ai.setTrafficLeft(trafficleft);
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
            /* Free Accounts should still be able to download but the limits can't be defined by a simple "traffic left" value. */
            ai.setUnlimitedTraffic();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        final int max = totalMaxSimultanFreeDownload.get();
        if (max == -1) {
            return -1;
        } else {
            // do not sync here! it's read only!
            final int running = freeRunning.get();
            final int ret = Math.min(running + 1, max);
            return ret;
        }
    }

    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        if (account == null || AccountType.FREE.equals(account.getType())) {
            synchronized (freeRunning) {
                final int before = freeRunning.get();
                final int after = before + num;
                freeRunning.set(after);
                logger.info("freeRunning(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        }
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
}