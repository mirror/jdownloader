//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.NitroflareConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class NitroFlareCom extends antiDDoSForHost {
    private final String         staticBaseURL             = "https://nitroflare.com";
    /* Documentation | docs: https://nitroflare.com/member?s=api */
    /* Don't touch the following! */
    private static AtomicInteger maxFree                   = new AtomicInteger(1);
    private final String         PROPERTY_PREMIUM_REQUIRED = "premiumRequired";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "nitroflare.com", "nitroflare.net", "nitro.download" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:view|watch)/([A-Z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    private static String[] getSupportedNamesStatic(final String targetDomain) {
        for (final String[] domains : getPluginDomains()) {
            for (final String domain : domains) {
                if (domain.equals(targetDomain)) {
                    return domains;
                }
            }
        }
        return null;
    }

    public NitroFlareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(null);
    }

    @Override
    public void init() {
        final String[] siteSupportedNames = siteSupportedNames();
        if (siteSupportedNames != null) {
            for (String siteSupportedName : siteSupportedNames) {
                try {
                    Browser.setRequestIntervalLimitGlobal(siteSupportedName, 500);
                } catch (final Throwable t) {
                    logger.log(t);
                }
            }
        }
    }

    /**
     * Use website or API: https://nitroflare.com/member?s=api </br>
     *
     * @return true: Use API for account login and premium downloading </br> false: Use website for everything (except linkcheck)
     */
    private boolean useAPIAccountMode() {
        return PluginJsonConfig.get(NitroflareConfig.class).isUsePremiumAPIEnabled();
    }

    private boolean useAPIFreeMode() {
        /** 2020-07-03: Doesn't work (yet) thus I've removed this setting RE: psp */
        // return PluginJsonConfig.get(NitroflareConfig.class).isUseFreeAPIEnabled();
        return false;
    }

    private static AtomicReference<String> BASE_DOMAIN = new AtomicReference<String>(null);

    /**
     * Finds valid base domain. </br> In some countries some nitroflare domains may be blocked by some ISPs.
     */
    public static String getBaseDomain(final Plugin plugin, final Browser br) throws PluginException {
        synchronized (BASE_DOMAIN) {
            String baseDomain = BASE_DOMAIN.get();
            if (baseDomain == null) {
                final Browser brc;
                if (br != null) {
                    brc = br.cloneBrowser();
                } else {
                    brc = new Browser();
                }
                brc.setFollowRedirects(true);
                for (final String siteSupportedName : getSupportedNamesStatic(plugin.getHost())) {
                    try {
                        brc.getPage("https://" + siteSupportedName);
                        baseDomain = brc.getHost();
                        if (baseDomain != null && brc.containsHTML(siteSupportedName)) {
                            BASE_DOMAIN.set(baseDomain);
                            return baseDomain;
                        }
                    } catch (final IOException e) {
                        plugin.getLogger().log(e);
                    }
                }
                /* Possibly blocked by ISP? */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "No working Nitroflare base domain found! Blocked?!");
            }
            return baseDomain;
        }
    }

    protected String getAPIBase() throws Exception {
        return "https://" + getBaseDomain(this, br) + "/api/v2";
    }

    @Override
    public String getAGBLink() {
        return staticBaseURL + "/tos";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(getCorrectedDownloadURL(link));
    }

    protected String getCorrectedDownloadURL(final DownloadLink link) {
        String host = siteSupportedNames()[0];
        try {
            host = getBaseDomain(this, br);
        } catch (final Exception e) {
            logger.log(e);
        }
        final String fid = getFID(link);
        if (fid == null) {
            throw new WTFException("unable to parse fid:" + link.getPluginPatternMatcher());
        }
        return "https://" + host + "/view/" + fid;
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

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return false;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    private int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    /** Returns property under which direct-urls can be stored/loaded for current download mode. */
    private String getDirectlinkProperty(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return "freelink2";
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return "premlink";
        } else {
            /* Free(anonymous) and unknown account type */
            return "freelink";
        }
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.addAllowedResponseCodes(500);
        }
        return prepBr;
    }

    private boolean isPremiumOnly(final DownloadLink link) {
        return link != null && link.getBooleanProperty(PROPERTY_PREMIUM_REQUIRED, false);
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (link != null) {
            if ((account == null || account.getType() == AccountType.FREE) && isPremiumOnly(link)) {
                return false;
            }
        }
        return true;
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        try {
            final Browser br = new Browser();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    if (links.size() == 100 || index == urls.length) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                final StringBuilder sb = new StringBuilder();
                sb.append("files=");
                boolean atLeastOneDL = false;
                for (final DownloadLink dl : links) {
                    if (atLeastOneDL) {
                        sb.append("%2C");
                    }
                    sb.append(getFID(dl));
                    atLeastOneDL = true;
                }
                getPage(br, getAPIBase() + "/getFileInfo?" + sb);
                if (br.containsHTML("(?i)In these moments we are upgrading the site system")) {
                    for (final DownloadLink dl : links) {
                        dl.getLinkStatus().setStatusText("Nitroflare.com is maintenance mode. Try again later.");
                        dl.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                    }
                    return true;
                }
                final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                final Map<String, Object> result = (Map<String, Object>) entries.get("result");
                /* This will be a map if we checked at least one valid fileID. If all given fileIDs are invalid it will be an empty list! */
                final Object filesO = result.get("files");
                Map<String, Object> filesmap = null;
                if (filesO instanceof Map) {
                    filesmap = (Map<String, Object>) filesO;
                }
                for (final DownloadLink link : links) {
                    final String fid = getFID(link);
                    final Map<String, Object> finfo = filesmap != null ? (Map<String, Object>) filesmap.get(fid) : null;
                    if (finfo == null) {
                        link.setAvailable(false);
                        continue;
                    }
                    if ("online".equalsIgnoreCase(finfo.get("status").toString())) {
                        link.setAvailable(true);
                    } else {
                        /* File is offline but more information about that file can still be available. */
                        link.setAvailable(false);
                    }
                    final String name = (String) finfo.get("name");
                    final Number size = (Number) ReflectionUtils.cast(finfo.get("size"), Long.class);
                    final String md5 = (String) finfo.get("md5");
                    if (name != null) {
                        link.setFinalFileName(name);
                    }
                    if (size != null) {
                        link.setVerifiedFileSize(size.longValue());
                    }
                    if (!StringUtils.isEmpty(md5)) {
                        link.setMD5Hash(md5);
                    }
                    if (PluginJsonConfig.get(NitroflareConfig.class).isTrustAPIAboutPremiumOnlyFlag()) {
                        if ((Boolean) finfo.get("premiumOnly") == Boolean.TRUE) {
                            link.setProperty(PROPERTY_PREMIUM_REQUIRED, true);
                        } else {
                            link.removeProperty(PROPERTY_PREMIUM_REQUIRED);
                        }
                    }
                    if (finfo.get("password") != null) {
                        link.setPasswordProtected(true);
                    } else {
                        link.setPasswordProtected(false);
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformationAPI(link);
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        correctDownloadLink(link);
        getPage(getCorrectedDownloadURL(link));
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*This file has been removed|>\\s*File doesn't exist<|This file has been removed due|>\\s*This file has been removed by its owner")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(this.br.getURL()) && !this.br.getURL().contains(this.getFID(link))) {
            /* 2021-03-25: E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("(?i)<b>File\\s*Name\\s*:\\s*</b><span title=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("alt=\"\" /><span title=\"([^<>\"]*?)\">").getMatch(0);
        }
        final String filesize = br.getRegex("dir=\"ltr\" style=\"text-align: left;\">([^<>\"]*?)</span>").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        this.setBrowserExclusive();
        handleFreeDownload(null, link);
    }

    /** Handles free- and free-account download. */
    private final void handleFreeDownload(final Account account, final DownloadLink link) throws Exception {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        br.setFollowRedirects(true);
        String dllink = link.getStringProperty(getDirectlinkProperty(account));
        if (!inValidate(dllink)) {
            logger.info("Trying to re-use stored generated directurl");
            this.dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
            if (looksLikeDownloadableContent(dl.getConnection())) {
                logger.info("Successfully re-used stored generated directurl");
                link.setProperty(getDirectlinkProperty(account), dllink);
                this.dl.startDownload();
                return;
            } else {
                logger.info("Failed to re-use stored generated directurl");
                this.dl = null;
                link.removeProperty(getDirectlinkProperty(account));
                handleDownloadErrors(account, link, false);
            }
        }
        /* We need to generate a fresh directurl */
        if (useAPIFreeMode()) {
            /* API mode */
            this.getPage(getAPIBase() + "/getDownloadLink?file=" + Encoding.urlEncode(this.getFID(link)));
            this.checkErrorsAPI(br, link, account);
            final String waittime = PluginJSonUtils.getJson(br, "delay");
            final String reCaptchaKey = PluginJSonUtils.getJson(br, "recaptchaPublic");
            String accessLink = PluginJSonUtils.getJson(br, "accessLink");
            if (StringUtils.isEmpty(waittime) || StringUtils.isEmpty(reCaptchaKey)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API error in free download step 1");
            }
            long wait = Long.parseLong(waittime) * 1000l;
            final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey);
            final int reCaptchaV2Timeout = rc2.getSolutionTimeout();
            final long timestampBeforeCaptchaSolving = System.currentTimeMillis();
            if (wait > reCaptchaV2Timeout) {
                final long prePrePreDownloadWait = wait - reCaptchaV2Timeout;
                logger.info("Waittime is higher than reCaptchaV2 timeout --> Waiting a part of it before solving captcha to avoid timeouts");
                logger.info("Pre-pre download waittime seconds: " + (prePrePreDownloadWait / 1000));
                this.sleep(prePrePreDownloadWait, link);
            }
            final String c = rc2.getToken();
            if (inValidate(c)) {
                // fixes timeout issues or client refresh, we have no idea at this stage
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            // remove one second from past, to prevent returning too quickly.
            final long passedTime = (System.currentTimeMillis() - timestampBeforeCaptchaSolving) - 1500;
            wait -= passedTime;
            if (wait > 0) {
                logger.info("Pre- download waittime seconds: " + (wait / 1000));
                sleep(wait, link);
            } else {
                logger.info("Congratulation: Captcha solving took so long that we do not have to wait at all");
            }
            accessLink += "&captcha=" + Encoding.urlEncode(c) + "&g-recaptcha-response=" + Encoding.urlEncode(c);
            this.getPage(accessLink);
            this.checkErrorsAPI(br, link, account);
            dllink = PluginJSonUtils.getJson(br, "url");
            if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API error in free download step 2");
            }
        } else {
            /* Website mode */
            /*
             * First check availablestatus via API because we can be sure that we will get all needed information especially the info
             * whether or not this file is downloadable for free users.
             */
            requestFileInformationAPI(link);
            if (isPremiumOnly(link)) {
                throwPremiumRequiredException(link);
            }
            requestFileInformationWebsite(link);
            handleErrors(account, br, false);
            // randomHash(br, link);
            final Browser ajax = this.setAjaxHeaders(br.cloneBrowser());
            postPage(ajax, "/ajax/setCookie.php", "fileId=" + getFID(link));
            {
                int i = 0;
                while (true) {
                    // lets add some randomization between submitting gotofreepage
                    sleep((new Random().nextInt(5) + 5) * 1000l, link);
                    // first post registers time value
                    postPage(br.getURL(), "goToFreePage=");
                    randomHash(br.cloneBrowser(), link);
                    postPage(ajax, "/ajax/setCookie.php", "fileId=" + getFID(link));
                    if (br.getURL().endsWith("/free")) {
                        break;
                    } else if (++i > 3) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        continue;
                    }
                }
            }
            postPage(ajax, "/ajax/freeDownload.php", "method=startTimer&fileId=" + getFID(link));
            handleErrors(account, ajax, false);
            if (!ajax.containsHTML("^\\s*1\\s*$")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final long timestampBeforeCaptchaSolving = System.currentTimeMillis();
            final String waitSecsStr = br.getRegex("<div id=\"CountDownTimer\" data-timer=\"(\\d+)\"").getMatch(0);
            // register wait i guess, it should return 1
            final int repeat = 5;
            for (int i = 1; i <= repeat; i++) {
                final UrlQuery query = new UrlQuery();
                query.add("method", "fetchDownload");
                if (br.containsHTML("plugins/cool-captcha/captcha.php")) {
                    /* Old simple picture captcha */
                    final String captchaCode = getCaptchaCode(br.getURL("/plugins/cool-captcha/captcha.php").toString(), link);
                    if (i == 1) {
                        long wait = 60;
                        if (waitSecsStr != null) {
                            // remove one second from past, to prevent returning too quickly.
                            final long passedTime = ((System.currentTimeMillis() - timestampBeforeCaptchaSolving) / 1000) - 1;
                            wait = Long.parseLong(waitSecsStr) - passedTime;
                        }
                        if (wait > 0) {
                            sleep(wait * 1000l, link);
                        }
                    }
                    query.add("captcha", Encoding.urlEncode(captchaCode));
                    postPage(ajax, "/ajax/freeDownload.php", query.toString());
                } else {
                    /* Either reCaptchaV2 or hcaptcha */
                    final int firstLoop = 1;
                    long waitMillis = 60;
                    if (waitSecsStr != null) {
                        logger.info("Found pre-download-waittime seconds: " + waitSecsStr);
                        waitMillis = Long.parseLong(waitSecsStr) * 1000l;
                    } else {
                        logger.warning("Failed to parse pre-download-waittime from html");
                    }
                    if (CaptchaHelperHostPluginHCaptcha.containsHCaptcha(br)) {
                        /* 2021-08-05: New hcaptcha handling */
                        final CaptchaHelperHostPluginHCaptcha hc = new CaptchaHelperHostPluginHCaptcha(this, br);
                        if (i == firstLoop && waitMillis > hc.getSolutionTimeout()) {
                            final int prePrePreDownloadWait = (int) (waitMillis - hc.getSolutionTimeout());
                            logger.info("Waittime is higher than hcaptcha timeout --> Waiting a part of it before solving captcha to avoid timeouts");
                            logger.info("Pre-pre download waittime seconds: " + (prePrePreDownloadWait / 1000));
                            this.sleep(prePrePreDownloadWait, link);
                        }
                        final String hcaptchaResponse = hc.getToken();
                        query.add("g-recaptcha-response", Encoding.urlEncode(hcaptchaResponse));
                        query.add("h-captcha-response", Encoding.urlEncode(hcaptchaResponse));
                    } else {
                        /* Old reCaptchaV2 handling */
                        final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
                        if (i == firstLoop && waitMillis > rc2.getSolutionTimeout()) {
                            final int prePrePreDownloadWait = (int) (waitMillis - rc2.getSolutionTimeout());
                            logger.info("Waittime is higher than reCaptchaV2 timeout --> Waiting a part of it before solving captcha to avoid timeouts");
                            logger.info("Pre-pre download waittime seconds: " + (prePrePreDownloadWait / 1000));
                            this.sleep(prePrePreDownloadWait, link);
                        }
                        final String reCaptchaV2Response = rc2.getToken();
                        if (inValidate(reCaptchaV2Response)) {
                            // fixes timeout issues or client refresh, we have no idea at this stage
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        query.add("captcha", Encoding.urlEncode(reCaptchaV2Response));
                        query.add("g-recaptcha-response", Encoding.urlEncode(reCaptchaV2Response));
                    }
                    if (i == firstLoop) {
                        /* Wait remaining pre-download-waittime if needed */
                        /* Remove some milliseconds, to prevent waiting not enough time. */
                        final long passedTime = (System.currentTimeMillis() - timestampBeforeCaptchaSolving) - 1500;
                        waitMillis -= passedTime;
                        if (waitMillis > 0) {
                            sleep(waitMillis, link);
                        } else {
                            logger.info("Congratulation: Captcha solving took so long that we do not have to wait at all");
                        }
                    }
                    postPage(ajax, "/ajax/freeDownload.php", query.toString());
                }
                if (ajax.containsHTML("(?i)The captcha wasn't entered correctly|You have to fill the captcha")) {
                    if (i + 1 == repeat) {
                        logger.info("Exhausted captcha attempts");
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    continue;
                }
                break;
            }
            dllink = ajax.getRegex("\"(https?://[a-z0-9\\-_]+\\." + buildHostsPatternPart(getSupportedNamesStatic(this.getHost())) + "/[^\"]+)\"").getMatch(0);
            if (dllink == null) {
                /* Domain independent RegEx */
                dllink = ajax.getRegex("(?i)href=\"(https?://[^\"]+)\"[^>]*>Click here to download").getMatch(0);
            }
            if (dllink == null) {
                handleErrors(account, ajax, true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        this.dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            handleDownloadErrors(account, link, true);
        } else {
            link.setProperty(getDirectlinkProperty(account), dllink);
            try {
                /* add a download slot */
                controlFree(+1);
                /* start the dl */
                this.dl.startDownload();
            } finally {
                /* remove download slot */
                controlFree(-1);
            }
        }
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
    private synchronized void controlFree(final int num) {
        final int totalMaxSimultanFreeDownload = PluginJsonConfig.get(NitroflareConfig.class).isAllowMultipleFreeDownloads() ? 20 : 1;
        logger.info("maxFree was = " + maxFree.get() + " total is = " + totalMaxSimultanFreeDownload + " change " + num);
        if (totalMaxSimultanFreeDownload == -1) {
            maxFree.set(maxFree.addAndGet(num));
        } else {
            maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload));
        }
        logger.info("maxFree now = " + maxFree.get());
    }

    /**
     * Mimics website request.
     *
     * @throws Exception
     **/
    private final Browser randomHash(final Browser br, final DownloadLink link) throws Exception {
        final String randomHash = JDHash.getMD5(link.getDownloadURL() + System.currentTimeMillis());
        // same cookie is set within as a cookie prior to registering
        final String host = getBaseDomain(this, br);
        br.setCookie(host, "randHash", randomHash);
        this.setAjaxHeaders(br);
        postPage(br, "https://" + host + "/ajax/randHash.php", "randHash=" + randomHash);
        return br;
    }

    private void handleErrors(final Account account, final Browser br, final boolean postCaptcha) throws PluginException {
        if (postCaptcha) {
            if (br.containsHTML("You don't have an entry ticket\\. Please refresh the page to get a new one")) {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (br.containsHTML("File doesn't exist")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (StringUtils.startsWithCaseInsensitive(br.toString(), "To continue this download please")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "To continue this download please purchase premium or turn off your VPN.", 60 * 60 * 1000l);
        } else if (StringUtils.startsWithCaseInsensitive(br.toString(), "You can't use free download with a VPN")) {
            /* You can't use free download with a VPN / proxy turned on. Please turn it off and try again. */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You can't use free download with a VPN / proxy turned on. Please turn it off and try again", 60 * 60 * 1000l);
        } else if (br.containsHTML("(?i)This file is available with premium key only|This file is available with Premium only")) {
            throwPremiumRequiredException(this.getDownloadLink());
        } else if (br.containsHTML("(?i)Free downloading is not possible")) {
            /* E.g. Free downloading is not possible. You have to wait 70 minutes to download your next file. */
            final String waitminutesStr = br.getRegex("You have to wait (\\d+) minutes to download").getMatch(0);
            if (waitminutesStr != null) {
                final int waitminutes = Integer.parseInt(waitminutesStr);
                /* Sometimes they got3 hour waittime but it will be over sooner --> Wait max 60 minutes. */
                if (waitminutes >= 60) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitminutes * 60 * 1001l);
                }
            } else if (PluginJsonConfig.get(NitroflareConfig.class).isAllowMultipleFreeDownloads()) {
                /* Wait shorter amount of time if user allows multiple free downloads according to plugin config. */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 20 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
        } else if (StringUtils.startsWithCaseInsensitive(br.toString(), "﻿Free download is currently unavailable due to overloading in the server")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Free download Overloaded, will try again later", 5 * 60 * 1000l);
        } else if (br.containsHTML("(?i)>\\s*Your ip is been blocked, if you think it is mistake contact us")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Your ip is blocked", 30 * 60 * 1000l);
        }
    }

    private Browser setAjaxHeaders(final Browser br) {
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        return br;
    }

    /**
     * Validates account and returns correct account info, when user has provided incorrect user pass fields to JD client. Or Throws
     * exception indicating users mistake, when it's a irreversible mistake.
     *
     * @param account
     * @return
     * @throws PluginException
     */
    @Deprecated
    private String validateAccountUserInputs(final Account account) throws PluginException {
        synchronized (account) {
            final String user = account.getUser().toLowerCase(Locale.ENGLISH);
            final String pass = account.getPass();
            if (inValidate(pass)) {
                // throw new PluginException(LinkStatus.ERROR_PREMIUM,
                // "\r\nYou haven't provided a valid password or premiumKey (this field can not be empty)!",
                // PluginException.VALUE_ID_PREMIUM_DISABLE);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou haven't provided a valid password (this field can not be empty)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (pass.matches("(?-i)NF[a-zA-Z0-9]{10}")) {
                // no need to urlencode, this is always safe.
                // return "user=&premiumKey=" + pass;
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPremiumKeys not accepted, you need to use Account (email and password).", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (inValidate(user) || !user.matches(".+@.+")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou haven't provided a valid username (must be email address)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else
                // check to see if the user added the email username with caps.. this can make login incorrect
                if (!user.equals(account.getUser())) {
                    logger.info("Corrected username: Old: " + account.getUser() + " | New: " + user);
                    account.setUser(user);
                }
            // urlencode required!
            return "user=" + Encoding.urlEncode(user) + "&premiumKey=" + Encoding.urlEncode(pass);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (useAPIAccountMode()) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWeb(account, true);
        }
    }

    /** Login and grab account information. */
    private AccountInfo fetchAccountInfoWeb(final Account account, boolean validateCookies) throws Exception {
        synchronized (account) {
            if (!account.getUser().matches(".+@.+")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou haven't provided a valid username (must be email address)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            try {
                final String host = getBaseDomain(this, br);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                boolean fullLogin;
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    br.setCookies(cookies);
                    if (!validateCookies) {
                        /* Trust cookies without check */
                        return null;
                    }
                    // lets do a test
                    final Browser br2 = br.cloneBrowser();
                    getPage(br2, "https://" + host);
                    if (br2.containsHTML(">\\s*Your password has expired") || br2.containsHTML(">\\s*Change Password\\s*<")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your password has expired. Please visit website and set new password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    final String user = br2.getCookie(host, "user", Cookies.NOTDELETEDPATTERN);
                    if (user != null) {
                        logger.info("Cookie login successful");
                        fullLogin = false;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                        fullLogin = true;
                    }
                } else {
                    fullLogin = true;
                }
                if (fullLogin) {
                    logger.info("Attempting full login");
                    getPage("https://" + host + "/login");
                    boolean captchaSolved = false;
                    for (int retry = 0; retry < 3; retry++) {
                        final Form f = getLoginFormWebsite(br);
                        if (f == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        if (requiresCaptchaWebsite(br)) {
                            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                            f.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                            captchaSolved = true;
                        }
                        f.put("email", Encoding.urlEncode(account.getUser().toLowerCase(Locale.ENGLISH)));
                        f.put("password", Encoding.urlEncode(account.getPass()));
                        f.put("login", "");
                        submitForm(f);
                        if (getLoginFormWebsite(br) == null) {
                            break;
                        } else if (!requiresCaptchaWebsite(br)) {
                            /* No captcha required for possible next run --> Looks like login failed --> Do not retry */
                            break;
                        } else if (captchaSolved) {
                            /* Do not allow multiple captcha attempts */
                            break;
                        }
                    }
                    if (getLoginFormWebsite(br) != null) {
                        throw new AccountInvalidException("Incorrect User/Password");
                    } else if (br.containsHTML("(?i)>\\s*Your password has expired") || br.containsHTML("(?i)>\\s*Change Password\\s*<")) {
                        throw new AccountInvalidException("Your password has expired. Please visit website and set new password!");
                    }
                    // final failover, we expect 'user' cookie
                    final String user = br.getCookie(host, "user", Cookies.NOTDELETEDPATTERN);
                    if (user == null) {
                        throw new AccountInvalidException("Could not find Account Cookie");
                    }
                }
                final AccountInfo ai = new AccountInfo();
                getPage("https://" + host + "/member?s=premium");
                final String status = br.getRegex("(?i)<label>Status</label><strong[^>]+>\\s*([^<]+)\\s*</strong>").getMatch(0);
                if (!inValidate(status)) {
                    if (StringUtils.equalsIgnoreCase(status, "Active")) {
                        account.setType(AccountType.PREMIUM);
                    } else {
                        account.setType(AccountType.FREE);
                    }
                } else {
                    account.setType(AccountType.FREE);
                }
                // extra traffic in webmode isn't added to daily traffic, so we need to do it manually. (api mode is has been added to
                // traffic left/max)
                final String extraTraffic = br.getRegex("(?i)<label>Your Extra Bandwidth</label><strong>(.*?)</strong>").getMatch(0);
                // do we have traffic?
                final String[] traffic = br.getRegex("(?i)<label>[^>]*Daily Limit\\s*</label><strong>(\\d+(?:\\.\\d+)?(?:\\s*[KMGT]{0,1}B)?) / (\\d+(?:\\.\\d+)?\\s*[KMGT]{0,1}B)</strong>").getRow(0);
                if (traffic != null) {
                    final long extratraffic = !inValidate(extraTraffic) ? SizeFormatter.getSize(extraTraffic) : 0;
                    final long trafficmax = SizeFormatter.getSize(traffic[1]);
                    // they show traffic used, not traffic left. we need to convert it.
                    final long trafficleft = trafficmax - SizeFormatter.getSize(traffic[0]);
                    // first value is traffic used, not remaining
                    ai.setTrafficLeft(trafficleft + extratraffic);
                    ai.setTrafficMax(trafficmax + extratraffic);
                }
                // expire time
                final String expire = br.getRegex("(?i)<label>\\s*Time Left\\s*</label><strong>(.*?)</strong>").getMatch(0);
                if (!inValidate(expire)) {
                    // <strong>11 days, 7 hours, 53 minutes.</strong>
                    final String tmpyears = new Regex(expire, "(\\d+)\\s*years?").getMatch(0);
                    final String tmpdays = new Regex(expire, "(\\d+)\\s*days?").getMatch(0);
                    final String tmphrs = new Regex(expire, "(\\d+)\\s*hours?").getMatch(0);
                    final String tmpmin = new Regex(expire, "(\\d+)\\s*minutes?").getMatch(0);
                    final String tmpsec = new Regex(expire, "(\\d+)\\s*seconds?").getMatch(0);
                    long years = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
                    if (!inValidate(tmpyears)) {
                        days = Integer.parseInt(tmpyears);
                    }
                    if (!inValidate(tmpdays)) {
                        days = Integer.parseInt(tmpdays);
                    }
                    if (!inValidate(tmphrs)) {
                        hours = Integer.parseInt(tmphrs);
                    }
                    if (!inValidate(tmpmin)) {
                        minutes = Integer.parseInt(tmpmin);
                    }
                    if (!inValidate(tmpsec)) {
                        seconds = Integer.parseInt(tmpsec);
                    }
                    long waittime = ((years * 86400000 * 365) + (days * 86400000) + (hours * 3600000) + (minutes * 60000) + (seconds * 1000));
                    ai.setValidUntil(System.currentTimeMillis() + waittime);
                }
                account.setAccountInfo(ai);
                if (!account.isValid()) {
                    throw new AccountInvalidException("Non Valid Account");
                }
                /** Save cookies */
                account.saveCookies(br.getCookies(br.getHost()), "");
                return ai;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private Form getLoginFormWebsite(final Browser br) {
        return br.getFormbyProperty("id", "login");
    }

    private boolean requiresCaptchaWebsite(final Browser br) {
        if (br.containsHTML("<div class=\"g-recaptcha\"")) {
            return true;
        } else {
            return false;
        }
    }

    private AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        synchronized (account) {
            if (!account.getUser().matches(".+@.+")) {
                throw new AccountInvalidException("Username must be email address!");
            }
            br.setCookiesExclusive(true);
            getPage(getAPIBase() + "/getKeyInfo?user=" + Encoding.urlEncode(account.getUser()) + "&premiumKey=" + Encoding.urlEncode(account.getPass()));
            checkErrorsAPI(br, null, account);
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Map<String, Object> result = (Map<String, Object>) entries.get("result");
            final AccountInfo ai = new AccountInfo();
            final String status = (String) result.get("status");
            final Number trafficLeft = (Number) result.get("trafficLeft");
            final Number trafficMax = (Number) result.get("trafficMax");
            final Object expiryDateO = result.get("expiryDate");
            if (trafficLeft != null && trafficMax != null) {
                ai.setTrafficLeft(trafficLeft.longValue());
                ai.setTrafficMax(trafficMax.longValue());
            }
            if (!"active".equalsIgnoreCase(status) || expiryDateO == null || expiryDateO.toString().equals("0")) {
                account.setType(AccountType.FREE);
            } else {
                account.setType(AccountType.PREMIUM);
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expiryDateO.toString(), "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), br);
            }
            return ai;
        }
    }

    /** Wrapper */
    private void checkErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws Exception {
        checkErrorsAPI(br, link, account, true);
    }

    private void checkErrorsAPI(final Browser br, final DownloadLink link, final Account account, final boolean solveCaptcha) throws Exception {
        int errorcode = -1;
        String msg = null;
        try {
            /* E.g. {"type":"error","message":"Wrong login","code":8} */
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            msg = (String) entries.get("message");
            errorcode = ((Number) entries.get("code")).intValue();
        } catch (final Throwable e) {
            logger.warning("Bad API response");
            // logger.log(e);
        }
        switch (errorcode) {
        case -1:
            /* No error */
            break;
        case 1:
            /* {"type":"error","message":"Access denied","code":1} */
            /* File is premiumonly */
            throw new AccountRequiredException();
        case 4:
            /* {"type":"error","message":"File doesn't exist","code":4} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 6:
            /* {"type":"error","message":"Invalid captcha","code":6} */
            /* This should rarely/never happen!! */
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        case 8:
            /* Invalid logindata */
            throw new AccountInvalidException(msg);
        case 12:
            /* API captcha required to continue/start using their API! */
            if (!solveCaptcha) {
                /* Captcha has been tried before and something went wrong... */
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            final Request previousRequest = br.getRequest().cloneRequest();
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Lenx_USAAAAAF5L1pmTWvWcH73dipAEzNnmNLgy").getToken();
            final UrlQuery query = new UrlQuery();
            query.add("response", Encoding.urlEncode(recaptchaV2Response));
            final String solveCaptchaURL = getAPIBase() + "/solveCaptcha?user=" + Encoding.urlEncode(account.getUser());
            this.postPage(solveCaptchaURL, query.toMap());
            if (!br.toString().equalsIgnoreCase("passed")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                br.getPage(solveCaptchaURL + "&solved=1");
                /* Looks like captcha was solved successfully --> Re-do previous request and re-check for errors. */
                br.getPage(previousRequest);
                this.checkErrorsAPI(br, link, account, false);
                break;
            }
        default:
            /* Handle unknown errors */
            if (link == null) {
                throw new AccountUnavailableException("API error: " + msg, 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "API error: " + msg, 5 * 60 * 1000l);
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* is free user? */
        if (account.getType() == AccountType.FREE) {
            /* Login */
            fetchAccountInfoWeb(account, false);
            handleFreeDownload(account, link);
        } else {
            /* Premium download */
            /* check cached download */
            String dllink = link.getStringProperty(getDirectlinkProperty(account));
            if (!inValidate(dllink)) {
                logger.info("Trying to re-use stored generated directurl");
                this.dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
                if (looksLikeDownloadableContent(dl.getConnection())) {
                    logger.info("Successfully re-used stored generated directurl");
                    link.setProperty(getDirectlinkProperty(account), dllink);
                    this.dl.startDownload();
                    return;
                } else {
                    logger.info("Failed to re-use stored generated directurl");
                    this.dl = null;
                    link.removeProperty(getDirectlinkProperty(account));
                    handleDownloadErrors(account, link, false);
                }
            }
            logger.info("Generating new directurl");
            if (this.useAPIAccountMode()) {
                /* 2020-06-24: According to API docs, we should check the file before performing premium-download-API-call! */
                requestFileInformationAPI(link);
                /* Additional login is not required. */
                handlePremiumDownloadAPI(link, account);
            } else {
                requestFileInformationAPI(link);
                /* Login */
                fetchAccountInfoWeb(account, false);
                /* Check if we can download right away */
                int counter = 0;
                final int maxtries = 2;
                boolean askedForCaptcha = false;
                do {
                    logger.info(String.format("Premium downloadloop %d / %d", counter + 1, maxtries));
                    this.dl = new jd.plugins.BrowserAdapter().openDownload(br, link, getCorrectedDownloadURL(link), this.isResumeable(link, account), this.getMaxChunks(account));
                    if (looksLikeDownloadableContent(dl.getConnection())) {
                        /* Directurl */
                        logger.info("Seems like user has direct downloads enabled");
                        link.setProperty(getDirectlinkProperty(account), dl.getConnection().getURL().toString());
                        this.dl.startDownload();
                        return;
                    }
                    logger.info("Seems like user has direct downloads disabled or VPN captcha required");
                    br.followConnection();
                    if (!requiresCaptchaVPNWarning(br)) {
                        /* Something else might have gone wrong. */
                        break;
                    }
                    /*
                     * Handle rare case: User uses VPN, nitroflare recognizes that and lets user solve an extra captcha to proceed via VPN.
                     */
                    logger.info("Premium captcha/VPN-captcha required");
                    if (askedForCaptcha) {
                        /* This should never happen */
                        logger.info("Premium captcha/VPN-captcha failure: Captcha required although user has already solved one");
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    final Browser ajax = this.setAjaxHeaders(br.cloneBrowser());
                    this.postPage(ajax, "/ajax/validate-dl-recaptcha", "response=" + Encoding.urlEncode(recaptchaV2Response));
                    if (!ajax.getRequest().getHtmlCode().equalsIgnoreCase("passed")) {
                        /* This should never happen */
                        logger.info("Premium captcha/VPN-captcha failure");
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    logger.info("Premium captcha/VPN-captcha success");
                    askedForCaptcha = true;
                    counter++;
                } while (counter < maxtries);
                handleDownloadErrors(account, link, false);
                /* Needed if user has disabled direct downloads in his nitroflare account. */
                dllink = br.getRegex("<a[^>]*id=\"download\"[^>]*href\\s*=\\s*\"([^\"]+)\"").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Can't find dllink!");
                }
                this.dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
                if (!looksLikeDownloadableContent(dl.getConnection())) {
                    handleDownloadErrors(account, link, true);
                }
                link.setProperty(getDirectlinkProperty(account), dllink);
                this.dl.startDownload();
            }
        }
    }

    private void handlePremiumDownloadAPI(final DownloadLink link, final Account account) throws Exception {
        this.getPage(getAPIBase() + "/getDownloadLink?user=" + Encoding.urlEncode(account.getUser()) + "&premiumKey=" + Encoding.urlEncode(account.getPass()) + "&file=" + Encoding.urlEncode(this.getFID(link)));
        this.checkErrorsAPI(br, link, account);
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final Map<String, Object> result = (Map<String, Object>) entries.get("result");
        final String dllink = result.get("url").toString();
        if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API download failure");
        }
        this.dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            handleDownloadErrors(account, link, true);
        }
        link.setProperty(getDirectlinkProperty(account), dllink);
        this.dl.startDownload();
    }

    private boolean requiresCaptchaVPNWarning(final Browser br) {
        if (br.containsHTML("(?i)To get rid of the captcha, please avoid using a dedicated server|/ajax/validate-dl-recaptcha")) {
            return true;
        } else {
            return false;
        }
    }

    private final void handleDownloadErrors(final Account account, final DownloadLink downloadLink, final boolean lastChance) throws PluginException, IOException {
        br.followConnection(true);
        final String err1 = "ERROR: Wrong IP. If you are using proxy, please turn it off / Or buy premium key to remove the limitation";
        if (br.containsHTML(err1)) {
            // I don't see why this would happening logs contain no proxy!
            throw new PluginException(LinkStatus.ERROR_FATAL, err1);
        } else if (account != null && br.getHttpConnection() != null && (br.toString().equals("Your premium has reached the maximum volume for today") || br.containsHTML("(?i)<p id=\"error\"[^>]+>\\s*Your premium has reached the maximum volume for today|>\\s*This download exceeds the daily download limit"))) {
            throw new AccountUnavailableException("Daily downloadlimit reached", 5 * 60 * 1000l);
        } else if (br.containsHTML("(?i)>\\s*This download exceeds the daily download limit\\. You can purchase")) {
            // not enough traffic to download THIS file, doesn't mean zero traffic left.
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "You do not have enough traffic left to start this download.");
        }
        if (lastChance) {
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("ERROR: link expired. Please unlock the file again")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'link expired'", 2 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private void throwPremiumRequiredException(final DownloadLink link) throws PluginException {
        if (link != null) {
            link.setProperty(PROPERTY_PREMIUM_REQUIRED, Boolean.TRUE);
        }
        throw new AccountRequiredException();
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link) throws IOException, PluginException {
        final boolean checked = checkLinks(new DownloadLink[] { link });
        // we can't throw exception in checklinks! This is needed to prevent multiple captcha events!
        if (!checked && hasAntiddosCaptchaRequirement()) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (!checked || !link.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKABLE;
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            return link.getAvailableStatus();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        /* 2020-06-24: Do NOT reset the properties that hold direct-URLs anymore! */
        if (link != null) {
            // link.setProperty("freelink2", Property.NULL);
            // link.setProperty("freelink", Property.NULL);
            // link.setProperty("premlink", Property.NULL);
            link.removeProperty(PROPERTY_PREMIUM_REQUIRED);
        }
    }

    public boolean hasCaptcha(final DownloadLink downloadLink, final jd.plugins.Account acc) {
        if (acc == null || acc.getType() == AccountType.FREE) {
            /* no account and free account, yes we can expect captcha */
            return true;
        } else {
            return false;
        }
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    public Class<NitroflareConfig> getConfigInterface() {
        return NitroflareConfig.class;
    }
}