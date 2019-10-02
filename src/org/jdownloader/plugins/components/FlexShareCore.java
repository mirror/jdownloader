package org.jdownloader.plugins.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

public class FlexShareCore extends antiDDoSForHost {
    public FlexShareCore(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://" + this.getHost() + "/get-premium.php");
    }

    @Override
    public String getAGBLink() {
        return "https://" + this.getHost() + "/help/terms.php";
    }

    // @Override
    // public void correctDownloadLink(final DownloadLink link) {
    // link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("https://", "http://").replace("/get/", "/files/"));
    // }
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
        return new Regex(link.getPluginPatternMatcher(), "/(?:files|get)/([A-Za-z0-9]+)").getMatch(0);
    }

    public static final String getDefaultAnnotationPatternPart() {
        return "/(?:files|get)/[A-Za-z0-9]+";
    }

    public static String[] buildAnnotationUrls(List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:[A-Za-z0-9]+\\.)?" + buildHostsPatternPart(domains) + FlexShareCore.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Can be found in account under: '/members/account.php'. Docs are usually here: '/api/docs.php'. Example website with working API:
     * filepup.net </br> The presence of an APIKey does not necessarily mean that the API or that filehost will work! Usually if it does
     * still not work, it will just return 404. Override this to use API.
     */
    protected String getAPIKey() {
        return null;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    /**
     * Returns how many max. chunks per file are allowed for current download mode based on account availability and account type. <br />
     * Override this function to set chunks settings!
     */
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    // Using FlexShareScript 1.2.1, heavily modified
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        if (getAPIKey() != null) {
            return requestFileInformationAPI(link);
        } else {
            return requestFileInformationWebsite(link);
        }
    }

    public AvailableStatus requestFileInformationAPI(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        getPage(getMainPage() + "/api/info.php?api_key=" + getAPIKey() + "&file_id=" + getFID(link));
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("file does not exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("\\[file_name\\] => (.*?)\n").getMatch(0);
        final String filesize = br.getRegex("\\[file_size\\] => (\\d+)\n").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Set final filename here because some hosters tag their files */
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    /** 2019-10-02: This will e.g. work for extmatrix.com. Will need to be tested with other filehosts! */
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">The file you have requested does not exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex fileInfo = br.getRegex("style=\"text-align:(center|left|right);\">(Premium Only\\!)?([^\"<>]+) \\(([0-9\\.]+ [A-Za-z]+)(\\))?(,[^<>\"/]+)?</h1>");
        String filename = fileInfo.getMatch(2);
        String filesize = fileInfo.getMatch(3);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Set final filename here because hoster taggs files
        link.setFinalFileName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (getAPIKey() != null) {
            getPage(link.getPluginPatternMatcher());
        }
        doFree(link, null);
    }

    protected void doFree(final DownloadLink link, Account account) throws Exception, PluginException {
        if (br.containsHTML("(>Premium Only\\!|you have requested require a premium account for download\\.<)")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        /** 2019-10-02: TODO: Add handling to re-use generated directurls */
        final String getLink = getLink();
        if (getLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // waittime
        final String waitStr = br.getRegex("var time\\s*=\\s*(\\d+);").getMatch(0);
        if (waitStr != null) {
            final int wait = Integer.parseInt(waitStr);
            if (wait > 240) {
                // 10 Minutes reconnect-waittime is not enough, let's wait one
                // hour
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            }
            // Short pre-download-waittime can be skipped
            // sleep(tt * 1001l, downloadLink);
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(getLink);
            if (con.getContentType().contains("html")) {
                br.followConnection();
                /* 2019-10-02: Tested with filepup.net, they do not have a download captcha! */
                final String action = getLink();
                final Form dlform = br.getFormbyProperty("name", "pipi");
                if (dlform == null || action == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (dlform.getAction() == null) {
                    dlform.setAction(action);
                }
                if (br.containsHTML("g-recaptcha-response")) {
                    final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
                    final String recaptchaV2Response = rc2.getToken();
                    dlform.put("g-recaptcha-response", recaptchaV2Response);
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlform, isResumeable(link, account), getMaxChunks(account));
            } else {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, getLink, isResumeable(link, account), getMaxChunks(account));
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (dl.getConnection().getContentType().contains("html")) {
            handleGeneralServerErrors();
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    protected void handleGeneralServerErrors() throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 10 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 10 * 60 * 1000l);
        }
    }

    protected void handleErrors() throws PluginException {
        if (br.containsHTML("Server is too busy for free users")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots", 10 * 60 * 1000l);
        }
        if (br.containsHTML("(files per hour for free users\\.</div>|>Los usuarios de Cuenta Gratis pueden descargar|hours for free users\\.|var time =)")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
        }
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/|\"g-recaptcha\")")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        final String unknownError = br.getRegex("class=\"error\">(.*?)\"").getMatch(0);
        if (unknownError != null) {
            logger.warning("Unknown error occured: " + unknownError);
        }
    }

    private static final String PREMIUMLIMIT = "out of 1024\\.00 TB</td>";
    private static final String PREMIUMTEXT  = "Account type:</td>\\s*<td><b>Premium</b>";

    protected void login(final Account account, boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(false);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedIN = false;
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    getPage(getMainPage() + "/members/myfiles.php");
                    loggedIN = isLoggedIN();
                    if (loggedIN) {
                        logger.info("Successfully loggedin via cookies");
                    }
                }
                if (!loggedIN) {
                    br.clearCookies(getHost());
                    logger.info("Performing full login");
                    getPage(getLoginURL());
                    final Form loginform = getAndFillLoginForm(account);
                    if (loginform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String captchaURL = new Regex(loginform.getHtmlCode(), "(/captcha\\.php[^\"\\']+)").getMatch(0);
                    if (captchaURL != null) {
                        final DownloadLink dlinkbefore = this.getDownloadLink();
                        final DownloadLink captchaLink;
                        if (dlinkbefore != null) {
                            captchaLink = dlinkbefore;
                        } else {
                            captchaLink = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                            this.setDownloadLink(captchaLink);
                        }
                        try {
                            final String code = this.getCaptchaCode(captchaURL, captchaLink);
                            loginform.put("captcha", Encoding.urlEncode(code));
                        } finally {
                            if (dlinkbefore != null) {
                                this.setDownloadLink(dlinkbefore);
                            }
                        }
                    }
                    submitForm(loginform);
                    /* Workaround for wrong URL after-login-redirect */
                    if (br.getHttpConnection().getResponseCode() == 404) {
                        getPage("/members/myfiles.php");
                    }
                    if (!isLoggedIN()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(br.getURL()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    /** Returns the full URL to the page which should contain the loginForm. */
    protected String getLoginURL() {
        return getMainPage() + "/login.php";
    }

    protected Form getAndFillLoginForm(final Account account) {
        /* Default handling - works e.g. for extmatrix.com */
        Form loginform = br.getFormbyActionRegex(".*login\\.php");
        if (loginform == null) {
            loginform = br.getFormByInputFieldKeyValue("task", "dologin");
        }
        if (loginform == null) {
            loginform = br.getFormbyKey("user");
        }
        if (loginform == null) {
            return null;
        }
        loginform.put("user", Encoding.urlEncode(account.getUser()));
        loginform.put("pass", Encoding.urlEncode(account.getPass()));
        return loginform;
    }

    protected boolean isLoggedIN() {
        return br.getCookie(br.getHost(), "auth", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (!br.containsHTML(PREMIUMTEXT) && (br.getURL() == null || !br.getURL().contains("/members/myfiles.php"))) {
            getPage("/members/myfiles.php");
            if (!br.containsHTML(PREMIUMLIMIT)) {
                account.setType(AccountType.FREE);
            } else {
                account.setType(AccountType.PREMIUM);
            }
        }
        String hostedFiles = br.getRegex("<td>Files Hosted:</td>[\t\r\n ]+<td>(\\d+)</td>").getMatch(0);
        if (hostedFiles != null) {
            ai.setFilesNum(Integer.parseInt(hostedFiles));
        }
        final String space_used = br.getRegex("<td>Spaced Used:</td>\\s*<td>\\s*(\\d+(?:\\.\\d+{1,2})? [A-Za-z]{2,5}) ").getMatch(0);
        if (space_used != null) {
            ai.setUsedSpace(space_used.trim());
        }
        ai.setUnlimitedTraffic();
        if (AccountType.FREE.equals(account.getType())) {
            // free accounts can still have captcha.
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
        } else {
            account.setMaxSimultanDownloads(20);
            account.setConcurrentUsePossible(true);
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
            getPage(getMainPage());
            final String validUntil = br.getRegex("Premium End:</td>\\s+<td>([^<>]*?)</td>").getMatch(0);
            if (validUntil != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "dd-MM-yyyy", Locale.ENGLISH));
            }
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        getPage(link.getPluginPatternMatcher());
        if (AccountType.FREE.equals(account.getType())) {
            doFree(link, account);
        } else {
            String getLink = br.getRedirectLocation();
            if (getLink != null && getLink.matches("https?://(?:www\\.)?" + Pattern.quote(this.getHost()) + "/get/.*?")) {
                getPage(getLink);
                getLink = br.getRedirectLocation();
            }
            if (getLink == null) {
                getLink = br.getRegex("<a id='jd_support' href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            }
            if (getLink == null) {
                getLink = getLink();
            }
            if (getLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, getLink, "task=download", isResumeable(link, account), this.getMaxChunks(account));
            if (dl.getConnection().getContentType().contains("html")) {
                handleGeneralServerErrors();
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private String getLink() {
        String getLink = br.getRegex("disabled=\"disabled\" onclick=\"document\\.location='(https?.*?)';\"").getMatch(0);
        if (getLink == null) {
            getLink = br.getRegex("('|\")(" + "https?://(www\\.)?([a-z0-9]+\\.)?" + Pattern.quote(this.getHost()) + "/get/[A-Za-z0-9]+/\\d+/[^<>\"/]+)\\1").getMatch(1);
        }
        return getLink;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (AccountType.FREE.equals(acc.getType())) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    /**
     * @return true: Website supports https and plugin will prefer https. <br />
     *         false: Website does not support https - plugin will avoid https. <br />
     *         default: true </br> Example which supports https: extmatrix.com </br> Example which does NOT support https: filepup.net
     */
    protected boolean supports_https() {
        return true;
    }

    /**
     * A correct setting increases linkcheck-speed as unnecessary redirects will be avoided. <br />
     *
     * @return true: Implies that website requires 'www.' in all URLs. <br />
     *         false: Implies that website does NOT require 'www.' in all URLs. <br />
     *         default: true
     */
    protected boolean requires_WWW() {
        return true;
    }

    /** Returns https?://host.tld */
    protected String getMainPage() {
        final String host;
        final String browser_host = this.br != null ? br.getHost() : null;
        final String[] hosts = this.siteSupportedNames();
        if (browser_host != null) {
            host = browser_host;
        } else {
            /* 2019-07-25: This may not be correct out of the box e.g. for imgmaze.com */
            host = hosts[0];
        }
        String mainpage;
        final String protocol;
        if (this.supports_https()) {
            protocol = "https://";
        } else {
            protocol = "http://";
        }
        mainpage = protocol;
        if (requires_WWW()) {
            mainpage += "www.";
        }
        mainpage += host;
        return mainpage;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.FlexShare;
    }
}