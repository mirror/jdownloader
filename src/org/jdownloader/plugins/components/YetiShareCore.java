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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class YetiShareCore extends antiDDoSForHost {
    public YetiShareCore(PluginWrapper wrapper) {
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
     * For sites which use this script: http://www.yetishare.com/<br />
     * YetiShareCore Version 2.0.0.6-psp<br />
     * mods: see overridden functions in host plugins<br />
     * limit-info:<br />
     * captchatype: null, solvemedia, reCaptchaV2<br />
     * other: Last compatible YetiShareBasic Version: YetiShareBasic 1.2.0-psp<br />
     * Another alternative method of linkchecking (displays filename only): host.tld/<fid>~s (statistics)
     */
    @Override
    public String getAGBLink() {
        return this.getMainPage() + "/terms.html";
    }

    public String getPurchasePremiumURL() {
        return this.getMainPage() + "/register.html";
    }

    // private static final boolean enable_regex_stream_url = true;
    private static AtomicReference<String> agent = new AtomicReference<String>(null);

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* link cleanup, but respect users protocol choosing or forced protocol */
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), "^https?://[^/]+/([A-Za-z0-9]+)");
        final String fid = urlinfo.getMatch(0);
        final String protocol;
        if (supports_https()) {
            protocol = "https";
        } else {
            protocol = "http";
        }
        link.setPluginPatternMatcher(String.format("%s://%s/%s", protocol, this.getHost(), fid));
        link.setLinkID(fid);
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
     *         false: Implies that website does NOT show filename & filesize via website.tld/<fuid>~i. <br />
     *         default: true
     */
    public boolean supports_availablecheck_over_info_page() {
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
     * TODO: 2019-02-20: Find website which supports video streaming!
     *
     * @return true: Implies that website supports embedding videos. <br />
     *         false: Implies that website does NOT support embedding videos. <br />
     *         default: false
     */
    protected boolean supports_embed_stream_download() {
        return false;
    }

    /**
     * When checking previously generated direct-URLs, this will count as an open connection so if the host only supports one connection at
     * a time, trying to download such an URL immediately after the check will result in an error so this is the time we wait before trying
     * to start the download with this URL.<br />
     * default: 8
     */
    public int getWaitTimeSecondsAfterDirecturlCheck() {
        return 8;
    }

    /** Returns empty StringArray for filename, filesize, [more information in the future?] */
    protected String[] getFileInfoArray() {
        return new String[2];
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setWeakFilename(link);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser(this.br);
        final String fallback_filename = this.getFallbackFilename(link);
        final String[] fileInfo = getFileInfoArray();
        try {
            if (supports_availablecheck_over_info_page()) {
                getPage(link.getPluginPatternMatcher() + "~i");
                if (!br.getURL().contains("~i") || br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else {
                getPage(link.getPluginPatternMatcher());
                if (isWaitBetweenDownloadsURL()) {
                    return AvailableStatus.TRUE;
                } else if (new Regex(br.getURL(), Pattern.compile(".*?e=Error%3A\\+Could\\+not\\+open\\+file\\+for\\+reading.*?", Pattern.CASE_INSENSITIVE)).matches()) {
                    return AvailableStatus.TRUE;
                } else if (isPremiumOnlyURL()) {
                    return AvailableStatus.TRUE;
                }
                final boolean isFileWebsite = br.containsHTML("class=\"downloadPageTable(V2)?\"") || br.containsHTML("class=\"download\\-timer\"");
                final boolean isErrorPage = br.getURL().contains("/error.html") || br.getURL().contains("/index.html");
                final boolean isOffline = br.getHttpConnection().getResponseCode() == 404;
                if (!isFileWebsite || isErrorPage || isOffline) {
                    checkErrors(link, null);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            scanInfo(fileInfo);
            if (StringUtils.isEmpty(fileInfo[0])) {
                /* Final fallback - this should never happen! */
                fileInfo[0] = fallback_filename;
            }
            fileInfo[0] = Encoding.htmlDecode(fileInfo[0]).trim();
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
    public String[] scanInfo(final String[] fileInfo) {
        if (supports_availablecheck_over_info_page()) {
            final String[] tableData = this.br.getRegex("class=\"responsiveInfoTable\">([^<>\"/]*?)<").getColumn(0);
            /* Sometimes we get crippled results with the 2nd RegEx so use this one first */
            if (StringUtils.isEmpty(fileInfo[0])) {
                fileInfo[0] = this.br.getRegex("data\\-animation\\-delay=\"\\d+\">(?:Information about|Informacion) ([^<>\"]*?)</div>").getMatch(0);
            }
            if (StringUtils.isEmpty(fileInfo[0])) {
                /* "Information about"-filename-trait without the animation(delay). */
                fileInfo[0] = this.br.getRegex("class=\"description\\-1\">Information about ([^<>\"]+)<").getMatch(0);
            }
            if (StringUtils.isEmpty(fileInfo[0])) {
                fileInfo[0] = this.br.getRegex("(?:Filename|Dateiname|اسم الملف|Nome|Dosya Adı):[\t\n\r ]*?</td>[\t\n\r ]*?<td(?: class=\"responsiveInfoTable\")?>([^<>\"]*?)<").getMatch(0);
            }
            if (StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = br.getRegex("(?:Filesize|Dateigröße|حجم الملف|Tamanho|Boyut):[\t\n\r ]*?</td>[\t\n\r ]*?<td(?: class=\"responsiveInfoTable\")?>([^<>\"]*?)<").getMatch(0);
            }
            try {
                /* Language-independant attempt ... */
                if (StringUtils.isEmpty(fileInfo[0])) {
                    fileInfo[0] = tableData[0];
                }
                if (StringUtils.isEmpty(fileInfo[1])) {
                    fileInfo[1] = tableData[1];
                }
            } catch (final Throwable e) {
            }
        } else {
            final Regex fInfo = br.getRegex("<strong>([^<>\"]*?) \\((\\d+(?:,\\d+)?(?:\\.\\d+)? (?:KB|MB|GB))\\)<");
            if (StringUtils.isEmpty(fileInfo[0])) {
                fileInfo[0] = fInfo.getMatch(0);
            }
            if (StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = fInfo.getMatch(1);
            }
            if (StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = br.getRegex("(\\d+(?:,\\d+)?(\\.\\d+)? (?:KB|MB|GB))").getMatch(0);
            }
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
        boolean skipWaittime = false;
        String continue_link = null;
        boolean captcha = false;
        boolean success = false;
        final long timeBeforeDirectlinkCheck = System.currentTimeMillis();
        long timeBeforeCaptchaInput;
        continue_link = checkDirectLink(link, directlinkproperty);
        br.setFollowRedirects(false);
        if (continue_link != null) {
            logger.info("Using previously stored direct-url");
            /*
             * Let the server 'calm down' (if it was slow before) otherwise it will thing that we tried to open two connections as we
             * checked the directlink before and return an error.
             */
            if ((System.currentTimeMillis() - timeBeforeDirectlinkCheck) > 1500) {
                sleep(getWaitTimeSecondsAfterDirecturlCheck() * 1000l, link);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_link, resume, maxchunks);
        } else {
            // if (supports_embed_stream_download()) {
            // try {
            // final Browser br2 = this.br.cloneBrowser();
            // getPage(br2, String.format("/embed/u=%s/", this.getFID(link)));
            // continue_link = this.getStreamUrl(br2);
            // } catch (final BrowserException e) {
            // }
            // }
            if (supports_availablecheck_over_info_page()) {
                getPage(link.getPluginPatternMatcher());
                /* For premium mode, we might get our final downloadurl here already. */
                final String redirect = this.br.getRedirectLocation();
                if (redirect != null && isDownloadlink(redirect)) {
                    continue_link = br.getRedirectLocation();
                } else if (redirect != null) {
                    /* Follow redirect */
                    br.setFollowRedirects(true);
                    getPage(redirect);
                    br.setFollowRedirects(false);
                }
            }
            if (continue_link == null) {
                checkErrors(link, account);
            }
            /* Passwords are usually before waittime. */
            handlePassword(link);
            /* Handle up to 3 pre-download pages before the (eventually existing) captcha */
            final int startValue = 1;
            for (int i = startValue; i <= 5; i++) {
                logger.info("Handling pre-download page #" + i);
                timeBeforeCaptchaInput = System.currentTimeMillis();
                if (continue_link == null || i > startValue) {
                    continue_link = getContinueLink();
                }
                if (isDownloadlink(continue_link)) {
                    /*
                     * If we already found a downloadlink let's try to download it because html can still contain captcha html --> We don't
                     * need a captcha in this case/loop/pass for sure! E.g. host '3rbup.com'.
                     */
                    waitTime(link, timeBeforeCaptchaInput, skipWaittime);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_link, resume, maxchunks);
                } else {
                    Form continue_form = null;
                    if (!StringUtils.isEmpty(continue_link)) {
                        continue_form = new Form();
                        continue_form.setMethod(MethodType.GET);
                        continue_form.setAction(continue_link);
                        continue_form.put("submit", "Submit");
                        continue_form.put("submitted", "1");
                        continue_form.put("d", "1");
                    }
                    if (i == startValue && continue_form == null) {
                        logger.info("No continue_form/continue_link available, plugin broken");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else if (continue_form == null) {
                        logger.info("No continue_form/continue_link available, stepping out of pre-download loop");
                        break;
                    } else {
                        logger.info("Found continue_form/continue_link, continuing...");
                    }
                    final String rcID = br.getRegex("recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
                    if (br.containsHTML("data\\-sitekey=|g\\-recaptcha\\'")) {
                        captcha = true;
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        success = true;
                        waitTime(link, timeBeforeCaptchaInput, skipWaittime);
                        continue_form.put("capcode", "false");
                        continue_form.put("g-recaptcha-response", recaptchaV2Response);
                        continue_form.setMethod(MethodType.POST);
                        dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_form, resume, maxchunks);
                    } else if (rcID != null) {
                        /* Dead end! */
                        captcha = true;
                        success = false;
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Website uses reCaptchaV1 which has been shut down by Google. Contact website owner!");
                    } else if (br.containsHTML("solvemedia\\.com/papi/")) {
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
                                throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                            }
                            throw e;
                        }
                        final String code = getCaptchaCode("solvemedia", cf, link);
                        final String chid = sm.getChallenge(code);
                        waitTime(link, timeBeforeCaptchaInput, skipWaittime);
                        continue_form.put("adcopy_challenge", Encoding.urlEncode(chid));
                        continue_form.put("adcopy_response", Encoding.urlEncode(code));
                        continue_form.setMethod(MethodType.POST);
                        dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_form, resume, maxchunks);
                    } else {
                        success = true;
                        waitTime(link, timeBeforeCaptchaInput, skipWaittime);
                        /* Use URL instead of Form - it is all we need! */
                        dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_link, resume, maxchunks);
                    }
                }
                checkResponseCodeErrors(dl.getConnection());
                if (dl.getConnection().isContentDisposition()) {
                    success = true;
                    break;
                }
                br.followConnection();
                checkErrors(link, account);
                if (captcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    logger.info("Wrong captcha");
                    continue;
                }
            }
        }
        /*
         * Save directurl before download-attempt as it should be valid even if it e.g. fails because of server issue 503 (= too many
         * connections) --> Should work fine after the next try.
         */
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        checkResponseCodeErrors(dl.getConnection());
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (captcha && !success) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            checkErrors(link, account);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    protected String getContinueLink() {
        String continue_link = br.getRegex("\\$\\(\\'\\.download\\-timer\\'\\)\\.html\\(\"<a href=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
        if (continue_link == null) {
            continue_link = br.getRegex("class=\\'btn btn\\-free\\' href=\\'(https?://[^<>\"]*?)\\'>").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("<div class=\"captchaPageTable\">[\t\n\r ]+<form method=\"POST\" action=\"(https?://[^<>\"]*?)\"").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("(?:\"|\\')(https?://(?:www\\.)?[^/]+/[^<>\"\\']*?pt=[^<>\"\\']*?)(?:\"|\\')").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = getDllink();
        }
        /** 2019-02-21: TODO: Find website which embeds videos / has video streaming activated! */
        // if (continue_link == null && enable_regex_stream_url) {
        // continue_link = getStreamUrl();
        // }
        return continue_link;
    }

    // private String getStreamUrl() {
    // return getStreamUrl(this.br);
    // }
    //
    // private String getStreamUrl(final Browser br) {
    // return br.getRegex("file\\s*?:\\s*?\"(https?://[^<>\"]+)\"").getMatch(0);
    // }
    private String getDllink() {
        return getDllink(this.br);
    }

    private String getDllink(final Browser br) {
        String dllink = br.getRegex("\"(https?://[A-Za-z0-9\\.\\-]+\\.[^/]+/[^<>\"]*?(?:\\?|\\&)download_token=[A-Za-z0-9]+[^<>\"]*?)\"").getMatch(0);
        return dllink;
    }

    public boolean isDownloadlink(final String url) {
        if (url == null) {
            return false;
        }
        final boolean isdownloadlink = url.contains("download_token=");
        return isdownloadlink;
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
            fallback_filename = this.getFUIDFromURL(dl);
        }
        return fallback_filename;
    }

    /** Tries to get filename from URL and if this fails, will return <fuid> filename. */
    public static String getFallbackFilename(final String url) {
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
            postPage(br.getURL(), "submit=access+file&submitme=1&file=" + this.getFUIDFromURL(dl) + "&filePassword=" + Encoding.urlEncode(passCode));
            if (br.getURL().contains("/file_password.html")) {
                logger.info("User entered incorrect password --> Retrying");
                dl.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            logger.info("User entered correct password --> Continuing");
        }
    }

    /** Handles pre download (pre-captcha) waittime. */
    private void waitTime(final DownloadLink downloadLink, final long timeBefore, final boolean skipWaittime) throws PluginException {
        if (skipWaittime) {
            logger.info("Skipping waittime");
        } else {
            final int extraWaitSeconds = 2;
            int wait = 0;
            int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - extraWaitSeconds;
            /* Ticket Time */
            final String ttt = regexWaittime();
            if (ttt != null && ttt.matches("\\d+")) {
                logger.info("Found waittime, parsing waittime: " + ttt);
                wait = Integer.parseInt(ttt);
            }
            /*
             * Check how much time has passed during eventual captcha event before this function has been called and see how much time is
             * left to wait.
             */
            wait -= passedTime;
            if (passedTime > 0) {
                /* This usually means that the user had to solve a captcha which cuts down the remaining time we have to wait. */
                logger.info("Total passed time during captcha: " + passedTime);
            }
            if (wait > 0) {
                logger.info("Waiting waittime: " + wait);
                sleep(wait * 1000l, downloadLink);
            } else if (wait < -extraWaitSeconds) {
                /* User needed more time to solve the captcha so there is no waittime left :) */
                logger.info("Congratulations: Time to solve captcha was higher than waittime");
            } else {
                /* No waittime at all */
                logger.info("Found no waittime");
            }
        }
    }

    public void checkErrors(final DownloadLink link, final Account account) throws PluginException {
        if (br.containsHTML("Error: Too many concurrent download requests")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 3 * 60 * 1000l);
        } else if (new Regex(br.getURL(), Pattern.compile(".*?e=You\\+have\\+reached\\+the\\+maximum\\+concurrent\\+downloads.*?", Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max. simultan downloads limit reached, wait to start more downloads", 1 * 60 * 1000l);
        } else if (br.getURL().contains("error.php?e=Error%3A+Could+not+open+file+for+reading")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
        } else if (isWaitBetweenDownloadsURL()) {
            final String wait_minutes = new Regex(br.getURL(), "wait\\+(\\d+)\\+minutes?").getMatch(0);
            final String errormessage = "You must wait between downloads!";
            if (wait_minutes != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errormessage, Integer.parseInt(wait_minutes) * 60 * 1001l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errormessage, 10 * 60 * 1001l);
        } else if (isPremiumOnlyURL()) {
            throw new AccountRequiredException();
        } else if (br.getURL().contains("You+have+reached+the+maximum+permitted+downloads+in")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily limit reached", 3 * 60 * 60 * 1001l);
        } else if (br.toString().equals("unknown user")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Unknown user'", 30 * 60 * 1000l);
        }
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
        } else if (responsecode == 429) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 429 connection limit reached, please contact our support!", 5 * 60 * 1000l);
        }
    }

    /**
     * Checks premiumonly status via current Browser-URL.
     *
     * @return true: Link only downloadable for premium users (sometimes also for registered users). <br />
     *         false: Link is downloadable for all users.
     */
    public boolean isPremiumOnlyURL() {
        return br.getURL() != null && new Regex(br.getURL(), Pattern.compile("(.+e=You\\+must\\+register\\+for\\+a\\+premium\\+account\\+to.+|.+/register\\..+)", Pattern.CASE_INSENSITIVE)).matches();
    }

    /**
     * Checks 'wait between downloads' status via current Browser-URL.
     *
     * @return true: User has to wait before new downloads can be started. <br />
     *         false: User can start new downloads right away.
     */
    public boolean isWaitBetweenDownloadsURL() {
        return br.getURL() != null && new Regex(br.getURL(), Pattern.compile(".*?e=You\\+must\\+wait\\+.*?", Pattern.CASE_INSENSITIVE)).matches();
    }

    /** Returns pre-download-waittime (seconds) from inside HTML. */
    public String regexWaittime() {
        String ttt = this.br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
        if (ttt == null) {
            ttt = this.br.getRegex("var\\s*?seconds\\s*?=\\s*?(\\d+);").getMatch(0);
        }
        return ttt;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            final Browser br2 = this.br.cloneBrowser();
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (br2.getHttpConnection().getResponseCode() == 429) {
                    /*
                     * Too many connections but that does not mean that our downloadlink is valid. Accept it and if it still returns 429 on
                     * download-attempt this error will get displayed to the user.
                     */
                    logger.info("Stored directurl lead to 429 | too many connections");
                    return dllink;
                }
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

    protected Browser prepBrowser(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 416, 429 });
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
                    getPage(this.getMainPage() + "/account_home.html");
                    loggedInViaCookies = br.containsHTML("/logout.html");
                }
                if (loggedInViaCookies) {
                    /* No additional check required --> We know cookies are valid and we're logged in --> Done! */
                    logger.info("Successfully logged in via cookies");
                } else {
                    logger.info("Performing full login");
                    getPage(this.getProtocol() + this.getHost() + "/login.html");
                    final String loginstart = new Regex(br.getURL(), "(https?://(www\\.)?)").getMatch(0);
                    Form loginform;
                    if (br.containsHTML("flow\\-login\\.js")) {
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
                        /* Old login method - rare case! Example: udrop.net */
                        logger.info("Using old login method");
                        loginform = br.getFormbyProperty("id", "form_login");
                        if (loginform == null) {
                            logger.info("Fallback to custom built loginform");
                            loginform = new Form();
                            loginform.put("submit", "Login");
                            loginform.put("submitme", "1");
                        }
                        loginform.put("username", Encoding.urlEncode(account.getUser()));
                        loginform.put("password", Encoding.urlEncode(account.getPass()));
                        submitForm(loginform);
                        // postPage(this.getProtocol() + this.getHost() + "/login.html", "submit=Login&submitme=1&loginUsername=" +
                        // Encoding.urlEncode(account.getUser()) + "&loginPassword=" + Encoding.urlEncode(account.getPass()));
                        if (br.containsHTML(">Your username and password are invalid<") || !br.containsHTML("/logout\\.html\">")) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        if (br.getURL() == null || !br.getURL().contains("/account_home.html")) {
            getPage("/account_home.html");
        }
        /* 2019-03-01: Bad german translation, example: freefile.me */
        boolean isPremium = br.containsHTML("class=\"badge badge\\-success\">(?:BEZAHLT(er)? BENUTZER|PAID USER|USUARIO DE PAGO|VIP|PREMIUM)</span>");
        if (!isPremium) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(this.getMaxSimultaneousFreeAccountDownloads());
            /* All accounts get the same (IP-based) downloadlimits --> Simultaneous free account usage makes no sense! */
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) account");
        } else {
            getPage("/upgrade.html");
            /* If the premium account is expired we'll simply accept it as a free account. */
            String expireStr = br.getRegex("Reverts To Free Account:[\t\n\r ]+</td>[\t\n\r ]+<td>[\t\n\r ]+(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            if (expireStr == null) {
                /* More wide RegEx to be more language independant (e.g. required for freefile.me) */
                expireStr = br.getRegex(">[\t\n\r ]*?(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})[\t\n\r ]*?<").getMatch(0);
            }
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
        return SiteTemplate.MFScripts_YetiShare;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}