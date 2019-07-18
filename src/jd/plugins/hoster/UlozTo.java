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

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uloz.to", "ulozto.net", "pornfile.cz" }, urls = { "https?://(?:www\\.)?(?:uloz\\.to|ulozto\\.sk|ulozto\\.cz|ulozto\\.net)/(?!soubory/)[\\!a-zA-Z0-9]+/[^\\?\\s]+", "https?://(?:www\\.)?ulozto\\.net/(?!soubory/)[\\!a-zA-Z0-9]+(?:/[^\\?\\s]+)?", "https?://(?:www\\.)?(?:pornfile\\.cz|pornfile\\.ulozto\\.net)/[\\!a-zA-Z0-9]+/[^\\?\\s]+" })
public class UlozTo extends PluginForHost {
    private boolean              passwordProtected            = false;
    private static final String  CAPTCHA_TEXT                 = "CAPTCHA_TEXT";
    private static final String  CAPTCHA_ID                   = "CAPTCHA_ID";
    private static final String  QUICKDOWNLOAD                = "https?://(?:www\\.)?uloz\\.to/quickDownload/\\d+";
    private static final String  PREMIUMONLYUSERTEXT          = "Only downloadable for premium users!";
    /* 2017-01-02: login API seems to be broken --> Use website as workaround */
    private static final boolean use_login_api                = false;
    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(20);
    /* don't touch the following! */
    private static AtomicInteger maxFree                      = new AtomicInteger(1);
    private static Object        CTRLLOCK                     = new Object();

    public UlozTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.uloz.to/kredit");
    }

    public void correctDownloadLink(final DownloadLink link) {
        // ulozto.net = the english version of the site
        link.setUrlDownload(link.getDownloadURL().replaceAll("(ulozto\\.sk|ulozto\\.cz|ulozto\\.net)", "uloz.to").replaceFirst("^http://", "https://"));
    }

    @Override
    public String[] siteSupportedNames() {
        if ("uloz.to".equals(getHost())) {
            return new String[] { "uloz.to", "ulozto.sk", "ulozto.cz", "ulozto.net" };
        } else {
            return new String[] { "pornfile.cz", "pornfile.ulozto.net" };
        }
    }

    @Override
    public String rewriteHost(String host) {
        if ("ulozto.sk".equalsIgnoreCase(host) || "ulozto.cz".equalsIgnoreCase(host) || "ulozto.net".equalsIgnoreCase(host)) {
            return "uloz.to";
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return "http://img.uloz.to/podminky.pdf";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private Browser prepBR(final Browser br) {
        br.setCustomCharset("utf-8");
        br.setAllowedResponseCodes(new int[] { 400, 401, 410, 451 });
        br.setCookie(this.getHost(), "adblock_detected", "false");
        br.setCookie(this.getHost(), "maturity", "adult");
        return br;
    }

    private String finalDirectDownloadURL = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        synchronized (CTRLLOCK) {
            passwordProtected = isPasswordProtected();
            correctDownloadLink(downloadLink);
            prepBR(this.br);
            br.setFollowRedirects(false);
            if (downloadLink.getDownloadURL().matches(QUICKDOWNLOAD)) {
                downloadLink.getLinkStatus().setStatusText(PREMIUMONLYUSERTEXT);
                return AvailableStatus.TRUE;
            }
            if (downloadLink.getDownloadURL().matches("https?://pornfile.cz/(podminky|tos)/[^/]+")) {
                return AvailableStatus.FALSE;
            }
            finalDirectDownloadURL = handleDownloadUrl(downloadLink);
            if (finalDirectDownloadURL != null) {
                return AvailableStatus.TRUE;
            }
            checkGeoBlocked(br, null);
            if (br.containsHTML("/limit-exceeded") || StringUtils.containsIgnoreCase(br.getURL(), "/limit-exceeded")) {
                final Form f = br.getFormbyAction("/limit-exceeded");
                if (f != null) {
                    if (f.containsHTML("class=\"g-recaptcha\"")) {
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        f.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    }
                    br.submitForm(f);
                } else {
                    return AvailableStatus.UNCHECKABLE;
                }
            }
            handleAgeRestrictedRedirects(downloadLink);
            if (br.containsHTML("The file is not available at this moment, please, try it later")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The file is not available at this moment, please, try it later", 15 * 60 * 1000l);
            }
            // responseCode offline check
            responseCodeOfflineCheck();
            // Wrong links show the mainpage so here we check if we got the mainpage or not
            if (br.containsHTML("(multipart/form\\-data|Chybka 404 \\- požadovaná stránka nebyla nalezena<br>|<title>Ulož\\.to</title>|<title>404 \\- Page not found</title>)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = null;
            passwordProtected = this.isPasswordProtected();
            if (!passwordProtected && !this.br.containsHTML("class=\"jsFileTitle")) {
                /* Seems like whatever url the user added, it is not a downloadurl. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (this.passwordProtected) {
                filename = getFilename();
                if (filename == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
                downloadLink.getLinkStatus().setStatusText("This link is password protected");
            } else {
                filename = getFilename();
                // For video links
                String filesize = br.getRegex("<span id=\"fileSize\">(\\d{2}:\\d{2}(:\\d{2})? \\| )?(\\d+(\\.\\d{2})? [A-Za-z]{1,5})</span>").getMatch(2);
                if (filesize == null) {
                    filesize = br.getRegex("id=\"fileVideo\".+class=\"fileSize\">\\d{2}:\\d{2} \\| ([^<>\"]*?)</span>").getMatch(0);
                    if (filesize == null) {
                        filesize = br.getRegex("<span>Velikost</span>([^<>\"]+)<").getMatch(0);
                        // For file links
                        if (filesize == null) {
                            filesize = br.getRegex("<span id=\"fileSize\">.*?\\|([^<>]*?)</span>").getMatch(0); // 2015-08-08
                            if (filesize == null) {
                                filesize = br.getRegex("<span id=\"fileSize\">([^<>\"]*?)</span>").getMatch(0);
                            }
                        }
                    }
                }
                if (filename == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
                if (filesize != null) {
                    downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
                }
            }
            return AvailableStatus.TRUE;
        }
    }

    private void handleAgeRestrictedRedirects(final DownloadLink downloadLink) throws Exception {
        /* For age restricted links */
        final String ageFormToken = br.getRegex("id=\"frm-askAgeForm-_token_\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (ageFormToken != null) {
            /* 2016-05-24: This might be outdated */
            br.postPage(br.getURL(), "agree=Confirm&do=askAgeForm-submit&_token_=" + Encoding.urlEncode(ageFormToken));
            handleRedirect(downloadLink);
        } else if (br.containsHTML("value=\"pornDisclaimer-submit\"")) {
            /* 2016-05-24: This might be outdated */
            br.setFollowRedirects(true);
            String currenturlpart = new Regex(br.getURL(), "https?://[^/]+(/.+)").getMatch(0);
            currenturlpart = Encoding.urlEncode(currenturlpart);
            br.postPage("/porn-disclaimer/?back=" + currenturlpart, "agree=Souhlas%C3%ADm&_do=pornDisclaimer-submit");
            br.setFollowRedirects(false);
        } else if (br.containsHTML("id=\"frm\\-askAgeForm\"")) {
            /*
             * 2016-05-24: Uloz.to recognizes porn files and moves them from uloz.to to pornfile.cz (usually with the same filename- and
             * link-ID.
             */
            this.br.setFollowRedirects(true);
            /* Agree to redirect from uloz.to to pornfile.cz */
            br.postPage(this.br.getURL(), "agree=Souhlas%C3%ADm&do=askAgeForm-submit");
            /* Agree to porn disclaimer */
            final String currenturlpart = new Regex(br.getURL(), "https?://[^/]+(/.+)").getMatch(0);
            br.postPage("/porn-disclaimer/?back=" + Encoding.urlEncode(currenturlpart), "agree=Souhlas%C3%ADm&do=pornDisclaimer-submit");
            br.setFollowRedirects(false);
        }
    }

    private String getFilename() {
        final String filename = br.getRegex("<title>\\s*(.*?)\\s*(?:\\|\\s*(PORNfile.cz|Ulož.to)\\s*)?</title>").getMatch(0);
        return filename;
    }

    private String handleDownloadUrl(final DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        int i = 0;
        while (br.getRedirectLocation() != null) {
            if (i == 10) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Redirect loop");
            }
            logger.info("Getting redirect-page");
            final URLConnectionAdapter con = br.openRequestConnection(br.createRedirectFollowingRequest(br.getRequest()));
            if (con.isContentDisposition() && con.isOK()) {
                con.disconnect();
                if (con.getLongContentLength() > 0) {
                    downloadLink.setVerifiedFileSize(con.getLongContentLength());
                }
                final String fileName = getFileNameFromDispositionHeader(con);
                if (fileName != null) {
                    downloadLink.setFinalFileName(fileName);
                }
                downloadLink.setAvailable(true);
                return con.getRequest().getUrl();
            }
            br.followConnection();
            if (StringUtils.containsIgnoreCase(br.getURL(), "/limit-exceeded")) {
                throw new AccountRequiredException("Not enough premium traffic available");
            }
            i++;
        }
        responseCodeOfflineCheck();
        return null;
    }

    private void responseCodeOfflineCheck() throws PluginException {
        final int responseCode = br.getHttpConnection().getResponseCode();
        if (responseCode == 400 || responseCode == 410 || responseCode == 451) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    /** Handles special redirects e.g. after submitting 'Age restricted' Form. */
    @SuppressWarnings("deprecation")
    private void handleRedirect(final DownloadLink downloadLink) throws Exception {
        for (int i = 0; i <= i; i++) {
            final String continuePage = br.getRegex("<p><a href=\"(http://.*?)\">Please click here to continue</a>").getMatch(0);
            if (continuePage != null) {
                if (downloadLink != null) {
                    downloadLink.setUrlDownload(continuePage);
                }
                br.getPage(continuePage);
            } else {
                break;
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        doFree(downloadLink, null);
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        AvailableStatus status = requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(QUICKDOWNLOAD)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
        }
        if (AvailableStatus.UNCHECKABLE.equals(status)) {
            final Form form = br.getFormbyActionRegex("limit-exceeded");
            if (form == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(form);
            status = requestFileInformation(downloadLink);
        }
        if (AvailableStatus.UNCHECKABLE.equals(status)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        br.setFollowRedirects(true);
        String dllink = checkDirectLink(downloadLink, "directlink_free");
        if (dllink == null) {
            boolean captcha_failed = false;
            /* 2019-05-15: New: Free download without captcha */
            dllink = br.getRegex("(/[^\"<>]+\\?do=slowDirectDownload[^\"<>]*?)\"").getMatch(0);
            if (dllink == null) {
                if (passwordProtected) {
                    handlePassword(downloadLink);
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                Browser cbr = null;
                captcha_failed = true;
                for (int i = 0; i <= 5; i++) {
                    if (i == 5) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    cbr = br.cloneBrowser();
                    cbr.getPage("/reloadXapca.php?rnd=" + System.currentTimeMillis());
                    if (cbr.getRequest().getHttpConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                    }
                    final String hash = PluginJSonUtils.getJsonValue(cbr, "hash");
                    final String timestamp = PluginJSonUtils.getJsonValue(cbr, "timestamp");
                    final String salt = PluginJSonUtils.getJsonValue(cbr, "salt");
                    String captchaUrl = PluginJSonUtils.getJsonValue(cbr, "image");
                    Form captchaForm = br.getFormbyProperty("id", "frm-downloadDialog-freeDownloadForm");
                    if (captchaForm == null) {
                        captchaForm = br.getFormbyProperty("id", "frm-download-freeDownloadTab-freeDownloadForm");
                    }
                    if (captchaForm == null || captchaUrl == null || hash == null || timestamp == null || salt == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String code = getCaptchaCode(captchaUrl, downloadLink);
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
                    this.sleep(3000, downloadLink);
                    br.submitForm(captchaForm);
                    // If captcha fails, throrotws exception
                    // If in automatic mode, clears saved data
                    if (br.containsHTML("\"errors\"\\s*:\\s*\\[\\s*\"(Error rewriting the text|Rewrite the text from the picture|Text je opsán špatně|An error ocurred while)") || br.containsHTML("\"new_captcha_data\"")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else if (br.containsHTML("\"errors\"\\s*:\\s*\\[\\s*\"Chyba při ověření uživatele")) {
                        /* 2019-05-15: This may also happen when user is not using an account */
                        if (account != null) {
                            synchronized (account) {
                                account.clearCookies("");
                            }
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        } else {
                            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                    }
                    dllink = PluginJSonUtils.getJsonValue(br, "url");
                    if (dllink == null) {
                        break;
                    }
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
                    if (!dl.getConnection().getContentType().contains("html") || dl.getConnection().isContentDisposition()) {
                        captcha_failed = false;
                        break;
                    } else {
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
                        dllink = handleDownloadUrl(downloadLink);
                        if (dllink != null) {
                            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
                            if (!dl.getConnection().getContentType().contains("html") || dl.getConnection().isContentDisposition()) {
                                captcha_failed = false;
                                break;
                            } else {
                                try {
                                    br.followConnection();
                                } catch (final IOException e) {
                                    logger.log(e);
                                }
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
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
        }
        if (dl.getConnection().getContentType().contains("html")) {
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
                synchronized (CTRLLOCK) {
                    totalMaxSimultanFreeDownload.set(Math.min(Math.max(1, maxFree.get() - 1), totalMaxSimultanFreeDownload.get()));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            logger.warning("The finallink doesn't seem to be a file: " + dllink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink_free", dl.getConnection().getURL().toString());
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
     * @author raztoki
     * @param downloadLink
     * @throws Exception
     */
    private void handlePassword(final DownloadLink downloadLink) throws Exception {
        final boolean ifr = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            String passCode = downloadLink.getDownloadPassword();
            if (StringUtils.isEmpty(passCode)) {
                passCode = getUserInput("Password?", downloadLink);
            }
            final boolean preferFormHandling = true;
            if (preferFormHandling) {
                /* 2016-12-07: Prefer this way to prevent failures due to wrong website language! */
                final Form pwform = br.getFormbyKey("password_send");
                if (pwform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                pwform.put("password", Encoding.urlEncode(passCode));
                br.submitForm(pwform);
            } else {
                br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode) + "&password_send=Send&do=passwordProtectedForm-submit");
            }
            if (br.toString().equals("No htmlCode read")) {
                // Benefit of statserv!
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (this.isPasswordProtected()) {
                // failure
                logger.info("Incorrect password was entered");
                downloadLink.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            } else if (!br.containsHTML(PREMIUMONLYUSERTEXT)) {
                logger.info("Correct password was entered");
                downloadLink.setDownloadPassword(passCode);
                return;
            } else {
                logger.info("Correct password was entered");
                downloadLink.setDownloadPassword(passCode);
                return;
            }
        } finally {
            br.setFollowRedirects(ifr);
        }
    }

    private boolean isPasswordProtected() {
        final boolean isPasswordProtectedAccordingToResponseCode = this.br.getHttpConnection() != null && this.br.getHttpConnection().getResponseCode() == 401;
        final boolean isPasswordProtectedAccordingToHTML = this.br.containsHTML("\"frm\\-passwordProtectedForm\\-password\"");
        return isPasswordProtectedAccordingToHTML || isPasswordProtectedAccordingToResponseCode;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.isOK() && (con.isContentDisposition() || (!con.getContentType().contains("html") && con.getLongContentLength() > 0))) {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            downloadLink.setProperty(property, Property.NULL);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        br = new Browser();
        login(account, false);
        if (account.getType() == AccountType.FREE) {
            /* Free Account */
            doFree(link, account);
        } else {
            /* Premium Account */
            requestFileInformation(link);
            String dllink = finalDirectDownloadURL;
            if (dllink == null) {
                if (link.getDownloadURL().matches(QUICKDOWNLOAD)) {
                    dllink = link.getDownloadURL();
                } else {
                    if (passwordProtected) {
                        handlePassword(link);
                    }
                    dllink = br.getURL() + "?do=directDownload";
                }
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                try {
                    br.followConnection();
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (br.containsHTML("Pro rychlé stažení") || br.containsHTML("You do not have  enough") || br.containsHTML("Nie masz wystarczającego")) {
                    throw new AccountUnavailableException("Not enough premium traffic available", 1 * 60 * 60 * 1000l);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
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

    private void loginAPI(Account account, final AccountInfo aa) throws Exception {
        synchronized (account) {
            try {
                final AccountInfo ai = aa != null ? aa : account.getAccountInfo();
                setBrowserExclusive();
                final Browser br = new Browser();
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

    private void checkGeoBlocked(Browser br, Account account) throws PluginException {
        if (StringUtils.containsIgnoreCase(br.getURL(), "/blocked")) {
            if (account != null) {
                throw new AccountUnavailableException("Geoblocked", 24 * 60 * 60 * 1000l);
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
                Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    this.br.getPage("https://" + account.getHoster());
                    checkGeoBlocked(br, account);
                    handleAgeRestrictedRedirects(null);
                    if (br.containsHTML("do=web-login")) {
                        cookies = null;
                    } else if (br.getCookie(this.br.getHost(), "permanentLogin2", Cookies.NOTDELETEDPATTERN) == null) {
                        cookies = null;
                    }
                }
                if (cookies == null) {
                    this.br.getPage("https://" + account.getHoster() + "/login");
                    checkGeoBlocked(br, account);
                    handleAgeRestrictedRedirects(null);
                    final Form loginform = br.getFormbyKey("username");
                    if (loginform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else if (loginform.hasInputFieldByName("remember")) {
                        loginform.remove("remember");
                    }
                    loginform.put("remember", "on");
                    loginform.put("username", Encoding.urlEncode(account.getUser()));
                    loginform.put("password", Encoding.urlEncode(account.getPass()));
                    br.submitForm(loginform);
                    if (br.getCookie(this.br.getHost(), "permanentLogin2", Cookies.NOTDELETEDPATTERN) == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
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
        final AccountInfo ai;
        if (use_login_api) {
            ai = fetchAccountInfoAPI(account);
        } else {
            ai = fetchAccountInfoWebsite(account);
        }
        return ai;
    }

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        loginAPI(account, ai);
        ai.setStatus("Premium Account");
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        String trafficleft = br.getRegex("<em>[^<]+</em>\\s*?\\(([^<>\"\\']+)\\)\\s*?</span>").getMatch(0);
        if (trafficleft == null) {
            trafficleft = br.getRegex("\"fi fi-user\">\\s*</i>\\s*<em>.*?</em>\\s*\\((.*?)\\)\\s*<").getMatch(0);
            if (trafficleft == null) {
                final String span = br.getRegex("<span\\s*class\\s*=\\s*\"t-header-username\">\\s*(.*?)\\s*</span>").getMatch(0);
                trafficleft = new Regex(span, ">\\s*\\(([0-9\\.,]+\\s*[BMTGK]+)\\)\\s*<").getMatch(0);
            }
        }
        ai.setTrafficRefill(false);
        if (trafficleft != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
        }
        if (ai.getTrafficLeft() > 0) {
            ai.setStatus("Premium Account");
            account.setType(AccountType.PREMIUM);
        } else {
            ai.setStatus("Free Account");
            account.setType(AccountType.FREE);
        }
        return ai;
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
    private void controlFree(final int num) {
        synchronized (CTRLLOCK) {
            logger.info("maxFree was = " + maxFree.get());
            maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
            logger.info("maxFree now = " + maxFree.get());
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