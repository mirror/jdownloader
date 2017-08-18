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

import java.io.IOException;
import java.net.UnknownHostException;
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
import org.appwork.utils.formatter.SizeFormatter;
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
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookies;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "save.tv" }, urls = { "https?://(?:www\\.)?save\\.tv/STV/M/obj/(?:archive/VideoArchiveDetails|TC/SendungsDetails)\\.cfm\\?TelecastID=\\d+(?:\\&adsfree=(?:true|false|unset))?(?:\\&preferformat=[3456])?|https?://[A-Za-z0-9\\-]+\\.save\\.tv/\\d+_\\d+_.+" })
public class SaveTv extends PluginForHost {

    /* Static information */
    // private static final String API_APP_NAME = "JDownloader";
    private static final String   API_PUBLIC_KEY                            = "NOT_YET_DONE";
    private static final String   API_SECRET_KEY                            = "NOT_YET_DONE";
    /*
     * Doc of an eventually soon existing new (finally public) API [Date added: 2015-06-25]: https://api.save.tv/v3/docs/index
     */
    /* Normal url */
    private final String          LINKTYPE_NORMAL                           = ".+/STV/M/obj/archive/VideoArchiveDetails\\.cfm\\?TelecastID=\\d+";
    /* User has programmed something but it has not aired yet (is not downloadable yet) --> We will change these urls to LINKTYPE_NORMAL */
    private final String          LINKTYPE_MAYBE_NOT_YET_DOWNLOADABLE       = ".+/STV/M/obj/TC/SendungsDetails\\.cfm\\?TelecastID=\\d+";
    /* Direct url --> We will change these to LINKTYPE_NORMAL */
    private final static String   LINKTYPE_DIRECT                           = "https?://[A-Za-z0-9\\-]+\\.save\\.tv/\\d+_\\d+_.+";
    public static final String    API_BASE                                  = "https://api.save.tv/v3";
    public static final double    QUALITY_HD_MB_PER_MINUTE                  = 22;
    public static final double    QUALITY_H264_NORMAL_MB_PER_MINUTE         = 12.605;
    public static final double    QUALITY_H264_MOBILE_MB_PER_MINUTE         = 4.64;
    private final static String   COOKIE_HOST                               = "http://save.tv";
    private static final String   NICE_HOST                                 = "save.tv";
    /* Properties */
    private static final String   CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS       = "CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS";
    private static final String   CRAWLER_PROPERTY_LASTCRAWL                = "CRAWLER_PROPERTY_LASTCRAWL";
    /* Frequently used internal plugin properties */
    public static final String    PROPERTY_ACCOUNT_API_SESSIONID            = "sessionid";
    private static final String   PROPERTY_DOWNLOADLINK_NORESUME            = "NORESUME";
    private static final String   PROPERTY_DOWNLOADLINK_NOCHUNKS            = "NOCHUNKS";
    private static final String   PROPERTY_DOWNLOADLINK_ADSFREEFAILED_COUNT = "current_no_ads_free_available_retries";
    public static final String    PROPERTY_quality                          = "quality";
    public static final String    PROPERTY_plainfilename                    = "plainfilename";
    public static final String    PROPERTY_server_filename                  = "server_filename";
    public static final String    PROPERTY_acc_username                     = "acc_username";
    public static final String    PROPERTY_ad_free                          = "ad_free";
    public static final String    PROPERTY_producecountry                   = "producecountry";
    public static final String    PROPERTY_genre                            = "genre";
    public static final String    PROPERTY_type                             = "type";
    public static final String    PROPERTY_produceyear                      = "produceyear";
    public static final String    PROPERTY_plain_tv_station                 = "plain_tv_station";
    public static final String    PROPERTY_plain_site_category              = "plain_site_category";
    public static final String    PROPERTY_episodename                      = "episodename";
    public static final String    PROPERTY_originaldate                     = "originaldate";
    public static final String    PROPERTY_episodenumber                    = "episodenumber";
    public static final String    PROPERTY_acc_count_telecast_ids           = "acc_count_telecast_ids";
    public static final String    PROPERTY_acc_type                         = "acc_type";
    public static final String    PROPERTY_acc_count_archive_entries        = "acc_count_archive_entries";
    public static final String    PROPERTY_lastuse                          = "lastuse";
    public static final String    PROPERTY_category                         = "category";
    public static final String    PROPERTY_stv_randomnumber                 = "stv_randomnumber";
    public static final String    PROPERTY_originaldate_end                 = "originaldate_end";
    public static final String    PROPERTY_site_runtime_minutes             = "site_runtime_minutes";
    public static final String    PROPERTY_acc_expire                       = "acc_expire";
    public static final String    PROPERTY_acc_package                      = "acc_package";
    public static final String    PROPERTY_acc_price                        = "acc_price";
    public static final String    PROPERTY_acc_runtime                      = "acc_runtime";
    public static final String    PROPERTY_downloadable_via                 = "downloadable_via";
    /* Settings stuff */
    private static final String   USEORIGINALFILENAME                       = "USEORIGINALFILENAME";
    private static final String   PREFERADSFREE                             = "PREFERADSFREE";
    private static final String   ADS_FREE_UNAVAILABLE_MAXRETRIES           = "ADS_FREE_UNAVAILABLE_MAXRETRIES";
    private static final String   FORCE_WITH_ADS_ON_ERROR_HOURS             = "FORCE_WITH_ADS_ON_ERROR_HOURS_2";
    private static final String   FORCE_WITH_ADS_ON_ERROR_MAXRETRIES        = "FORCE_WITH_ADS_ON_ERROR_HOURS_MAXRETRIES_3";
    private static final String   ADS_FREE_UNAVAILABLE_HOURS                = "DOWNLOADONLYADSFREE_RETRY_HOURS";
    private final static String   SELECTED_VIDEO_FORMAT                     = "selected_video_format";
    private static final String   USERTEXT_PREFERREDFORMATNOTAVAILABLE      = "Das bevorzugte Format ist (noch) nicht verfügbar. Warte oder ändere die Einstellung!";
    /* Text strings displayed to the user in various cases */
    private final String          USERTEXT_ADSFREEAVAILABLE                 = "Video ist werbefrei verfügbar";
    private final String          USERTEXT_ADSFREEANOTVAILABLE              = "Video ist nicht werbefrei verfügbar";
    private final static String   USERTEXT_NOCUTAVAILABLE                   = "Für diese Sendung steht (noch) keine Schnittliste zur Verfügung";
    /* The list of qualities/formats displayed to the user */
    private static final String[] FORMATS                                   = new String[] { "HD", "H.264 HQ", "H.264 MOBILE" };
    /* Crawler settings */
    private static final String   CRAWLER_ONLY_ADD_NEW_IDS                  = "CRAWLER_ONLY_ADD_NEW_IDS";
    private static final String   ACTIVATE_BETA_FEATURES                    = "ACTIVATE_BETA_FEATURES";
    private static final String   USEAPI                                    = "USEAPI";
    private static final String   CONFIGURED_APIKEY                         = "CONFIGURED_APIKEY";
    private static final String   CRAWLER_ACTIVATE                          = "CRAWLER_ACTIVATE";
    private static final String   CRAWLER_ENABLE_FASTER                     = "CRAWLER_ENABLE_FASTER_2";
    private static final String   CRAWLER_DISABLE_DIALOGS                   = "CRAWLER_DISABLE_DIALOGS";
    private static final String   CRAWLER_LASTHOURS_COUNT                   = "CRAWLER_LASTHOURS_COUNT";
    private static final String   DISABLE_LINKCHECK                         = "DISABLE_LINKCHECK";
    private static final String   DELETE_TELECAST_ID_AFTER_DOWNLOAD         = "DELETE_TELECAST_ID_AFTER_DOWNLOAD";
    private static final String   DELETE_TELECAST_ID_IF_FILE_ALREADY_EXISTS = "DELETE_TELECAST_ID_IF_FILE_ALREADY_EXISTS";
    /* Custom filename settings stuff */
    private static final String   CUSTOM_DATE                               = "CUSTOM_DATE";
    private static final String   CUSTOM_FILENAME_MOVIES                    = "CUSTOM_FILENAME_MOVIES";
    private static final String   CUSTOM_FILENAME_SERIES                    = "CUSTOM_FILENAME_SERIES_3";
    private static final String   CUSTOM_FILENAME_SEPERATION_MARK           = "CUSTOM_FILENAME_SEPERATION_MARK";
    private static final String   CUSTOM_FILENAME_EMPTY_TAG_STRING          = "CUSTOM_FILENAME_EMPTY_TAG_STRING";
    private static final String   FORCE_ORIGINALFILENAME_SERIES             = "FORCE_ORIGINALFILENAME_SERIES";
    private static final String   FORCE_ORIGINALFILENAME_MOVIES             = "FORCE_ORIGINALFILENAME_MOVIES";
    /* Variables */
    private boolean               FORCE_LINKCHECK                           = false;
    private boolean               ISADSFREEAVAILABLE                        = false;
    /* If this != null, API can be used */
    private String                API_SESSIONID                             = null;
    /* Download connections constants */
    private static final boolean  ACCOUNT_PREMIUM_RESUME                    = true;
    private static final int      ACCOUNT_PREMIUM_MAXCHUNKS                 = -2;
    private static final int      ACCOUNT_PREMIUM_MAXDOWNLOADS              = -1;
    private static final int      FREE_MAXDOWNLOADS                         = -1;
    /* Other API/site errorhandling constants */
    private static final String   HTML_SITE_DL_IMPOSSIBLE                   = ">Diese Sendung kann leider nicht heruntergeladen werden, da die Aufnahme fehlerhaft ist";
    private static final String   HTML_API_DL_IMPOSSIBLE                    = ">1418</ErrorCodeID>";
    public static final String    URL_LOGGED_OUT                            = "Token=MSG_LOGOUT_B";
    private static final String   DL_IMPOSSIBLE_USER_TEXT                   = "Aufnahme fehlerhaft - Download momentan nicht möglich";
    /* Property / Filename constants / States / Small user display texts */
    public static final String    STATE_QUALITY_LQ                          = "LQ";
    public static final String    STATE_QUALITY_HQ                          = "HQ";
    public static final String    STATE_QUALITY_HD                          = "HD";
    public static final String    STATE_QUALITY_UNKNOWN                     = "XX";
    public static final String    STATE_ad_free_true                        = "true";
    public static final String    STATE_ad_free_false                       = "false";
    public static final String    STATE_ad_free_unknown                     = "XX";
    public static final String    EXTENSION_default                         = ".mp4";
    /* Save.tv internal quality/format constants (IDs) for the API */
    private static final String   API_FORMAT_HD                             = "6";
    private static final String   API_FORMAT_HQ                             = "5";
    private static final String   API_FORMAT_LQ                             = "4";
    /*
     * In some cases it is good to have these values in Long. Also note that, except from the website download request they now seem to use
     * the same values for the website which they use for the API!
     */
    private static final long     API_FORMAT_HD_L                           = 6;
    private static final long     API_FORMAT_HQ_L                           = 5;
    private static final long     API_FORMAT_LQ_L                           = 4;
    /* Save.tv internal quality/format constants (IDs) for the website */
    private static final String   SITE_FORMAT_HD                            = "6";
    private static final String   SITE_FORMAT_HQ                            = "5";
    private static final String   SITE_FORMAT_LQ                            = "4";
    /*
     * In some cases it is good to have these values in Long.
     */
    private static final long     SITE_FORMAT_HD_L                          = 6;
    private static final long     SITE_FORMAT_HQ_L                          = 5;
    private static final long     SITE_FORMAT_LQ_L                          = 4;
    private static final int      INTERNAL_FORMAT_HD_I                      = 0;
    private static final int      INTERNAL_FORMAT_HQ_I                      = 1;
    private static final int      INTERNAL_FORMAT_LQ_I                      = 2;
    private static final int      INTERNAL_FORMAT_UNSET                     = 3;
    /* Other */
    private static Object         LOCK                                      = new Object();
    private static final int      MAX_RETRIES_LOGIN                         = 10;
    private static final int      MAX_RETRIES_SAFE_REQUEST                  = 3;
    private Account               currAcc                                   = null;
    private DownloadLink          currDownloadLink                          = null;
    private SubConfiguration      cfg                                       = null;

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
        /* Old: http://free.save.tv/STV/S/misc/miscShowTermsConditionsInMainFrame.cfm */
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

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @SuppressWarnings("deprecation")
    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
        this.cfg = this.getPluginConfig();
    }

    public static String getAPIClientID() {
        return API_PUBLIC_KEY;
    }

    public static String getAPISecretKey() {
        return API_SECRET_KEY;
    }

    /**
     * TODO: Known Bugs in API mode: API cannot differ between different video formats -> Cannot show any error in case user choice is not
     * available. --> NO FATAL bugs ---> Plugin will work fine with them!
     *
     * @property "category": 0 = undefined, 1 = movies,category: 2 = series, 3 = show, 7 = music
     */
    @SuppressWarnings({ "deprecation", "unused", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        br.setFollowRedirects(true);
        link.setProperty(PROPERTY_type, EXTENSION_default);
        /* Show telecast-ID + extension as dummy name for all error cases */
        final String telecast_ID = getTelecastId(link);
        if (telecast_ID == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (link.getName() != null && (link.getName().contains(telecast_ID) && !link.getName().endsWith(EXTENSION_default) || link.getName().contains(".cfm"))) {
            link.setName(telecast_ID + EXTENSION_default);
        }
        Account aa = null;
        final String account_username_via_which_url_is_downloadable = getDownloadableVia(link);
        final ArrayList<Account> all_stv_accounts = AccountController.getInstance().getValidAccounts(this.getHost());
        if (all_stv_accounts == null) {
            link.getLinkStatus().setStatusText("Kann Links ohne güntigen und dazugehörigen Account nicht überprüfen");
            checkAccountNeededDialog();
            return AvailableStatus.UNCHECKABLE;
        } else if (account_username_via_which_url_is_downloadable == null && all_stv_accounts.size() == 1) {
            /* User probably added save.tv urls manually and has only one account --> Allow these to be downloaded via this account! */
            aa = all_stv_accounts.get(0);
            link.setProperty(PROPERTY_downloadable_via, aa.getUser());
        } else {
            /* Find account via which we can download our url. */
            for (final Account aatemp : all_stv_accounts) {
                if (this.canHandle(link, aatemp)) {
                    aa = aatemp;
                    break;
                }
            }
        }
        if (aa == null) {
            link.getLinkStatus().setStatusText("Kann Links ohne güntigen und dazugehörigen Account nicht überprüfen");
            checkAccountNeededDialog();
            return AvailableStatus.UNCHECKABLE;
        }
        /* Set linkID for correct dupe-check as telecastID is bound to account! */
        if (link.getLinkID() == null || !link.getLinkID().matches("\\d+")) {
            /* Every account has individual telecastIDs, only downloadable via this account. */
            link.setLinkID(aa.getUser() + telecast_ID);
        }
        setConstants(aa, link);
        if (this.getPluginConfig().getBooleanProperty(DISABLE_LINKCHECK, false) && !FORCE_LINKCHECK) {
            link.getLinkStatus().setStatusText("Linkcheck deaktiviert - korrekter Dateiname erscheint erst beim Downloadstart");
            return AvailableStatus.TRUE;
        }
        String filesize = null;
        if (is_API_enabled()) {
            API_SESSIONID = login_api(this.br, aa, false);
            /* Last revision with old filename-convert handling: 26988 */
            api_doSoapRequestSafe(this.br, aa, "http://tempuri.org/ITelecast/GetTelecastDetail", "<telecastIds xmlns:a=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\"><a:int>" + getTelecastId(link) + "</a:int></telecastIds><detailLevel>2</detailLevel>");
            if (!br.containsHTML("<a:TelecastDetail>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            parseFilenameInformation_api(link, br.toString());
            parseQualityTag(link, null);
        } else {
            login_site(this.br, aa, false);
            getPageSafe("https://www." + this.getHost() + "/STV/M/obj/archive/JSON/VideoArchiveDetailsApi.cfm?TelecastID=" + telecast_ID, aa);
            if (!br.getURL().contains("/JSON/") || this.br.getHttpConnection().getResponseCode() == 404) {
                /* Offline#1 - offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final Object aRRALLOWDDOWNLOADFORMATS = entries.get("ARRALLOWDDOWNLOADFORMATS");
            final Object sTRRECORDORDER = entries.get("STRRECORDORDER");
            if (aRRALLOWDDOWNLOADFORMATS == null || sTRRECORDORDER == null) {
                /*
                 * Offline#2 - expired (download not possible anymore - if user tries to download something that has not been recorded yet,
                 * code will NOT jump into this as json will already contain some download information / these two important json objects)
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final ArrayList<Object> sourcelist = jsonGetVideoSourcelist(entries);
            entries = (LinkedHashMap<String, Object>) entries.get("TELECASTDETAILS");
            parseFilenameInformation_site(link, entries);
            parseQualityTag(link, sourcelist);
        }
        link.setAvailable(true);
        final String availablecheck_filename = getFilename(this, link);
        /*
         * Reset (final) filename from previous state so we can use the final filename as final filename later even if it has changed before
         */
        link.setFinalFileName(null);
        link.setName(null);
        link.setName(availablecheck_filename);
        if (filesize != null) {
            filesize = filesize.replace(".", "");
            final long page_size = SizeFormatter.getSize(filesize.replace(".", ""));
            link.setDownloadSize(page_size);
        } else {
            link.setDownloadSize(calculateFilesize(link, link.getLongProperty(PROPERTY_site_runtime_minutes, 0)));
        }
        /* TODO: Check if this errormessage still exists */
        if (this.br.containsHTML(HTML_SITE_DL_IMPOSSIBLE)) {
            if (br.containsHTML(HTML_SITE_DL_IMPOSSIBLE)) {
                link.getLinkStatus().setStatusText(DL_IMPOSSIBLE_USER_TEXT);
            }
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    public static String getFilename(final Plugin plugin, final DownloadLink dl) throws ParseException {
        /*
         * No custom filename if not all required tags are given, if the user prefers original filenames or if custom user regexes for
         * specified series or movies match to force original filenames
         */
        final SubConfiguration cfg = SubConfiguration.getConfig(NICE_HOST);
        final boolean force_original_general = (cfg.getBooleanProperty(USEORIGINALFILENAME) || dl.getLongProperty(PROPERTY_category, 0l) == 0);
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

    /** Their json is crazy regarding data types thus we have a lot of type conversions here ... */
    public static void parseFilenameInformation_site(final DownloadLink dl, final LinkedHashMap<String, Object> sourcemap) throws PluginException {
        /*
         * Caution with data types - if e.g. a movie is named "1987" they will actually use a double- or long value - this is totally crazy
         * as everything can happen here. Imagine a movie is named "true" ...
         */
        final Object site_title_o = sourcemap.get("STITLE");
        final String site_title = jsonobject_to_string(site_title_o);
        long datemilliseconds = 0;
        /* For series only */
        String episodenumber;
        final Object episodenumber_o = sourcemap.get("SFOLGE");
        final Object episodename_o = sourcemap.get("SSUBTITLE");
        final String episodename = jsonobject_to_string(episodename_o);
        if (episodenumber_o != null && episodenumber_o instanceof Double) {
            episodenumber = Double.toString(((Double) episodenumber_o).doubleValue());
        } else {
            episodenumber = (String) episodenumber_o;
        }
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
        String category = Long.toString(JavaScriptEngineFactory.toLong(sourcemap.get("TVCATEGORYID"), -1));
        /* Happens in decrypter - errorhandling! */
        if (category.equals("-1") && (episodename != null || episodenumber != null)) {
            category = "2";
        } else if (category == null) {
            category = "1";
        }
        final String runtime_start = (String) sourcemap.get("DSTARTDATE");
        /* For hosterplugin */
        String runtime_end = (String) sourcemap.get("ENDDATE");
        /* For decrypterplugin */
        if (runtime_end == null) {
            runtime_end = (String) sourcemap.get("DENDDATE");
        }
        final long runtime_end_long = TimeFormatter.getMilliSeconds(runtime_end, "yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
        datemilliseconds = TimeFormatter.getMilliSeconds(runtime_start, "yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
        final long site_runtime_minutes = (runtime_end_long - datemilliseconds) / 1000 / 60;
        final String tv_station = (String) sourcemap.get("STVSTATIONNAME");
        /* TODO: Add more/all numbers here, improve this! */
        if (category.equals("2")) {
            /* For series */
            dl.setProperty(PROPERTY_category, 2);
        } else if (category.equals("3")) {
            /* For shows */
            dl.setProperty(PROPERTY_category, 3);
        } else if (category.equals("7")) {
            /* For music */
            dl.setProperty(PROPERTY_category, 7);
        } else if (category.equals("1") || category.equals("6")) {
            /* For movies and magazines */
            dl.setProperty(PROPERTY_category, 1);
        } else {
            /* For everything else - same as movie but separated anyways */
            dl.setProperty(PROPERTY_category, 1);
        }
        /* Set properties which are needed for filenames */
        /* Add series information */
        if (episodenumber != null) {
            /* json response contains episodenumbers in double-data-type --> Makes no sense --> Correct it here */
            if (episodenumber.matches("^\\d+\\.0$")) {
                episodenumber = new Regex(episodenumber, "^(\\d+)\\.0$").getMatch(0);
            }
            dl.setProperty(PROPERTY_episodenumber, correctData(episodenumber));
        }
        /* Sometimes episodetitle == episodenumber (double) --> Do NOT set it as episodetitle is NOT given in this case! */
        if (episodename != null && !episodename.matches("\\d+\\.\\d+")) {
            dl.setProperty(PROPERTY_episodename, correctData(episodename));
        }
        /* Add other information */
        if (!produceyear.equals("0")) {
            dl.setProperty(PROPERTY_produceyear, correctData(produceyear));
        }
        if (genre != null) {
            dl.setProperty(PROPERTY_genre, correctData(genre));
        }
        if (producecountry != null) {
            dl.setProperty(PROPERTY_producecountry, correctData(producecountry));
        }
        dl.setProperty(PROPERTY_plain_site_category, category);
        if (tv_station != null) {
            dl.setProperty(PROPERTY_plain_tv_station, correctData(tv_station));
        }
        /* Add remaining basic information */
        if (site_title != null) {
            dl.setProperty(PROPERTY_plainfilename, correctData(site_title));
        }
        dl.setProperty(PROPERTY_originaldate, datemilliseconds);
        dl.setProperty(PROPERTY_originaldate_end, runtime_end_long);
        dl.setProperty(PROPERTY_site_runtime_minutes, site_runtime_minutes);
    }

    public static void parseFilenameInformation_api(final DownloadLink dl, final String source) throws PluginException {
        final String site_title = new Regex(source, "<a:T>([^<>]*?)</a:T>").getMatch(0);
        long datemilliseconds = 0;
        /* For series only */
        final String episodenumber = new Regex(source, "<a:Episode>([^<>\"]*?)</a:Episode>").getMatch(0);
        final String episodename = new Regex(source, "<a:ST>([^<>\"]*?)</a:ST>").getMatch(0);
        /* General */
        final String genre = new Regex(source, "<a:Char>([^<>\"]*?)</a:Char>").getMatch(0);
        String category = new Regex(source, "global/TVCategorie/kat(\\d+)\\.jpg</a:MIP>").getMatch(0);
        /* Happens in decrypter - errorhandling! */
        if (category == null && (episodename != null || episodenumber != null)) {
            category = "2";
        } else if (category == null) {
            category = "1";
        }
        String runtime_start = new Regex(source, "<a:SD>([^<>\"]*?)</a:SD>").getMatch(0);
        /* For hosterplugin */
        String runtime_end = new Regex(source, "<a:EndDate>([^<>\"]*?)</a:EndDate>").getMatch(0);
        runtime_start = runtime_start.replace("T", " ");
        runtime_end = runtime_end.replace("T", " ");
        final long runtime_end_long = TimeFormatter.getMilliSeconds(runtime_end, "yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
        datemilliseconds = TimeFormatter.getMilliSeconds(runtime_start, "yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
        final long site_runtime_minutes = (runtime_end_long - datemilliseconds) / 1000 / 60;
        final String tv_station = PluginJSonUtils.getJsonValue(source, "STVSTATIONNAME");
        /* TODO: Add more/all numbers here, improve this! */
        if (category.equals("2")) {
            /* For series */
            dl.setProperty(PROPERTY_category, 2);
        } else if (category.equals("3")) {
            /* For shows */
            dl.setProperty(PROPERTY_category, 3);
        } else if (category.equals("7")) {
            /* For music */
            dl.setProperty(PROPERTY_category, 7);
        } else if (category.equals("1") || category.equals("6")) {
            /* For movies and magazines */
            dl.setProperty(PROPERTY_category, 1);
        } else {
            /* For everything else - same as movie but separated anyways */
            dl.setProperty(PROPERTY_category, 1);
        }
        /* Set properties which are needed for filenames */
        /* Add series information */
        if (episodenumber != null) {
            dl.setProperty(PROPERTY_episodenumber, correctData(episodenumber));
        }
        if (episodename != null) {
            dl.setProperty(PROPERTY_episodename, correctData(episodename));
        }
        /* Add other information */
        if (genre != null) {
            dl.setProperty(PROPERTY_genre, correctData(genre));
        }
        dl.setProperty(PROPERTY_plain_site_category, category);
        if (tv_station != null) {
            dl.setProperty(PROPERTY_plain_tv_station, correctData(tv_station));
        }
        /* Add remaining basic information */
        if (site_title != null) {
            dl.setProperty(PROPERTY_plainfilename, correctData(site_title));
        }
        dl.setProperty(PROPERTY_originaldate, datemilliseconds);
        dl.setProperty(PROPERTY_originaldate_end, runtime_end_long);
        dl.setProperty(PROPERTY_site_runtime_minutes, site_runtime_minutes);
    }

    public static void parseQualityTag(final DownloadLink dl, final ArrayList<Object> sourcelist) {
        final int selected_video_format = getConfiguredVideoFormat(dl);
        /*
         * If we have no source, we can select HQ if the user chose HQ because it is always available. If the user selects any other quality
         * we need to know whether it exists or not and then set the data.
         */
        if (sourcelist == null) {
            if (selected_video_format == 1) {
                dl.setProperty(PROPERTY_quality, STATE_QUALITY_HQ);
            }
        } else {
            final String quality_best = jsonGetBestQualityId(sourcelist);
            final boolean isHDAvailable = sourcelist.size() == 3 || quality_best.equals("");
            switch (selected_video_format) {
            case INTERNAL_FORMAT_HD_I:
                if (isHDAvailable) {
                    dl.setProperty(PROPERTY_quality, STATE_QUALITY_HD);
                } else {
                    dl.setProperty(PROPERTY_quality, STATE_QUALITY_HQ);
                }
                break;
            case INTERNAL_FORMAT_HQ_I:
                dl.setProperty(PROPERTY_quality, STATE_QUALITY_HQ);
                break;
            case INTERNAL_FORMAT_LQ_I:
                if (sourcelist.size() == 2) {
                    /* Mobile version available (should alyways be the case!) */
                    dl.setProperty(PROPERTY_quality, STATE_QUALITY_LQ);
                } else {
                    dl.setProperty(PROPERTY_quality, STATE_QUALITY_HQ);
                }
                break;
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        synchronized (LOCK) {
            checkFeatureDialogAll();
            checkFeatureDialogCrawler();
            // checkFeatureDialogNew();
        }
        final boolean preferAdsFree = getPreferAdsFree(link);
        String request_value_downloadWithoutAds = Boolean.toString(preferAdsFree);
        FORCE_LINKCHECK = true;
        requestFileInformation(link);
        setConstants(account, link);
        /* Check if the content has been recorded already! */
        final long runtime_end = link.getLongProperty(PROPERTY_originaldate_end, System.currentTimeMillis() + 1);
        final long released_since = System.currentTimeMillis() - runtime_end;
        if (released_since < 0) {
            /*
             * Content not yet recorded --> Show errormessage with waittime. Waittime = END of the content + 10 minutes! Most times Stv
             * needs between 10 and 60 minutes to get the downloads ready.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Diese Sendung wurde noch nicht aufgenommen/ausgestrahlt!", released_since * (-1) + 10 * 60 * 60 * 1000l);
        }
        if (apiActive()) {
            /* Check if ads-free version is available */
            api_doSoapRequestSafe(this.br, account, "http://tempuri.org/IVideoArchive/GetAdFreeState", "<telecastId i:type=\"d:int\">" + getTelecastId(link) + "</telecastId><telecastIdSpecified i:type=\"d:boolean\">true</telecastIdSpecified>");
            if (br.containsHTML("<a:IsAdFreeAvailable>false</a:IsAdFreeAvailable>")) {
                /*
                 * TODO: If a telecastID is expired (deleted/not downloadable anymore), this is what will happen. There does not seem to be
                 * any possibility to get the correct status for this case via API.
                 */
                link.getLinkStatus().setStatusText(USERTEXT_ADSFREEANOTVAILABLE);
                ISADSFREEAVAILABLE = false;
            } else {
                link.getLinkStatus().setStatusText(USERTEXT_ADSFREEAVAILABLE);
                ISADSFREEAVAILABLE = true;
            }
        } else {
            /* TODO: Check if this errormessage still exists */
            if (br.containsHTML(HTML_SITE_DL_IMPOSSIBLE)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, DL_IMPOSSIBLE_USER_TEXT, 30 * 60 * 1000l);
            }
            /* Check if ads-free version is available */
            /* TODO: Check if the numbers are still correct */
            /* TODO: Enhance ad-free check - check if selected format is available and if it is available in ad-free */
            final String ad_Free_availability = PluginJSonUtils.getJsonValue(br, "BADFREEAVAILABLE");
            if (ad_Free_availability == null || ad_Free_availability.equals("3") || ad_Free_availability.equalsIgnoreCase("false")) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SaveTv.NoCutListAvailable", USERTEXT_NOCUTAVAILABLE));
            } else if (ad_Free_availability.equals("1") || ad_Free_availability.equalsIgnoreCase("true")) {
                link.getLinkStatus().setStatusText(USERTEXT_ADSFREEAVAILABLE);
                ISADSFREEAVAILABLE = true;
            } else {
                /* ad_Free_availability == "2" */
                link.getLinkStatus().setStatusText(USERTEXT_ADSFREEANOTVAILABLE);
                ISADSFREEAVAILABLE = false;
            }
        }
        /* Set ad-free state on DownloadLink for e.g. usage in filename later. */
        if (this.ISADSFREEAVAILABLE) {
            link.setProperty(PROPERTY_ad_free, STATE_ad_free_true);
        } else {
            link.setProperty(PROPERTY_ad_free, STATE_ad_free_false);
        }
        String dllink = null;
        /*
         * User wants ads-free but it's not available -> Wait X [User-Defined DOWNLOADONLYADSFREE_RETRY_HOURS] hours, status can still
         * change but probably won't -> If defined by user, force version with ads after a user defined amount of retries.
         */
        if (preferAdsFree && !this.ISADSFREEAVAILABLE) {
            logger.info("Ad-free version is unavailable");
            final long maxRetries = cfg.getLongProperty(ADS_FREE_UNAVAILABLE_MAXRETRIES, defaultADS_FREE_UNAVAILABLE_MAXRETRIES);
            long currentTryCount = link.getLongProperty(PROPERTY_DOWNLOADLINK_ADSFREEFAILED_COUNT, 0);
            final boolean load_with_ads = (maxRetries != 0 && currentTryCount >= maxRetries);
            /* Always increase error counter, even if the user downloads the version with ads. */
            currentTryCount++;
            link.setProperty(PROPERTY_DOWNLOADLINK_ADSFREEFAILED_COUNT, currentTryCount);
            if (load_with_ads) {
                logger.info("Ad-free version is unavailable --> Downloading version with ads");
                request_value_downloadWithoutAds = "false";
            } else {
                logger.info("Ad-free version is unavailable --> Waiting");
                logger.info("--> Throw Exception_no_ads_free_1 | Try " + currentTryCount + "/" + maxRetries);
                final long userDefinedWaitHours = cfg.getLongProperty(ADS_FREE_UNAVAILABLE_HOURS, SaveTv.defaultADS_FREE_UNAVAILABLE_HOURS);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, USERTEXT_NOCUTAVAILABLE, userDefinedWaitHours * 60 * 60 * 1000l);
            }
        }
        /* Set download options (ads-free or with ads) and get download url */
        String stv_request_selected_format_value = null;
        if (apiActive()) {
            stv_request_selected_format_value = api_get_format_request_value(link);
            /* Small workaround to prevent incorrect errormessages */
            final String[] formats = { stv_request_selected_format_value, API_FORMAT_HQ, API_FORMAT_LQ };
            for (final String format : formats) {
                api_postDownloadPage(link, format, request_value_downloadWithoutAds);
                /* TODO: Decide if we want to throw an error here or try until we find an existing format. */
                if ((br.containsHTML(HTML_API_DL_IMPOSSIBLE) || this.br.getHttpConnection().getResponseCode() == 500) && stv_request_selected_format_value == API_FORMAT_HD) {
                    // continue;
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, USERTEXT_PREFERREDFORMATNOTAVAILABLE, 4 * 60 * 60 * 1000l);
                }
                break;
            }
            if (br.containsHTML(HTML_API_DL_IMPOSSIBLE)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, DL_IMPOSSIBLE_USER_TEXT, 30 * 60 * 1000l);
            }
            dllink = br.getRegex("<a:DownloadUrl>(http://[^<>\"]*?)</a").getMatch(0);
        } else {
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final ArrayList<Object> sourcelist = jsonGetVideoSourcelist(entries);
            final String best_quality_id = jsonGetBestQualityId(sourcelist);
            stv_request_selected_format_value = site_get_format_request_value(link);
            final boolean desired_format_is_available = jsonIsDesiredFormatAvailable(sourcelist, Long.parseLong(stv_request_selected_format_value));
            if (!desired_format_is_available) {
                logger.info("Desired format is not available - falling back to highest format/quality possible");
                stv_request_selected_format_value = best_quality_id;
            }
            site_AccessDownloadPage(link, stv_request_selected_format_value, request_value_downloadWithoutAds);
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
            if (inValidate(dllink) && !dlurl_success) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download aus unbekannten Gründen zurzeit nicht möglich", 10 * 60 * 1000l);
            }
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (dllink) is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting download...");
        int maxChunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        boolean resume = ACCOUNT_PREMIUM_RESUME;
        if (link.getBooleanProperty(PROPERTY_DOWNLOADLINK_NORESUME, false)) {
            resume = false;
        }
        if (link.getBooleanProperty(SaveTv.PROPERTY_DOWNLOADLINK_NOCHUNKS, false) || resume == false) {
            maxChunks = 1;
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler 404", 60 * 60 * 1000l);
            }
            /* Handle (known) errors */
            logger.warning("Received HTML code instead of the file!");
            br.followConnection();
            if (br.containsHTML(">Die Aufnahme kann zum aktuellen Zeitpunkt nicht vollständig heruntergeladen werden")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler: 'Die Aufnahme kann zum aktuellen Zeitpunkt nicht vollständig heruntergeladen werden'");
            }
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
                /* Unknown error, we disable multiple chunks */
                if (link.getLinkStatus().getErrorMessage() != null && link.getBooleanProperty(SaveTv.PROPERTY_DOWNLOADLINK_NOCHUNKS, false) == false) {
                    link.setProperty(SaveTv.PROPERTY_DOWNLOADLINK_NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                if (cfg.getBooleanProperty(DELETE_TELECAST_ID_AFTER_DOWNLOAD, defaultDeleteTelecastIDAfterDownload)) {
                    logger.info("Download finished --> User WANTS telecastID " + getTelecastId(link) + " deleted");
                    site_killTelecastID(account, link);
                } else {
                    logger.info("Download finished --> User does NOT want telecastID " + getTelecastId(link) + " deleted");
                }
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_DOWNLOAD_INCOMPLETE) {
                logger.info("ERROR_DOWNLOAD_INCOMPLETE --> Handling it");
                if (link.getBooleanProperty(PROPERTY_DOWNLOADLINK_NORESUME, false)) {
                    link.setProperty(PROPERTY_DOWNLOADLINK_NORESUME, Boolean.valueOf(false));
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unbekannter Serverfehler 2", 30 * 60 * 1000l);
                }
                link.setProperty(PROPERTY_DOWNLOADLINK_NORESUME, Boolean.valueOf(true));
                link.setChunksProgress(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "ERROR_DOWNLOAD_INCOMPLETE --> Retrying");
            }
            try {
                if (e.getLinkStatus() == LinkStatus.ERROR_ALREADYEXISTS) {
                    if (cfg.getBooleanProperty(DELETE_TELECAST_ID_IF_FILE_ALREADY_EXISTS, defaultDeleteTelecastIDIfFileAlreadyExists)) {
                        logger.info("ERROR_ALREADYEXISTS --> User WANTS telecastID " + getTelecastId(link) + " deleted");
                        site_killTelecastID(account, link);
                    } else {
                        logger.info("ERROR_ALREADYEXISTS --> User does NOT want telecastID " + getTelecastId(link) + " deleted");
                    }
                    throw e;
                }
            } catch (final Throwable efail) {
                /* Do not fail here, throw exception which happened previously */
                throw e;
            }
            /* New V2 errorhandling */
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(SaveTv.PROPERTY_DOWNLOADLINK_NOCHUNKS, false) == false) {
                link.setProperty(SaveTv.PROPERTY_DOWNLOADLINK_NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private boolean getPreferAdsFree(final DownloadLink dl) {
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

    private boolean getPreferAdsFreePluginConfig() {
        final SubConfiguration cfg = SubConfiguration.getConfig(NICE_HOST);
        final boolean preferAdsFreeConfig = cfg.getBooleanProperty(PREFERADSFREE, defaultPreferAdsFree);
        return preferAdsFreeConfig;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai;
        setConstants(account, null);
        loginSafe(this.br, account, true);
        if (is_API_enabled()) {
            ai = fetchAccountInfoAPI(account);
        } else {
            ai = fetchAccountInfoWebsite(account);
        }
        /* Unlimited traffic == default */
        // ai.setUnlimitedTraffic();
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
        /* Do not fail here, TODO: Improve this */
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
                    account.saveCookies(this.br.getCookies(COOKIE_HOST), "");
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
            account.setProperty(PROPERTY_acc_package, correctData(package_name));
            if (price != null) {
                account.setProperty(PROPERTY_acc_price, correctData(price));
            }
            if (runtime != null) {
                account.setProperty(PROPERTY_acc_runtime, correctData(runtime));
            }
            if (acc_username != null) {
                this.getPluginConfig().setProperty(PROPERTY_acc_username, correctData(acc_username));
            }
            br.getPage("https://www." + this.getHost() + "/STV/M/obj/archive/JSON/VideoArchiveApi.cfm?iEntriesPerPage=1");
            final String totalLinks = PluginJSonUtils.getJsonValue(br, "ITOTALENTRIES");
            if (totalLinks != null) {
                account.setProperty(PROPERTY_acc_count_telecast_ids, totalLinks);
            }
            account.setType(AccountType.PREMIUM);
        } catch (final Throwable e) {
            /* Should not happen but a failure of the account detail crawler won't hurt - we logged in fine! */
            logger.info("Extended account check failed");
        }
        ai.setStatus(package_name);
        account.setProperty(PROPERTY_acc_type, package_name);
        account.setValid(true);
        return ai;
    }

    /** Performs login respecting api setting */
    private void login(final Browser br, final Account account, final boolean force) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (is_API_enabled()) {
            API_SESSIONID = login_api(br, account, force);
        } else {
            login_site(br, account, force);
        }
    }

    public static void login_site(final Browser br, final Account account, final boolean force) throws Exception {
        final String lang = System.getProperty("user.language");
        site_prepBrowser(br);
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(COOKIE_HOST, cookies);
                    return;
                }
                final String postData = "sUsername=" + Encoding.urlEncode(account.getUser()) + "&sPassword=" + Encoding.urlEncode(account.getPass()) + "&bAutoLoginActivate=1";
                br.postPage("https://www." + NICE_HOST + "/STV/M/Index.cfm?sk=PREMIUM", postData);
                if (br.containsHTML("No htmlCode read")) {
                    br.getPage("https://www." + NICE_HOST + "/STV/M/obj/TVProgCtr/tvctShow.cfm");
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
                account.saveCookies(br.getCookies(COOKIE_HOST), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static String login_api(final Browser br, final Account account, final boolean force) throws Exception {
        api_prepBrowser(br);
        String api_access_token = account.getStringProperty(PROPERTY_ACCOUNT_API_SESSIONID, null);
        final long lastUse = account.getLongProperty(PROPERTY_lastuse, -1l);
        long expires_in = account.getLongProperty("expires_in", 0);
        /* Only generate new access_token if we have none or it's older than 6 hours */
        if (api_access_token == null || (System.currentTimeMillis() - lastUse) > 360000 || force) {
            /* New token required */
            br.postPage("https://auth.save.tv/token", "grant_type=password&client_id=" + getAPIClientID() + "&client_secret=" + getAPISecretKey() + "&username={2}&password={3}");
            api_POST(br, "https://auth.save.tv/token", "grant_type=password&client_id={0}&client_secret={1}&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            api_access_token = PluginJSonUtils.getJson(br, "access_token");
            final String refresh_token = PluginJSonUtils.getJson(br, "refresh_token");
            final String seconds_expires_in_str = PluginJSonUtils.getJson(br, "expires_in");
            if (br.getHttpConnection().getResponseCode() == 400 || StringUtils.isEmpty(api_access_token) || StringUtils.isEmpty(refresh_token) || StringUtils.isEmpty(seconds_expires_in_str)) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            /* 5 seconds of tolerance to avoid requests happening while our token expires. */
            expires_in = (Long.parseLong(seconds_expires_in_str) - 5) * 1000;
            setAPIAuthHeaders(br, api_access_token);
            account.setProperty(PROPERTY_lastuse, System.currentTimeMillis());
            account.setProperty(PROPERTY_ACCOUNT_API_SESSIONID, api_access_token);
            account.setProperty("refresh_token", refresh_token);
            account.setProperty("expires_in", System.currentTimeMillis() + expires_in);
        }
        /* TODO: Check token and refresh it, if expired! */
        return api_access_token;
    }

    /**
     * Log in, handle all kinds of timeout- and browser errors - as long as we know that the account is valid it should stay active!
     */
    @SuppressWarnings("deprecation")
    private void loginSafe(final Browser br, final Account acc, final boolean force) throws Exception {
        boolean success = false;
        try {
            for (int i = 1; i <= MAX_RETRIES_LOGIN; i++) {
                logger.info("Login try " + i + " of " + MAX_RETRIES_LOGIN);
                try {
                    login(br, acc, true);
                } catch (final Exception e) {
                    if (e instanceof UnknownHostException || e instanceof BrowserException) {
                        logger.info("Login " + i + " of " + MAX_RETRIES_LOGIN + " failed because of server error or timeout");
                        continue;
                    } else {
                        throw e;
                    }
                }
                logger.info("Login " + i + " of " + MAX_RETRIES_LOGIN + " was successful");
                success = true;
                break;
            }
        } catch (final PluginException ep) {
            logger.info("save.tv Account ist ungültig!");
            acc.setValid(false);
            throw ep;
        }
        if (!success) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLogin wegen Serverfehler oder Timeout fehlgeschlagen!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
    }

    /* Always compare to the decrypter */
    private void getPageSafe(final String url, final Account acc) throws Exception {
        /*
         * Max 30 logins possible Max 3 accesses of the link possible -> Max 33 total requests
         */
        for (int i = 1; i <= MAX_RETRIES_SAFE_REQUEST; i++) {
            this.br.getPage(url);
            if (br.getURL().contains(URL_LOGGED_OUT)) {
                logger.info("Refreshing cookies to continue downloading " + i + " of " + MAX_RETRIES_SAFE_REQUEST);
                br.clearCookies(COOKIE_HOST);
                loginSafe(br, acc, true);
                continue;
            }
            if (i > 1) {
                logger.info("Successfully refreshed cookies to access url: " + url);
            }
            handleErrorsWebsiteJson(br);
            break;
        }
    }

    /* Handles website requests + errorhandling. */
    private void postPageSafe(final Browser br, final String url, final String postData) throws Exception {
        br.postPage(url, postData);
        handleErrorsWebsiteJson(br);
    }

    /*
     * Handles website json errors.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void handleErrorsWebsiteJson(final Browser br) throws Exception {
        /*
         * NOT use the json parser für the first check as especially when used via decrypter, the amount of data can be huge which can lead
         * to parser memory problems/crashes.
         */
        final boolean isInErrorState = br.containsHTML("\\d+\\.\\d+E\\d+,\"NOK\",\"");
        if (isInErrorState) {
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final ArrayList<Object> errorlist = (ArrayList) entries.get("ARRVIDEOURL");
            final String errormessage = (String) errorlist.get(2);
            if (errormessage.contains("Aufnahme zu betrachten, laden Sie bitte die ungeschnittene Version")) {
                final long userDefinedWaitHours = cfg.getLongProperty(ADS_FREE_UNAVAILABLE_HOURS, SaveTv.defaultADS_FREE_UNAVAILABLE_HOURS);
                long currentTryCount = this.currDownloadLink.getLongProperty(PROPERTY_DOWNLOADLINK_ADSFREEFAILED_COUNT, 0);
                /* Always increase error counter, even if the user downloads the version with ads. */
                currentTryCount++;
                this.currDownloadLink.setProperty(PROPERTY_DOWNLOADLINK_ADSFREEFAILED_COUNT, currentTryCount);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, USERTEXT_NOCUTAVAILABLE, userDefinedWaitHours * 60 * 60 * 1000l);
            } else if (errormessage.equals("unknown error")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler: 'unknown error'", 15 * 60 * 1000l);
            } else {
                /* We can retry on unknown errorstates here as we know that Stv will usually fix them soon. */
                logger.warning("Unhandled / Unknown error happened");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Undehandelter Fehler: " + errormessage, 30 * 60 * 1000l);
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
     */
    @SuppressWarnings("unused")
    private void api_PostDownloadPage(final DownloadLink dl, final String user_selected_video_quality, final String downloadWithoutAds) throws Exception {
        br.postPage("https://www.save.tv/STV/M/obj/cRecordOrder/croGetDownloadUrl.cfm?null.GetDownloadUrl", "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetDownloadUrl&c0-id=&c0-param0=number:" + getTelecastId(dl) + "&" + user_selected_video_quality + "&c0-param2=boolean:" + downloadWithoutAds + "&xml=true&");
    }

    /**
     * @param dl
     *            :Current DownloadLink Object
     * @param user_selected_video_quality
     *            : Video quality selected by the user
     * @param downloadWithoutAds
     *            : Prefer adsfree Videos or videos with ads
     * @throws Exception
     */
    private void site_AccessDownloadPage(final DownloadLink dl, final String user_selected_video_quality, final String downloadWithoutAds) throws Exception {
        final String downloadoverview_url = "https://www.save.tv/STV/M/obj/cRecordOrder/croGetDownloadUrl2.cfm?TelecastId=" + getTelecastId(dl) + "&iFormat=" + user_selected_video_quality + "&bAdFree=" + downloadWithoutAds;
        this.getPageSafe(downloadoverview_url, this.currAcc);
    }

    /**
     * @param dl
     *            DownloadLink
     * @param user_selected_video_quality
     *            : MVom Benutzer bevorzugte Qualitätsstufe
     * @param downloadWithoutAds
     *            : Videos mit angewandter Schnittliste bevorzugen oder nicht
     */
    private void api_postDownloadPage(final DownloadLink dl, final String user_selected_video_quality, final String downloadWithoutAds) throws Exception {
        api_POST(this.br, "http://tempuri.org/IDownload/GetStreamingUrl", "<sessionId i:type=\"d:string\">" + this.API_SESSIONID + "</sessionId><telecastId i:type=\"d:int\">" + getTelecastId(dl) + "</telecastId><telecastIdSpecified i:type=\"d:boolean\">true</telecastIdSpecified><recordingFormatId i:type=\"d:int\">" + user_selected_video_quality + "</recordingFormatId><recordingFormatIdSpecified i:type=\"d:boolean\">true</recordingFormatIdSpecified><adFree i:type=\"d:boolean\">" + downloadWithoutAds + "</adFree><adFreeSpecified i:type=\"d:boolean\">true</adFreeSpecified>");
    }

    /**
     * Deletes a desired telecastID from the users' account(!) <br />
     *
     * @param acc
     *            Account : The users' account which might be needed to log in in case the user uses the API
     * @param dl
     *            DownloadLink: The DownloadLink whose telecastID will be deleted.
     */
    private void site_killTelecastID(final Account acc, final DownloadLink dl) throws IOException, PluginException {
        try {
            /* If the API is used, we need to log in via site here to be able to delete the telecastID */
            if (apiActive()) {
                login_site(this.br, acc, false);
            }
            final String deleteurl = "https://www." + NICE_HOST + "/STV/M/obj/cRecordOrder/croDelete.cfm?TelecastID=" + getTelecastId(dl);
            this.br.getPage(deleteurl);
            if (br.containsHTML("\"ok\"")) {
                logger.info("Successfully deleted telecastID: " + getTelecastId(dl));
            } else {
                logger.warning("Failed to delete telecastID: " + getTelecastId(dl));
            }
        } catch (final Throwable e) {
            logger.info("Failed to delete telecastID: " + getTelecastId(dl));
        }
    }

    private static void site_prepBrowser(final Browser br) {
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        /* TODO: Chance User-Agent to 'JDownloader' after APIv3 implementation is complete. */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0");
    }

    /** Prepare Browser for API usage. */
    public static void api_prepBrowser(final Browser br) {
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.getHeaders().put("Accept", "application/json");
        br.setAllowedResponseCodes(new int[] { 400 });
    }

    /** Set Authorization header(s) for API usage. */
    public static void setAPIAuthHeaders(final Browser br, final String auth_token) {
        br.getHeaders().put("Authorization", "Bearer " + auth_token);
    }

    private boolean apiActive() {
        // return (API_SESSIONID != null && is_API_enabled());
        /* TODO: 2016-07-06: APIV2 has been shut down - implement APIV3, then re-enable functionality here! */
        return false;
    }

    public static long calculateFilesize(final DownloadLink dl, final String minutes) {
        double calculated_filesize = 0;
        final long duration_minutes = Long.parseLong(minutes);
        final int user_format = getConfiguredVideoFormat(dl);
        switch (user_format) {
        case INTERNAL_FORMAT_HD_I:
            calculated_filesize = QUALITY_HD_MB_PER_MINUTE * duration_minutes * 1024 * 1024;
            break;
        case INTERNAL_FORMAT_HQ_I:
            calculated_filesize = QUALITY_H264_NORMAL_MB_PER_MINUTE * duration_minutes * 1024 * 1024;
            break;
        case INTERNAL_FORMAT_LQ_I:
            calculated_filesize = QUALITY_H264_MOBILE_MB_PER_MINUTE * duration_minutes * 1024 * 1024;
            break;
        }
        return (long) calculated_filesize;
    }

    public static long calculateFilesize(final DownloadLink dl, final long minutes) {
        double calculated_filesize = 0;
        final long duration_minutes = minutes;
        final int user_format = getConfiguredVideoFormat(dl);
        switch (user_format) {
        case INTERNAL_FORMAT_HD_I:
            calculated_filesize = QUALITY_HD_MB_PER_MINUTE * duration_minutes * 1024 * 1024;
            break;
        case INTERNAL_FORMAT_HQ_I:
            calculated_filesize = QUALITY_H264_NORMAL_MB_PER_MINUTE * duration_minutes * 1024 * 1024;
            break;
        case INTERNAL_FORMAT_LQ_I:
            calculated_filesize = QUALITY_H264_MOBILE_MB_PER_MINUTE * duration_minutes * 1024 * 1024;
            break;
        }
        return (long) calculated_filesize;
    }

    /**
     * Returns the format value needed for format specific requests TODO: Once serverside implemented, add support for HD - at the moment,
     * if user selected HD, Normal H.264 will be downloaded instead. 2016-02-26: Website and API now use the same values!
     */
    private String api_get_format_request_value(final DownloadLink dl) {
        final int selected_video_format = getConfiguredVideoFormat(dl);
        String stv_request_selected_format = null;
        switch (selected_video_format) {
        case INTERNAL_FORMAT_HD_I:
            stv_request_selected_format = API_FORMAT_HD;
            break;
        case INTERNAL_FORMAT_HQ_I:
            stv_request_selected_format = API_FORMAT_HQ;
            break;
        case INTERNAL_FORMAT_LQ_I:
            stv_request_selected_format = API_FORMAT_LQ;
            break;
        }
        return stv_request_selected_format;
    }

    /** Returns the format value needed for format specific requests */
    private String site_get_format_request_value(final DownloadLink dl) {
        final int selected_video_format = getConfiguredVideoFormat(dl);
        String stv_request_selected_format = null;
        switch (selected_video_format) {
        case INTERNAL_FORMAT_HD_I:
            stv_request_selected_format = SITE_FORMAT_HD;
            break;
        case INTERNAL_FORMAT_HQ_I:
            stv_request_selected_format = SITE_FORMAT_HQ;
            break;
        case INTERNAL_FORMAT_LQ_I:
            stv_request_selected_format = SITE_FORMAT_LQ;
            break;
        default:
            stv_request_selected_format = SITE_FORMAT_HD;
            break;
        }
        return stv_request_selected_format;
    }

    public static int getConfiguredVideoFormat(final DownloadLink dl) {
        final int videoformat;
        final int videoformatURL = getConfiguredVideoFormatUrl(dl);
        if (videoformatURL != INTERNAL_FORMAT_UNSET) {
            videoformat = videoformatURL;
        } else {
            videoformat = getConfiguredVideoFormatConfig();
        }
        return videoformat;
    }

    @SuppressWarnings("deprecation")
    public static int getConfiguredVideoFormatConfig() {
        switch (SubConfiguration.getConfig(NICE_HOST).getIntegerProperty(SELECTED_VIDEO_FORMAT, INTERNAL_FORMAT_UNSET)) {
        case INTERNAL_FORMAT_HD_I:
            return INTERNAL_FORMAT_HD_I;
        case INTERNAL_FORMAT_HQ_I:
            return INTERNAL_FORMAT_HQ_I;
        case INTERNAL_FORMAT_LQ_I:
            return INTERNAL_FORMAT_LQ_I;
        default:
            return INTERNAL_FORMAT_HD_I;
        }
    }

    @SuppressWarnings("deprecation")
    public static int getConfiguredVideoFormatUrl(final DownloadLink dl) {
        if (dl == null) {
            return INTERNAL_FORMAT_UNSET;
        }
        final String format_from_url = new Regex(dl.getDownloadURL(), "preferformat=(\\d+)").getMatch(0);
        if (format_from_url == null) {
            return INTERNAL_FORMAT_UNSET;
        }
        /* Convert official videoformat-number to internal number. */
        switch (Integer.parseInt(format_from_url)) {
        case (int) SITE_FORMAT_HD_L:
            return INTERNAL_FORMAT_HD_I;
        case (int) SITE_FORMAT_HQ_L:
            return INTERNAL_FORMAT_HQ_I;
        case (int) SITE_FORMAT_LQ_L:
            return INTERNAL_FORMAT_LQ_I;
        default:
            return INTERNAL_FORMAT_HD_I;
        }
    }

    @SuppressWarnings("unused")
    private double site_get_calculated_runtime_minutes(final DownloadLink dl, final long page_size_mb) {
        double run_time_calculated = 0;
        final int selected_video_format = getConfiguredVideoFormat(dl);
        switch (selected_video_format) {
        case INTERNAL_FORMAT_HD_I:
            run_time_calculated = page_size_mb / QUALITY_HD_MB_PER_MINUTE;
            break;
        case INTERNAL_FORMAT_HQ_I:
            run_time_calculated = page_size_mb / QUALITY_H264_NORMAL_MB_PER_MINUTE;
            break;
        case INTERNAL_FORMAT_LQ_I:
            run_time_calculated = page_size_mb / QUALITY_H264_MOBILE_MB_PER_MINUTE;
            break;
        }
        return run_time_calculated;
    }

    @SuppressWarnings("unchecked")
    public static String jsonGetBestQualityId(final ArrayList<Object> sourcelist) {
        final String recordingformat;
        if (sourcelist != null && sourcelist.size() > 0) {
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) sourcelist.get(sourcelist.size() - 1);
            recordingformat = Long.toString(jsonGetRecordingformatid(entries));
        } else {
            /* Fallback to a format which is always available. */
            recordingformat = Long.toString(SITE_FORMAT_HQ_L);
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

    public static long jsonGetRecordingformatid(final LinkedHashMap<String, Object> entries) {
        final long recordingformatid;
        final Object recordingformatido;
        if (entries != null) {
            recordingformatido = entries.get("RECORDINGFORMATID");
        } else {
            /* Errorhandling */
            recordingformatido = null;
        }
        if (recordingformatido == null) {
            recordingformatid = API_FORMAT_HQ_L;
        } else if (recordingformatido instanceof Double) {
            recordingformatid = (long) ((Double) recordingformatido).doubleValue();
        } else {
            recordingformatid = ((Long) recordingformatido).longValue();
        }
        return recordingformatid;
    }

    @SuppressWarnings("unchecked")
    public static boolean jsonIsDesiredFormatAvailable(final ArrayList<Object> sourcelist, final long desiredFormat) {
        if (sourcelist == null) {
            return false;
        }
        LinkedHashMap<String, Object> entries = null;
        long format_id = -1;
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
     * Performs save.tv API POST requests. <br />
     * TODO: Add errorhandling
     */
    private static String api_POST(final Browser br, final String url, final String postdata) throws Exception {
        br.postPage(url, postdata);
        return br.toString();
    }

    /**
     * Performs save.tv API GET requests. <br />
     * TODO: Add errorhandling
     */
    private static String api_GET(final Browser br, final String url) throws Exception {
        br.getPage(url);
        return br.toString();
    }

    /**
     * Performs save.tv API soap requests. If the session-id expires it will get refreshed.
     *
     * @param soapAction
     *            : The soap link which should be accessed
     * @param soapPost
     *            : The soap post data
     * @throws PluginException
     */
    public static void api_doSoapRequestSafe(final Browser br, final Account acc, final String soapAction, final String soapPost) throws Exception {
        final String method = new Regex(soapAction, "([A-Za-z0-9]+)$").getMatch(0);
        br.getHeaders().put("SOAPAction", soapAction);
        br.getHeaders().put("Content-Type", "text/xml");
        String postdata = "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><" + method + " xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\">" + "<sessionId i:type=\"d:string\">" + acc.getStringProperty(PROPERTY_ACCOUNT_API_SESSIONID, null) + "</sessionId>" + soapPost + "</" + method + "></v:Body></v:Envelope>";
        br.postPageRaw(API_BASE, postdata);
        /* Check for invalid sessionid --> Refresh if needed & perform request again */
        if (br.containsHTML(">invalid session id</ErrorMessage>")) {
            login_api(br, acc, true);
            postdata = "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><" + method + " xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\">" + "<sessionId i:type=\"d:string\">" + acc.getStringProperty(PROPERTY_ACCOUNT_API_SESSIONID, null) + "</sessionId>" + soapPost + "</" + method + "></v:Body></v:Envelope>";
            br.getHeaders().put("SOAPAction", soapAction);
            br.getHeaders().put("Content-Type", "text/xml");
            br.postPageRaw(API_BASE, postdata);
        }
    }

    private boolean is_API_enabled() {
        return this.getPluginConfig().getBooleanProperty(USEAPI);
    }

    /** Corrects all kinds of Strings which Stv provides, makes filenames look nicer */
    @SuppressWarnings("deprecation")
    public static String correctData(final String input) {
        String output = Encoding.htmlDecode(input);
        output = output.replace("_", " ");
        output = output.trim();
        output = output.replaceAll("(\r|\n)", "");
        output = output.replace("/", SubConfiguration.getConfig(NICE_HOST).getStringProperty(CUSTOM_FILENAME_SEPERATION_MARK, defaultCustomSeperationMark));
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
        final SubConfiguration cfg = SubConfiguration.getConfig(NICE_HOST);
        final String customStringForEmptyTags = getCustomStringForEmptyTags();
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
        final String site_category = dl.getStringProperty(PROPERTY_plain_site_category, customStringForEmptyTags);
        /* For series */
        final String episodename = dl.getStringProperty(PROPERTY_episodename, customStringForEmptyTags);
        final String episodenumber = getEpisodeNumber(dl);
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
            formattedFilename = formattedFilename.replace("*kategorie*", site_category);
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
            if (!formattedFilename.contains("*endung*") || (!formattedFilename.contains("*serientitel*") && !formattedFilename.contains("*episodenname*") && !formattedFilename.contains("*episodennummer*") && !formattedFilename.contains("*zufallszahl*") && !formattedFilename.contains("*telecastid*") && !formattedFilename.contains("*sendername*") && !formattedFilename.contains("*username*") && !formattedFilename.contains("*quality*") && !formattedFilename.contains("*server_dateiname*"))) {
                formattedFilename = defaultCustomFilenameMovies;
            }
            formattedFilename = formattedFilename.replace("*zufallszahl*", randomnumber);
            formattedFilename = formattedFilename.replace("*telecastid*", telecastid);
            formattedFilename = formattedFilename.replace("*datum*", formattedDate);
            formattedFilename = formattedFilename.replace("*produktionsjahr*", produceyear);
            formattedFilename = formattedFilename.replace("*episodennummer*", episodenumber);
            formattedFilename = formattedFilename.replace("*endung*", ext);
            formattedFilename = formattedFilename.replace("*quality*", quality);
            formattedFilename = formattedFilename.replace("*werbefrei*", ad_free);
            formattedFilename = formattedFilename.replace("*sendername*", tv_station);
            formattedFilename = formattedFilename.replace("*kategorie*", site_category);
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
        final SubConfiguration cfg = SubConfiguration.getConfig(NICE_HOST);
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
            final String title = convertNormalDataToServer(downloadLink.getStringProperty(PROPERTY_plainfilename, getCustomStringForEmptyTags()));
            String episodename = downloadLink.getStringProperty(PROPERTY_episodename, null);
            final String episodenumber = getEpisodeNumber(downloadLink);
            formattedFilename = title + "_";
            if (episodename != null) {
                episodename = convertNormalDataToServer(episodename);
                formattedFilename += episodename + "_";
            }
            /* Only add "Folge" if episodenumber is available */
            if (episodenumber.matches("\\d+")) {
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

    private static String getEpisodeNumber(final DownloadLink dl) {
        /* Old way TODO: Remove after 11.2014 */
        String episodenumber = Long.toString(dl.getLongProperty(PROPERTY_episodenumber, 0l));
        if (episodenumber.equals("0")) {
            /* New way */
            episodenumber = dl.getStringProperty(PROPERTY_episodenumber, null);
        }
        if (episodenumber == null) {
            return getCustomStringForEmptyTags();
        } else {
            return episodenumber;
        }
    }

    public static String getAdFreeText(final DownloadLink dl) {
        final String ad_free_status = dl.getStringProperty(PROPERTY_ad_free, "XX");
        return ad_free_status;
    }

    /**
     * @return true: DownloadLink is a series false: DownloadLink is no series based on existing information.
     */
    private static boolean isSeries(final DownloadLink dl) {
        final String customStringForEmptyTags = getCustomStringForEmptyTags();
        /* For series */
        final String episodename = dl.getStringProperty(PROPERTY_episodename, customStringForEmptyTags);
        final String episodenumber = getEpisodeNumber(dl);
        /* If we have an episodename and/or episodenumber, we have a series, category does not matter then */
        final boolean forceSeries = (!inValidate(episodename) && !episodename.equals(customStringForEmptyTags) || episodenumber.matches("\\d+"));
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
    public static String getCustomStringForEmptyTags() {
        final SubConfiguration cfg = SubConfiguration.getConfig(NICE_HOST);
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

    private static long websiteGetLowestRecordingormatid() {
        return SITE_FORMAT_LQ_L;
    }

    private static long websiteGetHighestRecordingormatid() {
        return SITE_FORMAT_HD_L;
    }

    private static long apiGetLowestRecordingormatid() {
        return API_FORMAT_LQ_L;
    }

    private static long apiGetHighestRecordingormatid() {
        return API_FORMAT_LQ_L;
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

    @Override
    public String buildExternalDownloadURL(final DownloadLink downloadLink, final PluginForHost buildForThisPlugin) {
        final String telecastID = getTelecastId(downloadLink);
        return String.format("https://www.%s/STV/M/obj/archive/VideoArchiveDetails.cfm?TelecastId=%s", this.getHost(), telecastID);
    }

    @Override
    public String getDescription() {
        return "JDownloader's Save.tv Plugin vereinfacht das Downloaden aufgenommener Sendungen von save.tv. Es bietet viele Plugin Einstellungen.";
    }

    private final static String  defaultCustomFilenameMovies                = "*quality* ¦ *videotitel* ¦ *produktionsjahr* ¦ *telecastid**endung*";
    private final static String  defaultCustomSeriesFilename                = "*serientitel* ¦ *quality* ¦ *episodennummer* ¦ *episodenname* ¦ *telecastid**endung*";
    private final static String  defaultCustomSeperationMark                = "+";
    private final static String  defaultCustomStringForEmptyTags            = "-";
    private final static int     defaultCrawlLasthours                      = 0;
    private final static int     defaultADS_FREE_UNAVAILABLE_HOURS          = 12;
    private final static int     defaultADS_FREE_UNAVAILABLE_MAXRETRIES     = 0;
    private static final boolean defaultDisableLinkcheck                    = false;
    private static final boolean defaultCrawlerActivate                     = false;
    private static final boolean defaultCrawlerFastLinkcheck                = true;
    private static final boolean defaultCrawlerAddNew                       = false;
    private static final boolean defaultInfoDialogsDisable                  = false;
    private static final boolean defaultPreferAdsFree                       = true;
    private static final boolean defaultUseOriginalFilename                 = false;
    private static final String  defaultCustomDate                          = "dd.MM.yyyy";
    private static final boolean defaultACTIVATE_BETA_FEATURES              = false;
    private static final boolean defaultUSEAPI                              = false;
    private static final String  defaultCONFIGURED_APIKEY                   = "JDDEFAULT";
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), SaveTv.CRAWLER_LASTHOURS_COUNT, JDL.L("plugins.hoster.SaveTv.grabArchive.lastHours", "Nur Aufnahmen der letzten X Stunden crawlen??\r\nAnzahl der Stunden, die gecrawlt werden sollen [0 = komplettes Archiv]:"), 0, 1000, 24).setDefaultValue(defaultCrawlLasthours).setEnabledCondidtion(crawlerAddNew, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.CRAWLER_ENABLE_FASTER, JDL.L("plugins.hoster.SaveTv.grabArchiveFaster", "Aktiviere schnellen Linkcheck für Archiv-Crawler?\r\nVorteil: Über den Archiv-Crawler hinzugefügte Links landen viel schneller im Linkgrabber\r\nNachteil: Es sind nicht alle Informationen (z.B. Kategorie) verfügbar - erst beim Download oder späterem Linkcheck\r\n")).setDefaultValue(defaultCrawlerFastLinkcheck).setEnabledCondidtion(crawlerActivate, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.CRAWLER_DISABLE_DIALOGS, JDL.L("plugins.hoster.SaveTv.crawlerDisableDialogs", "Info Dialoge des Archiv-Crawlers (nach dem Crawlen oder im Fehlerfall) deaktivieren?")).setDefaultValue(defaultInfoDialogsDisable).setEnabledCondidtion(crawlerActivate, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Format & Quality settings */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Format & Qualitäts-Einstellungen:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SELECTED_VIDEO_FORMAT, FORMATS, JDL.L("plugins.hoster.SaveTv.prefer_format", "Bevorzugtes Format (ist dieses nicht verfügbar, wird das beste verfügbare genommen)")).setDefaultValue(0));
        final ConfigEntry preferAdsFree = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.PREFERADSFREE, JDL.L("plugins.hoster.SaveTv.PreferAdFreeVideos", "Aufnahmen mit angewandter Schnittliste bevorzugen?")).setDefaultValue(defaultPreferAdsFree);
        getConfig().addEntry(preferAdsFree);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), SaveTv.ADS_FREE_UNAVAILABLE_HOURS, JDL.L("plugins.hoster.SaveTv.downloadOnlyAdsFreeRetryHours", "Zeit [in stunden] bis zum Neuversuch für Aufnahmen, die (noch) keine Schnittliste haben.\r\nINFO: Der Standardwert beträgt 12 Stunden, um die Server nicht unnötig zu belasten.\r\n"), 1, 24, 1).setDefaultValue(defaultADS_FREE_UNAVAILABLE_HOURS).setEnabledCondidtion(preferAdsFree, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), SaveTv.ADS_FREE_UNAVAILABLE_MAXRETRIES, JDL.L("plugins.hoster.SaveTv.ignoreOnlyAdsFreeAfterRetries_maxRetries", "Max Anzahl Neuversuche bis der Download der Version ohne Schnittliste erzwungen wird [0 = nie erzwingen]:"), 0, 100, 1).setDefaultValue(defaultADS_FREE_UNAVAILABLE_MAXRETRIES).setEnabledCondidtion(preferAdsFree, true));
        /* Filename settings */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Dateiname Einstellungen:"));
        final ConfigEntry origName = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.USEORIGINALFILENAME, JDL.L("plugins.hoster.SaveTv.UseOriginalFilename", "Original (Server) Dateinamen verwenden? <html><b>[Erst beim Downloadstart sichtbar!]</b></html>")).setDefaultValue(defaultUseOriginalFilename);
        getConfig().addEntry(origName);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.savetv.customdate", "Setze das Datumsformat:\r\nWichtige Information dazu:\r\nDas Datum erscheint im angegebenen Format im Dateinamen, allerdings nur,\r\nwenn man das *datum* Tag auch verwendet (siehe Benutzerdefinierte Dateinamen für Filme und Serien unten)")).setDefaultValue(defaultCustomDate).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* General settings description */
        final StringBuilder sbinfo = new StringBuilder();
        sbinfo.append("Erklärung zur Nutzung eigener Dateinamen:\r\n");
        sbinfo.append("Eigene Dateinamen lassen sich unten über ein Tag-System (siehe weiter unten) nutzen.\r\n");
        sbinfo.append("Das bedeutet, dass man die Struktur seiner gewünschten Dateinamen definieren kann.\r\n");
        sbinfo.append("Dabei hat man Tags wie z.B. *telecastid*, die dann durch Daten ersetzt werden.\r\n");
        sbinfo.append("Wichtig dabei ist, dass Tags immer mit einem Stern starten und enden.\r\n");
        sbinfo.append("Man darf nichts zwischen ein Tag schreiben z.B. *-telecastid-*, da das\r\n");
        sbinfo.append("zu unschönen Dateinamen führt und das Tag nicht die Daten ersetzt werden kann.\r\n");
        sbinfo.append("Wenn man die Tags trennen will muss man die anderen Zeichen zwischen Tags\r\n");
        sbinfo.append("z.B. '-*telecastid*-*endung*' -> Der Dateiname würde dann in etwa so aussehen: '-7573789-.mp4' (ohne die '')\r\n");
        sbinfo.append("WICHTIG: Tags, zu denen die Daten fehlen, werden standardmäßig durch '-' (Bindestrich) ersetzt!\r\n");
        sbinfo.append("Fehlen z.B. die Daten zu *genre*, steht statt statt dem Genre dann ein Bindestrich ('-') an dieser Stelle im Dateinamen.\r\n");
        sbinfo.append("Gut zu wissen: Statt dem Bindestrich lässt sich hierfür unten auch ein anderes Zeichen bzw. Zeichenfolge definieren.\r\n");
        sbinfo.append("Außerdem: Für Filme und Serien gibt es unterschiedliche Tags.\r\n");
        sbinfo.append("Kaputtmachen kannst du mit den Einstellungen prinzipiell nichts also probiere es einfach aus ;)\r\n");
        sbinfo.append("Tipp: Die Save.tv Plugin Einstellungen lassen sich rechts oben wieder auf ihre Standardwerte zurücksetzen!\r\n");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbinfo.toString()).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Filename settings for movies */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_MOVIES, JDL.L("plugins.hoster.savetv.customfilenamemovies", "Eigener Dateiname für Filme/Shows:")).setDefaultValue(defaultCustomFilenameMovies).setEnabledCondidtion(origName, false));
        final StringBuilder sb = new StringBuilder();
        sb.append("Erklärung der verfügbaren Tags:\r\n");
        sb.append("*server_dateiname* = Original Dateiname (ohne Dateiendung)\r\n");
        sb.append("*username* = Benutzername\r\n");
        sb.append("*datum* = Datum der Ausstrahlung der aufgenommenen Sendung\r\n[Erscheint im oben definierten Format, wird von der save.tv Seite ausgelesen]\r\n");
        sb.append("*genre* = Das Genre\r\n");
        sb.append("*produktionsland* = Name des Produktionslandes\r\n");
        sb.append("*produktionsjahr* = Produktionsjahr\r\n");
        sb.append("*sendername* = Name des TV-Senders auf dem die Sendung ausgestrahlt wurde");
        sb.append("*kategorie* = Kategorie, siehe telecast-ID Seite\r\n");
        sb.append("*quality* = Qualitätsstufe des Downloads - Entspricht den Werten 'LQ', 'HQ', 'HD' oder 'XX' für den unbekannten Status\r\n");
        sb.append("*werbefrei* = Schnittliste-Status des Downloads - Entspricht den Werten 'true', 'false' oder 'XX' für den unbekannten Status\r\n");
        sb.append("*videotitel* = Name des Videos ohne Dateiendung\r\n");
        sb.append("*zufallszahl* = Eine vierstellige Zufallszahl\r\n[Nützlich um Dateinamenkollisionen zu vermeiden]\r\n");
        sb.append("*telecastid* = Die id, die in jedem save.tv Link steht: TelecastID=XXXXXXX\r\n[Nützlich um Dateinamenkollisionen zu vermeiden]\r\n");
        sb.append("*endung* = Die Dateiendung, in diesem Fall immer '.mp4'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Filename settings for series */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_SERIES, JDL.L("plugins.hoster.savetv.customseriesfilename", "Eigener Dateiname für Serien:")).setDefaultValue(defaultCustomSeriesFilename).setEnabledCondidtion(origName, false));
        final StringBuilder sbseries = new StringBuilder();
        sbseries.append("Erklärung der verfügbaren Tags:\r\n");
        sbseries.append("*server_dateiname* = Original Dateiname (ohne Dateiendung)\r\n");
        sbseries.append("*username* = Benutzername\r\n");
        sbseries.append("*datum* = Datum der Ausstrahlung der aufgenommenen Sendung\r\n[Erscheint im oben definierten Format, wird von der save.tv Seite ausgelesen]\r\n");
        sbseries.append("*genre* = Das Genre\r\n");
        sbseries.append("*produktionsland* = Name des Produktionslandes\r\n");
        sbseries.append("*produktionsjahr* = Produktionsjahr\r\n");
        sbseries.append("*sendername* = Name des TV-Senders auf dem die Sendung ausgestrahlt wurde");
        sbseries.append("*kategorie* = Kategorie, siehe telecast-ID Seite\r\n");
        sbseries.append("*quality* = Qualitätsstufe des Downloads - Entspricht den Werten 'LQ', 'HQ', 'HD' oder 'XX' für den unbekannten Status\r\n");
        sbseries.append("*werbefrei* = Schnittliste-Status des Downloads - Entspricht den Werten 'true', 'false' oder 'XX' für den unbekannten Status\r\n");
        sbseries.append("*serientitel* = Name der Serie\r\n");
        sbseries.append("*episodenname* = Name der Episode\r\n");
        sbseries.append("*episodennummer* = Episodennummer\r\n");
        sbseries.append("*zufallszahl* = Eine vierstellige Zufallszahl\r\n[Nützlich um Dateinamenkollisionen zu vermeiden\r\n");
        sbseries.append("*telecastid* = Die id, die in jedem save.tv Link steht: TelecastID=XXXXXXX\r\n[Nützlich um Dateinamenkollisionen zu vermeiden]\r\n");
        sbseries.append("*endung* = Die Dateiendung, immer '.mp4'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbseries.toString()).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Advanced settings */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Erweiterte Einstellungen:\r\n<html><p style=\"color:#F62817\"><b>Warnung: Ändere die folgenden Einstellungen nur, wenn du weißt was du tust!\r\nMit einem Klick auf den gelben Pfeil rechts oben kannst du jederzeit zu den Standardeinstellungen zurück.</b></p></html>"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.ACTIVATE_BETA_FEATURES, JDL.L("plugins.hoster.SaveTv.ActivateBETAFeatures", "Aktiviere BETA-Features?\r\nINFO: Was diese Features sind und ob es aktuell welche gibt steht im Support Forum.")).setEnabled(defaultACTIVATE_BETA_FEATURES));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry api = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.USEAPI, JDL.L("plugins.hoster.SaveTv.UseAPI", "<html><p style=\"color:#F62817\">Update 06.07.2016: APIV2 ist nicht mehr funktionsfähig - die folgenden Einstellungen wird wieder verfügbar, sobald APIV3 eingebaut ist!</p><p>API verwenden?</p><p>INFO: Aktiviert man die API, sind einige Features wie folgt betroffen:</p>-ENTFÄLLT: Anzeigen der Account Details in der Account-Verwaltung (Account Typ, Ablaufdatum, ...)<p>-EINGESCHRÄNKT NUTZBAR: Benutzerdefinierte Dateinamen</p></html>")).setDefaultValue(defaultUSEAPI).setEnabled(false);
        getConfig().addEntry(api);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CONFIGURED_APIKEY, JDL.L("plugins.hoster.SaveTv.apikey", "Benutzerdefinierten API-Key eingeben:\r\n<html><p style=\"color:#F62817\"><b>Warnung:</b> Die API ist nur mit gültigem API-Key (in der Regel mit den Standardeinstellungen) nutzbar!</p></html>")).setDefaultValue(defaultCONFIGURED_APIKEY).setEnabledCondidtion(api, true).setEnabled(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.DISABLE_LINKCHECK, JDL.L("plugins.hoster.SaveTv.DisableLinkcheck", "Linkcheck deaktivieren <html><b>[Nicht empfohlen]</b>?\r\n<p style=\"color:#F62817\"><b>Vorteile:\r\n</b>-Links landen schneller im Linkgrabber und können auch bei Serverproblemen oder wenn die save.tv Seite komplett offline ist gesammelt werden\r\n<b>Nachteile:\r\n</b>-Im Linkgrabber werden zunächst nur die telecastIDs als Dateinamen angezeigt\r\n-Die endgültigen Dateinamen werden erst beim Downloadstart angezeigt</p></html>")).setDefaultValue(defaultDisableLinkcheck));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Soll die telecastID in irgendeinem Fall aus dem save.tv Archiv gelöscht werden?\r\n<html><p style=\"color:#F62817\"><b>Warnung:</b> Gelöschte telecastIDs können nicht wiederhergestellt werden!</p></html>"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.DELETE_TELECAST_ID_AFTER_DOWNLOAD, JDL.L("plugins.hoster.SaveTv.deleteFromArchiveAfterDownload", "Erfolgreich geladene telecastIDs aus dem save.tv Archiv löschen?")).setDefaultValue(defaultDeleteTelecastIDAfterDownload));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.DELETE_TELECAST_ID_IF_FILE_ALREADY_EXISTS, JDL.L("plugins.hoster.SaveTv.deleteFromArchiveIfFileAlreadyExists", "Falls Datei bereits auf der Festplatte existiert, telecastIDs aus dem save.tv Archiv löschen?")).setDefaultValue(defaultDeleteTelecastIDIfFileAlreadyExists));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_SEPERATION_MARK, JDL.L("plugins.hoster.savetv.customFilenameSeperationmark", "Trennzeichen als Ersatz für '/'  (da ungültig in Dateinamen):")).setDefaultValue(defaultCustomSeperationMark).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_EMPTY_TAG_STRING, JDL.L("plugins.hoster.savetv.customEmptyTagsString", "Zeichen, mit dem Tags ersetzt werden sollen, deren Daten fehlen:")).setDefaultValue(defaultCustomStringForEmptyTags).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final StringBuilder sbmore = new StringBuilder();
        sbmore.append("Definiere Filme oder Serien, für die trotz obiger Einstellungen die Originaldateinamen\r\n");
        sbmore.append("verwendet werden sollen.\r\n");
        sbmore.append("Manche mehrteiligen Filme haben dieselben Titel und bei manchen Serien fehlen die Episodennamen,\r\n");
        sbmore.append("wodurch sie alle dieselben Dateinamen bekommen -> JDownloader denkt es seien Duplikate/Mirrors und lädt nur\r\n");
        sbmore.append("einen der scheinbar gleichen Dateien.\r\n");
        sbmore.append("Um dies zu verhindern, kann man in den Eingabefeldern Namen solcher Filme/Serien eintragen,\r\n");
        sbmore.append("für die trotz obiger Einstellungen der Original Dateiname verwendet werden soll.\r\n");
        sbmore.append("Beispiel: 'serienname 1|serienname 2|usw.' (ohne die '')\r\n");
        sbmore.append("Die Eingabe erfolgt als RegEx. Wer nicht weiß was das ist -> Google");
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
                        message += "Diese Einstellungen sind nur in der Version JDownloader 2 BETA verfügbar unter:\r\nEinstellungen -> Plugin Einstellungen -> save.tv";
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
                        message += "- Viele Dateiinfos trotz aktiviertem schnellen Linkcheck\r\n";
                        message += "- Eigene Dateinamen auch bei aktiviertem schnellen Linkcheck (eingeschränkt)\r\n";
                        message += "- Die Möglichkeit, immer nur neue abgeschlossene Aufnahmen zu crawlen\r\n";
                        message += "- Die Möglichkeit, wahlweise alle oder nur Aufnahmen der letzten X Stunden zu crawlen\r\n";
                        message += "\r\n";
                        message += "Um den Crawler nutzen zu können, musst du ihn erst in den Plugin Einstellungen aktivieren.\r\n";
                        message += "\r\n";
                        message += "Die Crawler Einstellungen sind nur in der Version JDownloader 2 BETA verfügbar unter:\r\nEinstellungen -> Plugin Einstellungen -> save.tv";
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
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("featuredialog_new11042015_Shown", Boolean.FALSE) == false) {
                if (config.getProperty("featuredialog_new11042015_Shown2") == null) {
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
                config.setProperty("featuredialog_new11042015_Shown", Boolean.TRUE);
                config.setProperty("featuredialog_new11042015_Shown2", "shown");
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
                        title = "11.04.2015 - Save.tv Plugin - Neues Feature";
                        message += "Hallo lieber save.tv Nutzer/liebe save.tv NutzerIn\r\n";
                        message += "Das save.tv Crawler Plugin bietet neben Fehlerbehebungen seit dem 11.04.2015 folgendes neues Feature:\r\n";
                        message += "- Nur neue abgeschlossene Aufnahmen hinzufügen\r\n";
                        message += "Ist das Feature aktiviert, werden statt der eingestellten letzten X Stunden bzw. komplettes Archiv immer nur die Aufnahmen hinzugefügt, die du noch nicht in JDownloader eingefügt hast.\r\n";
                        message += "Bei der ersten Verwendung dieses Features wird das komplette Archiv eingefügt.\r\n";
                        message += "Danach bei jedem Crawlvorgang nur Aufnahmen, die noch nicht eingefügt wurden.\r\n";
                        message += "Das Feature kann jederzeit wieder deaktiviert werden um das 'bekannte Crawlverhalten' zurückzubekommen.\r\n";
                        message += "Außerdem kann man die Daten der letzten erfolgreichen Crawlvorgänge ab sofort in den Account Details einsehen.\r\n";
                        message += "\r\n";
                        message += "Die Crawler Einstellungen sind nur in der Version JDownloader 2 BETA verfügbar unter:\r\nEinstellungen -> Plugin Einstellungen -> save.tv";
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
            String acc_expire = "Niemals";
            final String acc_package = account.getStringProperty(PROPERTY_acc_package, "?");
            final String acc_price = account.getStringProperty(PROPERTY_acc_price, "?");
            final String acc_runtime = account.getStringProperty(PROPERTY_acc_runtime, "?");
            final String acc_count_archive_entries = account.getStringProperty(PROPERTY_acc_count_archive_entries, "?");
            final String acc_count_telecast_ids = account.getStringProperty(PROPERTY_acc_count_telecast_ids, "?");
            final String user_lastcrawl_newlinks_date;
            final String user_lastcrawl_date;
            final long time_last_crawl_ended_newlinks = this.getPluginConfig().getLongProperty(CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS, 0);
            final long time_last_crawl_ended = this.getPluginConfig().getLongProperty(CRAWLER_PROPERTY_LASTCRAWL, 0);
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
            panel.addStringPair("Zuletzt neue Sendungen (siehe Plugin Einstellung) gefunden:", user_lastcrawl_newlinks_date);
            panel.addHeader(_GUI.T.lit_download(), new AbstractIcon(IconKey.ICON_DOWNLOAD, 18));
            panel.addStringPair(_GUI.T.lit_max_simultanous_downloads(), "20");
            panel.addStringPair(_GUI.T.lit_max_chunks_per_link(), maxchunks);
            panel.addStringPair(_GUI.T.lit_interrupted_downloads_are_resumable(), _JDT.T.literally_yes());
        }
    }
}