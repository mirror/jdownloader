package org.jdownloader.plugins.components;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ArchiveExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ImageExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.VideoExtensions;

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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class UnknownHostingScriptCore extends antiDDoSForHost {
    public UnknownHostingScriptCore(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(getPurchasePremiumURL());
    }
    // /* 1st domain = current domain! */
    // public static String[] domains = new String[] { "dummyhost.tld" };
    //
    // public static String[] getAnnotationNames() {
    // return new String[] { domains[0] };
    // }
    //
    // /**
    // * returns the annotation pattern array: 'https?://(?:www\\.)?(?:domain1|domain2)/[A-Za-z0-9]+'
    // *
    // */
    // /**
    // * returns the annotation pattern array: 'https?://(?:www\\.)?(?:domain1|domain2)/[A-Za-z0-9]+(?:/[^/]+)?'
    // *
    // */
    // public static String[] getAnnotationUrls() {
    // // construct pattern
    // final String host = getHostsPattern();
    // return new String[] { host + "/[A-Za-z0-9]+(?:/[^/<>]+)?" };
    // }

    //
    // /** Returns '(?:domain1|domain2)' */
    // private static String getHostsPatternPart() {
    // final StringBuilder pattern = new StringBuilder();
    // for (final String name : domains) {
    // pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
    // }
    // return pattern.toString();
    // }
    //
    // /** returns 'https?://(?:www\\.)?(?:domain1|domain2)' */
    // private static String getHostsPattern() {
    // final String hosts = "https?://(?:www\\.)?" + "(?:" + getHostsPatternPart() + ")";
    // return hosts;
    // }
    //
    // @Override
    // public String[] siteSupportedNames() {
    // return domains;
    // }
    /**
     * For sites which use this script (example-site, script-name is yet to be found out): https://anonfiles.com/<br />
     * mods: see overridden functions in host plugins<br />
     * limit-info:<br />
     * captchatype: null<br />
     */
    @Override
    public String getAGBLink() {
        return this.getMainPage() + "/terms";
    }

    public String getPurchasePremiumURL() {
        return this.getMainPage() + "/register";
    }

    public static final String getDefaultAnnotationPatternPart() {
        return "/[A-Za-z0-9]+(?:/[^/<>]+)?";
    }

    public static String[] buildAnnotationUrls(List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* 2019-05-07: So far I was not able to find any website using this script which required a captcha. */
        return false;
    }

    private static AtomicReference<String> agent = new AtomicReference<String>(null);

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* link cleanup, but respect users protocol choosing or forced protocol */
        final String fid = getFID(link);
        final String protocol;
        if (supports_https()) {
            protocol = "https";
        } else {
            protocol = "http";
        }
        link.setPluginPatternMatcher(String.format("%s://%s/%s", protocol, this.getHost(), fid));
        link.setLinkID(this.getHost() + "://" + fid);
    }

    private String getFID(final DownloadLink link) {
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), "^https?://[^/]+/([A-Za-z0-9]+)");
        final String fid = urlinfo.getMatch(0);
        return fid;
    }

    /**
     * Returns whether resume is supported or not for current download mode based on account availability and account type. <br />
     * Override this function to set resume settings!
     */
    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
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
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    /** Returns direct-link-property-String for current download mode based on account availibility and account type. */
    protected static String getDownloadModeDirectlinkProperty(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return "freelink2";
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return "premlink";
        } else {
            /* Free(anonymous) and unknown account type */
            return "freelink";
        }
    }

    /**
     * @return true: Website supports https and plugin will prefer https. <br />
     *         false: Website does not support https - plugin will avoid https. <br />
     *         default: true
     */
    public boolean supports_https() {
        return true;
    }

    /**
     * @return true: Implies that website has a working & relaible filecheck-API (check if "https://<host>/docs/api" exists) <br />
     *         false: Implies that website does NOT have a filecheck API. <br />
     *         default: true
     */
    public boolean supports_availablecheck_via_api() {
        return true;
    }

    /**
     * Most configurations won't use 'www.' by default. <br />
     *
     * @return true: Implies that website requires 'www.' in all URLs. <br />
     *         false: Implies that website does NOT require 'www.' in all URLs. <br />
     *         default: false
     */
    public boolean requires_WWW() {
        return false;
    }

    /**
     * @return true: Use random User-Agent. <br />
     *         false: Use Browsers' default User-Agent. <br />
     *         default: false
     */
    public boolean enable_random_user_agent() {
        return false;
    }

    /** Returns empty StringArray for filename, filesize, [more information in the future?] */
    protected String[] getFileInfoArray() {
        return new String[2];
    }

    /**
     * Their normal URLs look exactly like downloadurls and thus will get picked up. This does not have to be perfect as we got a 2nd
     * detection in code below.
     */
    protected boolean isNotADownloadURL(final String url) {
        return url.matches(".+/(abuse|faq|feedback|docs|terms|subscription).*?");
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setWeakFilename(link);
        if (isNotADownloadURL(link.getPluginPatternMatcher())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser(this.br);
        final String fallback_filename = this.getFallbackFilename(link);
        final String[] fileInfo = getFileInfoArray();
        try {
            if (supports_availablecheck_via_api()) {
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                getPage(String.format("https://%s/api/v2/file/%s/info", this.getHost(), this.getFID(link)));
                /*
                 * E.g.
                 * {"status":false,"error":{"message":"The file you are looking for does not exist.","type":"ERROR_FILE_NOT_FOUND","code":
                 * 404}}
                 */
                /*
                 * E.g. wrong language cookie set --> Website will always first redirect to mainpage and set supported language-cookie (e.g.
                 * minfil.com does not support "lang":"us") [see prepBrowser()]
                 */
                final boolean isNoAPIUrlAnymore = !br.getURL().contains(this.getFID(link));
                final boolean isOffline = br.getHttpConnection().getResponseCode() == 404;
                if (isOffline || isNoAPIUrlAnymore) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                scanInfoAPI(fileInfo);
                fileInfo[0] = correctFilename(fileInfo[0]);
            } else {
                getPage(link.getPluginPatternMatcher());
                if (isOfflineWebsite()) {
                    checkErrors(link, null);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                scanInfoWebsite(fileInfo);
                /* 2019-05-07: Website-filenames do not need any corrections. */
                // fileInfo[0] = correctFilename(fileInfo[0]);
            }
            if (StringUtils.isEmpty(fileInfo[0])) {
                /* Final fallback - this should never happen! */
                fileInfo[0] = fallback_filename;
            }
            fileInfo[0] = Encoding.htmlDecode(fileInfo[0]).trim();
            link.setName(fileInfo[0]);
            if (fileInfo[1] != null) {
                if (!fileInfo[1].matches("\\d+")) {
                    fileInfo[1] = Encoding.htmlDecode(fileInfo[1].replace(",", ""));
                }
                link.setDownloadSize(SizeFormatter.getSize(fileInfo[1]));
            }
        } finally {
            /* Something went seriously wrong? Use fallback filename! */
            if (StringUtils.isEmpty(fileInfo[0])) {
                link.setName(getFallbackFilename(link));
            }
        }
        return AvailableStatus.TRUE;
    }

    public boolean isOfflineWebsite() {
        final boolean isOffline = br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">The file you are looking for does not exist|>The file you were looking for could not be found");
        /* Some normal website URLs look exactly like downloadurls and will definitely get picked up by our hostpattern. */
        final boolean isNoDownloadableContent = !br.containsHTML("id=\"download\\-wrapper\"");
        return isOffline || isNoDownloadableContent;
    }

    /**
     * Tries to find filename and filesize inside html. On Override, make sure to FIRST use your special RegExes e.g. fileInfo[0]="bla",
     * THEN, if needed, call super.scanInfo(fileInfo). <br />
     * fileInfo[0] = filename, fileInfo[1] = filesize <br />
     */
    public String[] scanInfoAPI(final String[] fileInfo) {
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[0] = PluginJSonUtils.getJson(br, "name");
        }
        if (StringUtils.isEmpty(fileInfo[1])) {
            fileInfo[1] = PluginJSonUtils.getJson(br, "bytes");
        }
        return fileInfo;
    }

    /**
     * 2019-05-07: Special: API filenames and filenames inside URL sometimes returns bad filenames e.g. bayfiles.com, anonfiles.com. It
     * replaces some spaces and dots with "_" --> We will at least have to replace the last underscore by DOT to have a file-extension
     */
    public String correctFilename(final String input) {
        String output = input;
        if (!StringUtils.isEmpty(input) && input.contains("_") && input.length() > 2) {
            /* First fix filename beginning - beginning with underscore is definitely wrong! */
            if (input.startsWith("_")) {
                output = input.substring(1);
            }
            /* Now fix filename ending / fileextension */
            /* filename_part1: Filename without extension */
            final String filename_part1 = output.substring(0, output.lastIndexOf("_"));
            /* filename_part2_suggested_extension: The part which might be the extension of our filename */
            final String filename_part2_suggested_extension = output.substring(output.lastIndexOf("_") + 1);
            boolean ending_is_Extension = false;
            final ArrayList<Pattern> patterns = new ArrayList<Pattern>();
            for (final ExtensionsFilterInterface extension : ImageExtensions.values()) {
                final Pattern pattern = extension.getPattern();
                if (pattern != null) {
                    patterns.add(pattern);
                }
            }
            for (final ExtensionsFilterInterface extension : VideoExtensions.values()) {
                final Pattern pattern = extension.getPattern();
                if (pattern != null) {
                    patterns.add(pattern);
                }
            }
            for (final ExtensionsFilterInterface extension : ArchiveExtensions.values()) {
                final Pattern pattern = extension.getPattern();
                if (pattern != null) {
                    patterns.add(pattern);
                }
            }
            for (final Pattern pattern : patterns) {
                if (pattern.matcher(filename_part2_suggested_extension).matches()) {
                    ending_is_Extension = true;
                }
            }
            if (ending_is_Extension) {
                logger.info("Correcting SAFE extension");
                output = filename_part1 + "." + filename_part2_suggested_extension;
            } else {
                /*
                 * 2019-06-25: Filenames without extension are unlikely so for the ones for which we cannot clearly tell whether the ending
                 * shall be a filename or not, we'll simply assume it and 'fix' it. The correct name will be presented on downloadstart
                 * anyways!
                 */
                logger.info("Correcting UN_SAFE extension");
                output = filename_part1 + "." + filename_part2_suggested_extension;
            }
        }
        return output;
    }

    /**
     * Tries to find filename and filesize inside html. On Override, make sure to FIRST use your special RegExes e.g. fileInfo[0]="bla",
     * THEN, if needed, call super.scanInfo(fileInfo). <br />
     * fileInfo[0] = filename, fileInfo[1] = filesize
     */
    public String[] scanInfoWebsite(final String[] fileInfo) {
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[0] = br.getRegex("<h1 class=\"text\\-center text\\-wordwrap\">([^<>\"]+)</h1>").getMatch(0);
        }
        if (StringUtils.isEmpty(fileInfo[1])) {
            /* Language-independant RegEx */
            fileInfo[1] = br.getRegex("file/filetypes/[^\"]+\"/>\\s*?[A-Za-z0-9 ]+\\s*?\\(([^<>\"]+)\\)</a>").getMatch(0);
        }
        return fileInfo;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleDownload(downloadLink, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        final boolean resume = this.isResumeable(link, account);
        final int maxchunks = this.getMaxChunks(account);
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (supports_availablecheck_via_api()) {
                /* Did we use the API before? Then we'll have to access the website now. */
                this.getPage(link.getPluginPatternMatcher());
                /* Check again here just in case the API is wrong. */
                if (isOfflineWebsite()) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            /* Example of a website which supports videostreaming: minfil.com */
            dllink = getDllink(link);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        /*
         * Save directurl before download-attempt as it should be valid even if it e.g. fails because of server issue 503 (= too many
         * connections) --> Should work fine after the next try.
         */
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        checkResponseCodeErrors(dl.getConnection());
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            checkErrors(link, account);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Fix filename (e.g. required for anonfiles.com) */
        String server_filename = getFileNameFromDispositionHeader(dl.getConnection());
        if (server_filename != null && server_filename.contains("%")) {
            server_filename = Encoding.htmlDecode(server_filename);
            link.setFinalFileName(server_filename);
        }
        dl.startDownload();
    }

    private String getDllink(final DownloadLink dl) {
        return getDllink(this.br, dl);
    }

    private String getDllink(final Browser br, final DownloadLink link) {
        String dllink = br.getRegex("id=\"download\\-url\"\\s*?class=\"[^\"]+\"\\s*?href=\"(https[^<>\"]*?)\"").getMatch(0);
        if (StringUtils.isEmpty(dllink) || true) {
            /* 2019-05-07: E.g. bayfiles.com, anonfiles.com */
            final String linkid = getFID(link);
            /*
             * First try to find downloadurl which contains linkid as for different streaming qualities, downloadURLs look exactly the same
             * but lead to different video-resolutions.
             */
            /* 2019-05-07: E.g. bayfiles.com, anonfiles.com */
            dllink = br.getRegex("\"(https?://cdn-\\d+\\.[^/\"]+/" + linkid + "[^<>\"]+)\"").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("\"(https?://cdn\\-\\d+\\.[^/\"]+/[^<>\"]+)\"").getMatch(0);
            }
        }
        return dllink;
    }

    /** Returns unique id from inside URL - usually with this pattern: [A-Za-z0-9]+ */
    protected String getFUIDFromURL(final DownloadLink dl) {
        return getFUIDFromURL(dl.getPluginPatternMatcher());
    }

    /** Returns unique id from inside URL - usually with this pattern: [A-Za-z0-9]+ */
    public static String getFUIDFromURL(final String url) {
        try {
            final String result = new Regex(new URL(url).getPath(), "^/([A-Za-z0-9]+)").getMatch(0);
            return result;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * In some cases, URL may contain filename which can be used as fallback e.g. 'https://host.tld/<fuid>/<filename>'. Example host which
     * has URLs that contain filenames: freefile.me, letsupload.co
     */
    public String getFilenameFromURL(final DownloadLink dl) {
        final String result;
        if (dl.getContentUrl() != null) {
            result = getFilenameFromURL(dl.getContentUrl());
        } else {
            result = getFilenameFromURL(dl.getPluginPatternMatcher());
        }
        return result;
    }

    public String getFilenameFromURL(final String url) {
        try {
            String result = new Regex(new URL(url).getPath(), "[^/]+/(.+)$").getMatch(0);
            if (result != null) {
                result = correctFilename(result);
            }
            return result;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Tries to get filename from URL and if this fails, will return <fuid> filename. */
    public String getFallbackFilename(final DownloadLink dl) {
        String fallback_filename = this.getFilenameFromURL(dl);
        if (fallback_filename == null) {
            fallback_filename = this.getFUIDFromURL(dl);
        }
        return fallback_filename;
    }

    /** Tries to get filename from URL and if this fails, will return <fuid> filename. */
    public String getFallbackFilename(final String url) {
        String fallback_filename = getFilenameFromURL(url);
        if (fallback_filename == null) {
            fallback_filename = getFUIDFromURL(url);
        }
        return fallback_filename;
    }

    public void checkErrors(final DownloadLink link, final Account account) throws PluginException {
        /** TODO: Check if we need this. */
        // if (br.containsHTML("TODO")) {
        // throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "TODO", 3 * 60 * 1000l);
        // }
    }

    /** Handles all kinds of error-responsecodes! */
    private void checkResponseCodeErrors(final URLConnectionAdapter con) throws PluginException {
        if (con == null) {
            return;
        }
        final long responsecode = con.getResponseCode();
        if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
        } else if (responsecode == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
        } else if (responsecode == 416) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 2 * 60 * 1000l);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            final Browser br2 = this.br.cloneBrowser();
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
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

    protected String getProtocol() {
        if ((this.br.getURL() != null && this.br.getURL().contains("https://")) || supports_https()) {
            return "https://";
        } else {
            return "http://";
        }
    }

    public Browser prepBrowser(final Browser br) {
        /*
         * Prefer English html CAUTION: A wrong language cookie (= unsupported by that website) will cause every request to redirect to
         * their mainpage until a supported language-cookie is set (example of unsupported English language: minfil.com)
         */
        br.setCookie(this.getHost(), "lang", "us");
        if (enable_random_user_agent()) {
            if (agent.get() == null) {
                agent.set(UserAgents.stringUserAgent());
            }
            br.getHeaders().put("User-Agent", agent.get());
        }
        return br;
    }

    private static final Object LOCK = new Object();

    private void login(final Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                prepBrowser(this.br, account.getHoster());
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedInViaCookies = false;
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !force) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        return;
                    }
                    logger.info("Verifying login-cookies");
                    getPage(this.getMainPage() + "/");
                    loggedInViaCookies = isLoggedinHTML();
                }
                if (loggedInViaCookies) {
                    /* No additional check required --> We know cookies are valid and we're logged in --> Done! */
                    logger.info("Successfully logged in via cookies");
                } else {
                    logger.info("Performing full login");
                    getPage(this.getProtocol() + this.getHost() + "/login");
                    Form loginform = br.getFormByInputFieldKeyValue("submit", "Login");
                    if (loginform == null) {
                        loginform = br.getFormbyKey("password");
                    }
                    if (loginform == null) {
                        logger.info("Fallback to custom built loginform");
                        loginform = new Form();
                        loginform.setAction(br.getURL());
                        loginform.put("submit", "Login");
                    }
                    loginform.put("username", Encoding.urlEncode(account.getUser()));
                    loginform.put("password", Encoding.urlEncode(account.getPass()));
                    submitForm(loginform);
                    if (!isLoggedinHTML()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedinHTML() {
        return br.containsHTML("/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        if (br.getURL() == null) {
            getPage(this.getMainPage());
        }
        String free_space = br.getRegex("class=\"navbar\\-text hidden\\-xs\">(\\d+ [A-Za-z]{1,5}) free\\.").getMatch(0);
        if (free_space == null) {
            free_space = br.getRegex(">(\\d+ GB) free").getMatch(0);
        }
        boolean isPremium = br.containsHTML("TODO_DID_NOT_HAVE_PREMIUM_ACCOUNT_TO_CHECK");
        if (!isPremium) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(this.getMaxSimultaneousFreeAccountDownloads());
            /* All accounts get the same (IP-based) downloadlimits --> Simultaneous free account usage makes no sense! */
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) account");
        } else {
            getPage("/orders");
            /* If the premium account is expired we'll simply accept it as a free account. */
            String expireStr = br.getRegex("TODO_DID_NOT_HAVE_PREMIUM_ACCOUNT_TO_CHECK").getMatch(0);
            if (expireStr == null) {
                /*
                 * 2019-03-01: As far as we know, EVERY premium account will have an expire-date given but we will still accept accounts for
                 * which we fail to find the expire-date.
                 */
                logger.info("Failed to find expire-date");
                return ai;
            }
            long expire_milliseconds = TimeFormatter.getMilliSeconds(expireStr, "MM/dd/yyyy hh:mm:ss", Locale.ENGLISH);
            isPremium = expire_milliseconds > System.currentTimeMillis();
            if (!isPremium) {
                /* Expired premium == FREE */
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(this.getMaxSimultaneousFreeAccountDownloads());
                /* All accounts get the same (IP-based) downloadlimits --> Simultan free account usage makes no sense! */
                account.setConcurrentUsePossible(false);
                ai.setStatus("Registered (free) user");
            } else {
                ai.setValidUntil(expire_milliseconds, this.br);
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(this.getMaxSimultanPremiumDownloadNum());
                ai.setStatus("Premium account");
            }
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        getPage(link.getPluginPatternMatcher());
        handleDownload(link, account);
    }

    @Override
    protected void getPage(String page) throws Exception {
        page = correctProtocol(page);
        getPage(br, page);
    }

    @Override
    protected void getPage(final Browser br, String page) throws Exception {
        page = correctProtocol(page);
        super.getPage(br, page);
    }

    @Override
    protected void postPage(String page, final String postdata) throws Exception {
        page = correctProtocol(page);
        postPage(br, page, postdata);
    }

    @Override
    protected void postPage(final Browser br, String page, final String postdata) throws Exception {
        page = correctProtocol(page);
        super.postPage(br, page, postdata);
    }

    protected String correctProtocol(String url) {
        if (supports_https()) {
            /* Prefer https whenever possible */
            url = url.replaceFirst("http://", "https://");
        } else {
            url = url.replaceFirst("https://", "http://");
        }
        if (this.requires_WWW() && !url.contains("www.")) {
            url = url.replace("//", "//www.");
        } else if (!this.requires_WWW()) {
            url = url.replace("www.", "");
        }
        return url;
    }

    /** Returns https?://host.tld */
    protected String getMainPage() {
        final String[] hosts = this.siteSupportedNames();
        return ("http://" + hosts[0]).replaceFirst("https?://", this.supports_https() ? "https://" : "http://");
    }

    /**
     * Use this to set filename based on filename inside URL or fuid as filename either before a linkcheck happens so that there is a
     * readable filename displayed in the linkgrabber.
     */
    protected void setWeakFilename(final DownloadLink link) {
        final String weak_fallback_filename = this.getFallbackFilename(link);
        /* Set fallback_filename if no better filename has ever been set before. */
        final boolean setWeakFilename = link.getName() == null || (weak_fallback_filename != null && weak_fallback_filename.length() > link.getName().length());
        if (setWeakFilename) {
            link.setName(weak_fallback_filename);
            /// * TODO: Find better way to determine whether a String contains a file-extension or not. */
            // final boolean fallback_filename_contains_file_extension = weak_fallback_filename != null &&
            /// weak_fallback_filename.contains(".");
            // if (!fallback_filename_contains_file_extension) {
            // /* Only setMimeHint if weak filename does not contain filetype. */
            // if (this.isAudiohoster()) {
            // link.setMimeHint(CompiledFiletypeFilter.AudioExtensions.MP3);
            // }
            // }
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_FilehostScript;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE && link != null) {
            /* Reset directurl-properties in stable, NOT in dev mode */
            /*
             * TODO 2019-04-05: Either just don't do this or find a better solution for this. This will cause unnecessary captchas and will
             * just waste the users' hard work of generating direct-URLs (e.g. entering captchas). I have never seen a situation in which
             * one of our plugins e.g. looped forever because of bad directlink handling. This plugin is designed to verify directlinks and
             * automatically delete that property once a directlink is not valid anymore!
             */
            link.removeProperty("freelink2");
            link.removeProperty("premlink");
            link.removeProperty("freelink");
        }
    }
}