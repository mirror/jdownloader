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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uloz.to", "pornfile.cz" }, urls = { "https?://(?:www\\.)?(?:uloz\\.to|ulozto\\.sk|ulozto\\.cz|ulozto\\.net)/(?!soubory/)[\\!a-zA-Z0-9]+/[^\\?\\s]+", "https?://(?:www\\.)?pornfile\\.(?:cz|ulozto\\.net)/[\\!a-zA-Z0-9]+/[^\\?\\s]+" })
public class UlozTo extends PluginForHost {
    private boolean              passwordProtected            = false;
    private static final String  REPEAT_CAPTCHA               = "REPEAT_CAPTCHA";
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
    private static Object        ACCLOCK                      = new Object();

    public UlozTo(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
        this.enablePremium("http://www.uloz.to/kredit");
    }

    public void correctDownloadLink(final DownloadLink link) {
        // ulozto.net = the english version of the site
        link.setUrlDownload(link.getDownloadURL().replaceAll("(ulozto\\.sk|ulozto\\.cz|ulozto\\.net)", "uloz.to").replaceFirst("^http://", "https://"));
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

    @SuppressWarnings("deprecation")
    private void handleRedirect(final DownloadLink downloadLink) throws Exception {
        for (int i = 0; i <= i; i++) {
            String continuePage = br.getRegex("<p><a href=\"(http://.*?)\">Please click here to continue</a>").getMatch(0);
            if (continuePage != null) {
                downloadLink.setUrlDownload(continuePage);
                br.getPage(downloadLink.getDownloadURL());
            } else {
                break;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        doFree(downloadLink);
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink downloadLink) throws Exception {
        this.getPluginConfig().setProperty(REPEAT_CAPTCHA, false);
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
            if (passwordProtected) {
                handlePassword(downloadLink);
            }
            final Browser br2 = br.cloneBrowser();
            boolean failed = true;
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            Browser cbr = null;
            for (int i = 0; i <= 5; i++) {
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
                String code = null, ts = null, sign = null, cid = null;
                // Tries to read if property selected
                if (getPluginConfig().getBooleanProperty(REPEAT_CAPTCHA)) {
                    code = getPluginConfig().getStringProperty(CAPTCHA_TEXT);
                    ts = getPluginConfig().getStringProperty("ts");
                    sign = getPluginConfig().getStringProperty("cid");
                    cid = getPluginConfig().getStringProperty("sign");
                }
                // If property not selected or read failed (no data), asks to solve
                if (code == null) {
                    code = getCaptchaCode(captchaUrl, downloadLink);
                    final Matcher m = Pattern.compile("http://img\\.uloz\\.to/captcha/(\\d+)\\.png").matcher(captchaUrl);
                    if (m.find()) {
                        getPluginConfig().setProperty(CAPTCHA_TEXT, code);
                        getPluginConfig().setProperty("ts", new Regex(captchaForm.getHtmlCode(), "name=\"ts\" id=\"frmfreeDownloadForm\\-ts\" value=\"([^<>\"]*?)\"").getMatch(0));
                        getPluginConfig().setProperty("cid", new Regex(captchaForm.getHtmlCode(), "name=\"cid\" id=\"frmfreeDownloadForm\\-cid\" value=\"([^<>\"]*?)\"").getMatch(0));
                        getPluginConfig().setProperty("sign", new Regex(captchaForm.getHtmlCode(), "name=\"sign\" id=\"frmfreeDownloadForm\\-sign\" value=\"([^<>\"]*?)\"").getMatch(0));
                        getPluginConfig().setProperty(REPEAT_CAPTCHA, true);
                    }
                }
                // if something failed
                if (code == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                captchaForm.put("captcha_value", Encoding.urlEncode(code));
                captchaForm.remove(null);
                captchaForm.remove("freeDownload");
                if (ts != null) {
                    captchaForm.put("ts", ts);
                }
                if (cid != null) {
                    captchaForm.put("cid", cid);
                }
                if (sign != null) {
                    captchaForm.put("sign", sign);
                }
                captchaForm.put("timestamp", timestamp);
                captchaForm.put("salt", salt);
                captchaForm.put("hash", hash);
                br.submitForm(captchaForm);
                // If captcha fails, throrotws exception
                // If in automatic mode, clears saved data
                if (br.containsHTML("\"errors\":\\[\"(Error rewriting the text|Rewrite the text from the picture|Text je opsán špatně|An error ocurred while|Chyba při ověření uživatele, zkus to znovu)")) {
                    if (getPluginConfig().getBooleanProperty(REPEAT_CAPTCHA)) {
                        getPluginConfig().setProperty(CAPTCHA_ID, Property.NULL);
                        getPluginConfig().setProperty(CAPTCHA_TEXT, Property.NULL);
                        getPluginConfig().setProperty(REPEAT_CAPTCHA, false);
                        getPluginConfig().setProperty("ts", Property.NULL);
                        getPluginConfig().setProperty("cid", Property.NULL);
                        getPluginConfig().setProperty("sign", Property.NULL);
                    }
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                dllink = PluginJSonUtils.getJsonValue(br, "url");
                if (dllink == null) {
                    break;
                }
                URLConnectionAdapter con = null;
                try {
                    br2.setDebug(true);
                    con = br2.openGetConnection(dllink);
                    if (!con.getContentType().contains("html")) {
                        failed = false;
                        break;
                    } else {
                        br2.followConnection();
                        if (br2.containsHTML("Stránka nenalezena")) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        if (br2.containsHTML("dla_backend/uloz\\.to\\.overloaded\\.html")) {
                            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 10 * 60 * 1000l);
                        }
                        if (br2.containsHTML("Chyba při ověření uživatele, zkus to znovu|Nastala chyba při odeslání textu\\. Znovu opiš text z obrázku\\.")) {
                            /* \"errors\":\\[\"Chyba při ověření uživatele, zkus to znovu */
                            // Error in user authentication, try again
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        }
                        br.clearCookies("//ulozto.net/");
                        br.clearCookies("//uloz.to/");
                        dllink = handleDownloadUrl(downloadLink);
                        if (dllink != null) {
                            failed = false;
                            break;
                        }
                        continue;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.contains("/error404/?fid=file_not_found")) {
                logger.info("The user entered the correct captcha but this file is offline...");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (failed) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
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
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink_free", dllink);
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
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @SuppressWarnings("deprecation")
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        br = new Browser();
        if (account.getType() == AccountType.FREE) {
            /* Free Account */
            login(account, false);
            doFree(link);
        } else {
            /* Premium Account */
            login(account, false);
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
                if (br.containsHTML("Pro rychlé stažení") || br.containsHTML("You do not have  enough") || br.containsHTML("Nie masz wystarczającego")) {
                    throw new AccountRequiredException("Not enough premium traffic available");
                }
                try {
                    br.followConnection();
                } catch (final IOException e) {
                    logger.log(e);
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

    private void loginAPI(final Account account, final AccountInfo aa) throws Exception {
        synchronized (ACCLOCK) {
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
            } finally {
                setBasicAuthHeader(account);
            }
        }
    }

    private void loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (ACCLOCK) {
            try {
                setBrowserExclusive();
                br.setFollowRedirects(true);
                prepBR(this.br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                this.br.getPage("https://" + account.getHoster() + "/login");
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
                if (br.getCookie(this.br.getHost(), "permanentLogin2") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } finally {
                // setBasicAuthHeader(account);
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
        try {
            loginAPI(account, ai);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Premium Account");
        account.setType(AccountType.PREMIUM);
        account.setValid(true);
        return ai;
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            loginWebsite(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String trafficleft = br.getRegex("<em>[^<]+</em>\\s*?\\(([^<>\"\\']+)\\)\\s*?</span>").getMatch(0);
        if (trafficleft == null) {
            trafficleft = br.getRegex("\"fi fi-user\">\\s*</i>\\s*<em>.*?</em>\\s*\\((.*?)\\)\\s*<").getMatch(0);
        }
        if (trafficleft != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
        }
        ai.setStatus("Premium Account");
        account.setType(AccountType.PREMIUM);
        account.setValid(true);
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), UlozTo.REPEAT_CAPTCHA, "Solve captcha by replaying previous (disable to solve manually)").setDefaultValue(true));
    }
}