//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "save.tv" }, urls = { "https?://(?:www\\.)?save\\.tv/STV/M/obj/(?:archive/VideoArchiveDetails|archive/VideoArchiveStreaming|TC/SendungsDetails)\\.cfm\\?TelecastID=\\d+(?:\\&adsfree=(?:true|false|unset))?(?:\\&preferformat=[0-9])?|https?://[A-Za-z0-9\\-]+\\.save\\.tv/\\d+_\\d+_.+" })
public class SaveTv extends PluginForHost {
    /* Static information */
    /* API functions developed for API version 3.0.0.1631 */
    // private static final String API_APP_NAME = "JDownloader";
    private static final String   API_PUBLIC_KEY                               = "2005d51304a04a6398bfbc1f3c2a1c8b";
    private static final String   API_SECRET_KEY                               = "3711128ae57644e9a6278adda57a85de457a257fc3ca4130ab5ac863940923be";
    public static final String    HOST_STATIC                                  = "save.tv";
    /* Linktypes */
    public static final String    LINKTYPE_TELECAST_ID                         = ".+/STV/M/obj/archive/VideoArchiveDetails\\.cfm\\?TelecastID=\\d+";
    /*
     * User has programmed something but it has not aired yet (is not downloadable yet) OR it is offline for a long time already
     */
    public static final String    LINKTYPE_TELECAST_ID_RECORD_OVERVIEW         = ".+/STV/M/obj/TC/SendungsDetails\\.cfm\\?TelecastID=\\d+";
    public static final String    LINKTYPE_TELECAST_ID_VIDEO_ARCHIVE_STREAMING = ".+/STV/M/obj/archive/VideoArchiveStreaming\\.cfm\\?TelecastID=\\d+";
    public static final String    LINKTYPE_DIRECT                              = "https?://[A-Za-z0-9\\-]+\\.save\\.tv/\\d+_\\d+_.+";
    /* API static information */
    public static final String    API_BASE                                     = "https://api.save.tv/v3";
    public static final String    API_BASE_AUTH                                = "https://auth.save.tv/";
    public static final double    QUALITY_HD_MB_PER_SECOND                     = 0.3735593220338983;
    public static final double    QUALITY_H264_NORMAL_MB_PER_SECOND            = 0.1944268524382521;
    public static final double    QUALITY_H264_MOBILE_MB_PER_SECOND            = 0.0740975300823306;
    /* Properties */
    private static final String   CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS          = "CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS";
    private static final String   CRAWLER_PROPERTY_LASTCRAWL                   = "CRAWLER_PROPERTY_LASTCRAWL";
    /* Frequently used internal plugin properties */
    public static final String    PROPERTY_ACCOUNT_API_SESSIONID               = "sessionid";
    public static final String    PROPERTY_quality                             = "quality";
    public static final String    PROPERTY_plainfilename                       = "plainfilename";
    public static final String    PROPERTY_server_filename                     = "server_filename";
    public static final String    PROPERTY_acc_username                        = "acc_username";
    public static final String    PROPERTY_ad_free                             = "ad_free";
    public static final String    PROPERTY_producecountry                      = "producecountry";
    public static final String    PROPERTY_genre                               = "genre";
    public static final String    PROPERTY_type                                = "type";
    public static final String    PROPERTY_produceyear                         = "produceyear";
    public static final String    PROPERTY_plain_tv_station                    = "plain_tv_station";
    public static final String    PROPERTY_plain_site_category                 = "plain_site_category";
    public static final String    PROPERTY_episodename                         = "episodename";
    public static final String    PROPERTY_originaldate                        = "originaldate";
    public static final String    PROPERTY_episodenumber                       = "episodenumber";
    public static final String    PROPERTY_seasonnumber                        = "seasonnumber";
    public static final String    PROPERTY_acc_count_telecast_ids              = "acc_count_telecast_ids";
    public static final String    PROPERTY_acc_type                            = "acc_type";
    public static final String    PROPERTY_acc_count_archive_entries           = "acc_count_archive_entries";
    public static final String    PROPERTY_category                            = "category";
    public static final String    PROPERTY_stv_randomnumber                    = "stv_randomnumber";
    public static final String    PROPERTY_originaldate_end                    = "originaldate_end";
    public static final String    PROPERTY_site_runtime_seconds_withads        = "site_runtime_minutes";
    public static final String    PROPERTY_site_runtime_seconds_adsfree        = "site_runtime_minutes_adsfree";
    public static final String    PROPERTY_acc_expire                          = "acc_expire";
    public static final String    PROPERTY_acc_package                         = "acc_package";
    public static final String    PROPERTY_acc_price                           = "acc_price";
    public static final String    PROPERTY_acc_runtime                         = "acc_runtime";
    public static final String    PROPERTY_has_moved                           = "has_moved";
    public static final String    PROPERTY_downloadable_via                    = "downloadable_via";
    public static final String    PROPERTY_refresh_token                       = "refresh_token";
    public static final String    PROPERTY_expires_in                          = "expires_in";
    /* Settings stuff */
    private static final String   PROPERTY_USEORIGINALFILENAME                 = "USEORIGINALFILENAME";
    public static final String    PROPERTY_PREFERADSFREE                       = "PREFERADSFREE";
    private static final String   ADS_FREE_UNAVAILABLE_HOURS                   = "DOWNLOADONLYADSFREE_RETRY_HOURS_2";
    private final static String   SELECTED_VIDEO_FORMAT                        = "selected_video_format";
    /* Text strings displayed to the user in various cases */
    private final static String   USERTEXT_NOCUTAVAILABLE                      = "Für diese Sendung steht (noch) keine Schnittliste zur Verfügung";
    /* The list of qualities/formats displayed to the user */
    private static final String[] FORMATS                                      = new String[] { "HD", "H.264 HQ", "H.264 MOBILE" };
    /* Crawler settings */
    private static final String   CRAWLER_ONLY_ADD_NEW_IDS                     = "CRAWLER_ONLY_ADD_NEW_IDS";
    private static final String   ACTIVATE_BETA_FEATURES                       = "ACTIVATE_BETA_FEATURES";
    private static final String   USEAPI                                       = "USEAPI_2017_10_02";
    private static final String   CRAWLER_ACTIVATE                             = "CRAWLER_ACTIVATE";
    public static final String    CRAWLER_ENABLE_FAST_LINKCHECK                = "CRAWLER_ENABLE_FASTER_2";
    public static final String    CRAWLER_ENABLE_DIALOGS                       = "CRAWLER_ENABLE_DIALOGS";
    public static final String    CRAWLER_GRAB_TIMEFRAME_COUNT                 = "CRAWLER_GRAB_TIMEFRAME_COUNT";
    private static final String   DELETE_TELECAST_ID_AFTER_DOWNLOAD            = "DELETE_TELECAST_ID_AFTER_DOWNLOAD";
    private static final String   DELETE_TELECAST_ID_IF_FILE_ALREADY_EXISTS    = "DELETE_TELECAST_ID_IF_FILE_ALREADY_EXISTS";
    /* Custom filename settings stuff */
    private static final String   CUSTOM_DATE                                  = "CUSTOM_DATE";
    private static final String   CUSTOM_FILENAME_MOVIES                       = "CUSTOM_FILENAME_MOVIES";
    private static final String   CUSTOM_FILENAME_SERIES                       = "CUSTOM_FILENAME_SERIES_2017_10_02";
    public static final String    CUSTOM_API_PARAMETERS_CRAWLER                = "CUSTOM_API_PARAMETERS_CRAWLER";
    private static final String   CUSTOM_FILENAME_SEPERATION_MARK              = "CUSTOM_FILENAME_SEPERATION_MARK";
    private static final String   CUSTOM_FILENAME_EMPTY_TAG_STRING             = "CUSTOM_FILENAME_EMPTY_TAG_STRING";
    private static final String   FORCE_ORIGINALFILENAME_SERIES                = "FORCE_ORIGINALFILENAME_SERIES";
    private static final String   FORCE_ORIGINALFILENAME_MOVIES                = "FORCE_ORIGINALFILENAME_MOVIES";
    /* Download connections constants */
    private static final boolean  ACCOUNT_PREMIUM_RESUME                       = true;
    private static final int      ACCOUNT_PREMIUM_MAXCHUNKS                    = -2;
    private static final int      ACCOUNT_PREMIUM_MAXDOWNLOADS                 = -1;
    private static final int      FREE_MAXDOWNLOADS                            = -1;
    /* Other API/site errorhandling constants */
    public static final String    URL_LOGGED_OUT                               = "Token=MSG_LOGOUT_B";
    /* Property / Filename constants / States / Small user display texts */
    public static final String    STATE_QUALITY_LQ                             = "LQ";
    public static final String    STATE_QUALITY_HQ                             = "HQ";
    public static final String    STATE_QUALITY_HD                             = "HD";
    public static final String    STATE_QUALITY_UNKNOWN                        = "XX";
    public static final String    STATE_ad_free_true                           = "true";
    public static final String    STATE_ad_free_false                          = "false";
    public static final String    STATE_ad_free_unknown                        = "XX";
    public static final String    EXTENSION_default                            = ".mp4";
    /* Save.tv internal quality/format constants (IDs) for the API & website */
    private static final int      SITE_FORMAT_HD                               = 6;
    private static final int      SITE_FORMAT_HQ                               = 5;
    private static final int      SITE_FORMAT_LQ                               = 4;
    /* Other */
    public static Object          LOCK                                         = new Object();
    public static Object          LOCK_login                                   = new Object();
    private Account               currAcc                                      = null;
    private DownloadLink          currDownloadlink                             = null;
    private SubConfiguration      cfg                                          = null;

    @SuppressWarnings("deprecation")
    public SaveTv(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.save.tv/stv/s/obj/registration/RegPage1.cfm");
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setContentUrl(buildExternalDownloadURL(link, this));
    }

    @Override
    public String getAGBLink() {
        return "http://www.save.tv/STV/S/misc/terms.cfm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @SuppressWarnings("deprecation")
    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadlink = dl;
        this.cfg = this.getPluginConfig();
    }

    public static String getAPIClientID() {
        return API_PUBLIC_KEY;
    }

    public static String getAPISecretKey() {
        return API_SECRET_KEY;
    }

    /**
     * @property "category": 0 = undefined, 1 = movies,category: 2 = series, 3 = show, 7 = music
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        br.setFollowRedirects(true);
        link.setProperty(PROPERTY_type, EXTENSION_default);
        /* Show telecast-ID + extension as dummy name for all error cases */
        final String telecast_ID = getTelecastId(link);
        if (telecast_ID == null) {
            /* This should never happen! */
            logger.warning("telecastID is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (link.getName() != null && (link.getName().contains(telecast_ID) && !link.getName().endsWith(EXTENSION_default) || link.getName().contains(".cfm"))) {
            link.setName(telecast_ID + EXTENSION_default);
        }
        Account account = null;
        final String account_username_via_which_url_is_downloadable = getDownloadableVia(link);
        final ArrayList<Account> all_stv_accounts = AccountController.getInstance().getValidAccounts(this.getHost());
        if (all_stv_accounts == null || all_stv_accounts.size() == 0) {
            link.getLinkStatus().setStatusText("Kann Links ohne güntigen und dazugehörigen Account nicht überprüfen");
            checkAccountNeededDialog();
            return AvailableStatus.UNCHECKABLE;
        } else if (account_username_via_which_url_is_downloadable == null && all_stv_accounts.size() == 1) {
            /* User probably added save.tv urls manually and has only one account --> Allow these to be downloaded via this account! */
            account = all_stv_accounts.get(0);
            link.setProperty(PROPERTY_downloadable_via, account.getUser());
        } else {
            /* Find account via which we can download our url. */
            for (final Account aatemp : all_stv_accounts) {
                if (this.canHandle(link, aatemp)) {
                    account = aatemp;
                    break;
                }
            }
        }
        /* Set linkID for correct dupe-check as telecastID is bound to account! */
        if (link.getLinkID() == null || (link.getLinkID() != null && !link.getLinkID().matches("\\d+"))) {
            /* Every account has individual telecastIDs, only downloadable via this account. */
            link.setLinkID(account.getUser() + telecast_ID);
        }
        setConstants(account, link);
        final AvailableStatus availablestatus;
        if (is_API_enabled(this.getHost())) {
            availablestatus = requestFileInformationAPI(link, account);
        } else {
            availablestatus = requestFileInformationWebsite(link, account);
        }
        link.setAvailable(true);
        final String availablecheck_filename = getFilename(this, link);
        /*
         * Reset (final) filename from previous state so we can use the final filename as final filename later even if it has changed before
         */
        link.setFinalFileName(null);
        link.setName(null);
        link.setName(availablecheck_filename);
        return availablestatus;
    }

    @SuppressWarnings({ "unchecked" })
    private AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account) throws Exception {
        login_website(this.br, account, false);
        getPageSafe("https://www." + this.getHost() + "/STV/M/obj/archive/JSON/VideoArchiveDetailsApi.cfm?TelecastID=" + getTelecastId(link), account);
        if (!br.getURL().contains("/JSON/") || this.br.getHttpConnection().getResponseCode() == 404) {
            /* Offline#1 - offline */
            return AvailableStatus.FALSE;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final Object aRRALLOWDDOWNLOADFORMATS = entries.get("ARRALLOWDDOWNLOADFORMATS");
        final Object sTRRECORDORDER = entries.get("STRRECORDORDER");
        if (aRRALLOWDDOWNLOADFORMATS == null || sTRRECORDORDER == null) {
            /*
             * Offline#2 - expired (download not possible anymore - if user tries to download something that has not been recorded yet, code
             * will NOT jump into this as json will already contain some download information / these two important json objects)
             */
            return AvailableStatus.FALSE;
        }
        final ArrayList<Object> sourcelist = jsonGetVideoSourcelist(entries);
        entries = (LinkedHashMap<String, Object>) entries.get("TELECASTDETAILS");
        parseFilenameInformation_site(link, entries);
        parseQualityTagWebsite(link, sourcelist);
        link.setAvailable(true);
        return AvailableStatus.TRUE;
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account) throws Exception {
        login_api(this.br, account, false);
        /* Let's assume that all URLs the user adds are telecastIDs which have already been recorded. */
        /* TODO: Maybe add erorhandling based on recordStateID, see: https://api.save.tv:443/v3/recordstates */
        final String telecastID = getTelecastId(link);
        ArrayList<Object> qualityList = null;
        LinkedHashMap<String, Object> entries = null;
        boolean existsRecord = false;
        if (isTypeTelecastIDOverview(link)) {
            /* telecast info --> If available, find record info */
            callAPITelecastsSingle(telecastID);
            /* Object could be offline here but that does not matter, we will check for this later! */
            try {
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                existsRecord = ((Boolean) entries.get("existsRecord")).booleanValue();
            } catch (final Throwable e) {
                /* On offline, we might get a Map instead of LinkedHashMap here */
            }
            if (existsRecord) {
                /* Item downloadable --> Find quality list */
                logger.info("Assumed not-yet-recorded telecastID is recorded and downloadable");
                callAPIRecordsSingle(telecastID);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                /* Set current correct downloadurl so that on next linkcheck, we can request the record information right away. */
                link.setUrlDownload(buildArchiveDownloadURL(link));
            }
        } else {
            /* record info --> If NOT available, find telecast info */
            callAPIRecordsSingle(telecastID);
            if (isOfflineAPI(this.br)) {
                /* Item not downloadable --> At least try to get general information about this ID */
                logger.info("Failed to find record --> Checking if maybe it hasn't been recorded yet or is too old (offline)");
                callAPITelecastsSingle(telecastID);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            } else {
                /* Item downloadable --> Find quality list */
                existsRecord = true;
                /*
                 * Only parse json if we know that the telecastID is not offline because else we might get an Exception as we get a Map
                 * instead of the expected LinkedHashMap!
                 */
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            }
        }
        if (isOfflineAPI(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (existsRecord) {
            qualityList = jsonGetFormatArrayAPI(entries);
        }
        parseFilenameInformation_api(link, entries, existsRecord);
        parseQualityTagAPI(link, qualityList);
        if (!existsRecord && link.getLongProperty(PROPERTY_originaldate_end, 0) < System.currentTimeMillis()) {
            /*
             * TODO: Maybe make this boolean public to make SURE that we do never try to download such URLs as we KNOW that they are NOT
             * downloadable.
             */
            logger.info("This telecastID has probably been deleted a long time ago --> Found information but it is offline nevertheless");
            /*
             * As a final attempt to provide a working example URL for the user, let's set the 'overview-URL' to make sure whenever he
             * copies the URL he will have a URL which actually shows information in browser. We could also verify this status via
             * TELECAST_ID_EXPIRE_TIME but it leaves room for errors open so let's assume that this is a definite offline case!
             */
            link.setContentUrl(buildNotYetRecordedOrOfflineDownloadURL(link));
            return AvailableStatus.FALSE;
        } else if (!existsRecord && !allowNonProgrammedTelecastIDs()) {
            logger.info("telecastID exists but has not been programmed to record AND non programmed telecastIDs are not allowed at the moment --> Display as offline");
            return AvailableStatus.FALSE;
        } else if (!existsRecord) {
            logger.info("telecastID is online but has not not been programmed by the user --> NOT downloadable at this stage");
        }
        return AvailableStatus.TRUE;
    }

    private void callAPITelecastsSingle(final String telecastID) throws Exception {
        api_GET(this.br, "/telecasts/" + telecastID + "?fields=country%2C%20description%2C%20enddate%2C%20episode%2C%20existsrecord%2C%20id%2C%20startdate%2C%20subject%2C%20subtitle%2C%20title%2C%20tvcategory.id%2C%20tvcategory.name%2C%20tvstation.id%2C%20tvstation.name%2C%20tvsubcategory.id%2C%20tvsubcategory.name%2C%20year");
    }

    private void callAPIRecordsSingle(final String telecastID) throws Exception {
        api_GET(this.br, "/records/" + telecastID + "?fields=" + getRecordsFieldsValue());
    }

    public static String getRecordsFieldsValue() {
        return "adfreeavailable%2C%20adfreelength%2C%20createdate%2C%20telecast.hasmoved%2C%20enddate%2C%20formats%2C%20formats.recordformat.id%2C%20formats.recordformat.name%2C%20formats.recordstate.id%2C%20formats.recordstate.name%2C%20formats.retentiondate%2C%20formats.uncutvideosize%2C%20isadcutenabled%2C%20startdate%2C%20telecast.country%2C%20telecast.description%2C%20telecast.enddate%2C%20telecast.episode%2C%20telecast.id%2C%20telecast.startdate%2C%20telecast.subject%2C%20telecast.subtitle%2C%20telecast.title%2C%20telecast.tvcategory.id%2C%20telecast.tvcategory.name%2C%20telecast.tvstation.id%2C%20telecast.tvstation.name%2C%20telecast.tvsubcategory.id%2C%20telecast.tvsubcategory.name%2C%20telecast.year%2C%20telecastid";
    }

    private boolean isOfflineAPI(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    @SuppressWarnings("deprecation")
    public static String getFilename(final Plugin plugin, final DownloadLink dl) throws ParseException {
        /*
         * No custom filename if not all required tags are given, if the user prefers original filenames or if custom user regexes for
         * specified series or movies match to force original filenames
         */
        final SubConfiguration cfg = SubConfiguration.getConfig(plugin.getHost());
        final boolean force_original_general = (cfg.getBooleanProperty(PROPERTY_USEORIGINALFILENAME) || dl.getLongProperty(PROPERTY_category, 0) == 0);
        final String site_title = dl.getStringProperty(PROPERTY_plainfilename);
        final String server_filename = dl.getStringProperty(PROPERTY_server_filename, null);
        final String fake_original_filename = getFakeOriginalFilename(plugin, dl);
        boolean force_original_series = false;
        boolean force_original_movies = false;
        String formattedFilename;
        if (isSeries(dl)) {
            formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_SERIES, defaultCustomSeriesFilename);
            try {
                if (site_title.matches(cfg.getStringProperty(FORCE_ORIGINALFILENAME_SERIES, null))) {
                    force_original_series = true;
                }
            } catch (final Throwable e) {
                System.out.println("FORCE_ORIGINALFILENAME_SERIES custom regex failed");
            }
        } else {
            formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_MOVIES, defaultCustomFilenameMovies);
            try {
                if (site_title.matches(cfg.getStringProperty(FORCE_ORIGINALFILENAME_MOVIES, null))) {
                    force_original_movies = true;
                }
            } catch (final Throwable e) {
                System.out.println("FORCE_ORIGINALFILENAME_MOVIES custom regex failed");
            }
        }
        /* If user wants to use the original server filename in a custom filename we need to have it present here - if not, we fake it */
        final boolean force_original_original_missing = (formattedFilename.contains("*server_dateiname*") && server_filename == null);
        if (force_original_original_missing) {
            dl.setProperty(PROPERTY_server_filename, fake_original_filename.substring(0, fake_original_filename.lastIndexOf(".")));
        }
        final boolean force_original_filename = (force_original_general || force_original_series || force_original_movies);
        String filename;
        if (force_original_filename) {
            filename = fake_original_filename;
        } else {
            filename = getFormattedFilename(plugin, dl);
        }
        /* Cut filenames for Windows systems if necessary */
        if (CrossSystem.isWindows() && filename.length() > 255) {
            filename = filename.replace(EXTENSION_default, "");
            if (filename.length() >= 251) {
                filename = filename.substring(0, 250) + EXTENSION_default;
            } else {
                filename += EXTENSION_default;
            }
        }
        return filename;
    }

    /**
     * Get- and set the information, we later need for the custom filenames. <br />
     * Their json is crazy regarding data types thus we have a lot of type conversions here ...
     */
    public static void parseFilenameInformation_site(final DownloadLink dl, final LinkedHashMap<String, Object> sourcemap) throws PluginException {
        /*
         * Caution with data types - if e.g. a movie is named "1987" they will actually use a double- or long value - this is totally crazy
         * as everything can happen here. Imagine a movie is named "true" ...
         */
        final Object site_title_o = sourcemap.get("STITLE");
        final String site_title = jsonobject_to_string(site_title_o);
        long datemilliseconds = 0;
        /* For series only */
        final Object season_episode_information = sourcemap.get("SFOLGE");
        final Object episodename_o = sourcemap.get("SSUBTITLE");
        final String episodename = jsonobject_to_string(episodename_o);
        /* General */
        final String genre = (String) sourcemap.get("SCHAR");
        final String producecountry = (String) sourcemap.get("SCOUNTRY");
        final Object produceyear_o = sourcemap.get("SPRODUCTIONYEAR");
        final String produceyear;
        if (produceyear_o instanceof Double) {
            /* Yes - they acrtually return a YEAR as double value */
            produceyear = Long.toString((long) ((Double) produceyear_o).doubleValue());
        } else {
            /* In case they correct their horrible json we might as well get a long value --> Handle this too :) */
            produceyear = Long.toString(JavaScriptEngineFactory.toLong(produceyear_o, 0));
        }
        final int category = (int) JavaScriptEngineFactory.toLong(sourcemap.get("TVCATEGORYID"), -1);
        final String runtime_start = (String) sourcemap.get("DSTARTDATE");
        /* For hosterplugin */
        String runtime_end = (String) sourcemap.get("ENDDATE");
        /* For decrypterplugin */
        if (runtime_end == null) {
            runtime_end = (String) sourcemap.get("DENDDATE");
        }
        final long runtime_end_long = TimeFormatter.getMilliSeconds(runtime_end, "yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
        datemilliseconds = TimeFormatter.getMilliSeconds(runtime_start, "yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
        final int site_runtime_seconds = (int) ((runtime_end_long - datemilliseconds) / 1000);
        final boolean hasMoved = (site_title != null && site_title.matches(".*?kurzfristige Programmänderung.*?")) ? true : false;
        final String tv_station = (String) sourcemap.get("STVSTATIONNAME");
        /* Set properties which are needed for filenames */
        /* Add series information */
        setFilenameInformationSeasonnumberEpisodenumberUniversal(dl, season_episode_information);
        /* Sometimes episodetitle == episodenumber (double) --> Do NOT set it as episodetitle is NOT given in this case! */
        if (episodename != null && !episodename.matches("\\d+\\.\\d+")) {
            dl.setProperty(PROPERTY_episodename, correctData(dl.getHost(), episodename));
        }
        /* Add other information */
        if (!produceyear.equals("0")) {
            dl.setProperty(PROPERTY_produceyear, correctData(dl.getHost(), produceyear));
        }
        if (genre != null) {
            dl.setProperty(PROPERTY_genre, correctData(dl.getHost(), genre));
        }
        if (producecountry != null) {
            dl.setProperty(PROPERTY_producecountry, correctData(dl.getHost(), producecountry));
        }
        if (tv_station != null) {
            dl.setProperty(PROPERTY_plain_tv_station, correctData(dl.getHost(), tv_station));
        }
        parseCategoryID(dl, category, episodename);
        if (site_title != null) {
            dl.setProperty(PROPERTY_plainfilename, correctData(dl.getHost(), site_title));
        }
        dl.setProperty(PROPERTY_originaldate, datemilliseconds);
        dl.setProperty(PROPERTY_originaldate_end, runtime_end_long);
        dl.setProperty(PROPERTY_site_runtime_seconds_withads, site_runtime_seconds);
        dl.setProperty(PROPERTY_has_moved, hasMoved);
    }

    /**
     * Get- and set the information we later need for the custom filenames. <br />
     *
     * @param dl
     *            : Given DownloadLink
     * @param entries
     *            : Given json --> Java Map
     * @param hasToGrabTelecastMap
     *            : For downloadable telecastIDs we're not yet at the telecastMap which is why we have to grab it (this is set to true)!
     */
    @SuppressWarnings("unchecked")
    public static void parseFilenameInformation_api(final DownloadLink dl, LinkedHashMap<String, Object> entries, final boolean hasToGrabTelecastMap) throws PluginException {
        if (entries == null) {
            return;
        }
        /* First grab the only property (in this function) which is only available for finished 'records' which are downloadable. */
        final short isAdsFreeAvailable = jsonGetAdsFreeAvailableAPIDetailed(entries);
        final int runtime_seconds_adsfree = (int) JavaScriptEngineFactory.toLong(entries.get("adFreeLength"), -1);
        if (hasToGrabTelecastMap) {
            entries = (LinkedHashMap<String, Object>) entries.get("telecast");
            if (entries == null) {
                return;
            }
        }
        // ---------------------------------------------
        final String runtime_start = ((String) entries.get("startDate")).replace("Z", "+0000");
        final String runtime_end = ((String) entries.get("endDate")).replace("Z", "+0000");
        final long runtime_end_long = TimeFormatter.getMilliSeconds(runtime_end, "yyyy-MM-dd'T'HH:mm:ssZ", Locale.GERMANY);
        final long runtime_start_long = TimeFormatter.getMilliSeconds(runtime_start, "yyyy-MM-dd'T'HH:mm:ssZ", Locale.GERMANY);
        final int site_runtime_seconds_withads = (int) ((runtime_end_long - runtime_start_long) / 1000);
        // ---------------------------------------------
        final Object hasMovedO = entries.get("hasMoved");
        final boolean hasMoved = hasMovedO != null ? ((Boolean) hasMovedO).booleanValue() : false;
        final String site_title = (String) entries.get("title");
        /* For series only */
        final Object season_episode_information = entries.get("episode");
        final String episodename = (String) entries.get("subTitle");
        /* General */
        final String genre = (String) JavaScriptEngineFactory.walkJson(entries, "tvSubCategory/name");
        final String country = (String) entries.get("country");
        final int category = (int) JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "tvCategory/id"), -1);
        final String tv_station = (String) JavaScriptEngineFactory.walkJson(entries, "tvStation/name");
        /* Set properties which are needed for filenames */
        /* Add series information */
        setFilenameInformationSeasonnumberEpisodenumberUniversal(dl, season_episode_information);
        if (episodename != null) {
            dl.setProperty(PROPERTY_episodename, correctData(dl.getHost(), episodename));
        }
        /* Add other information */
        if (genre != null) {
            dl.setProperty(PROPERTY_genre, correctData(dl.getHost(), genre));
        }
        if (country != null) {
            dl.setProperty(PROPERTY_producecountry, correctData(dl.getHost(), country));
        }
        if (tv_station != null) {
            dl.setProperty(PROPERTY_plain_tv_station, correctData(dl.getHost(), tv_station));
        }
        parseCategoryID(dl, category, episodename);
        if (site_title != null) {
            /* This should actually never be null */
            dl.setProperty(PROPERTY_plainfilename, correctData(dl.getHost(), site_title));
        }
        /*
         * Set ad-free state on DownloadLink for e.g. usage in filename later.
         */
        if (isAdsFreeAvailable == 1) {
            dl.setProperty(PROPERTY_ad_free, STATE_ad_free_true);
        } else if (isAdsFreeAvailable == 0) {
            dl.setProperty(PROPERTY_ad_free, STATE_ad_free_false);
        } else {
            // Status unknown - do not set status at all
        }
        dl.setProperty(PROPERTY_originaldate, runtime_start_long);
        dl.setProperty(PROPERTY_originaldate_end, runtime_end_long);
        dl.setProperty(PROPERTY_site_runtime_seconds_withads, site_runtime_seconds_withads);
        if (runtime_seconds_adsfree > -1) {
            dl.setProperty(PROPERTY_site_runtime_seconds_adsfree, runtime_seconds_adsfree);
        }
        dl.setProperty(PROPERTY_has_moved, hasMoved);
    }

    public static void setFilenameInformationSeasonnumberEpisodenumberUniversal(final DownloadLink dl, final Object episodeO) {
        if (episodeO == null) {
            return;
        }
        if (episodeO instanceof Double) {
            /* Website may sometimes return episodenumber only ... as double! */
            dl.setProperty(PROPERTY_episodenumber, (int) ((Double) episodeO).doubleValue());
        } else if (episodeO instanceof String) {
            final String episodeInformation = episodeO instanceof String ? (String) episodeO : null;
            String episodenumber = null;
            String seasonnumber = null;
            if (episodeInformation != null && new Regex(episodeInformation, "^S\\d+E\\d+$").matches()) {
                final Regex seasonAndEpisode = new Regex(episodeInformation, "^S(\\d+)E(\\d+)$");
                episodenumber = seasonAndEpisode.getMatch(0);
                seasonnumber = seasonAndEpisode.getMatch(1);
            } else if (episodeInformation != null && episodeInformation.matches("\\d+")) {
                episodenumber = episodeInformation;
            }
            if (seasonnumber != null) {
                dl.setProperty(PROPERTY_seasonnumber, Integer.parseInt(seasonnumber));
            }
            if (episodenumber != null) {
                dl.setProperty(PROPERTY_episodenumber, Integer.parseInt(episodenumber));
            }
        } else {
            /* Set nothing */
            return;
        }
    }

    public static void parseCategoryID(final DownloadLink dl, int category, final String episodename) {
        /* Happens in decrypter - errorhandling! */
        final int episodenumber = getEpisodeNumber(dl);
        if (category == -1 && (episodename != null || episodenumber > -1)) {
            /* Force id for series if we are sure that our object belongs to that category! */
            category = 2;
        } else if (category == -1) {
            /* Fallback */
            category = 1;
        }
        switch (category) {
        /* 2 = series, 7 = music, 1 = movies, 6 = magazines */
        case 1:
            /* For movies and magazines */
            dl.setProperty(PROPERTY_category, 1);
            break;
        case 6:
            /* For movies and magazines */
            dl.setProperty(PROPERTY_category, 1);
            break;
        default:
            /* For everything else - same as movie but separated anyways */
            dl.setProperty(PROPERTY_category, 1);
        }
        dl.setProperty(PROPERTY_plain_site_category, category);
    }

    /** Sets available quality as PROPERTY_quality and sets filesize. */
    public static void parseQualityTagWebsite(final DownloadLink dl, final ArrayList<Object> sourcelist) {
        final int selected_video_format = getConfiguredVideoFormatID(dl);
        /*
         * If we have no source, we can select HQ if the user chose HQ because it is always available. If the user selects any other quality
         * we need to know whether it exists or not and then set the data.
         */
        final String final_quality;
        if (sourcelist == null) {
            /* No qualities given from website/API */
            final_quality = STATE_QUALITY_HQ;
        } else {
            final int quality_best = jsonGetBestQualityIdWebsite(sourcelist);
            final boolean isHDAvailable = quality_best == getBestFormatID();
            switch (selected_video_format) {
            case SITE_FORMAT_HD:
                if (isHDAvailable) {
                    final_quality = STATE_QUALITY_HD;
                } else {
                    final_quality = STATE_QUALITY_HQ;
                }
                break;
            case SITE_FORMAT_HQ:
                final_quality = STATE_QUALITY_HQ;
                break;
            case SITE_FORMAT_LQ:
                if (sourcelist.size() == 2) {
                    /* Mobile version available (should always be the case!) */
                    final_quality = STATE_QUALITY_LQ;
                } else {
                    final_quality = STATE_QUALITY_HQ;
                }
                break;
            default:
                final_quality = STATE_QUALITY_HQ;
            }
        }
        dl.setProperty(PROPERTY_quality, final_quality);
        dl.setDownloadSize(calculateFilesize(dl, final_quality));
    }

    /**
     * Sets available quality as PROPERTY_quality and sets filesize. <br />
     * Has fallback for all possible errorcases!
     */
    public static void parseQualityTagAPI(final DownloadLink dl, final ArrayList<Object> sourcelist) {
        final int selected_video_format = getConfiguredVideoFormatID(dl);
        /*
         * If we have no source, we can select HQ if the user chose HQ because it is always available. If the user selects any other quality
         * we need to know whether it exists or not and then set the data.
         */
        final String final_quality;
        if (sourcelist == null) {
            /* No qualities given from website/API */
            final_quality = STATE_QUALITY_HQ;
        } else {
            // final String quality_best = jsonGetBestQualityIdAPI(sourcelist);
            final boolean isHDAvailable = sourcelist.size() == 3;
            switch (selected_video_format) {
            case SITE_FORMAT_HD:
                if (isHDAvailable) {
                    final_quality = STATE_QUALITY_HD;
                } else {
                    final_quality = STATE_QUALITY_HQ;
                }
                break;
            case SITE_FORMAT_HQ:
                final_quality = STATE_QUALITY_HQ;
                break;
            case SITE_FORMAT_LQ:
                if (sourcelist.size() == 2) {
                    /* Mobile version available (should always be the case!) */
                    final_quality = STATE_QUALITY_LQ;
                } else {
                    final_quality = STATE_QUALITY_HQ;
                }
                break;
            default:
                final_quality = STATE_QUALITY_HQ;
            }
        }
        dl.setProperty(PROPERTY_quality, final_quality);
        /* Finally set downloadsize based on given format information. */
        dl.setDownloadSize(calculateFilesize(dl, final_quality));
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        synchronized (LOCK) {
            checkFeatureDialogAll();
            checkFeatureDialogCrawler();
            checkFeatureDialogNew();
        }
        requestFileInformation(link);
        setConstants(account, link);
        /* Check whether content has been recorded already or not! */
        final long runtime_end = link.getLongProperty(PROPERTY_originaldate_end, System.currentTimeMillis() + 1);
        final long released_since = System.currentTimeMillis() - runtime_end;
        if (released_since < 0) {
            /*
             * Content not yet recorded --> Show errormessage with waittime. Waittime = Releasedate(END-Timestamp of video) of content + 10
             * minutes! Most times Stv needs between 10 and 60 minutes to get the downloads ready.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Diese Sendung wurde noch nicht aufgenommen!", released_since * (-1) + 10 * 60 * 60 * 1000l);
        }
        if (this.apiActive()) {
            handlePremiumAPI(link, account);
        } else {
            handlePremiumWebsite(link, account);
        }
    }

    @SuppressWarnings({ "unchecked" })
    public void handlePremiumWebsite(final DownloadLink link, final Account account) throws Exception {
        boolean preferAdsFree = getPreferAdsFree(link);
        /* Check if ads-free version is available */
        final String ad_Free_availability = PluginJSonUtils.getJsonValue(br, "BADFREEAVAILABLE");
        final boolean isAdsFreeAvailable;
        if (!StringUtils.isEmpty(ad_Free_availability) && (ad_Free_availability.equals("1") || ad_Free_availability.equalsIgnoreCase("true"))) {
            /* Set ad-free state on DownloadLink for e.g. usage in filename later. */
            link.setProperty(PROPERTY_ad_free, STATE_ad_free_true);
            isAdsFreeAvailable = true;
        } else {
            /* ad_Free_availability == "2" */
            /* Set ad-free state on DownloadLink for e.g. usage in filename later. */
            link.setProperty(PROPERTY_ad_free, STATE_ad_free_false);
            isAdsFreeAvailable = false;
        }
        final boolean downloadAdsFreeValue = verifyAdsFreeUserSelection(link, preferAdsFree, isAdsFreeAvailable);
        /* Set download options (ads-free or with ads) and get download url */
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final ArrayList<Object> sourcelist = jsonGetVideoSourcelist(entries);
        final int best_quality_id = jsonGetBestQualityIdWebsite(sourcelist);
        int stv_request_selected_format_id_value = getConfiguredVideoFormatID(link);
        final boolean desired_format_is_available = jsonIsDesiredFormatAvailableWebsite(sourcelist, stv_request_selected_format_id_value);
        if (!desired_format_is_available) {
            logger.info("Desired format is not available - falling back to highest format/quality possible");
            stv_request_selected_format_id_value = best_quality_id;
        }
        String dllink = checkDirectLink(link, stv_request_selected_format_id_value, downloadAdsFreeValue);
        if (StringUtils.isEmpty(dllink)) {
            requestDownloadWebsite(link, stv_request_selected_format_id_value, downloadAdsFreeValue);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            /* 2016-07-06: Collecting errors: */
            /*
             * {"ERROR":
             * "Durch Anwendung der Schnittliste wurde der gesamte Inhalt der Aufnahme entfernt. Um den Inhalt ihrer Aufnahme zu betrachten, laden Sie bitte die ungeschnittene Version."
             * ,"TELECASTID":1.2579225E7}
             */
            final String error = (String) entries.get("ERROR");
            if (!inValidate(error)) {
                /* 2016-07-06: As long as we haven't collected *all* possible errors let's just have generic handling for all. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Fehler beim Downloadversuch: " + error, 10 * 60 * 1000l);
            }
            final boolean dlurl_success = ((Boolean) entries.get("SUCCESS")).booleanValue();
            dllink = (String) entries.get("DOWNLOADURL");
            if (StringUtils.isEmpty(dllink) && !dlurl_success) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download aus unbekannten Gründen zurzeit nicht möglich", 10 * 60 * 1000l);
            }
        }
        handleDownload(link, account, dllink);
    }

    @SuppressWarnings("unchecked")
    public void handlePremiumAPI(final DownloadLink link, final Account account) throws Exception {
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(this.br.toString());
        final boolean preferAdsFree = getPreferAdsFree(link);
        int formatIDselected = getConfiguredVideoFormatID(link);
        int formatIDFallback = getDefaultFormatID();
        int formatIDtemp;
        boolean selectedFormatIsAvailable = false;
        final boolean isAdsFreeAvailable = jsonGetAdsFreeAvailableAPI(entries);
        final boolean downloadAdsFreeValue = verifyAdsFreeUserSelection(link, preferAdsFree, isAdsFreeAvailable);
        ArrayList<Object> qualityList = jsonGetFormatArrayAPI(entries);
        /* Now let's find the best quality AND find out whether the user-selected format is available or not. */
        for (final Object qualityo : qualityList) {
            entries = (LinkedHashMap<String, Object>) qualityo;
            entries = (LinkedHashMap<String, Object>) entries.get("recordFormat");
            formatIDtemp = jsonGetFormatArrayGetIDAPI(entries);
            if (formatIDtemp > formatIDFallback) {
                formatIDFallback = formatIDtemp;
            }
            if (formatIDtemp == formatIDselected) {
                selectedFormatIsAvailable = true;
            }
        }
        if (selectedFormatIsAvailable) {
            logger.info("Selected format is available --> Downloading that: " + formatIDselected);
        } else {
            logger.info("Selected format is not available --> Downloading highest format available instead: " + formatIDFallback);
            formatIDselected = formatIDFallback;
        }
        String dllink = checkDirectLink(link, formatIDselected, downloadAdsFreeValue);
        if (StringUtils.isEmpty(dllink)) {
            accessDownloadPageAPI(link, formatIDselected, downloadAdsFreeValue);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(this.br.toString());
            dllink = (String) entries.get("downloadUrl");
        }
        handleDownload(link, account, dllink);
    }

    /**
     * Check whether the users' ads-free selection is available. <br />
     * If not, act according to the users' settings and either wait and retry or download the version with ads instead. <br />
     *
     * @return: Basically this can change preferAdsFree from true to false if ads-free version is now available and this is allowed by the
     *          user. <br />
     *          Can be used an API- and website mode.
     */
    private boolean verifyAdsFreeUserSelection(final DownloadLink dl, boolean preferAdsFree, boolean isAdsFreeAvailable) throws PluginException {
        final int adsFreeLength = dl.getIntegerProperty(PROPERTY_site_runtime_seconds_adsfree, -1);
        if (isAdsFreeAvailable && adsFreeLength == 0) {
            /* 2017-10-02: Try to prevent errors because of "DOWNLOADSESSIONVIDEOFILESSERVICE_NOCONTENT" at this stage already. */
            logger.info("adsFreeLength is ZERO but according to API, adFree version is available --> changing isAdsFreeAvailable to false");
            isAdsFreeAvailable = false;
        }
        if (preferAdsFree && !isAdsFreeAvailable) {
            /*
             * User wants ads-free but it's not available -> Wait X [User-Defined DOWNLOADONLYADSFREE_RETRY_HOURS] hours, status can still
             * change but probably won't -> If defined by user, force version with ads after a user defined amount of retries.
             */
            logger.info("Ad-free version is unavailable");
            final long userDefinedWaitHours = cfg.getLongProperty(ADS_FREE_UNAVAILABLE_HOURS, SaveTv.defaultADS_FREE_UNAVAILABLE_HOURS);
            final long timestamp_releasedate = dl.getLongProperty(PROPERTY_originaldate_end, 0);
            final long timestamp_with_ads_allowed = getTimestampWhenDownloadWithAdsIsAllowed(dl);
            final boolean allow_load_with_ads = userDefinedWaitHours > 0 && System.currentTimeMillis() > timestamp_with_ads_allowed;
            if (allow_load_with_ads) {
                logger.info("Ad-free version is unavailable AND download with-ads is allowed by user defined timeout --> Downloading version with ads instead");
                preferAdsFree = false;
            } else if (userDefinedWaitHours == 0) {
                logger.info("Ad-free version is unavailable AND download with ads is prohibited by user timeout(default==never_allow_ads) --> Waiting");
                errorAdsFreeUnavailable(60 * 60 * 1000l, null);
            } else {
                logger.info("Ad-free version is unavailable AND download with ads is prohibited by user timeout(" + userDefinedWaitHours + ") --> Waiting");
                final String adsfree_unavailable_message;
                final long waitMilliseconds;
                final long waitMillisecondsDefault = 60 * 60 * 1000l;
                if (timestamp_releasedate > 0) {
                    final long milliseconds_time_remaining_until_download_with_ads_allowed = timestamp_with_ads_allowed - System.currentTimeMillis();
                    adsfree_unavailable_message = getErrormessageDownloadWithadsInTimeX(milliseconds_time_remaining_until_download_with_ads_allowed);
                    if (milliseconds_time_remaining_until_download_with_ads_allowed < waitMillisecondsDefault) {
                        /* Small waittime exact waittime */
                        waitMilliseconds = milliseconds_time_remaining_until_download_with_ads_allowed;
                    } else {
                        /* Long waittime - wait default wait */
                        waitMilliseconds = waitMillisecondsDefault;
                    }
                } else {
                    adsfree_unavailable_message = "Werbefreie Version noch nicht verfügbar";
                    waitMilliseconds = waitMillisecondsDefault;
                }
                errorAdsFreeUnavailable(waitMilliseconds, adsfree_unavailable_message);
            }
        } else if (preferAdsFree) {
            /* User selection (with- or without ads) is available! */
            logger.info("Ad-free version is available AND preferred by the user AND will be downloaded");
        } else {
            logger.info("Version with ads is available AND preferred by the user AND will be downloaded");
        }
        return preferAdsFree;
    }

    /** Handles download for API- and website mode */
    private void handleDownload(final DownloadLink link, final Account account, final String dllink) throws Exception {
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Final downloadlink is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting download...");
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler 404", 15 * 60 * 1000l);
            }
            /* Handle (known) errors */
            logger.warning("Received HTML code instead of the file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unbekannter Serverfehler 1 - bitte dem JDownloader Support mit Log melden!", 60 * 60 * 1000l);
        } else if (dl.getConnection().getLongContentLength() <= 1048576) {
            /* Avoid downloading (too small) trash data */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler: Datei vom Server zu klein", 60 * 60 * 1000l);
        }
        String server_filename = getFileNameFromHeader(dl.getConnection());
        server_filename = fixCharIssues(server_filename);
        server_filename = server_filename.substring(0, server_filename.lastIndexOf("."));
        link.setProperty(PROPERTY_server_filename, server_filename);
        /* This is for checking server speed. */
        final String previouscomment = link.getComment();
        if (previouscomment == null || previouscomment.contains("Aktuell/Zuletzt verwendeter direkter Downloadlink:")) {
            link.setComment("Aktuell/Zuletzt verwendeter direkter Downloadlink: " + dllink);
        }
        final String final_filename = getFilename(this, link);
        link.setFinalFileName(final_filename);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
            } else {
                if (cfg.getBooleanProperty(DELETE_TELECAST_ID_AFTER_DOWNLOAD, defaultDeleteTelecastIDAfterDownload)) {
                    logger.info("Download finished --> User WANTS telecastID " + getTelecastId(link) + " deleted");
                    killTelecastID(link);
                } else {
                    logger.info("Download finished --> User does NOT want telecastID " + getTelecastId(link) + " deleted");
                }
                markTelecastIdAsDownloaded(link);
            }
        } catch (final PluginException e) {
            try {
                if (e.getLinkStatus() == LinkStatus.ERROR_ALREADYEXISTS) {
                    if (cfg.getBooleanProperty(DELETE_TELECAST_ID_IF_FILE_ALREADY_EXISTS, defaultDeleteTelecastIDIfFileAlreadyExists)) {
                        logger.info("ERROR_ALREADYEXISTS --> User WANTS telecastID " + getTelecastId(link) + " deleted");
                        killTelecastID(link);
                    } else {
                        logger.info("ERROR_ALREADYEXISTS --> User does NOT want telecastID " + getTelecastId(link) + " deleted");
                    }
                    throw e;
                }
            } catch (final Throwable efail) {
                /* Do not fail here, throw Exception which happened previously */
                throw e;
            }
            throw e;
        }
    }

    /**
     * Check if a stored directlink exists under property and if so, check if it is still valid (leads to a downloadable content [NOT
     * html]). <br />
     * This will only returns something if the user hasn't changed the settings from what he had before to make sure we won't download
     * invalid videofiles.
     */
    private String checkDirectLink(final DownloadLink dl, final int formatID, final boolean adsFree) {
        final String property = getDirectlinkProperty(dl, formatID, adsFree);
        String dllink = dl.getStringProperty(property);
        if (dllink != null) {
            logger.info("Found saved downloadurl --> Checking if it is still valid");
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    dl.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                dl.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            if (dllink != null) {
                logger.info("Re-using saved directurl");
            } else {
                logger.info("Failed to re-use saved directurl --> Deleted it");
            }
        } else {
            logger.info("Failed to find saved downloadurl");
        }
        return dllink;
    }

    /**
     * Returns property to find final downloadurl based on users' settings. <br />
     * This way we make sure that we grab the correct saved downloadurl.
     */
    private String getDirectlinkProperty(final DownloadLink dl, final int formatID, final boolean adsFree) {
        final boolean hasMoved = dl.getBooleanProperty(PROPERTY_has_moved, false);
        return "directurl_" + formatID + "_" + Boolean.toString(adsFree) + "_" + Boolean.toString(hasMoved);
    }

    /**
     * Returns whether user wants to download the ads-free version or the version with ads. <br />
     * Parameters inside the user-added URL can override his basic plugin settings. This function respects that.
     */
    public static boolean getPreferAdsFree(final DownloadLink dl) {
        final String preferAdsFreeUrl = new Regex(dl.getDownloadURL(), "adsfree=(true|false)").getMatch(0);
        final boolean preferAdsFree;
        if (preferAdsFreeUrl != null) {
            /* Parameters in urls can override plugin settings! */
            preferAdsFree = Boolean.parseBoolean(preferAdsFreeUrl);
        } else {
            preferAdsFree = getPreferAdsFreePluginConfig();
        }
        return preferAdsFree;
    }

    /**
     * Returns whether user wants to download the ads-free version or the version with ads. <br />
     * Only based on users' config!!
     */
    public static boolean getPreferAdsFreePluginConfig() {
        final SubConfiguration cfg = SubConfiguration.getConfig(HOST_STATIC);
        final boolean preferAdsFreeConfig = cfg.getBooleanProperty(PROPERTY_PREFERADSFREE, defaultPreferAdsFree);
        return preferAdsFreeConfig;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai;
        setConstants(account, null);
        login(this.br, account, true);
        if (is_API_enabled(account.getHoster())) {
            ai = fetchAccountInfoAPI(account);
        } else {
            ai = fetchAccountInfoWebsite(account);
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    /**
     * TODO: Can't we find the expire date of the account?
     *
     * @throws Exception
     */
    private AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        String package_name = null;
        api_GET(br, "/user?fields=contract.hasxlpackage%2C%20contract.hasxxlpackage%2C%20contract.islocked%2C%20contract.isrunning%2C%20contract.packagename");
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("contract");
        final boolean isPremium = ((Boolean) entries.get("isRunning")).booleanValue();
        package_name = (String) entries.get("packageName");
        if (isPremium) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        if (StringUtils.isEmpty(package_name)) {
            package_name = ACCOUNTTYPE_UNKNOWN;
        }
        ai.setStatus(package_name);
        account.setProperty(PROPERTY_acc_type, package_name);
        return ai;
    }

    private AccountInfo fetchAccountInfoWebsite(final Account account) {
        final AccountInfo ai = new AccountInfo();
        /* Do not fail here */
        String package_name = null;
        try {
            /*
             * Get long lasting login cookie. Keep in mind that such a cookie can only exist once for every account so in case a user uses
             * multiple JDs it might happen that they "steal" themselves this cookie but it should still work fine for up to 3 JDownloader
             * instances.
             */
            String long_cookie = br.getCookie("http://" + this.getHost(), "SLOCO");
            if (long_cookie == null || long_cookie.trim().equals("bAutoLoginActive=1")) {
                logger.info("Long session cookie does not exist yet/anymore - enabling it");
                br.postPage("https://www." + this.getHost() + "/STV/M/obj/user/submit/submitAutoLogin.cfm", "IsAutoLogin=true&Messages=");
                long_cookie = br.getCookie("http://save.tv/", "SLOCO");
                if (long_cookie == null || long_cookie.trim().equals("")) {
                    logger.info("Failed to get long session cookie");
                } else {
                    logger.info("Successfully received long session cookie and saved cookies");
                    account.saveCookies(this.br.getCookies(account.getHoster()), "");
                }
            } else {
                logger.info("Long session cookie exists");
            }
            /* Find account details */
            String price = null;
            br.getPage("https://www." + this.getHost() + "/STV/M/obj/user/JSON/userConfigApi.cfm?iFunction=2");
            final String acc_username = br.getRegex("\"SUSERNAME\":(\\d+)").getMatch(0);
            final String user_packet_id = PluginJSonUtils.getJsonValue(br, "CURRENTARTICLEID");
            /* Find the price of the package which the user uses. */
            final String all_packages_string = br.getRegex("\"ARRRENEWARTICLES\":\\[(.*?)\\]").getMatch(0);
            final String[] all_packets = all_packages_string.split("\\},\\{");
            for (final String packet : all_packets) {
                if (packet.contains("\"ID\":" + user_packet_id + ".0")) {
                    price = PluginJSonUtils.getJsonValue(packet, "IPRICE");
                    if (price != null) {
                        break;
                    }
                }
            }
            final String expireDate_str = PluginJSonUtils.getJsonValue(br, "DCURRENTARTICLEENDDATE");
            final long expireDate_real = TimeFormatter.getMilliSeconds(expireDate_str, "yyyy-MM-dd hh:mm:ss", Locale.GERMAN);
            long expireDate_user_display = expireDate_real;
            final long timeleft = System.currentTimeMillis() - expireDate_real;
            if ((timeleft > 0 && timeleft < 24 * 60 * 60 * 1000l) || (timeleft < 0 && timeleft > -2 * 60 * 60 * 1000l)) {
                /*
                 * Account expired less then 24 hours ago or is only valid for 2 hours or less --> Add 24 hours to it so in case the user
                 * has a subscription JD does not deactivate the account because save.tv needs some time to show the new expire date.
                 */
                expireDate_user_display += 24 * 60 * 60 * 1000;
            }
            ai.setValidUntil(expireDate_user_display);
            account.setProperty(PROPERTY_acc_expire, expireDate_real);
            package_name = PluginJSonUtils.getJsonValue(br, "SCURRENTARTICLENAME");
            if (inValidate(package_name)) {
                /* This should never happen */
                package_name = ACCOUNTTYPE_UNKNOWN;
            }
            final String runtime = new Regex(package_name, "(\\d+ Monate)").getMatch(0);
            account.setProperty(PROPERTY_acc_package, correctData(account.getHoster(), package_name));
            if (price != null) {
                account.setProperty(PROPERTY_acc_price, correctData(account.getHoster(), price));
            }
            if (runtime != null) {
                account.setProperty(PROPERTY_acc_runtime, correctData(account.getHoster(), runtime));
            }
            if (acc_username != null) {
                account.setProperty(PROPERTY_acc_username, correctData(account.getHoster(), acc_username));
            }
            br.getPage("https://www." + this.getHost() + "/STV/M/obj/archive/JSON/VideoArchiveApi.cfm?iEntriesPerPage=1");
            final String totalLinks = PluginJSonUtils.getJsonValue(br, "ITOTALENTRIES");
            if (totalLinks != null) {
                account.setProperty(PROPERTY_acc_count_telecast_ids, totalLinks);
            }
            account.setType(AccountType.PREMIUM);
        } catch (final Throwable e) {
            /* Should not happen but a failure of the account detail crawler won't hurt - we logged in fine! */
            logger.info("Extended account check via website failed");
        }
        ai.setStatus(package_name);
        account.setProperty(PROPERTY_acc_type, package_name);
        return ai;
    }

    /** Performs login respecting api setting */
    private void login(final Browser br, final Account account, final boolean force) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (is_API_enabled(account.getHoster())) {
            login_api(br, account, force);
        } else {
            login_website(br, account, force);
        }
    }

    public static void login_website(final Browser br, final Account account, final boolean force) throws Exception {
        final String lang = System.getProperty("user.language");
        site_prepBrowser(br);
        synchronized (LOCK_login) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(account.getHoster(), cookies);
                    return;
                }
                final String postData = "sUsername=" + Encoding.urlEncode(account.getUser()) + "&sPassword=" + Encoding.urlEncode(account.getPass()) + "&bAutoLoginActivate=1";
                br.postPage("https://www." + account.getHoster() + "/STV/M/Index.cfm?sk=PREMIUM", postData);
                if (br.containsHTML("No htmlCode read")) {
                    br.getPage("https://www." + account.getHoster() + "/STV/M/obj/TVProgCtr/tvctShow.cfm");
                }
                if (!br.containsHTML("class=\"member\\-nav\\-li member\\-nav\\-account \"") || br.containsHTML("Bitte verifizieren Sie Ihre Logindaten")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final String acc_count_archive_entries = br.getRegex(">(\\d+) Sendungen im Archiv<").getMatch(0);
                if (acc_count_archive_entries != null) {
                    account.setProperty(PROPERTY_acc_count_archive_entries, acc_count_archive_entries);
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    public static String getAccessTokenAndSetHeaderAPI(final Browser br, final Account account) {
        final String api_access_token = account.getStringProperty(PROPERTY_ACCOUNT_API_SESSIONID, null);
        if (api_access_token != null) {
            setAPIAuthHeaders(br, api_access_token);
        }
        return api_access_token;
    }

    public static void login_api(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK_login) {
            api_prepBrowser(br);
            String api_access_token = getAccessTokenAndSetHeaderAPI(br, account);
            String refresh_token = account.getStringProperty(PROPERTY_refresh_token, null);
            long expires_in = account.getLongProperty(PROPERTY_expires_in, 0);
            final boolean token_expired = expires_in > 0 && expires_in <= System.currentTimeMillis();
            if (refresh_token != null && (force || token_expired)) {
                /* Avoid full login - try to refresh old token instead! */
                api_POST(br, API_BASE_AUTH + "token", "grant_type=refresh_token&client_id=" + getAPIClientID() + "&client_secret=" + getAPISecretKey() + "&refresh_token=" + Encoding.urlEncode(refresh_token));
                try {
                    parseLoginInfo(br, account);
                } catch (final PluginException e) {
                    api_access_token = null;
                }
            }
            if (api_access_token == null) {
                /* Only generate new access_token if we have none */
                /* New token required */
                api_POST(br, API_BASE_AUTH + "token", "grant_type=password&client_id=" + getAPIClientID() + "&client_secret=" + getAPISecretKey() + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                parseLoginInfo(br, account);
            }
        }
    }

    public static void parseLoginInfo(final Browser br, final Account account) throws PluginException {
        String api_access_token = PluginJSonUtils.getJson(br, "access_token");
        String refresh_token = PluginJSonUtils.getJson(br, "refresh_token");
        /* 2017-09-18: This will usually last 3599 seconds --> ~1 hour */
        final String seconds_expires_in_str = PluginJSonUtils.getJson(br, "expires_in");
        if (br.getHttpConnection().getResponseCode() == 400 || StringUtils.isEmpty(api_access_token) || StringUtils.isEmpty(refresh_token) || StringUtils.isEmpty(seconds_expires_in_str)) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        /* 60 seconds of tolerance to avoid requests happening while our token expires. */
        long expires_in = (Long.parseLong(seconds_expires_in_str) - 60) * 1000;
        account.setProperty(PROPERTY_ACCOUNT_API_SESSIONID, api_access_token);
        account.setProperty(PROPERTY_refresh_token, refresh_token);
        account.setProperty(PROPERTY_expires_in, System.currentTimeMillis() + expires_in);
        setAPIAuthHeaders(br, api_access_token);
    }

    /* Always compare to the decrypter */
    private void getPageSafe(final String url, final Account account) throws Exception {
        this.br.getPage(url);
        handleErrorsWebsite(br, account);
    }

    /*
     * Handles website json errors.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void handleErrorsWebsite(final Browser br, final Account account) throws Exception {
        /*
         * Do NOT use the json parser for the first check as especially when used via decrypter, the amount of data can be huge which can
         * lead to parser memory problems/crashes.
         */
        final boolean isInErrorState = br.containsHTML("\\d+\\.\\d+E\\d+,\"NOK\",\"");
        if (isInErrorState) {
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final ArrayList<Object> errorlist = (ArrayList) entries.get("ARRVIDEOURL");
            final String errormessage = (String) errorlist.get(2);
            if (errormessage.contains("Aufnahme zu betrachten, laden Sie bitte die ungeschnittene Version")) {
                errorAdsFreeUnavailable(60 * 60 * 1000, null);
            } else if (errormessage.equals("unknown error")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler: 'unknown error'", 15 * 60 * 1000l);
            } else {
                /* We can retry on unknown errorstates here as we know that Stv will usually fix them soon. */
                logger.warning("Unhandled / Unknown error happened");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Undehandelter Fehler: " + errormessage, 30 * 60 * 1000l);
            }
        } else if (br.getURL().contains(URL_LOGGED_OUT)) {
            logger.info("We were loggedout for unknown reasons --> Trying to perform full login");
            login_website(br, account, true);
            throw new PluginException(LinkStatus.ERROR_RETRY, "Loginproblem", 30 * 1000l);
        }
    }

    private String getErrormessageDownloadWithadsInTimeX(final long milliseconds_time_remaining_until_download_with_ads_allowed) {
        return String.format("%s bis Download mit Werbung | Werbefreie Version noch nicht verfügbar", TimeFormatter.formatMilliSeconds(milliseconds_time_remaining_until_download_with_ads_allowed, 0));
    }

    /**
     * Execute this if a download attempt was made but the length of the adFree version is 0. <br />
     * Waits either the user-defined waittime or at least the forcedWaittime.
     */
    private void errorAdsFreeUnavailableWithForcedWaittime(final DownloadLink dl, final long forcedWaittime) throws PluginException {
        final String errormessage;
        final long waittime;
        final long timestamp_with_ads_allowed = getTimestampWhenDownloadWithAdsIsAllowed(dl);
        final long timestamp_current = System.currentTimeMillis();
        if (timestamp_with_ads_allowed > timestamp_current) {
            waittime = timestamp_with_ads_allowed - System.currentTimeMillis();
            errormessage = getErrormessageDownloadWithadsInTimeX(waittime);
        } else {
            waittime = forcedWaittime;
            errormessage = USERTEXT_NOCUTAVAILABLE;
        }
        errorAdsFreeUnavailable(waittime, errormessage);
    }

    private void errorAdsFreeUnavailable(final long waitMilliseconds, String errormessage) throws PluginException {
        if (errormessage == null) {
            errormessage = USERTEXT_NOCUTAVAILABLE;
        }
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage, waitMilliseconds);
    }

    private void handleErrorsAPI(final Browser br, final Account account) throws Exception {
        /* TODO: Fill me */
        if (br.getHttpConnection().getResponseCode() == 401) {
            login_api(br, account, true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Loginproblem", 1 * 60 * 1000l);
        } else {
            LinkedHashMap<String, Object> error_map = null;
            try {
                final Object errorO = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                if (errorO instanceof ArrayList) {
                    final ArrayList<Object> errorlist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                    error_map = (LinkedHashMap<String, Object>) errorlist.get(1);
                }
            } catch (final Throwable e) {
            }
            if (error_map != null) {
                logger.info("An API error happened");
                final String humanReadableErrormessage = (String) error_map.get("userMessage");
                final String id = (String) error_map.get("id");
                if (humanReadableErrormessage != null) {
                    logger.info("API_error: " + humanReadableErrormessage);
                }
                if (id.equalsIgnoreCase("DOWNLOADSESSIONVIDEOFILESSERVICE_NOCONTENT")) {
                    logger.info("AdFree version is empty --> Failed to start download");
                    errorAdsFreeUnavailableWithForcedWaittime(this.currDownloadlink, 60 * 60 * 1000);
                } else {
                    /** TODO: Collect errors at this stage */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
    }

    /**
     * @param dl
     *            DownloadLink
     * @param user_selected_video_quality
     *            : Vom Benutzer bevorzugte Qualitätsstufe
     * @param downloadWithoutAds
     *            : Videos mit angewandter Schnittliste bevorzugen oder nicht
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private void requestDownloadAPI(final DownloadLink dl, final String user_selected_video_quality, final String downloadWithoutAds) throws Exception {
        api_POST(this.br, "https://www." + dl.getHost() + "/STV/M/obj/cRecordOrder/croGetDownloadUrl.cfm?null.GetDownloadUrl", "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetDownloadUrl&c0-id=&c0-param0=number:" + getTelecastId(dl) + "&" + user_selected_video_quality + "&c0-param2=boolean:" + downloadWithoutAds + "&xml=true&");
    }

    /**
     * @param dl
     *            :DownloadLink
     * @param user_selected_video_quality
     *            : ID of user selected quality
     * @param downloadWithoutAds
     *            : Download with- or without ads
     */
    private void requestDownloadWebsite(final DownloadLink dl, final int user_selected_video_quality, final boolean downloadWithoutAds) throws Exception {
        final String downloadoverview_url = "https://www." + dl.getHost() + "/STV/M/obj/cRecordOrder/croGetDownloadUrl2.cfm?TelecastId=" + getTelecastId(dl) + "&iFormat=" + user_selected_video_quality + "&bAdFree=" + Boolean.toString(downloadWithoutAds);
        this.getPageSafe(downloadoverview_url, this.currAcc);
    }

    /**
     * @param dl
     *            :DownloadLink
     * @param user_selected_video_quality
     *            : ID of user selected quality
     * @param downloadWithoutAds
     *            : Download with- or without ads
     * @throws Exception
     */
    private void accessDownloadPageAPI(final DownloadLink dl, final int user_selected_video_quality, final boolean downloadWithoutAds) throws Exception {
        api_GET(this.br, String.format("/records/%s/downloads/%d?adfree=%s", getTelecastId(dl), user_selected_video_quality, Boolean.toString(downloadWithoutAds)));
    }

    /**
     * Deletes a desired telecastID from the users' account(!) <br />
     * Works for API- and website mode.
     *
     * @param dl
     *            DownloadLink: The DownloadLink whose telecastID will be deleted.
     */
    private void killTelecastID(final DownloadLink dl) throws Exception {
        if (apiActive()) {
            killTelecastIDAPI(dl);
        } else {
            killTelecastIDWebsite(dl);
        }
    }

    /**
     * Marks a downloaded file serverside as downloaded. <br />
     * Only usable in API mode. Will do nothing in website mode! <br />
     * On success, user will see the text "Completely downloaded" on the Stv telecastID site in the section "Tags".
     *
     * @throws Exception
     */
    private void markTelecastIdAsDownloaded(final DownloadLink dl) throws Exception {
        if (!this.apiActive()) {
            logger.info("API is not available --> Cannot mark telecastID as downloaded");
        } else {
            logger.info("Mark telecastID as downloaded ...");
            final String telecastID = getTelecastId(dl);
            api_POST(this.br, "/records/" + telecastID + "/tags/download-completed", "id=" + telecastID);
            final int responsecode = this.br.getHttpConnection().getResponseCode();
            if (responsecode == 200) {
                logger.info("Successfully marked telecastID as downloaded");
            } else {
                logger.info("Unknown status: Not sure whether telecastID has been marked as downloaded or not");
            }
        }
    }

    /**
     * Deletes a desired telecastID from the users' account(!) <br />
     *
     * @param dl
     *            DownloadLink: The DownloadLink whose telecastID will be deleted.
     */
    private void killTelecastIDWebsite(final DownloadLink dl) throws Exception {
        try {
            final String deleteurl = "https://www." + dl.getHost() + "/STV/M/obj/cRecordOrder/croDelete.cfm?TelecastID=" + getTelecastId(dl);
            this.br.getPage(deleteurl);
            if (br.containsHTML("\"ok\"")) {
                logger.info("Successfully deleted telecastID");
            } else {
                logger.warning("Failed to delete telecastID");
            }
        } catch (final Throwable e) {
            logger.info("Failed to delete telecastID");
        }
    }

    /**
     * Deletes a desired telecastID from the users' account(!) <br />
     *
     * @param dl
     *            DownloadLink: The DownloadLink whose telecastID will be deleted.
     */
    private void killTelecastIDAPI(final DownloadLink dl) throws Exception {
        try {
            api_GET(this.br, "/records/" + getTelecastId(dl));
            final long responsecode = br.getHttpConnection().getResponseCode();
            if (responsecode == 422) {
                logger.info("Failed to delete telecastID");
            } else if (responsecode == 200) {
                logger.info("Successfully deleted telecastID");
            } else {
                logger.info("Unknown status: Not sure whether telecastID has been deleted or not");
            }
        } catch (final Throwable e) {
            logger.info("Failed to delete telecastID");
        }
    }

    private static void site_prepBrowser(final Browser br) {
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        br.getHeaders().put("User-Agent", "JDownloader");
    }

    /** Prepare Browser for API usage. */
    public static void api_prepBrowser(final Browser br) {
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        br.getHeaders().put("User-Agent", "JDownloader");
        /* Important header; without it, we might get XML instead! */
        br.getHeaders().put("Accept", "application/json");
        br.setAllowedResponseCodes(new int[] { 400, 422 });
    }

    /** Set Authorization header(s) for API usage. */
    public static void setAPIAuthHeaders(final Browser br, final String auth_token) {
        br.getHeaders().put("Authorization", "Bearer " + auth_token);
    }

    /**
     * Returns boolean to indicate whether API usage is enabled and working (= login token exists and is valid). <br />
     */
    private boolean apiActive() {
        return (this.currAcc != null && this.currAcc.getStringProperty(PROPERTY_ACCOUNT_API_SESSIONID, null) != null && is_API_enabled(this.getHost()));
    }

    private long getTimestampWhenDownloadWithAdsIsAllowed(final DownloadLink dl) {
        final long userDefinedWaitHours = cfg.getLongProperty(ADS_FREE_UNAVAILABLE_HOURS, SaveTv.defaultADS_FREE_UNAVAILABLE_HOURS);
        final long timestamp_releasedate = dl.getLongProperty(PROPERTY_originaldate_end, 0);
        final long timestamp_with_ads_allowed = timestamp_releasedate + userDefinedWaitHours * 60 * 60 * 1000;
        return timestamp_with_ads_allowed;
    }

    public static long calculateFilesize(final DownloadLink dl, final String formatString) {
        return calculateFilesize(dl, convertQualityStringToInternalID(formatString));
    }

    /**
     * Returns estimate filesize based on calculation via time and hardcoded bitrate. <br />
     * Especially useful whenever filesize is not given via website / API but still 'nice to have'. <br />
     * Keep in mind that this calculation must not be very accurate.
     */
    public static long calculateFilesize(final DownloadLink dl, final int formatID) {
        final int duration_relevant = getDurationDependingOnUserSettings(dl);
        final double mb_per_second = getBitrateForFormat(formatID);
        final double calculated_filesize = mb_per_second * duration_relevant * 1024 * 1024;
        return (long) calculated_filesize;
    }

    public static double getBitrateForFormat(final int format) {
        switch (format) {
        case SITE_FORMAT_LQ:
            return QUALITY_H264_MOBILE_MB_PER_SECOND;
        case SITE_FORMAT_HQ:
            return QUALITY_H264_NORMAL_MB_PER_SECOND;
        case SITE_FORMAT_HD:
            return QUALITY_HD_MB_PER_SECOND;
        default:
            return getDefaultFormatID();
        }
    }

    /** AdsFree duration < Duration of video with ads */
    public static int getDurationDependingOnUserSettings(final DownloadLink dl) {
        final int duration_seconds_adsfree = dl.getIntegerProperty(PROPERTY_site_runtime_seconds_adsfree, 0);
        final int duration_seconds_withads = dl.getIntegerProperty(PROPERTY_site_runtime_seconds_withads, 0);
        final int duration_relevant;
        final boolean user_prefers_adsfree = getPreferAdsFree(dl);
        if (user_prefers_adsfree && duration_seconds_adsfree > 0) {
            duration_relevant = duration_seconds_adsfree;
        } else {
            duration_relevant = duration_seconds_withads;
        }
        return duration_relevant;
    }

    /** @return true: formatID is known, false: formatID is unknown */
    public static boolean isKnownFormatID(final int formatID) {
        return (formatID > 3 && formatID < 7);
    }

    /**
     * Returns selected quality id. <br />
     * This is only that complicated because the user selection can be overridden via parameters inside the URL.
     */
    public static int getConfiguredVideoFormatID(final DownloadLink dl) {
        int videoformat;
        final int videoformatURL = getConfiguredVideoFormatUrl(dl);
        if (videoformatURL != -1) {
            videoformat = videoformatURL;
        } else {
            videoformat = getConfiguredVideoFormatConfig();
        }
        if (!isKnownFormatID(videoformat)) {
            /* E.g. wrong formatID given via URL --> Fallback to default formatID */
            videoformat = getDefaultFormatID();
        }
        return videoformat;
    }

    /** Returns default formatID. 2017-08-11: Returns ID for HQ format --> Website-default */
    public static int getDefaultFormatID() {
        return SITE_FORMAT_HQ;
    }

    /** Returns BEST formatID. 2017-08-11: Returns ID for HD format --> Website-best */
    public static int getBestFormatID() {
        return SITE_FORMAT_HD;
    }

    /** Converts quality Strings e.g. "HD" to their corresponding IDs: --> "HD" --> 6 */
    public static int convertQualityStringToInternalID(final String qualityString) {
        final int qualityID;
        if (qualityString == null) {
            qualityID = SITE_FORMAT_HQ;
        } else if (qualityString.equals(STATE_QUALITY_HD)) {
            qualityID = SITE_FORMAT_HD;
        } else if (qualityString.equals(STATE_QUALITY_HQ)) {
            qualityID = SITE_FORMAT_HQ;
        } else {
            qualityID = SITE_FORMAT_LQ;
        }
        return qualityID;
    }

    /** Returns user selected formatID based on PluginConfiguration */
    @SuppressWarnings("deprecation")
    public static int getConfiguredVideoFormatConfig() {
        switch (SubConfiguration.getConfig(HOST_STATIC).getIntegerProperty(SELECTED_VIDEO_FORMAT, -1)) {
        case 0:
            return SITE_FORMAT_HD;
        case 1:
            return SITE_FORMAT_HQ;
        case 2:
            return SITE_FORMAT_LQ;
        default:
            /* Should never happen */
            return getBestFormatID();
        }
    }

    /**
     * Returns formatID from inside user-added URL. <br />
     * Keep in mind, this does NOT verify whether the found value is correct or not. <br />
     *
     * @return Either formatID from inside URL or -1 if nothing found.
     */
    @SuppressWarnings("deprecation")
    public static int getConfiguredVideoFormatUrl(final DownloadLink dl) {
        if (dl == null) {
            /* Fallback - upper functions should now use formatID which user has selected in plugin settings. */
            return -1;
        }
        final String format_from_url = new Regex(dl.getDownloadURL(), "preferformat=(\\d+)").getMatch(0);
        if (format_from_url == null) {
            /* Fallback - upper functions should now use formatID which user has selected in plugin settings. */
            return -1;
        }
        /* Convert official videoformat-number to internal number. */
        return Integer.parseInt(format_from_url);
    }

    /** Calculates runtime in minutes based on given filesize and hardcoded bitrates. */
    @SuppressWarnings("unused")
    private double site_get_calculated_runtime_minutes(final DownloadLink dl, final long page_size_mb) {
        double run_time_calculated = 0;
        final int selected_video_format = getConfiguredVideoFormatID(dl);
        switch (selected_video_format) {
        case SITE_FORMAT_HD:
            run_time_calculated = page_size_mb / QUALITY_HD_MB_PER_SECOND;
            break;
        case SITE_FORMAT_HQ:
            run_time_calculated = page_size_mb / QUALITY_H264_NORMAL_MB_PER_SECOND;
            break;
        case SITE_FORMAT_LQ:
            run_time_calculated = page_size_mb / QUALITY_H264_MOBILE_MB_PER_SECOND;
            break;
        default:
            run_time_calculated = page_size_mb / QUALITY_H264_NORMAL_MB_PER_SECOND;
            break;
        }
        return run_time_calculated;
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Object> jsonGetFormatArrayAPI(final LinkedHashMap<String, Object> entries) {
        if (entries == null) {
            return null;
        }
        final Object formatsO = entries.get("formats");
        if (formatsO == null || !(formatsO instanceof ArrayList)) {
            return null;
        }
        return (ArrayList<Object>) formatsO;
    }

    public static int jsonGetFormatArrayGetIDAPI(final LinkedHashMap<String, Object> entries) {
        return (int) JavaScriptEngineFactory.toLong(entries.get("id"), SITE_FORMAT_HD);
    }

    /** Ads-Free status -1 = Unknown, 0 = false, 1 = true */
    public static short jsonGetAdsFreeAvailableAPIDetailed(final LinkedHashMap<String, Object> entries) {
        final Object adFreeAvailableO = entries != null ? entries.get("adFreeAvailable") : null;
        final short adsfreeStatus;
        if (adFreeAvailableO == null || !(adFreeAvailableO instanceof Boolean)) {
            adsfreeStatus = -1;
        } else {
            final boolean adsFree = ((Boolean) adFreeAvailableO).booleanValue();
            if (adsFree) {
                adsfreeStatus = 1;
            } else {
                adsfreeStatus = 0;
            }
        }
        return adsfreeStatus;
    }

    /** Ads-Free status true, false (NOT unknown!) */
    public static boolean jsonGetAdsFreeAvailableAPI(final LinkedHashMap<String, Object> entries) {
        final short adsFreeStatus = jsonGetAdsFreeAvailableAPIDetailed(entries);
        if (adsFreeStatus == 1) {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static int jsonGetBestQualityIdWebsite(final ArrayList<Object> sourcelist) {
        final long recordingformat;
        if (sourcelist != null && sourcelist.size() > 0) {
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) sourcelist.get(sourcelist.size() - 1);
            recordingformat = jsonGetRecordingformatid(entries);
        } else {
            /* Fallback to a format which is always available. */
            recordingformat = getDefaultFormatID();
        }
        return (int) recordingformat;
    }

    @SuppressWarnings("unchecked")
    public static String jsonGetBestQualityIdAPI(final ArrayList<Object> sourcelist) {
        final String recordingformat;
        if (sourcelist != null && sourcelist.size() > 0) {
            LinkedHashMap<String, Object> entries = null;
            long quality_max = SITE_FORMAT_HQ;
            long quality_temp;
            for (final Object qualityo : sourcelist) {
                entries = (LinkedHashMap<String, Object>) qualityo;
                quality_temp = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "recordFormat/id"), SITE_FORMAT_HQ);
                if (quality_temp > quality_max) {
                    quality_max = quality_temp;
                }
            }
            recordingformat = Long.toString(quality_max);
        } else {
            /* Fallback to a format which is always available. */
            recordingformat = Long.toString(SITE_FORMAT_HQ);
        }
        return recordingformat;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static ArrayList<Object> jsonGetVideoSourcelist(final LinkedHashMap<String, Object> sourcemap) {
        if (sourcemap == null) {
            return null;
        }
        final ArrayList<Object> sourcelist = (ArrayList) sourcemap.get("ARRALLOWDDOWNLOADFORMATS");
        return sourcelist;
    }

    public static int jsonGetRecordingformatid(final LinkedHashMap<String, Object> entries) {
        final long recordingformatid;
        final Object recordingformatido;
        if (entries != null) {
            recordingformatido = entries.get("RECORDINGFORMATID");
        } else {
            /* Errorhandling */
            recordingformatido = null;
        }
        if (recordingformatido == null) {
            recordingformatid = getDefaultFormatID();
        } else if (recordingformatido instanceof Double) {
            recordingformatid = (long) ((Double) recordingformatido).doubleValue();
        } else {
            recordingformatid = ((Integer) recordingformatido).longValue();
        }
        return (int) recordingformatid;
    }

    /** Checks whether user-defined formatID is available or not. */
    @SuppressWarnings("unchecked")
    public static boolean jsonIsDesiredFormatAvailableWebsite(final ArrayList<Object> sourcelist, final int desiredFormat) {
        if (sourcelist == null) {
            return false;
        }
        LinkedHashMap<String, Object> entries = null;
        int format_id = -1;
        for (final Object vsorceo : sourcelist) {
            entries = (LinkedHashMap<String, Object>) vsorceo;
            format_id = jsonGetRecordingformatid(entries);
            if (format_id == desiredFormat) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs save.tv API POST requests without erorhandling. <br />
     *
     * @throws Exception
     */
    private static String api_POST(final Browser br, String url, final String postdata) throws Exception {
        url = correctURLAPI(url);
        br.postPage(url, postdata);
        return br.toString();
    }

    /**
     * Performs save.tv API GET requests. <br />
     *
     * @throws Exception
     */
    private String api_GET(final Browser br, String url) throws Exception {
        url = correctURLAPI(url);
        br.getPage(url);
        handleErrorsAPI(br, this.currAcc);
        return br.toString();
    }

    public static String correctURLAPI(String url) {
        if (url.startsWith("/")) {
            /* Just in case we use another host e.g. for login. */
            url = API_BASE + url;
        }
        return url;
    }

    public static boolean is_API_enabled(final String host) {
        return SubConfiguration.getConfig(host).getBooleanProperty(USEAPI, defaultUSEAPI);
    }

    /** Corrects all kinds of Strings which Stv provides, also makes filenames look nicer. */
    @SuppressWarnings("deprecation")
    public static String correctData(final String host, final String input) {
        String output = Encoding.htmlDecode(input);
        output = output.replace("_", " ");
        output = output.trim();
        output = output.replaceAll("(\r|\n)", "");
        output = output.replace("/", SubConfiguration.getConfig(host).getStringProperty(CUSTOM_FILENAME_SEPERATION_MARK, defaultCustomSeperationMark));
        /* Correct spaces */
        final String[] unneededSpaces = new Regex(output, ".*?([ ]{2,}).*?").getColumn(0);
        if (unneededSpaces != null && unneededSpaces.length != 0) {
            for (String unneededSpace : unneededSpaces) {
                output = output.replace(unneededSpace, " ");
            }
        }
        return output;
    }

    @SuppressWarnings("deprecation")
    private static String getTelecastId(final DownloadLink link) {
        final String telecastID;
        final String url = link.getDownloadURL();
        if (new Regex(url, Pattern.compile(LINKTYPE_DIRECT)).matches()) {
            /* Convert directurls --> 'Normal' Stv 'telecastID'-URLs */
            telecastID = new Regex(url, Pattern.compile("https?://[A-Za-z0-9\\-]+\\.save\\.tv/\\d+_(\\d+)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        } else {
            telecastID = new Regex(url, Pattern.compile("telecastid=(\\d+)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        }
        return telecastID;
    }

    /**
     * @return: Returns a random number for the DownloadLink - uses saved random number if existant - if not, creates random number and
     *          saves it.
     */
    private static String getRandomNumber(final DownloadLink dl) {
        String randomnumber = dl.getStringProperty(PROPERTY_stv_randomnumber, null);
        if (randomnumber == null) {
            final DecimalFormat df = new DecimalFormat("0000");
            randomnumber = df.format(new Random().nextInt(10000));
            dl.setProperty(PROPERTY_stv_randomnumber, randomnumber);
        }
        return randomnumber;
    }

    @SuppressWarnings("deprecation")
    public static String getFormattedFilename(final Plugin plugin, final DownloadLink dl) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig(plugin.getHost());
        final String customStringForEmptyTags = getCustomStringForEmptyTags(plugin.getHost());
        final String acc_username = cfg.getStringProperty(PROPERTY_acc_username, customStringForEmptyTags);
        final String server_filename = dl.getStringProperty(PROPERTY_server_filename, customStringForEmptyTags);
        final String site_title = dl.getStringProperty(PROPERTY_plainfilename, customStringForEmptyTags);
        final String ext = dl.getStringProperty(PROPERTY_type, EXTENSION_default);
        final String quality = dl.getStringProperty(PROPERTY_quality, STATE_QUALITY_UNKNOWN);
        final String ad_free = getAdFreeText(dl);
        final String genre = dl.getStringProperty(PROPERTY_genre, customStringForEmptyTags);
        final String producecountry = dl.getStringProperty(PROPERTY_producecountry, customStringForEmptyTags);
        final String produceyear = dl.getStringProperty(PROPERTY_produceyear, customStringForEmptyTags);
        final String randomnumber = getRandomNumber(dl);
        final String telecastid = getTelecastId(dl);
        final String tv_station = dl.getStringProperty(PROPERTY_plain_tv_station, customStringForEmptyTags);
        final long site_category = dl.getLongProperty(PROPERTY_plain_site_category, -1);
        final String site_category_str = site_category == -1 ? defaultCustomStringForEmptyTags : Long.toString(site_category);
        /* For series */
        final String episodename = dl.getStringProperty(PROPERTY_episodename, customStringForEmptyTags);
        final int seasonnumber = getSeasonNumber(dl);
        final int episodenumber = getEpisodeNumber(dl);
        final String seasonAndEpisodenumber = getSeasonnumberAndEpisodenumber(seasonnumber, episodenumber);
        final long date = dl.getLongProperty(PROPERTY_originaldate, 0l);
        String formattedDate = null;
        final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
        Date theDate = new Date(date);
        if (userDefinedDateFormat != null) {
            try {
                final SimpleDateFormat formatter = new SimpleDateFormat(userDefinedDateFormat);
                formattedDate = formatter.format(theDate);
            } catch (Exception e) {
                /* prevent user error killing plugin */
                formattedDate = defaultCustomStringForEmptyTags;
            }
        }
        String formattedFilename = null;
        if (!isSeries(dl)) {
            /* For all links except series */
            formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_MOVIES, defaultCustomFilenameMovies);
            if (formattedFilename == null || formattedFilename.equals("")) {
                formattedFilename = defaultCustomFilenameMovies;
            }
            formattedFilename = formattedFilename.toLowerCase();
            /* Make sure that the user entered a VALID custom filename - if not, use the default name */
            if (!formattedFilename.contains("*endung*") || (!formattedFilename.contains("*videotitel*") && !formattedFilename.contains("*zufallszahl*") && !formattedFilename.contains("*telecastid*") && !formattedFilename.contains("*sendername*") && !formattedFilename.contains("*username*") && !formattedFilename.contains("*quality*") && !formattedFilename.contains("*server_dateiname*"))) {
                formattedFilename = defaultCustomFilenameMovies;
            }
            formattedFilename = formattedFilename.replace("*zufallszahl*", randomnumber);
            formattedFilename = formattedFilename.replace("*telecastid*", telecastid);
            formattedFilename = formattedFilename.replace("*datum*", formattedDate);
            formattedFilename = formattedFilename.replace("*produktionsjahr*", produceyear);
            formattedFilename = formattedFilename.replace("*endung*", ext);
            formattedFilename = formattedFilename.replace("*quality*", quality);
            formattedFilename = formattedFilename.replace("*werbefrei*", ad_free);
            formattedFilename = formattedFilename.replace("*sendername*", tv_station);
            formattedFilename = formattedFilename.replace("*kategorie*", site_category_str);
            formattedFilename = formattedFilename.replace("*genre*", genre);
            formattedFilename = formattedFilename.replace("*produktionsland*", producecountry);
            formattedFilename = formattedFilename.replace("*username*", acc_username);
            /* Insert actual filename at the end to prevent errors with tags */
            formattedFilename = formattedFilename.replace("*server_dateiname*", server_filename);
            formattedFilename = formattedFilename.replace("*videotitel*", site_title);
        } else {
            /* For series */
            formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_SERIES, defaultCustomSeriesFilename);
            if (formattedFilename == null || formattedFilename.equals("")) {
                formattedFilename = defaultCustomFilenameMovies;
            }
            formattedFilename = formattedFilename.toLowerCase();
            /* Make sure that the user entered a VALID custom filename - if not, use the default name */
            if (!formattedFilename.contains("*endung*") || (!formattedFilename.contains("*serientitel*") && !formattedFilename.contains("*episodenname*") && !formattedFilename.contains("*staffelnummer*") && !formattedFilename.contains("*episodennummer*") && !formattedFilename.contains("*episodennummer_und_staffelnummer*") && !formattedFilename.contains("*zufallszahl*") && !formattedFilename.contains("*telecastid*") && !formattedFilename.contains("*sendername*") && !formattedFilename.contains("*username*") && !formattedFilename.contains("*quality*") && !formattedFilename.contains("*server_dateiname*"))) {
                formattedFilename = defaultCustomFilenameMovies;
            }
            formattedFilename = formattedFilename.replace("*zufallszahl*", randomnumber);
            formattedFilename = formattedFilename.replace("*telecastid*", telecastid);
            formattedFilename = formattedFilename.replace("*datum*", formattedDate);
            formattedFilename = formattedFilename.replace("*produktionsjahr*", produceyear);
            formattedFilename = formattedFilename.replace("*staffelnummer*", Integer.toString(seasonnumber));
            formattedFilename = formattedFilename.replace("*episodennummer*", Integer.toString(episodenumber));
            formattedFilename = formattedFilename.replace("*episodennummer_und_staffelnummer*", seasonAndEpisodenumber);
            formattedFilename = formattedFilename.replace("*endung*", ext);
            formattedFilename = formattedFilename.replace("*quality*", quality);
            formattedFilename = formattedFilename.replace("*werbefrei*", ad_free);
            formattedFilename = formattedFilename.replace("*sendername*", tv_station);
            formattedFilename = formattedFilename.replace("*kategorie*", site_category_str);
            formattedFilename = formattedFilename.replace("*genre*", genre);
            formattedFilename = formattedFilename.replace("*produktionsland*", producecountry);
            formattedFilename = formattedFilename.replace("*username*", acc_username);
            /* Insert filename at the end to prevent errors with tags */
            formattedFilename = formattedFilename.replace("*server_dateiname*", server_filename);
            formattedFilename = formattedFilename.replace("*serientitel*", site_title);
            formattedFilename = formattedFilename.replace("*episodenname*", episodename);
        }
        formattedFilename = plugin.encodeUnicode(formattedFilename);
        formattedFilename = fixCharIssues(formattedFilename);
        return formattedFilename;
    }

    /** Returns either the original server filename or one that is very similar to the original */
    @SuppressWarnings("deprecation")
    public static String getFakeOriginalFilename(final Plugin plugin, final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig(plugin.getHost());
        final String ext = downloadLink.getStringProperty(PROPERTY_type, EXTENSION_default);
        final long date = downloadLink.getLongProperty(PROPERTY_originaldate, 0l);
        String formattedDate = null;
        /* Get correctly formatted date */
        String dateFormat = "yyyy-MM-dd";
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        Date theDate = new Date(date);
        try {
            formatter = new SimpleDateFormat(dateFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent user error killing plugin */
            formattedDate = "";
        }
        /* Get correctly formatted time */
        dateFormat = "HHmm";
        String time = "0000";
        try {
            formatter = new SimpleDateFormat(dateFormat);
            time = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent user error killing plugin */
            time = "0000";
        }
        final String acc_username = cfg.getStringProperty(PROPERTY_acc_username, getRandomNumber(downloadLink));
        String formattedFilename = downloadLink.getStringProperty(PROPERTY_server_filename, null);
        if (formattedFilename != null) {
            /* Server = already original filename - no need to 'fake' anything */
            formattedFilename += EXTENSION_default;
        } else {
            final String title = convertNormalDataToServer(downloadLink.getStringProperty(PROPERTY_plainfilename, getCustomStringForEmptyTags(plugin.getHost())));
            String episodename = downloadLink.getStringProperty(PROPERTY_episodename, null);
            final int episodenumber = getEpisodeNumber(downloadLink);
            formattedFilename = title + "_";
            if (episodename != null) {
                episodename = convertNormalDataToServer(episodename);
                formattedFilename += episodename + "_";
            }
            /* Only add "Folge" if episodenumber is available */
            if (episodenumber > -1) {
                formattedFilename += "Folge" + episodenumber + "_";
            }
            formattedFilename += formattedDate + "_";
            formattedFilename += time + "_" + acc_username;
            /*
             * Finally, make sure we got no double underscores. Do this before we set the file extension es dots will be replaced within the
             * convertNormalDataToServer method!
             */
            formattedFilename = convertNormalDataToServer(formattedFilename);
            formattedFilename += ext;
            formattedFilename = plugin.encodeUnicode(formattedFilename);
        }
        formattedFilename = fixCharIssues(formattedFilename);
        return formattedFilename;
    }

    private static int getSeasonNumber(final DownloadLink dl) {
        return dl.getIntegerProperty(PROPERTY_seasonnumber, -1);
    }

    private static int getEpisodeNumber(final DownloadLink dl) {
        return dl.getIntegerProperty(PROPERTY_episodenumber, -1);
    }

    /**
     * Returns data in format S00E00. <br />
     * If only the episodenumber is available (which is often the case), it will only returns e.g. 'E00' [same for season only but I've
     * never seen this case]!
     */
    private static String getSeasonnumberAndEpisodenumber(final int seasonnumber, final int episodenumber) {
        String result;
        if (seasonnumber > -1 || episodenumber > -1) {
            result = "";
            if (seasonnumber > -1) {
                result += String.format("S%02d", seasonnumber);
            }
            if (episodenumber > -1) {
                result += String.format("E%02d", episodenumber);
            }
        } else {
            result = getCustomStringForEmptyTags(HOST_STATIC);
        }
        return result;
    }

    public static String getAdFreeText(final DownloadLink dl) {
        final String ad_free_status = dl.getStringProperty(PROPERTY_ad_free, "XX");
        return ad_free_status;
    }

    public static boolean allowNonProgrammedTelecastIDs() {
        /*
         * 2017-09-21: Set this to false for now - it might be useful in the future. In general it makes no sense to display non programmed
         * IDs as online.
         */
        return false;
    }

    /**
     * @return true: DownloadLink is a series false: DownloadLink is no series based on existing information.
     */
    private static boolean isSeries(final DownloadLink dl) {
        final String customStringForEmptyTags = getCustomStringForEmptyTags(dl.getHost());
        /* For series */
        final String episodename = dl.getStringProperty(PROPERTY_episodename, customStringForEmptyTags);
        final int episodenumber = getEpisodeNumber(dl);
        /* If we have an episodename and/or episodenumber, we have a series, category does not matter then */
        final boolean forceSeries = (!inValidate(episodename) && !episodename.equals(customStringForEmptyTags) || episodenumber != -1);
        /* Check if we have a series or movie category */
        long cat = dl.getLongProperty(PROPERTY_category, 0l);
        final boolean belongsToCategoryMovie = (cat == 0 || cat == 1 || cat == 3 || cat == 7);
        final boolean isSeries = (forceSeries || !belongsToCategoryMovie);
        return isSeries;
    }

    /**
     * Returns the user-defined string to be used for empty filename-tags.Empty filename tag = needed data is not there (null) and
     * CUSTOM_FILENAME_EMPTY_TAG_STRING will be used instead.
     */
    @SuppressWarnings("deprecation")
    public static String getCustomStringForEmptyTags(final String host) {
        final SubConfiguration cfg = SubConfiguration.getConfig(host);
        final String customStringForEmptyTags = cfg.getStringProperty(CUSTOM_FILENAME_EMPTY_TAG_STRING, defaultCustomStringForEmptyTags);
        return customStringForEmptyTags;
    }

    private static String getDownloadableVia(final DownloadLink dl) {
        return dl.getStringProperty(PROPERTY_downloadable_via, null);
    }

    /**
     * Helps to get good looking original server-filenames, correct things, before corrected by correctData, in the end data/filename should
     * be 99% close to the originals. After all this does not have to be perfect as this data is only displayed to the user (e.g. a faked
     * 'original server filename' but NEVER used e.g. as a final filename.
     */
    private static String convertNormalDataToServer(String parameter) {
        /* Corrections with spaces */
        parameter = parameter.replace(" - ", "_");
        parameter = parameter.replace(" + ", "_");
        parameter = parameter.replace("(", "_");
        parameter = parameter.replace(")", "_");
        /* Correction via replaces */
        parameter = parameter.replace(" ", "_");
        parameter = parameter.replace("é", "_");
        parameter = parameter.replace("ä", "ae");
        parameter = parameter.replace("Ä", "Ae");
        parameter = parameter.replace("ö", "oe");
        parameter = parameter.replace("Ö", "Oe");
        parameter = parameter.replace("ü", "ue");
        parameter = parameter.replace("Ü", "Ue");
        parameter = parameter.replace("ß", "ss");
        /* Correction via replace remove */
        parameter = parameter.replace("+", "");
        parameter = parameter.replace("!", "");
        parameter = parameter.replace("'", "");
        parameter = parameter.replace("\"", "");
        parameter = parameter.replace("&", "");
        parameter = parameter.replace(",", "");
        parameter = parameter.replace(".", "");
        parameter = parameter.replace("?", "");
        /* Multiple underscores do never occur in server filenames --> Fix this here */
        final String[] underscores = new Regex(parameter, "(_{2,})").getColumn(0);
        if (underscores != null && underscores.length != 0) {
            for (final String underscoress : underscores) {
                parameter = parameter.replace(underscoress, "_");
            }
        }
        return parameter;
    }

    /* Helps to get good looking custom filenames out of server filenames */
    @SuppressWarnings("unused")
    private static String convertServerDataToNormal(String parameter) {
        parameter = parameter.replace("_", " ");
        parameter = parameter.replace("ae", "ä");
        parameter = parameter.replace("AE", "Ä");
        parameter = parameter.replace("Ae", "Ä");
        parameter = parameter.replace("oe", "ö");
        parameter = parameter.replace("OE", "Ö");
        parameter = parameter.replace("Oe", "Ö");
        parameter = parameter.replace("ue", "ü");
        parameter = parameter.replace("UE", "Ü");
        parameter = parameter.replace("Ue", "Ü");
        return parameter;
    }

    /* Correct characters of serverside encoding failures */
    private static String fixCharIssues(final String input) {
        String output = input;
        /* Part 1 */
        output = output.replace("ÃŸ", "ß");
        output = output.replace("Ã„", "Ä");
        output = output.replace("Ãœ", "Ü");
        output = output.replace("Ã–", "Ö");
        /* Part 2 */
        output = output.replace("Ã¶", "ö");
        output = output.replace("Ã¤", "ä");
        output = output.replace("Ã¼", "ü");
        // output = output.replace("Ã?", "");
        return output;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    private static boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    public static String jsonobject_to_string(final Object jsonobject) {
        final String result;
        if (jsonobject == null) {
            result = null;
        } else if (jsonobject instanceof Double) {
            result = Integer.toString((int) ((Double) jsonobject).doubleValue());
        } else if (jsonobject instanceof Long) {
            result = Long.toString(((Long) jsonobject).longValue());
        } else if (jsonobject instanceof Boolean) {
            result = Boolean.toString(((Boolean) jsonobject).booleanValue());
        } else {
            result = (String) jsonobject;
        }
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download any link for this host */
            return false;
        }
        final String account_username = account.getUser();
        final String account_username_from_which_url_was_added = getDownloadableVia(downloadLink);
        return account_username_from_which_url_was_added != null && account_username != null && account_username_from_which_url_was_added.equals(account_username);
    }

    public static boolean isTypeTelecastIDOverview(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), Pattern.compile(LINKTYPE_TELECAST_ID_RECORD_OVERVIEW, Pattern.CASE_INSENSITIVE)).matches();
    }

    public static boolean isTypeTelecastID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), Pattern.compile(LINKTYPE_TELECAST_ID, Pattern.CASE_INSENSITIVE)).matches();
    }

    public static boolean isTypeTelecastIDVideoArchiveStreaming(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), Pattern.compile(LINKTYPE_TELECAST_ID_VIDEO_ARCHIVE_STREAMING, Pattern.CASE_INSENSITIVE)).matches();
    }

    public static boolean isTypeDirect(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), Pattern.compile(LINKTYPE_DIRECT, Pattern.CASE_INSENSITIVE)).matches();
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink downloadLink, final PluginForHost buildForThisPlugin) {
        if (isTypeDirect(downloadLink)) {
            /*
             * Directurls can expire --> Return Archive-Downloadurl if URL is online and telecastID overview URL if status is unknown or
             * offline
             */
            if (downloadLink.isAvailable()) {
                return buildArchiveDownloadURL(downloadLink);
            } else {
                return buildNotYetRecordedOrOfflineDownloadURL(downloadLink);
            }
        } else {
            return downloadLink.getDownloadURL();
        }
    }

    /** Returns 'normal' save.tv downloadurl. */
    private String buildArchiveDownloadURL(final DownloadLink downloadLink) {
        final String telecastID = getTelecastId(downloadLink);
        return String.format("https://www.%s/STV/M/obj/archive/VideoArchiveDetails.cfm?TelecastId=%s", this.getHost(), telecastID);
    }

    /**
     * Returns downloadurl for non-yet recorded shows or, in some cases this is also suitable for records which are already offline --> Via
     * this URL the user will still get information via browser. <br />
     * LINKTYPE_RECORD_OVERVIEW
     */
    private String buildNotYetRecordedOrOfflineDownloadURL(final DownloadLink downloadLink) {
        final String telecastID = getTelecastId(downloadLink);
        return String.format("https://www.%s/STV/M/obj/TC/SendungsDetails.cfm?TelecastId=%s", this.getHost(), telecastID);
    }

    @Override
    public String getDescription() {
        return "JDownloader's Save.tv Plugin vereinfacht das Downloaden aufgenommener Sendungen von save.tv. Es bietet viele Plugin Einstellungen.";
    }

    private final static String  defaultCustomFilenameMovies                = "*quality* ¦ *videotitel* ¦ *produktionsjahr* ¦ *telecastid**endung*";
    private final static String  defaultCustomSeriesFilename                = "*serientitel* ¦ *quality* ¦ *episodennummer_und_staffelnummer* ¦ *episodenname* ¦ *telecastid*|*datum*|*endung*";
    private final static String  defaultCustomSeperationMark                = "+";
    private final static String  defaultCustomStringForEmptyTags            = "-";
    private final static int     defaultCrawlLastDays                       = 0;
    private final static int     defaultADS_FREE_UNAVAILABLE_HOURS          = 24;
    private static final boolean defaultCrawlerActivate                     = false;
    public static final boolean  defaultCrawlerActivateInformationDialogs   = true;
    private static final boolean defaultCrawlerFastLinkcheck                = true;
    private static final boolean defaultCrawlerAddNew                       = false;
    private static final boolean defaultPreferAdsFree                       = true;
    private static final boolean defaultUseOriginalFilename                 = false;
    private static final String  defaultCustomDate                          = "dd.MM.yyyy";
    private static final boolean defaultACTIVATE_BETA_FEATURES              = false;
    public static final boolean  defaultUSEAPI                              = true;
    private static final boolean defaultDeleteTelecastIDAfterDownload       = false;
    private static final boolean defaultDeleteTelecastIDIfFileAlreadyExists = false;
    private static final String  ACCOUNTTYPE_UNKNOWN                        = "Unbekanntes Paket";

    private void setConfigElements() {
        /* Crawler settings */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Archiv-Crawler Einstellungen:"));
        final ConfigEntry crawlerActivate = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.CRAWLER_ACTIVATE, JDL.L("plugins.hoster.SaveTv.activateCrawler", "Archiv-Crawler aktivieren?\r\nINFO: Fügt das komplette Archiv oder Teile davon beim Einfügen dieses Links ein:\r\n'https://www.save.tv/STV/M/obj/archive/VideoArchive.cfm\r\n")).setDefaultValue(defaultCrawlerActivate);
        getConfig().addEntry(crawlerActivate);
        final ConfigEntry crawlerAddNew = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.CRAWLER_ONLY_ADD_NEW_IDS, JDL.L("plugins.hoster.SaveTv.crawlerOnlyAddNewIDs", "Nur neue abgeschlossene Aufnahmen crawlen?\r\nJDownloader gleicht dein save.tv Archiv ab mit den Einträgen, die du bereits eingefügt hast und zeigt immer nur neue Einträge an!\r\n<html><b>Wichtig:</b> JDownloader kann nicht wissen, welche Sendungen du bereits geladen hast - nur, welche bereits in JDownloader eingefügt wurden!</html>")).setDefaultValue(defaultCrawlerAddNew).setEnabledCondidtion(crawlerActivate, true);
        getConfig().addEntry(crawlerAddNew);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), SaveTv.CRAWLER_GRAB_TIMEFRAME_COUNT, "Nur Aufnahmen der letzten X Tage crawlen??\r\nAnzahl der Tage, die gecrawlt werden sollen [0 = komplettes Archiv]:", 0, 62, 1).setDefaultValue(defaultCrawlLastDays).setEnabledCondidtion(crawlerAddNew, false));
        /*
         * 2017-09-21: Disable this setting as we're switching to API-only mode and API provides all required information straight away so
         * this is not required anymore.
         */
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.CRAWLER_ENABLE_FAST_LINKCHECK,
        // "Aktiviere schnellen Linkcheck für Archiv-Crawler?\r\nVorteil: Über den Archiv-Crawler hinzugefügte Links landen viel schneller
        // im Linkgrabber\r\nNachteil: Es sind nicht alle Informationen (z.B. Kategorie) verfügbar - erst beim Download oder späterem
        // Linkcheck\r\n").setDefaultValue(defaultCrawlerFastLinkcheck).setEnabledCondidtion(crawlerActivate, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.CRAWLER_ENABLE_DIALOGS, "<html>Info Dialoge des Archiv-Crawlers aktivieren?<br />[Erscheinen nach dem Crawlen oder im Fehlerfall] </html>").setDefaultValue(defaultCrawlerActivateInformationDialogs).setEnabledCondidtion(crawlerActivate, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Format & Quality settings */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Format & Qualitäts-Einstellungen:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SELECTED_VIDEO_FORMAT, FORMATS, JDL.L("plugins.hoster.SaveTv.prefer_format", "Bevorzugtes Format (ist dieses nicht verfügbar, wird das beste verfügbare genommen):")).setDefaultValue(0));
        final ConfigEntry preferAdsFree = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.PROPERTY_PREFERADSFREE, JDL.L("plugins.hoster.SaveTv.PreferAdFreeVideos", "Aufnahmen mit angewandter Schnittliste bevorzugen?")).setDefaultValue(defaultPreferAdsFree);
        getConfig().addEntry(preferAdsFree);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), SaveTv.ADS_FREE_UNAVAILABLE_HOURS, "Download von Aufnahmen ohne Schnittliste erzwingen, sofern X Stunden nach Aufnahmedatum keine Schnittliste verfügbar ist? <html><b><br />[0 = nie erzwingen = ausschließlich werbefreie Aufnahmen herunterladen]</b></html>", 0, 720, 24).setDefaultValue(defaultADS_FREE_UNAVAILABLE_HOURS).setEnabledCondidtion(preferAdsFree, true));
        /* Filename settings */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Dateiname Einstellungen:"));
        final ConfigEntry origName = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.PROPERTY_USEORIGINALFILENAME, JDL.L("plugins.hoster.SaveTv.UseOriginalFilename", "Original (Server) Dateinamen verwenden? <html><b>[Erst beim Downloadstart sichtbar!]</b></html>")).setDefaultValue(defaultUseOriginalFilename);
        getConfig().addEntry(origName);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.savetv.customdate", "Setze das Datumsformat:\r\nWichtige Information dazu:\r\nDas Datum erscheint im angegebenen Format im Dateinamen, allerdings nur,\r\nwenn man das *datum* Tag auch verwendet (siehe Benutzerdefinierte Dateinamen für Filme und Serien unten)")).setDefaultValue(defaultCustomDate).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* General settings description */
        final StringBuilder description_custom_filenames_howto = new StringBuilder();
        description_custom_filenames_howto.append("<html><b>Erklärung zur Nutzung eigener Dateinamen:</b><br />");
        description_custom_filenames_howto.append("Eigene Dateinamen lassen sich unten über ein Tag-System (siehe weiter unten) nutzen.<br />");
        description_custom_filenames_howto.append("Das bedeutet, dass man die Struktur seiner gewünschten Dateinamen definieren kann.<br />");
        description_custom_filenames_howto.append("Dabei hat man Tags wie z.B. *telecastid*, die dann durch Daten ersetzt werden.<br />");
        description_custom_filenames_howto.append("Wichtig dabei ist, dass Tags immer mit einem Stern starten und enden.<br />");
        description_custom_filenames_howto.append("Man darf nichts zwischen ein Tag schreiben z.B. *-telecastid-*, da das<br />");
        description_custom_filenames_howto.append("zu unschönen Dateinamen führt und das Tag nicht die Daten ersetzt werden kann.<br />");
        description_custom_filenames_howto.append("Wenn man die Tags trennen will muss man die anderen Zeichen zwischen Tags<br />");
        description_custom_filenames_howto.append("z.B. '-*telecastid*-*endung*' -> Der Dateiname würde dann in etwa so aussehen: '-7573789-.mp4' (ohne die '')<br />");
        description_custom_filenames_howto.append("<b>WICHTIG:</b> Tags, zu denen die Daten fehlen, werden standardmäßig durch '-' (Bindestrich) ersetzt!<br />");
        description_custom_filenames_howto.append("Fehlen z.B. die Daten zu *genre*, steht statt statt dem Genre dann ein Bindestrich ('-') an dieser Stelle im Dateinamen.<br />");
        description_custom_filenames_howto.append("Gut zu wissen: Statt dem Bindestrich lässt sich hierfür unten auch ein anderes Zeichen bzw. Zeichenfolge definieren.<br />");
        description_custom_filenames_howto.append("Außerdem: Für Filme und Serien gibt es unterschiedliche Tags.<br />");
        description_custom_filenames_howto.append("Kaputtmachen kannst du mit den Einstellungen prinzipiell nichts also probiere es einfach aus ;)<br />");
        description_custom_filenames_howto.append("<b>Tipp:</b> Die Save.tv Plugin Einstellungen lassen sich rechts oben wieder auf ihre Standardwerte zurücksetzen!<br />");
        description_custom_filenames_howto.append("</html>");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, description_custom_filenames_howto.toString()).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final StringBuilder description_custom_filenames_tags = new StringBuilder();
        description_custom_filenames_tags.append("<html><b>Erklärung der <u>allgemein</u> verfügbaren Tags:</b><br />");
        description_custom_filenames_tags.append("*server_dateiname* = Original Dateiname (ohne Dateiendung)<br />");
        description_custom_filenames_tags.append("*username* = Benutzername<br />");
        description_custom_filenames_tags.append("*datum* = Datum der Ausstrahlung der aufgenommenen Sendung<br />[Erscheint im oben definierten Format, wird von der save.tv Seite ausgelesen]<br />");
        description_custom_filenames_tags.append("*genre* = Das Genre<br />");
        description_custom_filenames_tags.append("*produktionsland* = Name des Produktionslandes<br />");
        description_custom_filenames_tags.append("*produktionsjahr* = Produktionsjahr<br />");
        description_custom_filenames_tags.append("*sendername* = Name des TV-Senders auf dem die Sendung ausgestrahlt wurde");
        description_custom_filenames_tags.append("*kategorie* = Kategorie, siehe telecast-ID Seite<br />");
        description_custom_filenames_tags.append("*quality* = Qualitätsstufe des Downloads - Entspricht den Werten 'LQ', 'HQ', 'HD' oder 'XX' für den unbekannten Status<br />");
        description_custom_filenames_tags.append("*werbefrei* = Schnittliste-Status des Downloads - Entspricht den Werten 'true', 'false' oder 'XX' für den unbekannten Status<br />");
        description_custom_filenames_tags.append("*zufallszahl* = Eine vierstellige Zufallszahl<br />[Nützlich um Dateinamenkollisionen zu vermeiden]<br />");
        description_custom_filenames_tags.append("*telecastid* = Die id, die in jedem save.tv Link steht: TelecastID=XXXXXXX<br />[Nützlich um Dateinamenkollisionen zu vermeiden]<br />");
        description_custom_filenames_tags.append("*endung* = Die Dateiendung, in diesem Fall immer '.mp4'<br />");
        /* Description of tags for movies ONLY */
        description_custom_filenames_tags.append("<b>Erklärung der <u>nur für Filme</u> verfügbaren Tags:</b><br />");
        description_custom_filenames_tags.append("*videotitel* = Name des Videos ohne Dateiendung<br />");
        /* Description of tags for series ONLY */
        description_custom_filenames_tags.append("<b>Erklärung der <u>nur für Serien</u> verfügbaren Tags:</b><br />");
        description_custom_filenames_tags.append("*serientitel* = Name der Serie<br />");
        description_custom_filenames_tags.append("*episodenname* = Name der Episode<br />");
        description_custom_filenames_tags.append("*staffelnummer* = Staffelnummer<br />");
        description_custom_filenames_tags.append("*episodennummer* = Episodennummer<br />");
        description_custom_filenames_tags.append("*episodennummer_und_staffelnummer* = Episodennummer mit Staffelnummer z.B. 'S01E03'<br />");
        description_custom_filenames_tags.append("</html>");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, description_custom_filenames_tags.toString()).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Filename settings for movies */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_MOVIES, JDL.L("plugins.hoster.savetv.customfilenamemovies", "Eigener Dateiname für Filme/Shows:")).setDefaultValue(defaultCustomFilenameMovies).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Filename settings for series */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_SERIES, JDL.L("plugins.hoster.savetv.customseriesfilename", "Eigener Dateiname für Serien:")).setDefaultValue(defaultCustomSeriesFilename).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Advanced settings */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Erweiterte Einstellungen:\r\n<html><p style=\"color:#F62817\"><b>Warnung: Ändere die folgenden Einstellungen nur, wenn du weißt was du tust!\r\nMit einem Klick auf den gelben Pfeil rechts oben kannst du jederzeit zu den Standardeinstellungen zurück.</b></p></html>"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.ACTIVATE_BETA_FEATURES,
        // JDL.L("plugins.hoster.SaveTv.ActivateBETAFeatures", "Aktiviere BETA-Features?\r\nINFO: Was diese Features sind und ob es aktuell
        // welche gibt steht im Support Forum.")).setEnabled(defaultACTIVATE_BETA_FEATURES));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry api = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.USEAPI, JDL.L("plugins.hoster.SaveTv.UseAPI", "<html><p>APIv3 verwenden?</p><p>INFO: Aktiviert man die API, sind einige Features wie folgt betroffen:</p>-ENTFÄLLT: Anzeigen des Account-Ablaufdatums in der Accountverwaltung</p></html>")).setDefaultValue(defaultUSEAPI);
        getConfig().addEntry(api);
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CONFIGURED_APIKEY,
        // JDL.L("plugins.hoster.SaveTv.apikey", "Benutzerdefinierten API-Key eingeben:\r\n<html><p style=\"color:#F62817\"><b>Warnung:</b>
        // Die API ist nur mit gültigem API-Key (in der Regel mit den Standardeinstellungen)
        // nutzbar!</p></html>")).setDefaultValue(defaultCONFIGURED_APIKEY).setEnabledCondidtion(api, true).setEnabled(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Soll die telecastID in bestimmten Situationen aus dem save.tv Archiv gelöscht werden?\r\n<html><p style=\"color:#F62817\"><b>Warnung:</b> Gelöschte telecastIDs können nicht wiederhergestellt werden!</p></html>"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.DELETE_TELECAST_ID_AFTER_DOWNLOAD, "Erfolgreich geladene telecastIDs aus dem save.tv Archiv löschen?").setDefaultValue(defaultDeleteTelecastIDAfterDownload));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.DELETE_TELECAST_ID_IF_FILE_ALREADY_EXISTS, "Falls Datei bereits auf der Festplatte existiert, telecastIDs aus dem save.tv Archiv löschen?").setDefaultValue(defaultDeleteTelecastIDIfFileAlreadyExists));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_API_PARAMETERS_CRAWLER, "Crawler: eigene API Parameter definieren (alles außer 'limit,fields,nopagingheader,paging,offset') [urlEncoded]:\r\nBeispiel: 'tags=record:manual&fsk=6'\r\nWeitere Informationen siehe: api.save.tv/v3/docs/index#!/Records_|_get/Records_Get<html><p style=\"color:#F62817\"><b>Warnung:</b> Falsche Werte können den Crawler 'kaputtmachen' und andere Crawler-Einstellungen beeinflussen (ggf. Einstellungen zurücksetzen)!</p></html>").setDefaultValue(null).setEnabledCondidtion(api, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_SEPERATION_MARK, "Trennzeichen als Ersatz für '/'  (da ungültig in Dateinamen):").setDefaultValue(defaultCustomSeperationMark).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_EMPTY_TAG_STRING, "Zeichen, mit dem Tags ersetzt werden sollen, deren Daten fehlen:").setDefaultValue(defaultCustomStringForEmptyTags).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final StringBuilder sbmore = new StringBuilder();
        sbmore.append("<html>Definiere Filme oder Serien, für die trotz obiger Einstellungen die Originaldateinamen<br />");
        sbmore.append("verwendet werden sollen.<br />");
        sbmore.append("Manche mehrteiligen Filme haben dieselben Titel und bei manchen Serien fehlen die Episodennamen,<br />");
        sbmore.append("wodurch sie alle dieselben Dateinamen bekommen -> JDownloader denkt es seien Duplikate/Mirrors und lädt nur<br />");
        sbmore.append("einen der scheinbar gleichen Dateien.<br />");
        sbmore.append("Um dies zu verhindern, kann man in den Eingabefeldern Namen solcher Filme/Serien eintragen,<br />");
        sbmore.append("für die trotz obiger Einstellungen der Original Dateiname verwendet werden soll.<br />");
        sbmore.append("Beispiel: 'serienname 1|serienname 2|usw.' (ohne die '')<br />");
        sbmore.append("Die Eingabe erfolgt als RegEx. Wer nicht weiß was das ist -> Google</html>");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbmore.toString()).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), FORCE_ORIGINALFILENAME_SERIES, JDL.L("plugins.hoster.savetv.forceoriginalnameforspecifiedseries", "Original Dateinamen für folgende Serien erzwingen [Eingabe erfolgt in RegEx]:")).setDefaultValue("").setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), FORCE_ORIGINALFILENAME_MOVIES, JDL.L("plugins.hoster.savetv.forcefilenameforspecifiedmovies", "Original Dateinamen für folgende Filme erzwingen [Eingabe erfolgt in RegEx]:")).setDefaultValue("").setEnabledCondidtion(origName, false));
    }

    private static String getMessageEnd() {
        String message = "";
        message += "\r\n\r\n";
        message += "Falls du Fehler findest oder Fragen hast, melde dich jederzeit gerne bei uns im Supportforum:\r\nhttp://board.jdownloader.org/\r\n";
        message += "\r\n";
        message += "Dieses Fenster wird nur einmal angezeigt.\r\nAlle wichtigen Informationen stehen auch in den save.tv Plugin Einstellungen.\r\n";
        message += "\r\n";
        message += "- Das JDownloader Team wünscht weiterhin viel Spaß mit JDownloader und save.tv! -";
        return message;
    }

    @SuppressWarnings("deprecation")
    private void checkAccountNeededDialog() {
        synchronized (LOCK) {
            SubConfiguration config = null;
            try {
                config = getPluginConfig();
                if (config.getBooleanProperty("accNeededShown", Boolean.FALSE) == false) {
                    if (config.getProperty("accNeededShown2") == null) {
                        showAccNeededDialog();
                    } else {
                        config = null;
                    }
                } else {
                    config = null;
                }
            } catch (final Throwable e) {
            } finally {
                if (config != null) {
                    config.setProperty("accNeededShown", Boolean.TRUE);
                    config.setProperty("accNeededShown2", "shown");
                    config.save();
                }
            }
        }
    }

    private static void showAccNeededDialog() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message = "";
                        String title = null;
                        title = "Save.tv - Account benötigt";
                        message += "Hallo lieber save.tv Nutzer.\r\n";
                        message += "Um über JDownloader Videos aus deinem save.tv Archiv herunterladen zu können musst du\r\nzunächst deinen save.tv Account in JDownloader eintragen.";
                        message += "\r\n";
                        message += "Das geht unter:\r\n";
                        message += "Einstellungen -> Accountverwaltung -> Hinzufügen -> save.tv\r\n";
                        message += "\r\n";
                        message += "Sobald du deinen Account eingetragen hast kannst du aus deinem save.tv Archiv\r\n";
                        message += "Links dieses Formats in JDownloader einfügen und herunterladen:\r\n";
                        message += "https://www.save.tv/STV/M/obj/archive/VideoArchive.cfm";
                        message += getMessageEnd();
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    @SuppressWarnings("deprecation")
    private void checkFeatureDialogAll() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("featuredialog_all_Shown", Boolean.FALSE) == false) {
                if (config.getProperty("featuredialog_all_Shown2") == null) {
                    showFeatureDialogAll();
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("featuredialog_all_Shown", Boolean.TRUE);
                config.setProperty("featuredialog_all_Shown2", "shown");
                config.save();
            }
        }
    }

    private static void showFeatureDialogAll() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message = "";
                        String title = null;
                        title = "Save.tv Plugin - Features";
                        message += "Hallo lieber save.tv Nutzer/liebe save.tv Nutzerin\r\n";
                        message += "Das save.tv Plugin bietet folgende Features:\r\n";
                        message += "- Automatisierter Download von save.tv Links (telecast-IDs)\r\n";
                        message += "- Laden des kompletten save.tv Archivs über wenige Klicks\r\n";
                        message += "-- > Oder wahlweise nur alle Links der letzten X Tage/Stunden\r\n";
                        message += "- Benutzerdefinierte Dateinamen über ein Tag-System mit vielen Möglichkeiten\r\n";
                        message += "- Alles unter beachtung der Schnittlisten-Einstellungen und des Formats\r\n";
                        message += "- Und viele mehr...\r\n";
                        message += "\r\n";
                        message += "Diese Einstellungen sind in der Version JDownloader 2 verfügbar unter:\r\nEinstellungen -> Plugin Einstellungen -> save.tv";
                        message += getMessageEnd();
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    @SuppressWarnings("deprecation")
    private void checkFeatureDialogCrawler() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("featuredialog_crawler_Shown", Boolean.FALSE) == false) {
                if (config.getProperty("featuredialog_crawler_Shown2") == null) {
                    showFeatureDialogCrawler();
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("featuredialog_crawler_Shown", Boolean.TRUE);
                config.setProperty("featuredialog_crawler_Shown2", "shown");
                config.save();
            }
        }
    }

    private static void showFeatureDialogCrawler() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message = "";
                        String title = null;
                        title = "Save.tv Plugin - Crawler Features";
                        message += "Hallo lieber save.tv Nutzer/liebe save.tv NutzerIn\r\n";
                        message += "Das save.tv Crawler Plugin bietet folgende Features:\r\n";
                        message += "- Info-Dialoge\r\n";
                        message += "- Die Möglichkeit, immer nur neue abgeschlossene Aufnahmen zu crawlen\r\n";
                        message += "- Die Möglichkeit, wahlweise alle oder nur Aufnahmen der letzten X Stunden zu crawlen\r\n";
                        message += "\r\n";
                        message += "Um den Crawler nutzen zu können, musst du ihn erst in den Plugin Einstellungen aktivieren.\r\n";
                        message += "\r\n";
                        message += "Die Crawler Einstellungen sind in der Version JDownloader 2 verfügbar unter:\r\nEinstellungen -> Plugin Einstellungen -> save.tv";
                        message += getMessageEnd();
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    @SuppressWarnings("deprecation")
    private void checkFeatureDialogNew() {
        final String featureDialogProperty = "featuredialog_new20092017_Shown";
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty(featureDialogProperty, Boolean.FALSE) == false) {
                if (config.getProperty(featureDialogProperty + "2") == null) {
                    showFeatureDialogNew();
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty(featureDialogProperty, Boolean.TRUE);
                config.setProperty(featureDialogProperty + "2", "shown");
                config.save();
            }
        }
    }

    private static void showFeatureDialogNew() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message = "";
                        String title = null;
                        title = "20.09.2017 - Save.tv Plugin - Neue Features";
                        message += "Hallo lieber save.tv Nutzer/liebe save.tv NutzerIn\r\n";
                        message += "Das save.tv Plugin bietet neben Fehlerbehebungen seit dem 20.09.2017 folgende neue Features:\r\n";
                        message += "- Abgeschlossene Downloads bekommen einen 'Download vollständig' Verweis; zu sehen auf der telecastID Save.tv Seite unter 'Tags' (da wo auch sowas steht wie 'Länge werbefreie Aufnahme: 22:56')\r\n";
                        message += "- Einstellung zum erzwungenen Download ohne Schnittliste (falls keine verfügbar) wurde vereinfacht\r\n";
                        message += "- Berechnung der Dateigröße ist nun viel genauer[bis auf 10 MB]\r\n";
                        message += "- Und vieles mehr ...\r\n";
                        message += "\r\n";
                        message += "Die Plugin Einstellungen sind in der Version JDownloader 2 verfügbar unter:\r\nEinstellungen -> Plugin Einstellungen -> save.tv";
                        message += getMessageEnd();
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    @Override
    public void extendAccountSettingsPanel(Account account, PluginConfigPanelNG panel) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            final String accType = account.getStringProperty(PROPERTY_acc_type, ACCOUNTTYPE_UNKNOWN);
            final String accUsername = account.getStringProperty(PROPERTY_acc_username, "?");
            String acc_expire = "Unbekannt";
            final String acc_package = account.getStringProperty(PROPERTY_acc_package, "?");
            final String acc_price = account.getStringProperty(PROPERTY_acc_price, "?");
            final String acc_runtime = account.getStringProperty(PROPERTY_acc_runtime, "?");
            final String acc_count_archive_entries = account.getStringProperty(PROPERTY_acc_count_archive_entries, "?");
            final String acc_count_telecast_ids = account.getStringProperty(PROPERTY_acc_count_telecast_ids, "?");
            final String user_lastcrawl_newlinks_date;
            final String user_lastcrawl_date;
            final long time_last_crawl_ended_newlinks = account.getLongProperty(CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS, 0);
            final long time_last_crawl_ended = account.getLongProperty(CRAWLER_PROPERTY_LASTCRAWL, 0);
            final String maxchunks;
            if (ACCOUNT_PREMIUM_MAXCHUNKS == 0) {
                maxchunks = "20";
            } else if (ACCOUNT_PREMIUM_MAXCHUNKS < 1) {
                maxchunks = Integer.toString(-ACCOUNT_PREMIUM_MAXCHUNKS);
            } else {
                maxchunks = Integer.toString(ACCOUNT_PREMIUM_MAXCHUNKS);
            }
            final SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy 'um' HH:mm 'Uhr'");
            if (time_last_crawl_ended_newlinks > 0 && time_last_crawl_ended > 0) {
                user_lastcrawl_date = formatter.format(time_last_crawl_ended);
                user_lastcrawl_newlinks_date = formatter.format(time_last_crawl_ended_newlinks);
            } else {
                user_lastcrawl_date = "Nie";
                user_lastcrawl_newlinks_date = "Nie";
            }
            if (account.getAccountInfo().getValidUntil() > 0) {
                acc_expire = formatter.format(account.getAccountInfo().getValidUntil());
            }
            panel.addStringPair(_GUI.T.lit_username(), accUsername);
            panel.addStringPair(_GUI.T.lit_account_type(), accType);
            panel.addStringPair(_GUI.T.lit_package(), acc_package);
            panel.addStringPair(_GUI.T.lit_runtime(), acc_runtime);
            panel.addStringPair(_GUI.T.lit_expire_date(), acc_expire);
            panel.addStringPair(_GUI.T.lit_price(), acc_price);
            panel.addStringPair("Sendungen im Archiv:", acc_count_archive_entries);
            panel.addStringPair("Ladbare Sendungen im Archiv (telecast-IDs):", acc_count_telecast_ids);
            panel.addStringPair("Datum des letzten erfolgreichen Crawlvorganges: ", user_lastcrawl_date);
            panel.addStringPair("Zuletzt erfolgreich telecastIDs per Crawler hinzugefügt:", user_lastcrawl_newlinks_date);
            panel.addHeader(_GUI.T.lit_download(), new AbstractIcon(IconKey.ICON_DOWNLOAD, 18));
            panel.addStringPair(_GUI.T.lit_max_simultanous_downloads(), "20");
            panel.addStringPair(_GUI.T.lit_max_chunks_per_link(), maxchunks);
            panel.addStringPair(_GUI.T.lit_interrupted_downloads_are_resumable(), _JDT.T.literally_yes());
        }
    }
}