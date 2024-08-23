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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.plugins.components.config.YoutvDeConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.requests.DeleteRequest;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class YoutvDe extends PluginForHost {
    public YoutvDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.youtv.de/produkte");
    }

    public static final String PROPERTY_DIRECTURL                              = "directurl";
    public static final String PROPERTY_FILESIZE                               = "filesize";
    public static final String PROPERTY_RECORDED_STATUS                        = "recorded_status";
    public static final String PROPERTY_STARTS_AT                              = "starts_at";
    public static final String PROPERTY_LAST_FILE_ERROR                        = "last_file_error";
    public static final String PROPERTY_TIMESTAMP_LAST_FILE_ERROR              = "timestamp_last_file_error";
    public static final String PROPERTY_LAST_USED_QUALITY                      = "last_used_quality";
    public static final String PROPERTY_LAST_FORCE_CHOSEN_QUALITY_IN_LINKCHECK = "last_force_chosen_quality_in_linkcheck";
    public static final String WEBAPI_BASE                                     = "https://www.youtv.de/api/v2";

    @Override
    public String getAGBLink() {
        return "https://help.youtv.de/hc/de/articles/360029117431-AGB";
    }

    public static List<String[]> getPluginDomains() {
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

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
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
        if (link != null && StringUtils.equals(getHost(), link.getHost())) {
            return getHost() + "://" + getFID(link);
        } else {
            return super.getMirrorID(link);
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
        final String fid = urlinfo.getMatch(0);
        if (!link.isNameSet()) {
            final String slug = urlinfo.getMatch(1);
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
        br.getPage(WEBAPI_BASE + "/recordings/" + fid + ".json");
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        checkErrors(br, link, account);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> recording = (Map<String, Object>) entries.get("recording");
        parseFileInformation(link, recording);
        return AvailableStatus.TRUE;
    }

    public void parseFileInformation(final DownloadLink link, final Map<String, Object> recording) throws Exception {
        final String preferredQualityStr = getPreferredQualityStr(link);
        final List<Map<String, Object>> files = (List<Map<String, Object>>) recording.get("files");
        if (files != null && files.size() > 0) {
            Map<String, Object> chosenQuality = null;
            for (final Map<String, Object> quality : files) {
                final String qualityStr = quality.get("quality").toString();
                if (qualityStr.equals(preferredQualityStr)) {
                    chosenQuality = quality;
                    break;
                }
            }
            boolean qualityWasAutoChosen = false;
            if (chosenQuality == null) {
                logger.info("Failed to find preferred quality -> Fallback to first/best");
                chosenQuality = files.get(0);
                qualityWasAutoChosen = true;
            }
            final String chosenQualityStr = chosenQuality.get("quality").toString();
            final String description = (String) recording.get("long_text");
            if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
                link.setComment(description);
            }
            link.setProperty(PROPERTY_DIRECTURL + "_" + chosenQualityStr, chosenQuality.get("file"));
            link.setProperty(PROPERTY_FILESIZE + "_" + chosenQualityStr, chosenQuality.get("size"));
            if (qualityWasAutoChosen) {
                link.setProperty(PROPERTY_LAST_FORCE_CHOSEN_QUALITY_IN_LINKCHECK, chosenQualityStr);
            } else {
                link.removeProperty(PROPERTY_LAST_FORCE_CHOSEN_QUALITY_IN_LINKCHECK);
            }
        }
        link.setProperty(PROPERTY_RECORDED_STATUS, recording.get("recorded"));
        link.setProperty(PROPERTY_STARTS_AT, recording.get("starts_at"));
        setFilenameAndSize(link);
        if (link.getFinalFileName() == null) {
            /* No downloadurl given yet -> Build filename here */
            final List<String> recorded_qualities = (List<String>) recording.get("recorded_qualities");
            String qualityStringForWeakFilenames = null;
            if (recorded_qualities != null && recorded_qualities.size() > 0) {
                for (final String qualityStr : recorded_qualities) {
                    if (qualityStr.equals(preferredQualityStr)) {
                        qualityStringForWeakFilenames = qualityStr;
                        break;
                    }
                }
            }
            if (qualityStringForWeakFilenames == null) {
                /* Fallback */
                qualityStringForWeakFilenames = preferredQualityStr;
            }
            final Number season_number = (Number) recording.get("series_season");
            final Number episode_number = (Number) recording.get("series_number");
            final String title = recording.get("title").toString();
            final String subtitle = (String) recording.get("subtitle");
            String filename = title;
            if (season_number != null && episode_number != null) {
                filename += "_S" + season_number + "E" + episode_number;
            } else if (episode_number != null) {
                filename += "_E" + episode_number;
            }
            if (!StringUtils.isEmpty(subtitle)) {
                filename += "_" + subtitle;
            }
            filename += "_" + qualityStringForWeakFilenames;
            filename += ".mp4";
            link.setName(filename);
        }
        final List<String> file_errors = (List<String>) recording.get("file_errors");
        if (file_errors != null && file_errors.size() > 0) {
            /* Typically daily downloadlimit reached */
            link.setProperty(PROPERTY_LAST_FILE_ERROR, file_errors.get(0));
            link.setProperty(PROPERTY_TIMESTAMP_LAST_FILE_ERROR, System.currentTimeMillis());
        } else {
            link.removeProperty(PROPERTY_LAST_FILE_ERROR);
            link.removeProperty(PROPERTY_TIMESTAMP_LAST_FILE_ERROR);
        }
    }

    public void setFilenameAndSize(final DownloadLink link) {
        final String qualityStr = getPreferredQualityStr(link);
        final String directurl = getDirecturl(link);
        if (directurl != null) {
            try {
                final String urlWithoutParams = URLHelper.getUrlWithoutParams(directurl);
                final String originalFilename = new Regex(urlWithoutParams, "/([^/]+)$").getMatch(0);
                if (originalFilename != null) {
                    link.setFinalFileName(originalFilename);
                }
            } catch (final MalformedURLException ignore) {
                /* This should never happen */
                logger.warning("Bad final downloadurl");
            }
        }
        link.setVerifiedFileSize(link.getLongProperty(PROPERTY_FILESIZE + "_" + qualityStr, -1));
    }

    public static String getPreferredQualityStr(final DownloadLink link) {
        /* Prefer last used quality if existent. */
        String qualityStr = link.getStringProperty(PROPERTY_LAST_USED_QUALITY);
        if (qualityStr == null) {
            /*
             * Last quality which was force set during linkcheck -> This means that the user preferred quality was not available last time.
             */
            qualityStr = link.getStringProperty(PROPERTY_LAST_FORCE_CHOSEN_QUALITY_IN_LINKCHECK);
        }
        if (qualityStr == null) {
            qualityStr = PluginJsonConfig.get(YoutvDeConfig.class).getPreferredQuality().name();
        }
        return qualityStr.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        /* Account required to download any content of this website. */
        throw new AccountRequiredException();
    }

    public static String getDirecturl(final DownloadLink link) {
        return link.getStringProperty(getDirecturlProperty(link));
    }

    public static String getDirecturlProperty(final DownloadLink link) {
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

    public void login(final Account account, final boolean force) throws Exception {
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
                        return;
                    }
                    br.getPage("https://www." + this.getHost() + "/abo-shop");
                    checkErrors(br, null, account);
                    if (this.isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
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
                    throw new AccountInvalidException();
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
        if (!br.getURL().endsWith("/abo-shop")) {
            br.getPage("/abo-shop");
        }
        ai.setUnlimitedTraffic();
        String packageName = br.getRegex("(?i)class=\"subscription-details-name\"[^>]*>\\s*<b>([^<]+)</b>").getMatch(0);
        if (packageName != null) {
            packageName = Encoding.htmlDecode(packageName).trim();
        } else {
            packageName = "Unbekanntes Paket";
        }
        boolean autoSubscription = false;
        String expireDateStr = br.getRegex("(?i)<b>\\s*Nächste Zahlung am\\s*:\\s*</b>\\s*(\\d{2}\\.\\d{2}\\.\\d{4})").getMatch(0);
        if (expireDateStr == null) {
            /* Expire-date when auto payment is not active. Newly created free accounts will also have this expire-date! */
            expireDateStr = br.getRegex("(?i)<b>\\s*Abo Laufzeit bis\\s*:\\s*</b>\\s*(\\d{2}\\.\\d{2}\\.\\d{4})").getMatch(0);
        }
        if (StringUtils.containsIgnoreCase(packageName, "free")) {
            account.setType(AccountType.FREE);
        } else {
            account.setType(AccountType.PREMIUM);
            autoSubscription = br.containsHTML("(?i)Automatische Verlängerung:</b>\\s*Ja,\\s*automatisch abgebucht");
        }
        /*
         * 2022-12-23: All accounts, even free accounts can download without any sort of traffic limit (except this "fair use" limit of 80
         * downloads per day).
         */
        ai.setUnlimitedTraffic();
        if (expireDateStr != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDateStr, "dd.MM.yyyy", Locale.GERMANY));
        }
        ai.setStatus(packageName + " | Auto Verlängerung: " + (autoSubscription ? "Ja" : "Nein"));
        return ai;
    }

    /** Checks for generic errors that can happen after any http request. */
    private void checkErrors(final Browser br, final DownloadLink link, final Account account) throws AccountUnavailableException {
        if (br.containsHTML("(?i)(Du siehst diese Seite, da YouTV\\.de deinen Aufruf blockierte|Die erwähnte Sicherheits-Blockade ist maximal|Zugriff verweigert\\s*<)")) {
            /**
             * This solely is an IP block. Changing the IP will fix this. </br>
             * Current/previous session cookies remain valid even if this happens!
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
            logger.info("Stored errormessage is too old -> Trigger retry and throw exception with the following error-text if this is still the case: " + lastStoredErrormessage);
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
        setFilenameAndSize(link);
        String dllink = getDirecturl(link);
        if (StringUtils.isEmpty(dllink)) {
            requestFileInformation(link, account);
            // handleStoredErrors(link);
            dllink = getDirecturl(link);
            if (dllink == null) {
                if (link.getBooleanProperty(PROPERTY_RECORDED_STATUS, true) == false) {
                    /* Rare case: Broadcast hasn't aired yet -> Wait until it airs to be able to download it. */
                    final long minimumWaittime = 10 * 60 * 1000l;
                    long timeUntilRecordingStart = 0;
                    final String starts_at = link.getStringProperty(PROPERTY_STARTS_AT);
                    if (starts_at != null) {
                        final long startsAtTimestamp = TimeFormatter.getMilliSeconds(starts_at, "yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.GERMANY);
                        timeUntilRecordingStart = startsAtTimestamp - System.currentTimeMillis();
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Warte auf Aufnahme dieser Sendung", Math.max(timeUntilRecordingStart, minimumWaittime));
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } else {
            this.login(account, false);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            checkErrors(br, link, account);
            handleStoredErrors(link);
            /* Force generation of new directurl on next try. */
            link.removeProperty(getDirecturlProperty(link));
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 30 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken media?", 1 * 60 * 1000l);
            }
        }
        /* Save last selected/used quality so we will try to resume that same one next time unless user resets this item in between. */
        final YoutvDeConfig cfg = PluginJsonConfig.get(YoutvDeConfig.class);
        link.setProperty(PROPERTY_DIRECTURL, cfg.getPreferredQuality().name());
        if (dl.startDownload() && cfg.isDeleteRecordingsAfterDownload()) {
            logger.info("Trying to delete recording after download...");
            try {
                final DeleteRequest req = new DeleteRequest(WEBAPI_BASE + "/recordings/" + this.getFID(link) + ".json");
                final Browser brc = br.cloneBrowser();
                brc.getPage(req);
                if (brc.getHttpConnection().getResponseCode() == 200) {
                    logger.info("Successfully deleted recording after download");
                } else {
                    logger.warning("Failed to delete recording after download");
                }
            } catch (final Throwable ignore) {
                logger.log(ignore);
                logger.warning("Failed to delete recording after download");
            }
        }
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
            link.removeProperty(PROPERTY_LAST_FORCE_CHOSEN_QUALITY_IN_LINKCHECK);
            link.removeProperty(PROPERTY_LAST_FILE_ERROR);
            link.removeProperty(PROPERTY_TIMESTAMP_LAST_FILE_ERROR);
        }
    }
}