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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
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
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.TimeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "save.tv" }, urls = { "https?://(www\\.)?save\\.tv/STV/M/obj/user/usShowVideoArchive\\.cfm(\\?iPageNumber=\\d+)?" }, flags = { 0 })
public class SaveTvDecrypter extends PluginForDecrypt {

    public SaveTvDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Settings stuff */
    private static final String           USEAPI                            = "USEAPI";
    private static final String           PREFERH264MOBILE                  = "PREFERH264MOBILE";

    private static final String           CRAWLER_ACTIVATE                  = "CRAWLER_ACTIVATE";
    private static final String           CRAWLER_ENABLE_FASTER             = "CRAWLER_ENABLE_FASTER";
    private static final String           CRAWLER_DISABLE_DIALOGS           = "CRAWLER_DISABLE_DIALOGS";
    private static final String           CRAWLER_LASTDAYS_COUNT            = "CRAWLER_LASTDAYS_COUNT";

    private static final double           QUALITY_H264_NORMAL_MB_PER_MINUTE = 12.605;
    private static final double           QUALITY_H264_MOBILE_MB_PER_MINUTE = 4.64;

    private static final SubConfiguration cfg                               = SubConfiguration.getConfig("save.tv");
    private static final boolean          FAST_LINKCHECK                    = cfg.getBooleanProperty(CRAWLER_ENABLE_FASTER, false);

    private final String                  CONTAINSPAGE                      = "https?://(www\\.)?save\\.tv/STV/M/obj/user/usShowVideoArchive\\.cfm\\?iPageNumber=\\d+";

    private boolean                       crawler_DialogsDisabled           = false;

    final ArrayList<DownloadLink>         decryptedLinks                    = new ArrayList<DownloadLink>();
    private long                          grab_last_days_num                = 0;
    private long                          tdifference_milliseconds          = 0;

    private int                           totalLinksNum                     = 0;
    private int                           maxPage                           = 1;
    private long                          time_crawl_started                = 0;
    private boolean                       grab_specified_page_only          = false;

    // TODO: Find a better solution than "param3=string:984899" -> Maybe try to use API if it has a function to get the whole archive
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
            getPageSafe(parameter);
            if (parameter.matches(CONTAINSPAGE)) {
                grab_specified_page_only = true;
                maxPage = Integer.parseInt(new Regex(parameter, "iPageNumber=(\\d+)").getMatch(0));
            } else {
                final String[] pages = br.getRegex("PageNumber=(\\d+)\\&bLoadLast=1\"").getColumn(0);
                if (pages != null && pages.length != 0) {
                    for (final String page : pages) {
                        final int currentpage = Integer.parseInt(page);
                        if (currentpage > maxPage) maxPage = currentpage;
                    }
                }
            }
            final String totalLinks = br.getRegex(">Gefundene Sendungen: (\\d+)").getMatch(0);
            if (totalLinks == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return decryptedLinks;
            }
            /* Save on account to display in account information */
            aa.setProperty("acc_count_telecast_ids", totalLinks);
            totalLinksNum = Integer.parseInt(totalLinks);
            final DecimalFormat df = new DecimalFormat("0000");
            final DecimalFormat df2 = new DecimalFormat("0000000000000");
            final String random_one = df.format(new Random().nextInt(10000));
            final String random_two = df2.format(new Random().nextInt(1000000000));

            final ArrayList<String> ajaxLoad = new ArrayList<String>();

            int added_entries = 0;
            boolean decryptAborted = false;

            try {
                for (int i = 1; i <= maxPage; i++) {
                    try {
                        if (this.isAbort()) {
                            decryptAborted = true;
                            throw new DecrypterException("Decrypt aborted!");
                        }
                    } catch (final DecrypterException e) {
                        // Not available in old 0.9.581 Stable
                        if (decryptAborted) throw e;
                    }

                    if (grab_specified_page_only) {
                        logger.info("save.tv: Decrypting specified page " + maxPage);
                    } else {
                        logger.info("save.tv: Decrypting page " + i + " of " + maxPage);
                    }

                    if (i > 1) {
                        br.getPage("https://www.save.tv/STV/M/obj/user/usShowVideoArchive.cfm?iPageNumber=" + i + "&bLoadLast=1");
                    }

                    /* Find and save all links which we have to load later */
                    final String[] ajxload = br.getRegex("(<tr id=\"archive\\-list\\-row\\-toogle\\-\\d+\".*?</tr>)").getColumn(0);
                    if (ajxload != null && ajxload.length != 0) {
                        for (final String singleaxaxload : ajxload) {
                            ajaxLoad.add(singleaxaxload);
                            added_entries++;
                        }
                    }

                    final String[] directIDs = get_telecast_ids();
                    if (directIDs != null && directIDs.length != 0) {
                        for (final String singleid : directIDs) {
                            addID(singleid);
                            added_entries++;
                        }
                    }

                    if (added_entries == 0) {
                        logger.info("save.tv. Can't find entries, stopping at page: " + i + " of " + maxPage);
                        break;
                    }
                    if (grab_specified_page_only) {
                        logger.info("Found " + added_entries + " entries on desired page " + i);
                        break;
                    } else {
                        logger.info("Found " + added_entries + " entries on page " + i + " of " + maxPage);
                        continue;
                    }
                }

                /* Do all ajax requests */
                if (ajaxLoad.size() > 0) {
                    final int total_ajax_requests = ajaxLoad.size();
                    int counter_ajax = 1;
                    for (final String ajax_info : ajaxLoad) {
                        try {
                            try {
                                if (this.isAbort()) {
                                    decryptAborted = true;
                                    throw new DecrypterException("Decrypt aborted!");
                                }
                            } catch (final DecrypterException e) {
                                // Not available in old 0.9.581 Stable
                                if (decryptAborted) throw e;
                            }
                            logger.info("Making ajax request " + counter_ajax + " of maximum " + total_ajax_requests);
                            if (ajax_info.contains("data-load=\"2\"")) {
                                logger.info("ajax request " + counter_ajax + " is not needed, continuing...");
                                continue;
                            }
                            final Regex info = new Regex(ajax_info, "data\\-rownumber=\"(\\d+)\", data\\-title=\"([^<>\"]*?)\"");
                            final String dlid = info.getMatch(0);
                            final String dlname = Encoding.htmlDecode(info.getMatch(1));
                            br.postPage("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveLoadEntries.cfm?null.GetVideoEntries", "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetVideoEntries&c0-id=" + random_one + "_" + random_two + "&c0-param0=string:1&c0-param1=string:&c0-param2=string:1&c0-param3=string:984899&c0-param4=string:1&c0-param5=string:0&c0-param6=string:1&c0-param7=string:0&c0-param8=string:1&c0-param9=string:&c0-param10=string:" + Encoding.urlEncode(dlname) + "&c0-param11=string:" + dlid + "&c0-param12=string:toggleSerial&xml=true&extend=function (object) for (property in object) { this[property] = object[property]; } return this;}&");
                            br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                            final String[] directIDs = get_telecast_ids();
                            if (directIDs == null || directIDs.length == 0) {
                                logger.warning("Decrypter broken for link: " + parameter);
                                return decryptedLinks;
                            }
                            for (final String singleid : directIDs) {
                                addID(singleid);
                            }
                            logger.info("Found " + directIDs.length + " telecast-ids in ajax request " + counter_ajax + " of maximum " + total_ajax_requests);
                        } finally {
                            counter_ajax++;
                        }
                    }
                }

            } catch (final DecrypterException edec) {
                logger.info("Decrypt process aborted by user: " + parameter);
                if (!crawler_DialogsDisabled) {
                    if (grab_specified_page_only) {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String title = "Save.tv Archiv-Crawler - Crawler abgebrochen";
                                        String message = "Save.tv - Der Crawler wurde frühzeitig vom Benutzer beendet!\r\n";
                                        message += "Es wurden bisher " + decryptedLinks.size() + " Links (telecastIDs) auf Archivseite " + maxPage + " gefunden!";
                                        message += getDialogEnd();
                                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
                                    } catch (final Throwable e) {
                                    }
                                }
                            });
                        } catch (Throwable e2) {
                        }
                    } else {
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

    private String[] get_telecast_ids() {
        return br.getRegex("(<tr name=\"archive\\-list\\-row\\-\\d+\".*?</tr>)").getColumn(0);
    }

    private void addID(final String id_info) throws ParseException {
        final String telecast_id = new Regex(id_info, "name=\"lTelecastID\" value=\"(\\d+)\"").getMatch(0);
        final String telecast_url = "https://www.save.tv/STV/M/obj/user/usShowVideoArchiveDetail.cfm?TelecastID=" + telecast_id;
        final Regex dateRegex = new Regex(id_info, "(\\d{2}\\.\\d{2}\\.\\d{2}) \\| (\\d{2}:\\d{2})[\t\n\r ]+\\((\\d+)min\\)");
        final String date = dateRegex.getMatch(0);
        final String time = dateRegex.getMatch(1);
        final int duration_minutes = Integer.parseInt(dateRegex.getMatch(2));
        double filesize;
        /* User doesn't prefer the mobile version */
        if (!mobilePreferred()) {
            filesize = QUALITY_H264_NORMAL_MB_PER_MINUTE * duration_minutes * 1024 * 1024;
            /* User prefers mobile version & it's available */
        } else if (mobilePreferred() && id_info.contains("class=\"rec4\"")) {
            filesize = QUALITY_H264_MOBILE_MB_PER_MINUTE * duration_minutes * 1024 * 1024;
        } else {
            /* User prefers mobile version but it's not available -> Don't set filesize */
            filesize = 0;
        }
        final Regex nameRegex = new Regex(id_info, "class=\"normal\">([^<>\"]*?)</a>([^<>\"]*?)</td>");
        String name = nameRegex.getMatch(0);
        if (name == null) name = new Regex(id_info, "class=\"child\">([^<>\"]*?)</a>").getMatch(0);
        name = Encoding.htmlDecode(name);
        String sur_name = nameRegex.getMatch(1);
        if (sur_name != null) sur_name = Encoding.htmlDecode(sur_name).trim();

        final long datemilliseconds = TimeFormatter.getMilliSeconds(date + ":" + time, "dd.MM.yy:HH:mm", Locale.GERMAN);
        final long current_tdifference = System.currentTimeMillis() - datemilliseconds;
        if (tdifference_milliseconds == 0 || current_tdifference <= tdifference_milliseconds) {

            final DownloadLink dl = createDownloadlink(telecast_url);
            /* Nothing to hide - Always show original links in JD */
            dl.setBrowserUrl(telecast_url);
            dl.setDownloadSize((long) filesize);
            if (FAST_LINKCHECK) dl.setAvailable(true);

            if (sur_name != null && !sur_name.equals("")) {
                /* For series */
                /* Correct bad names */
                if (sur_name.startsWith("- ")) sur_name = sur_name.substring(2, sur_name.length());
                dl.setProperty("category", 2);
                dl.setProperty("seriestitle", name);
                dl.setProperty("episodename", sur_name);

            } else {
                /* For all others */
                dl.setProperty("category", 1);
            }

            // Add remaining information
            dl.setProperty("plainfilename", name);
            dl.setProperty("type", ".mp4");
            dl.setProperty("originaldate", datemilliseconds);
            final String formatted_filename = jd.plugins.hoster.SaveTv.getFormattedFilename(dl);
            dl.setFinalFileName(formatted_filename);

            try {
                distribute(dl);
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            decryptedLinks.add(dl);
        }
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("save.tv");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) { return false; }
        try {
            ((jd.plugins.hoster.SaveTv) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {

            aa.setValid(false);
            return false;
        }
        return true;
    }

    // Sync this with the decrypter
    private void getPageSafe(final String url) throws Exception {
        // Limits made by me:
        // Max 6 logins possible
        // Max 3 accesses of the link possible
        // -> Max 9 total requests
        for (int i = 0; i <= 2; i++) {
            br.getPage(url);
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

    private boolean mobilePreferred() {
        return cfg.getBooleanProperty(PREFERH264MOBILE, false);
    }

    private void handleEndDialogs() {
        if (!crawler_DialogsDisabled) {
            if (grab_specified_page_only && grab_last_days_num > 0 && decryptedLinks.size() == 0) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - nichts gefunden";
                                String message = "Save.tv - leider wurden keine Links gefunden!\r\n";
                                message += "Bedenke, dass du nur alle Aufnahmen der Seite " + maxPage + " deines\r\n";
                                message += "und außerdem nur die der letzten " + grab_last_days_num + " Tage wolltest!\r\n";
                                message += "Vermutlich gab es auf dieser Archivseite und/oder in diesem Zeitraum keine Aufnahmen!";
                                message += getDialogEnd();
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
                            } catch (final Throwable e) {
                            }
                        }
                    });
                } catch (final Throwable e) {
                }
            } else if (grab_last_days_num > 0 && decryptedLinks.size() == 0) {
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
            } else if (grab_specified_page_only && grab_last_days_num > 0) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - alle Aufnahmen aus Seite " + maxPage + " deines Archives und der letzten " + grab_last_days_num + " Tage wurden gefunden!";
                                String message = "Save.tv Archiv-Crawler - alle Aufnahmen aus Seite " + maxPage + "deines Archives und der letzten " + grab_last_days_num + " Tage wurden gefunden!\r\n";
                                message += "Es wurden " + decryptedLinks.size() + " Links gefunden!";
                                message += getDialogEnd();
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                            } catch (Throwable e) {
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
            } else if (grab_specified_page_only) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - Aufnahmen der Archivseite komplett gefunden";
                                String message = "Save.tv - alle Links der eingefügten Archivseite Nummer " + maxPage + " wurden erfolgreich gefunden!\r\n";
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
