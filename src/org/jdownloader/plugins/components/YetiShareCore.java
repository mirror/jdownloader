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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.appwork.storage.StorageException;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.hcaptcha.AbstractHCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.InputField;
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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class YetiShareCore extends antiDDoSForHost {
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
    // return this.rewriteHost(getPluginDomains(), host);
    // }
    public static final String getDefaultAnnotationPatternPart() {
        return "/(?!folder|shared|account|upgrade|faq|register)[A-Za-z0-9]+(?:/[^/<>]+)?";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + YetiShareCore.buildHostsPatternPart(domains) + YetiShareCore.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Override this and add dead domains so upper handling can auto update added URLs and change domain if it contains a dead domain. This
     * way a lot of "old" URLs will continue to work in JD while they may fail in browser.
     */
    protected List<String> getDeadDomains() {
        return null;
    }

    /**
     * For sites which use this script: http://www.yetishare.com/<br />
     * YetiShareCore Version 2.0.1.0-psp<br />
     * mods: see overridden functions in host plugins<br />
     * limit-info:<br />
     * captchatype: null, solvemedia, reCaptchaV2, hcaptcha<br />
     * Another alternative method of linkchecking (displays filename only): host.tld/<fid>~s (statistics) 2019-06-12: Consider adding API
     * support: https://fhscript.com/api Examples for websites which have the API enabled (but not necessarily unlocked for all users,
     * usually only special-uploaders): secufiles.com <br />
     * 2020-03-30: I failed to make ANY successful API tests. 100% of all websites which support this API are running a broken version!
     */
    @Override
    public String getAGBLink() {
        if (isNewYetiShareVersion(null)) {
            return this.getMainPage((String) null) + "/terms";
        } else {
            return this.getMainPage((String) null) + "/terms.html";
        }
    }

    public String getPurchasePremiumURL() {
        if (isNewYetiShareVersion(null)) {
            return this.getMainPage((String) null) + "/upgrade";
        } else {
            return this.getMainPage((String) null) + "/upgrade.html";
        }
    }

    // private static final boolean enable_regex_stream_url = true;
    private static Map<String, String> agent                             = new HashMap<String, String>();
    public static final String         PROPERTY_INTERNAL_FILE_ID         = "INTERNALFILEID";
    public static final String         PROPERTY_UPLOAD_DATE_RAW          = "UPLOADDATE_RAW";
    public static final String         PROPERTY_IS_NEW_YETISHARE_VERSION = "is_new_yetishare_version";

    @Override
    public String getLinkID(DownloadLink link) {
        final String fuid = getFUID(link);
        if (fuid != null) {
            return this.getHost() + "://" + fuid;
        } else {
            return super.getLinkID(link);
        }
    }

    /**
     * Returns the desired host. Override is required in some cases where given host can contain unwanted subdomains.
     */
    protected String getCorrectHost(final DownloadLink link, URL url) {
        return url.getHost();
    }

    protected String getContentURL(final DownloadLink link) {
        /* Link cleanup, but respect users protocol choosing or forced protocol */
        if (link == null) {
            return null;
        }
        final String originalURL = link.getPluginPatternMatcher();
        if (originalURL == null) {
            return null;
        }
        try {
            final URL url = new URL(originalURL);
            final String urlHost = getCorrectHost(link, url);
            final List<String> deadDomains = this.getDeadDomains();
            final String protocol = getProtocol(originalURL);
            if (deadDomains == null || !deadDomains.contains(urlHost)) {
                /* Only correct domains if we know they're dead. */
                return protocol + url.getHost() + url.getPath();
            }
            final String pluginHost = this.getHost();
            final String host;
            if (StringUtils.equalsIgnoreCase(urlHost, pluginHost)) {
                /* E.g. down.example.com -> down.example.com */
                host = urlHost;
            } else {
                /* e.g. down.xx.com -> down.yy.com, keep subdomain(s) */
                host = urlHost.replaceFirst("(?i)" + Pattern.quote(Browser.getHost(url, false)) + "$", pluginHost);
            }
            final String hostCorrected = this.appendWWWIfRequired(host);
            return protocol + hostCorrected + url.getPath();
        } catch (final MalformedURLException e) {
            LogController.getRebirthLogger(logger).log(e);
        }
        return originalURL;
    }

    /** Adds "www." to given host if it doesn't already contain that and if it doesn't contain any other subdomain. */
    protected String appendWWWIfRequired(final String host) {
        if (!requiresWWW() || StringUtils.startsWithCaseInsensitive(host, "www.")) {
            // do not modify host
            return host;
        } else {
            final String hostTld = Browser.getHost(host, false);
            if (!StringUtils.equalsIgnoreCase(host, hostTld)) {
                // keep subdomain
                return host;
            } else {
                // add www.
                return "www." + host;
            }
        }
    }

    /**
     * Raturns URL to alternative page from which content information can be obtained. </br>
     * Structure: https://domain.tld/<fuid>~i
     */
    protected String getInfoPageURL(final DownloadLink link) {
        final String url = this.getContentURL(link);
        return new Regex(url, "(?i)(https?://[^/]+)").getMatch(0) + "/" + this.getFUID(link) + "~i";
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

    /** Returns direct-link-property-String for current download mode based on account availability and account type. */
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
    public boolean supports_availablecheck_over_info_page(final DownloadLink link) {
        return true;
    }

    /**
     * @return default: true: Availablecheck: Look for filesize inside HTML code. Disable this if filesize is not given in html and/or html
     *         contains similar traits and wrong string gets picked up.
     */
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
    protected boolean requiresWWW() {
        return true;
    }

    /**
     * @return true: Use random User-Agent. <br />
     *         false: Use Browsers' default User-Agent. <br />
     *         default: false
     */
    public boolean enableRandomUserAgent() {
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

    /** Returns empty StringArray for filename, filesize, [more information in the future?] */
    protected String[] getFileInfoArray() {
        return new String[2];
    }

    @Override
    public boolean enoughTrafficFor(final DownloadLink link, final Account account) throws Exception {
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        final String dllink = link.getStringProperty(directlinkproperty, null);
        /*
         * E.g. account doesn't have enough traffic left but we still got a stored directurl from a previous downloadstart --> Allow
         * download attempt anyways as we can be quite sure that it will still be valid.
         */
        if (StringUtils.isNotEmpty(dllink)) {
            return true;
        } else {
            return super.enoughTrafficFor(link, account);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (this.supportsAPISingleAvailablecheck(link)) {
            final Account account = this.findAccountWithAPICredentials();
            if (account != null) {
                return this.requestFileInformationAPI(this.br, link, account);
            }
        }
        return requestFileInformationWebsite(link, null, false);
    }

    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            setWeakFilename(link);
        }
        br.setFollowRedirects(true);
        prepBrowserWebsite(this.br);
        final String[] fileInfo = getFileInfoArray();
        if (supports_availablecheck_over_info_page(link)) {
            getPage(getInfoPageURL(link));
            /* Offline check is unsafe which is why we need to check for other errors first! */
            try {
                this.checkErrors(br, link, account);
            } catch (final PluginException e) {
                if (isDownload || e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    /* File offline or error during download -> Always throw exception. */
                    throw e;
                } else {
                    /* Other error (during linkcheck - e.g. limit reached) -> Return online status - file should be online. */
                    logger.log(e);
                    return AvailableStatus.TRUE;
                }
            }
            /* Offline errorhandling */
            if (isOfflineWebsiteInfoPage(br, link)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            parseAndSetYetiShareVersion(this.br, account);
            scanInfo(link, fileInfo);
            if (!StringUtils.isEmpty(fileInfo[0])) {
                link.setName(fileInfo[0]);
            }
            if (fileInfo[1] != null) {
                link.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(fileInfo[1].replace(",", ""))));
            }
            /**
             * Additional offline check. Useful for websites which still provide filename & filesize for offline files. </br>
             * This can only happen on special file information page!
             */
            if (br.containsHTML("(?i)>\\s*Status\\s*:\\s*</span>\\s*<span>\\s*(Deleted|Usunięto)\\s*</span>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            getPage(getContentURL(link));
            /* Offline check is very unsafe which is why we need to check for other errors first! */
            try {
                this.checkErrors(br, link, account);
            } catch (final PluginException e) {
                if (isDownload || e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    /* File offline or error during download -> Always throw exception. */
                    throw e;
                } else {
                    logger.log(e);
                    /* Other error (during linkcheck - e.g. limit reached) -> Return online status - file should be online. */
                    return AvailableStatus.TRUE;
                }
            }
            /* Offline errorhandling */
            if (isOfflineWebsite(this.br, link)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            parseAndSetYetiShareVersion(this.br, account);
            scanInfo(link, fileInfo);
            if (!StringUtils.isEmpty(fileInfo[0])) {
                link.setName(fileInfo[0]);
            }
            if (fileInfo[1] != null) {
                link.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(fileInfo[1].replace(",", ""))));
            }
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && !StringUtils.startsWithCaseInsensitive(link.getName(), "[NewYetiShare]") && !StringUtils.startsWithCaseInsensitive(link.getName(), "[OldYetiShare]")) {
            if (this.isNewYetiShareVersion(account)) {
                link.setName("[NewYetiShare] " + link.getName());
            } else {
                link.setName("[OldYetiShare] " + link.getName());
            }
        }
        return AvailableStatus.TRUE;
    }

    /**
     * Tries to find filename and filesize inside html. On Override, make sure to first use your special RegExes e.g. fileInfo[0]="bla",
     * THEN, if needed, call super.scanInfo(fileInfo). </br>
     * fileInfo[0] = filename, fileInfo[1] = filesize </br>
     * TODO: 2021-11-19: Clean this up - it is a mess!
     */
    public String[] scanInfo(final DownloadLink link, final String[] fileInfo) {
        /* New YetiShare versions are using the same layout for "/<fuid>" and "/<fuid>~i" pages. */
        if (this.isNewYetiShareVersion(null) || supports_availablecheck_over_info_page(link)) {
            /*
             * 2021-11-19: YetiShare sometimes have a bug where it URL-encodes "_20" to "%20" but at this place this bug does not occur -->
             * Prefer to get filename from here.
             */
            String betterFilename = br.getRegex("(?i)>\\s*File Page Link\\s*</span>\\s*<pre>https?://[^/]+/[A-Za-z0-9]+/([^<]+)</pre>").getMatch(0);
            if (betterFilename == null) {
                betterFilename = br.getRegex("(?i)>\\s*(?:Information about|Informação sobre|Informacja o) \"([^\"]+)\"\\s*<").getMatch(0);
            }
            if (betterFilename != null) {
                fileInfo[0] = betterFilename;
            }
            /** 2021-01-07: Traits for the new style YetiShare layout --> See {@link #isNewYetiShareVersion()} */
            /* 2020-10-12: Special */
            final String betterFilesize = br.getRegex("Filesize\\s*:\\s*</span>\\s*<span>([^<>\"]+)<").getMatch(0);
            if (!StringUtils.isEmpty(betterFilesize)) {
                fileInfo[1] = betterFilesize;
            }
            if (betterFilename != null && betterFilesize != null) {
                return fileInfo;
            }
        }
        if (supports_availablecheck_over_info_page(link)) {
            /* Older handling */
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
                    lang_str_information = "(?:Information about|Informação sobre)";
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
                    fileInfo[1] = br.getRegex("(?:>\\s*|\\(\\s*|\"\\s*|\\[\\s*|\\s+)([0-9\\.]+(?:\\s+|\\&nbsp;)?(bytes)(?!ps|/s|\\w|\\s*Storage|\\s*Disk|\\s*Space|\\s*traffic))").getMatch(0);
                    if (StringUtils.isEmpty(fileInfo[1])) {
                        fileInfo[1] = br.getRegex("(?:>\\s*|\\(\\s*|\"\\s*|\\[\\s*|\\s+)([0-9\\.]+(?:\\s+|\\&nbsp;)?(TB|GB|MB|KB)(?!ps|/s|\\w|\\s*Storage|\\s*Disk|\\s*Space|\\s*traffic))").getMatch(0);
                    }
                }
            }
        }
        return fileInfo;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownloadWebsite(link, null);
    }

    protected void handleDownloadWebsite(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* Login if possible although login is usually not required to access directurls. */
        boolean hasGoneThroughVerifiedLoginOnce = account != null && loginWebsite(account, false);
        /* First try to re-use stored directurl */
        checkDirectLink(link, account);
        if (this.dl == null) {
            /* Access downloadurl */
            br.setFollowRedirects(false);
            prepBrowserWebsite(br);
            getPage(this.getContentURL(link));
            final boolean resume = this.isResumeable(link, account);
            final int maxchunks = this.getMaxChunks(account);
            /*
             * Check for direct-download and ensure that we're logged in! </br> This is needed because we usually load- and set stored
             * cookies without verifying them to save time!
             */
            do {
                if (br.getRedirectLocation() != null) {
                    if (this.isDownloadlink(br.getRedirectLocation())) {
                        br.setFollowRedirects(true);
                        dl = jd.plugins.BrowserAdapter.openDownload(br, link, br.getRedirectLocation(), resume, maxchunks);
                        if (this.looksLikeDownloadableContent(dl.getConnection())) {
                            logger.info("Direct download");
                            break;
                        } else {
                            br.followConnection(true);
                            dl = null;
                            continue;
                        }
                    } else {
                        /* just follow a single redirect */
                        this.br.followRedirect(false);
                        continue;
                    }
                } else {
                    if (account == null) {
                        break;
                    } else if (this.isLoggedin(this.br, account)) {
                        break;
                    } else if (hasGoneThroughVerifiedLoginOnce) {
                        /**
                         * Only try once! </br>
                         * We HAVE to be logged in at this stage!
                         */
                        this.loggedInOrException(this.br, account);
                        break;
                    } else {
                        /*
                         * Some websites only allow 1 session per user. If a user then logs in again via browser while JD is logged in, we
                         * might download as a free-user without noticing that. Example host: Przeslij.com </br> This may also help in other
                         * situations in which we get logged out all of the sudden.
                         */
                        logger.warning("Possible login failure -> Trying again");
                        hasGoneThroughVerifiedLoginOnce = loginWebsite(account, true);
                        br.setFollowRedirects(false);
                        getPage(this.getContentURL(link));
                        continue;
                    }
                }
            } while (true);
            if (this.dl == null) {
                parseAndSetYetiShareVersion(this.br, account);
                /* Check for password protected */
                this.handlePasswordProtection(link, account, this.br);
                /* Now handle pre-download-waittime, captcha and other pre-download steps. */
                if (this.dl == null) {
                    checkErrors(br, link, account);
                    /*
                     * loopLog holds information about the continue_link of each loop so afterwards we get an overview via log output.
                     */
                    String loopLog = null;
                    /* Handle up to x pre-download pages before the (eventually existing) captcha */
                    final int maxPreDownloadPages = 8;
                    final int maxCaptchaAttempts = 3;
                    int preDownloadPageCounter = 0;
                    int captchaAttemptCounter = 0;
                    long timeBeforeCaptchaInput;
                    boolean hasRequestedCaptcha = false; // true if user has been asked for captcha at least once
                    preDownloadLoop: do {
                        logger.info("Handling pre-download page " + (preDownloadPageCounter + 1) + " of max " + maxPreDownloadPages + " | Captcha attempts needed: " + captchaAttemptCounter + "/" + maxCaptchaAttempts);
                        timeBeforeCaptchaInput = Time.systemIndependentCurrentJVMTimeMillis();
                        if (hasRequestedCaptcha && this.containsCaptcha(br)) {
                            /* Retry on wrong captcha */
                            captchaAttemptCounter++;
                        } else {
                            /* Pass to next download page */
                            preDownloadPageCounter++;
                        }
                        /* Captcha, pre-download pages or new YetiShare direct_download API-style handling. */
                        /* Check if new YetiShare API-like handling can be used. */
                        final String continueLink;
                        final String storedInternalFileID = this.getStoredInternalFileID(link);
                        final String htmlFileID = regexInternalFileID(this.br);
                        if (htmlFileID != null || (storedInternalFileID != null && this.allowDirectDownloadAlsoWhenOnlyStoredInternalFileIDIsAvailable(link, account))) {
                            /* For new website layout handling: Assume that direct_download is available. */
                            if (storedInternalFileID == null) {
                                link.setProperty(PROPERTY_INTERNAL_FILE_ID, htmlFileID);
                            } else if (htmlFileID != null && !StringUtils.equals(storedInternalFileID, htmlFileID)) {
                                logger.info("fileID changed!?:stored=" + storedInternalFileID + "|html=" + htmlFileID);
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            final String fileID = this.getStoredInternalFileID(link);
                            this.postPage("/account/ajax/file_details", "u=" + fileID);
                            final Map<String, Object> root = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                            final String html = (String) root.get("html");
                            /* Small workaround to have this html code available in our current browser instance. */
                            br.getRequest().setHtmlCode(html);
                            /* Check for rare case: Final downloadurl inside html provided by via json (e.g. mediafile.cc). */
                            continueLink = getContinueLink(this.br);
                            if (!StringUtils.isEmpty(continueLink)) {
                                logger.info("Found possible continueLink in html provided via json: " + continueLink);
                            } else {
                                /* Assume that direct_download handling is possible. */
                                if (br.containsHTML("triggerFileDownload\\(" + fileID)) {
                                    logger.info("Attempting direct_download");
                                    this.hookBeforeV2DirectDownload(link, account, br);
                                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, "/account/direct_download/" + fileID, resume, maxchunks);
                                    break;
                                } else {
                                    /* This should never happen */
                                    logger.warning("direct_download impossible for unknown reasons");
                                }
                            }
                        } else {
                            continueLink = getContinueLink(this.br);
                        }
                        final Form continueform = getContinueForm(this.br, preDownloadPageCounter, continueLink);
                        if (continueform == null && StringUtils.isEmpty(continueLink)) {
                            logger.info("No continue_form/continue_link available, plugin broken --> Step: " + (preDownloadPageCounter + 1));
                            checkErrors(br, link, account);
                            checkErrorsLastResort(br, link, account);
                        }
                        if (loopLog == null) {
                            loopLog = continueLink;
                        } else {
                            loopLog += " --> " + continueLink;
                        }
                        if (isDownloadlink(continueLink)) {
                            /*
                             * If we already found a downloadlink let's try to download it because html can still contain captcha html -->
                             * We don't need a captcha in this case/loop/pass for sure! E.g. host 'filecad.com'.
                             */
                            waitTime(this.br, link, timeBeforeCaptchaInput);
                            this.dl = jd.plugins.BrowserAdapter.openDownload(br, link, continueLink, resume, maxchunks);
                            break;
                        } else if (continueform == null) {
                            /* Check if link to next download-page redirects directly to final downloadurl. */
                            waitTime(this.br, link, timeBeforeCaptchaInput);
                            br.setFollowRedirects(false);
                            this.dl = jd.plugins.BrowserAdapter.openDownload(br, link, continueLink, resume, maxchunks);
                            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                                break preDownloadLoop;
                            } else {
                                br.followConnection(true);
                            }
                            /* Loop to handle redirects */
                            while (true) {
                                final String redirect = this.br.getRedirectLocation();
                                if (redirect == null) {
                                    break;
                                } else {
                                    this.dl = jd.plugins.BrowserAdapter.openDownload(br, link, redirect, resume, maxchunks);
                                    if (this.looksLikeDownloadableContent(dl.getConnection())) {
                                        break preDownloadLoop;
                                    } else {
                                        br.followConnection(true);
                                        br.followRedirect();
                                        if (this.isAbort()) {
                                            throw new InterruptedException();
                                        }
                                    }
                                }
                            }
                            br.setFollowRedirects(true);
                        } else if (containsRecaptchaV2Class(continueform) || br.containsHTML("data\\-sitekey\\s*=\\s*|g\\-recaptcha\\'")) {
                            loopLog += " --> reCaptchaV2";
                            hasRequestedCaptcha = true;
                            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                            waitTime(this.br, link, timeBeforeCaptchaInput);
                            continueform.put("capcode", "false");
                            continueform.put("g-recaptcha-response", recaptchaV2Response);
                            continueform.setMethod(MethodType.POST);
                            br.setFollowRedirects(true);
                            dl = jd.plugins.BrowserAdapter.openDownload(br, link, continueform, resume, maxchunks);
                        } else if (containsSolvemediaCaptcha(continueform) || this.containsSolvemediaCaptcha(br.getRequest().getHtmlCode())) {
                            loopLog += " --> SolvemediaCaptcha";
                            hasRequestedCaptcha = true;
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
                            String chid = null;
                            try {
                                chid = sm.getChallenge(code);
                            } catch (final PluginException e) {
                                if (e.getLinkStatus() == LinkStatus.ERROR_CAPTCHA) {
                                    logger.info("Wrong captcha");
                                    if (captchaAttemptCounter >= maxCaptchaAttempts) {
                                        logger.info("Ending main loop because: Many wrong captchas");
                                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                                    } else {
                                        this.invalidateLastChallengeResponse();
                                        continue;
                                    }
                                } else {
                                    throw e;
                                }
                            }
                            waitTime(this.br, link, timeBeforeCaptchaInput);
                            continueform.put("adcopy_challenge", Encoding.urlEncode(chid));
                            continueform.put("adcopy_response", Encoding.urlEncode(code));
                            continueform.setMethod(MethodType.POST);
                            br.setFollowRedirects(true);
                            dl = jd.plugins.BrowserAdapter.openDownload(br, link, continueform, resume, maxchunks);
                        } else if (continueform != null && continueform.getMethod() == MethodType.POST) {
                            /* Form + no captcha */
                            loopLog += " --> Form_POST";
                            waitTime(this.br, link, timeBeforeCaptchaInput);
                            br.setFollowRedirects(true);
                            dl = jd.plugins.BrowserAdapter.openDownload(br, link, continueform, resume, maxchunks);
                        } else {
                            logger.warning("Unknown state");
                            break;
                        }
                        logger.info("loopLog: " + loopLog);
                        try {
                            checkResponseCodeErrors(dl.getConnection());
                        } catch (final PluginException e) {
                            try {
                                br.followConnection(true);
                            } catch (final IOException ioe) {
                                throw Exceptions.addSuppressed(e, ioe);
                            }
                            throw e;
                        }
                        if (looksLikeDownloadableContent(this.dl.getConnection())) {
                            loopLog += " --> DOWNLOAD!";
                            logger.info("Successfully initiated download");
                            break;
                        } else {
                            br.followConnection(true);
                            this.dl = null;
                            /* Get new continue_link for the next run */
                            logger.info("Failed to find downloadurl in this round");
                            checkErrors(br, link, account);
                            if (preDownloadPageCounter >= maxPreDownloadPages) {
                                logger.info("Breaking main loop because: Too many pre-download-pages");
                                break;
                            } else if (captchaAttemptCounter >= maxCaptchaAttempts) {
                                logger.info("Ending main loop because: Many wrong captchas");
                                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                            }
                            /* Captcha logger */
                            if (hasRequestedCaptcha) {
                                if (containsCaptcha(br)) {
                                    this.invalidateLastChallengeResponse();
                                    logger.info("Wrong captcha");
                                } else {
                                    this.validateLastChallengeResponse();
                                }
                            }
                        }
                    } while (true);
                    logger.info("Final loopLog: " + loopLog);
                }
            }
        }
        if (dl == null) {
            checkErrors(br, link, account);
            if (isOfflineWebsite(this.br, link)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            checkErrorsLastResort(br, link, account);
        }
        /*
         * Save directurl before download-attempt as it should be valid even if it e.g. fails because of server issue 503 (= too many
         * connections) --> Should work fine after the next try.
         */
        link.setProperty(getDownloadModeDirectlinkProperty(account), dl.getConnection().getURL().toString());
        try {
            checkResponseCodeErrors(dl.getConnection());
        } catch (final PluginException e) {
            try {
                br.followConnection(true);
            } catch (final IOException ioe) {
                throw Exceptions.addSuppressed(e, ioe);
            }
            throw e;
        }
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            checkErrors(br, link, account);
            checkErrorsLastResort(br, link, account);
        }
        dl.setFilenameFix(isContentDispositionFixRequired(dl, dl.getConnection(), link));
        dl.startDownload();
    }

    private String regexInternalFileID(final Browser br) {
        return br.getRegex("showFileInformation\\((\\d+)").getMatch(0);
    }

    protected boolean containsCaptcha(final Browser br) {
        if (AbstractRecaptchaV2.containsRecaptchaV2Class(br) || SolveMedia.containsSolvemediaCaptcha(br.toString()) || br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/|solvemedia\\.com/papi/)") || AbstractHCaptcha.containsHCaptcha(br)) {
            return true;
        } else {
            return false;
        }
    }

    protected void hookBeforeV2DirectDownload(final DownloadLink link, final Account account, final Browser br) throws Exception {
        /* E.g. used for letsupload.io/letsupload.co */
    }

    protected void handlePasswordProtection(final DownloadLink link, final Account account, final Browser br) throws Exception {
        if (getPasswordProtectedForm(this.br) != null) {
            /* Old layout additionally redirects to "/file_password.html?file=<fuid>" */
            String passCode = link.getDownloadPassword();
            if (passCode == null) {
                passCode = getUserInput("Password?", link);
            }
            final Form pwform = this.getPasswordProtectedForm(this.br);
            pwform.put("filePassword", Encoding.urlEncode(passCode));
            br.setFollowRedirects(false);
            this.submitForm(pwform);
            if (this.isDownloadlink(br.getRedirectLocation())) {
                /*
                 * We can start the download right away -> Entered password is correct and we're probably logged in into a premium account.
                 */
                link.setDownloadPassword(passCode);
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, br.getRedirectLocation(), this.isResumeable(link, account), this.getMaxChunks(account));
            } else {
                /* No download -> Either wrong password or correct password & free download */
                br.setFollowRedirects(true);
                if (getPasswordProtectedForm(this.br) != null) {
                    /* Assume that entered password is wrong! */
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else {
                    /* Correct password --> Store it */
                    link.setDownloadPassword(passCode);
                }
            }
        }
    }

    protected Form getPasswordProtectedForm(final Browser br) {
        return br.getFormbyKey("filePassword");
    }

    /**
     * API file operations usually requires us to have the internal ID of files. </br>
     * Most of all times we don't have this but if a website is using the "new" YetiShare script version and files were added as part of a
     * folder, we do have these internal fileIDs available!
     */
    protected String getStoredInternalFileID(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_INTERNAL_FILE_ID);
    }

    /** In some cases this can be useful to skip captchas and waittimes. */
    protected boolean allowDirectDownloadAlsoWhenOnlyStoredInternalFileIDIsAvailable(final DownloadLink link, final Account account) {
        return false;
    }

    protected Form getContinueForm(final Browser br, final int loop_counter, final String continue_link) throws PluginException {
        /* 2019-07-05: continue_form without captcha is a rare case. Example-site: freefile.me */
        Form continueform = br.getFormbyActionRegex(".+pt=.+");
        if (continueform == null) {
            continueform = br.getFormByInputFieldKeyValue("submitted", "1");
            if (continueform == null) {
                continueform = br.getFormbyKey("submitted");
            }
        }
        if (continueform == null) {
            return null;
        } else if (continueform.containsHTML("(?i)" + Pattern.quote(continue_link))) {
            return continueform;
        } else {
            /* Manually build / complete form. */
            final Form ret = new Form();
            ret.setMethod(continueform.getMethod());
            ret.setAction(continue_link);
            ret.put("submit", "Submit");
            ret.put("submitted", "1");
            ret.put("d", "1");
            for (final InputField field : continueform.getInputFields()) {
                if (field.getKey() != null && ret.getInputField(field.getKey()) != null) {
                    ret.addInputField(field);
                }
            }
            return ret;
        }
    }

    protected String getContinueLink(final Browser br) throws Exception {
        String continue_link = br.getRegex("\\$\\(\\'\\.download\\-timer\\'\\)\\.html\\(\"<a href=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
        if (continue_link == null) {
            continue_link = br.getRegex("class=\\'btn btn\\-free\\'[^>]*href=\\'([^<>\"\\']*?)\\'>").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("<div class=\"captchaPageTable\">\\s*<form method=\"POST\" action=\"(https?://[^<>\"]*?)\"").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("(https?://[^/]+/[^<>\"\\':]*pt=[^<>\"\\']*)(?:\"|\\')").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = getDllink(br);
        }
        return continue_link;
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
        return link != null ? getFUIDFromURL(link.getPluginPatternMatcher()) : null;
    }

    /** Returns unique id from inside URL - usually with this pattern: [A-Za-z0-9]+ */
    public String getFUIDFromURL(final String url) {
        try {
            if (url != null) {
                final String result = new Regex(new URL(url).getPath(), "^/([A-Za-z0-9]+)").getMatch(0);
                return result;
            } else {
                return null;
            }
        } catch (final MalformedURLException ignore) {
            ignore.printStackTrace();
            return null;
        }
    }

    /**
     * In some cases, URL may contain filename which can be used as fallback e.g. 'https://host.tld/<fuid>/<filename>'. </br>
     * Example host which has URLs that contain filenames: freefile.me, letsupload.co
     */
    public String getFilenameFromURL(final DownloadLink link) {
        final String result;
        final String contentURL = getPluginContentURL(link);
        if (contentURL != null) {
            result = getFilenameFromURL(contentURL);
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
        if (fallback_filename != null) {
            return fallback_filename;
        } else {
            /* Return FUID as fallback name */
            return getFUIDFromURL(url);
        }
    }

    /**
     * Handles pre download (pre-captcha[first attempt]) waittime.
     */
    protected void waitTime(final Browser br, final DownloadLink link, final long timeBefore) throws PluginException {
        /* Ticket Time */
        final String waitStr = regexWaittime(br);
        final int extraWaitSeconds = 1;
        int wait;
        if (waitStr != null && waitStr.matches("\\d+")) {
            int passedTime = (int) ((Time.systemIndependentCurrentJVMTimeMillis() - timeBefore) / 1000) - extraWaitSeconds;
            logger.info("Found waittime, parsing waittime: " + waitStr);
            wait = Integer.parseInt(waitStr);
            /*
             * Check how much time has passed during eventual captcha event before this function has been called and see how much time is
             * left to wait.
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
            sleep(wait * 1000l, link);
        } else if (wait < -extraWaitSeconds) {
            /* User needed more time to solve the captcha so there is no waittime left :) */
            logger.info("Congratulations: Time to solve captcha was higher than waittime --> No waittime left");
        } else {
            /* No waittime at all */
            logger.info("Found no waittime");
        }
    }

    /* https://stackoverflow.com/questions/10664434/escaping-special-characters-in-java-regular-expressions/25853507 */
    private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

    static String escapeSpecialRegexChars(final String str) {
        return SPECIAL_REGEX_CHARS.matcher(str).replaceAll("\\\\$0");
    }

    private Map<String, Object> getErrorKeyFromErrorMessage(final String errorStr) {
        if (StringUtils.isEmpty(errorStr)) {
            return null;
        }
        try {
            final String language_keys_json = br.getRegex("l\\s*=\\s*(\\{.*?\\});\\s*?return").getMatch(0);
            final Map<String, Object> language_keys_map = JavaScriptEngineFactory.jsonToJavaMap(language_keys_json);
            final Iterator<Entry<String, Object>> iterator = language_keys_map.entrySet().iterator();
            String found_key_for_errormessage = null;
            final Map<String, String> errorProperties = new HashMap<String, String>();
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
                final Map<String, Object> errorMap = new HashMap<String, Object>();
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

    /** Returns urldecoded errormessage inside current URL (parameter "e"). */
    protected String getErrorMsgURL(final Browser br) {
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
        return errorMsgURL;
    }

    private static final String error_you_have_reached_the_download_limit                                   = "error_you_have_reached_the_download_limit";
    private static final String error_you_have_reached_the_download_limit_this_file                         = "error_you_have_reached_the_download_limit_this_file";
    private static final String error_you_must_register_for_a_premium_account_for_filesize                  = "error_you_must_register_for_a_premium_account_for_filesize";
    private static final String error_file_is_not_publicly_shared                                           = "error_file_is_not_publicly_shared";
    private static final String error_you_must_be_a_x_user_to_download_this_file                            = "error_you_must_be_a_x_user_to_download_this_file";
    private static final String error_you_have_reached_the_maximum_daily_download_limit                     = "error_you_have_reached_the_maximum_daily_download_limit";
    private static final String error_you_have_reached_the_maximum_band_width_per_day_in_the_last_24_hours  = "error_you_have_reached_the_maximum_band_width_per_day_in_the_last_24_hours";
    private static final String error_you_have_reached_the_maximum_daily_download_limit_this_file           = "error_you_have_reached_the_maximum_daily_download_limit_this_file";
    private static final String error_you_have_reached_the_maximum_permitted_downloads_in_the_last_24_hours = "error_you_have_reached_the_maximum_permitted_downloads_in_the_last_24_hours";
    private static final String error_you_have_reached_the_max_permitted_downloads                          = "error_you_have_reached_the_max_permitted_downloads";
    private static final String error_you_must_wait_between_downloads                                       = "error_you_must_wait_between_downloads";

    /**
     * Checks for errormessages inside the current browsers' URL and also for errors based on the current browsers' URL structure. </br>
     * It was intended to replace this with checkErrorsLanguageIndependant but this doesn't work out as different templates/versions of
     * YetiShare are using different errors and not all have their full language keys available. </br>
     * Newer versions of YetiShare don't provide any language keys at all but provide mostly English errors inside URLs ("?e=...").
     */
    private void checkErrorsURL(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        final String errorMsgURL = this.getErrorMsgURL(br);
        if (br.containsHTML("(?i)Error: Too many concurrent download requests")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 3 * 60 * 1000l);
        }
        if (errorMsgURL != null) {
            if (StringUtils.containsIgnoreCase(errorMsgURL, "You have reached the maximum concurrent downloads")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max. simultan downloads limit reached, wait to start more downloads", 1 * 60 * 1000l);
            } else if (StringUtils.containsIgnoreCase(errorMsgURL, "Could not open file for reading")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 'Could not open file for reading'", 60 * 60 * 1000l);
            } else if (new Regex(errorMsgURL, "(?i).*(You must register for a premium account to|Ten plik jest za duży do pobrania dla darmowego użytkownika).*").matches()) {
                throw new AccountRequiredException(errorMsgURL);
            } else if (new Regex(errorMsgURL, "(?i).*File is not publicly available.*").matches()) {
                /* Private file -> Only owner can download it. */
                throw new AccountRequiredException(errorMsgURL);
            } else if (StringUtils.containsIgnoreCase(errorMsgURL, "You have reached the maximum permitted downloads in")) {
                ipBlockedOrAccountLimit(link, account, errorMsgURL, 3 * 60 * 60 * 1001l);
            } else if (StringUtils.containsIgnoreCase(errorMsgURL, "File not found") || StringUtils.containsIgnoreCase(errorMsgURL, "File has been removed") || StringUtils.containsIgnoreCase(errorMsgURL, "Dosya kaldırıldı") || StringUtils.containsIgnoreCase(errorMsgURL, "Nie znaleziono pliku")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (errorMsgURL.matches("(?i).*(You must wait |Você deve esperar).*")) {
                final long extraWaittimeMilliseconds = 1000;
                long waittime = this.parseWaittime(errorMsgURL);
                if (waittime <= 0) {
                    /* Fallback */
                    logger.info("Waittime RegExes seem to be broken - using default waittime");
                }
                waittime += extraWaittimeMilliseconds;
                logger.info("Detected reconnect waittime (milliseconds): " + waittime);
                /* Not enough wait time to reconnect -> Wait short and retry */
                if (waittime < 180000) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait until new downloads can be started", waittime);
                } else {
                    ipBlockedOrAccountLimit(link, account, errorMsgURL, waittime);
                }
            } else {
                logger.info("Found unidentified errormessage: " + errorMsgURL);
            }
        } else if (br.getURL().matches("(?i)https?://[^/]+/register\\?f=.+")) {
            throw new AccountRequiredException();
        }
    }

    /**
     * Checks for reasons to ignore given PluginExceptions. </br>
     * Example: Certain errors thay may happen during availablecheck when user is not yet logged in but won't happen when user is logged in.
     * </br>
     * TODO: Remove this once "fastdrive.io" does not exist anymore.
     */
    @Deprecated
    protected void ignorePluginException(final PluginException exception, final Browser br, final DownloadLink link, final Account account) throws PluginException {
        if (account != null) {
            // TODO: update with more account specific error handling or find a way to eliminate the need of this handling!
            final Set<String> mightBeOkayWithAccountLogin = new HashSet<String>();
            switch (account.getType()) {
            case PREMIUM:
                mightBeOkayWithAccountLogin.add(error_you_have_reached_the_maximum_permitted_downloads_in_the_last_24_hours);
                mightBeOkayWithAccountLogin.add(error_you_must_register_for_a_premium_account_for_filesize);
                mightBeOkayWithAccountLogin.add(error_you_must_be_a_x_user_to_download_this_file);
                break;
            default:
                mightBeOkayWithAccountLogin.add(error_you_must_be_a_x_user_to_download_this_file);
                break;
            }
            final String mappedErrorKey;
            synchronized (errorMsgURLMap) {
                mappedErrorKey = errorMsgURLMap.get(exception.getMessage());
            }
            if (mightBeOkayWithAccountLogin.contains(mappedErrorKey)) {
                logger.exception("special handling to ignore:" + exception.getMessage() + "|" + mappedErrorKey, exception);
                return;
            } else {
                logger.info("no special handling for:" + exception.getMessage() + "|" + mappedErrorKey);
            }
        }
        throw exception;
    }

    @Deprecated
    protected static HashMap<String, String> errorMsgURLMap = new HashMap<String, String>();

    /**
     * 2020-03-25: No plugin should ever have to override this. </br>
     * Please create a ticket before changing this!
     */
    private void checkErrorsLanguageIndependant(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        final String errorMsgURL = this.getErrorMsgURL(br);
        if (!StringUtils.isEmpty(errorMsgURL)) {
            logger.info("Found errormessage in current URL: " + errorMsgURL);
            final Map<String, Object> errorMap = getErrorKeyFromErrorMessage(errorMsgURL);
            if (errorMap == null) {
                /* Not all websites have (all) language keys present e.g. ultimbox.com */
                logger.info("Failed to find error_key --> Trying checkErrorsURL");
                checkErrorsURL(br, link, account);
                logger.info("checkErrorsURL did not do anything --> Throwing Exception ERROR_TEMPORARILY_UNAVAILABLE because of errorMsgURL: " + errorMsgURL);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error without errorkey: " + errorMsgURL);
            }
            final String errorkey = (String) errorMap.get("error_key");
            synchronized (errorMsgURLMap) {
                errorMsgURLMap.put(errorMsgURL, errorkey);
            }
            logger.info("Found key to errormessage: " + errorkey);
            Map<String, String> errorProperties = null;
            if (errorMap.containsKey("error_properties")) {
                /* This is really only for logging purposes */
                errorProperties = (Map<String, String>) errorMap.get("error_properties");
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
            else if (errorkey.equalsIgnoreCase(error_you_must_register_for_a_premium_account_for_filesize)) {
                throw new AccountRequiredException(errorMsgURL);
            } else if (errorkey.equalsIgnoreCase(error_you_must_be_a_x_user_to_download_this_file)) {
                throw new AccountRequiredException(errorMsgURL);
            } else if (errorkey.equalsIgnoreCase(error_file_is_not_publicly_shared)) {
                /* Very very rare case */
                logger.info("This file can only be downloaded by the initial uploader");
                throw new AccountRequiredException(errorMsgURL);
            } /** Limit errorhandling */
            else if (errorkey.equalsIgnoreCase(error_you_have_reached_the_download_limit)) {
                ipBlockedOrAccountLimit(link, account, errorMsgURL, default_waittime);
            } else if (errorkey.equalsIgnoreCase(error_you_have_reached_the_download_limit_this_file)) {
                /* "You do not have enough bandwidth left to download this file." */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorMsgURL, default_waittime);
            } else if (errorkey.equalsIgnoreCase(error_you_have_reached_the_maximum_daily_download_limit)) {
                /* "You have reached the maximum download bandwidth today, please upgrade or try again later." */
                ipBlockedOrAccountLimit(link, account, errorMsgURL, default_waittime);
            } else if (errorkey.equalsIgnoreCase(error_you_have_reached_the_maximum_band_width_per_day_in_the_last_24_hours)) {
                /* 2020-12-11: sundryshare.com */
                /* "You have reached the maximum permitted downloads  band width per day." */
                ipBlockedOrAccountLimit(link, account, errorMsgURL, default_waittime);
            } else if (errorkey.equalsIgnoreCase(error_you_have_reached_the_maximum_daily_download_limit_this_file)) {
                /* "You do not have enough bandwidth left today to download this file." */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorMsgURL, default_waittime);
            } else if (errorkey.equalsIgnoreCase(error_you_have_reached_the_maximum_permitted_downloads_in_the_last_24_hours)) {
                ipBlockedOrAccountLimit(link, account, errorMsgURL, default_waittime);
            } else if (errorkey.equalsIgnoreCase(error_you_have_reached_the_max_permitted_downloads)) {
                /*
                 * Limit per file -> All we can do is wait.
                 * "You have reached the maximum concurrent downloads. Please wait for your existing downloads to complete or register for a premium account above."
                 */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, errorMsgURL, 5 * 60 * 1000l);
            } else if (errorkey.equalsIgnoreCase(error_you_must_wait_between_downloads)) {
                // VERIFIED
                /* E.g. "30 minutes" */
                final String waitStr = errorProperties.get("WAITING_TIME_LABEL");
                long waittime = parseWaittime(waitStr);
                if (waittime <= 0) {
                    waittime = default_waittime;
                }
                if (waittime < 180000) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, errorMsgURL, waittime);
                } else {
                    ipBlockedOrAccountLimit(link, account, errorMsgURL, waittime);
                }
            } else {
                logger.warning("Unknown errorkey: " + errorkey + " --> Trying checkErrorsURL");
                checkErrorsURL(br, link, account);
                logger.info("checkErrorsURL did not do anything --> Throwing Exception ERROR_TEMPORARILY_UNAVAILABLE because of errorMsgURL: " + errorMsgURL);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error: " + errorMsgURL + " | Errorkey: " + errorkey);
            }
        }
    }

    /** Throws appropriate Exception depending on whether or not an account is given. */
    private void ipBlockedOrAccountLimit(final DownloadLink link, final Account account, final String errorMsg, final long waitMillis) throws PluginException {
        if (account != null) {
            throw new AccountUnavailableException(errorMsg, waitMillis);
        } else {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errorMsg, waitMillis);
        }
    }

    private long parseWaittime(final String src) {
        /*
         * Important: URL Might contain htmlencoded parts! Be sure that these RegExes are tolerant enough to get the information we need!
         */
        final String wait_hours = new Regex(src, "(?i)(\\d+)\\s*?hours?").getMatch(0);
        final String wait_minutes = new Regex(src, "(?i)(\\d+)\\s*?minutes?").getMatch(0);
        final String wait_seconds = new Regex(src, "(?i)(\\d+)\\s*?(?:seconds?|segundos)").getMatch(0);
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

    public void checkErrors(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        checkErrorsLanguageIndependant(br, link, account);
        /*
         * Now check for errors which checkErrorsLanguageIndependant failed to handle
         */
        checkErrorsURL(br, link, account);
        if (br.containsHTML(">\\s*File is not publicly available")) {
            /*
             * 2021-11-18: Spotted on YetiShareNew (oxyclod.com) on file info page (URL ending with "~i") --> Treat file as offline if this
             * happens!
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.toString().equalsIgnoreCase("unknown user")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Unknown user'", 30 * 60 * 1000l);
        } else if (br.toString().equalsIgnoreCase("ERROR: Wrong IP")) {
            /*
             * 2019-07-05: New: rare case but this can either happen randomly or when you try to resume a stored downloadurl with a new IP.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Wrong IP'", 5 * 60 * 1000l);
        }
        /* 2020-10-12: New YetiShare */
        final String waittimeBetweenDownloadsStr = br.getRegex("(?i)>\\s*You must wait (\\d+) minutes? between downloads").getMatch(0);
        if (waittimeBetweenDownloadsStr != null) {
            ipBlockedOrAccountLimit(link, account, "Wait between downloads", Integer.parseInt(waittimeBetweenDownloadsStr) * 60 * 1001l);
        }
    }

    /** Only call this if you're sure that login state can be recognized properly! */
    protected void loggedInOrException(final Browser br, final Account account) throws PluginException {
        if (account == null) {
            /* Programmer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (br.getHttpConnection().getResponseCode() == 200 && !this.isLoggedin(br, account)) {
            throw new AccountUnavailableException("Session expired?", 5 * 60 * 1000l);
        } else {
            // TODO: Jiaz: other response codes?
            /*
             * 2021-05-04: psp: No check for other responsecodes required: This is supposed to throw Exception if we're not logged in. If we
             * can't reliably check the loggedin state we just do nothing.
             */
        }
    }

    protected void checkErrorsLastResort(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        logger.info("Last resort errorhandling");
        final String rcID = br.getRegex("recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
        if (account != null) {
            this.loggedInOrException(br, account);
        } else if (new Regex(br.getURL(), "^https?://[^/]+/?$").matches()) {
            /* Handle redirect to mainpage as premiumonly */
            throw new AccountRequiredException();
        } else if (rcID != null) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Website uses reCaptchaV1 which has been shut down by Google. Contact website owner!");
        }
        logger.warning("Unknown error happened");
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /** Handles all kinds of error response codes! */
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
                exception429TooManyConnections();
            } else if (responsecode == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503 too many connections", 1 * 60 * 1000l);
            }
        }
    }

    private void exception429TooManyConnections() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 429 too many requests", 5 * 60 * 1000l);
    }

    /* Returns true if file under url /<fileID>~i is offline. */
    protected boolean isOfflineWebsiteInfoPage(final Browser br, final DownloadLink link) throws Exception {
        if (!br.getURL().contains("~i") || br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return true = file is offline, false = file is online </br>
     *         Be sure to always call checkErrors before calling this! </br>
     *         Do not call this on file info page (URL ending with "~i")!
     * @throws Exception
     */
    protected boolean isOfflineWebsite(final Browser br, final DownloadLink link) throws Exception {
        // final boolean currentURLContainsFID = br.getURL().contains(this.getFUIDFromURL(link));
        final boolean isDownloadableOldWebsiteOrFreeMode = this.getContinueLink(br) != null;
        final boolean isDownloadableNewWebsite = this.getStoredInternalFileID(link) != null || this.regexInternalFileID(br) != null;
        final boolean isDownloadable = isDownloadableOldWebsiteOrFreeMode || isDownloadableNewWebsite;
        final boolean isFileWebsite = br.containsHTML("class=\"downloadPageTable(V2)?\"") || br.containsHTML("class=\"download\\-timer\"");
        final boolean isErrorPage = br.getURL().contains("/error.html") || br.getURL().contains("/index.html");
        final boolean isOffline404 = br.getHttpConnection().getResponseCode() == 404;
        if ((!isFileWebsite || isErrorPage || isOffline404) && !isDownloadable) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns pre-download-waittime (seconds) from inside HTML. */
    public String regexWaittime(final Browser br) {
        String waitSecondsStr = br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
        if (waitSecondsStr == null) {
            waitSecondsStr = br.getRegex("var\\s*?seconds\\s*=\\s*(\\d+);").getMatch(0);
        }
        return waitSecondsStr;
    }

    protected final String checkDirectLink(final DownloadLink link, final Account account) throws Exception {
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        final String dllink = link.getStringProperty(directlinkproperty);
        if (dllink == null) {
            return null;
        }
        final Browser br2 = this.br.cloneBrowser();
        br2.setFollowRedirects(true);
        boolean valid = false;
        boolean throwException = false;
        try {
            // con = br2.openHeadConnection(dllink);
            this.dl = jd.plugins.BrowserAdapter.openDownload(br2, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
            if (br2.getHttpConnection().getResponseCode() == 429) {
                logger.info("Stored directurl lead to 429 | too many connections");
                /* URL is valid but we need to wait before we can use it. */
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable ignore) {
                }
                throwException = true;
                exception429TooManyConnections();
                return null;
            } else if (looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return dllink;
            } else {
                /* Remove property so we don't try the same invalid directurl again. */
                link.removeProperty(directlinkproperty);
                br2.followConnection(true);
                throw new IOException();
            }
        } catch (final InterruptedException e) {
            throw e;
        } catch (final Exception e) {
            if (throwException) {
                throw e;
            } else {
                logger.log(e);
                return null;
            }
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                this.dl = null;
            }
        }
    }

    protected String getProtocol(final String url) {
        if (StringUtils.startsWithCaseInsensitive(url, "https://") && allowGetProtocolHttpsAutoHandling(url)) {
            return "https://";
        } else if (supports_https()) {
            return "https://";
        } else {
            return "http://";
        }
    }

    /**
     * If set to true, {@link #getProtocol()} will prefer https over http even if {@link #supports_https()} is set to return false. </br>
     * If set to false, {@link #getProtocol()} will only rely on {@link #supports_https()}.
     */
    protected boolean allowGetProtocolHttpsAutoHandling(final String url) {
        return true;
    }

    protected Browser prepBrowserWebsite(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 416, 429 });
        if (enableRandomUserAgent()) {
            String ua = null;
            synchronized (agent) {
                ua = agent.get(getHost());
                if (ua == null) {
                    ua = UserAgents.stringUserAgent();
                    agent.put(getHost(), ua);
                }
            }
            br.getHeaders().put("User-Agent", ua);
        }
        return br;
    }

    protected String getAccountNameSpaceLogin(final Account account) throws PluginException {
        if (account == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (isNewYetiShareVersion(account)) {
            return "/account/login";
        } else {
            return "/login.html";
        }
    }

    protected String getAccountNameSpaceHome(final Account account) throws PluginException {
        if (account == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (isNewYetiShareVersion(account)) {
            return "/account";
        } else {
            return "/account_home.html";
        }
    }

    protected String getAccountNameSpaceUpgrade(final Account account) throws PluginException {
        if (account == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (isNewYetiShareVersion(account)) {
            /* Wome websites will redirect to "/upgrade2" which is fine but it's probably not YetiShare stock! */
            return "/upgrade";
        } else {
            return "/upgrade.html";
        }
    }

    /** Special: In this case, older URLs will also work via new YetiShare e.g. "/account_edit.html" will redirect to "/account/edit". */
    protected String getAccountNameSpaceEditAccount(final Account account) throws PluginException {
        if (account == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (isNewYetiShareVersion(account)) {
            return "/account/edit";
        } else {
            return "/account_edit.html";
        }
    }

    protected String getAccountNameSpaceLogout(final Account account) throws PluginException {
        if (account == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (isNewYetiShareVersion(account)) {
            return "/account/logout";
        } else {
            return "/logout.html";
        }
    }

    protected boolean isNewYetiShareVersion(final Account account) {
        if (account == null) {
            return this.getPluginConfig().getBooleanProperty(PROPERTY_IS_NEW_YETISHARE_VERSION, false);
        } else {
            return account.getBooleanProperty(PROPERTY_IS_NEW_YETISHARE_VERSION, false);
        }
    }

    /**
     * Access any YetiShare website via browser before and call this once to auto set flag for new YetiShare version! </br>
     * New version = YetiShare 5.0 and above, see: https://yetishare.com/release_history.html
     */
    protected void parseAndSetYetiShareVersion(final Browser br, final Account account) {
        if (br.containsHTML("(?i)https?://[^/]+/(account|register|account/login|account/logout)\"")) {
            this.getPluginConfig().setProperty(PROPERTY_IS_NEW_YETISHARE_VERSION, true);
            if (account != null) {
                account.setProperty(PROPERTY_IS_NEW_YETISHARE_VERSION, true);
            }
        } else {
            this.getPluginConfig().removeProperty(PROPERTY_IS_NEW_YETISHARE_VERSION);
            if (account != null) {
                account.removeProperty(PROPERTY_IS_NEW_YETISHARE_VERSION);
            }
        }
    }

    protected void fillWebsiteLoginForm(Browser br, Form loginform, Account account) {
        {
            final String user = Encoding.urlEncode(account.getUser());
            InputField userField = null;
            final String userFieldNames[] = new String[] { "username", "loginUsername", "email" };
            for (String userFieldName : userFieldNames) {
                userField = loginform.getInputFieldByName(userFieldName);
                if (userField != null) {
                    break;
                }
            }
            if (userField != null) {
                userField.setValue(user);
            } else {
                loginform.put(userFieldNames[0], user);
            }
        }
        {
            final String password = Encoding.urlEncode(account.getPass());
            InputField passwordField = null;
            final String passwordFieldNames[] = new String[] { "password", "loginPassword" };
            for (String passwordFieldName : passwordFieldNames) {
                passwordField = loginform.getInputFieldByName(passwordFieldName);
                if (passwordField != null) {
                    break;
                }
            }
            if (passwordField != null) {
                passwordField.setValue(password);
            } else {
                loginform.put(passwordFieldNames[0], password);
            }
        }
    }

    /**
     * @return true: Cookies were validated</br>
     *         false: Cookies were not validated
     */
    public boolean loginWebsite(final Account account, boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBrowser(this.br, account.getHoster());
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies != null) {
                    this.br.setCookies(this.getHost(), userCookies);
                    if (!force) {
                        /* Trust cookies without checking */
                        return false;
                    }
                    if (this.verifyCookies(br, account, userCookies)) {
                        return true;
                    } else {
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                } else if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Trust cookies without checking */
                        return false;
                    }
                    if (this.verifyCookies(br, account, cookies)) {
                        /* Refresh stored cookies */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    }
                }
                logger.info("Performing full login");
                getPage(this.getProtocol(null) + this.getHost());
                /* This is crucial!! */
                this.parseAndSetYetiShareVersion(br, account);
                getPage(getAccountNameSpaceLogin(account));
                if (br.containsHTML("flow-login\\.js") && !enforce_old_login_method()) {
                    final String loginstart = new Regex(br.getURL(), "(https?://(www\\.)?)").getMatch(0);
                    /* New (ajax) login method - mostly used - example: iosddl.net */
                    logger.info("Using new login method");
                    /* These headers are important! */
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                    Form loginform = br.getFormbyProperty("id", "form_login");
                    if (loginform == null) {
                        logger.info("Fallback to custom built loginform");
                        loginform = new Form();
                        loginform.put("submitme", "1");
                    }
                    fillWebsiteLoginForm(br, loginform, account);
                    final String action = loginstart + this.getHost() + "/ajax/_account_login.ajax.php";
                    loginform.setAction(action);
                    if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(loginform)) {
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    }
                    submitForm(loginform);
                    if (!br.containsHTML("\"login_status\"\\s*:\\s*\"success\"")) {
                        throw new AccountInvalidException();
                    }
                } else {
                    /* Old non-ajax method - rare case! Example: All extremely old YetiShare versions and all >= 5.0 */
                    logger.info("Using non-ajax login method");
                    Form loginform = br.getFormbyProperty("id", "form_login");
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
                    fillWebsiteLoginForm(br, loginform, account);
                    if (br.containsHTML("solvemedia\\.com/papi/")) {
                        /* Handle login-captcha if required */
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
                        final String code = getCaptchaCode("solvemedia", cf, new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                        final String chid = sm.getChallenge(code);
                        loginform.put("adcopy_challenge", chid);
                        loginform.put("adcopy_response", "manual_challenge");
                    } else if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(loginform)) {
                        /* E.g. thotdrive.com */
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    }
                    submitForm(loginform);
                    if (br.containsHTML(">\\s*Your username and password are invalid\\s*<") || !isLoggedin(br, account)) {
                        throw new AccountInvalidException();
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public String buildExternalDownloadURL(DownloadLink downloadLink, PluginForHost buildForThisPlugin) {
        return getContentURL(downloadLink);
    }

    /** Sets given cookies and checks if we can login with them. */
    protected boolean verifyCookies(final Browser br, final Account account, final Cookies cookies) throws Exception {
        br.setCookies(this.getHost(), cookies);
        getPage(br, this.getProtocol(br.getURL()) + this.getHost());
        /* This is crucial!! */
        this.parseAndSetYetiShareVersion(br, account);
        getPage(br, this.getMainPage(br.getURL()) + this.getAccountNameSpaceUpgrade(account));
        if (isLoggedin(br, account)) {
            /* Set/Update account-type */
            if (this.isPremiumAccount(account, br)) {
                setAccountLimitsByType(account, AccountType.PREMIUM);
            } else {
                setAccountLimitsByType(account, AccountType.FREE);
            }
            logger.info("Successfully logged in via cookies:" + account.getType());
            return true;
        } else {
            logger.info("Failed to login via cookies");
            br.clearCookies(br.getHost());
            return false;
        }
    }

    public boolean isLoggedin(final Browser br, final Account account) throws PluginException {
        /**
         * User is logged in when: 1. Logout button is visible or </br>
         * 2. When "Account Overview" Buttons is visible e.g. when on mainpage or trying to download a file.
         */
        if (br.containsHTML(Pattern.quote(this.getAccountNameSpaceLogout(account)) + "\"")) {
            return true;
        } else if (br.containsHTML(Pattern.quote(this.getAccountNameSpaceHome(account)) + "\"")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (enableAPIOnlyMode()) {
            return fetchAccountInfoAPI(this.br, account, account.getUser(), account.getPass());
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (account) {
            loginWebsite(account, true);
            final AccountInfo apiAccInfo = fetchAccountInfoWebsiteAPI(this.br.cloneBrowser(), account);
            if (apiAccInfo != null) {
                logger.info("Found AccountInfo via API --> Prefer this over account information from website");
                return apiAccInfo;
            }
            getPage(this.getAccountNameSpaceUpgrade(account));
            if (this.isPremiumAccount(account, this.br)) {
                final String expireStr = regexExpireDate(this.br);
                if (expireStr != null) {
                    final long expire_milliseconds = parseExpireTimeStamp(account, expireStr);
                    ai.setValidUntil(expire_milliseconds, this.br);
                } else {
                    /*
                     * 2021-01-07: Allow premium accounts without expire-date: I've never seen such accounts but let's say we're unable to
                     * parse expire-date but we are sure it is a premium account -> That case is covered now
                     */
                }
                this.setAccountLimitsByType(account, AccountType.PREMIUM);
            } else {
                this.setAccountLimitsByType(account, AccountType.FREE);
            }
        }
        ai.setUnlimitedTraffic();
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            final String accStatus;
            if (ai.getStatus() != null) {
                accStatus = ai.getStatus();
            } else {
                accStatus = account.getType().toString();
            }
            if (this.isNewYetiShareVersion(account)) {
                ai.setStatus("[NewYetiShare] " + accStatus);
            } else {
                ai.setStatus("[OldYetiShare] " + accStatus);
            }
        }
        return ai;
    }

    protected boolean isPremiumAccount(final Account account, final Browser br) {
        String accountType = br.getRegex("(?i)>\\s*Account\\s*Type\\s*:?\\s*</[^>]*>\\s*<[^>]*>\\s*([^<]{3,})\\s*<").getMatch(0);
        if (StringUtils.isEmpty(accountType)) {
            /* 2023-01-27: For subpage "/upgrade" on new YetiShare versions */
            accountType = br.getRegex(">\\s*Account\\s*Type\\s*</strong></td>\\s*<td>([^<]+)</td>").getMatch(0);
        }
        final boolean isPremiumType = StringUtils.containsIgnoreCase(accountType, "Premium") || StringUtils.containsIgnoreCase(accountType, "Paid User");
        final String expireStr = regexExpireDate(br);
        if (expireStr == null) {
            return isPremiumType;
        } else {
            final long expire_milliseconds = parseExpireTimeStamp(account, expireStr);
            return isPremiumType || expire_milliseconds > System.currentTimeMillis();
        }
    }

    protected String regexExpireDate(final Browser br) {
        String expireStr = br.getRegex("Reverts\\s*To\\s*Free\\s*Account\\s*:\\s*</td>\\s*<td>\\s*(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expireStr == null) {
            expireStr = br.getRegex("Reverts\\s*To\\s*Free\\s*Account\\s*:\\s*</span>\\s*<input[^>]*value\\s*=\\s*\"(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            if (expireStr == null) {
                /* More wide RegEx to be more language independant (e.g. required for freefile.me) */
                expireStr = br.getRegex(">\\s*(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})\\s*<").getMatch(0);
            }
        }
        return expireStr;
    }

    /** Tries to auto-find API keys in website HTML code and return account information from API! */
    protected AccountInfo fetchAccountInfoWebsiteAPI(final Browser brc, final Account account) {
        try {
            synchronized (account) {
                this.getPage(brc, getAccountNameSpaceEditAccount(account));
                String key1 = null;
                String key2 = null;
                final Form[] forms = brc.getForms();
                Form generateAPIKeyForm = null;
                for (Form form : forms) {
                    final InputField fieldKey1 = form.getInputField("key1");
                    final InputField fieldKey2 = form.getInputField("key2");
                    if (fieldKey1 != null && fieldKey2 != null) {
                        key1 = fieldKey1.getValue();
                        key2 = fieldKey2.getValue();
                        generateAPIKeyForm = form;
                        break;
                    }
                }
                final boolean foundValidLookingAPICredentialsOnWebsite;
                if (this.isAPICredential(key1) && this.isAPICredential(key2)) {
                    logger.info("Found pre-generated API credentials on website: " + key1 + ":" + key2);
                    foundValidLookingAPICredentialsOnWebsite = true;
                } else {
                    logger.info("Failed to find API credentials on website");
                    foundValidLookingAPICredentialsOnWebsite = false;
                }
                final boolean devForceNewApikeyCreation = false;
                if (generateAPIKeyForm != null) {
                    logger.info("Found generateAPIKeyForm -> Looks like " + this.getHost() + " has enabled API usage in general");
                    if (!this.allowToAttemptAPIUsageInWebsiteModeDuringAccountCheck()) {
                        logger.info("Not attempting to generate API keys although it looks possible because that is disabled!");
                        return null;
                    }
                    if ((!foundValidLookingAPICredentialsOnWebsite && this.allowToGenerateAPIKeyInWebsiteModeDuringAccountCheck()) || (devForceNewApikeyCreation && DebugMode.TRUE_IN_IDE_ELSE_FALSE)) {
                        /* Tested with: letsupload.io */
                        logger.info("Generating API keys");
                        key1 = this.websiteGenerateRandomAPIKey();
                        key2 = this.websiteGenerateRandomAPIKey();
                        generateAPIKeyForm.put("key1", key1);
                        generateAPIKeyForm.put("key2", key2);
                        /* Small workaround for encoded values in multipart forms where data should not be URL-encoded. */
                        final InputField emailAddr = generateAPIKeyForm.getInputField("emailAddress");
                        if (emailAddr != null) {
                            final String value = emailAddr.getValue();
                            if (value != null && Encoding.isUrlCoded(value)) {
                                emailAddr.setValue(URLEncode.decodeURIComponent(value));
                            }
                        }
                        /**
                         * Required minimum fields are: firstame,lastname, emailAddress,key1,key2,submitme. </br>
                         * We'll remove all other fields as we do not want to mistakenly change any of the users' account settings!
                         */
                        final String[] keysToRemove = new String[] { "marketingEmails", "privateFileStatistics", "isPublic", "title", "password", "passwordConfirm", "fileReferrerWhitelist", "watermarkPosition", "watermarkPadding" };
                        for (final String fieldKeyToRemove : keysToRemove) {
                            generateAPIKeyForm.remove(fieldKeyToRemove);
                        }
                        this.submitForm(brc, generateAPIKeyForm);
                        if (brc.containsHTML(key1) && brc.containsHTML(key2)) {
                            logger.info("Looks like API key creation was successful");
                        } else {
                            logger.warning("Looks like API key creation has failed");
                        }
                        /* Generated credentials will be checked down below */
                    } else {
                        /* API key creation not necessary or not allowed */
                    }
                }
                if (!this.allowToAttemptAPIUsageInWebsiteModeDuringAccountCheck()) {
                    if (foundValidLookingAPICredentialsOnWebsite) {
                        logger.info("Not using found API credentials because API usage in website mode during account check has been disabled by developer");
                    }
                    return null;
                }
                logger.info("Checking API login credentials | Trying API accountcheck...");
                try {
                    final AccountInfo apiAccInfo = this.fetchAccountInfoAPI(brc, account, key1, key2);
                    logger.info("Successfully performed accountcheck via API using credentials " + key1 + ":" + key2);
                    /* Save API keys for future usage! */
                    account.setProperty(PROPERTY_API_KEY1, key1);
                    account.setProperty(PROPERTY_API_KEY2, key2);
                    return apiAccInfo;
                } catch (final Throwable e) {
                    logger.log(e);
                    /**
                     * Failure: Happens most likely due to missing API permissions e.g. user can create API keys but it's not an "uploader
                     * account" thus he cannot do anything with the API. </br>
                     * Example: {"response":"Your account level does not have access to the file upload API. Please contact site support for
                     * more information.","_status":"error","_datetime":"2022-09-29 22:30:17"} </br>
                     * {"status":"error","response":"Could not authenticate user. The key pair may be invalid or your account may be locked
                     * from too many failed logins.","_datetime":"2022-10-07 16:32:48"}
                     */
                    logger.info("API handling inside website handling failed! In most of all cases this means that the website owner only enabled API usage for specific users and only for uploading files.");
                    logger.info("@@Developer: Consider disabling API usage in website mode completely via TODO");
                    if (!foundValidLookingAPICredentialsOnWebsite) {
                        logger.info("@@Developer: Consider disabling website API key generation via allowToGenerateAPIKeyInWebsiteMode");
                    }
                }
            }
        } catch (final Exception ignore) {
            logger.log(ignore);
        }
        /** API is not available or does not work (anymore) --> Remove previously saved credentials if existent */
        account.removeProperty(PROPERTY_API_KEY1);
        account.removeProperty(PROPERTY_API_KEY2);
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
            /* All free accounts get the same (IP-based) downloadlimits --> Simultaneous free account usage makes no sense! */
            account.setConcurrentUsePossible(false);
            account.setMaxSimultanDownloads(this.getMaxSimultaneousFreeAccountDownloads());
            break;
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
        if (this.enableAPIOnlyMode()) {
            /* Download via API using credentials added by user. */
            this.handleDownloadAPI(link, account, account.getUser(), account.getPass());
        } else if (this.supportsAPIDownloads(link, account)) {
            /* Download via API using credentials found/created on website. */
            this.handleDownloadAPI(link, account, account.getStringProperty(PROPERTY_API_KEY1), account.getStringProperty(PROPERTY_API_KEY2));
        } else {
            this.handleDownloadWebsite(link, account);
        }
    }

    @Override
    protected void sendRequest(Browser ibr, Request request) throws Exception {
        final URL before = request.getURL();
        super.sendRequest(ibr, request);
        final URL after = ibr._getURL();
        if (after != null) {
            if (StringUtils.equalsIgnoreCase(Browser.getHost(before, false), Browser.getHost(after, false))) {
                if (!StringUtils.equals(getProtocol(null), after.getProtocol() + "://")) {
                    /* Nothing tragic but can be a useful information. */
                    logger.info("@Dev:check supports_https!before=" + before + "|after=" + after);
                }
                final String beforeSub = Browser.getSubdomain(before, true);
                final String afterSub = Browser.getSubdomain(after, true);
                if ((beforeSub == null && StringUtils.equalsIgnoreCase(afterSub, "www")) || (afterSub == null && StringUtils.equalsIgnoreCase(beforeSub, "www"))) {
                    /* Nothing tragic but can be a useful information. */
                    logger.info("@Dev:check requires_WWW!before=" + before + "|after=" + after);
                }
            }
        }
    }

    /** Returns https?://(www.)?host.tld */
    @Deprecated
    protected String getMainPage(final String url) {
        return this.getProtocol(url) + this.appendWWWIfRequired(getHost());
    }

    protected String getMainPage(final DownloadLink link) {
        final String urlHost = Browser.getHost(link.getPluginPatternMatcher(), true);
        final List<String> deadDomains = this.getDeadDomains();
        String domainToUse;
        if (deadDomains != null && deadDomains.contains(urlHost)) {
            domainToUse = this.getHost();
        } else {
            domainToUse = urlHost;
        }
        final String protocol = getProtocol(link.getPluginPatternMatcher());
        domainToUse = this.appendWWWIfRequired(domainToUse);
        return protocol + domainToUse;
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

    /** See https://fhscript.com/themes/spirit/assets/js/global.js */
    private String websiteGenerateRandomAPIKey() {
        final String possibleChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            sb.append(possibleChars.charAt(new Random().nextInt(possibleChars.length() - 1)));
        }
        return sb.toString();
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
    protected static final String PROPERTY_API_KEY1                  = "API_KEY1";
    protected static final String PROPERTY_API_KEY2                  = "API_KEY2";
    protected static final String API_LOGIN_HAS_BEEN_SUCCESSFUL_ONCE = "API_LOGIN_HAS_BEEN_SUCCESSFUL_ONCE";

    /**
     * If enabled, this plugin will only accept API login credentials (key1 and key2) instead of username and password for login. </br>
     * Also, API usage will be enforced for all other methods (linkcheck & download)!
     */
    protected boolean enableAPIOnlyMode() {
        return false;
    }

    protected boolean canUseAPI() {
        /*
         * 2021-04-27: At this moment an account is required to use the API. In the future users might be able to add apikeys via plugin
         * setting.
         */
        if (findAccountWithAPICredentials() != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * If enabled, this plugin will try to obtain account information from API during account check via website if API credentials are
     * found.
     */
    protected boolean allowToAttemptAPIUsageInWebsiteModeDuringAccountCheck() {
        return true;
    }

    /**
     * If enabled this plugin will attempt to generate an API key if no API key was found and it looks like the website supports that. </br>
     * Example website(s) with broken API: megaup.net, ultimbox.org, secufiles.com
     */
    protected boolean allowToGenerateAPIKeyInWebsiteModeDuringAccountCheck() {
        return true;
    }

    protected Account findAccountWithAPICredentials() {
        final ArrayList<Account> accounts = AccountController.getInstance().getValidAccounts(this.getHost());
        if (accounts != null && accounts.size() > 0) {
            for (final Account account : accounts) {
                if (isWebsiteCrawledAPICredentialsAvailable(account)) {
                    return account;
                }
            }
            if (enableAPIOnlyMode()) {
                return accounts.get(0);
            }
        }
        return null;
    }

    /** Returns true if API credentials that have been crawled via API are available. */
    protected boolean isWebsiteCrawledAPICredentialsAvailable(final Account account) {
        if (account == null) {
            return false;
        } else {
            return account.hasProperty(PROPERTY_API_KEY1) && account.hasProperty(PROPERTY_API_KEY2);
        }
    }

    /**
     * true = API will be used for downloading whenever possible (usually, a special internal fileID is required to download files via API).
     * </br>
     * This ID is also used for some website requests but this method is not related to website requests!! </br>
     * Only use this after testing!
     */
    protected boolean supportsAPIDownloads(final DownloadLink link, final Account account) {
        return false;
        /* You would typically use the line of code below when overriding this. */
        // return enableAPIOnlyMode() || (this.getApiFileID(link) != null && this.canUseAPI());
    }

    protected String getAPIBase() {
        return "https://" + this.getHost() + "/api/v2";
    }

    /** Only use this for hosts for which we're using the API only without making use of their website! */
    protected Browser prepBrowserAPI(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    /** 2020-08-26: Same pattern for user & pw ("key1" & "key2") */
    protected boolean isAPICredential(final String str) {
        return str != null && str.matches("[A-Za-z0-9]{64}");
    }

    /** If this returns true, linkcheck when adding links/manual linkcheck will be performed via API. */
    protected boolean supportsAPISingleAvailablecheck(final DownloadLink link) {
        return false;
        /* Typically use the line below to enable this */
        // return enableAPIOnlyMode() || (this.getApiFileID(link) != null && this.canUseAPI());
    }

    protected String getStoredAPIAccessToken(final Account account, final String key1, final String key2) {
        return account.getStringProperty(PROPERTY_API_ACCESS_TOKEN + Hash.getSHA256(key1 + ":" + key2));
    }

    protected String getStoredAPIAccessToken(final Account account) {
        synchronized (account) {
            final String key1;
            final String key2;
            if (this.enableAPIOnlyMode()) {
                key1 = account.getUser();
                key2 = account.getPass();
            } else {
                key1 = account.getStringProperty(PROPERTY_API_KEY1);
                key2 = account.getStringProperty(PROPERTY_API_KEY2);
            }
            return getStoredAPIAccessToken(account, key1, key2);
        }
    }

    protected String getAPIAccountID(final Account account, final String key1, final String key2) {
        return account.getStringProperty(PROPERTY_API_ACCOUNT_ID + Hash.getSHA256(key1 + ":" + key2));
    }

    protected String getAPIAccountID(final Account account) {
        synchronized (account) {
            final String key1;
            final String key2;
            if (this.enableAPIOnlyMode()) {
                key1 = account.getUser();
                key2 = account.getPass();
            } else {
                key1 = account.getStringProperty(PROPERTY_API_KEY1);
                key2 = account.getStringProperty(PROPERTY_API_KEY2);
            }
            return getAPIAccountID(account, key1, key2);
        }
    }

    /**
     * According to: https://fhscript.com/api#account-info </br>
     * and: https://fhscript.com/api#account-package </br>
     */
    protected AccountInfo fetchAccountInfoAPI(final Browser br, final Account account, final String key1, final String key2) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.loginAPI(br, account, key1, key2, true);
        final UrlQuery query = new UrlQuery();
        query.add("access_token", getStoredAPIAccessToken(account, key1, key2));
        query.add("account_id", getAPIAccountID(account, key1, key2));
        if (br.getURL() == null || !br.getURL().contains("/account/info")) {
            this.getPage(br, this.getAPIBase() + "/account/info?access_token=" + getStoredAPIAccessToken(account, key1, key2) + "&account_id=" + getAPIAccountID(account, key1, key2));
            /* We don't expect any errors to happen at this stage but we can never know... */
            checkErrorsAPI(br, null, account);
        }
        final Map<String, Object> jsonRoot = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String server_timeStr = (String) jsonRoot.get("_datetime");
        final Map<String, Object> data = (Map<String, Object>) jsonRoot.get("data");
        // final String username = (String) entries.get("username");
        // final String status = (String) entries.get("status"); --> Mostly "active" (also free accounts)
        final String datecreatedStr = (String) data.get("datecreated");
        final String premiumExpireDateStr = (String) data.get("paidExpiryDate");
        long premiumExpireMilliseconds = 0;
        final long currentTime;
        if (server_timeStr != null && server_timeStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            currentTime = TimeFormatter.getMilliSeconds(server_timeStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        } else {
            /* Fallback */
            logger.warning("Failed to find serverside _datetime field");
            currentTime = System.currentTimeMillis();
        }
        if (premiumExpireDateStr != null && premiumExpireDateStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            premiumExpireMilliseconds = TimeFormatter.getMilliSeconds(premiumExpireDateStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        this.getPage(br, this.getAPIBase() + "/account/package?access_token=" + getStoredAPIAccessToken(account, key1, key2) + "&account_id=" + getAPIAccountID(account, key1, key2));
        final Map<String, Object> packageInfoRoot = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> packageInfoData = (Map<String, Object>) packageInfoRoot.get("data");
        final String accountType = (String) packageInfoData.get("label");
        final String level_type = (String) packageInfoData.get("level_type");
        checkErrorsAPI(br, null, account);
        final long premiumDurationMilliseconds = premiumExpireMilliseconds - currentTime;
        if (premiumExpireMilliseconds > currentTime || StringUtils.equalsIgnoreCase(level_type, "paid")) {
            account.setType(AccountType.PREMIUM);
            if (premiumExpireMilliseconds > currentTime) {
                ai.setValidUntil(currentTime + premiumDurationMilliseconds);
            }
        } else {
            /* Free- or expired premium account */
            account.setType(AccountType.FREE);
        }
        ai.setStatus(accountType);
        final Object concurrent_downloadsO = packageInfoData.get("concurrent_downloads");
        if (concurrent_downloadsO != null && (concurrent_downloadsO instanceof Number || concurrent_downloadsO.toString().matches("\\d+"))) {
            final int simultaneousDownloads;
            if (concurrent_downloadsO instanceof Number) {
                simultaneousDownloads = ((Number) concurrent_downloadsO).intValue();
            } else {
                simultaneousDownloads = Integer.parseInt(concurrent_downloadsO.toString());
            }
            if (simultaneousDownloads == 0) {
                logger.info("Max. concurrent downloads for this account according to API: Unlimited");
                account.setMaxSimultanDownloads(-1);
            } else {
                logger.info("Max. concurrent downloads for this account according to API: " + simultaneousDownloads);
                account.setMaxSimultanDownloads(simultaneousDownloads);
            }
        }
        /* Now set unnecessary data */
        if (datecreatedStr != null && datecreatedStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            ai.setCreateTime(TimeFormatter.getMilliSeconds(datecreatedStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        } else {
            /* This should never happen */
            logger.warning("datecreated field invalid or not given via API");
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            if (this.isNewYetiShareVersion(account)) {
                ai.setStatus("[NewYetiShare][API] " + ai.getStatus());
            } else {
                ai.setStatus("[OldYetiShare][API] " + ai.getStatus());
            }
        }
        return ai;
    }

    /** Login according to: https://fhscript.com/api#authorize */
    protected void loginAPI(final Browser br, final Account account, final String key1, final String key2, final boolean verifyToken) throws Exception {
        final String storedAccessToken = this.getStoredAPIAccessToken(account, key1, key2);
        final String storedAccountID = this.getAPIAccountID(account, key1, key2);
        if (this.enableAPIOnlyMode()) {
            prepBrowserAPI(br);
        }
        if (!StringUtils.isEmpty(storedAccessToken) && !StringUtils.isEmpty(storedAccountID)) {
            logger.info("Trying to re-use stored access_token");
            if (!verifyToken) {
                logger.info("Trust existing access_token");
                return;
            }
            this.getPage(br, this.getAPIBase() + "/account/info?access_token=" + storedAccessToken + "&account_id=" + storedAccountID);
            try {
                checkErrorsAPI(br, null, account);
                final Map<String, Object> jsonRoot = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final Map<String, Object> data = (Map<String, Object>) jsonRoot.get("data");
                final Object currentAccountID = data != null ? data.get("id").toString() : null;
                if (currentAccountID != null) {
                    /* Compare accountID with stored accountID --> If it matches, we trust login to be successful */
                    if (StringUtils.equals(currentAccountID.toString(), storedAccountID)) {
                        logger.info("Successfully re-used access_token");
                        return;
                    }
                }
                logger.info("Failed to re-use access_token");
            } catch (final Exception ignore) {
                /*
                 * 2020-09-10: E.g. misleading response on expired access_token: {"message":"Username could not be found.","result":false}
                 */
                logger.log(ignore);
                logger.info("Failed to re-use access_token due to Exception");
            }
        }
        logger.info("Performing full API login");
        if (!this.isAPICredential(key1) || !this.isAPICredential(key2)) {
            /*
             * Only display dialog to user if we expect user to enter API credentials. Apart from that API login can also be used in website
             * mode in an automated way in which case we do not want to bother our user with login dialogs!
             */
            if (this.enableAPIOnlyMode()) {
                showAPILoginInformation(account);
            }
            throw new AccountInvalidException("Invalid API credentials");
        }
        this.postPage(br, this.getAPIBase() + "/authorize", "key1=" + Encoding.urlEncode(key1) + "&key2=" + Encoding.urlEncode(key2));
        checkErrorsAPI(br, null, account);
        /*
         * 2021-04-22: Token should be valid for at least 60 minutes but in my tests it lasted more like... 60 seconds! As long as it is
         * only used for account checking that is fine though.
         */
        final Map<String, Object> authorizationResponse = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> authorizationData = (Map<String, Object>) authorizationResponse.get("data");
        final String accessToken = authorizationData.get("access_token").toString();
        final String accountIDStr = authorizationData.get("account_id").toString();
        /*
         * 2020-08-27: API can basically return anything except expected json --> Do not check for errors here - just check for the expected
         * token --> Account should be invalid if token is not available. Only check for errors if this account has been valid before
         * already!
         */
        if (StringUtils.isEmpty(accessToken) || StringUtils.isEmpty(accountIDStr) || !accountIDStr.matches("\\d+")) {
            /* This should never happen as we've checked for errors already! */
            throw new AccountInvalidException("Authorization API broken?");
        }
        account.setProperty(PROPERTY_API_ACCESS_TOKEN + Hash.getSHA256(key1 + ":" + key2), accessToken);
        account.setProperty(PROPERTY_API_ACCOUNT_ID + Hash.getSHA256(key1 + ":" + key2), accountIDStr);
        account.setProperty(API_LOGIN_HAS_BEEN_SUCCESSFUL_ONCE + Hash.getSHA256(key1 + ":" + key2), true);
    }

    protected AvailableStatus requestFileInformationAPI(final Browser br, final DownloadLink link, final Account account) throws Exception {
        final UrlQuery query = new UrlQuery();
        query.add("access_token", this.getStoredAPIAccessToken(account));
        query.add("account_id", this.getAPIAccountID(account));
        query.add("file_id", this.getStoredInternalFileID(link));
        /* Availablecheck not required as we do check for offline here! */
        if (this.enableAPIOnlyMode()) {
            prepBrowserAPI(br);
        }
        this.getPage(br, this.getAPIBase() + "/file/info?" + query.toString());
        this.checkErrorsAPI(br, link, account);
        final Map<String, Object> jsonRoot = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> data = (Map<String, Object>) jsonRoot.get("data");
        final String filename = (String) data.get("filename");
        final Number filesize = (Number) data.get("fileSize");
        final String file_status = data.get("file_status").toString();
        final String description = (String) data.get("description");
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        if (filesize != null) {
            link.setVerifiedFileSize(filesize.longValue());
        }
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        /* 2021-04-23: Not sure but let's assume there can be files with filename and size still available but offline status! */
        if (!file_status.equalsIgnoreCase("active")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    private Thread showAPILoginInformation(final Account account) throws PluginException {
        final String host = this.getHost();
        final String editAccountURL = this.getAccountNameSpaceEditAccount(account);
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = host + " - Login";
                        message += "Hallo liebe(r) " + host + " NutzerIn\r\n";
                        message += "Um deinen " + host + " Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch geschieht und logge dich auf der Webseite ein:\r\n\t'" + editAccountURL + "'\t\r\n";
                        message += "2. Scrolle herunter bis du die Eingabefelder \"Key 1\" und \"Key 2\" siehst oder klicke rechts auf 'API Access'.\r\n";
                        message += "3. Klicke rechts in beiden Eingabefeldern auf \"Generate\".\r\n";
                        message += "4. Scrolle bis ans Ende der Webseite und klicke auf \"Update Account\".\r\n";
                        message += "5. Wechsle in JD, schließe diesen Dialog und öffne erneut die Maske um einen Account für diesen Anbieter hinzuzufügen.\r\n";
                        message += "Gib den Wert von \"Key 1\" als Benutzername- und den Wert von \"Key 2\" als Passwort ein und bestätige deine Eingaben.\r\n";
                        message += "Dein Account sollte nach einigen Sekunden von JDownloader akzeptiert werden.\r\n";
                    } else {
                        title = host + " - Login";
                        message += "Hello dear " + host + " user\r\n";
                        message += "In order to use your account of this service in JDownloader, you need to follow these steps:\r\n";
                        message += "1. Open this URL in your browser if it is not opened automatically and login in your browser:\r\n\t'" + editAccountURL + "'\t\r\n";
                        message += "2. Scroll down until you see the fields \"Key 1\" and \"Key 2\" or click on the tab 'API Access' (right side).\r\n";
                        message += "3. For each of both fields there is a \"Generate\" button on the right side - click both of them.\r\n";
                        message += "4. Scroll down all the way and confirm your generated keys by clicking on \"Update Account\".\r\n";
                        message += "5. Now go back into JD, close this dialog and re-open the \"Add account\" dialog for this host.\r\n";
                        message += "Enter the values of \"Key 1\" an \"Key 2\" into the username & password fields and confirm.\r\n";
                        message += "Your account should be accepted in JDownloader within a few seconds.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(editAccountURL);
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

    /**
     * https://fhscript.com/api#file-download </br>
     * This API call only works with self-uploaded files and/or whenever the internal fileID is given --> Most of all times it is of no use
     * for us!
     */
    protected void handleDownloadAPI(final DownloadLink link, final Account account, final String apikey1, final String apikey2) throws StorageException, Exception {
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        if (this.checkDirectLink(link, account) == null) {
            /* Login not really required as it won't do anything but check validity of APIKey strings here! */
            this.loginAPI(this.br, account, apikey1, apikey2, false);
            /* Headers are set in login! */
            // if (this.enableAPIOnlyMode()) {
            // prepBrowserAPI(br);
            // }
            final UrlQuery query = new UrlQuery();
            query.add("access_token", this.getStoredAPIAccessToken(account, apikey1, apikey2));
            query.add("account_id", this.getAPIAccountID(account, apikey1, apikey2));
            query.add("file_id", getStoredInternalFileID(link));
            /* Availablecheck not required as download call will return offline message. */
            // this.requestFileInformationAPI(link, account);
            this.getPage(this.getAPIBase() + "/file/download?" + query.toString());
            this.checkErrorsAPI(this.br, link, account);
            final Map<String, Object> jsonRoot = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> data = (Map<String, Object>) jsonRoot.get("data");
            final String dllink = (String) data.get("download_url");
            if (StringUtils.isEmpty(dllink)) {
                /* We're using an API --> Never throw plugin defect! */
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl");
            }
            final String filename = (String) data.get("filename");
            if (!StringUtils.isEmpty(filename)) {
                link.setFinalFileName(filename);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
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
            br.followConnection(true);
            checkErrors(br, link, account);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to downloadable content");
        }
        dl.setFilenameFix(isContentDispositionFixRequired(dl, con, link));
        dl.startDownload();
    }

    /**
     * Handles API errormessages. </br>
     * We usually can't use this API for downloading thus all Exceptions will be account related (as of 2021-04-22)
     */
    protected void checkErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        Map<String, Object> entries = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } catch (final Exception e) {
            /* API response is not json */
            throw new AccountUnavailableException("Invalid API response (no json)", 1 * 60 * 1000l);
        }
        if (entries.containsKey("data")) {
            /* No error */
            return;
        }
        final Object statusO = entries.get("_status");
        if (statusO != null) {
            /* Collection of newer json errormessages */
            /*
             * { "response":
             * "Your account level does not have access to the file upload API. Please contact site support for more information.",
             * "_status": "error", "_datetime": "2021-04-01 11:04:23"}
             */
            /*
             * 2021-04-22: {"response":"Could not validate access_token, please reauthenticate or try again.","_status":"error",
             * "_datetime":"2021-04-22 20:22:08"}
             */
            final String status = statusO.toString();
            if (status.equalsIgnoreCase("error")) {
                final String errorMsg = (String) entries.get("response");
                if (StringUtils.isEmpty(errorMsg)) {
                    /* This should never happen */
                    throw new AccountUnavailableException("Unknown API error", 10 * 60 * 1000l);
                } else if (errorMsg.equalsIgnoreCase("Could not find file based on file_id.")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (errorMsg.equalsIgnoreCase("Your account level does not have access to the file upload API. Please contact site support for more information.")) {
                    throw new AccountInvalidException("API cannot be used with this account");
                } else {
                    logger.info("API returned errormessage: " + errorMsg);
                    throw new AccountUnavailableException(errorMsg, 5 * 60 * 1000l);
                }
            }
        } else {
            /* Handling for older json (= older YetiShare versions) */
            /* Collection of older json errormessages */
            /* E.g. {"message":"Username could not be found.","result":false} */
            /* {"status":"error","response":"User not found.","_datetime":"2020-09-03 13:48:46"} */
            /* {"success":false,"message":"You can`t download files of this size."} */
            /*
             * {"status":"error",
             * "response":"Your account level does not have access to the file upload API. Please contact site support for more information."
             * ,"_datetime":"2021-01-19 16:48:28"}
             * {"status":"error","response":"Please provide a username.","_datetime":"2021-05-11 10:45:58"}
             */
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
                    throw new AccountUnavailableException(msg, 5 * 60 * 1000l);
                }
            }
        }
    }
}