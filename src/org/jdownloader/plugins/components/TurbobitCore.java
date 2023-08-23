package org.jdownloader.plugins.components;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.components.UserAgents;
import jd.plugins.download.HashInfo;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class TurbobitCore extends antiDDoSForHost {
    /**
     * TODO: Check if we already got errorhandling for this kind of error http://turbobit.net/error/download/dcount/xxxtesst --> "
     *
     * An amount of maximum downloads for this link has been exceeded "
     *
     * When adding new domains here also add them to the turbobit.net decrypter (TurboBitNetFolder)
     *
     */
    /* Settings */
    // private static final String SETTING_JAC = "SETTING_JAC";
    public static final String       SETTING_FREE_PARALLEL_DOWNLOADSTARTS          = "SETTING_FREE_PARALLEL_DOWNLOADSTARTS";
    public static final String       SETTING_PREFERRED_DOMAIN                      = "SETTING_PREFERRED_DOMAIN";
    private static String            PROPERTY_DOWNLOADLINK_checked_atleast_onetime = "checked_atleast_onetime";
    private static final int         FREE_MAXDOWNLOADS_PLUGINSETTING               = 20;
    private static final boolean     prefer_single_linkcheck_via_mass_linkchecker  = true;
    private static final String      TYPE_premiumRedirectLinks                     = "(?i)(?:https?://[^/]+/)?/?download/redirect/[A-Za-z0-9]+/([a-z0-9]+)";
    private static Map<String, Long> hostLastPremiumCaptchaProcessedTimestampMap   = new HashMap<String, Long>();

    public TurbobitCore(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        enablePremium("https://" + this.getHost() + "/turbo/v2/");
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        try {
            final String fuid = getFUID(link);
            return this.getHost() + "://" + fuid;
        } catch (PluginException e) {
            e.printStackTrace();
            return super.getLinkID(link);
        }
    }

    protected boolean isFastLinkcheckEnabled() {
        return true;
    }

    /**
     * 2019-05-11: There is also an API-version of this but it seems like it only returns online/offline - no filename/filesize:
     * https://hitfile.net/linkchecker/api
     */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        /**
         * Enabled = Do not check for filesize via single-linkcheck on first time linkcheck - only on the 2nd linkcheck and when the
         * filesize is not known already. This will speedup the linkcheck! </br>
         * Disabled = Check for filesize via single-linkcheck even first time links get added as long as no filesize is given. This will
         * slow down the linkcheck and cause more http requests in a short amount of time!
         */
        final boolean fastLinkcheck = isFastLinkcheckEnabled();
        final ArrayList<DownloadLink> deepChecks = new ArrayList<DownloadLink>();
        try {
            final Browser br_linkcheck = new Browser();
            prepBrowserWebsite(br_linkcheck, null);
            br_linkcheck.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br_linkcheck.setCookiesExclusive(true);
            br_linkcheck.setFollowRedirects(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            getPage(br_linkcheck, "https://" + getConfiguredDomain() + "/linkchecker");
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 49) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("links_to_check=");
                for (final DownloadLink dl : links) {
                    sb.append(Encoding.urlEncode(this.getContentURL(dl)));
                    sb.append("%0A");
                }
                /* remove last */
                sb.delete(sb.length() - 3, sb.length());
                /*
                 * '/linkchecker/csv' is the official "API" method but this will only return fileID and online/offline - not even the
                 * filename
                 */
                postPage(br_linkcheck, "https://" + br_linkcheck.getHost() + "/linkchecker/check", sb.toString());
                for (final DownloadLink link : links) {
                    final Regex fileInfo = br_linkcheck.getRegex("<td>" + getFUID(link) + "</td>\\s*<td>([^<]*)</td>\\s*<td style=\"text-align:center;\">(?:[\t\n\r ]*)?<img src=\"(?:[^\"]+)?/(done|error)\\.png\"");
                    if (fileInfo.getMatches() == null || fileInfo.getMatches().length == 0) {
                        /*
                         * 2020-01-27: E.g. "<p>Number of requests exceeded the limit. Please wait 5 minutes to check links again</p></div>"
                         */
                        link.setAvailableStatus(AvailableStatus.UNCHECKED);
                        logger.warning("Unable to check link: " + link.getPluginPatternMatcher());
                    } else {
                        if (fileInfo.getMatch(1).equals("error")) {
                            link.setAvailable(false);
                        } else {
                            final String name = fileInfo.getMatch(0);
                            link.setAvailable(true);
                            link.setFinalFileName(Encoding.htmlDecode(name).trim());
                            final boolean checkedBeforeAlready = link.getBooleanProperty(PROPERTY_DOWNLOADLINK_checked_atleast_onetime, false);
                            if (link.getKnownDownloadSize() < 0 && (checkedBeforeAlready || !fastLinkcheck)) {
                                deepChecks.add(link);
                            }
                            /* Allows it to look for the filesize on 2nd linkcheck. */
                            link.setProperty(PROPERTY_DOWNLOADLINK_checked_atleast_onetime, true);
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            logger.log(e);
            return false;
        } finally {
            for (final DownloadLink deepCheck : deepChecks) {
                try {
                    final AvailableStatus availableStatus = requestFileInformation_Web(deepCheck);
                    deepCheck.setAvailableStatus(availableStatus);
                } catch (PluginException e) {
                    logger.log(e);
                    final AvailableStatus availableStatus;
                    switch (e.getLinkStatus()) {
                    case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
                    case LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE:
                        availableStatus = AvailableStatus.UNCHECKABLE;
                        break;
                    case LinkStatus.ERROR_FILE_NOT_FOUND:
                        availableStatus = AvailableStatus.FALSE;
                        break;
                    case LinkStatus.ERROR_PREMIUM:
                        if (e.getValue() == PluginException.VALUE_ID_PREMIUM_ONLY) {
                            availableStatus = AvailableStatus.UNCHECKABLE;
                            break;
                        }
                    default:
                        availableStatus = AvailableStatus.UNCHECKABLE;
                        break;
                    }
                    deepCheck.setAvailableStatus(availableStatus);
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        }
        return true;
    }

    // Also check HitFileNet plugin if this one is broken
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (prefer_single_linkcheck_via_mass_linkchecker && supports_mass_linkcheck()) {
            return requestFileInformation_Mass_Linkchecker(link);
        } else {
            return requestFileInformation_Web(link);
        }
    }

    public AvailableStatus requestFileInformation_Mass_Linkchecker(final DownloadLink link) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        } else {
            if (link.isAvailabilityStatusChecked() && !link.isAvailable()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                return AvailableStatus.TRUE;
            }
        }
    }

    public AvailableStatus requestFileInformation_Web(final DownloadLink link) throws Exception {
        /* premium links should not be accessed here, we will just return true */
        setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowserWebsite(br, userAgent.get());
        accessContentURL(br, link);
        if (isFileOfflineWebsite(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String titlePattern = "(?i)<title>\\s*(?:Download\\s+file|Datei\\s+downloaden|Descargar\\s+el\\s+archivo|Télécharger\\s+un\\s+fichier|Scarica\\s+il\\s+file|Pobierz\\s+plik|Baixar\\s+arquivo|İndirilecek\\s+dosya|ファイルのダウンロード)\\s*(.*?)\\s*\\(([\\d\\.,]+\\s*[BMKGTP]{1,2})\\)\\s*\\|\\s*(?:TurboBit|Hitfile)\\.net";
        String fileName = br.getRegex(titlePattern).getMatch(0);
        String fileSize = br.getRegex(titlePattern).getMatch(1);
        if (fileName == null) {
            fileName = br.getRegex("<span class\\s*=\\s*(\"|')file\\-title\\1[^>]*>\\s*(.*?)\\s*</span>").getMatch(1);
            if (StringUtils.contains(fileName, "...")) {
                final String[] split = fileName.split("\\.\\.\\.");
                if (split.length == 2) {
                    final String customTitlePattern = "<title>\\s*[^<]*\\s*(" + Pattern.quote(split[0]) + ".*?" + Pattern.quote(split[1]) + ")\\s*\\(([\\d\\., ]+\\s*[BMKGTP]{1,2}\\s*)\\)";
                    final String fromTitle = br.getRegex(customTitlePattern).getMatch(0);
                    if (fromTitle != null && fromTitle.length() > fileName.length()) {
                        fileName = fromTitle;
                    }
                    if (fileSize == null) {
                        fileSize = br.getRegex(customTitlePattern).getMatch(1);
                    }
                }
            }
        }
        if (fileSize == null) {
            /* E.g. for hitfile.net, filesize is in brakets '(")(")' */
            fileSize = br.getRegex("class\\s*=\\s*\"file-size\"\\s*>\\s*\\(?\\s*([^<>\"]*?)\\s*\\)?\\s*<").getMatch(0);
        }
        if (fileName != null) {
            link.setName(fileName);
        }
        if (fileSize != null) {
            fileSize = fileSize.replace("М", "M");
            fileSize = fileSize.replace("к", "k");
            fileSize = fileSize.replace("Г", "g");
            fileSize = fileSize.replace("б", "");
            if (!fileSize.endsWith("b")) {
                fileSize = fileSize + "b";
            }
            link.setDownloadSize(SizeFormatter.getSize(fileSize.trim().replace(",", ".").replace(" ", "")));
        }
        return AvailableStatus.TRUE;
    }

    public static boolean isFileOfflineWebsite(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(<div class=\"code-404\">404</div>|Файл не найден\\. Возможно он был удален\\.<br|(?:Document|File|Page)\\s*(was)?\\s*not found|It could possibly be deleted\\.)");
    }

    /** 2019-05-09: Seems like API can only be used to check self uploaded content - <b>it is useless for us</b>! */
    public AvailableStatus requestFileInformation_API(final DownloadLink link) throws Exception {
        if (true) {
            return AvailableStatus.UNCHECKABLE;
        }
        br.setFollowRedirects(true);
        prepBrowserAPI(br, userAgent.get());
        getPage("https://turbobit.net/v001/files/" + this.getLinkID(link));
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail address in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        try {
            login(account, true);
        } catch (final PluginException e) {
            if (br.containsHTML("(?i)Our service is currently unavailable in your country\\.")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nWebsite is currently unavailable in your country!\r\nDiese webseite ist in deinem Land momentan nicht verfügbar!", PluginException.VALUE_ID_PREMIUM_DISABLE, e);
            } else {
                throw e;
            }
        }
        ai.setUnlimitedTraffic();
        // >Turbo access till 27.09.2015</span>
        String expire = br.getRegex("(?i)>\\s*Turbo access till\\s*(.*?)\\s*</span>").getMatch(0);
        if (expire == null) {
            /* 2019-05-22: hitfile.net */
            expire = br.getRegex("'/premium(?:/info)?'\\s*>\\s*(\\d+\\.\\d+\\.\\d+)\\s*<").getMatch(0);
        }
        if (expire != null) {
            long endBlockingTime = -1;
            if (br.containsHTML("<span class='glyphicon glyphicon-ok banturbo'>") || (endBlockingTime = getBlockingEndTime(br, account)) > 0) {
                if (endBlockingTime > 0) {
                    final String readableTime = new SimpleDateFormat("yyyy-MM-dd' 'HH':'mm':'ss", Locale.ENGLISH).format(new Date(endBlockingTime));
                    final long wait = Math.max(5 * 60 * 1000l, Math.min(endBlockingTime - System.currentTimeMillis(), 30 * 60 * 1000l));
                    throw new AccountUnavailableException("You have reached limit of premium downloads:" + readableTime, wait);
                } else {
                    throw new AccountUnavailableException("You have reached limit of premium downloads", 30 * 60 * 1000l);
                }
            }
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "dd.MM.yyyy", Locale.ENGLISH));
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        return ai;
    }

    protected static long getBlockingEndTime(final Browser br, final Account account) {
        final String[] endTimes = br.getRegex("Blocking end time\\s*:\\s*(\\d{4}-\\d{2}-\\d{2}\\s*\\d{2}:\\d{2}:\\d{2})\\s*<").getColumn(0);
        if (endTimes != null && endTimes.length > 0) {
            final long now = System.currentTimeMillis();
            for (final String endTime : endTimes) {
                final long timeStamp = TimeFormatter.getMilliSeconds(endTime, "yyyy-MM-dd' 'HH':'mm':'ss", Locale.ENGLISH);
                if (timeStamp > 0 && timeStamp > now) {
                    return timeStamp;
                }
            }
        }
        return -1;
    }

    @Override
    public String getAGBLink() {
        return getMainpage() + "/rules";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        if (this.getPluginConfig().getBooleanProperty(SETTING_FREE_PARALLEL_DOWNLOADSTARTS, false)) {
            return FREE_MAXDOWNLOADS_PLUGINSETTING;
        } else {
            return 1;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    protected String getAPIKey() {
        final String userDefinedAPIKey = this.getPluginConfig().getStringProperty("APIKEY");
        if (StringUtils.isEmpty(userDefinedAPIKey) || StringUtils.equalsIgnoreCase(userDefinedAPIKey, "DEFAULT")) {
            /* No APIKey set or default? Return default key. */
            return getAPIKeyDefault();
        } else {
            return userDefinedAPIKey;
        }
    }

    public String getAPIKeyDefault() {
        return null;
    }

    /**
     * @return true: Website supports https and plugin will prefer https. <br />
     *         false: Website does not support https - plugin will avoid https. <br />
     *         default: true
     */
    public boolean supports_https() {
        return true;
    }

    public boolean supports_mass_linkcheck() {
        return true;
    }

    /** E.g. '.html' needed at the end of downloadurls: turbobit.net - e.g. NOT needed: hitfile.net */
    public boolean downloadurls_need_html_ending() {
        return true;
    }

    /** If no waittime is found or it is less than this, a fallback waittime will get used. */
    public int minimum_pre_download_waittime_seconds() {
        return 60;
    }

    /** Waittime which is used if no waittime was found or the found waittime is less than minimum_pre_download_waittime_seconds */
    protected int get_fallback_waittime() {
        return 600;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleFree(link, null);
    }

    protected void accessContentURL(final Browser thisbr, final DownloadLink link) throws Exception {
        getPage(thisbr, getContentURL(link));
    }

    protected String getContentURL(final DownloadLink link) throws PluginException {
        return getContentURL(getConfiguredDomain(), this.getFUID(link));
    }

    /** Returns base content URL for given DownloadLink. Most times this will be: https://<domain>/<fuid>.html */
    protected String getContentURL(final String domain, final String fuid) throws PluginException {
        String contentURL = "https://" + domain + "/" + fuid;
        if (downloadurls_need_html_ending()) {
            contentURL += ".html";
        }
        return contentURL;
    }

    /** Handles free- and free account downloads */
    protected void handleFree(final DownloadLink link, Account account) throws Exception {
        /* support for public premium links */
        if (link.getPluginPatternMatcher().matches(TYPE_premiumRedirectLinks)) {
            if (handlePremiumLink(link, account)) {
                return;
            } else {
                logger.info("Download of pre given directurl failed --> Attempting normal free download");
            }
        }
        requestFileInformation_Web(link);
        if (checkShowFreeDialog(getHost())) {
            super.showFreeDialog(getHost());
        }
        sleep(2000, link);
        getPage(br, "/download/free/" + this.getFUID(link));
        if (isFileOfflineWebsite(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isPremiumOnly(link, account, br, false)) {
            throw new AccountRequiredException();
        }
        partTwo(link, account, true);
    }

    protected boolean isPremiumOnly(final DownloadLink link, final Account account, final Browser br, final boolean throwException) throws PluginException {
        if (br.containsHTML("(?i)<div class=\"free-limit-note\">\\s*Limit reached for free download of this file\\.")) {
            if (throwException) {
                throw new AccountRequiredException();
            } else {
                return true;
            }
        } else if (br.containsHTML("<a\\s*data-premium-only-download") || br.containsHTML("href\\s*=\\s*\"/download/free/[^>]*?data-premium-only-download")) {
            if (throwException) {
                throw new AccountRequiredException();
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Fills in captchaForm. </br>
     * DOES NOT SEND CAPTCHA-FORM!!
     */
    protected boolean processCaptchaForm(final DownloadLink link, final Account account, final Form captchaform, final Browser br, final boolean optionalCaptcha) throws PluginException, InterruptedException {
        if (containsHCaptcha(br)) {
            final String response = new CaptchaHelperHostPluginHCaptcha(this, br).getToken();
            captchaform.put("g-recaptcha-response", Encoding.urlEncode(response));
            captchaform.put("h-captcha-response", Encoding.urlEncode(response));
            return true;
        } else if (containsRecaptchaV2Class(br)) {
            /* ReCaptchaV2 */
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            captchaform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            return true;
        } else if (!optionalCaptcha) {
            /* This should not happen - see old captcha handling in TurboBitNet class revision 40594 */
            logger.warning("Captcha-handling failed: Captcha handling was executed and captcha is expected to be there but is not there");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return false;
        }
    }

    protected void handleWaitingTime(final Browser br, final DownloadLink downloadLink, final Account account) throws Exception {
        final String waittime = br.getRegex("limit\\s?:\\s?(\\d+)\\s*,").getMatch(0);
        if (waittime != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 1001l);
        } else if (br.containsHTML("Timeout\\.limit\\s*>\\s*0")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1001l);
        }
    }

    private final void partTwo(final DownloadLink link, final Account account, final boolean allowRetry) throws Exception {
        Form captchaform = null;
        final Form[] allForms = br.getForms();
        if (allForms != null && allForms.length != 0) {
            for (final Form aForm : allForms) {
                if (aForm.containsHTML("captcha")) {
                    captchaform = aForm;
                    break;
                }
            }
        }
        if (captchaform == null) {
            handleGeneralErrors(br, account);
            if (!br.getURL().contains("/download/free/")) {
                if (allowRetry && br.containsHTML("/download/free/" + Pattern.quote(this.getFUID(link)))) {
                    // from a log where the first call to this, just redirected to main page and set some cookies
                    getPage("/download/free/" + this.getFUID(link));
                    partTwo(link, account, false);
                    return;
                }
                /* 2019-04-24: This should not happen anymore but still we should retry if it happens. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Captcha form fail", 1 * 60 * 1000l);
            }
            handleWaitingTime(br, link, account);
            /* Give up */
            logger.warning("captchaform equals null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Fix Form */
        if (StringUtils.equalsIgnoreCase(captchaform.getAction(), "#")) {
            captchaform.setAction(br.getURL());
        }
        if (!captchaform.hasInputFieldByName("captcha_type") && captchaform.containsHTML("recaptcha2")) {
            /* E.g. hitfile.net */
            captchaform.put("captcha_type", "recaptcha2");
        }
        if (!captchaform.hasInputFieldByName("captcha_subtype") && captchaform.containsHTML("captcha_subtype")) {
            /* E.g. hitfile.net */
            captchaform.put("captcha_subtype", "");
        }
        processCaptchaForm(link, account, captchaform, br, false);
        submitForm(captchaform);
        if (br.getHttpConnection().getResponseCode() == 302 || br.containsHTML("<div\\s*class\\s*=\\s*\"captcha-error\"\\s*>\\s*Incorrect")) {
            /* This should never happen - solving took too long? */
            invalidateLastChallengeResponse();
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        final String continueLink = br.getRegex("\\$\\('#timeoutBox'\\)\\.load\\(\"(/[^\"]+)\"\\);").getMatch(0);
        if (continueLink == null) {
            handleWaitingTime(br, link, account);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Pre download wait */
        final int waitSeconds = getPreDownloadWaittime(br);
        if (waitSeconds > 250) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", waitSeconds * 1001l);
        }
        this.sleep(waitSeconds * 1001l, link);
        final Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        getPage(br2, continueLink);
        {
            /* 2019-07-11: New for turbobit.net */
            final String continueLink2 = br2.getRegex("(\"|')(/?/download/started/[^\"\\']+)\\1").getMatch(1);
            if (!StringUtils.isEmpty(continueLink2)) {
                br2.getPage(continueLink2);
            }
        }
        final String downloadUrl = br2.getRegex("(\"|')(/?/download/redirect/[^\"\\']+)\\1").getMatch(1);
        handleDownloadRedirectErrors(downloadUrl, link);
        /** 2019-05-11: Not required for e.g. hitfile.net but it does not destroy anything either so let's set it anyways. */
        br.setCookie(br.getHost(), "turbobit2", getCurrentTimeCookie(br2));
        initDownload(DownloadType.GUEST_FREE, link, account, downloadUrl);
        handleErrorsPreDownloadstart(dl.getConnection());
        dl.startDownload();
    }

    @Override
    protected void runPostRequestTask(Browser ibr) throws Exception {
        final Request request = ibr != null ? ibr.getRequest() : null;
        if (request.isRequested()) {
            // remove (on purpose?) invalid html comment like <!--empty--!>
            String html = request.getHtmlCode();
            html = html.replaceAll("<!--[\\w\\-]*--!>", "");
            if (request.getHtmlCode() != html) {
                request.setHtmlCode(html);
            }
        }
    }

    public int getPreDownloadWaittime(final Browser br) {
        int wait = 0;
        final String wait_str = br.getRegex("minLimit\\s*?:\\s*?(\\d+)").getMatch(0);
        if (wait_str == null) {
            logger.warning("Using fallback pre-download-wait");
            wait = get_fallback_waittime();
        } else {
            wait = Integer.parseInt(wait_str);
            /* Check for too short/too long waittime. */
            if (wait > 800 || wait < minimum_pre_download_waittime_seconds()) {
                /* We do not want to wait too long! */
                wait = get_fallback_waittime();
            }
        }
        return wait;
    }

    /** Handles errors */
    private void handleDownloadRedirectErrors(final String redirect, final DownloadLink link) throws PluginException {
        if (StringUtils.isEmpty(redirect)) {
            logger.info("'redirect' downloadurl is null");
            if (br.toString().matches("Error: \\d+")) {
                // unknown error...
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (br.toString().matches("^The file is not avaliable now because of technical problems\\. <br> Try to download it once again after 10-15 minutes\\..*?")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File not avaiable due to technical problems.", 15 * 60 * 1001l);
            } else if (br.containsHTML("<a href=\\'/" + this.getLinkID(link) + "(\\.html)?\\'>new</a>")) {
                /* Expired downloadlink - rare issue. If user has added such a direct-URL, we're not able to retry. */
                /**
                 * 2019-05-14: TODO: Even premium-directurls should contain the linkid so we should be able to use that to 'convert' such
                 * problematic URLs to 'normal' URLs. Keep in mind that this is a VERY VERY rare case!
                 */
                /*
                 * <div class="action-block"><p>Der Link ist abgelaufen. Fordern Sie bitte <a href='/FUID.html'>new</a> download
                 * link.</p></div></div> </div> Example: http://turbobit.net/download/redirect/TEST/TEST
                 */
                if (link.getPluginPatternMatcher().matches(TYPE_premiumRedirectLinks)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Generated Premium link has expired");
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to generate final downloadlink");
                }
            }
            final String linkerror = br.getRegex("<div\\s*id\\s*=\\s*\"brin-link-error\"\\s*>\\s*([^>]+)\\s*</div>").getMatch(0);
            if (linkerror != null) {
                /* 2019-07-10: E.g. <div id="brin-link-error">Failed to generate link. Internal server error. Please try again.</div> */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, linkerror);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown Error - failed to find redirect-url to final downloadurl");
        } else if (redirect.matches("^https?://[^/]+/?$")) {
            /* Redirect to mainpage --> expired/invalid? */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Premium link no longer valid");
        }
    }

    private String getCurrentTimeCookie(Browser ibr) throws PluginException {
        if (ibr == null) {
            ibr = br;
        }
        String output = ibr.getRequest().getResponseHeader("Date");
        if (output == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        output = Encoding.urlEncode_light(output);
        output = output.replace(":", "%3A");
        return output;
    }

    /**
     * fuid = case sensitive.
     *
     * @param link
     * @return
     * @throws PluginException
     */
    private String getFUID(final DownloadLink link) throws PluginException {
        /* standard links turbobit.net/uid.html && turbobit.net/uid/filename.html */
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else {
            if (link.getPluginPatternMatcher().matches(TYPE_premiumRedirectLinks)) {
                final String fuid = new Regex(link.getPluginPatternMatcher(), TYPE_premiumRedirectLinks).getMatch(0);
                if (fuid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    return fuid;
                }
            } else {
                String fuid = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/([A-Za-z0-9]+)(?:/[^/]+)?(?:\\.html)?$").getMatch(0);
                if (fuid == null) {
                    /* download/free/ */
                    fuid = new Regex(link.getPluginPatternMatcher(), "download/free/([A-Za-z0-9]+)").getMatch(0);
                }
                if (fuid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    return fuid;
                }
            }
        }
    }

    protected void handlePremiumCaptcha(final Browser br, final DownloadLink link, final Account account) throws Exception {
        Form premiumCaptchaForm = null;
        for (final Form form : br.getForms()) {
            if ((form.containsHTML("(?i)>\\s*Please enter captcha to continue\\s*<") || form.hasInputFieldByName("captcha_type") || form.hasInputFieldByName("g-captcha-index")) && form.hasInputFieldByName("check")) {
                premiumCaptchaForm = form;
                break;
            }
        }
        if (premiumCaptchaForm != null) {
            /* 2021-03-30: Captchas can sometimes happen in premium mode (wtf but confirmed!) */
            final Long lastPremiumCaptchaRequestedTimestampOld = hostLastPremiumCaptchaProcessedTimestampMap.get(this.getHost());
            synchronized (hostLastPremiumCaptchaProcessedTimestampMap) {
                logger.info("Detected premium download-captcha");
                if (lastPremiumCaptchaRequestedTimestampOld != null && !lastPremiumCaptchaRequestedTimestampOld.equals(hostLastPremiumCaptchaProcessedTimestampMap.get(this.getHost()))) {
                    // TODO: Check if a retry makes sense here when one captcha was solved by the user
                    logger.info("Captcha has just been solved -> We might be able to skip this and all other subsequent premium captchas by just retrying");
                }
                processCaptchaForm(link, account, premiumCaptchaForm, br, false);
                this.submitForm(premiumCaptchaForm);
                hostLastPremiumCaptchaProcessedTimestampMap.put(this.getHost(), System.currentTimeMillis());
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            this.handleFree(link, account);
        } else {
            if (link.getPluginPatternMatcher().matches(TYPE_premiumRedirectLinks)) {
                /* Direct-downloadable public premium link. */
                if (handlePremiumLink(link, account)) {
                    return;
                } else {
                    logger.info("Download of pre given directurl failed --> Attempting normal premium download");
                }
            }
            requestFileInformation(link);
            login(account, false);
            sleep(2000, link);
            accessContentURL(br, link);
            handlePremiumCaptcha(br, link, account);
            String dllink = null;
            final String[] mirrors = br.getRegex("(?i)('|\")(https?://([a-z0-9\\.]+)?[^/\\'\"]+//?download/redirect/.*?)\\1").getColumn(1);
            if (mirrors == null || mirrors.length == 0) {
                if (br.containsHTML("(?i)You have reached the.*? limit of premium downloads")) {
                    throw new AccountUnavailableException("Downloadlimit reached", 30 * 60 * 1000l);
                } else if (br.containsHTML("(?i)'>\\s*Premium access is blocked\\s*<")) {
                    logger.info("Premium access is blocked --> No traffic available?");
                    throw new AccountUnavailableException("Error 'Premium access is blocked' --> No traffic available?", 30 * 60 * 1000l);
                }
                this.handleGeneralErrors(br, account);
                logger.warning("dllink equals null, plugin seems to be broken!");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadlink");
            }
            br.setFollowRedirects(false);
            final Browser br2 = br.cloneBrowser();
            for (int i = 0; i < mirrors.length; i++) {
                final String currentlink = mirrors[i];
                logger.info("Checking mirror: " + i + "/" + mirrors.length + ": " + currentlink);
                getPage(currentlink);
                if (br.getHttpConnection().getResponseCode() == 503) {
                    logger.info("Too many connections on current account via current IP");
                    throw new AccountUnavailableException("Too many connections on current account via current IP", 30 * 1000l);
                }
                if (br.getRedirectLocation() == null) {
                    logger.info("Skipping broken mirror reason#1: " + currentlink);
                    br = br2.cloneBrowser();
                    continue;
                }
                dllink = br.getRedirectLocation();
                try {
                    if (initDownload(DownloadType.ACCOUNT_PREMIUM, link, account, dllink)) {
                        break;
                    }
                } catch (final PluginException e) {
                    final boolean isLastMirror = mirrors.length - 1 == i;
                    if (isLastMirror) {
                        throw e;
                    } else {
                        logger.log(e);
                        logger.info("Skipping broken mirror reason#2: " + dllink);
                        continue;
                    }
                }
                /* Ugly workaround */
                logger.info("Skipping non working mirror: " + dllink);
                br = br2.cloneBrowser();
            }
            if (dl == null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadlink");
            } else {
                dl.startDownload();
            }
        }
    }

    /**
     * 2019-05-11: Their final-downloadlinks usually contain the md5 checksum of the file and this is the only place we can get it from.
     * This function tries to find this md5 value and sets it if possible.
     */
    protected boolean getAndSetMd5Hash(final DownloadLink link, final String dllink) {
        final String md5Value = new Regex(dllink, "md5=([a-f0-9]{32})").getMatch(0);
        final HashInfo md5Hash = HashInfo.parse(md5Value, true, false);
        if (md5Hash != null) {
            final HashInfo existingHash = link.getHashInfo();
            if (existingHash == null || existingHash.isWeakerThan(md5Hash) || (existingHash.getType() == md5Hash.getType() && !existingHash.equals(md5Hash))) {
                logger.info("Found hash on downloadstart:" + md5Hash + "|Existing:" + existingHash);
                link.setHashInfo(md5Hash);
            }
            return true;
        }
        return false;
    }

    static enum DownloadType {
        ACCOUNT_PREMIUM,
        ACCOUNT_FREE,
        GUEST_FREE,
        GUEST_PREMIUMLINK;
    }

    /** 2019-05-11: Limits seem to be the same for all of their services. */
    private boolean initDownload(final DownloadType downloadType, final DownloadLink link, final Account account, final String directlink) throws Exception {
        if (directlink == null) {
            logger.warning("dllink is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean success = false;
        final boolean previousFollowRedirectState = br.isFollowingRedirects();
        br.setFollowRedirects(true);
        try {
            switch (downloadType) {
            case ACCOUNT_PREMIUM:
            case GUEST_PREMIUMLINK:
                dl = new jd.plugins.BrowserAdapter().openDownload(br, this.getDownloadLink(), directlink, true, 0);
                break;
            default:
                dl = new jd.plugins.BrowserAdapter().openDownload(br, this.getDownloadLink(), directlink, true, 1);
                break;
            }
            if (dl.getConnection().getURL().getPath().startsWith("/error/download/ip")) {
                br.followConnection(true);
                // 403 by itself
                // response code 403 && <p>You have reached the limit of downloads from this IP address, please contact our
                if (downloadType == DownloadType.ACCOUNT_PREMIUM) {
                    throw new AccountUnavailableException("403: You have reached the limit of downloads from this IP address", 30 * 60 * 1000l);
                } else {
                    // some reason we have different error handling for free.
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "You cannot download this file with your current IP", 60 * 60 * 1000l);
                }
            } else if (dl.getConnection().getResponseCode() == 403) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403");
            } else if (dl.getConnection().getResponseCode() == 404) {
                br.followConnection(true);
                if (dl.getConnection().getURL().getPath().startsWith("landpage")) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Blocked by ISP?", 60 * 60 * 1000l);
                } else {
                    logger.info("File is offline on download-attempt");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                handleGeneralErrors(br, account);
                return false;
            } else {
                getAndSetMd5Hash(link, dl.getConnection().getURL().toString());
                success = true;
                return true;
            }
        } finally {
            br.setFollowRedirects(previousFollowRedirectState);
            try {
                if (!success) {
                    dl.getConnection().disconnect();
                    dl = null;
                }
            } catch (final Throwable t) {
            }
            logger.info("Mirror is " + (success ? "okay: " : "down: ") + directlink);
        }
    }

    /** Attempts to download pre-given premium direct-URLs. */
    protected boolean handlePremiumLink(final DownloadLink link, final Account account) throws Exception {
        if (!link.getPluginPatternMatcher().matches(TYPE_premiumRedirectLinks)) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (initDownload(DownloadType.GUEST_PREMIUMLINK, link, account, link.getPluginPatternMatcher())) {
            handleErrorsPreDownloadstart(dl.getConnection());
            dl.startDownload();
            return true;
        } else {
            logger.info("Download of supposedly direct-downloadable premium link failed");
            this.dl = null;
            return false;
        }
    }

    private void handleGeneralErrors(final Browser br, final Account account) throws PluginException {
        if (br.containsHTML("(?i)Try to download it once again after")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Try again later'", 20 * 60 * 1000l);
        } else if (br.containsHTML("(?i)>\\s*Ссылка просрочена\\. Пожалуйста получите")) {
            /* Either user waited too long for the captcha or maybe slow servers */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'link expired'", 5 * 60 * 1000l);
        } else if (br.containsHTML("(?i)Our service is currently unavailable in your country\\.")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, this.getHost() + " is currently unavailable in your country!");
        } else if (br.containsHTML("(?i)>\\s*Your IP exceeded the max\\.? number of files that can be downloaded")) {
            final Regex durationRegex = br.getRegex("You will be able to download at high speed again in (\\d+) hour\\(s\\) (\\d+) minute");
            final String hoursStr = durationRegex.getMatch(0);
            final String minutesStr = durationRegex.getMatch(1);
            long wait = 30 * 60 * 1000l;
            if (hoursStr != null && minutesStr != null) {
                wait = (Long.parseLong(hoursStr) * 60 * 60 + Long.parseLong(minutesStr) * 60) * 1000l;
            }
            if (account != null) {
                throw new AccountUnavailableException("Your IP exceeded the max number of files that can be downloaded", wait);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Your IP exceeded the max number of files that can be downloaded", 5 * 60 * 1000l);
            }
        }
    }

    private void handleErrorsPreDownloadstart(final URLConnectionAdapter con) throws PluginException {
        if (con.getLongContentLength() == 0) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, server sends empty file", 5 * 60 * 1000l);
        }
    }

    @Override
    public boolean hasAutoCaptcha() {
        return false;
    }

    private static AtomicReference<String> userAgent = new AtomicReference<String>(null);

    private Browser prepBrowserGeneral(final Browser prepBr, String UA) {
        if (UA == null) {
            userAgent.set(UserAgents.stringUserAgent());
            UA = userAgent.get();
        }
        prepBr.getHeaders().put("Pragma", null);
        prepBr.getHeaders().put("Cache-Control", null);
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Accept", "text/html, application/xhtml+xml, */*");
        prepBr.getHeaders().put("Accept-Language", "en-EN");
        prepBr.getHeaders().put("User-Agent", UA);
        prepBr.getHeaders().put("Referer", null);
        prepBr.setCustomCharset("UTF-8");
        prepBr.setCookie(getMainpage(), "JD", "1");
        prepBr.setCookie(getMainpage(), "set_user_lang_change", "en");
        return prepBr;
    }

    /** Only call this if a valid APIKey is available!! */
    private Browser prepBrowserAPI(final Browser prepBr, String UA) {
        prepBrowserGeneral(prepBr, UA);
        prepBr.getHeaders().put("X-API-KEY", this.getAPIKey());
        return prepBr;
    }

    private Browser prepBrowserWebsite(final Browser prepBr, String UA) {
        prepBrowserGeneral(prepBr, UA);
        return prepBr;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                setBrowserExclusive();
                /*
                 * we have to reuse old UA, else the cookie will become invalid
                 */
                String ua = account.getStringProperty("UA");
                prepBrowserWebsite(br, ua);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                getPage(this.getMainpage());
                final String curr_domain = br.getHost();
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    br.setCookies(curr_domain, cookies);
                    /* Request same URL again, this time with cookies set */
                    getPage(br.getURL());
                    if (isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        /* Set new cookie timestamp */
                        br.setCookies(curr_domain, cookies);
                        return;
                    }
                    logger.info("cookie login failed: Full login is required");
                    if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                        logger.warning("Cookie login failed MAKE SURE THAT YOU RE-USED THE SAME USER-AGENT AS USED FOR THE FIRST LOGIN ELSE COOKIE LOGIN WILL NOT WORK!!!");
                    }
                }
                /* lets set a new User-Agent */
                logger.info("Performing full login");
                prepBrowserWebsite(br, null);
                getPage("https://" + curr_domain + "/login");
                Form loginform = findAndPrepareLoginForm(br, account);
                submitForm(loginform);
                boolean requiredLoginCaptcha = false;
                if (findLoginForm(br, account) != null) {
                    /* Check for stupid login captcha */
                    loginform = findAndPrepareLoginForm(br, account);
                    DownloadLink link = getDownloadLink();
                    if (link == null) {
                        link = new DownloadLink(this, "Account", account.getHoster(), getMainpage(), true);
                        this.setDownloadLink(link);
                    }
                    processCaptchaForm(link, account, loginform, br, true);
                    if (loginform.containsHTML("class=\"reloadCaptcha\"")) {
                        /* Old captcha - e.g. wayupload.com */
                        requiredLoginCaptcha = true;
                        final String captchaurl = br.getRegex("(https?://[^/]+/captcha/securimg[^\"<>]+)").getMatch(0);
                        if (captchaurl == null) {
                            logger.warning("Failed to find captchaURL");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final String code = this.getCaptchaCode(captchaurl, link);
                        loginform.put("user%5Bcaptcha_response%5D", Encoding.urlEncode(code));
                        loginform.put("user%5Bcaptcha_type%5D", "securimg");
                        loginform.put("user%5Bcaptcha_subtype%5D", "9");
                    }
                    submitForm(loginform);
                }
                universalLoginErrorhandling(br);
                handlePremiumActivation(br, account);
                if (!isLoggedIN(br)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.\r\n3. Gehe auf folgende Seite, deaktiviere den Login Captcha Schutz deines Accounts und versuche es erneut: " + account.getHoster() + "/user/settings", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.\r\n3. Access the following site and disable the login captcha protection of your account and try again: " + account.getHoster() + "/user/settings", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (requiredLoginCaptcha) {
                    /* Display hint to user on how to disable login captchas. */
                    showLoginCaptchaInformation(account);
                }
                account.saveCookies(br.getCookies(curr_domain), "");
                account.setProperty("UA", userAgent.get());
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.setProperty("UA", Property.NULL);
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    protected void handlePremiumActivation(Browser br, Account account) throws Exception {
        if (!isLoggedIN(br) && br.containsHTML("<div[^>]*id\\s*=\\s*\"activation-form\"")) {
            // <h1>Premium activation</h1>
            // <div id="activation-form">
            // <input-premium-block
            // predefined-premium-key="XXXX"
            // predefined-email="YYYYYYY"
            // :logged-in="false"
            // :auto-submit="true"
            // custom-handler=""
            // >
            // </input-premium-block>
            // <div class="block-title"> Please, enter the premium code below, if you already have it. </div>
            String predefinedpremiumkey = br.getRegex("predefined-premium-key\\s*=\\s*\"(.*?)\"").getMatch(0);
            String predefinedemail = br.getRegex("predefined-email\\s*=\\s*\"(.*?)\"").getMatch(0);
            if (StringUtils.isEmpty(predefinedemail)) {
                predefinedemail = account.getUser();
            }
            if (StringUtils.isEmpty(predefinedpremiumkey)) {
                // same as account password?
                predefinedpremiumkey = account.getPass();
            }
            postPage(br, "/payments/premium/process", "premium=" + URLEncode.encodeRFC2396(predefinedpremiumkey) + "&email=" + URLEncode.encodeRFC2396(predefinedemail));
            final Map<String, Object> response = restoreFromString(br.toString(), TypeRef.MAP);
            if (Boolean.TRUE.equals(response.get("success"))) {
                final String redirect = (String) response.get("redirect");
                if (redirect != null) {
                    getPage(redirect);
                }
                if (isLoggedIN(br)) {
                    return;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            final String message = (String) response.get("message");
            throw new AccountInvalidException(message);
        }
    }

    private Thread showLoginCaptchaInformation(final Account account) {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = account.getHoster() + " - Login Captcha";
                        message += "Hallo liebe(r) " + account.getHoster() + " NutzerIn\r\n";
                        message += "Um den Account dieses Anbieters in JDownloader verwenden zu können, musst du derzeit ein Login-Captcha eingeben.\r\n";
                        message += "Falls dich das stört, kannst du folgendes tun:\r\n";
                        message += "1. Logge dich im Browser ein.\r\n";
                        message += "2. Navigiere zu: " + account.getHoster() + "/user/settings\r\n";
                        message += "3. Entferne das Häckchen bei 'Captcha einsetzen, um meinen Account zu schützen' und klicke unten auf speichern.\r\n";
                        message += "Dein Account sollte sich ab sofort ohne Login-Captchas in JD hinzufügen/prüfen lassen.\r\n";
                    } else {
                        title = account.getHoster() + " - Login-Captcha";
                        message += "Hello dear " + account.getHoster() + " user\r\n";
                        message += "In order to add/check your account of this service, you have to solve login-captchas at this moment.\r\n";
                        message += "If that is annoying to you, you can deactivate them as follows:\r\n";
                        message += "1. Login via browser.\r\n";
                        message += "2. Open this page: " + account.getHoster() + "/user/settings\r\n";
                        message += "3. Uncheck the checkbox 'Use a captcha to protect my account'.\r\n";
                        message += "From now on, you should be able to add/check your account in JD without the need of a login-captcha.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
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

    protected boolean isLoggedIN(final Browser br) {
        return br != null && br.containsHTML("(?i)/user/logout");
    }

    private Form findAndPrepareLoginForm(Browser br, final Account account) throws PluginException {
        if (account == null) {
            return null;
        } else {
            final Form loginForm = findLoginForm(br, account);
            if (loginForm != null) {
                loginForm.put("user%5Blogin%5D", Encoding.urlEncode(account.getUser()));
                loginForm.put("user%5Bpass%5D", Encoding.urlEncode(account.getPass()));
                return loginForm;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private Form findLoginForm(Browser br, final Account account) {
        return br.getFormbyAction("/user/login");
    }

    public static void universalLoginErrorhandling(final Browser br) throws PluginException {
        if (br.containsHTML(">Limit of login attempts exceeded for your account")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nMaximale Anzahl von Loginversuchen überschritten - dein Account wurde temporär gesperrt!\r\nBestätige deinen Account per E-Mail um ihn zu entsperren.\r\nFalls du keine E-Mail bekommen hast, gib deine E-Mail Adresse auf folgender Seite ein und lasse dir erneut eine zuschicken: " + br.getHost() + "/restoreaccess", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLimit of login attempts exceeded for your account - your account is locked!\r\nConfirm your account via e-mail to unlock it.\r\nIf you haven't received an e-mail, enter your e-mail address on the following site so the service can send you a new confirmation mail: " + br.getHost() + "/restoreaccess", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    public String getMainpage() {
        if (supports_https()) {
            return "https://" + this.getConfiguredDomain() + "/";
        } else {
            return "http://" + this.getConfiguredDomain() + "/";
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    /** For some hosts, users can configure their preferred domain. */
    protected String getConfiguredDomain() {
        return this.getHost();
    }

    protected void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_FREE_PARALLEL_DOWNLOADSTARTS, "Activate parallel downloadstarts in free mode?\r\n<html><p style=\"color:#F62817\"><b>Warning: This setting can lead to a lot of non-accepted captcha popups!</b></p></html>").setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), "APIKEY", "Define custom APIKey (can be
        // found on website in account settings ['/user/settings'])").setDefaultValue(""));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Turbobit_Turbobit;
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink link, final PluginForHost buildForThisPlugin) {
        try {
            return getContentURL(this.getHost(), this.getFUID(link));
        } catch (final PluginException e) {
            return null;
        }
    }
}