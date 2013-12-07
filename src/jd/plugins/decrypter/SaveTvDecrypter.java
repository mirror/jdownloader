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
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "save.tv" }, urls = { "https?://(www\\.)?save\\.tv/STV/M/obj/user/usShowVideoArchive\\.cfm(\\?iPageNumber=\\d+)?" }, flags = { 0 })
public class SaveTvDecrypter extends PluginForDecrypt {

    public SaveTvDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CRAWLER_ACTIVATE        = "CRAWLER_ACTIVATE";
    private static final String CRAWLER_ENABLE_FASTER   = "CRAWLER_ENABLE_FASTER";
    private static final String USEAPI                  = "USEAPI";
    private static final String CRAWLER_DISABLE_DIALOGS = "CRAWLER_DISABLE_DIALOGS";

    private final String        CONTAINSPAGE            = "https?://(www\\.)?save\\.tv/STV/M/obj/user/usShowVideoArchive\\.cfm\\?iPageNumber=\\d+";

    // TODO: Find a better solution than "param3=string:984899" -> Maybe try to use API if it has a function to get the whole archive
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final SubConfiguration cfg = SubConfiguration.getConfig("save.tv");
        if (!cfg.getBooleanProperty(CRAWLER_ACTIVATE, false)) {
            logger.info("dave.tv: Decrypting save.tv archives is disabled, doing nothing...");
            return decryptedLinks;
        } else if (cfg.getBooleanProperty(USEAPI, false)) {
            logger.info("save.tv: Cannot decrypt the archive while the API is enabled.");
            return decryptedLinks;
        }
        if (!getUserLogin(false)) {
            logger.info("Failed to decrypt link because account is missing: " + parameter);
            return decryptedLinks;
        }
        final boolean fastLinkcheck = cfg.getBooleanProperty(CRAWLER_ENABLE_FASTER, false);
        final boolean crawlerInfiDialogsDisabled = cfg.getBooleanProperty(CRAWLER_DISABLE_DIALOGS, false);
        getPageSafe(parameter);
        int maxPage = 1;
        if (!parameter.matches(CONTAINSPAGE)) {
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
        final int totalLinksNum = Integer.parseInt(totalLinks);
        final DecimalFormat df = new DecimalFormat("0000");
        final DecimalFormat df2 = new DecimalFormat("0000000000000");
        final String one = df.format(new Random().nextInt(10000));
        final String two = df2.format(new Random().nextInt(1000000000));
        final ArrayList<String> addedIDs_backup = new ArrayList<String>();
        final ArrayList<String> addedIDs = new ArrayList<String>();
        int addedlinksnum = 0;
        boolean decryptAborted = false;
        try {
            for (int i = 1; i <= maxPage; i++) {
                try {
                    if (this.isAbort()) {
                        decryptAborted = true;
                        throw new DecrypterException("Decrypt aborted!");
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                    if (decryptAborted) throw new DecrypterException("Decrypt aborted!");
                }
                logger.info("Decrypting page " + i + " of " + maxPage);
                if (i > 1) {
                    br.getPage("https://www.save.tv/STV/M/obj/user/usShowVideoArchive.cfm?iPageNumber=" + i + "&bLoadLast=1");
                }
                // debug code start
                final String[] ids_backup_array = br.getRegex("name=\"lTelecastID\" value=\"(\\d+)\"").getColumn(0);
                if (ids_backup_array != null && ids_backup_array.length != 0) {
                    for (final String bckupid : ids_backup_array) {
                        if (!addedIDs_backup.contains(bckupid)) addedIDs_backup.add(bckupid);
                    }
                }
                // debug code end
                // Find series links
                final String[][] directSeriesLinks = br.getRegex("(\\d+)\" class=\"child\">([^<>\"]*?)</a>[\t\n\r ]+\\-(.*?)(\r|\t|\n]+)").getMatches();
                final String[][] directSeriesLinks2 = br.getRegex("TelecastID=(\\d+)\" class=\"normal\">([^<>\"]*?)</a>").getMatches();
                if (directSeriesLinks != null && directSeriesLinks.length != 0) {
                    for (final String[] directserieslinkinfo : directSeriesLinks) {
                        final String telecastID = directserieslinkinfo[0];
                        if (!addedIDs.contains(telecastID)) {
                            final String seriesName = Encoding.htmlDecode(directserieslinkinfo[1].trim());
                            final String episodeTitle = Encoding.htmlDecode(directserieslinkinfo[2].trim());
                            final DownloadLink dl = createDownloadlink("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveDetail.cfm?TelecastID=" + telecastID);
                            final FilePackage fp = FilePackage.getInstance();
                            fp.setName(Encoding.htmlDecode(seriesName));
                            fp.addLinks(decryptedLinks);
                            dl.setFinalFileName(seriesName + " - " + episodeTitle + " " + telecastID + ".mp4");
                            dl._setFilePackage(fp);
                            if (fastLinkcheck) dl.setAvailable(true);
                            try {
                                distribute(dl);
                            } catch (final Throwable e) {
                                // Not available in old 0.9.581 Stable
                            }
                            decryptedLinks.add(dl);
                            addedIDs.add(telecastID);
                            addedlinksnum++;
                        }
                    }
                }
                if (directSeriesLinks2 != null && directSeriesLinks2.length != 0) {
                    for (final String[] directserieslinkinfo : directSeriesLinks2) {
                        final String telecastID = directserieslinkinfo[0];
                        if (!addedIDs.contains(telecastID)) {
                            final String seriesName = Encoding.htmlDecode(directserieslinkinfo[1].trim());
                            final DownloadLink dl = createDownloadlink("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveDetail.cfm?TelecastID=" + telecastID);
                            final FilePackage fp = FilePackage.getInstance();
                            fp.setName(Encoding.htmlDecode(seriesName));
                            fp.addLinks(decryptedLinks);
                            dl.setFinalFileName(seriesName + " " + telecastID + ".mp4");
                            dl._setFilePackage(fp);
                            if (fastLinkcheck) dl.setAvailable(true);
                            try {
                                distribute(dl);
                            } catch (final Throwable e) {
                                // Not available in old 0.9.581 Stable
                            }
                            decryptedLinks.add(dl);
                            addedIDs.add(telecastID);
                            addedlinksnum++;
                        }
                    }
                }

                // Find movie links
                final String[][] directMovieLinks = br.getRegex("(\\d+)\" class=\"normal\">([^<>\"]*?)</a>[\t\n\r ]+\\-(.*?)(\r|\t|\n]+)").getMatches();
                final String[][] directMovieLinks2 = br.getRegex("(\\d+)\" class=\"normal\">([^<>\"]*?)</a>").getMatches();
                if (directMovieLinks != null && directMovieLinks.length != 0) {
                    for (final String[] directmovieslinkinfo : directMovieLinks) {
                        final String telecastID = directmovieslinkinfo[0];
                        if (!addedIDs.contains(telecastID)) {
                            final String movieName = Encoding.htmlDecode(directmovieslinkinfo[1].trim());
                            final String episodeTitle = Encoding.htmlDecode(directmovieslinkinfo[2].trim());
                            final DownloadLink dl = createDownloadlink("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveDetail.cfm?TelecastID=" + telecastID);
                            final FilePackage fp = FilePackage.getInstance();
                            fp.setName(Encoding.htmlDecode(movieName));
                            fp.addLinks(decryptedLinks);
                            dl.setFinalFileName(movieName + " - " + episodeTitle + " " + telecastID + ".mp4");
                            dl._setFilePackage(fp);
                            if (fastLinkcheck) dl.setAvailable(true);
                            try {
                                distribute(dl);
                            } catch (final Throwable e) {
                                // Not available in old 0.9.581 Stable
                            }
                            decryptedLinks.add(dl);
                            addedIDs.add(telecastID);
                            addedlinksnum++;
                        }
                    }
                }
                if (directMovieLinks2 != null && directMovieLinks2.length != 0) {
                    for (final String[] directmovieslinkinfo : directMovieLinks2) {
                        final String telecastID = directmovieslinkinfo[0];
                        if (!addedIDs.contains(telecastID)) {
                            final String movieName = Encoding.htmlDecode(directmovieslinkinfo[1].trim());
                            final DownloadLink dl = createDownloadlink("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveDetail.cfm?TelecastID=" + telecastID);
                            final FilePackage fp = FilePackage.getInstance();
                            fp.setName(Encoding.htmlDecode(movieName));
                            fp.addLinks(decryptedLinks);
                            dl.setFinalFileName(movieName + " " + telecastID + ".mp4");
                            dl._setFilePackage(fp);
                            if (fastLinkcheck) dl.setAvailable(true);
                            try {
                                distribute(dl);
                            } catch (final Throwable e) {
                                // Not available in old 0.9.581 Stable
                            }
                            decryptedLinks.add(dl);
                            addedIDs.add(telecastID);
                            addedlinksnum++;
                        }
                    }
                }
                // Find all the stuff which has to be parsed via ajax
                final String[][] dlInfo = br.getRegex("data\\-rownumber=\"(\\d+)\", data\\-title=\"([^<>\"]*?)\"").getMatches();
                int seriesLinksCounter = 0;
                if (dlInfo != null && dlInfo.length != 0) {
                    logger.info("save.tv: Page: " + i + ": Found " + dlInfo.length + " series to expand and decrypt!");
                    int seriesCounter = 1;
                    for (final String[] dInfo : dlInfo) {
                        logger.info("save.tv: Page: " + i + ": Decrypting series " + seriesCounter + " of " + dlInfo.length);
                        try {
                            if (this.isAbort()) {
                                logger.info("Decrypt process aborted by user: " + parameter);
                                decryptAborted = true;
                                throw new DecrypterException("Decrypt aborted!");
                            }
                        } catch (final Throwable e) {
                            // Not available in old 0.9.581 Stable
                            if (decryptAborted) throw new DecrypterException("Decrypt aborted!");
                        }
                        final String dlid = dInfo[0];
                        final String dlname = Encoding.htmlDecode(dInfo[1]);
                        final FilePackage fp = FilePackage.getInstance();
                        fp.setName(dlname);
                        fp.addLinks(decryptedLinks);
                        try {
                            br.postPage("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveLoadEntries.cfm?null.GetVideoEntries", "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetVideoEntries&c0-id=" + one + "_" + two + "&c0-param0=string:1&c0-param1=string:&c0-param2=string:1&c0-param3=string:984899&c0-param4=string:1&c0-param5=string:0&c0-param6=string:1&c0-param7=string:0&c0-param8=string:1&c0-param9=string:&c0-param10=string:" + Encoding.urlEncode(dlname) + "&c0-param11=string:" + dlid + "&c0-param12=string:toggleSerial&xml=true&extend=function (object) for (property in object) { this[property] = object[property]; } return this;}&");
                        } catch (final BrowserException e) {
                            logger.warning("Plugin broken for link: " + parameter);
                            logger.warning("Stopped at page " + i + " of " + maxPage);
                            return null;
                        }
                        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                        // debug code start
                        final String[] ids_backup_array2 = br.getRegex("name=\"lTelecastID\" value=\"(\\d+)\"").getColumn(0);
                        if (ids_backup_array2 != null && ids_backup_array2.length != 0) {
                            for (final String bckupid : ids_backup_array2) {
                                if (!addedIDs_backup.contains(bckupid)) addedIDs_backup.add(bckupid);
                            }
                        }
                        // debug code end
                        final String[][] epinfos = br.getRegex("TelecastID=(\\d+)\" class=\"normal\">([^<>\"]*?)</a> \\- ([^<>\"]*?)</td>").getMatches();
                        final String[][] epinfos2 = br.getRegex("TelecastID=(\\d+)\" class=\"normal\">([^<>\"]*?)</a>").getMatches();
                        if (((epinfos == null || epinfos.length == 0) && (epinfos2 == null || epinfos2.length == 0)) && addedlinksnum == 0) {
                            logger.info("Can't find more links, stopping at page: " + i + " of " + maxPage);
                            return decryptedLinks;
                        } else if (((epinfos == null || epinfos.length == 0) && (epinfos2 == null || epinfos2.length == 0)) && decryptedLinks.size() == 0) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            logger.warning("Stopped at page " + i + " of " + maxPage);
                            return null;
                        }
                        if (epinfos != null && epinfos.length != 0) {
                            for (final String[] episodeinfo : epinfos) {
                                final String telecastID = episodeinfo[0];
                                if (!addedIDs.contains(telecastID)) {
                                    final String seriesName = Encoding.htmlDecode(episodeinfo[1].trim());
                                    final DownloadLink dl = createDownloadlink("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveDetail.cfm?TelecastID=" + telecastID);
                                    final String episodeTitle = Encoding.htmlDecode(episodeinfo[2].trim());
                                    dl.setFinalFileName(seriesName + " - " + episodeTitle + " " + telecastID + ".mp4");
                                    dl._setFilePackage(fp);
                                    if (fastLinkcheck) dl.setAvailable(true);
                                    try {
                                        distribute(dl);
                                    } catch (final Throwable e) {
                                        // Not available in old 0.9.581 Stable
                                    }
                                    decryptedLinks.add(dl);
                                    addedIDs.add(telecastID);
                                    addedlinksnum++;
                                    seriesLinksCounter++;
                                }
                            }
                        }

                        if (epinfos2 != null && epinfos2.length != 0) {
                            for (final String[] episodeinfo : epinfos2) {
                                final String telecastID = episodeinfo[0];
                                if (!addedIDs.contains(telecastID)) {
                                    final String seriesName = Encoding.htmlDecode(episodeinfo[1].trim());
                                    final DownloadLink dl = createDownloadlink("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveDetail.cfm?TelecastID=" + telecastID);
                                    dl.setFinalFileName(seriesName + " " + telecastID + ".mp4");
                                    dl._setFilePackage(fp);
                                    if (fastLinkcheck) dl.setAvailable(true);
                                    try {
                                        distribute(dl);
                                    } catch (final Throwable e) {
                                        // Not available in old 0.9.581 Stable
                                    }
                                    decryptedLinks.add(dl);
                                    addedIDs.add(telecastID);
                                    addedlinksnum++;
                                    seriesLinksCounter++;
                                }
                            }
                        }
                        seriesCounter++;
                        logger.info("save.tv: Page: " + i + ": Found " + seriesLinksCounter + " telecastIDs of series: " + dlname);
                    }
                }

                if (addedlinksnum == 0 && decryptedLinks.size() == 0) {
                    logger.warning("save.tv: Plugin broken for link: " + parameter);
                    return null;
                } else if (addedlinksnum == 0) {
                    logger.info("save.tv. Can't find more links, stopping at page: " + i + " of " + maxPage);
                    break;
                }
                logger.info("Found " + addedlinksnum + " links on page " + i + " of " + maxPage);
            }
        } catch (final DecrypterException edec) {
            logger.info("Decrypt process aborted by user: " + parameter);
            if (!crawlerInfiDialogsDisabled) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String title = "Save.tv Archiv-Crawler - Crawler abgebrochen";
                                String message = "Save.tv - Der Crawler wurde frühzeitig vom Benutzer beendet!\r\n";
                                message += "Es wurden bisher " + decryptedLinks.size() + " von " + totalLinksNum + " Links (telecastIDs) gefunden!";
                                message += "\r\n\r\n";
                                message += "Genervt von diesen Info-Dialogen? In den Plugin Einstellung kannst du sie deaktivieren ;)";
                                JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
                            } catch (Throwable e) {
                            }
                        }
                    });
                } catch (Throwable e2) {
                }
            }
            return decryptedLinks;
        }
        if (decryptedLinks.size() >= totalLinksNum && !crawlerInfiDialogsDisabled) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            String title = "Save.tv Archiv-Crawler - Archiv komplett gefunden";
                            String message = "Save.tv - alle Links des Archives wurden erfolgreich gefunden!\r\n";
                            message += "Es wurden " + decryptedLinks.size() + " von " + totalLinksNum + " Links gefunden!";
                            message += "\r\n\r\n";
                            message += "Genervt von diesen Info-Dialogen? In den Plugin Einstellung kannst du sie deaktivieren ;)";
                            JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                        } catch (Throwable e) {
                        }
                    }
                });
            } catch (Throwable e) {
            }
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String title = "Save.tv Archiv-Crawler - Archiv nicht komplett gefunden";
                            String message = "Save.tv - leider wurden nicht alle Links des Archives gefunden!\r\n";
                            message += "Es wurden nur " + decryptedLinks.size() + " von " + totalLinksNum + " Links (telecastIDs) gefunden!";
                            message += "\r\n\r\n";
                            message += "Genervt von diesen Info-Dialogen? In den Plugin Einstellung kannst du sie deaktivieren ;)";
                            JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
                        } catch (Throwable e) {
                        }
                    }
                });
            } catch (Throwable e) {
            }
        }
        logger.info("save.tv: total links found: " + decryptedLinks.size() + " of " + totalLinks);

        return decryptedLinks;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("save.tv");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) { return false; }
        try {
            ((jd.plugins.hoster.SaveTv) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setEnabled(false);
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

}
