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

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.TimeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "save.tv" }, urls = { "https?://(www\\.)?save\\.tv/STV/M/obj/archive/VideoArchive\\.cfm" }, flags = { 0 })
public class SaveTvDecrypter extends PluginForDecrypt {

    public SaveTvDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Settings stuff */
    @SuppressWarnings("deprecation")
    private final SubConfiguration       cfg                                = SubConfiguration.getConfig("save.tv");
    private static final String          ACTIVATE_BETA_FEATURES             = "ACTIVATE_BETA_FEATURES";
    private final String                 USEAPI                             = "USEAPI";
    private final String                 CRAWLER_ONLY_ADD_NEW_IDS           = "CRAWLER_ONLY_ADD_NEW_IDS";
    private final String                 CRAWLER_ENABLE_FASTER              = "CRAWLER_ENABLE_FASTER_2";
    private final String                 CRAWLER_ACTIVATE                   = "CRAWLER_ACTIVATE";
    private final String                 CRAWLER_DISABLE_DIALOGS            = "CRAWLER_DISABLE_DIALOGS";
    private final String                 CRAWLER_LASTHOURS_COUNT            = "CRAWLER_LASTHOURS_COUNT";

    private static final String          CRAWLER_PROPERTY_TELECASTIDS_ADDED = "CRAWLER_PROPERTY_TELECASTIDS_ADDED";
    private static final String          CRAWLER_PROPERTY_LASTCRAWL         = "CRAWLER_PROPERTY_LASTCRAWL";

    /* Decrypter constants */
    private static final int             ENTRIES_PER_REQUEST                = 1000;
    private static final long            TELECAST_ID_EXPIRE_TIME            = 32 * 24 * 60 * 60 * 1000l;

    /* Property / Filename constants */
    public static final String           QUALITY_PARAM                      = "quality";
    public static final String           QUALITY_LQ                         = "LQ";
    public static final String           QUALITY_HQ                         = "HQ";
    public static final String           QUALITY_HD                         = "HD";
    public static final String           EXTENSION                          = ".mp4";

    /* Decrypter variables */
    final ArrayList<DownloadLink>        decryptedLinks                     = new ArrayList<DownloadLink>();
    private static HashMap<String, Long> crawledTelecastIDsMap              = new HashMap<String, Long>();
    private long                         grab_last_hours_num                = 0;
    private long                         tdifference_milliseconds           = 0;

    private int                          totalLinksNum                      = 0;
    private int                          foundLinksNum                      = 0;
    private int                          requestCount                       = 1;
    private long                         time_crawl_started                 = 0;
    private long                         time_last_crawl_ended              = 0;
    private boolean                      decryptAborted                     = false;
    private Account                      acc                                = null;

    /* Settings */
    private boolean                      crawler_DialogsDisabled            = false;
    private boolean                      api_enabled                        = false;
    private boolean                      only_grab_new_entries              = false;
    /* If this != null, API can be used */
    private boolean                      fast_linkcheck                     = false;
    private String                       parameter                          = null;

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

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        time_crawl_started = System.currentTimeMillis();
        parameter = param.toString();
        api_enabled = cfg.getBooleanProperty(USEAPI, false);
        fast_linkcheck = cfg.getBooleanProperty(CRAWLER_ENABLE_FASTER, false);
        crawler_DialogsDisabled = cfg.getBooleanProperty(CRAWLER_DISABLE_DIALOGS, false);
        only_grab_new_entries = cfg.getBooleanProperty(ACTIVATE_BETA_FEATURES, false);
        grab_last_hours_num = getLongProperty(cfg, CRAWLER_LASTHOURS_COUNT, 0);
        time_last_crawl_ended = getLongProperty(cfg, CRAWLER_PROPERTY_LASTCRAWL, 0);
        this.br.setFollowRedirects(true);
        this.br.setLoadLimit(this.br.getLoadLimit() * 3);
        if (!cfg.getBooleanProperty(CRAWLER_ACTIVATE, false)) {
            logger.info("save.tv: Decrypting save.tv archives is disabled, doing nothing...");
            return decryptedLinks;
        }
        if (!getUserLogin(false)) {
            logger.info("Failed to decrypt link because no account is available: " + parameter);
            return decryptedLinks;
        }
        if (only_grab_new_entries) {
            /* Load list of saved IPs + timestamp of last download */
            final Object crawledIDSMap = cfg.getProperty(CRAWLER_PROPERTY_TELECASTIDS_ADDED);
            if (crawledIDSMap != null && crawledIDSMap instanceof HashMap) {
                crawledTelecastIDsMap = (HashMap<String, Long>) crawledIDSMap;
            }
        } else {
            tdifference_milliseconds = grab_last_hours_num * 60 * 60 * 1000;
        }

        try {
            if (api_enabled) {
                api_decrypt_All();
            } else {
                site_decrypt_All();
            }
            /*
             * Let's clean our ID map. TelecastIDs automatically get deleted after 30 days (when this documentation was written) so we do
             * not need to store them longer than that as it will eat up more RAM/space for no reason.
             */
            final Iterator entries = crawledTelecastIDsMap.entrySet().iterator();
            while (entries.hasNext()) {
                Entry thisEntry = (Entry) entries.next();
                Object value = thisEntry.getValue();
                final String telecastID = (String) thisEntry.getKey();
                final long timestamp = ((Number) value).longValue();
                if (System.currentTimeMillis() - timestamp >= TELECAST_ID_EXPIRE_TIME) {
                    /* Remove old entries */
                    crawledTelecastIDsMap.remove(telecastID);
                }
            }
            /* Save telecastID map so later we know what is new and what we crawled before ;) */
            cfg.setProperty(CRAWLER_PROPERTY_TELECASTIDS_ADDED, crawledTelecastIDsMap);
            if (decryptedLinks.size() > 0 && (foundLinksNum >= totalLinksNum)) {
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
            }
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
            return decryptedLinks;
        }
        handleEndDialogs();
        return decryptedLinks;
    }

    /** This will first grab alle IDs, then their details as the 2nd used request returns more information than the first one. */
    private void api_decrypt_All() throws Exception {
        final ArrayList<String> temp_telecastIDs = new ArrayList<String>();
        int offset = 0;
        while (true) {
            try {
                if (this.isAbort()) {
                    decryptAborted = true;
                    throw new DecrypterException("Decrypt aborted!");
                }
            } catch (final DecrypterException e) {
                // Not available in old 0.9.581 Stable
                if (decryptAborted) {
                    throw e;
                }
            }
            jd.plugins.hoster.SaveTv.api_doSoapRequestSafe(this.br, acc, "http://tempuri.org/IVideoArchive/SimpleParamsGetVideoArchiveList", "<channelId>0</channelId><filterType>0</filterType><recordingState>0</recordingState><tvCategoryId>0</tvCategoryId><tvSubCategoryId>0</tvSubCategoryId><textSearchType>0</textSearchType><from>" + offset + "</from><count>" + (offset + ENTRIES_PER_REQUEST) + "</count>");
            final String[] telecastIDs = br.getRegex("Stv\\.Api\\.Contract\\.Telecast\">(\\d+)</Id>").getColumn(0);
            for (final String telecastID : telecastIDs) {
                temp_telecastIDs.add(telecastID);
                offset++;
            }
            logger.info("Found " + temp_telecastIDs.size() + " RAW telecastIDs so far");
            if (telecastIDs.length < ENTRIES_PER_REQUEST) {
                logger.info("Seems like we found all telecastIDs --> Getting further information");
                break;
            }
        }

        /* Save on account to display in account information */
        totalLinksNum = temp_telecastIDs.size();
        acc.setProperty("acc_count_telecast_ids", Integer.toString(totalLinksNum));
        final BigDecimal bd = new BigDecimal((double) totalLinksNum / ENTRIES_PER_REQUEST);
        requestCount = bd.setScale(0, BigDecimal.ROUND_UP).intValue();

        offset = 0;
        int request_num = 1;
        while (true) {
            try {
                if (this.isAbort()) {
                    decryptAborted = true;
                    throw new DecrypterException("Decrypt aborted!");
                }
            } catch (final DecrypterException e) {
                // Not available in old 0.9.581 Stable
                if (decryptAborted) {
                    throw e;
                }
            }
            String telecastid_post_data = "";
            for (int i = 0; i <= ENTRIES_PER_REQUEST - 1; i++) {
                if (offset >= totalLinksNum) {
                    break;
                }
                telecastid_post_data += "<a:int>" + temp_telecastIDs.get(offset) + "</a:int>";
                offset++;
            }
            logger.info("save.tv: Decrypting request " + request_num + " of " + requestCount);
            jd.plugins.hoster.SaveTv.api_doSoapRequestSafe(this.br, acc, "http://tempuri.org/ITelecast/GetTelecastDetail", "<telecastIds xmlns:a=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">" + telecastid_post_data + "</telecastIds><detailLevel>2</detailLevel>");
            final String[] entries = br.getRegex("<a:TelecastDetail>(.*?)</a:TelecastDetail>").getColumn(0);
            if (entries == null || entries.length == 0) {
                logger.info("save.tv. Can't find entries, stopping at request: " + request_num + " of " + requestCount);
                break;
            }
            for (final String entry : entries) {
                addID_api(entry);
                foundLinksNum++;
            }
            if (offset >= totalLinksNum) {
                logger.info("Seems like decryption is complete --> Stopping!");
                break;
            }
            request_num++;
        }
    }

    private void site_decrypt_All() throws Exception {
        boolean is_groups_enabled = false;
        boolean groups_enabled_by_user = false;
        getPageSafe("https://www.save.tv/STV/M/obj/archive/JSON/VideoArchiveApi.cfm?iEntriesPerPage=1&iCurrentPage=1");
        is_groups_enabled = !br.containsHTML("\"IGROUPCOUNT\":1\\.0");
        groups_enabled_by_user = is_groups_enabled;
        final String totalLinks = getJson(br.toString(), "ITOTALENTRIES");
        if (totalLinks == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return;
        }
        /* Save on account to display in account information */
        acc.setProperty("acc_count_telecast_ids", totalLinks);
        totalLinksNum = Integer.parseInt(totalLinks);
        final BigDecimal bd = new BigDecimal((double) totalLinksNum / ENTRIES_PER_REQUEST);
        requestCount = bd.setScale(0, BigDecimal.ROUND_UP).intValue();

        int added_entries = 0;

        try {
            for (int i = 1; i <= requestCount; i++) {
                try {
                    if (this.isAbort()) {
                        decryptAborted = true;
                        throw new DecrypterException("Decrypt aborted!");
                    }
                } catch (final DecrypterException e) {
                    // Not available in old 0.9.581 Stable
                    if (decryptAborted) {
                        throw e;
                    }
                }

                logger.info("save.tv: Decrypting request " + i + " of " + requestCount);

                if (is_groups_enabled) {
                    /* Disable stupid groups setting to crawl faster and to make it work anyways */
                    logger.info("Disabling groups setting");
                    postPageSafe(this.br, "https://www.save.tv/STV/M/obj/user/submit/submitVideoArchiveOptions.cfm", "ShowGroupedVideoArchive=false");
                    is_groups_enabled = false;
                }
                getPageSafe("https://www.save.tv/STV/M/obj/archive/JSON/VideoArchiveApi.cfm?iEntriesPerPage=" + ENTRIES_PER_REQUEST + "&iCurrentPage=" + i);
                final String array_text = br.getRegex("\"ARRVIDEOARCHIVEENTRIES\":\\[(\\{.*?\\})\\],\"ENABLEDEFAULTFORMATSETTINGS\"").getMatch(0);
                final String[] telecast_array = array_text.split("TelecastId=\\d+\"\\}\\},\\{");

                for (final String singleid_information : telecast_array) {
                    addID_site(singleid_information);
                    added_entries++;
                    foundLinksNum++;
                }

                if (added_entries == 0) {
                    logger.info("save.tv. Can't find entries, stopping at request: " + i + " of " + requestCount);
                    break;
                }
                logger.info("Found " + added_entries + " entries in request " + i + " of " + requestCount);
                continue;
            }
        } finally {
            try {
                if (groups_enabled_by_user && !is_groups_enabled) {
                    /* Restore users' groups-setting after decryption if changed */
                    logger.info("Re-enabling groups setting");
                    postPageSafe(this.br, "https://www.save.tv/STV/M/obj/user/submit/submitVideoArchiveOptions.cfm", "ShowGroupedVideoArchive=true");
                    logger.info("Successfully re-enabled groups setting");
                }
            } catch (final Throwable settingfail) {
                logger.info("Failed to re-enable groups setting");
            }
        }
    }

    private void addID_api(final String id_source) throws ParseException, DecrypterException, PluginException {
        final String telecast_id = new Regex(id_source, "<a:Id>(\\d+)</a:Id>").getMatch(0);
        final DownloadLink dl = createStvDownloadlink(telecast_id);
        jd.plugins.hoster.SaveTv.parseFilenameInformation_api(dl, id_source);
        jd.plugins.hoster.SaveTv.parseQualityTag(dl, null);
        final long calculated_filesize = jd.plugins.hoster.SaveTv.calculateFilesize(getLongProperty(dl, "site_runtime_minutes", 0));

        if (id_IS_Allowed(dl)) {
            dl.setDownloadSize(calculated_filesize);
            dl.setName(jd.plugins.hoster.SaveTv.getFilename(dl));
            try {
                distribute(dl);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            decryptedLinks.add(dl);
        }
    }

    private void addID_site(final String id_source) throws ParseException, DecrypterException, PluginException {
        final String telecast_id = getJson(id_source, "ITELECASTID");
        final DownloadLink dl = createStvDownloadlink(telecast_id);
        jd.plugins.hoster.SaveTv.parseFilenameInformation_site(dl, id_source);
        jd.plugins.hoster.SaveTv.parseQualityTag(dl, id_source);
        final long calculated_filesize = jd.plugins.hoster.SaveTv.calculateFilesize(getLongProperty(dl, "site_runtime_minutes", 0));
        if (id_IS_Allowed(dl)) {
            dl.setDownloadSize(calculated_filesize);
            dl.setName(jd.plugins.hoster.SaveTv.getFilename(dl));

            try {
                distribute(dl);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            decryptedLinks.add(dl);
        }
    }

    private DownloadLink createStvDownloadlink(final String telecastID) {
        final String telecast_url = "https://www.save.tv/STV/M/obj/archive/VideoArchiveDetails.cfm?TelecastId=" + telecastID;
        final DownloadLink dl = createDownloadlink(telecast_url);
        dl.setName(telecastID + ".mp4");
        if (fast_linkcheck) {
            dl.setAvailable(true);
        }
        try {
            /* JD2 only */
            dl.setContentUrl(telecast_url);
            dl.setLinkID(telecastID);
        } catch (Throwable e) {
            /* Not available in old 0.9.581 Stable */
            dl.setProperty("LINKDUPEID", telecastID);
        }
        return dl;
    }

    private boolean id_IS_Allowed(final DownloadLink dl) {
        final boolean onlyAddNewIDs = cfg.getBooleanProperty(ACTIVATE_BETA_FEATURES, false);
        final long datemilliseconds = getLongProperty(dl, "originaldate", 0);
        final long current_tdifference = System.currentTimeMillis() - datemilliseconds;
        final String telecastID = dl.getStringProperty("LINKDUPEID", null);
        if ((!onlyAddNewIDs && tdifference_milliseconds == 0 || current_tdifference <= tdifference_milliseconds) || (onlyAddNewIDs && !crawledTelecastIDsMap.containsKey(telecastID))) {
            /* TODO: Fix that! */
            crawledTelecastIDsMap.put(telecastID, datemilliseconds);
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private String correctData(final String input) {
        return jd.plugins.hoster.SaveTv.correctData(input);
    }

    @SuppressWarnings("deprecation")
    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("save.tv");
        acc = AccountController.getInstance().getValidAccount(hostPlugin);
        if (acc == null) {
            return false;
        }
        try {
            if (api_enabled) {
                jd.plugins.hoster.SaveTv.api_login(this.br, acc, force);
            } else {
                jd.plugins.hoster.SaveTv.site_login(this.br, acc, force);
            }
        } catch (final PluginException e) {
            acc.setValid(false);
            return false;
        }
        return true;
    }

    /* Sync this with the decrypter */
    private void getPageSafe(final String url) throws Exception {
        // Limits made by me (pspzockerscene):
        // Max 6 logins possible
        // Max 3 accesses of the link possible
        // -> Max 9 total requests
        for (int i = 0; i <= 2; i++) {
            jd.plugins.hoster.SaveTv.getPageCorrectBr(this.br, url);
            if (br.getURL().contains("Token=MSG_LOGOUT_B")) {
                for (int i2 = 0; i2 <= 1; i2++) {
                    logger.info("Link redirected to login page, logging in again to retry this: " + url);
                    logger.info("Try " + i2 + " of 1");
                    try {
                        getUserLogin(true);
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

    /* Avoid 503 server errors */
    @SuppressWarnings("unused")
    private void postPageSafe(final Browser br, final String url, final String postData) throws IOException, PluginException, InterruptedException {
        for (int i = 1; i <= 3; i++) {
            try {
                br.postPage(url, postData);
            } catch (final BrowserException e) {
                if (br.getRequest().getHttpConnection().getResponseCode() == 503) {
                    final DownloadLink dummyLink = createDownloadlink("https://www.save.tv/STV/M/obj/archive/VideoArchive.cfm");
                    logger.info("503 BrowserException occured, retry " + i + " of 3");
                    Thread.sleep(3000);
                    continue;
                }
                logger.info("Unhandled BrowserException occured...");
                throw e;
            }
            break;
        }
    }

    /**
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private static String getJson(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(-?\\d+(\\.\\d+)?|true|false|null)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + key + "\":\"([^\"]*?)\"").getMatch(0);
        }
        if (result == null || result.equals("")) {
            /* Workaround - sometimes they use " plain in json even though usually this has to be encoded! */
            result = new Regex(source, "\"" + key + "\":\"([^<>]*?)\",\"").getMatch(0);
        }
        if (result != null) {
            result = result.replaceAll("\\\\/", "/");
        }
        return result;
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
                                message += "Vermutlich gab es bisher keine neuen Aufnahmen!";
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
                                message += "Vermutlich gab es in diesem Zeitraum keine Aufnahmen!";
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
                                String message = "Save.tv Archiv-Crawler - alle Aufnahmen der letzten " + grab_last_hours_num + " Stunden wurden ergolgreich gefunden!\r\n";
                                message += "Es wurden " + decryptedLinks.size() + " Links gefunden!";
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
                                String message = "Save.tv - alle Links des Archives wurden erfolgreich gefunden!\r\n";
                                message += "Es wurden " + decryptedLinks.size() + " von " + totalLinksNum + " Links gefunden!";
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
                                message += "Es wurden nur " + decryptedLinks.size() + " von " + totalLinksNum + " Links (telecastIDs) gefunden!";
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

    private String getDialogEnd() {
        final long crawl_duration = System.currentTimeMillis() - time_crawl_started;
        String message = "\r\n";
        message += "Dauer des Crawlvorganges: " + TimeFormatter.formatMilliSeconds(crawl_duration, 0);
        message += "\r\n\r\nGenervt von diesen Info-Dialogen? In den Plugin Einstellung kannst du sie deaktivieren ;)";
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
