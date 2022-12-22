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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.config.YoutvDeConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class YoutvDe extends PluginForHost {
    public YoutvDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.youtv.de/produkte");
    }

    private final String PROPERTY_DIRECTURL                 = "directurl";
    private final String PROPERTY_LAST_FILE_ERROR           = "last_file_error";
    private final String PROPERTY_TIMESTAMP_LAST_FILE_ERROR = "timestamp_last_file_error";
    private final String PROPERTY_LAST_USED_QUALITY         = "last_used_quality";

    @Override
    public String getAGBLink() {
        return "https://help.youtv.de/hc/de/articles/360029117431-AGB";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "youtv.de" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/tv-sendungen/(\\d+)(-[a-z0-9\\-]+)?");
        }
        return ret.toArray(new String[0]);
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

    private Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(422); // response on login failure
        return br;
    }

    public void setBrowser(Browser br) {
        prepBR(br);
        super.setBrowser(br);
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
        final String fid = urlinfo.getMatch(0);
        if (!link.isNameSet()) {
            final String slug = urlinfo.getMatch(0);
            if (slug != null) {
                link.setName(fid + "_" + slug.replace("-", " ").trim() + ".mp4");
            } else {
                link.setName(fid + ".mp4");
            }
        }
        if (account == null) {
            /* Account needed to check such items. */
            return AvailableStatus.UNCHECKABLE;
        }
        this.setBrowserExclusive();
        this.login(account, false);
        br.setFollowRedirects(true);
        br.getPage("https://www." + this.getHost() + "/api/v2/recordings/" + fid + ".json");
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        checkErrors(br, link, account);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> recording = (Map<String, Object>) entries.get("recording");
        final List<Map<String, Object>> files = (List<Map<String, Object>>) recording.get("files");
        final String preferredQualityStr = getPreferredQualityStr(link);
        Map<String, Object> chosenQuality = null;
        for (final Map<String, Object> quality : files) {
            final String qualityStr = quality.get("quality").toString();
            if (qualityStr.equals(preferredQualityStr)) {
                chosenQuality = quality;
                break;
            }
        }
        if (chosenQuality == null) {
            logger.info("Failed to find preferred quality -> Fallback to first/best");
            chosenQuality = files.get(0);
        }
        final String description = (String) recording.get("long_text");
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        link.setVerifiedFileSize(((Number) chosenQuality.get("size")).longValue());
        final String directurl = chosenQuality.get("file").toString();
        final String originalFilename = new Regex(directurl, "/([^/]+\\.mp4)$").getMatch(0);
        if (originalFilename != null) {
            link.setFinalFileName(originalFilename);
        }
        link.setProperty(PROPERTY_DIRECTURL + "_" + chosenQuality.get("quality"), directurl);
        final List<String> file_errors = (List<String>) recording.get("file_errors");
        if (file_errors != null && file_errors.size() > 0) {
            /* Typically daily downloadlimit reached */
            link.setProperty(PROPERTY_LAST_FILE_ERROR, file_errors.get(0));
            link.setProperty(PROPERTY_TIMESTAMP_LAST_FILE_ERROR, System.currentTimeMillis());
        } else {
            link.removeProperty(PROPERTY_LAST_FILE_ERROR);
        }
        return AvailableStatus.TRUE;
    }

    private String getPreferredQualityStr(final DownloadLink link) {
        /* Prefer last used quality if existent. */
        String qualityStr = link.getStringProperty(PROPERTY_LAST_USED_QUALITY);
        if (qualityStr == null) {
            qualityStr = PluginJsonConfig.get(YoutvDeConfig.class).getPreferredQuality().name();
        }
        return qualityStr.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    private String getDirecturl(final DownloadLink link) {
        return link.getStringProperty(getDirecturlProperty(link));
    }

    private String getDirecturlProperty(final DownloadLink link) {
        return PROPERTY_DIRECTURL + "_" + getPreferredQualityStr(link);
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account != null) {
            /* Account is always needed to download items of this host. */
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Don't validate cookies */
                        return false;
                    }
                    br.getPage("https://www." + this.getHost() + "/abo-shop");
                    checkErrors(br, null, account);
                    if (this.isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://www." + this.getHost() + "/login");
                checkErrors(br, null, account);
                final Form loginform = br.getFormbyActionRegex(".*/login");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("session[email]", Encoding.urlEncode(account.getUser()));
                loginform.put("session[password]", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (br.getHttpConnection().getResponseCode() == 201) {
                    /* Returns empty page with response "201 created" on success */
                    logger.info("Login looks to be successful");
                    br.getPage("/");
                }
                if (!isLoggedin(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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

    private boolean isLoggedin(final Browser br) {
        if (br.containsHTML("href=\"/abo-shop\"[^>]*>\\s*Mein Abo\\s*<") || br.containsHTML("href=\"/login\"[^>]*>\\s*Abmelden\\s*</a>")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (!br.getURL().contains("/abo-shop")) {
            br.getPage("/abo-shop");
        }
        ai.setUnlimitedTraffic();
        String packageName = br.getRegex("(?i)class=\"subscription-details-name\"[^>]*>\\s*<b>([^<]+)</b>").getMatch(0);
        if (packageName != null) {
            packageName = Encoding.htmlDecode(packageName).trim();
        } else {
            packageName = "Unbekanntes Paket";
        }
        boolean automaticSubscription = false;
        String expireDateStr = br.getRegex("(?i)<b>\\s*Nächste Zahlung am\\s*:\\s*</b>\\s*(\\d{2}\\.\\d{2}\\.\\d{4})").getMatch(0);
        if (expireDateStr == null) {
            /* Expire-date when auto payment is not active. Newly created free accounts will also have this expire-date! */
            expireDateStr = br.getRegex("(?i)<b>\\s*Abo Laufzeit bis\\s*:\\s*</b>\\s*(\\d{2}\\.\\d{2}\\.\\d{4})").getMatch(0);
        }
        if (StringUtils.containsIgnoreCase(packageName, "free")) {
            account.setType(AccountType.FREE);
            /* TODO: Check if free users can download anything. */
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setTrafficLeft(0);
            automaticSubscription = br.containsHTML("(?i)Automatische Verlängerung:</b>\\s*Ja,\\s*automatisch abgebucht");
            ai.setUnlimitedTraffic();
        }
        if (expireDateStr != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDateStr, "dd.MM.yyyy", Locale.GERMANY));
        }
        ai.setStatus(packageName + " | Auto Verlängerung: " + (automaticSubscription ? "Ja" : "Nein"));
        return ai;
    }

    /** Checks for generic errors that can happen after any http request. */
    private void checkErrors(final Browser br, final DownloadLink link, final Account account) throws AccountUnavailableException {
        if (br.containsHTML("(?i)(Du siehst diese Seite, da YouTV\\.de deinen Aufruf blockierte|Die erwähnte Sicherheits-Blockade ist maximal|Zugriff verweigert\\s*<)")) {
            /*
             * This solely is an IP block. Changing the IP will fix this. Current/previous session cookies remain valid even if this
             * happens!
             */
            throw new AccountUnavailableException("IP gesperrt", 5 * 60 * 1000l);
        }
    }

    private void handleStoredErrors(final DownloadLink link) throws PluginException {
        final String lastStoredErrormessage = link.getStringProperty(PROPERTY_LAST_FILE_ERROR);
        if (lastStoredErrormessage == null) {
            /* No stored errormessage available */
            return;
        }
        final long ageOfStoredErrorMillis = System.currentTimeMillis() - link.getLongProperty(PROPERTY_TIMESTAMP_LAST_FILE_ERROR, 0);
        if (ageOfStoredErrorMillis < 5 * 60 * 1000l) {
            /*
             * Do not trust stored errormessage as it was stored too long ago -> Retry to get current data. Download will either work or
             * fail and in the latter case we can then trust the stored errormessage.
             */
            /* Remove directurl property to prevent infinite loop. */
            link.removeProperty(getDirecturlProperty(link));
            throw new PluginException(LinkStatus.ERROR_RETRY, "Retry to confirm error: " + lastStoredErrormessage);
        }
        if (lastStoredErrormessage.matches("(?i).*du hast heute auf viele Sendungen zugegriffen.*") || lastStoredErrormessage.matches("(?i).*Du hast das tägliche Limit von.*")) {
            /*
             * Wow, du hast heute auf viele Sendungen zugegriffen. Die private Nutzung muss gegeben sein. Du hast das tägliche Limit von 80
             * Sendungen überschritten. Bei Fragen melde dich unter support@youtv.de
             */
            final Date dateJustBeforeNextDay = new Date(System.currentTimeMillis());
            dateJustBeforeNextDay.setHours(23);
            dateJustBeforeNextDay.setMinutes(59);
            dateJustBeforeNextDay.setSeconds(29);
            final long timeUntilNextDayMillis = dateJustBeforeNextDay.getTime() - System.currentTimeMillis();
            throw new AccountUnavailableException("Tägliches Downloadlimit erreicht", timeUntilNextDayMillis + 30000);
        } else {
            /* Unknown error */
            throw new PluginException(LinkStatus.ERROR_FATAL, lastStoredErrormessage);
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        String dllink = getDirecturl(link);
        if (StringUtils.isEmpty(dllink)) {
            requestFileInformation(link, account);
            // handleStoredErrors(link);
            dllink = getDirecturl(link);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            this.login(account, false);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            checkErrors(br, link, account);
            handleStoredErrors(link);
            /* Force generation of new directurl next time */
            link.removeProperty(getDirecturlProperty(link));
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?", 1 * 60 * 1000l);
            }
        }
        /* Save last selected/used quality so we will try to resume that same one next time unless user resets this item in between. */
        link.setProperty(PROPERTY_DIRECTURL, PluginJsonConfig.get(YoutvDeConfig.class).getPreferredQuality().name());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        /* No captchas at all */
        return false;
    }

    @Override
    public Class<? extends YoutvDeConfig> getConfigInterface() {
        return YoutvDeConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.removeProperty(PROPERTY_LAST_USED_QUALITY);
            link.removeProperty(PROPERTY_TIMESTAMP_LAST_FILE_ERROR);
        }
    }
}