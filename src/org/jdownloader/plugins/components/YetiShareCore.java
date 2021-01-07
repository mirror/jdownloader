package org.jdownloader.plugins.components;

//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.StorageException;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class YetiShareCore extends antiDDoSForHost {
    public YetiShareCore(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(getPurchasePremiumURL());
    }

    // public static List<String[]> getPluginDomains() {
    // final List<String[]> ret = new ArrayList<String[]>();
    // // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
    // ret.add(new String[] { "testhost.com" });
    // return ret;
    // }
    //
    // public static String[] getAnnotationNames() {
    // return buildAnnotationNames(getPluginDomains());
    // }
    //
    // @Override
    // public String[] siteSupportedNames() {
    // return buildSupportedNames(getPluginDomains());
    // }
    //
    // public static String[] getAnnotationUrls() {
    // final List<String[]> pluginDomains = getPluginDomains();
    // final List<String> ret = new ArrayList<String>();
    // for (final String[] domains : pluginDomains) {
    // ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + YetiShareCore.getDefaultAnnotationPatternPart());
    // }
    // return ret.toArray(new String[0]);
    // }
    // @Override
    // public String rewriteHost(String host) {
    // return this.rewriteHost(getPluginDomains(), host, new String[0]);
    // }
    public static final String getDefaultAnnotationPatternPart() {
        return "/(?!folder)[A-Za-z0-9]+(?:/[^/<>]+)?";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + YetiShareCore.buildHostsPatternPart(domains) + YetiShareCore.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    /**
     * For sites which use this script: http://www.yetishare.com/<br />
     * YetiShareCore Version 2.0.1.0-psp<br />
     * mods: see overridden functions in host plugins<br />
     * limit-info:<br />
     * captchatype: null, solvemedia, reCaptchaV2<br />
     * Another alternative method of linkchecking (displays filename only): host.tld/<fid>~s (statistics) 2019-06-12: Consider adding API
     * support: https://fhscript.com/admin/api_documentation.php#account-info Examples for websites which have the API enabled (but not
     * necessarily unlocked for all users, usually only special-uploaders): crazyshare.cc, easylinkz.net, freefile.me <br />
     * 2020-03-30: I failed to make ANY successful API tests. 100% of all websites which support this API are running a broken version!
     */
    @Override
    public String getAGBLink() {
        return this.getMainPage() + "/terms.html";
    }

    public String getPurchasePremiumURL() {
        return this.getMainPage() + "/register.html";
    }

    // private static final boolean enable_regex_stream_url = true;
    private static AtomicReference<String> agent                     = new AtomicReference<String>(null);
    /** See {@link #usesNewYetiShareVersion()} */
    public static final String             PROPERTY_INTERNAL_FILE_ID = "INTERNALFILEID";
    public static final String             PROPERTY_UPLOAD_DATE_RAW  = "UPLOADDATE_RAW";

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* link cleanup, but respect users protocol choosing or forced protocol */
        final String fid = getFUID(link);
        final String protocol;
        if (supports_https()) {
            protocol = "https://";
        } else {
            protocol = "http://";
        }
        final String url_path = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/(.+)").getMatch(0);
        link.setPluginPatternMatcher(protocol + this.getHost() + "/" + url_path);
        link.setLinkID(this.getHost() + "://" + fid);
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
     * @return true: Implies that website will show filename & filesize via website.tld/<fuid>~i <br />
     *         Most YetiShare websites support this kind of linkcheck! </br>
     *         false: Implies that website does NOT show filename & filesize via website.tld/<fuid>~i. <br />
     *         default: true
     */
    public boolean supports_availablecheck_over_info_page(DownloadLink link) {
        return true;
    }

    /** @return default: true */
    protected boolean supports_availablecheck_filesize_html() {
        return true;
    }

    /**
     * Most YetiShare configurations will use 'www.' by default but will work with- and without 'www.' and will let the user decide (= no
     * redirect happens when we use 'www.' although it is not used by them by default). <br />
     *
     * @return true: Implies that website requires 'www.' in all URLs. <br />
     *         false: Implies that website does NOT require 'www.' in all URLs. <br />
     *         default: true
     */
    public boolean requires_WWW() {
        return true;
    }

    /**
     * @return true: Use random User-Agent. <br />
     *         false: Use Browsers' default User-Agent. <br />
     *         default: false
     */
    public boolean enable_random_user_agent() {
        return false;
    }

    /**
     * <b> Enabling this may lead to at least one additional website-request! </b><br />
     * TODO: 2019-02-20: Find website which supports video streaming! --> 2020-03-25: Deprecated since 2020-05-14 as this has never been
     * used so far!
     *
     * @return true: Implies that website supports embedding videos. <br />
     *         false: Implies that website does NOT support embedding videos. <br />
     *         default: false
     */
    @Deprecated
    protected boolean supports_embed_stream_download() {
        return false;
    }

    /**
     * Enforces old, non-ajax login-method. </br>
     * This is only rarely needed e.g. filemia.com </br>
     * default = false
     */
    @Deprecated
    protected boolean enforce_old_login_method() {
        return false;
    }

    /** Enable this if new YetiShare version is used. Only mandatory if auto handling fails. */
    protected boolean usesNewYetiShareVersion() {
        return false;
    }

    /** Returns empty StringArray for filename, filesize, [more information in the future?] */
    protected String[] getFileInfoArray() {
        return new String[2];
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            setWeakFilename(link);
        }
        br.setFollowRedirects(true);
        prepBrowserWebsite(this.br);
        final String fallback_filename = this.getFallbackFilename(link);
        final String[] fileInfo = getFileInfoArray();
        try {
            if (supports_availablecheck_over_info_page(link)) {
                getPage(this.getMainPage() + "/" + this.getFUID(link) + "~i");
                /* Offline check is unsafe which is why we need to check for other errors first! */
                try {
                    this.checkErrors(link, account);
                } catch (final PluginException e) {
                    if (isDownload || e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                        throw e;
                    } else {
                        logger.log(e);
                        return AvailableStatus.TRUE;
                    }
                }
                /* Offline errorhandling */
                if (!br.getURL().contains("~i") || br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else {
                getPage(link.getPluginPatternMatcher());
                /* Offline check is very unsafe which is why we need to check for other errors first! */
                try {
                    this.checkErrors(link, account);
                } catch (final PluginException e) {
                    if (isDownload || e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                        throw e;
                    } else {
                        logger.log(e);
                        /* File is probably online but another error happened --> Do not throw it during availablecheck! */
                        return AvailableStatus.TRUE;
                    }
                }
                if (isOfflineWebsite(link)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            scanInfo(link, fileInfo);
            if (StringUtils.isEmpty(fileInfo[0])) {
                /* Final fallback - this should never happen! */
                fileInfo[0] = fallback_filename;
            }
            link.setName(fileInfo[0]);
            if (fileInfo[1] != null) {
                link.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(fileInfo[1].replace(",", ""))));
            }
        } finally {
            /* Something went seriously wrong? Use fallback filename! */
            if (StringUtils.isEmpty(fileInfo[0])) {
                link.setName(getFallbackFilename(link));
            }
        }
        return AvailableStatus.TRUE;
    }

    /**
     * Tries to find filename and filesize inside html. On Override, make sure to first use your special RegExes e.g. fileInfo[0]="bla",
     * THEN, if needed, call super.scanInfo(fileInfo). <br />
     * fileInfo[0] = filename, fileInfo[1] = filesize
     */
    public String[] scanInfo(final DownloadLink link, final String[] fileInfo) {
        if (supports_availablecheck_over_info_page(link)) {
            final List<String> fileNameCandidates = new ArrayList<String>();
            /* Add pre given candidate */
            if (!StringUtils.isEmpty(fileInfo[0])) {
                fileNameCandidates.add(Encoding.htmlDecode(fileInfo[0]).trim());
            }
            final String[] tableData = this.br.getRegex("class=\"responsiveInfoTable\">([^<>\"/]*?)<").getColumn(0);
            /* Sometimes we get crippled results with the 2nd RegEx so use this one first */
            {
                String lang_str_information = PluginJSonUtils.getJson(br, "file_information_left_description");
                if (StringUtils.isEmpty(lang_str_information)) {
                    /* Fallback to English */
                    lang_str_information = "Information about";
                }
                String name = this.br.getRegex("data\\-animation\\-delay=\"\\d+\"\\s*>\\s*" + Pattern.quote(lang_str_information) + "\\s*([^<>\"]*?)\\s*</div>").getMatch(0);
                if (name == null) {
                    /* Wider attempt */
                    name = this.br.getRegex(">\\s*" + Pattern.quote(lang_str_information) + "\\s*([^<>\"]+)<").getMatch(0);
                }
                name = name != null ? Encoding.htmlOnlyDecode(name).trim() : null;
                if (name != null && !fileNameCandidates.contains(name)) {
                    fileNameCandidates.add(name);
                }
            }
            {
                /*
                 * "Information about"-filename-trait without the animation(delay). E.g. easylinkz.net - sometimes it may also happen that
                 * the 'Filename:' is empty and the filename is only present at this place!
                 */
                String name = this.br.getRegex("<meta\\s*name\\s*=\\s*\"description[^\"]*\"\\s*content\\s*=\\s*\"\\s*(?:Information about|informacje o)\\s*([^<>\"]+)\\s*\"").getMatch(0);
                name = name != null ? Encoding.htmlOnlyDecode(name).trim() : null;
                if (name != null && !fileNameCandidates.contains(name)) {
                    fileNameCandidates.add(name);
                }
            }
            {
                /*
                 * "Information about"-filename-trait without the animation(delay). E.g. easylinkz.net - sometimes it may also happen that
                 * the 'Filename:' is empty and the filename is only present at this place!
                 */
                String name = this.br.getRegex("class\\s*=\\s*\"description\\-1[^\"]*\"\\s*>\\s*(?:Information about|informacje o)\\s*([^<>\"]+)\\s*<").getMatch(0);
                name = name != null ? Encoding.htmlOnlyDecode(name).trim() : null;
                if (name != null && !fileNameCandidates.contains(name)) {
                    fileNameCandidates.add(name);
                }
            }
            {
                String name = fileInfo[0] = this.br.getRegex("(?:Filename|Dateiname|اسم الملف|Nome|Dosya Adı|Nazwa Pliku)\\s*:[\t\n\r ]*?</td>[\t\n\r ]*?<td(?: class=\"responsiveInfoTable\")?>\\s*([^<>\"]*?)\\s*<").getMatch(0);
                name = name != null ? Encoding.htmlOnlyDecode(name).trim() : null;
                if (name != null && !fileNameCandidates.contains(name)) {
                    fileNameCandidates.add(name);
                }
            }
            if (StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = br.getRegex("(?:Filesize|Dateigröße|حجم الملف|Tamanho|Boyut|Rozmiar Pliku)\\s*:\\s*</td>\\s*?<td(?:\\s*class=\"responsiveInfoTable\")?>\\s*([^<>\"]*?)\\s*<").getMatch(0);
            }
            {
                /** 2021-01-07: Traits for the new style YetiShare layout --> See {@link #usesNewYetiShareVersion()} */
                if (supports_availablecheck_over_info_page(link)) {
                    /* 2020-10-12: Special */
                    final String betterFilesize = br.getRegex("Filesize\\s*:\\s*</span>\\s*<span>([^<>\"]+)<").getMatch(0);
                    if (!StringUtils.isEmpty(betterFilesize)) {
                        fileInfo[1] = betterFilesize;
                    }
                }
            }
            try {
                /* Language-independant attempt ... */
                if (StringUtils.isEmpty(fileInfo[0]) && tableData.length > 0) {
                    String name = tableData[0];
                    name = name != null ? Encoding.htmlOnlyDecode(name).trim() : null;
                    if (StringUtils.isNotEmpty(name) && !fileNameCandidates.contains(name)) {
                        fileNameCandidates.add(name);
                    }
                }
                if (StringUtils.isEmpty(fileInfo[1]) && tableData.length > 1) {
                    fileInfo[1] = tableData[1];
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
            String bestName = null;
            for (final String fileNameCandidate : fileNameCandidates) {
                if (StringUtils.isEmpty(fileNameCandidate)) {
                    continue;
                } else if (bestName == null) {
                    bestName = fileNameCandidate;
                } else if (bestName.contains("...") && !fileNameCandidate.contains("...")) {
                    bestName = fileNameCandidate;
                } else if (bestName.length() < fileNameCandidate.length()) {
                    bestName = fileNameCandidate;
                }
            }
            fileInfo[0] = bestName;
        } else {
            final Regex fInfo = br.getRegex("<strong>([^<>\"]*?) \\((\\d+(?:,\\d+)?(?:\\.\\d+)? (?:TB|GB|MB|KB|B))\\)<");
            if (StringUtils.isEmpty(fileInfo[0])) {
                fileInfo[0] = fInfo.getMatch(0);
            }
            if (supports_availablecheck_filesize_html()) {
                if (StringUtils.isEmpty(fileInfo[1])) {
                    fileInfo[1] = fInfo.getMatch(1);
                }
                /* Generic failover */
                if (StringUtils.isEmpty(fileInfo[1])) {
                    // sync with XFileSharingProBasic.scanInfo- Generic failover
                    fileInfo[1] = br.getRegex("(?:>\\s*|\\(\\s*|\"\\s*|\\[\\s*|\\s+)([0-9\\.]+(?:\\s+|\\&nbsp;)?(TB|GB|MB|KB)(?!ps|/s|\\s*Storage|\\s*Disk|\\s*Space))").getMatch(0);
                }
            }
        }
        return fileInfo;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        handleDownloadWebsite(link, null);
    }

    public void handleDownloadWebsite(final DownloadLink link, final Account account) throws Exception, PluginException {
        final boolean resume = this.isResumeable(link, account);
        final int maxchunks = this.getMaxChunks(account);
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        String continue_link = null;
        boolean captcha = false;
        boolean success = false;
        long timeBeforeCaptchaInput;
        continue_link = checkDirectLink(link, account);
        if (StringUtils.isEmpty(continue_link) && this.dl == null) {
            br.setFollowRedirects(false);
            // if (supports_embed_stream_download()) {
            // try {
            // final Browser br2 = this.br.cloneBrowser();
            // getPage(br2, String.format("/embed/u=%s/", this.getFID(link)));
            // continue_link = this.getStreamUrl(br2);
            // } catch (final BrowserException e) {
            // }
            // }
            if (supports_availablecheck_over_info_page(link)) {
                br.setFollowRedirects(true);
                /* For premium mode, we might get our final downloadurl here already. */
                final URLConnectionAdapter con = br.openGetConnection(link.getPluginPatternMatcher());
                if (this.looksLikeDownloadableContent(con)) {
                    con.disconnect();
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, con.getRequest(), resume, maxchunks);
                } else {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            } else if (br.getRedirectLocation() != null) {
                br.setFollowRedirects(true);
                /* For premium mode, we might get our final downloadurl here already. */
                final URLConnectionAdapter con = br.openGetConnection(br.getRedirectLocation());
                if (this.looksLikeDownloadableContent(con)) {
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, con.getRequest(), resume, maxchunks);
                } else {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            if (this.dl == null) {
                if (StringUtils.isEmpty(continue_link)) {
                    checkErrors(link, account);
                    continue_link = getContinueLink();
                }
                /* Passwords are usually before waittime. */
                handlePassword(link);
                /* Handle up to x pre-download pages before the (eventually existing) captcha */
                final int startValue = 0;
                /* loopLog holds information about the continue_link of each loop so afterwards we get an overview via logger */
                String loopLog = continue_link;
                final int maxLoops = 8;
                for (int i = startValue; i <= maxLoops; i++) {
                    logger.info("Handling pre-download page " + (i + 1) + " of max. allowed " + maxLoops);
                    timeBeforeCaptchaInput = System.currentTimeMillis();
                    if (i > startValue) {
                        loopLog += " --> " + continue_link;
                    }
                    if (isDownloadlink(continue_link)) {
                        /*
                         * If we already found a downloadlink let's try to download it because html can still contain captcha html --> We
                         * don't need a captcha in this case/loop/pass for sure! E.g. host '3rbup.com'.
                         */
                        waitTime(link, timeBeforeCaptchaInput);
                        dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_link, resume, maxchunks);
                    } else {
                        final Form continueform = getContinueForm(i, continue_link);
                        if (i == startValue && continueform == null) {
                            logger.info("No continue_form/continue_link available, plugin broken");
                            checkErrorsLastResort(link, account);
                        } else if (continueform == null) {
                            logger.info("No continue_form/continue_link available, stepping out of pre-download loop");
                            break;
                        } else {
                            logger.info("Found continue_form/continue_link, continuing...");
                        }
                        final String rcID = br.getRegex("recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
                        if (br.containsHTML("data\\-sitekey=|g\\-recaptcha\\'")) {
                            loopLog += " --> reCaptchaV2";
                            captcha = true;
                            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                            success = true;
                            waitTime(link, timeBeforeCaptchaInput);
                            continueform.put("capcode", "false");
                            continueform.put("g-recaptcha-response", recaptchaV2Response);
                            continueform.setMethod(MethodType.POST);
                            dl = jd.plugins.BrowserAdapter.openDownload(br, link, continueform, resume, maxchunks);
                        } else if (rcID != null) {
                            /* Dead end! */
                            captcha = true;
                            success = false;
                            throw new PluginException(LinkStatus.ERROR_FATAL, "Website uses reCaptchaV1 which has been shut down by Google. Contact website owner!");
                        } else if (br.containsHTML("solvemedia\\.com/papi/")) {
                            loopLog += " --> SolvemediaCaptcha";
                            captcha = true;
                            success = false;
                            logger.info("Detected captcha method \"solvemedia\" for this host");
                            final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                            if (br.containsHTML("api\\-secure\\.solvemedia\\.com/")) {
                                sm.setSecure(true);
                            }
                            File cf = null;
                            try {
                                cf = sm.downloadCaptcha(getLocalCaptchaFile());
                            } catch (final Exception e) {
                                if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                                    throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support", -1, e);
                                } else {
                                    throw e;
                                }
                            }
                            final String code = getCaptchaCode("solvemedia", cf, link);
                            final String chid = sm.getChallenge(code);
                            waitTime(link, timeBeforeCaptchaInput);
                            continueform.put("adcopy_challenge", Encoding.urlEncode(chid));
                            continueform.put("adcopy_response", Encoding.urlEncode(code));
                            continueform.setMethod(MethodType.POST);
                            dl = jd.plugins.BrowserAdapter.openDownload(br, link, continueform, resume, maxchunks);
                        } else if (continueform != null && continueform.getMethod() == MethodType.POST) {
                            loopLog += " --> Form_POST";
                            success = true;
                            waitTime(link, timeBeforeCaptchaInput);
                            /* Use URL instead of Form - it is all we need! */
                            dl = jd.plugins.BrowserAdapter.openDownload(br, link, continueform, resume, maxchunks);
                        } else {
                            if (continue_link == null) {
                                checkErrors(link, account);
                                logger.warning("Failed to find continue_link");
                                checkErrorsLastResort(link, account);
                            }
                            br.setFollowRedirects(false);
                            waitTime(link, timeBeforeCaptchaInput);
                            getPage(continue_link);
                            /* Loop to handle redirects */
                            while (true) {
                                final String redirect = this.br.getRedirectLocation();
                                if (redirect != null) {
                                    if (isDownloadlink(redirect)) {
                                        continue_link = redirect;
                                        break;
                                    } else {
                                        br.followRedirect();
                                    }
                                } else {
                                    continue_link = this.getContinueLink();
                                    break;
                                }
                            }
                            br.setFollowRedirects(true);
                            continue;
                        }
                    }
                    final URLConnectionAdapter con = dl.getConnection();
                    try {
                        checkResponseCodeErrors(con);
                    } catch (final PluginException e) {
                        try {
                            br.followConnection(true);
                        } catch (IOException ioe) {
                            throw Exceptions.addSuppressed(e, ioe);
                        }
                        throw e;
                    }
                    if (looksLikeDownloadableContent(con)) {
                        success = true;
                        loopLog += " --> " + con.getURL().toString();
                        break;
                    } else {
                        try {
                            br.followConnection(true);
                        } catch (IOException e) {
                            logger.log(e);
                        }
                        /* Get new continue_link for the next run */
                        continue_link = getContinueLink();
                        checkErrors(link, account);
                        if (captcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                            logger.info("Wrong captcha");
                            continue;
                        }
                    }
                }
                logger.info("loopLog: " + loopLog);
            }
        }
        if (dl == null) {
            checkErrors(link, account);
            checkErrorsLastResort(link, account);
        }
        final URLConnectionAdapter con = dl.getConnection();
        /*
         * Save directurl before download-attempt as it should be valid even if it e.g. fails because of server issue 503 (= too many
         * connections) --> Should work fine after the next try.
         */
        link.setProperty(directlinkproperty, con.getURL().toString());
        try {
            checkResponseCodeErrors(con);
        } catch (final PluginException e) {
            try {
                br.followConnection(true);
            } catch (IOException ioe) {
                throw Exceptions.addSuppressed(e, ioe);
            }
            throw e;
        }
        if (!looksLikeDownloadableContent(con)) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            if (captcha && !success) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            checkErrors(link, account);
            checkErrorsLastResort(link, account);
        }
        dl.setFilenameFix(isContentDispositionFixRequired(dl, con, link));
        dl.startDownload();
    }

    protected Form getContinueForm(final int loop_counter, final String continue_link) throws PluginException {
        /* 2019-07-05: continue_form without captcha is a rare case. Example-site: freefile.me */
        Form continueform = br.getFormbyActionRegex(".+pt=.+");
        if (continueform == null) {
            continueform = br.getFormByInputFieldKeyValue("submitted", "1");
        }
        if (continueform == null) {
            continueform = br.getFormbyKey("submitted");
        }
        if (!StringUtils.isEmpty(continue_link) && continueform == null) {
            continueform = new Form();
            continueform.setMethod(MethodType.GET);
            continueform.setAction(continue_link);
            continueform.put("submit", "Submit");
            continueform.put("submitted", "1");
            continueform.put("d", "1");
        }
        return continueform;
    }

    protected String getContinueLink() throws Exception {
        String continue_link = br.getRegex("\\$\\(\\'\\.download\\-timer\\'\\)\\.html\\(\"<a href=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
        if (continue_link == null) {
            continue_link = br.getRegex("class=\\'btn btn\\-free\\' href=\\'(https?://[^<>\"]*?)\\'>").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("<div class=\"captchaPageTable\">\\s*<form method=\"POST\" action=\"(https?://[^<>\"]*?)\"").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("(https?://[^/]+/[^<>\"\\':]*pt=[^<>\"\\']*)(?:\"|\\')").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = getDllink();
        }
        return continue_link;
    }

    // private String getStreamUrl() {
    // return getStreamUrl(this.br);
    // }
    //
    // private String getStreamUrl(final Browser br) {
    // return br.getRegex("file\\s*?:\\s*?\"(https?://[^<>\"]+)\"").getMatch(0);
    // }
    /** 2019-08-29: Never call this directly - always call it via getContinueLink!! */
    private String getDllink() {
        return getDllink(this.br);
    }

    /** If overridden, make sure to make isDownloadlink compatible as well! */
    protected String getDllink(final Browser br) {
        /* 2020-02-26: Example without 'http': cnubis.com */
        /* 2020-12-11: Example ... "/themes/files/": zupload.me */
        String ret = br.getRegex("(?:\"|\\')((?:https?:)?//[A-Za-z0-9\\.\\-]+\\.[^/]+/[^<>\"]*?(?:\\?|\\&)download_token=[A-Za-z0-9]+[^<>\"\\']*?)(?:\"|\\')").getMatch(0);
        if (StringUtils.isEmpty(ret)) {
            ret = br.getRegex("\"(https?://[^\"]+(?<!/themes)/files/[^\"]+)\"").getMatch(0);
        }
        if (isDownloadlink(ret)) {
            return ret;
        } else if (ret != null) {
            logger.info("isDownloadlink false:" + ret);
            return null;
        } else {
            return null;
        }
    }

    public boolean isDownloadlink(final String url) {
        /* Most sites use 'download_token', '/files/' is e.g. used by dropmega.com and dbree.co */
        final boolean ret = url != null && (url.contains("download_token=") || url.contains("/files/"));
        return ret;
    }

    /** Returns unique id from inside URL - usually with this pattern: [A-Za-z0-9]+ */
    protected String getFUID(final DownloadLink link) {
        return getFUIDFromURL(link.getPluginPatternMatcher());
    }

    /** Returns unique id from inside URL - usually with this pattern: [A-Za-z0-9]+ */
    public String getFUIDFromURL(final String url) {
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
    public String getFilenameFromURL(final DownloadLink link) {
        final String result;
        if (link.getContentUrl() != null) {
            result = getFilenameFromURL(link.getContentUrl());
        } else {
            result = getFilenameFromURL(link.getPluginPatternMatcher());
        }
        return result;
    }

    public static String getFilenameFromURL(final String url) {
        try {
            final String result = new Regex(new URL(url).getPath(), "[^/]+/(.+)$").getMatch(0);
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
            /* Final fallback */
            fallback_filename = this.getFUID(dl);
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

    private void handlePassword(final DownloadLink dl) throws Exception {
        if (br.getURL().contains("/file_password.html")) {
            logger.info("Current link is password protected");
            String passCode = dl.getStringProperty("pass", null);
            if (passCode == null) {
                passCode = Plugin.getUserInput("Password?", dl);
                if (passCode == null || passCode.equals("")) {
                    logger.info("User has entered blank password, exiting handlePassword");
                    dl.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                }
                dl.setDownloadPassword(passCode);
            }
            postPage(br.getURL(), "submit=access+file&submitme=1&file=" + this.getFUID(dl) + "&filePassword=" + Encoding.urlEncode(passCode));
            if (br.getURL().contains("/file_password.html")) {
                logger.info("User entered incorrect password --> Retrying");
                dl.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            logger.info("User entered correct password --> Continuing");
        }
    }

    protected boolean preDownloadWaittimeSkippable() {
        return false;
    }

    /**
     * Handles pre download (pre-captcha) waittime. If WAITFORCED it ensures to always wait long enough even if the waittime RegEx fails.
     */
    protected void waitTime(final DownloadLink downloadLink, final long timeBefore) throws PluginException {
        /* Ticket Time */
        final String waitStr = regexWaittime();
        if (this.preDownloadWaittimeSkippable()) {
            /* Very rare case! */
            logger.info("Skipping pre-download waittime: " + waitStr);
        } else {
            final int extraWaitSeconds = 1;
            int wait;
            if (waitStr != null && waitStr.matches("\\d+")) {
                int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - extraWaitSeconds;
                logger.info("Found waittime, parsing waittime: " + waitStr);
                wait = Integer.parseInt(waitStr);
                /*
                 * Check how much time has passed during eventual captcha event before this function has been called and see how much time
                 * is left to wait.
                 */
                wait -= passedTime;
                if (passedTime > 0) {
                    /* This usually means that the user had to solve a captcha which cuts down the remaining time we have to wait. */
                    logger.info("Total passed time during captcha: " + passedTime);
                }
            } else {
                /* No waittime at all */
                wait = 0;
            }
            if (wait > 0) {
                logger.info("Waiting final waittime: " + wait);
                sleep(wait * 1000l, downloadLink);
            } else if (wait < -extraWaitSeconds) {
                /* User needed more time to solve the captcha so there is no waittime left :) */
                logger.info("Congratulations: Time to solve captcha was higher than waittime --> No waittime left");
            } else {
                /* No waittime at all */
                logger.info("Found no waittime");
            }
        }
    }

    /* https://stackoverflow.com/questions/10664434/escaping-special-characters-in-java-regular-expressions/25853507 */
    static Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

    static String escapeSpecialRegexChars(String str) {
        return SPECIAL_REGEX_CHARS.matcher(str).replaceAll("\\\\$0");
    }

    private HashMap<String, Object> getErrorKeyFromErrorMessage(final String errorStr) {
        if (StringUtils.isEmpty(errorStr)) {
            return null;
        }
        try {
            final String language_keys_json = br.getRegex("l\\s*=\\s*(\\{.*?\\});\\s*?return").getMatch(0);
            final HashMap<String, Object> language_keys_map = (HashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(language_keys_json);
            final Iterator<Entry<String, Object>> iterator = language_keys_map.entrySet().iterator();
            String found_key_for_errormessage = null;
            final HashMap<String, String> errorProperties = new HashMap<String, String>();
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                final String key = entry.getKey();
                final String langStr = (String) entry.getValue();
                final String[] dynamicVarsNames = new Regex(langStr, "\\[\\[\\[([^\\]]+)\\]\\]\\]").getColumn(0);
                if (dynamicVarsNames.length > 0) {
                    /* First escape alll special chars in our langString */
                    String langStr2 = escapeSpecialRegexChars(langStr);
                    /* Now we have to replace the placeholders so that we can match the whole String against errorStr. */
                    langStr2 = langStr2.replaceAll("\\\\\\[\\\\\\[\\\\\\[[^\\]]+\\\\\\]\\\\\\]\\\\\\]", "(.*?)");
                    // Pattern langStrEscaped = Pattern.compile(langStr.replaceAll("\\[\\[\\[[^\\]]+\\]\\]\\]", ".*?"));
                    final Regex langStrRegEx = new Regex(errorStr, langStr2);
                    if (!langStrRegEx.matches()) {
                        continue;
                    }
                    found_key_for_errormessage = key;
                    for (int index = 0; index < dynamicVarsNames.length; index++) {
                        final String dynamicVarName = dynamicVarsNames[index];
                        final String dynamicVarValue = langStrRegEx.getMatch(index);
                        errorProperties.put(dynamicVarName, dynamicVarValue);
                    }
                    break;
                } else {
                    /* Easy - static String */
                    if (langStr.equalsIgnoreCase(errorStr)) {
                        found_key_for_errormessage = key;
                        break;
                    }
                }
            }
            if (found_key_for_errormessage != null) {
                HashMap<String, Object> errorMap = new HashMap<String, Object>();
                errorMap.put("error_key", found_key_for_errormessage);
                if (!errorProperties.isEmpty()) {
                    errorMap.put("error_properties", errorProperties);
                }
                return errorMap;
            } else {
                logger.info("Failed to find language key for errormessage: " + errorStr);
                return null;
            }
        } catch (final Throwable e) {
            logger.log(e);
            return null;
        }
    }

    /* 2020-03-25: No plugin should ever have to override this. Please create a ticket before changing this! */
    private void checkErrorsLanguageIndependant(final DownloadLink link, final Account account) throws PluginException {
        String errorMsgURL = null;
        try {
            final UrlQuery query = UrlQuery.parse(br.getURL());
            errorMsgURL = query.get("e");
            if (errorMsgURL != null) {
                errorMsgURL = URLDecoder.decode(errorMsgURL, "UTF-8");
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        if (!StringUtils.isEmpty(errorMsgURL)) {
            logger.info("Found errormessage in current URL: " + errorMsgURL);
            final HashMap<String, Object> errorMap = getErrorKeyFromErrorMessage(errorMsgURL);
            if (errorMap == null) {
                /* Not all websites have (all) language keys present e.g. ultimbox.com */
                logger.info("Failed to find error_key --> Trying checkErrorsURLOld");
                checkErrorsURLOld(link, account);
                logger.info("checkErrorsURLOld did not do anything --> Throwing Exception ERROR_TEMPORARILY_UNAVAILABLE because of errorMsgURL: " + errorMsgURL);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error without errorkey: " + errorMsgURL);
            }
            final String errorkey = (String) errorMap.get("error_key");
            logger.info("Found key to errormessage: " + errorkey);
            HashMap<String, String> errorProperties = null;
            if (errorMap.containsKey("error_properties")) {
                /* This is really only for logging purposes */
                errorProperties = (HashMap<String, String>) errorMap.get("error_properties");
                final Iterator<Entry<String, String>> dynVarsIterator = errorProperties.entrySet().iterator();
                int counter = 0;
                while (dynVarsIterator.hasNext()) {
                    counter++;
                    final Entry<String, String> entry = dynVarsIterator.next();
                    final String key = entry.getKey();
                    final String value = entry.getValue();
                    logger.info("Found ErrorProperty " + counter + " | " + key + " : " + value);
                }
            }
            final long default_waittime = 15 * 60 * 1000l;
            /* Now handle errors */
            if (errorkey.equalsIgnoreCase("error_file_has_been_removed_by_admin") || errorkey.equalsIgnoreCase("error_file_has_been_removed_by_user") || errorkey.equalsIgnoreCase("error_file_has_been_removed_due_to_copyright") || errorkey.equalsIgnoreCase("error_file_has_expired")) {
                // VERIFIED
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, errorMsgURL);
            }
            /** premiumonly errorhandling */
            else if (errorkey.equalsIgnoreCase("error_you_must_register_for_a_premium_account_for_filesize")) {
                throw new AccountRequiredException(errorMsgURL);
            } else if (errorkey.equalsIgnoreCase("error_you_must_be_a_x_user_to_download_this_file")) {
                throw new AccountRequiredException(errorMsgURL);
            } else if (errorkey.equalsIgnoreCase("error_you_must_register_for_a_premium_account_for_filesize")) {
                throw new AccountRequiredException(errorMsgURL);
            } else if (errorkey.equalsIgnoreCase("error_file_is_not_publicly_shared")) {
                /* Very very rare case */
                logger.info("This file can only be downloaded by the initial uploader");
                throw new AccountRequiredException(errorMsgURL);
            } /** Limit errorhandling */
            else if (errorkey.equalsIgnoreCase("error_you_have_reached_the_download_limit")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errorMsgURL, default_waittime);
            } else if (errorkey.equalsIgnoreCase("error_you_have_reached_the_download_limit_this_file")) {
                /* "You do not have enough bandwidth left to download this file." */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorMsgURL, default_waittime);
            } else if (errorkey.equalsIgnoreCase("error_you_have_reached_the_maximum_daily_download_limit")) {
                /* "You have reached the maximum download bandwidth today, please upgrade or try again later." */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errorMsgURL, default_waittime);
            } else if (errorkey.equalsIgnoreCase("error_you_have_reached_the_maximum_band_width_per_day_in_the_last_24_hours")) {
                /* 2020-12-11: sundryshare.com */
                /* "You have reached the maximum permitted downloads  band width per day." */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errorMsgURL, 1 * 60 * 60 * 1000l);
            } else if (errorkey.equalsIgnoreCase("error_you_have_reached_the_maximum_daily_download_limit_this_file")) {
                /* "You do not have enough bandwidth left today to download this file." */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorMsgURL, default_waittime);
            } else if (errorkey.equalsIgnoreCase("error_you_have_reached_the_maximum_permitted_downloads_in_the_last_24_hours")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errorMsgURL, default_waittime);
            } else if (errorkey.equalsIgnoreCase("error_you_have_reached_the_max_permitted_downloads")) {
                /*
                 * "You have reached the maximum concurrent downloads. Please wait for your existing downloads to complete or register for a premium account above."
                 */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, errorMsgURL, 5 * 60 * 1000l);
            } else if (errorkey.equalsIgnoreCase("error_you_must_wait_between_downloads")) {
                // VERIFIED
                /* E.g. "30 minutes" */
                final String waitStr = errorProperties.get("WAITING_TIME_LABEL");
                long waittime = parseWaittime(waitStr);
                if (waittime <= 0) {
                    waittime = default_waittime;
                }
                if (waittime < 180000) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, errorMsgURL, waittime);
                } else if (account != null) {
                    throw new AccountUnavailableException(errorMsgURL, waittime);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errorMsgURL, waittime);
                }
            } else {
                logger.warning("Unknown errorkey: " + errorkey + " --> Trying checkErrorsURLOld");
                checkErrorsURLOld(link, account);
                logger.info("checkErrorsURLOld did not do anything --> Throwing Exception ERROR_TEMPORARILY_UNAVAILABLE because of errorMsgURL: " + errorMsgURL);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error: " + errorMsgURL + " | Errorkey: " + errorkey);
            }
        }
    }

    private long parseWaittime(final String src) {
        /*
         * Important: URL Might contain htmlencoded parts! Be sure that these RegExes are tolerant enough to get the information we need!
         */
        final String wait_hours = new Regex(src, "(\\d+)\\s*?hours?").getMatch(0);
        final String wait_minutes = new Regex(src, "(\\d+)\\s*?minutes?").getMatch(0);
        final String wait_seconds = new Regex(src, "(\\d+)\\s*?(?:seconds?|segundos)").getMatch(0);
        int minutes = 0, seconds = 0, hours = 0;
        if (wait_hours != null) {
            hours = Integer.parseInt(wait_hours);
        }
        if (wait_minutes != null) {
            minutes = Integer.parseInt(wait_minutes);
        }
        if (wait_seconds != null) {
            seconds = Integer.parseInt(wait_seconds);
        }
        final int extraWaittimeSeconds = 1;
        int waittime = ((3600 * hours) + (60 * minutes) + seconds + extraWaittimeSeconds) * 1000;
        if (waittime <= 0) {
            /* Fallback */
            logger.info("Waittime RegExes seem to be broken or given String does not contain any waittime");
        }
        return waittime;
    }

    public void checkErrors(final DownloadLink link, final Account account) throws PluginException {
        checkErrorsLanguageIndependant(link, account);
        /*
         * Old / fallback / English / additional errorhandling - please leave this here although checkErrorsLanguageIndependant should cover
         * most of all errors
         */
        checkErrorsURLOld(link, account);
        if (br.toString().equals("unknown user")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Unknown user'", 30 * 60 * 1000l);
        } else if (br.toString().equals("ERROR: Wrong IP")) {
            /*
             * 2019-07-05: New: rare case but this can either happen randomly or when you try to resume a stored downloadurl with a new IP.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Wrong IP'", 5 * 60 * 1000l);
        }
    }

    protected void loggedInOrException(final Account account) throws PluginException {
        if (account == null) {
            /* Programmer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (!this.isLoggedin()) {
            throw new AccountUnavailableException("Session expired?", 5 * 60 * 1000l);
        }
    }

    protected void checkErrorsLastResort(final DownloadLink link, final Account account) throws PluginException {
        logger.info("Last resort errorhandling");
        if (account != null) {
            this.loggedInOrException(account);
        } else if (new Regex(br.getURL(), "^https?://[^/]+/?$").matches()) {
            /* Handle redirect to mainpage as premiumonly */
            throw new AccountRequiredException();
        }
        logger.warning("Unknown error happened");
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Deprecated
    /** It is intended to replace this with checkErrorsLanguageIndependant! */
    private void checkErrorsURLOld(final DownloadLink link, final Account account) throws PluginException {
        final String url = getCurrentURLDecoded();
        if (br.containsHTML("Error: Too many concurrent download requests")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 3 * 60 * 1000l);
        } else if (new Regex(br.getURL(), Pattern.compile(".*?e=You\\+have\\+reached\\+the\\+maximum\\+concurrent\\+downloads.*?", Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max. simultan downloads limit reached, wait to start more downloads", 1 * 60 * 1000l);
        } else if (new Regex(br.getURL(), Pattern.compile(".*e=.*Could\\+not\\+open\\+file\\+for\\+reading.*", Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
        } else if (url != null && new Regex(url, Pattern.compile(".*?\\?e=(You must wait |Você deve esperar).*?", Pattern.CASE_INSENSITIVE)).matches()) {
            final long extraWaittimeMilliseconds = 1000;
            long waittime = this.parseWaittime(url);
            if (waittime <= 0) {
                /* Fallback */
                logger.info("Waittime RegExes seem to be broken - using default waittime");
            }
            waittime += extraWaittimeMilliseconds;
            logger.info("Detected reconnect waittime (milliseconds): " + waittime);
            /* Not enough wait time to reconnect -> Wait short and retry */
            if (waittime < 180000) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait until new downloads can be started", waittime);
            } else if (account != null) {
                throw new AccountUnavailableException("Download limit reached", waittime);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        } else if (url != null && new Regex(url, Pattern.compile(".*?(You must register for a premium account to|Ten plik jest za duży do pobrania dla darmowego użytkownika|/register\\.).+", Pattern.CASE_INSENSITIVE)).matches()) {
            throw new AccountRequiredException();
        } else if (br.getURL().contains("You+have+reached+the+maximum+permitted+downloads+in")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily limit reached", 3 * 60 * 60 * 1001l);
        }
    }

    /** Handles all kinds of error-responsecodes! */
    protected void checkResponseCodeErrors(final URLConnectionAdapter con) throws PluginException {
        if (con != null) {
            final long responsecode = con.getResponseCode();
            if (responsecode == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else if (responsecode == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
            } else if (responsecode == 416) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 2 * 60 * 1000l);
            } else if (responsecode == 429) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 429 connection limit reached, please contact our support!", 5 * 60 * 1000l);
            }
        }
    }

    /**
     * @return true = file is offline, false = file is online </br>
     *         Be sure to always call checkErrors before calling this!
     * @throws Exception
     */
    protected boolean isOfflineWebsite(final DownloadLink link) throws Exception {
        /* TODO: Consider checking for fuid in URL too in the future --> This might be a good offline indicator */
        // final String fid = this.getFUIDFromURL(link);
        // final boolean currentURLContainsFID = br.getURL().contains(fid);
        final boolean isDownloadable = this.getContinueLink() != null;
        final boolean isFileWebsite = br.containsHTML("class=\"downloadPageTable(V2)?\"") || br.containsHTML("class=\"download\\-timer\"");
        final boolean isErrorPage = br.getURL().contains("/error.html") || br.getURL().contains("/index.html");
        final boolean isOffline404 = br.getHttpConnection().getResponseCode() == 404;
        if ((!isFileWebsite || isErrorPage || isOffline404) && !isDownloadable) {
            return true;
        }
        return false;
    }

    protected String getCurrentURLDecoded() {
        if (br.getURL() != null) {
            String currentURL = br.getURL();
            if (Encoding.isUrlCoded(currentURL)) {
                try {
                    currentURL = URLDecoder.decode(currentURL, "UTF-8");
                } catch (final Throwable e) {
                    logger.info("Failed to urldecode current URL: " + br.getURL());
                }
            }
            return currentURL;
        }
        return null;
    }

    /** Returns pre-download-waittime (seconds) from inside HTML. */
    public String regexWaittime() {
        String ttt = this.br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
        if (ttt == null) {
            ttt = this.br.getRegex("var\\s*?seconds\\s*=\\s*(\\d+);").getMatch(0);
        }
        return ttt;
    }

    protected String checkDirectLink(final DownloadLink link, final Account account) throws InterruptedException, PluginException {
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        final String dllink = link.getStringProperty(directlinkproperty);
        if (dllink != null) {
            final boolean resume = this.isResumeable(link, account);
            final int maxchunks = this.getMaxChunks(account);
            final Browser br2 = this.br.cloneBrowser();
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            boolean valid = false;
            try {
                // con = br2.openHeadConnection(dllink);
                this.dl = jd.plugins.BrowserAdapter.openDownload(br2, link, dllink, resume, maxchunks);
                con = dl.getConnection();
                if (br2.getHttpConnection().getResponseCode() == 429) {
                    logger.info("Stored directurl lead to 429 | too many connections");
                    try {
                        br2.followConnection(true);
                    } catch (IOException e) {
                        logger.log(e);
                    }
                    /*
                     * Too many connections but that does not mean that our downloadlink is valid. Accept it and if it still returns 429 on
                     * download-attempt this error will get displayed to the user.
                     */
                    valid = true;
                    return dllink;
                } else if (!looksLikeDownloadableContent(con)) {
                    try {
                        br2.followConnection(true);
                    } catch (IOException e) {
                        logger.log(e);
                    }
                    throw new IOException();
                } else {
                    valid = true;
                    return dllink;
                }
            } catch (final InterruptedException e) {
                throw e;
            } catch (final Exception e) {
                link.setProperty(directlinkproperty, Property.NULL);
                logger.log(e);
                return null;
            } finally {
                if (!valid) {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                    this.dl = null;
                }
            }
        } else {
            return null;
        }
    }

    protected String getProtocol() {
        if ((this.br.getURL() != null && this.br.getURL().contains("https://")) || supports_https()) {
            return "https://";
        } else {
            return "http://";
        }
    }

    protected Browser prepBrowserWebsite(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 416, 429 });
        if (enable_random_user_agent()) {
            if (agent.get() == null) {
                agent.set(UserAgents.stringUserAgent());
            }
            br.getHeaders().put("User-Agent", agent.get());
        }
        return br;
    }

    protected String getAccountNameSpaceLogin() {
        return "/login.html";
    }

    protected String getAccountNameSpaceHome() {
        return "/account_home.html";
    }

    protected String getAccountNameSpaceUpgrade() {
        return "/upgrade.html";
    }

    protected void loginWebsite(final Account account, boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBrowser(this.br, account.getHoster());
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !force) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        return;
                    }
                    logger.info("Verifying login-cookies");
                    getPage(this.getMainPage() + this.getAccountNameSpaceUpgrade());
                    if (isLoggedin()) {
                        logger.info("Successfully logged in via cookies");
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        /* Set/Update account-type */
                        if (this.isPremiumAccount(account, br)) {
                            setAccountLimitsByType(account, AccountType.PREMIUM);
                        } else {
                            setAccountLimitsByType(account, AccountType.FREE);
                        }
                        return;
                    } else {
                        logger.info("Failed to login via cookies");
                    }
                }
                logger.info("Performing full login");
                getPage(this.getProtocol() + this.getHost() + getAccountNameSpaceLogin());
                Form loginform;
                /*
                 * TODO: Optimize recognition of required login type see all plugins that override enforce_old_login_method (not many - low
                 * priority)
                 */
                if (br.containsHTML("flow\\-login\\.js") && !enforce_old_login_method()) {
                    final String loginstart = new Regex(br.getURL(), "(https?://(www\\.)?)").getMatch(0);
                    /* New (ajax) login method - mostly used - example: iosddl.net */
                    logger.info("Using new login method");
                    /* These headers are important! */
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                    loginform = br.getFormbyProperty("id", "form_login");
                    if (loginform == null) {
                        logger.info("Fallback to custom built loginform");
                        loginform = new Form();
                        loginform.put("submitme", "1");
                    }
                    loginform.put("username", Encoding.urlEncode(account.getUser()));
                    loginform.put("password", Encoding.urlEncode(account.getPass()));
                    final String action = loginstart + this.getHost() + "/ajax/_account_login.ajax.php";
                    loginform.setAction(action);
                    if (loginform.containsHTML("class=\"g\\-recaptcha\"")) {
                        /* E.g. crazyshare.cc */
                        final DownloadLink dlinkbefore = this.getDownloadLink();
                        if (dlinkbefore == null) {
                            this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                        }
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        if (dlinkbefore != null) {
                            this.setDownloadLink(dlinkbefore);
                        }
                        loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    }
                    submitForm(loginform);
                    if (!br.containsHTML("\"login_status\":\"success\"")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else {
                    /* Old login method - rare case! Example: udrop.net --> 2020-08-07: Website is dead */
                    logger.info("Using old login method");
                    loginform = br.getFormbyProperty("id", "form_login");
                    if (loginform == null) {
                        loginform = br.getFormbyKey("loginUsername");
                    }
                    if (loginform == null) {
                        logger.info("Fallback to custom built loginform");
                        loginform = new Form();
                        loginform.setMethod(MethodType.POST);
                        loginform.put("submit", "Login");
                        loginform.put("submitme", "1");
                    }
                    if (loginform.hasInputFieldByName("loginUsername") && loginform.hasInputFieldByName("loginPassword")) {
                        /* 2019-07-08: Rare case: Example: freaktab.org */
                        loginform.put("loginUsername", Encoding.urlEncode(account.getUser()));
                        loginform.put("loginPassword", Encoding.urlEncode(account.getPass()));
                    } else if (loginform.hasInputFieldByName("email")) {
                        /* 2020-04-30: E.g. filemia.com */
                        loginform.put("email", Encoding.urlEncode(account.getUser()));
                        loginform.put("password", Encoding.urlEncode(account.getPass()));
                    } else {
                        loginform.put("username", Encoding.urlEncode(account.getUser()));
                        loginform.put("password", Encoding.urlEncode(account.getPass()));
                    }
                    /* 2019-07-31: At the moment only this older login method supports captchas. Examplehost: uploadship.com */
                    if (br.containsHTML("solvemedia\\.com/papi/")) {
                        /* Handle login-captcha if required */
                        DownloadLink dlinkbefore = this.getDownloadLink();
                        try {
                            final DownloadLink dl_dummy;
                            if (dlinkbefore != null) {
                                dl_dummy = dlinkbefore;
                            } else {
                                dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                                this.setDownloadLink(dl_dummy);
                            }
                            final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                            if (br.containsHTML("api\\-secure\\.solvemedia\\.com/")) {
                                sm.setSecure(true);
                            }
                            File cf = null;
                            try {
                                cf = sm.downloadCaptcha(getLocalCaptchaFile());
                            } catch (final Exception e) {
                                if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                                    throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                                }
                                throw e;
                            }
                            final String code = getCaptchaCode("solvemedia", cf, dl_dummy);
                            final String chid = sm.getChallenge(code);
                            loginform.put("adcopy_challenge", chid);
                            loginform.put("adcopy_response", "manual_challenge");
                        } finally {
                            if (dlinkbefore != null) {
                                this.setDownloadLink(dlinkbefore);
                            }
                        }
                    }
                    submitForm(loginform);
                    if (br.containsHTML(">\\s*Your username and password are invalid<") || !isLoggedin()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    protected boolean isLoggedin() {
        return br.containsHTML("/logout\\.html\"");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (supports_api()) {
            return fetchAccountInfoAPI(account, account.getUser(), account.getPass());
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    protected String getAccountEditURL() {
        return "/account_edit.html";
    }

    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        // if (br.getURL() == null || !br.getURL().contains(getAccountHomeNameSpace())) {
        // getPage(getAccountHomeNameSpace());
        // }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* TODO: 2020-09-02: Try to parse API tokens abd obtain account information from API. */
            /* TODO: Make this work for all YetiShare websites that support their API */
            final AccountInfo apiAccInfo = fetchAccountInfoWebsiteAPI(account);
            if (apiAccInfo != null) {
                logger.info("Found AccountInfo via API --> Prefer this over website AccountInfo");
                return apiAccInfo;
            }
        }
        getPage(this.getAccountNameSpaceUpgrade());
        String expireStr = regexExpireDate();
        if (expireStr == null) {
            /*
             * 2019-03-01: As far as we know, EVERY premium account will have an expire-date given but we will still accept accounts for
             * which we fail to find the expire-date.
             */
            logger.info("Failed to find expire-date --> Probably a FREE account");
            this.setAccountLimitsByType(account, AccountType.FREE);
            return ai;
        }
        long expire_milliseconds = parseExpireTimeStamp(account, expireStr);
        final boolean isPremium = expire_milliseconds > System.currentTimeMillis();
        /* If the premium account is expired we'll simply accept it as a free account. */
        if (!isPremium) {
            /* Expired premium == FREE */
            this.setAccountLimitsByType(account, AccountType.FREE);
            // ai.setStatus("Registered (free) user");
        } else {
            ai.setValidUntil(expire_milliseconds, this.br);
            this.setAccountLimitsByType(account, AccountType.PREMIUM);
            // ai.setStatus("Premium account");
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    protected boolean isPremiumAccount(final Account account, final Browser br) {
        final String expireStr = regexExpireDate();
        if (expireStr == null) {
            return false;
        } else {
            final long expire_milliseconds = parseExpireTimeStamp(account, expireStr);
            return expire_milliseconds > System.currentTimeMillis();
        }
    }

    protected String regexExpireDate() {
        String expireStr = br.getRegex("Reverts To Free Account\\s*:\\s*</td>\\s*<td>\\s*(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expireStr == null) {
            expireStr = br.getRegex("Reverts To Free Account\\s*:\\s*</span>\\s*<input[^>]*value\\s*=\\s*\"(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            if (expireStr == null) {
                /* More wide RegEx to be more language independant (e.g. required for freefile.me) */
                expireStr = br.getRegex(">\\s*(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})\\s*<").getMatch(0);
            }
        }
        return expireStr;
    }

    /** Tries to auto-find API keys in website HTML code and return account information from API! */
    protected AccountInfo fetchAccountInfoWebsiteAPI(final Account account) {
        /* TODO: 2020-09-02: Try to parse API tokens abd obtain account information from API. */
        /* TODO: Make this work for all YetiShare websites that support their API */
        try {
            final Browser brc = br.cloneBrowser();
            this.getPage(brc, getAccountEditURL());
            String key1 = null;
            String key2 = null;
            final Form[] forms = brc.getForms();
            for (Form form : forms) {
                final InputField fieldKey1 = form.getInputField("key1");
                final InputField fieldKey2 = form.getInputField("key2");
                if (fieldKey1 != null && fieldKey2 != null) {
                    key1 = fieldKey1.getValue();
                    key2 = fieldKey2.getValue();
                    break;
                }
            }
            if (this.isAPICredential(key1) && this.isAPICredential(key2)) {
                logger.info("Found possibly valid API login credentials");
                try {
                    final AccountInfo apiAccInfo = this.fetchAccountInfoAPI(account, key1, key2);
                    if (apiAccInfo != null) {
                        logger.info("API AccountInfo found");
                        return apiAccInfo;
                    } else {
                        logger.info("Failed to find API AccountInfo");
                    }
                } catch (final Throwable e) {
                    logger.info("API handling inside website handling failed");
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void setAccountLimitsByType(final Account account, final AccountType type) {
        account.setType(type);
        switch (type) {
        case PREMIUM:
            account.setConcurrentUsePossible(true);
            account.setMaxSimultanDownloads(this.getMaxSimultanPremiumDownloadNum());
            break;
        case FREE:
            /* All accounts get the same (IP-based) downloadlimits --> Simultaneous free account usage makes no sense! */
            account.setConcurrentUsePossible(false);
            account.setMaxSimultanDownloads(this.getMaxSimultaneousFreeAccountDownloads());
            break;
        case UNKNOWN:
        default:
            account.setConcurrentUsePossible(false);
            account.setMaxSimultanDownloads(1);
            break;
        }
    }

    protected long parseExpireTimeStamp(final Account account, final String expireString) {
        if (expireString == null) {
            return -1;
        }
        /** TODO: Try to auto-find correct format based on html/js/website language (??) */
        final String first = new Regex(expireString, "^(\\d+)/").getMatch(0);
        final String second = new Regex(expireString, "^\\d+/(\\d+)").getMatch(0);
        final int firstNumber = Integer.parseInt(first);
        final int secondNumber = Integer.parseInt(second);
        long timestamp_daysfirst = 0;
        long timestamp_dayssecond = 0;
        /* Days first */
        if (secondNumber <= 12) {
            timestamp_daysfirst = TimeFormatter.getMilliSeconds(expireString, "dd/MM/yyyy hh:mm:ss", Locale.ENGLISH);
        }
        /* Months first */
        if (firstNumber <= 12) {
            timestamp_dayssecond = TimeFormatter.getMilliSeconds(expireString, "MM/dd/yyyy hh:mm:ss", Locale.ENGLISH);
        }
        if (timestamp_daysfirst > timestamp_dayssecond) {
            return timestamp_daysfirst;
        } else {
            return timestamp_dayssecond;
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (this.supports_api()) {
            this.handleDownloadAPI(link, account);
        } else {
            requestFileInformation(link, account, true);
            loginWebsite(account, false);
            br.setFollowRedirects(false);
            getPage(link.getPluginPatternMatcher());
            handleDownloadWebsite(link, account);
        }
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
        if (weak_fallback_filename != null) {
            link.setName(weak_fallback_filename);
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MFScripts_YetiShare;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* *************************** PUT API RELATED METHODS HERE *************************** */
    protected static final String PROPERTY_API_ACCESS_TOKEN          = "ACCESS_TOKEN";
    protected static final String PROPERTY_API_ACCOUNT_ID            = "ACCOUNT_ID";
    protected static final String API_LOGIN_HAS_BEEN_SUCCESSFUL_ONCE = "API_LOGIN_HAS_BEEN_SUCCESSFUL_ONCE";

    /** true = API will be used to login and for (premium/free)-account downloading. */
    protected boolean supports_api() {
        return false;
    }

    protected String getAPIBase() {
        return "https://" + this.getHost() + "/api/v2";
    }

    protected String getAccountOverviewURL() {
        return this.getMainPage() + "/account_edit.html";
    }

    protected Browser prepBrowserAPI(final Browser br) {
        // br.setAllowedResponseCodes(new int[] { 416, 429 });
        // if (enable_random_user_agent()) {
        // if (agent.get() == null) {
        // agent.set(UserAgents.stringUserAgent());
        // }
        // br.getHeaders().put("User-Agent", agent.get());
        // }
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    /** 2020-08-26: Same pattern for user & pw ("key1" & "key2") */
    protected boolean isAPICredential(final String str) {
        return str != null && str.matches("[A-Za-z0-9]{64}");
    }

    protected String getAPIAccessToken(final Account account, final String key1, final String key2) {
        return account.getStringProperty(PROPERTY_API_ACCESS_TOKEN + Hash.getSHA256(key1 + ":" + key2));
    }

    protected String getAPIAccountID(final Account account, final String key1, final String key2) {
        return account.getStringProperty(PROPERTY_API_ACCOUNT_ID + Hash.getSHA256(key1 + ":" + key2));
    }

    /**
     * 2020-05-14: https://fhscript.com/admin/api_documentation.php?username=admin&password=password&submitme=1 </br>
     */
    protected AccountInfo fetchAccountInfoAPI(final Account account, final String key1, final String key2) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.loginAPI(account, key1, key2, true);
        final Map<String, Object> postAccountInfo = new HashMap<String, Object>();
        postAccountInfo.put("access_token", getAPIAccessToken(account, key1, key2));
        postAccountInfo.put("account_id", getAPIAccountID(account, key1, key2));
        if (this.br.getURL() == null || !this.br.getURL().contains("/account/info")) {
            this.postPageRaw(this.getAPIBase() + "/account/info", JSonStorage.serializeToJson(postAccountInfo), true);
            checkErrorsAPI(br, null, account);
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String server_timeStr = (String) entries.get("_datetime");
        entries = (Map<String, Object>) entries.get("data");
        /* TODO: Maybe find a way to set this username as account username so that it looks better in the account overview in JD */
        // final String username = (String) entries.get("username");
        // final String status = (String) entries.get("status"); --> Mostly "active" (also free accounts)
        final String datecreatedStr = (String) entries.get("datecreated");
        final String premiumExpireDateStr = (String) entries.get("paidExpiryDate");
        long premiumExpireMilliseconds = 0;
        final long currentTime;
        if (server_timeStr != null && server_timeStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            currentTime = TimeFormatter.getMilliSeconds(server_timeStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        } else {
            /* Fallback */
            currentTime = System.currentTimeMillis();
        }
        if (premiumExpireDateStr != null && premiumExpireDateStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            premiumExpireMilliseconds = TimeFormatter.getMilliSeconds(premiumExpireDateStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        this.postPageRaw(this.getAPIBase() + "/account/package", JSonStorage.serializeToJson(postAccountInfo), true);
        Map<String, Object> packageInfo = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        packageInfo = (Map<String, Object>) packageInfo.get("data");
        final String accountType = (String) packageInfo.get("label");
        final String level_type = (String) packageInfo.get("level_type");
        checkErrorsAPI(br, null, account);
        final long premiumDurationMilliseconds = premiumExpireMilliseconds - currentTime;
        if (premiumExpireMilliseconds > currentTime || StringUtils.equalsIgnoreCase(level_type, "paid")) {
            account.setType(AccountType.PREMIUM);
            if (premiumExpireMilliseconds > currentTime) {
                ai.setValidUntil(System.currentTimeMillis() + premiumDurationMilliseconds);
            }
        } else {
            /* Free- or expired premium account */
            account.setType(AccountType.FREE);
        }
        ai.setStatus(accountType);
        final Object concurrent_downloadsO = packageInfo.get("concurrent_downloads");
        if (concurrent_downloadsO != null && concurrent_downloadsO instanceof Integer) {
            final int simultaneousDownloads = ((Integer) packageInfo.get("concurrent_downloads")).intValue();
            if (simultaneousDownloads > 0) {
                account.setMaxSimultanDownloads(simultaneousDownloads);
            }
        }
        /* Now set unnecessary data */
        if (datecreatedStr != null && datecreatedStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            ai.setCreateTime(TimeFormatter.getMilliSeconds(datecreatedStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        }
        return ai;
    }

    protected void loginAPI(final Account account, final String key1, final String key2, final boolean verifyToken) throws Exception {
        String access_token = this.getAPIAccessToken(account, key1, key2);
        String account_id = this.getAPIAccountID(account, key1, key2);
        if (!StringUtils.isEmpty(access_token) && !StringUtils.isEmpty(account_id)) {
            logger.info("Trying to re-use stored access_token");
            if (!verifyToken) {
                logger.info("Trust existing access_token");
                return;
            }
            final Map<String, Object> postAccountInfo = new HashMap<String, Object>();
            postAccountInfo.put("access_token", access_token);
            postAccountInfo.put("account_id", account_id);
            this.postPageRaw(this.getAPIBase() + "/account/info", JSonStorage.serializeToJson(postAccountInfo), true);
            try {
                Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                entries = (Map<String, Object>) entries.get("data");
                final int json_accountID = (int) JavaScriptEngineFactory.toLong(entries.get("id"), -1);
                /* Compare accountID with stored accountID --> If it matches, we trust login to be successful */
                if (json_accountID == Integer.parseInt(account_id)) {
                    logger.info("Successfully re-used access_token");
                    return;
                } else {
                    logger.info("Failed to re-use access_token");
                }
            } catch (final Throwable e) {
                /*
                 * 2020-09-10: E.g. misleading response on expired access_token: {"message":"Username could not be found.","result":false}
                 */
                logger.info("Failed to re-use access_token");
            }
        }
        logger.info("Performing full login");
        if (!this.isAPICredential(key1) || !this.isAPICredential(key2)) {
            showAPILoginInformation();
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid API credentials", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final Map<String, Object> postLogin = new HashMap<String, Object>();
        postLogin.put("key1", key1);
        postLogin.put("key2", key2);
        this.postPageRaw(this.getAPIBase() + "/authorize", JSonStorage.serializeToJson(postLogin), true);
        access_token = PluginJSonUtils.getJson(br, "access_token");
        account_id = PluginJSonUtils.getJson(br, "account_id");
        /*
         * 2020-08-27: API can basically return anything except expected json --> Do not check for errors here - just check for the expected
         * token --> Account should be invalid of token is not available. Only check for errors if this account has been valid before
         * already!
         */
        if (account.getBooleanProperty(API_LOGIN_HAS_BEEN_SUCCESSFUL_ONCE + Hash.getSHA256(key1 + ":" + key2), false)) {
            checkErrorsAPI(br, null, account);
        }
        if (StringUtils.isEmpty(access_token) || StringUtils.isEmpty(account_id) || !account_id.matches("\\d+")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        account.setProperty(PROPERTY_API_ACCESS_TOKEN + Hash.getSHA256(key1 + ":" + key2), access_token);
        account.setProperty(PROPERTY_API_ACCOUNT_ID + Hash.getSHA256(key1 + ":" + key2), account_id);
        account.setProperty(API_LOGIN_HAS_BEEN_SUCCESSFUL_ONCE + Hash.getSHA256(key1 + ":" + key2), true);
    }

    private Thread showAPILoginInformation() {
        final String host = this.getHost();
        final String account_overview_url = getAccountOverviewURL();
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = host + " - Login";
                        message += "Hallo liebe(r) " + host + " NutzerIn\r\n";
                        message += "Um deinen " + host + " Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch geschieht und logge dich auf der Webseite ein:\r\n\t'" + account_overview_url + "'\t\r\n";
                        message += "2. Scrolle herunter bis du die Eingabefelder \"Key 1\" und \"Key 2\" siehst.\r\n";
                        message += "3. Klicke rechts in beiden Eingabefeldern auf \"Generate\".\r\n";
                        message += "4. Scrolle bis ans Ende der Webseite und klicke auf \"Update Account\".\r\n";
                        message += "5. Wechsle in JD, schließe diesen Dialog und öffne erneut die Maske um einen Account für diesen Anbieter hinzuzufügen.\r\n";
                        message += "Gib den Wert von \"Key 1\" als Benutzername- und den Wert von \"Key 2\" als Passwort ein und bestätige deine Eingaben.\r\n";
                        message += "Dein Account sollte nach einigen Sekunden von JDownloader akzeptiert werden.\r\n";
                    } else {
                        title = host + " - Login";
                        message += "Hello dear " + host + " user\r\n";
                        message += "In order to use your account of this service in JDownloader, you need to follow these steps:\r\n";
                        message += "1. Open this URL in your browser if it is not opened automatically and login in your browser:\r\n\t'" + account_overview_url + "'\t\r\n";
                        message += "2. Scroll down until you see the fields \"Key 1\" and \"Key 2\".\r\n";
                        message += "3. For each of both fields there is a \"Generate\" button on the right side - click both of them.\r\n";
                        message += "4. Scroll down all the way and confirm your generated keys by clicking on \"Update Account\".\r\n";
                        message += "5. Now go back into JD, close this dialog and re-open the \"Add account\" dialog for this host.\r\n";
                        message += "Enter the values of \"Key 1\" an \"Key 2\" into the username & password fields and confirm.\r\n";
                        message += "Your account should be accepted in JDownloader within a few seconds.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(account_overview_url);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /*
     * This API call only works with self-uploaded file whenever the internal file-id is known --> It is of no use for us! TODO: Re-check
     * this before using it in any official YetiShare plugin!
     */
    protected void handleDownloadAPI(final DownloadLink link, final Account account) throws StorageException, Exception {
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        String dllink = this.checkDirectLink(link, account);
        if (dllink == null) {
            this.loginAPI(account, account.getUser(), account.getPass(), false);
            final Map<String, Object> postDownload = new HashMap<String, Object>();
            postDownload.put("access_token", this.getAPIAccessToken(account, account.getUser(), account.getPass()));
            postDownload.put("account_id", this.getAPIAccountID(account, account.getUser(), account.getPass()));
            postDownload.put("file_id", this.getFUID(link));
            this.postPageRaw(this.getAPIBase() + "/file/download", JSonStorage.serializeToJson(postDownload), true);
            this.checkErrorsAPI(this.br, link, account);
            dllink = PluginJSonUtils.getJson(this.br, "download_url");
            if (StringUtils.isEmpty(dllink)) {
                /* We're using an API --> Never throw plugin defect! */
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl");
            }
            final boolean resume = this.isResumeable(link, account);
            final int maxchunks = this.getMaxChunks(account);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        }
        final URLConnectionAdapter con = dl.getConnection();
        /*
         * Save directurl before download-attempt as it should be valid even if it e.g. fails because of server issue 503 (= too many
         * connections) --> Should work fine after the next try.
         */
        link.setProperty(directlinkproperty, con.getURL().toString());
        try {
            checkResponseCodeErrors(con);
        } catch (final PluginException e) {
            try {
                br.followConnection(true);
            } catch (IOException ioe) {
                throw Exceptions.addSuppressed(e, ioe);
            }
            throw e;
        }
        if (!looksLikeDownloadableContent(con)) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            checkErrors(link, account);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to downloadable content");
        }
        dl.setFilenameFix(isContentDispositionFixRequired(dl, con, link));
        dl.startDownload();
    }

    protected void checkErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        /** TODO: Add functionality */
        Map<String, Object> entries = null;
        try {
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        } catch (final Throwable e) {
            /* API response is not json */
            throw new AccountUnavailableException("Invalid API response", 1 * 60 * 1000l);
        }
        /* Collection of errormessages (from the only testable YetiShare API, oxycloud.pl) */
        /* E.g. {"message":"Username could not be found.","result":false} */
        /* {"status":"error","response":"User not found.","_datetime":"2020-09-03 13:48:46"} */
        /* {"success":false,"message":"You can`t download files of this size."} */
        boolean result = true;
        String msg = null;
        try {
            result = ((Boolean) entries.get("result")).booleanValue();
            msg = (String) entries.get("message");
            if (StringUtils.isEmpty(msg)) {
                msg = (String) entries.get("response");
            }
        } catch (final Throwable e) {
            /* Try to parse other kinds of error responses */
            try {
                result = ((Boolean) entries.get("success")).booleanValue();
                msg = (String) entries.get("message");
            } catch (final Throwable e2) {
            }
        }
        if (!result) {
            if (StringUtils.isEmpty(msg)) {
                msg = "Unknown error";
            }
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, msg, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        }
    }
}