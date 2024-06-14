//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.PixeldrainConfig;
import org.jdownloader.plugins.components.config.PixeldrainConfig.ActionOnCaptchaRequired;
import org.jdownloader.plugins.components.config.PixeldrainConfig.ActionOnSpeedLimitReached;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PixeldrainCom extends PluginForHost {
    public PixeldrainCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://pixeldrain.com/#pro");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.API_KEY_LOGIN };
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/about";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pixeldrain.com", "pixeldra.in" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:api/file/|u/)?([A-Za-z0-9]{2,})");
        }
        return ret.toArray(new String[0]);
    }

    /* Docs: https://pixeldrain.com/api */
    public static final String  API_BASE                                      = "https://pixeldrain.com/api";
    private static final String PROPERTY_CAPTCHA_REQUIRED                     = "captcha_required";
    private static final String PROPERTY_ACCOUNT_HAS_SHOWN_APIKEY_HELP_DIALOG = "has_shown_apikey_help_dialog";

    private String getAPIURLUser() {
        return API_BASE + "/user";
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free user/ free download */
            return true;
        }
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        if (this.isLinktypeFilesystem(link)) {
            /**
             * Premium link -> No limit </br>
             * Links which can basically be downloaded like a premium user but for free -> No limit
             */
            return 0;
        } else if (account != null && AccountType.PREMIUM.equals(account.getType())) {
            /* Premium user -> No limit */
            return 0;
        } else {
            /* Free user/ free download */
            return 1;
        }
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

    @Override
    public String getMirrorID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    private String getFilesystemFileID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(?i)https?://[^/]+/api/filesystem/(.+)").getMatch(0);
    }

    /**
     * Returns true for direct downloadable 'filesystem' single file URL. </br>
     * User docs: https://pixeldrain.com/filesystem
     */
    private boolean isLinktypeFilesystem(final DownloadLink link) {
        if (getFilesystemFileID(link) != null) {
            return true;
        } else {
            return false;
        }
    }

    private String getFID(final DownloadLink link) {
        final String filesystemFid = this.getFilesystemFileID(link);
        if (filesystemFid != null) {
            return filesystemFid;
        } else {
            return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        }
    }

    public boolean isTransferLimitReached(final DownloadLink link, final Account account) {
        final String speedLimitPropertyKey = getSpeedLimitProperty(account);
        if (account != null && AccountType.PREMIUM.equals(account.getType())) {
            /* User owns premium account -> No speed limit */
            return false;
        } else if (link.getIntegerProperty(speedLimitPropertyKey, 0) > 0) {
            /* Speed limit is set on link in free mode -> Speed limit is active */
            return true;
        } else {
            /* No speed limit active on link -> No speed limit */
            return false;
        }
    }

    protected static String getSpeedLimitProperty(final Account account) {
        if (account == null || !AccountType.PREMIUM.equals(account.getType())) {
            return "download_speed_limit";
        } else {
            return "download_speed_limit_" + account.getUser();
        }
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return this.checkLinks(urls, account);
    }

    public boolean checkLinks(final DownloadLink[] allLinks, final Account account) {
        if (allLinks == null || allLinks.length == 0) {
            return false;
        }
        try {
            if (account != null) {
                this.login(account, false);
            }
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                final StringBuilder sb = new StringBuilder();
                links.clear();
                while (true) {
                    /* We test up to 100 links at once (more is possible). */
                    if (index == allLinks.length || links.size() == 100) {
                        break;
                    } else {
                        links.add(allLinks[index]);
                        index++;
                    }
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink link : links) {
                    if (this.isLinktypeFilesystem(link)) {
                        /* Such items do not [yet] have mass-linkcheck support! */
                        continue;
                    }
                    if (sb.length() > 0) {
                        /* Append comma */
                        sb.append("%2C");
                    }
                    sb.append(this.getFID(link));
                }
                try {
                    List<Map<String, Object>> items = null;
                    /**
                     * If the StringBuilder item is empty this means that currently we got only items which cannot be mass-linkchecked.
                     * </br>
                     * In this case, no http request is needed.
                     */
                    if (sb.length() > 0) {
                        br.getPage(API_BASE + "/file/" + sb.toString() + "/info");
                        final Object response = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
                        if (response instanceof List) {
                            items = (List<Map<String, Object>>) response;
                        } else {
                            /* E.g. when only one fileID was checked API will return a map instead of a list of maps. */
                            final Map<String, Object> responseMap = (Map<String, Object>) response;
                            if ((Boolean) responseMap.get("success") == Boolean.FALSE) {
                                /* All files checked in this run are offline. */
                                for (final DownloadLink link : links) {
                                    link.setAvailable(false);
                                }
                                continue;
                            } else {
                                items = new ArrayList<Map<String, Object>>();
                                items.add((Map<String, Object>) response);
                            }
                        }
                    }
                    /* Offline/invalid fileIDs won't be returned via API so we'll have to look for the data of our targetID. */
                    for (final DownloadLink link : links) {
                        if (items == null || this.isLinktypeFilesystem(link)) {
                            /* Such items do not [yet] have mass-linkcheck support! */
                            link.setAvailableStatus(AvailableStatus.UNCHECKED);
                        } else {
                            final String id = this.getFID(link);
                            Map<String, Object> data = null;
                            for (final Map<String, Object> item : items) {
                                final String thisID = (String) item.get("id");
                                if (StringUtils.equals(thisID, id)) {
                                    data = item;
                                    break;
                                }
                            }
                            if (data == null) {
                                /* FileID not in response, so it's offline */
                                link.setAvailable(false);
                            } else {
                                setDownloadLinkInfo(this, link, account, data);
                            }
                        }
                    }
                } finally {
                    if (index == allLinks.length) {
                        /* We've reached the end */
                        break;
                    }
                }
            }
        } catch (final Exception ignore) {
            logger.log(ignore);
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (this.isLinktypeFilesystem(link)) {
            /* 2024-06-12: Do not check such links at all for now. */
            if (account != null) {
                this.login(account, false);
            }
            return AvailableStatus.UNCHECKABLE;
        } else {
            this.checkLinks(new DownloadLink[] { link }, account);
            checkErrors(br, link, account);
            if (!link.isAvailable()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        }
    }

    /** Shared function used by crawler & host plugin. */
    public static void setDownloadLinkInfo(final Plugin plugin, final DownloadLink link, final Account account, final Map<String, Object> data) throws PluginException {
        final String filename = (String) data.get("name");
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        link.setVerifiedFileSize(((Number) data.get("size")).longValue());
        link.setSha256Hash(data.get("hash_sha256").toString());
        final String description = (String) data.get("description");
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        final String availability = (String) data.get("availability");
        if (isCaptchaRequiredStatus(availability)) {
            link.setProperty(PROPERTY_CAPTCHA_REQUIRED, true);
        } else {
            link.removeProperty(PROPERTY_CAPTCHA_REQUIRED);
        }
        /* Determine if there is a speed-limit in place. */
        final Object speedLimit = data.get("download_speed_limit");
        final String speedLimitPropertyKey = getSpeedLimitProperty(account);
        if (speedLimit == null || ((speedLimit instanceof Number) && ((Number) speedLimit).intValue() == 0)) {
            /* No speed limit in place */
            link.removeProperty(speedLimitPropertyKey);
        } else {
            link.setProperty(speedLimitPropertyKey, speedLimit);
        }
        /**
         * Check if file has been abused. </br>
         * We are checking this just here at the end because file information for such files can still be available and we want to provide
         * as much information to the user as possible.
         */
        final String abuse_type = (String) data.get("abuse_type");
        if (!StringUtils.isEmpty(abuse_type)) {
            /* File has been abused / deleted. */
            link.setAvailable(false);
        } else {
            /* File is online and downloadable. */
            link.setAvailable(true);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account, true);
        final String dllink;
        if (isLinktypeFilesystem(link)) {
            /* That link shall be direct-downloadable. */
            dllink = link.getPluginPatternMatcher();
        } else {
            final PixeldrainConfig cfg = PluginJsonConfig.get(getConfigInterface());
            if (isTransferLimitReached(link, account) && cfg.getActionOnSpeedLimitReached() == ActionOnSpeedLimitReached.TRIGGER_RECONNECT_TO_CHANGE_IP) {
                /**
                 * User prefers to perform reconnect to be able to download without speedlimit again. </br>
                 * 2022-07-19: Speedlimit sits only on IP, not on account but our upper system will of not do reconnects for accounts atm.
                 */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, String.format("You are speed limited. Change your IP, try again later or allow speed limited downloads in %s plugin settings.", this.getHost()), 15 * 60 * 1000l);
            }
            final UrlQuery query = new UrlQuery();
            query.add("download", "");
            if (this.hasCaptcha(link, account)) {
                if (cfg.getActionOnCaptchaRequired() == ActionOnCaptchaRequired.PROCESS_CAPTCHA) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Lfbzz4UAAAAAAaBgox1R7jU0axiGneLDkOA-PKf").getToken();
                    query.appendEncoded("recaptcha_response", recaptchaV2Response);
                } else {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait to avoid captcha", 5 * 60 * 1000l);
                }
            }
            /* That link shall be direct-downloadable. */
            dllink = API_BASE + "/file/" + this.getFID(link) + "?" + query.toString();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            checkErrors(br, link, account);
            /* We're using an API so let's never throw PluginExceptions with LinkStatus "Plugin defect". */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private static boolean isCaptchaRequiredStatus(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("(?i).*_captcha_required$")) {
            /*
             * 2022-02-23: Either "file_rate_limited_captcha_required" or "virus_detected_captcha_required". This can also happen for other
             * reasons such as reached rate-limits.
             */
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        /**
         * 2021-01-15: (Free) Accounts = No captcha required for downloading (usually not even via anonymous files but captchas can
         * sometimes be required for files with high traffic). </br>
         * There are also "Donator" Accounts (at this moment we don't try to differ between them) but the download process is no different
         * when using those!
         */
        final Map<String, Object> user = login(account, true);
        /* User will always only have one running subscription. */
        final Map<String, Object> subscription = (Map<String, Object>) user.get("subscription");
        final AccountInfo ai = new AccountInfo();
        account.setUser(user.get("email").toString());
        ai.setUsedSpace(((Number) user.get("storage_space_used")).longValue());
        if ("free".equalsIgnoreCase(subscription.get("type").toString())) {
            /* Assume it's a free account */
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(this.getMaxSimultanFreeDownloadNum());
            ai.setUnlimitedTraffic();
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(this.getMaxSimultanPremiumDownloadNum());
        }
        account.setConcurrentUsePossible(true);
        final long monthlyTrafficMax = ((Number) subscription.get("monthly_transfer_cap")).longValue();
        final long monthlyTrafficUsed = ((Number) user.get("monthly_transfer_used")).longValue();
        if (monthlyTrafficMax == -1) {
            ai.setUnlimitedTraffic();
        } else {
            ai.setTrafficMax(monthlyTrafficMax);
            ai.setTrafficLeft(monthlyTrafficMax - monthlyTrafficUsed);
        }
        /* Build text string which will be displayed to user in GUI. */
        String accountStatusText = subscription.get("name").toString();
        final double euroBalance = ((Number) user.get("balance_micro_eur")).doubleValue();
        accountStatusText += String.format(" | %2.2f€", euroBalance / 1000000);
        accountStatusText += " | Monthly used: " + SizeFormatter.formatBytes(monthlyTrafficUsed);
        ai.setStatus(accountStatusText);
        /**
         * Global limits and limits for (anonymous) users can be checked here: https://pixeldrain.com/api/misc/rate_limits </br>
         * Once one of these limits is hit, a captcha will be required for downloading.</br>
         * These captchas can be avoided by using free/paid accounts.
         */
        account.setAllowReconnectToResetLimits(true);
        return ai;
    }

    private Map<String, Object> login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            final String apikey = account.getPass();
            /* Set login auth header */
            br.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Basic " + Encoding.Base64Encode(":" + apikey));
            if (!force) {
                /* Do not check API Key. */
                return null;
            }
            br.getPage(getAPIURLUser());
            final Map<String, Object> response = this.checkErrors(br, null, account);
            return response;
        }
    }

    private Map<String, Object> checkErrors(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        /* Wait milliseconds for unknown/generic errors */
        final long waitmillis = 60 * 1000;
        Map<String, Object> entries = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } catch (final JSonMapperException ignore) {
            /* This should never happen. */
            final String errortext = "Invalid API response";
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errortext, waitmillis);
            } else {
                throw new AccountUnavailableException(errortext, waitmillis);
            }
        }
        final Object successO = entries.get("success");
        if (successO == null || Boolean.TRUE.equals(successO)) {
            /* No error */
            return entries;
        }
        final String value = (String) entries.get("value");
        final String message = (String) entries.get("message");
        if (isCaptchaRequiredStatus(value)) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA, message);
        } else if (value.equalsIgnoreCase("not_found")) {
            /* {"success":false,"value":"not_found","message":"The entity you requested could not be found"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (value.equalsIgnoreCase("authentication_failed")) {
            /* Login failed. */
            if (!account.hasEverBeenValid()) {
                showApiLoginInformation(account);
            }
            throw new AccountInvalidException(message);
        } else if (value.equalsIgnoreCase("out_of_transfer")) {
            /**
             * Typically for 'filesystem' links: </br>
             * This happens if the user who has provided that link has run out of traffic. </br>
             * The link can be downloaded once the uploader has more traffic available or if the user who wants to download it has premium
             * traffic.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message, 2 * 60 * 60 * 1000);
        } else {
            /* Unknown error */
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message, waitmillis);
            } else {
                throw new AccountUnavailableException(message, waitmillis);
            }
        }
    }

    /** Shows special login information if it hasn't already been displayed for the given account. */
    private void showApiLoginInformation(final Account account) {
        synchronized (account) {
            /* Do not display this dialog if it has been displayed before for this account. */
            if (account.hasProperty(PROPERTY_ACCOUNT_HAS_SHOWN_APIKEY_HELP_DIALOG)) {
                return;
            }
            /* Only display this dialog once per account! */
            account.setProperty(PROPERTY_ACCOUNT_HAS_SHOWN_APIKEY_HELP_DIALOG, true);
            final Thread thread = new Thread() {
                public void run() {
                    try {
                        String message = "";
                        final String title;
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            title = "Pixeldrain - Login";
                            message += "\r\nHallo liebe(r) Pixeldrain NutzerIn";
                            message += "\r\nUm deinen Pixeldrain Account in JDownloader verwenden zu können, musst du auf der Pixeldrain Webseite einen API Key erstellen und ihn in JDownloader in das 'Passwort' bzw. 'API Key' Feld eingeben:";
                            message += "\r\n" + getAPILoginHelpURL();
                            message += "\r\nFalls du MyJDownloader benutzt, gib den API Key in die Felder Benutzername und Passwort ein.";
                        } else {
                            title = "Pixeldrain - Login";
                            message += "\r\nHello dear Pixeldrain user";
                            message += "\r\nIn order to use an account of this service in JDownloader, you need to generate an API key on the following page and put it into the 'Password' or 'API Key' field in JDownloader:";
                            message += "\r\n" + getAPILoginHelpURL();
                            message += "\r\nIf you are using MyJDownloader, enter the API key in both the username- and password fields.";
                        }
                        final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                        dialog.setTimeout(3 * 60 * 1000);
                        if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                            CrossSystem.openURL(getAPILoginHelpURL());
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
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        if (acc != null && acc.getType() == AccountType.PREMIUM) {
            /* Premium users never need to enter captchas. */
            return false;
        } else if (link.hasProperty(PROPERTY_CAPTCHA_REQUIRED)) {
            /* To download this link in free mode, a captcha is required at this moment. */
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    @Override
    public Class<? extends PixeldrainConfig> getConfigInterface() {
        return PixeldrainConfig.class;
    }

    @Override
    protected String getAPILoginHelpURL() {
        return "https://" + getHost() + "/user/connect_app?app=jdownloader";
    }

    @Override
    protected boolean looksLikeValidAPIKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.replace("-", "").matches("[A-Fa-f0-9]{32}")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}