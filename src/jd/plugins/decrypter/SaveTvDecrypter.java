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

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
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
    private final String           USEAPI                            = "USEAPI";
    private final String           USEORIGINALFILENAME               = "USEORIGINALFILENAME";

    private final String           CRAWLER_ACTIVATE                  = "CRAWLER_ACTIVATE";
    private final String           CRAWLER_ENABLE_FASTER             = "CRAWLER_ENABLE_FASTER";
    private final String           CRAWLER_DISABLE_DIALOGS           = "CRAWLER_DISABLE_DIALOGS";
    private final String           CRAWLER_LASTDAYS_COUNT            = "CRAWLER_LASTDAYS_COUNT";

    private static final double    QUALITY_HD_MB_PER_MINUTE          = jd.plugins.hoster.SaveTv.QUALITY_HD_MB_PER_MINUTE;
    private static final double    QUALITY_H264_NORMAL_MB_PER_MINUTE = jd.plugins.hoster.SaveTv.QUALITY_H264_NORMAL_MB_PER_MINUTE;
    private static final double    QUALITY_H264_MOBILE_MB_PER_MINUTE = jd.plugins.hoster.SaveTv.QUALITY_H264_MOBILE_MB_PER_MINUTE;

    private final SubConfiguration cfg                               = SubConfiguration.getConfig("save.tv");
    private final boolean          FAST_LINKCHECK                    = cfg.getBooleanProperty(CRAWLER_ENABLE_FASTER, false);

    private boolean                crawler_DialogsDisabled           = false;

    /* Decrypter constants */
    private static final int       ENTRIES_PER_REQUEST               = 1000;

    final ArrayList<DownloadLink>  decryptedLinks                    = new ArrayList<DownloadLink>();
    private long                   grab_last_days_num                = 0;
    private long                   tdifference_milliseconds          = 0;

    private int                    totalLinksNum                     = 0;
    private int                    requestCount                      = 1;
    private long                   time_crawl_started                = 0;

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    // TODO: Find a better solution than "param3=string:984899" -> Maybe try to use API if it has a function to get the whole archive
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.toString();
        if (!cfg.getBooleanProperty(CRAWLER_ACTIVATE, false)) {
            logger.info("dave.tv: Decrypting save.tv archives is disabled, doing nothing...");
            return decryptedLinks;
        } else if (cfg.getBooleanProperty(USEAPI, false)) {
            logger.info("save.tv: Cannot decrypt the archive while the API is enabled.");
            return decryptedLinks;
        }
        time_crawl_started = System.currentTimeMillis();
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("save.tv");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (!getUserLogin(false)) {
            logger.info("Failed to decrypt link because account is missing: " + parameter);
            return decryptedLinks;
        }
        crawler_DialogsDisabled = cfg.getBooleanProperty(CRAWLER_DISABLE_DIALOGS, false);

        grab_last_days_num = getLongProperty(cfg, CRAWLER_LASTDAYS_COUNT, 0);
        tdifference_milliseconds = grab_last_days_num * 24 * 60 * 60 * 1000;

        try {
            getPageSafe("https://www.save.tv/STV/M/obj/archive/JSON/VideoArchiveApi.cfm?iEntriesPerPage=1&iCurrentPage=1");
            final String totalLinks = getJson(br.toString(), "ITOTALENTRIES");
            if (totalLinks == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return decryptedLinks;
            }
            /* Save on account to display in account information */
            aa.setProperty("acc_count_telecast_ids", totalLinks);
            totalLinksNum = Integer.parseInt(totalLinks);
            final BigDecimal bd = new BigDecimal((double) totalLinksNum / ENTRIES_PER_REQUEST);
            requestCount = bd.setScale(0, BigDecimal.ROUND_UP).intValue();

            int added_entries = 0;
            boolean decryptAborted = false;

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

                    getPageSafe("https://www.save.tv/STV/M/obj/archive/JSON/VideoArchiveApi.cfm?iEntriesPerPage=" + ENTRIES_PER_REQUEST + "&iCurrentPage=" + i);
                    final String array_text = br.getRegex("\"ARRVIDEOARCHIVEENTRIES\":\\[(.*?)\\]").getMatch(0);
                    final String[] telecast_array = array_text.split("\\},\\{");

                    for (final String singleid_information : telecast_array) {
                        addID(singleid_information);
                        added_entries++;
                    }

                    if (added_entries == 0) {
                        logger.info("save.tv. Can't find entries, stopping at request: " + i + " of " + requestCount);
                        break;
                    }
                    logger.info("Found " + added_entries + " entries in request " + i + " of " + requestCount);
                    continue;
                }

            } catch (final DecrypterException edec) {
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
                return decryptedLinks;
            }
            logger.info("save.tv: total links found: " + decryptedLinks.size() + " of " + totalLinksNum);
            handleEndDialogs();
        } catch (final Throwable e) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String title = "Save.tv Archiv-Crawler - Archiv nicht komplett gefunden (Server Fehler)";
                            String message = "Save.tv - leider wurden nicht alle Links des Archives gefunden!\r\n";
                            message += "Während dem Crawlen ist es zu einem Server Fehler gekommen!\r\n";
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

        return decryptedLinks;
    }

    private void addID(final String id_info) throws ParseException, DecrypterException {
        final String telecast_id = getJson(id_info, "ITELECASTID");
        final String telecast_url = "https://www.save.tv/STV/M/obj/archive/VideoArchiveDetails.cfm?TelecastId=" + telecast_id;
        final DownloadLink dl = createDownloadlink(telecast_url);
        jd.plugins.hoster.SaveTv.siteParseFilenameInformation(dl, id_info);
        final long calculated_filesize = jd.plugins.hoster.SaveTv.calculateFilesize(getLongProperty(dl, "site_runtime_minutes", 0));

        final long datemilliseconds = getLongProperty(dl, "originaldate", 0);
        final long current_tdifference = System.currentTimeMillis() - datemilliseconds;
        if (tdifference_milliseconds == 0 || current_tdifference <= tdifference_milliseconds) {

            String filename;
            /* Nothing to hide - Always show original links in JD */
            dl.setBrowserUrl(telecast_url);
            dl.setDownloadSize(calculated_filesize);
            if (FAST_LINKCHECK) {
                dl.setAvailable(true);
            }
            /* Get and set filename */
            if (cfg.getBooleanProperty(USEORIGINALFILENAME)) {
                filename = jd.plugins.hoster.SaveTv.getFakeOriginalFilename(dl);
            } else {
                filename = jd.plugins.hoster.SaveTv.getFormattedFilename(dl);
            }
            dl.setName(filename);

            try {
                distribute(dl);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            decryptedLinks.add(dl);
        }
    }

    @SuppressWarnings("unused")
    private String correctData(final String input) {
        return jd.plugins.hoster.SaveTv.correctData(input);
    }

    @SuppressWarnings("deprecation")
    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("save.tv");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            return false;
        }
        try {
            ((jd.plugins.hoster.SaveTv) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {

            aa.setValid(false);
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

    private void handleEndDialogs() {
        if (!crawler_DialogsDisabled) {
            if (grab_last_days_num > 0 && decryptedLinks.size() == 0) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - nichts gefunden";
                                String message = "Save.tv - leider wurden keine Links gefunden!\r\n";
                                message += "Bedenke, dass du nur alle Aufnahmen der letzten " + grab_last_days_num + " Tage wolltest.\r\n";
                                message += "Vermutlich gab es in diesem Zeitraum keine Aufnahmen!";
                                message += getDialogEnd();
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
                            } catch (final Throwable e) {
                            }
                        }
                    });
                } catch (final Throwable e) {
                }
            } else if (grab_last_days_num > 0) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - alle Aufnahmen der letzten " + grab_last_days_num + " Tage wurden gefunden";
                                String message = "Save.tv Archiv-Crawler - alle Aufnahmen der letzten " + grab_last_days_num + " Tage wurden ergolgreich gefunden!\r\n";
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
