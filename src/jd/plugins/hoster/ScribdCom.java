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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "scribd.com" }, urls = { "https?://(?:www\\.)?(?:(?:de|ru|es)\\.)?scribd\\.com/(doc|document|book|embeds|read)/\\d+" })
public class ScribdCom extends PluginForHost {
    private final String                  formats    = "formats";
    /** The list of server values displayed to the user */
    private final String[]                allFormats = new String[] { "PDF", "TXT", "DOCX" };
    private static final String           FORMAT_PPS = "class=\"format_ext\">\\.PPS</span>";
    private static final String           TYPE_AUDIO = ".+/audiobook/.+";
    private String                        origurl    = null;
    private LinkedHashMap<String, Object> entries    = null;
    private int                           json_type  = 1;

    public ScribdCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.scribd.com");
        setConfigElements();
        /* 2019-08-12: Seems like this startintervall is not needed anymore */
        // this.setStartIntervall(5 * 1000l);
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, InterruptedException {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean checkViaJson) throws IOException, PluginException, InterruptedException {
        // this.setBrowserExclusive();
        br.setFollowRedirects(false);
        prepFreeBrowser(this.br);
        // final boolean checkViaJson = !link.getPluginPatternMatcher().matches(TYPE_AUDIO);
        String filename = null;
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
            br.setFollowRedirects(true);
            br.getPage("https://www." + this.getHost() + "/scepub/" + fid + "/book_metadata.json?token=" + token);
            filename = PluginJSonUtils.getJson(br, "title");
        } else {
            int counter400 = 0;
            do {
                br.getPage(link.getPluginPatternMatcher());
                counter400++;
            } while (counter400 <= 5 && br.getHttpConnection().getResponseCode() == 400);
            for (int i = 0; i <= 5; i++) {
                String newurl = br.getRedirectLocation();
                if (newurl != null) {
                    if (newurl.contains("/removal/") || newurl.contains("/deleted/")) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    link.setPluginPatternMatcher(newurl);
                    br.getPage(link.getPluginPatternMatcher());
                } else {
                    break;
                }
            }
            if (br.getHttpConnection().getResponseCode() == 400) {
                logger.info("Server returns error 400");
                return AvailableStatus.UNCHECKABLE;
            } else if (br.getHttpConnection().getResponseCode() == 410) {
                logger.info("Server returns error 410");
                return AvailableStatus.UNCHECKABLE;
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
                final String json3 = br.getRegex("<script type=\"application/json\" data-hypernova-key=\"doc_page\"[^>]+><[^\\{]*?(\\{.*?)</script>").getMatch(0);
                if (json1 != null) {
                    /* Type 1 */
                    json_type = 1;
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json1);
                    is_audiobook = ((Boolean) entries.get("is_audiobook")).booleanValue();
                    filename = (String) entries.get("title");
                    description = (String) entries.get("description");
                } else if (json2 != null) {
                    /* Type 2 */
                    json_type = 2;
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json2);
                    is_audiobook = ((Boolean) entries.get("is_audiobook")).booleanValue();
                    filename = (String) entries.get("document_title");
                    description = (String) entries.get("document_description");
                    is_deleted = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "document/deleted")).booleanValue();
                } else {
                    json_type = 3;
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json3);
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "page/word_document");
                    filename = (String) entries.get("title");
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
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = fid;
        }
        link.setName(Encoding.htmlDecode(filename).trim() + "." + getExtension(is_audiobook));
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
        login(this.br, account, true);
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
            brc.getHeaders().put("x-requested-with", "XMLHttpRequest");
            brc.getHeaders().put("sec-fetch-mode", "cors");
            brc.getHeaders().put("sec-fetch-site", "same-origin");
            brc.getHeaders().put("accept", "application/json, text/javascript, */*; q=0.01");
            brc.getPage("/account-settings/payment-transactions");
            try {
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(brc.toString());
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("payment_transactions");
                for (final Object transactionO : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) transactionO;
                    final boolean refunded = ((Boolean) entries.get("refunded")).booleanValue();
                    final Object descriptionO = entries.get("description");
                    if (refunded || descriptionO == null) {
                        continue;
                    }
                    accountPaymentType = (String) entries.get("payment_method");
                    entries = (LinkedHashMap<String, Object>) descriptionO;
                    expiredateStr = (String) entries.get("valid_until");
                    if (!StringUtils.isEmpty(expiredateStr)) {
                        break;
                    }
                }
                /* E.g. "Gültig: 8/12/19 - 9/11/19" */
                final String[] createDataAndExpireDate = new Regex(expiredateStr, "(\\d{1,2}/\\d{1,2}/\\d{1,2})").getColumn(0);
                if (createDataAndExpireDate.length >= 2) {
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
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_account);
                final long userIDLong = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "user/id"), 0);
                if (userIDLong > 0) {
                    userID = Long.toString(userIDLong);
                }
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "membership_info/plan_info");
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /* Account required */
        throw new AccountRequiredException();
    }

    public void handlePremium(final DownloadLink parameter, final Account account) throws Exception {
        login(this.br, account, false);
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
                entries = (LinkedHashMap<String, Object>) entries.get("document");
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
        } else if (is_view_restricted_archive && show_archive_paywall) {
            this.premiumonlyArchiveViewRestricted();
        } else if (is_audiobook) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Audiobooks cannot be downloaded yet!");
        }
        final String[] downloadInfo = getDllink(parameter, account, is_audiobook);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, downloadInfo[0], false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            /* Assume that our current account type = free and the file is not downloadable */
            throw new AccountRequiredException();
        }
        parameter.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBRGeneral(br);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean isLoggedin = false;
                String authenticity_token = null;
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    authenticity_token = getauthenticity_token(account);
                    br.getPage("https://www." + account.getHoster() + "/");
                    isLoggedin = isLoggedin(br);
                }
                if (!isLoggedin) {
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    authenticity_token = createCSRFTOKEN(br, account.getHoster());
                    br.postPage("/login", "authenticity_token=" + authenticity_token + "&login_params%5Bnext_url%5D=&login_params%5Bcontext%5D=join2&form_name=login_lb_form_login_lb&login_or_email=" + Encoding.urlEncode(account.getUser()) + "&login_password=" + Encoding.urlEncode(account.getPass()));
                    final String loginstatus = PluginJSonUtils.getJson(br, "login");
                    if (br.containsHTML("Invalid username or password") || !"true".equals(loginstatus) || !isLoggedin(br)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
                account.setProperty("authenticity_token", authenticity_token);
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                    account.removeProperty("authenticity_token");
                }
                throw e;
            }
        }
    }

    public static boolean isLoggedin(final Browser br) {
        return br.getCookie(br.getHost(), "_scribd_session", Cookies.NOTDELETEDPATTERN) != null;
    }

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
        br.getPage(origurl);
        if (this.br.getHttpConnection().getResponseCode() == 400) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 400");
        }
        String[] dlinfo = new String[2];
        dlinfo[1] = getExtension(is_audiobook);
        final String fileId = this.getFID(link);
        if (fileId == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        Browser xmlbrowser = br.cloneBrowser();
        xmlbrowser.setFollowRedirects(false);
        xmlbrowser.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        xmlbrowser.getHeaders().put("Accept", "*/*");
        // This will make it fail...
        // xmlbrowser.postPage("http://www.scribd.com/document_downloads/request_document_for_download", "id=" + fileId);
        final String correctedXML = xmlbrowser.toString().replace("\\", "");
        // Check if the selected format is available
        if (correctedXML.contains("premium: true")) {
            throw new AccountRequiredException();
        }
        xmlbrowser.getHeaders().put("X-Tried-CSRF", "1");
        xmlbrowser.getHeaders().put("X-CSRF-Token", getauthenticity_token(account));
        /* Seems like this is not needed anymore. */
        // xmlbrowser.postPage("/document_downloads/register_download_attempt", "doc_id=" + fileId +
        // "&next_screen=download_lightbox&source=read");
        dlinfo[0] = "https://de." + this.getHost() + "/document_downloads/" + fileId + "?extension=" + dlinfo[1];
        xmlbrowser = new Browser();
        final String scribdsession = getSpecifiedCookie(this.br, "_scribd_session");
        final String scribdexpire = getSpecifiedCookie(this.br, "_scribd_expire");
        xmlbrowser.setCookie("http://" + this.getHost(), "_scribd_session", scribdsession);
        xmlbrowser.setCookie("http://" + this.getHost(), "_scribd_expire", scribdexpire);
        this.br.setCookie("http://" + this.getHost(), "_scribd_expire", scribdexpire);
        this.br.setCookie("http://" + this.getHost(), "_scribd_expire", scribdexpire);
        xmlbrowser.getPage(dlinfo[0]);
        if (xmlbrowser.containsHTML("Sorry, downloading this document in the requested format has been disallowed")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, dlinfo[1] + " format is not available for this file!");
        }
        if (xmlbrowser.containsHTML("You do not have access to download this document|Invalid document format")) {
            throw new AccountRequiredException("This file can only be downloaded by premium users");
        }
        dlinfo[0] = xmlbrowser.getRedirectLocation();
        if (dlinfo[0] == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dlinfo;
    }

    private void premiumonlyArchiveViewRestricted() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "You need to upload a document in order to be able to download this document");
    }

    /** Returns the most important account token */
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
            if (cookieMonster.get() != null && cookieMonster.get() instanceof HashMap<?, ?>) {
                final HashMap<String, String> cookies = (HashMap<String, String>) cookieMonster.get();
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), formats, allFormats, JDL.L("plugins.host.ScribdCom.formats", "Download files in this format:")).setDefaultValue(0));
    }
}