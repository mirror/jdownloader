package org.jdownloader.plugins.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.appwork.utils.DebugMode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class FlexShareCore extends antiDDoSForHost {
    public FlexShareCore(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://" + this.getHost() + "/get-premium.php");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://" + this.getHost() + "/help/terms.php";
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

    public static final String getDefaultAnnotationPatternPart() {
        return "/(?:files|get)/([A-Za-z0-9]+)(/([^/]+)\\.html)?";
    }

    public static String[] buildAnnotationUrls(List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:[A-Za-z0-9]+\\.)?" + buildHostsPatternPart(domains) + FlexShareCore.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    private String getContentURL(final DownloadLink link) {
        return link.getPluginPatternMatcher().replace("http://", "https://").replaceFirst("/get/", "/files/");
    }

    /**
     * Can be found in account under: '/members/account.php'. Docs are usually here: '/api/docs.php'. Example website with working API:
     * filepup.net </br>
     * The presence of an APIKey does not necessarily mean that the API or that filehost will work! Usually if it does still not work, it
     * will just return 404. Override this to use API.
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
        /*
         * * 2023-07-06: extmatrix.com: Some files are displayed as offline when accessing them as a free/non-account user thus let's try to
         * always provide an account.
         */
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, acc);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (!link.isNameSet()) {
            final String filenameFromURL = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
            if (filenameFromURL != null) {
                link.setName(filenameFromURL);
            } else {
                link.setName(this.getFID(link));
            }
        }
        this.setBrowserExclusive();
        if (getAPIKey() != null) {
            return requestFileInformationAPI(link);
        } else {
            return requestFileInformationWebsite(link, account);
        }
    }

    public AvailableStatus requestFileInformationAPI(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        getPage(getMainPage() + "/api/info.php?api_key=" + getAPIKey() + "&file_id=" + getFID(link));
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)file does not exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("\\[file_name\\] => (.*?)\n").getMatch(0);
        final String filesize = br.getRegex("\\[file_size\\] => (\\d+)\n").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Set final filename here because some hosters tag their files */
        link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    /** 2019-10-02: This will e.g. work for extmatrix.com. Will need to be tested with other filehosts! */
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        if (account != null) {
            this.login(account, null, false);
        }
        final URLConnectionAdapter con = br.openGetConnection(this.getContentURL(link));
        if (this.looksLikeDownloadableContent(con)) {
            /* Directurl or we're logged in a premium account with direct downloads enabled. */
            link.setFinalFileName(Plugin.getFileNameFromHeader(con));
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
        } else {
            br.followConnection();
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*The file you have requested does not exist")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = br.getRegex("(?i)<title>\\s*([^<]+)\\s*\\|\\s*ExtMatrix\\s*-\\s*The Premium Cloud Storage\\s*</title>").getMatch(0);
            final Regex fileInfo = br.getRegex("style=\"text-align:(center|left|right);\">\\s*(Premium Only\\!)?([^\"<>]+) \\(([0-9\\.]+ [A-Za-z]+)(\\))?(,[^<>\"/]+)?</h1>");
            if (filename == null) {
                filename = fileInfo.getMatch(2);
            }
            String filesize = fileInfo.getMatch(3);
            /* Set final filename here because hoster is tagging filenames that are given via Content-Disposition header. */
            if (filename != null) {
                filename = Encoding.htmlDecode(filename).trim();
                /*
                 * 2023-07-14: Premium users may get another representation of the file information html website -> Remove stuff we don't
                 * need.
                 */
                filename = filename.replaceFirst("^(?i)Download \\|\\s*", "");
                link.setFinalFileName(filename);
            }
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    protected void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformationWebsite(link, account);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, this.getContentURL(link), isResumeable(link, account), this.getMaxChunks(account));
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            boolean isSpecialJDLink = false;
            String getLink = br.getRegex("<a\\s*id\\s*=\\s*'jd_support'\\s*href\\s*=\\s*\"(https?://[^<>\"\\']*?)\"").getMatch(0);
            if (getLink != null) {
                isSpecialJDLink = true;
            } else {
                getLink = findFreeGetLink(br);
            }
            Form dlform = br.getFormbyProperty("name", "pipi");
            if (dlform == null) {
                /* 2020-12-07: extmatrix.com */
                dlform = br.getFormByInputFieldKeyValue("task", "download");
            }
            if (dlform == null && getLink == null) {
                handleErrors(link, account);
                handleGeneralServerErrors(br.getHttpConnection());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Pre download wait */
            final String waitStr = br.getRegex("var time\\s*=\\s*(\\d+);").getMatch(0);
            if (waitStr != null) {
                final int wait = Integer.parseInt(waitStr);
                if (wait > 240) {
                    /* 10 Minutes reconnect-waittime is not enough, let's wait one hour */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
                }
                // Short pre-download-waittime can be skipped
                final boolean skipShortPreDownloadWaittime = true;
                if (!skipShortPreDownloadWaittime) {
                    sleep(wait * 1001l, link);
                }
            }
            if (dlform != null) {
                // POST-request (Form)
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlform, isResumeable(link, account), this.getMaxChunks(account));
            } else if (isSpecialJDLink) {
                // GET-request
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, getLink, isResumeable(link, account), this.getMaxChunks(account));
            } else {
                // POST-request
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, getLink, "task=download", isResumeable(link, account), this.getMaxChunks(account));
            }
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection(true);
                handleErrors(link, account);
                handleGeneralServerErrors(dl.getConnection());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    protected void handleGeneralServerErrors(final URLConnectionAdapter con) throws PluginException {
        if (con.getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
        } else if (con.getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
        } else if (con.getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 5 * 60 * 1000l);
        }
    }

    protected void handleErrors(final DownloadLink link, final Account account) throws PluginException {
        if (br.containsHTML("(?i)(>\\s*Premium Only\\s*\\!|you have requested require a premium account for download\\.\\s*<|you have requested require a premium account for download|>\\s*Only premium accounts are able to download this file)")) {
            throw new AccountRequiredException();
        } else if (br.containsHTML("(?i)<title>\\s*Site Maintenance\\s*</title>")) {
            if (dl != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server Maintenance", 30 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server Maintenance", 30 * 60 * 1000l);
            }
        } else if (br.containsHTML("Server is too busy for free users")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots", 10 * 60 * 1000l);
        } else if (br.containsHTML("(files per hour for free users\\.</div>|>Los usuarios de Cuenta Gratis pueden descargar|hours for free users\\.|var time =)")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
        } else if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/|\"g-recaptcha\")")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (br.getURL().contains("error_503.html")) {
            /* 2020-11-17: E.g.: http://www.filepup.net/err/error_503.html */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 503 too many connections", 1 * 60 * 1000l);
        }
        final String unknownError = br.getRegex("class\\s*=\\s*\"error\"\\s*>\\s*(.*?)\"").getMatch(0);
        if (unknownError != null) {
            logger.warning("Unknown error occured: " + unknownError);
        }
    }

    protected void login(final Account account, final AccountInfo ai, boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(false);
                final Cookies cookies = account.loadCookies("");
                boolean loggedIN = false;
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!validateCookies) {
                        /* Do not validate cookies */
                        return;
                    }
                    getPage(getMainPage() + "/members/myfiles.php");
                    if (isLoggedIN(br)) {
                        updateAccountType(br, account, ai);
                        logger.info("Successfully loggedin via cookies");
                        loggedIN = true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                if (!loggedIN) {
                    br.clearCookies(null);
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
                    if (!isLoggedIN(br)) {
                        throw new AccountInvalidException();
                    }
                    updateAccountType(br, account, ai);
                    logger.info("Successfully loggedin:" + account.getType());
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
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

    protected boolean isLoggedIN(final Browser br) {
        if (br.getCookie(br.getHost(), "auth", Cookies.NOTDELETEDPATTERN) != null && br.containsHTML("(?i)/logout\\.php")) {
            return true;
        } else {
            return false;
        }
    }

    protected void updateAccountType(final Browser br, final Account account, AccountInfo ai) throws Exception {
        if (br.getURL() == null || !br._getURL().getPath().matches("^/?$")) {
            getPage(getMainPage());
        }
        synchronized (account) {
            if (ai == null) {
                ai = account.getAccountInfo();
                if (ai == null) {
                    ai = new AccountInfo();
                }
            }
            ai.setUnlimitedTraffic();
            final String registeredDateStr = br.getRegex("(?i)Registered\\s*:\\s*</td>\\s+<td>([^<>]*?)</td>").getMatch(0);
            if (registeredDateStr != null) {
                if (registeredDateStr.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    ai.setCreateTime(TimeFormatter.getMilliSeconds(registeredDateStr, "dd-MM-yyyy", Locale.ENGLISH));
                } else {
                    ai.setCreateTime(TimeFormatter.getMilliSeconds(registeredDateStr, "yyyy-MM-dd", Locale.ENGLISH));
                }
            }
            final String validUntilDateStr = br.getRegex("(?i)Premium End\\s*:\\s*</td>\\s+<td>([^<>]*?)</td>").getMatch(0);
            if (br.containsHTML("(?i)>\\s*Premium Member\\s*<")) {
                account.setType(AccountType.PREMIUM);
                account.setConcurrentUsePossible(true);
                if (validUntilDateStr != null) {
                    if (validUntilDateStr.matches("\\d{2}-\\d{2}-\\d{4}")) {
                        ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntilDateStr, "dd-MM-yyyy", Locale.ENGLISH));
                    } else {
                        ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntilDateStr, "yyyy-MM-dd", Locale.ENGLISH));
                    }
                }
                if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    if (br.containsHTML("(?i)direct=0")) {
                        ai.setStatus(account.getType().getLabel() + " | Direct Downloads: Enabled");
                    } else {
                        ai.setStatus(account.getType().getLabel() + " | Direct Downloads: Disabled");
                    }
                }
            } else {
                account.setType(AccountType.FREE);
                account.setConcurrentUsePossible(false);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, ai, true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        this.handleDownload(link, account);
    }

    private String findFreeGetLink(final Browser br) {
        String getLink = br.getRegex("disabled\\s*=\\s*\"disabled\"\\s*onclick\\s*=\\s*\"document\\.location\\s*=\\s*'(https?.*?)';\"").getMatch(0);
        if (getLink == null) {
            getLink = br.getRegex("('|\")(" + "https?://(?:www\\.)?([a-z0-9]+\\.)?" + Pattern.quote(br.getHost()) + "/get/[A-Za-z0-9]+/\\d+/[^<>\"/]+)\\1").getMatch(1);
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
     *         default: true </br>
     *         Example which supports https: extmatrix.com </br>
     *         Example which does NOT support https: filepup.net
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