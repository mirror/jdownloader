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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "scribd.com" }, urls = { "https?://(?:www\\.)?(?:(?:de|ru|es)\\.)?scribd\\.com/(doc|document|book|embeds|read)/\\d+" })
public class ScribdCom extends PluginForHost {
    private final String        formats       = "formats";
    /** The list of server values displayed to the user */
    private final String[]      allFormats    = new String[] { "PDF", "TXT", "DOCX" };
    private static final String FORMAT_PPS    = "class=\"format_ext\">\\.PPS</span>";
    private static final String TYPE_DOCUMENT = ".+/(doc|document)/.+";
    private static final String TYPE_AUDIO    = ".+/audiobook/.+";
    private String              origurl       = null;
    private Map<String, Object> entries       = null;
    private int                 json_type     = 1;

    public ScribdCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.scribd.com");
        setConfigElements();
        /* 2019-08-12: Seems like this startintervall is not needed anymore */
        // this.setStartIntervall(5 * 1000l);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
    }

    public void correctDownloadLink(final DownloadLink link) {
        /* Forced https */
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceAll("https?://[^/]+/", "https://www.scribd.com/"));
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "/(\\d+)(?:/[^/]+)?$").getMatch(0);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, InterruptedException {
        prepFreeBrowser(this.br);
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean checkViaJson) throws IOException, PluginException, InterruptedException {
        // final boolean checkViaJson = !link.getPluginPatternMatcher().matches(TYPE_AUDIO);
        String title = null;
        String description = null;
        final String fid = this.getFID(link);
        boolean is_audiobook = false;
        if (checkViaJson) {
            /* This way we can only determine is_audiobook status via URL. */
            is_audiobook = link.getPluginPatternMatcher().matches(TYPE_AUDIO);
            origurl = link.getPluginPatternMatcher();
            // createCSRFTOKEN();
            br.postPage("https://de." + this.getHost() + "/read2/" + fid + "/access_token", "doc_id=" + fid);
            final String token = PluginJSonUtils.getJson(br, "response");
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("https://www." + this.getHost() + "/scepub/" + fid + "/book_metadata.json?token=" + token);
            title = PluginJSonUtils.getJson(br, "title");
        } else {
            int counter400 = 0;
            do {
                br.getPage(link.getPluginPatternMatcher());
                counter400++;
            } while (counter400 <= 5 && br.getHttpConnection().getResponseCode() == 400);
            if (br.getURL().contains("/removal/") || br.getURL().contains("/deleted/")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getResponseCode() == 400) {
                logger.info("Server returns error 400");
                return AvailableStatus.UNCHECKABLE;
            } else if (br.getHttpConnection().getResponseCode() == 410) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getResponseCode() == 500) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            origurl = br.getURL();
            boolean is_deleted = false;
            try {
                final String json1 = br.getRegex(">Scribd\\.current_doc\\s*?=\\s*?(\\{.*?\\})</script>").getMatch(0);
                final String json2 = br.getRegex("ReactDOM\\.render\\(React\\.createElement\\(Scribd\\.BookPage\\.Modules\\.Header, (\\{.*?), document\\.getElementById").getMatch(0);
                final String json3 = br.getRegex("<script type=\"application/json\" data-hypernova-key=\"doc_page\"[^>]+><[^\\{]*?(\\{[^<]*\\})[^\\}<]*</script>").getMatch(0);
                if (json1 != null) {
                    /* Type 1 */
                    json_type = 1;
                    entries = restoreFromString(json1, TypeRef.MAP);
                    is_audiobook = ((Boolean) entries.get("is_audiobook")).booleanValue();
                    title = (String) entries.get("title");
                    description = (String) entries.get("description");
                } else if (json2 != null) {
                    /* Type 2 */
                    json_type = 2;
                    entries = restoreFromString(json2, TypeRef.MAP);
                    is_audiobook = ((Boolean) entries.get("is_audiobook")).booleanValue();
                    title = (String) entries.get("document_title");
                    description = (String) entries.get("document_description");
                    is_deleted = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "document/deleted")).booleanValue();
                } else {
                    json_type = 3;
                    entries = restoreFromString(json3, TypeRef.MAP);
                    Map<String, Object> docmap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "page/word_document");
                    if (docmap == null) {
                        /* 2024-03-07 */
                        docmap = (Map<String, Object>) entries.get("wordDocument");
                    }
                    entries = docmap;
                    title = (String) entries.get("title");
                    // final boolean show_archive_paywall = ((Boolean) entries.get("show_archive_paywall")).booleanValue();
                }
                /* 2019-08-11: TODO: Find out what these are good for: 'secret_password' and 'access_key' */
            } catch (final Throwable e) {
            }
            if (is_deleted) {
                /* 2019-08-12: Rare case */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            title = new Regex(br.getURL(), "/" + fid + "/([^/]+)").getMatch(0); // url slug
            if (title == null) {
                title = fid;
            }
        }
        link.setName(Encoding.htmlDecode(title).trim() + "." + getExtension(is_audiobook));
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        /* saving session info can result in avoiding 400, 410 server errors */
        final HashMap<String, String> cookies = new HashMap<String, String>();
        final Cookies add = br.getCookies(this.getHost());
        for (final Cookie c : add.getCookies()) {
            cookies.put(c.getKey(), c.getValue());
        }
        synchronized (cookieMonster) {
            cookieMonster.set(cookies);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        final boolean fetchDataViaPurchaseHistory = true;
        String accountType = null;
        String accountPaymentType = null;
        String expiredateStr = null;
        /* The userID might be used in our crawler later */
        String userID = null;
        long expireTimestamp = 0;
        if (fetchDataViaPurchaseHistory) {
            /* 2019-08-12: Alternative way */
            /* First attempt - go through payment history and grab the latest entry + expire-date */
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            brc.getHeaders().put("sec-fetch-mode", "cors");
            brc.getHeaders().put("sec-fetch-site", "same-origin");
            brc.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            brc.getPage("/account-settings/payment-transactions");
            try {
                Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(brc.toString());
                final List<Object> ressourcelist = (List<Object>) entries.get("payment_transactions");
                for (final Object transactionO : ressourcelist) {
                    entries = (Map<String, Object>) transactionO;
                    final boolean refunded = ((Boolean) entries.get("refunded")).booleanValue();
                    final Object descriptionO = entries.get("description");
                    if (refunded || descriptionO == null) {
                        continue;
                    }
                    accountPaymentType = (String) entries.get("payment_method");
                    entries = (Map<String, Object>) descriptionO;
                    expiredateStr = (String) entries.get("valid_until");
                    if (!StringUtils.isEmpty(expiredateStr)) {
                        break;
                    }
                }
                /* E.g. "Gültig: 8/12/19 - 9/11/19" */
                final String[] createDataAndExpireDate = new Regex(expiredateStr, "(\\d{1,2}/\\d{1,2}/\\d{1,2})").getColumn(0);
                if (createDataAndExpireDate != null && createDataAndExpireDate.length >= 2) {
                    expiredateStr = createDataAndExpireDate[1];
                    expireTimestamp = TimeFormatter.getMilliSeconds(expiredateStr, "MM/dd/yy", Locale.ENGLISH);
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        } else {
            br.getPage("/account-settings/account");
            final String json_account = br.getRegex("ReactDOM\\.render\\(React\\.createElement\\(Scribd\\.AccountSettings\\.Show, (\\{.*?\\})\\), document\\.getElementById").getMatch(0);
            try {
                Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json_account);
                final long userIDLong = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "user/id"), 0);
                if (userIDLong > 0) {
                    userID = Long.toString(userIDLong);
                }
                entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "membership_info/plan_info");
                /* 2019-08-12: E.g. "cancelled_with_valid_to" */
                accountType = (String) entries.get("type");
                expiredateStr = (String) entries.get("valid_to_date");
                expireTimestamp = TimeFormatter.getMilliSeconds(expiredateStr, "yyyy-MM-dd HH:mm:ss ZZZ", Locale.ENGLISH);
            } catch (final Throwable e) {
            }
        }
        if (StringUtils.isEmpty(userID)) {
            /* Fallback */
            userID = br.getRegex("var _user_id\\s*?=\\s*?\"(\\d+)\";").getMatch(0);
        }
        String accountStatus = null;
        if (expireTimestamp > System.currentTimeMillis()) {
            account.setType(AccountType.PREMIUM);
            accountStatus = "Premium account";
            if ("cancelled_with_valid_to".equalsIgnoreCase(accountType)) {
                accountStatus += " [Subscription cancelled]";
            } else if (!StringUtils.isEmpty(accountType)) {
                /* TODO: 2019-08-12: Find out which other versions of "type" they have! */
                accountStatus += " [Active subscription]";
            }
            if (accountPaymentType != null) {
                accountStatus += " [Paid via " + accountPaymentType + "]";
            }
            ai.setValidUntil(expireTimestamp, br);
        } else {
            account.setType(AccountType.FREE);
            accountStatus = "Registered (Free) account";
        }
        ai.setStatus(accountStatus);
        ai.setUnlimitedTraffic();
        if (!StringUtils.isEmpty(userID)) {
            account.setProperty("userid", userID);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://support.scribd.com/forums/33939/entries/25459";
    }

    private String getExtension(final boolean is_audiobook) {
        /* Special cases first */
        if (br.containsHTML(FORMAT_PPS)) {
            return "pps";
        } else if (is_audiobook) {
            return "mp3";
        }
        /* Seems like we have a document so user-settings are allowed! */
        switch (getPluginConfig().getIntegerProperty(formats, -1)) {
        case 0:
            logger.fine("PDF format is configured");
            return "pdf";
        case 1:
            logger.fine("TXT format is configured");
            return "txt";
        case 2:
            logger.fine("DOCX format is configured");
            return "docx";
        default:
            logger.fine("No format is configured, returning PDF...");
            return "pdf";
        }
    }

    // private String getConfiguredReplacedServer(final String oldText) {
    // String newText = null;
    // if (oldText.equals("pdf")) {
    // newText = "\"pdf_download\":1";
    // } else if (oldText.equals("txt")) {
    // newText = "\"text_download\":1";
    // } else if (oldText.equals("docx")) {
    // newText = "\"word_download\":1";
    // }
    // return newText;
    // }
    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (link.getPluginPatternMatcher().matches(TYPE_DOCUMENT)) {
            /* Account required to be able to download anything */
            throw new AccountRequiredException();
        } else {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This item is not downloadable");
        }
    }

    public void handlePremium(final DownloadLink parameter, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(parameter, false);
        boolean is_downloadable = true;
        boolean is_downloadable_for_premium_users = false;
        boolean is_view_restricted_archive = false;
        boolean show_archive_paywall = false;
        boolean is_audiobook = false;
        try {
            switch (this.json_type) {
            case 1:
                is_audiobook = ((Boolean) entries.get("is_audiobook")).booleanValue();
                is_downloadable = ((Boolean) entries.get("is_downloadable")).booleanValue();
                is_downloadable_for_premium_users = ((Boolean) entries.get("downloadable_for_premium_users")).booleanValue();
                break;
            case 2:
                entries = (Map<String, Object>) entries.get("document");
                /* 2019-08-12: Unsure about that, there is also: word_download, text_download, pdf_download */
                is_downloadable = ((Boolean) entries.get("all_download")).booleanValue();
                final String document_type = (String) entries.get("document_type");
                /* E.g. "audiobook", "book" */
                if ("audiobook".equalsIgnoreCase(document_type)) {
                    is_audiobook = true;
                }
                break;
            case 3:
                is_downloadable = ((Boolean) entries.get("is_downloadable")).booleanValue();
                is_view_restricted_archive = ((Boolean) entries.get("is_view_restricted_archive")).booleanValue();
                show_archive_paywall = ((Boolean) entries.get("show_archive_paywall")).booleanValue();
                break;
            default:
                break;
            }
        } catch (final Throwable e) {
            logger.info("Possible json parsing issues, moving forward to download attempt anyways");
            e.printStackTrace();
        }
        // final boolean is_paid = ((Boolean) entries.get("is_paid")).booleanValue();
        /* 2019-08-11: TODO: Find out what 'is_credit_restricted' and 'is_paid' means */
        // final boolean is_credit_restricted = ((Boolean) entries.get("is_credit_restricted")).booleanValue();
        if (is_downloadable_for_premium_users && account.getType() != AccountType.PREMIUM) {
            throw new AccountRequiredException("Only downloadable for premium users");
        } else if (!is_downloadable) {
            /* 2019-08-11: Not downloadable at all (?!) */
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is not downloadable");
        } else if (is_view_restricted_archive && show_archive_paywall && account.getType() != AccountType.PREMIUM) {
            this.premiumonlyArchiveViewRestricted();
        } else if (is_audiobook) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Audiobooks cannot be downloaded yet!");
        }
        final String[] downloadInfo = getDllink(parameter, account, is_audiobook);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, downloadInfo[0], this.isResumeable(parameter, account), 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            /* Assume that our current account type = free and the file is not downloadable */
            throw new AccountRequiredException();
        }
        parameter.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            prepBRGeneral(br);
            final boolean allowCookieLoginOnly = true; // 2023-11-13
            final Cookies cookies = account.loadCookies("");
            final Cookies userCookies = account.loadUserCookies();
            if (userCookies == null && allowCookieLoginOnly) {
                showCookieLoginInfo();
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            if (cookies != null || userCookies != null) {
                if (userCookies != null) {
                    br.setCookies(userCookies);
                } else {
                    br.setCookies(cookies);
                }
                logger.info("Verifying login cookies");
                br.getPage("https://www." + this.getHost() + "/");
                if (isLoggedIN(br)) {
                    /* Cookie login successful --> Save cookie timestamp */
                    logger.info("Cookie login successful");
                    if (userCookies == null) {
                        account.saveCookies(br.getCookies(br.getHost()), "");
                    }
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    if (userCookies != null) {
                        /* Dead end */
                        logger.info("User Cookie login failed");
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
            }
            br.getPage("https://www." + this.getHost() + "/login");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            // br.getHeaders().put("origin", "https://de.scribd.com");
            // br.getHeaders().put("referer", "https://de.scribd.com/");
            final Form loginform = br.getFormbyKey("password");
            if (loginform != null) {
                /* 2023-11-13: New */
                final String rcSitekey = br.getRegex("data-captcha-sitekey=\"([^\"]+)").getMatch(0);
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                if (rcSitekey != null) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, rcSitekey) {
                        @Override
                        public TYPE getType() {
                            return TYPE.INVISIBLE;
                        }
                    }.getToken();
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                } else {
                    logger.warning("Failed to find rcSitekey -> Login will probably fail");
                }
                br.submitForm(loginform);
            } else {
                /* Old handling */
                final Map<String, Object> post = new HashMap<String, Object>();
                post.put("login_or_email", account.getUser());
                post.put("login_password", account.getPass());
                post.put("rememberme", "");
                post.put("signup_location", "");
                post.put("https://de.scribd.com/", "");
                post.put("login_params", new HashMap<String, Object>());
                if (br.containsHTML("\"reCaptchaEnabled\"\\s*:\\s*true")) {
                    final String rcKey = PluginJSonUtils.getJson(br, "reCaptchaSiteKey");
                    final String recaptchaV2Response;
                    if (!StringUtils.isEmpty(rcKey)) {
                        recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, rcKey).getToken();
                    } else {
                        recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    }
                    post.put("g-recaptcha-response", recaptchaV2Response);
                }
                br.getHeaders().put("Content-Type", "application/json");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPageRaw("/login", JSonStorage.serializeToJson(post));
            }
            /* E.g. success: {"login":true,"success":true,"user":{"id":123456789}} */
            final String loginstatus = PluginJSonUtils.getJson(br, "login");
            if (br.containsHTML("Invalid username or password") || !"true".equals(loginstatus) || !isLoggedinViaCookie(br)) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("/logout")) {
            return true;
        } else {
            return false;
        }
    }

    @Deprecated
    public static boolean isLoggedinViaCookie(final Browser br) {
        return br.getCookie(br.getHost(), "_scribd_session", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Deprecated
    public static String createCSRFTOKEN(final Browser br, final String host) throws IOException, PluginException {
        br.getPage("https://www." + host + "/csrf_token?href=http%3A%2F%2Fwww.scribd.com%2F");
        final String authenticity_token = PluginJSonUtils.getJson(br, "csrf_token");
        if (StringUtils.isEmpty(authenticity_token)) {
            // logger.warning("Failed to find authenticity_token");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return authenticity_token;
    }

    private String[] getDllink(final DownloadLink link, final Account account, final boolean is_audiobook) throws PluginException, IOException {
        final String userPreferredFormat = getExtension(is_audiobook);
        final String fileId = this.getFID(link);
        if (fileId == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // final String scribdsession = getSpecifiedCookie(this.br, "_scribd_session");
        // final String scribdexpire = getSpecifiedCookie(this.br, "_scribd_expire");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "*/*");
        // xmlbrowser.setCookie("http://" + this.getHost(), "_scribd_session", scribdsession);
        // xmlbrowser.setCookie("http://" + this.getHost(), "_scribd_expire", scribdexpire);
        // this.br.setCookie("http://" + this.getHost(), "_scribd_expire", scribdexpire);
        // this.br.setCookie("http://" + this.getHost(), "_scribd_expire", scribdexpire);
        // This will make it fail...
        // xmlbrowser.postPage("http://www.scribd.com/document_downloads/request_document_for_download", "id=" + fileId);
        // xmlbrowser.getHeaders().put("X-Tried-CSRF", "1");
        // xmlbrowser.getHeaders().put("X-CSRF-Token", getauthenticity_token(account));
        /* Seems like this is not needed anymore. */
        // xmlbrowser.postPage("/document_downloads/register_download_attempt", "doc_id=" + fileId +
        // "&next_screen=download_lightbox&source=read");
        /*
         * Check which formats are available for download. Try to download user preferred format. If not found, download first available
         * format in list.
         */
        String formatToDownload = null;
        br.getPage("https://www." + this.getHost() + "/doc-page/download-receipt-modal-props/" + fileId);
        Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        entries = (Map<String, Object>) entries.get("document");
        String firstAvailableFormat = null;
        final List<Object> ressourcelist = (List<Object>) entries.get("formats");
        String filesize = null;
        String firstFilesize = null;
        for (final Object typeO : ressourcelist) {
            entries = (Map<String, Object>) typeO;
            final String extensionTmp = (String) entries.get("extension");
            final String filesizeTmp = (String) entries.get("filesize");
            if (StringUtils.isEmpty(extensionTmp)) {
                continue;
            }
            if (firstAvailableFormat == null) {
                firstAvailableFormat = extensionTmp;
                firstFilesize = filesizeTmp;
            }
            if (firstAvailableFormat.equalsIgnoreCase(userPreferredFormat)) {
                logger.info("User preferred format is available: " + userPreferredFormat);
                formatToDownload = userPreferredFormat;
                filesize = filesizeTmp;
                break;
            }
        }
        if (firstAvailableFormat == null) {
            /* E.g. for free accounts, this will return an empty list of items */
            logger.info("Seems like not a single download is available --> This item is READ-ONLY");
            if (account != null) {
                if (link.getPluginPatternMatcher().matches(TYPE_DOCUMENT)) {
                    /*
                     * Should never happen - maybe we were logged-out or account is not premium but item is only downloadable for premium
                     * users ... or not downloadable at all for some reason.
                     */
                    throw new AccountRequiredException();
                } else {
                    /* E.g. not even website provides download-button. Maybe downloadable inside their own app (DRM protected). */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "This item is not downloadable");
                }
            } else {
                throw new AccountRequiredException();
            }
        }
        if (formatToDownload == null) {
            logger.info("User preferred format is not available - downloading first in the list: " + formatToDownload);
            formatToDownload = firstAvailableFormat;
            /* Set filesize so we got it set just in case our final download requests is returning an error. */
            if (!StringUtils.isEmpty(firstFilesize)) {
                link.setDownloadSize(SizeFormatter.getSize(firstFilesize));
            }
        } else {
            if (!StringUtils.isEmpty(filesize)) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        }
        String[] dlinfo = new String[2];
        dlinfo[1] = formatToDownload;
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(false);
        brc.getPage("/document_downloads/" + fileId + "?extension=" + dlinfo[1]);
        if (brc.containsHTML("Sorry, downloading this document in the requested format has been disallowed")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, dlinfo[1] + " format is not available for this file!");
        } else if (brc.containsHTML("You do not have access to download this document|Invalid document format")) {
            /* This will usually go along with response 403. */
            if (account != null) {
                logger.info("This file might not be downloadable at all");
            }
            throw new AccountRequiredException("This file can only be downloaded by premium users");
        }
        dlinfo[0] = brc.getRedirectLocation();
        if (dlinfo[0] == null) {
            /* 2020-07-20: */
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (brc.getRequest().getHtmlCode().length() <= 100) {
                /* 2020-07-20: E.g. errormessage: All download limits exceeded from your IP (123.123.123.123). */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Website-error: " + brc.getRequest().getHtmlCode());
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl");
            }
        }
        return dlinfo;
    }

    private void premiumonlyArchiveViewRestricted() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "You need to upload a document in order to be able to download this document");
    }

    /** Returns the most important account token */
    @Deprecated
    private static String getauthenticity_token(final Account acc) {
        return acc.getStringProperty("authenticity_token", null);
    }

    private String getSpecifiedCookie(final Browser brc, final String paramname) {
        ArrayList<String> sessionids = new ArrayList<String>();
        final HashMap<String, String> cookies = new HashMap<String, String>();
        final Cookies add = this.br.getCookies("http://" + this.getHost() + "/");
        for (final Cookie c : add.getCookies()) {
            cookies.put(c.getKey(), c.getValue());
        }
        if (cookies != null) {
            for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                final String key = cookieEntry.getKey();
                final String value = cookieEntry.getValue();
                if (key.equals(paramname)) {
                    sessionids.add(value);
                }
            }
        }
        final String finalID = sessionids.get(sessionids.size() - 1);
        return finalID;
    }

    private boolean                        coLoaded      = false;
    private static AtomicReference<Object> cookieMonster = new AtomicReference<Object>();

    @SuppressWarnings("unchecked")
    private Browser prepFreeBrowser(final Browser prepBR) {
        prepBRGeneral(prepBR);
        // loading previous cookie session results in less captchas
        synchronized (cookieMonster) {
            if (cookieMonster.get() != null && cookieMonster.get() instanceof Map<?, ?>) {
                final Map<String, String> cookies = (Map<String, String>) cookieMonster.get();
                for (Map.Entry<String, String> entry : cookies.entrySet()) {
                    prepBR.setCookie(this.getHost(), entry.getKey(), entry.getValue());
                }
                coLoaded = true;
            }
        }
        return prepBR;
    }

    private static void prepBRGeneral(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0");
        br.setAllowedResponseCodes(new int[] { 400, 410, 500 });
        br.setLoadLimit(br.getLoadLimit() * 5);
        br.setCookie("https://www.scribd.com/", "lang", "en");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), formats, allFormats, "Preferably download files in this format:").setDefaultValue(0));
    }
}