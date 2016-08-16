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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
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

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "save.tv" }, urls = { "https?://(www\\.)?save\\.tv/STV/M/obj/archive/(?:Horizontal)?VideoArchive\\.cfm" }, flags = { 0 })
public class SaveTvDecrypter extends PluginForDecrypt {

    public SaveTvDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Settings stuff */
    @SuppressWarnings("deprecation")
    private final SubConfiguration       cfg                                  = SubConfiguration.getConfig("save.tv");
    private static final String          ACTIVATE_BETA_FEATURES               = "ACTIVATE_BETA_FEATURES";
    private final String                 USEAPI                               = "USEAPI";
    private final String                 CRAWLER_ONLY_ADD_NEW_IDS             = "CRAWLER_ONLY_ADD_NEW_IDS";
    private final String                 CRAWLER_ENABLE_FASTER                = "CRAWLER_ENABLE_FASTER_2";
    private final String                 CRAWLER_ACTIVATE                     = "CRAWLER_ACTIVATE";
    private final String                 CRAWLER_DISABLE_DIALOGS              = "CRAWLER_DISABLE_DIALOGS";
    private final String                 CRAWLER_LASTHOURS_COUNT              = "CRAWLER_LASTHOURS_COUNT";

    private static final String          CRAWLER_PROPERTY_TELECASTIDS_ADDED   = "CRAWLER_PROPERTY_TELECASTIDS_ADDED";
    private static final String          CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS  = "CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS";
    private static final String          CRAWLER_PROPERTY_LASTCRAWL           = "CRAWLER_PROPERTY_LASTCRAWL";

    /* Decrypter constants */
    private static final int             API_ENTRIES_PER_REQUEST              = 1000;
    /* Website gets max 35 items per request. Using too much = server will ate us and return response code 500! */
    private static final int             SITE_ENTRIES_PER_REQUEST             = 100;
    /* Max time in which save.tv recordings are saved inside a users' account. */
    private static final long            TELECAST_ID_EXPIRE_TIME              = 32 * 24 * 60 * 60 * 1000l;
    private static final long            waittime_between_crawl_page_requests = 2500;

    /* Decrypter variables */
    final ArrayList<DownloadLink>        decryptedLinks                       = new ArrayList<DownloadLink>();
    final ArrayList<String>              dupecheckList                        = new ArrayList<String>();
    private static HashMap<String, Long> crawledTelecastIDsMap                = new HashMap<String, Long>();
    private long                         grab_last_hours_num                  = 0;
    private long                         tdifference_milliseconds             = 0;

    private int                          totalLinksNum                        = 0;
    private int                          foundLinksNum                        = 0;
    private int                          totalAccountsNum                     = 0;
    private int                          totalAccountsLoggedInSuccessfulNum   = 0;
    private int                          requestCount                         = 1;
    private long                         time_crawl_started                   = 0;
    private long                         time_last_crawl_ended                = 0;

    /* Settings */
    private boolean                      crawler_DialogsDisabled              = false;
    private boolean                      api_enabled                          = false;
    private boolean                      only_grab_new_entries                = false;

    /* If this != null, API is currently used */
    private boolean                      fast_linkcheck                       = false;
    private String                       parameter                            = null;

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
        api_enabled = cfg.getBooleanProperty(USEAPI, false);
        fast_linkcheck = cfg.getBooleanProperty(CRAWLER_ENABLE_FASTER, false);
        crawler_DialogsDisabled = cfg.getBooleanProperty(CRAWLER_DISABLE_DIALOGS, false);
        only_grab_new_entries = cfg.getBooleanProperty(CRAWLER_ONLY_ADD_NEW_IDS, false);
        grab_last_hours_num = getLongProperty(cfg, CRAWLER_LASTHOURS_COUNT, 0);
        time_last_crawl_ended = getLongProperty(cfg, CRAWLER_PROPERTY_LASTCRAWL_NEWLINKS, 0);
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
    private void api_decrypt_All(final Account acc) throws Exception {
        final ArrayList<String> temp_telecastIDs = new ArrayList<String>();
        int offset = 0;
        /* API does not tell us the total number of telecastIDs so let's find that out first! */
        while (true) {
            if (this.isAbort()) {
                throw new DecrypterException("Decrypt aborted!");
            }
            jd.plugins.hoster.SaveTv.api_doSoapRequestSafe(this.br, acc, "http://tempuri.org/IVideoArchive/SimpleParamsGetVideoArchiveList", "<channelId>0</channelId><filterType>0</filterType><recordingState>0</recordingState><tvCategoryId>0</tvCategoryId><tvSubCategoryId>0</tvSubCategoryId><textSearchType>0</textSearchType><from>" + offset + "</from><count>" + (offset + API_ENTRIES_PER_REQUEST) + "</count>");
            final String[] telecastIDs = br.getRegex("Stv\\.Api\\.Contract\\.Telecast\">(\\d+)</Id>").getColumn(0);
            for (final String telecastID : telecastIDs) {
                temp_telecastIDs.add(telecastID);
                offset++;
            }
            logger.info("Found " + temp_telecastIDs.size() + " RAW telecastIDs so far");
            if (telecastIDs.length < API_ENTRIES_PER_REQUEST) {
                logger.info("Seems like we found all telecastIDs --> Getting further information");
                break;
            }
        }

        /* Save number of telecastIDs on account to display information in account information. */
        totalLinksNum = temp_telecastIDs.size();
        acc.setProperty(SaveTv.PROPERTY_acc_count_telecast_ids, Integer.toString(totalLinksNum));
        final BigDecimal bd = new BigDecimal((double) totalLinksNum / API_ENTRIES_PER_REQUEST);
        requestCount = bd.setScale(0, BigDecimal.ROUND_UP).intValue();
        /* Reset offset as we re-use that variable. */
        offset = 0;
        /* Finally crawl telecastIDs. */
        int request_num = 1;
        int added_entries;
        while (true) {
            added_entries = 0;
            if (this.isAbort()) {
                throw new DecrypterException("Decrypt aborted!");
            }
            String telecastid_post_data = "";
            for (int i = 0; i <= API_ENTRIES_PER_REQUEST - 1; i++) {
                if (offset >= totalLinksNum) {
                    break;
                }
                telecastid_post_data += "<a:int>" + temp_telecastIDs.get(offset) + "</a:int>";
                offset++;
            }
            logger.info("Decrypting request " + request_num + " of " + requestCount);
            /* Wait some time or the server migh return response 500 */
            Thread.sleep(waittime_between_crawl_page_requests);
            jd.plugins.hoster.SaveTv.api_doSoapRequestSafe(this.br, acc, "http://tempuri.org/ITelecast/GetTelecastDetail", "<telecastIds xmlns:a=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">" + telecastid_post_data + "</telecastIds><detailLevel>2</detailLevel>");
            final String[] entries = br.getRegex("<a:TelecastDetail>(.*?)</a:TelecastDetail>").getColumn(0);
            if (entries == null || entries.length == 0) {
                logger.info("save.tv. Can't find entries, stopping at request: " + request_num + " of " + requestCount);
                break;
            }
            for (final String entry : entries) {
                addID_api(acc, entry);
                foundLinksNum++;
                added_entries++;
            }
            logger.info("Found " + added_entries + " entries in request " + request_num + " of " + requestCount);
            if (added_entries == 0) {
                logger.info("Can't find any entries, stopping at request: " + request_num + " of " + requestCount);
                break;
            }
            if (offset >= totalLinksNum) {
                logger.info("Seems like decryption is complete --> Stopping!");
                break;
            }
            request_num++;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    private void site_decrypt_All(final Account acc) throws Exception {
        boolean is_groups_enabled = false;
        boolean groups_enabled_by_user = false;
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        getPageSafe(acc, "https://www.save.tv/STV/M/obj/archive/JSON/VideoArchiveApi.cfm?" + "iEntriesPerPage=1&iCurrentPage=1&dStartdate=0");
        is_groups_enabled = !br.containsHTML("\"IGROUPCOUNT\":1\\.0");
        groups_enabled_by_user = is_groups_enabled;
        final String totalLinksInsideCurrentAccount = PluginJSonUtils.getJson(this.br, "ITOTALENTRIES");
        if (totalLinksInsideCurrentAccount == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return;
        }
        final int totalLinksInsideCurrentAccount_int = (int) Double.parseDouble(totalLinksInsideCurrentAccount);
        /* Parse as double as 'totalLinks' can contain dots although it makes absolutely no sense as that number will always be flat! */
        totalLinksNum += totalLinksInsideCurrentAccount_int;
        /* Save on account to display in account information */
        acc.setProperty(SaveTv.PROPERTY_acc_count_telecast_ids, Integer.toString(totalLinksInsideCurrentAccount_int));
        final BigDecimal bd = new BigDecimal((double) totalLinksNum / SITE_ENTRIES_PER_REQUEST);
        requestCount = bd.setScale(0, BigDecimal.ROUND_UP).intValue();

        int added_entries;

        try {
            for (int request_num = 1; request_num <= requestCount; request_num++) {
                added_entries = 0;
                if (this.isAbort()) {
                    throw new DecrypterException("Decryption aborted!");
                }

                logger.info("Decrypting request " + request_num + " of " + requestCount);

                if (is_groups_enabled) {
                    /* Disable stupid groups setting to crawl faster and to make it work anyways */
                    logger.info("Disabling groups setting");
                    postPageSafe(acc, this.br, "/STV/M/obj/user/submit/submitVideoArchiveOptions.cfm", "ShowGroupedVideoArchive=false");
                    is_groups_enabled = false;
                }
                this.postPageSafe(acc, this.br, "/STV/M/obj/archive/JSON/VideoArchiveApi.cfm", "iEntriesPerPage=" + SITE_ENTRIES_PER_REQUEST + "&iCurrentPage=" + request_num + "&dStartdate=0");

                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                final ArrayList<Object> resource_data_list = (ArrayList) entries.get("ARRVIDEOARCHIVEENTRIES");

                for (final Object singleid_information : resource_data_list) {
                    addID_site(acc, singleid_information);
                    added_entries++;
                    foundLinksNum++;
                }

                logger.info("Found " + added_entries + " entries in request " + request_num + " of " + requestCount);
                if (added_entries == 0) {
                    logger.info("Can't find any entries, stopping at request: " + request_num + " of " + requestCount);
                    break;
                }
            }
        } finally {
            try {
                if (groups_enabled_by_user && !is_groups_enabled) {
                    /* Restore users' groups-setting after decryption if changed */
                    logger.info("Re-enabling groups setting");
                    postPageSafe(acc, this.br, "https://www." + this.getHost() + "/STV/M/obj/user/submit/submitVideoArchiveOptions.cfm", "ShowGroupedVideoArchive=true");
                    logger.info("Successfully re-enabled groups setting");
                }
            } catch (final Throwable settingfail) {
                logger.info("Failed to restore previous groups setting");
            }
        }
    }

    private void addID_api(final Account acc, final String id_source) throws ParseException, DecrypterException, PluginException {
        final String telecast_id = new Regex(id_source, "<a:Id>(\\d+)</a:Id>").getMatch(0);
        if (dupecheckList.contains(telecast_id)) {
            throw new DecrypterException("Decryption aborted because of dupecheck-failure!");
        }

        final DownloadLink dl = createStvDownloadlink(acc, telecast_id);
        jd.plugins.hoster.SaveTv.parseFilenameInformation_api(dl, id_source);
        jd.plugins.hoster.SaveTv.parseQualityTag(dl, null);
        final long calculated_filesize = jd.plugins.hoster.SaveTv.calculateFilesize(dl, getLongProperty(dl, "site_runtime_minutes", 0));

        if (id_IS_Allowed(dl)) {
            dl.setDownloadSize(calculated_filesize);
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
        jd.plugins.hoster.SaveTv.parseFilenameInformation_site(dl, entries);
        jd.plugins.hoster.SaveTv.parseQualityTag(dl, (ArrayList) entries.get("ARRALLOWDDOWNLOADFORMATS"));
        final long calculated_filesize = jd.plugins.hoster.SaveTv.calculateFilesize(dl, getLongProperty(dl, "site_runtime_minutes", 0));
        if (id_IS_Allowed(dl)) {
            dl.setDownloadSize(calculated_filesize);
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
        if (fast_linkcheck) {
            dl.setAvailable(true);
        }
        dl.setContentUrl(telecast_url);
        dl.setLinkID(account_username + telecastID);
        /* Property is needed to later determine which url is downloadable via which account. */
        dl.setProperty(jd.plugins.hoster.SaveTv.PROPERTY_downloadable_via, account_username);
        return dl;
    }

    private boolean id_IS_Allowed(final DownloadLink dl) {
        final long datemilliseconds = getLongProperty(dl, "originaldate", 0);
        final long current_tdifference = time_crawl_started - datemilliseconds;
        final String telecastID = dl.getStringProperty("LINKDUPEID", null);
        boolean isAllowed = false;
        if (only_grab_new_entries && !crawledTelecastIDsMap.containsKey(telecastID)) {
            /* User only wants telecastIDs which he did not add before and this ID has not been added before --> Allow to add it! */
            crawledTelecastIDsMap.put(telecastID, datemilliseconds);
            isAllowed = true;
        } else if (!only_grab_new_entries && (tdifference_milliseconds == 0 || current_tdifference <= tdifference_milliseconds)) {
            /* User only wants to add telecastIDs of a user-defined time-range. */
            isAllowed = true;
        }
        return isAllowed;
    }

    @SuppressWarnings("unused")
    private String correctData(final String input) {
        return jd.plugins.hoster.SaveTv.correctData(input);
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
                jd.plugins.hoster.SaveTv.login_site(this.br, aa, force);
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

    @SuppressWarnings({ "deprecation" })
    private void postPageSafe(final Account acc, final Browser br, final String url, final String postData) throws Exception {
        // Limits made by me (pspzockerscene):
        // Max 6 logins possible
        // Max 15 accesses of the link possible
        // -> Max 21 total requests

        // Limits made by me (pspzockerscene):
        // Max 6 logins possible
        // Max 15 postPage possible
        // -> Max 21 total requests
        int failcounter_url = 0;
        for (int i = 0; i <= 2; i++) {
            boolean failed = true;
            do {
                try {
                    br.postPage(url, postData);
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

    // /**
    // * @param soapAction
    // * : The soap link which should be accessed
    // * @param soapPost
    // * : The soap post data
    // */
    // private void api_doSoapRequest(final String soapAction, final String soapPost) throws IOException {
    // final String method = new Regex(soapAction, "([A-Za-z0-9]+)$").getMatch(0);
    // br.getHeaders().put("SOAPAction", soapAction);
    // br.getHeaders().put("Content-Type", "text/xml");
    // final String postdata =
    // "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><"
    // + method + " xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\">" + soapPost + "</" + method + "></v:Body></v:Envelope>";
    // br.postPageRaw("http://api.save.tv/v2/Api.svc", postdata);
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
        final String message = "Es wurden Archive von insgesamt " + totalAccountsNum + " save.tv Account(s) durchsucht.";
        return message;
    }

    private String getDialogEnd() {
        final long crawl_duration = System.currentTimeMillis() - time_crawl_started;
        String message = "\r\n";
        message += "Dauer des Crawlvorganges: " + TimeFormatter.formatMilliSeconds(crawl_duration, 0);
        message += "\r\n\r\nGenervt von diesen Info-Dialogen? In den Plugin Einstellungen kannst du sie deaktivieren ;)";
        return message;
    }

    private static long getLongProperty(final Property link, final String key, final long def) {
        try {
            return link.getLongProperty(key, def);
        } catch (final Throwable e) {
            try {
                Object r = link.getProperty(key, def);
                if (r instanceof String) {
                    r = Long.parseLong((String) r);
                } else if (r instanceof Integer) {
                    r = ((Integer) r).longValue();
                }
                final Long ret = (Long) r;
                return ret;
            } catch (final Throwable e2) {
                return def;
            }
        }
    }

}
