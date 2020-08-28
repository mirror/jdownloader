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
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.YetiShareCore;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class OxycloudPl extends YetiShareCore {
    public OxycloudPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    private static final String PROPERTY_needs_premium      = "needs_premium";
    private static final String PROPERTY_account_max_chunks = "max_chunks";

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null solvemedia reCaptchaV2<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "oxycloud.pl" });
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
        return YetiShareCore.buildAnnotationUrls(getPluginDomains());
    }

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

    public int getMaxChunks(final Account account) {
        if (account == null) {
            return 1;
        } else {
            /* Limits may vary depending on account type - controlled via API! */
            return account.getIntegerProperty(PROPERTY_account_max_chunks, 1);
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (link.getBooleanProperty(PROPERTY_needs_premium, false) && (account == null || account.getType() != AccountType.PREMIUM)) {
            return false;
        } else {
            return true;
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

    @Override
    public String[] scanInfo(final DownloadLink link, final String[] fileInfo) {
        super.scanInfo(link, fileInfo);
        if (supports_availablecheck_over_info_page(link)) {
            /* 2020-08-14: Special */
            if (StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = br.getRegex("File size</th>\\s*<td>([^<>\"]+)</td>").getMatch(0);
            }
        }
        return fileInfo;
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        this.getPage("/download-limits-calculator");
        final String trafficLeft = br.getRegex("class=\"fa fa-download\"></i>([^<>]*)</span>").getMatch(0);
        final String expireDate = br.getRegex("This package is active until (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2})").getMatch(0);
        if (trafficLeft != null && expireDate != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy-MM-dd hh:mm", Locale.ENGLISH), this.br);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(this.getMaxSimultanPremiumDownloadNum());
            ai.setStatus("Premium account");
            ai.setTrafficLeft(SizeFormatter.getSize(trafficLeft));
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(this.getMaxSimultaneousFreeAccountDownloads());
            /* All accounts get the same (IP-based) downloadlimits --> Simultan free account usage makes no sense! */
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
            ai.setUnlimitedTraffic();
        }
        return ai;
    }
    /* *************************** PUT API RELATED METHODS HERE *************************** */

    @Override
    protected String getAPIBase() {
        return "https://" + this.getHost() + "/api/v1";
    }

    @Override
    protected String getAccountOverviewURL() {
        return this.getMainPage() + "/account";
    }

    @Override
    protected boolean supports_api() {
        return true;
    }

    private void setAPIHeaders(final Browser br, final Account account) {
        br.getHeaders().put("authentication", this.getAPIAccessToken(account));
        br.getHeaders().put("account", this.getAPIAccountID(account));
    }

    @Override
    protected AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = super.fetchAccountInfoAPI(account);
        /* 2020-08-26: They've built some stuff on top of the normal YetiShare API --> Handle this here */
        final Browser brc = br.cloneBrowser();
        setAPIHeaders(brc, account);
        this.getPage(brc, this.getAPIBase() + "/package/limits");
        Map<String, Object> entries = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("account");
        final String accType = (String) entries.get("type");
        final boolean isUnlimited = ((Boolean) entries.get("isUnlimited")).booleanValue();
        final boolean isPremium = ((Boolean) entries.get("isPremium")).booleanValue();
        if (isUnlimited) {
            ai.setUnlimitedTraffic();
        } else {
            /*
             * 2020-08-26: These values are usually null for free accounts but we'll try to set them anyways in case they change this in the
             * future.
             */
            long totalBytesLeft = 0;
            long dailyBytesLeft = 0;
            Object totalBytesLeftO = entries.get("totalBytesLeft");
            Object dailyBytesLeftO = entries.get("dailyBytesLeft");
            if (totalBytesLeftO != null && totalBytesLeftO instanceof Number) {
                totalBytesLeft = ((Number) totalBytesLeftO).longValue();
            }
            if (dailyBytesLeftO != null && totalBytesLeftO instanceof Number) {
                dailyBytesLeft = ((Number) dailyBytesLeftO).longValue();
            }
            ai.setTrafficLeft(totalBytesLeft);
            /* 2020-08-27: Rather display the complete traffic left than daily limits. */
            // try {
            // final long maxDailyBytes = ((Long) entries.get("maxDailyBytes")).longValue();
            // ai.setTrafficMax(maxDailyBytes);
            // ai.setTrafficLeft(dailyBytesLeft);
            // } catch (final Throwable e) {
            // }
            if (dailyBytesLeft <= 0) {
                /* TODO: Test what happens is a user tries to download in this state */
                logger.warning("No daily traffic left");
            }
        }
        if ("paid".equalsIgnoreCase(accType) || isPremium) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        final int maxNumberOfActiveDownloads;
        final Object maxNumberOfActiveDownloadsO = entries.get("maxNumberOfActiveDownloads");
        if (maxNumberOfActiveDownloadsO != null && maxNumberOfActiveDownloadsO instanceof Number) {
            maxNumberOfActiveDownloads = ((Number) maxNumberOfActiveDownloadsO).intValue();
        } else {
            /* Fallback */
            maxNumberOfActiveDownloads = 1;
        }
        if (maxNumberOfActiveDownloads > 0) {
            account.setMaxSimultanDownloads(maxNumberOfActiveDownloads);
        }
        final Object maxNumberOfChunksO = entries.get("maxNumberOfChunks");
        if (maxNumberOfChunksO != null && maxNumberOfChunksO instanceof Number) {
            final int maxNumberOfChunks = ((Number) maxNumberOfActiveDownloadsO).intValue();
            if (maxNumberOfChunks > 1) {
                account.setProperty(PROPERTY_account_max_chunks, -maxNumberOfChunks);
            }
        }
        /* Do not set account status here as upper code already did that! */
        // ai.setStatus("Bla");
        return ai;
    }

    private Account getApiAccount() {
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc != null && this.getAPIAccessToken(acc) != null && this.getAPIAccountID(acc) != null) {
            return acc;
        } else {
            return null;
        }
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        final Account apiAccount = getApiAccount();
        if (apiAccount != null) {
            return massLinkcheckerAPI(urls, apiAccount);
        } else {
            /* No mass linkchecking possible */
            return false;
        }
    }

    /**
     * Checks multiple URLs via API. Only works when an account with API credentials is given.
     */
    public boolean massLinkcheckerAPI(final DownloadLink[] urls, final Account apiAccount) {
        if (urls == null || urls.length == 0 || apiAccount == null) {
            return false;
        }
        boolean linkcheckerHasFailed = false;
        try {
            final Browser br = new Browser();
            this.setAPIHeaders(br, apiAccount);
            this.prepBrowser(br, getMainPage());
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /*
                     * 2020-08-27: Tested for up to 100 items but we'll check max. 50 per request.
                     */
                    if (index == urls.length || links.size() == 50) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink dl : links) {
                    // sb.append("%0A");
                    sb.append(dl.getPluginPatternMatcher());
                    sb.append("%2C");
                }
                getPage(br, this.getAPIBase() + "/file/check?links=" + sb.toString());
                try {
                    this.checkErrorsAPI(br, links.get(0), null);
                } catch (final Throwable e) {
                    logger.log(e);
                    /* E.g. invalid apikey, broken serverside API. */
                    logger.info("Fatal failure");
                    return false;
                }
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("files");
                for (final DownloadLink link : links) {
                    boolean foundResult = false;
                    final String fuid = this.getFUID(link);
                    for (final Object fileO : ressourcelist) {
                        entries = (LinkedHashMap<String, Object>) fileO;
                        final String url = (String) entries.get("url");
                        final String fuid_tmp = this.getFUIDFromURL(url);
                        if (fuid_tmp != null && fuid_tmp.equalsIgnoreCase(fuid)) {
                            foundResult = true;
                            break;
                        }
                    }
                    if (!foundResult) {
                        /**
                         * This should never happen!
                         */
                        logger.warning("WTF failed to find information for fuid: " + fuid);
                        linkcheckerHasFailed = true;
                        continue;
                    }
                    final String status = (String) entries.get("status");
                    if (!"online".equalsIgnoreCase(status)) {
                        link.setAvailable(false);
                        setWeakFilename(link);
                    } else {
                        link.setAvailable(true);
                        String filename = (String) entries.get("filename");
                        final long filesize = JavaScriptEngineFactory.toLong(entries.get("fileSize"), 0);
                        link.setFinalFileName(filename);
                        link.setDownloadSize(filesize);
                        /* We don't care if one or both of these fields are not given though they should always be available! */
                        try {
                            link.setProperty(PROPERTY_needs_premium, ((Boolean) entries.get("isPremium")).booleanValue());
                            /* 2020-08-28: isPrivate = file will not be grabbed by search engines. Whoever has the URL can download it. */
                            // link.setProperty(PROPERTY_is_private, ((Boolean) entries.get("isPrivate")).booleanValue());
                        } catch (final Throwable e) {
                            logger.log(e);
                        }
                        /* 2020-08-28: CRC32 hash of the first file chunk (50MiB) */
                        // String hash = (String) entries.get("hash");
                        // if (!StringUtils.isEmpty(hash)) {
                        // hash = hash.replace("-", "");
                        // link.setMD5Hash(hash);
                        // }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            logger.log(e);
            return false;
        }
        if (linkcheckerHasFailed) {
            return false;
        } else {
            return true;
        }
    }
}