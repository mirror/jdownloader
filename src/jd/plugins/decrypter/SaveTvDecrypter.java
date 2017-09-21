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
package jd.plugins.decrypter;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.SaveTv;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "save.tv" }, urls = { "https?://(www\\.)?save\\.tv/STV/M/obj/archive/(?:Horizontal)?VideoArchive\\.cfm" })
public class SaveTvDecrypter extends PluginForDecrypt {
    public SaveTvDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Settings stuff */
    @SuppressWarnings("deprecation")
    private final SubConfiguration       cfg                                 = SubConfiguration.getConfig("save.tv");
    // private static final String ACTIVATE_BETA_FEATURES = "ACTIVATE_BETA_FEATURES";
    private final String                 CRAWLER_ONLY_ADD_NEW_IDS            = "CRAWLER_ONLY_ADD_NEW_IDS";
    private final String                 CRAWLER_ACTIVATE                    = "CRAWLER_ACTIVATE";
    private final String                 CRAWLER_DISABLE_DIALOGS             = "CRAWLER_DISABLE_DIALOGS";
    private final String                 CRAWLER_LASTHOURS_COUNT             = "CRAWLER_LASTHOURS_COUNT";
    private static final String          CRAWLER_PROPERTY_TELECASTIDS_ADDED  = "CRAWLER_PROPERTY_TELECASTIDS_ADDED";
    private static final String          CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS = "CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS";
    private static final String          CRAWLER_PROPERTY_LASTCRAWL          = "CRAWLER_PROPERTY_LASTCRAWL";
    /* Decrypter constants */
    private static final int             API_ENTRIES_PER_REQUEST             = 1000;
    /* Website gets max 35 items per request. Using too much = server will ate us and return response code 500! */
    private static final int             SITE_ENTRIES_PER_REQUEST            = 100;
    /*
     * Max time in which save.tv recordings are saved inside a users' account. This value is only used to cleanup the internal HashMap of
     * 'already downloaded' telecastIDs!
     */
    private static final long            TELECAST_ID_EXPIRE_TIME             = 62 * 24 * 60 * 60 * 1000l;
    /* Decrypter variables */
    final ArrayList<DownloadLink>        decryptedLinks                      = new ArrayList<DownloadLink>();
    final ArrayList<String>              dupecheckList                       = new ArrayList<String>();
    private static HashMap<String, Long> crawledTelecastIDsMap               = new HashMap<String, Long>();
    private long                         grab_last_hours_num                 = 0;
    private long                         tdifference_milliseconds            = 0;
    private int                          totalLinksNum                       = 0;
    private int                          foundLinksNum                       = 0;
    private int                          totalAccountsNum                    = 0;
    private int                          totalAccountsLoggedInSuccessfulNum  = 0;
    private int                          requestCountMax                     = 1;
    private long                         time_crawl_started                  = 0;
    private long                         time_last_crawl_ended               = 0;
    /* Settings */
    private boolean                      crawler_DialogsDisabled             = false;
    private boolean                      api_enabled                         = false;
    private boolean                      only_grab_new_entries               = false;
    /* If this != null, API is currently used */
    private boolean                      fast_linkcheck                      = false;
    private String                       parameter                           = null;

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FOR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /*
     * Never run multiple crawl instances of this plugin at once - but actually because we only have one matching input-URL it is not
     * possible anyways.
     */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        time_crawl_started = System.currentTimeMillis();
        parameter = param.toString();
        api_enabled = jd.plugins.hoster.SaveTv.is_API_enabled(this.getHost());
        fast_linkcheck = cfg.getBooleanProperty(jd.plugins.hoster.SaveTv.CRAWLER_ENABLE_FAST_LINKCHECK, false);
        crawler_DialogsDisabled = cfg.getBooleanProperty(CRAWLER_DISABLE_DIALOGS, false);
        only_grab_new_entries = cfg.getBooleanProperty(CRAWLER_ONLY_ADD_NEW_IDS, false);
        grab_last_hours_num = cfg.getLongProperty(CRAWLER_LASTHOURS_COUNT, 0);
        time_last_crawl_ended = cfg.getLongProperty(CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS, 0);
        this.br.setFollowRedirects(true);
        this.br.setLoadLimit(this.br.getLoadLimit() * 3);
        if (!cfg.getBooleanProperty(CRAWLER_ACTIVATE, false)) {
            logger.info("save.tv: Decrypting save.tv archives is disabled, doing nothing...");
            return decryptedLinks;
        }
        try {
            final ArrayList<Account> all_stv_accounts = AccountController.getInstance().getValidAccounts(this.getHost());
            totalAccountsNum = all_stv_accounts.size();
            if (totalAccountsNum == 0) {
                logger.info("At least one account needed to use this crawler");
                return decryptedLinks;
            }
            for (final Account stvacc : all_stv_accounts) {
                if (!getUserLogin(stvacc, false)) {
                    logger.info("Failed to log in account: " + stvacc.getUser());
                    continue;
                }
                dupecheckList.clear();
                totalAccountsLoggedInSuccessfulNum++;
                if (only_grab_new_entries) {
                    /* Load list of saved IPs + timestamp of last download */
                    final Object crawledIDSMap = stvacc.getProperty(CRAWLER_PROPERTY_TELECASTIDS_ADDED);
                    if (crawledIDSMap != null && crawledIDSMap instanceof HashMap) {
                        crawledTelecastIDsMap = (HashMap<String, Long>) crawledIDSMap;
                    }
                } else {
                    tdifference_milliseconds = grab_last_hours_num * 60 * 60 * 1000;
                }
                if (api_enabled) {
                    api_decrypt_All(stvacc);
                } else {
                    site_decrypt_All(stvacc);
                }
                /*
                 * Let's clean our ID map. TelecastIDs automatically get deleted after 30 days (when this documentation was written) so we
                 * do not need to store them longer than that as it will eat up more RAM/space for no reason.
                 */
                synchronized (crawledTelecastIDsMap) {
                    final Iterator<Entry<String, Long>> it = crawledTelecastIDsMap.entrySet().iterator();
                    while (it.hasNext()) {
                        final Entry<String, Long> entry = it.next();
                        final long timestamp = entry.getValue();
                        if (System.currentTimeMillis() - timestamp >= TELECAST_ID_EXPIRE_TIME) {
                            /* Remove old entries */
                            it.remove();
                        }
                    }
                }
                /* Save telecastID map so later we know what is new and what we crawled before ;) */
                stvacc.setProperty(CRAWLER_PROPERTY_TELECASTIDS_ADDED, crawledTelecastIDsMap);
            }
            if (foundLinksNum >= totalLinksNum) {
                if (decryptedLinks.size() > 0) {
                    cfg.setProperty(CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS, System.currentTimeMillis());
                }
                cfg.setProperty(CRAWLER_PROPERTY_LASTCRAWL, System.currentTimeMillis());
            }
            logger.info("save.tv: total links found: " + decryptedLinks.size() + " of " + totalLinksNum);
        } catch (final Throwable e) {
            logger.info("save.tv: total links found: " + decryptedLinks.size() + " of " + totalLinksNum);
            if (decryptedLinks.size() >= totalLinksNum) {
                /* This can happen if the user aborts but the crawler already found all links. */
                handleEndDialogs();
                return decryptedLinks;
            }
            if (e instanceof DecrypterException) {
                logger.info("Decrypt process aborted by user: " + parameter);
                if (!crawler_DialogsDisabled) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String title = "Save.tv Archiv-Crawler - Crawler abgebrochen";
                                    String message = "Save.tv - Der Crawler wurde frühzeitig vom Benutzer beendet!\r\n";
                                    message += "Es wurden bisher " + decryptedLinks.size() + " von " + totalLinksNum + " Links (telecastIDs) gefunden!";
                                    message += getDialogEnd();
                                    JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
                                } catch (final Throwable e) {
                                }
                            }
                        });
                    } catch (Throwable e2) {
                    }
                }
            } else if (e instanceof BrowserException) {
                try {
                    e.printStackTrace();
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - Archiv nicht komplett gefunden (Server Fehler)";
                                String message = "Save.tv - leider wurden nicht alle Links des Archives gefunden!\r\n";
                                message += "Während dem Crawlen ist es zu einem Serverfehler gekommen!\r\n";
                                message += "Wir empfehlen, es zu einem späteren Zeitpunkt nochmals zu versuchen.\r\n";
                                message += "Es wurden nur " + decryptedLinks.size() + " von " + totalLinksNum + " Links (telecastIDs) gefunden!";
                                message += getDialogEnd();
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
                            } catch (Throwable e) {
                            }
                        }
                    });
                } catch (Throwable ebr) {
                }
            } else {
                try {
                    e.printStackTrace();
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - Archiv nicht komplett gefunden (Unbekannter Fehler)";
                                String message = "Save.tv - leider wurden nicht alle Links des Archives gefunden!\r\n";
                                message += "Während dem Crawlen ist es zu einem unbekannten Fehler gekommen!\r\n";
                                message += "Wir empfehlen, es zu einem späteren Zeitpunkt nochmals zu versuchen und uns den Fehler ggf. zu melden.\r\n";
                                message += "Es wurden nur " + decryptedLinks.size() + " von " + totalLinksNum + " Links (telecastIDs) gefunden!";
                                message += getDialogEnd();
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
                            } catch (Throwable e) {
                            }
                        }
                    });
                } catch (Throwable ebr) {
                }
            }
            return decryptedLinks;
        }
        handleEndDialogs();
        return decryptedLinks;
    }

    /** This will first grab alle IDs, then their details as the 2nd used request returns more information than the first one. */
    @SuppressWarnings("unchecked")
    private void api_decrypt_All(final Account acc) throws Exception {
        /*
         * We need the parameters here already as we might limit what the API sends us so we should apply the filter to the 'count' request
         * as well so that we get a correct number.
         */
        final String api_records_parameters = getParametersRecordsAPI();
        /* First let's find the number of items to expect */
        api_GET(this.br, "/records/count" + "?" + api_records_parameters);
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        totalLinksNum += (int) JavaScriptEngineFactory.toLong(entries.get("count"), 0);
        if (totalLinksNum == 0) {
            logger.info("WTF zero entries in archive of current account");
            return;
        }
        /* Find out how many requests we will need */
        final BigDecimal bd = new BigDecimal((double) totalLinksNum / API_ENTRIES_PER_REQUEST);
        requestCountMax = bd.setScale(0, BigDecimal.ROUND_UP).intValue();
        // /* We do not want entries which are in the future! */
        // final String formattedMaxDate = formatToStvDate(time_crawl_started);
        /* Now let's decrypt everything */
        int offset = 0;
        int currentRequestCount = 0;
        /**
         * 'recordstates' Values: <br />
         * 1 = The user has requested the format. <br />
         * 2 = The format was successfully recorded or the recording process failed.<br />
         * 3 = The format was recorded and encoded successful and the user can download the format.<br />
         * 4 = The recording or encoding process produced errors. The user cannot download the format.<br />
         * 5 = The user has deleted the format. <br />
         * (Comma separated)
         */
        final String api_get_data = "?fields=" + jd.plugins.hoster.SaveTv.getRecordsFieldsValue() + "&" + api_records_parameters + "&offset=" + offset;
        /* API does not tell us the total number of telecastIDs so let's find that out first! */
        ArrayList<Object> ressourcelist;
        do {
            if (this.isAbort()) {
                throw new DecrypterException("Decrypt aborted!");
            }
            logger.info("Request " + currentRequestCount + " of " + requestCountMax);
            api_GET(this.br, "/records" + api_get_data + offset);
            ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            for (final Object telecastID_o : ressourcelist) {
                addID_api(acc, telecastID_o);
                offset++;
            }
            logger.info("Found " + ressourcelist.size() + " telecastIDs so far");
            currentRequestCount++;
        } while (ressourcelist.size() >= API_ENTRIES_PER_REQUEST);
        /* Save number of telecastIDs on account to display information in account information. */
        acc.setProperty(SaveTv.PROPERTY_acc_count_telecast_ids, Integer.toString(totalLinksNum));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void site_decrypt_All(final Account acc) throws Exception {
        boolean is_groups_enabled = false;
        boolean groups_enabled_by_user = false;
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        getPageSafe(acc, "https://www.save.tv/STV/M/obj/archive/JSON/VideoArchiveApi.cfm?" + "iEntriesPerPage=1&iCurrentPage=1&dStartdate=0");
        is_groups_enabled = !br.containsHTML("\"IGROUPCOUNT\":1\\.0");
        groups_enabled_by_user = is_groups_enabled;
        final String totalLinksInsideCurrentAccount = PluginJSonUtils.getJsonValue(this.br, "ITOTALENTRIES");
        if (totalLinksInsideCurrentAccount == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return;
        }
        final int totalLinksInsideCurrentAccount_int = (int) Double.parseDouble(totalLinksInsideCurrentAccount);
        /* Parse as double as 'totalLinks' can contain dots although it makes absolutely no sense as that number will always be flat! */
        totalLinksNum += totalLinksInsideCurrentAccount_int;
        if (totalLinksInsideCurrentAccount_int == 0) {
            logger.info("WTF zero entries in archive of current account");
            return;
        }
        /* Save on account to display in account information */
        acc.setProperty(SaveTv.PROPERTY_acc_count_telecast_ids, Integer.toString(totalLinksInsideCurrentAccount_int));
        final BigDecimal bd = new BigDecimal((double) totalLinksNum / SITE_ENTRIES_PER_REQUEST);
        requestCountMax = bd.setScale(0, BigDecimal.ROUND_UP).intValue();
        int added_entries;
        final long date_current = System.currentTimeMillis();
        /* 2 months before current date */
        final long date_start = date_current - 5259492000l;
        /* One month after current date */
        final long date_end = date_current + 2629746000l;
        final String targetFormat = "yyyy-MM-dd";
        final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
        final String date_start_formatted = formatter.format(date_start);
        final String date_end_formatted = formatter.format(date_end);
        try {
            for (int request_num = 1; request_num <= requestCountMax; request_num++) {
                added_entries = 0;
                if (this.isAbort()) {
                    throw new DecrypterException("Decryption aborted!");
                }
                logger.info("Decrypting request " + request_num + " of " + requestCountMax);
                if (is_groups_enabled) {
                    /* Disable stupid groups setting to crawl faster and to make it work anyways */
                    logger.info("Disabling groups setting");
                    this.br.postPage("/STV/M/obj/user/submit/submitVideoArchiveOptions.cfm", "ShowGroupedVideoArchive=false");
                    is_groups_enabled = false;
                }
                /* 2016-09-14: dStartdate and dEnddate parameters are important now! */
                this.br.postPage("/STV/M/obj/archive/JSON/VideoArchiveApi.cfm", "iEntriesPerPage=" + SITE_ENTRIES_PER_REQUEST + "&iCurrentPage=" + request_num + "&dStartdate=" + date_start_formatted + "&dEnddate=" + date_end_formatted);
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                final ArrayList<Object> resource_data_list = (ArrayList) entries.get("ARRVIDEOARCHIVEENTRIES");
                for (final Object singleid_information : resource_data_list) {
                    addID_site(acc, singleid_information);
                    added_entries++;
                    foundLinksNum++;
                }
                logger.info("Found " + added_entries + " entries in request " + request_num + " of " + requestCountMax);
                if (added_entries == 0) {
                    logger.info("Can't find any entries, stopping at request: " + request_num + " of " + requestCountMax);
                    break;
                }
            }
        } finally {
            try {
                if (groups_enabled_by_user && !is_groups_enabled) {
                    /* Restore users' groups-setting after decryption if changed */
                    logger.info("Re-enabling groups setting");
                    this.br.postPage("https://www." + this.getHost() + "/STV/M/obj/user/submit/submitVideoArchiveOptions.cfm", "ShowGroupedVideoArchive=true");
                    logger.info("Successfully re-enabled groups setting");
                }
            } catch (final Throwable settingfail) {
                logger.info("Failed to restore previous groups setting");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addID_api(final Account acc, final Object json_o) throws ParseException, DecrypterException, PluginException {
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) json_o;
        final String telecast_id = Long.toString(JavaScriptEngineFactory.toLong(entries.get("telecastId"), -1));
        if (telecast_id.equalsIgnoreCase("-1")) {
            throw new DecrypterException("Decryption aborted because of BAD telecastID");
        } else if (dupecheckList.contains(telecast_id)) {
            throw new DecrypterException("Decryption aborted because of dupecheck-failure!");
        }
        final DownloadLink dl = createStvDownloadlink(acc, telecast_id);
        /* 2017-09-21: API provides all information we need which is why we do not need a 'fast-linkcheck-setting' anymore! */
        dl.setAvailable(true);
        jd.plugins.hoster.SaveTv.parseFilenameInformation_api(dl, entries, true);
        jd.plugins.hoster.SaveTv.parseQualityTagAPI(dl, jd.plugins.hoster.SaveTv.jsonGetFormatArrayAPI(entries));
        if (telecastID_IS_AllowedGeneral(dl)) {
            dl.setName(jd.plugins.hoster.SaveTv.getFilename(this, dl));
            distribute(dl);
            decryptedLinks.add(dl);
        }
        /* No matter whether we added the ID or not - we need it on our dupecheck list! */
        dupecheckList.add(telecast_id);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addID_site(final Account acc, final Object object_source) throws ParseException, DecrypterException, PluginException {
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) object_source;
        entries = (LinkedHashMap<String, Object>) entries.get("STRTELECASTENTRY");
        final String telecastURL = (String) entries.get("SDETAILSURL");
        final String telecast_id = new Regex(telecastURL, "(\\d+)$").getMatch(0);
        if (dupecheckList.contains(telecast_id)) {
            throw new DecrypterException("Decryption aborted because of dupecheck-failure!");
        }
        final DownloadLink dl = createStvDownloadlink(acc, telecast_id);
        if (fast_linkcheck) {
            dl.setAvailable(true);
        }
        jd.plugins.hoster.SaveTv.parseFilenameInformation_site(dl, entries);
        jd.plugins.hoster.SaveTv.parseQualityTagWebsite(dl, (ArrayList) entries.get("ARRALLOWDDOWNLOADFORMATS"));
        if (telecastID_IS_AllowedWebsite(dl)) {
            dl.setName(jd.plugins.hoster.SaveTv.getFilename(this, dl));
            distribute(dl);
            decryptedLinks.add(dl);
        }
        /* No matter whether we added the ID or not - we need it on our dupecheck list! */
        dupecheckList.add(telecast_id);
    }

    private DownloadLink createStvDownloadlink(final Account acc, final String telecastID) {
        final String account_username = acc.getUser();
        final String telecast_url = "https://www.save.tv/STV/M/obj/archive/VideoArchiveDetails.cfm?TelecastId=" + telecastID;
        final DownloadLink dl = createDownloadlink(telecast_url);
        dl.setName(telecastID + ".mp4");
        dl.setContentUrl(telecast_url);
        dl.setLinkID(account_username + telecastID);
        /* Property is needed to later determine which url is downloadable via which account. */
        dl.setProperty(jd.plugins.hoster.SaveTv.PROPERTY_downloadable_via, account_username);
        return dl;
    }

    /** Checks if telecastID should be added in respects of the users' settings. */
    private boolean telecastID_IS_AllowedWebsite(final DownloadLink dl) {
        final long datemilliseconds = dl.getLongProperty(jd.plugins.hoster.SaveTv.PROPERTY_originaldate, 0);
        final long current_tdifference = time_crawl_started - datemilliseconds;
        boolean isAllowed = false;
        if (!only_grab_new_entries && (tdifference_milliseconds == 0 || current_tdifference <= tdifference_milliseconds)) {
            /* User only wants to add telecastIDs of a user-defined time-range. */
            /*
             * TODO: For API handling: Maybe add timeframe to filter parameters so that the Stv servers filter out (most of) the items we
             * don't want.
             */
            isAllowed = true;
        }
        if (!only_grab_new_entries && (tdifference_milliseconds == 0 || current_tdifference <= tdifference_milliseconds)) {
            /* User only wants to add telecastIDs of a user-defined time-range. */
            /*
             * TODO: For API handling: Maybe add timeframe to filter parameters so that the Stv servers filter out (most of) the items we
             * don't want.
             */
            isAllowed = true;
        } else {
            isAllowed = telecastID_IS_AllowedGeneral(dl);
        }
        return isAllowed;
    }

    /** Checks if telecastID should be added in respects of the users' settings. */
    private boolean telecastID_IS_AllowedGeneral(final DownloadLink dl) {
        final long datemilliseconds = dl.getLongProperty(jd.plugins.hoster.SaveTv.PROPERTY_originaldate, 0);
        /* TODO: Change from property to 'getLinkid()' */
        final String telecastID = dl.getLinkID();
        final boolean telecastIDHasBeenCrawledBefore = crawledTelecastIDsMap.containsKey(telecastID);
        final boolean isAllowed;
        if (only_grab_new_entries && !telecastIDHasBeenCrawledBefore) {
            /* User only wants telecastIDs which he did not add before and this ID has not been added before --> Allow to add it! */
            crawledTelecastIDsMap.put(telecastID, datemilliseconds);
            isAllowed = true;
        } else if (only_grab_new_entries && telecastIDHasBeenCrawledBefore) {
            isAllowed = false;
        } else {
            isAllowed = true;
        }
        return isAllowed;
    }

    public static String formatToStvDateAPI(final long milliseconds) {
        final Date theDate = new Date(milliseconds);
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'");
        final String formattedDate = formatter.format(theDate);
        return formattedDate;
    }

    /** Returns basic parameters for '/records' request. */
    private String getParametersRecordsAPI() {
        final String formattedMinDate;
        if (grab_last_hours_num > 0) {
            formattedMinDate = formatToStvDateAPI(System.currentTimeMillis() - (grab_last_hours_num * 60 * 60 * 1000));
        } else {
            formattedMinDate = null;
        }
        String api_records_parameters = "nopagingheader=false&recordstates=2&limit=" + API_ENTRIES_PER_REQUEST;
        if (formattedMinDate != null) {
            api_records_parameters += "&minstartdate=" + Encoding.urlEncode(formattedMinDate);
        }
        return api_records_parameters;
    }

    @SuppressWarnings("deprecation")
    private boolean getUserLogin(final Account aa, final boolean force) throws Exception {
        if (aa == null) {
            return false;
        }
        try {
            if (api_enabled) {
                jd.plugins.hoster.SaveTv.login_api(this.br, aa, force);
            } else {
                jd.plugins.hoster.SaveTv.login_website(this.br, aa, force);
            }
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    private void getPageSafe(final Account acc, final String url) throws Exception {
        // Limits made by me (pspzockerscene):
        // Max 6 logins possible
        // Max 15 accesses of the link possible
        // -> Max 21 total requests
        int failcounter_url = 0;
        for (int i = 0; i <= 2; i++) {
            boolean failed = true;
            do {
                try {
                    this.br.getPage(url);
                    failed = false;
                } catch (final BrowserException e) {
                    failed = true;
                    failcounter_url++;
                    if (failcounter_url > 4) {
                        logger.info("Failed to avoid timeouts / server issues");
                        throw e;
                    }
                }
            } while (failed);
            if (this.br.getURL().contains(jd.plugins.hoster.SaveTv.URL_LOGGED_OUT)) {
                for (int i2 = 0; i2 <= 1; i2++) {
                    logger.info("Link redirected to login page, logging in again to retry this: " + url);
                    logger.info("Try " + i2 + " of 1");
                    try {
                        getUserLogin(acc, true);
                    } catch (final BrowserException e) {
                        logger.info("Login " + i2 + "of 1 failed, re-trying...");
                        continue;
                    }
                    logger.info("Re-Login " + i2 + "of 1 successful...");
                    break;
                }
                continue;
            }
            break;
        }
    }

    /**
     * Performs save.tv API GET requests. <br />
     * TODO: Add errorhandling
     *
     * @throws Exception
     */
    private String api_GET(final Browser br, String url) throws Exception {
        url = jd.plugins.hoster.SaveTv.correctURLAPI(url);
        br.getPage(url);
        return br.toString();
    }

    // }
    private void handleEndDialogs() {
        if (!crawler_DialogsDisabled) {
            final long lastcrawl_ago = System.currentTimeMillis() - time_last_crawl_ended;
            if (only_grab_new_entries && decryptedLinks.size() == 0 && time_last_crawl_ended > 0) {
                /* User recently added all new entries and now there are no new entries available. */
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - nichts Neues gefunden";
                                String message = "Save.tv - es wurden keine neuen Aufnahmen gefunden!\r\n";
                                message += "Bedenke, dass du vor " + TimeFormatter.formatMilliSeconds(lastcrawl_ago, 0) + " bereits alle neuen Aufnahmen eingefügt hast.\r\n";
                                message += "Vermutlich gab es bisher keine neuen Aufnahmen!\r\n";
                                message += getDialogAccountsInfo();
                                message += getDialogEnd();
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
                            } catch (final Throwable e) {
                            }
                        }
                    });
                } catch (final Throwable e) {
                }
            } else if (only_grab_new_entries) {
                /* User recently added all new entries and now there are no new entries available. */
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - neue Einträge gefunden";
                                String message = "Save.tv - es wurden " + decryptedLinks.size() + " neue Aufnahmen gefunden!\r\n";
                                message += getDialogAccountsInfo();
                                message += getDialogEnd();
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                            } catch (final Throwable e) {
                            }
                        }
                    });
                } catch (final Throwable e) {
                }
            } else if (grab_last_hours_num > 0 && decryptedLinks.size() == 0) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - nichts gefunden";
                                String message = "Save.tv - leider wurden keine Links gefunden!\r\n";
                                message += "Bedenke, dass du nur alle Aufnahmen der letzten " + grab_last_hours_num + " Stunden wolltest.\r\n";
                                message += "Vermutlich gab es in diesem Zeitraum keine neuen Aufnahmen!\r\n";
                                message += getDialogAccountsInfo();
                                message += getDialogEnd();
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
                            } catch (final Throwable e) {
                            }
                        }
                    });
                } catch (final Throwable e) {
                }
            } else if (grab_last_hours_num > 0) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - alle Aufnahmen der letzten " + grab_last_hours_num + " Stunden wurden gefunden";
                                String message = "Save.tv Archiv-Crawler - alle Aufnahmen der letzten " + grab_last_hours_num + " Stunden wurden gefunden!\r\n";
                                message += "Es wurden " + decryptedLinks.size() + " Links gefunden!\r\n";
                                message += getDialogAccountsInfo();
                                message += getDialogEnd();
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                            } catch (Throwable e) {
                            }
                        }
                    });
                } catch (final Throwable e) {
                }
            } else if (decryptedLinks.size() >= totalLinksNum) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler -Alle Aufnahmen des Archives gefunden";
                                String message = "Save.tv - alle Links des Archives wurden gefunden!\r\n";
                                message += "Es wurden " + decryptedLinks.size() + " von " + totalLinksNum + " Links gefunden!\r\n";
                                message += getDialogAccountsInfo();
                                message += getDialogEnd();
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                            } catch (Throwable e) {
                            }
                        }
                    });
                } catch (final Throwable e) {
                }
            } else if (decryptedLinks.size() < totalLinksNum) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - Fehler beim Crawlen des kompletten Archives";
                                String message = "Save.tv - leider wurden nicht alle Links des Archives gefunden!\r\n";
                                message += "Es wurden nur " + decryptedLinks.size() + " von " + totalLinksNum + " Links (telecastIDs) gefunden!\r\n";
                                message += getDialogAccountsInfo();
                                message += getDialogEnd();
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
                            } catch (final Throwable e) {
                            }
                        }
                    });
                } catch (final Throwable e) {
                }
            }
        }
    }

    private String getDialogAccountsInfo() {
        final String message = "Es wurden Archive von insgesamt " + totalAccountsNum + " save.tv Account(s) durchsucht.\r\nDavon erfolgreich: " + totalAccountsLoggedInSuccessfulNum;
        return message;
    }

    private String getDialogEnd() {
        final long crawl_duration = System.currentTimeMillis() - time_crawl_started;
        String message = "\r\n";
        message += "Dauer des Crawlvorganges: " + TimeFormatter.formatMilliSeconds(crawl_duration, 0);
        message += "\r\n\r\nGenervt von diesen Info-Dialogen? In den Plugin Einstellungen kannst du sie deaktivieren ;)";
        return message;
    }
}
