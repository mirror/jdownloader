//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/(?!art/)[^<>\"]+" }, flags = { 0 })
public class DevArtCm extends PluginForDecrypt {

    /**
     * @author raztoki
     */
    public DevArtCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This plugin grabs range of content depending on parameter.
    // profile.devart.com/gallery/uid*
    // profile.devart.com/favorites/uid*
    // profile.devart.com/gallery/*
    // profile.devart.com/favorites/*
    // * = ?offset=\\d+
    //
    // All of the above formats should support spanning pages, but when
    // parameter contains '?offset=x' it will not span.
    //
    // profilename.deviantart.com/art/uid/ == grabs the 'download image' (best
    // quality available).
    //
    // I've created the plugin this way to allow users to grab as little or as
    // much, content as they wish. Hopefully this wont create any
    // issues.

    private static Object         LOCK             = new Object();

    private static final String   FASTLINKCHECK_2  = "FASTLINKCHECK_2";
    private static final String   TYPE_COLLECTIONS = "https?://[\\w\\.\\-]*?deviantart\\.com/[A-Za-z0-9\\-]+/collections/\\d+";
    private static final String   TYPE_CATPATH_ALL = "https?://[\\w\\.\\-]*?deviantart\\.com/(gallery|favourites)/\\?catpath(=.+)?";
    private static final String   TYPE_CATPATH_1   = "https?://[\\w\\.\\-]*?deviantart\\.com/(gallery|favourites)/\\?catpath(=(/|%2F([a-z0-9]+)?|[a-z0-9]+)(\\&offset=\\d+)?)?";
    private static final String   TYPE_CATPATH_2   = "https?://[\\w\\.\\-]*?deviantart\\.com/(gallery|favourites)/\\?catpath=[a-z0-9]{1,}(\\&offset=\\d+)?";
    private static final String   TYPE_JOURNAL     = "https?://[\\w\\.\\-]*?deviantart\\.com/journal.+";
    private static final String   LINKTYPE_JOURNAL = "https?://[\\w\\.\\-]*?deviantart\\.com/journal/[\\w\\-]+";

    final ArrayList<DownloadLink> decryptedLinks   = new ArrayList<DownloadLink>();

    private String                PARAMETER        = null;
    private boolean               FASTLINKCHECK    = false;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        FASTLINKCHECK = SubConfiguration.getConfig("deviantart.com").getBooleanProperty(FASTLINKCHECK_2, false);
        synchronized (LOCK) {
            // checkFeatureDialog();
            checkFeatureDialog();
        }
        PARAMETER = param.toString();
        if (PARAMETER.matches(LINKTYPE_JOURNAL)) {
            final DownloadLink journal = createDownloadlink(PARAMETER.replace("deviantart.com/", "deviantartdecrypted.com/"));
            journal.setName("deviantart\\.com/journal/([\\w\\-]+)");
            if (FASTLINKCHECK) {
                journal.setAvailable(true);
            }
            decryptedLinks.add(journal);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.getPage(PARAMETER);
        if (br.containsHTML("The page you were looking for doesn\\'t exist\\.")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + PARAMETER);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }

        if (PARAMETER.matches(TYPE_JOURNAL)) {
            decryptJournals();
        } else if (PARAMETER.matches(TYPE_COLLECTIONS)) {
            decryptCollections();
        } else if (PARAMETER.contains("/gallery/") || PARAMETER.contains("/favourites/")) {
            decryptStandard();
        } else {
            logger.info("Link unsupported: " + PARAMETER);
            return decryptedLinks;
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Link probably offline: " + PARAMETER);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private void decryptJournals() throws DecrypterException, IOException {
        final String username = getUsername();
        String paramdecrypt;
        if (PARAMETER.contains("catpath=/")) {
            paramdecrypt = PARAMETER.replace("catpath=/", "catpath=%2F") + "&offset=";
        } else {
            paramdecrypt = PARAMETER + "?offset=";
        }
        if (username == null) {
            logger.warning("Plugin broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username + " - Journal");
        fp.setProperty("ALLOW_MERGE", true);
        String next = null;
        int previousOffset = 0;
        int currentOffset = 0;
        final boolean stop_after_first_run = new Regex(PARAMETER, "offset=(\\d+)").getMatch(0) != null;
        do {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + PARAMETER);
                    return;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            logger.info("Decrypting offset " + next);
            if (next != null) {
                currentOffset = Integer.parseInt(next);
                /* Fail safe */
                if (currentOffset <= previousOffset) {
                    logger.info("Seems like we're done!");
                    break;
                }
                br.getPage(paramdecrypt + next);
            }
            final String jinfo[] = br.getRegex("data\\-deviationid=\"\\d+\" href=\"(http://[\\w\\.\\-]*?\\.deviantart\\.com/journal/[\\w\\-]+)\"").getColumn(0);
            if (jinfo == null || jinfo.length == 0) {
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            for (final String link : jinfo) {
                final String urltitle = new Regex(link, "deviantart\\.com/journal/([\\w\\-]+)").getMatch(0);
                final DownloadLink dl = createDownloadlink(link.replace("deviantart.com/", "deviantartdecrypted.com/"));
                if (FASTLINKCHECK) {
                    dl.setAvailable(true);
                }
                /* No reason to hide their single links */
                dl.setBrowserUrl(link);
                dl.setName(urltitle + ".html");
                dl._setFilePackage(fp);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                decryptedLinks.add(dl);
            }
            next = br.getRegex("class=\"next\"><a class=\"away\" data\\-offset=\"(\\d+)\"").getMatch(0);
            previousOffset = currentOffset;
            if (stop_after_first_run) {
                logger.info("Decrypted given offset, stopping...");
                break;
            }
        } while (next != null);
        fp.addLinks(decryptedLinks);
    }

    private void decryptCollections() throws DecrypterException {
        final String[] links = br.getRegex("<a href=\"(http://[^<>\"/]+\\.deviantart\\.com/(art|journal)/[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Plugin broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        for (final String aLink : links) {
            decryptedLinks.add(createDownloadlink(aLink));
        }
    }

    private void decryptStandard() throws DecrypterException, IOException {
        /* Correct input links */
        if (PARAMETER.matches("http://[^<>\"/]+\\.deviantart\\.com/gallery/\\?\\d+")) {
            final Regex paramregex = new Regex(PARAMETER, "(http://[^<>\"/]+\\.deviantart\\.com/gallery/\\?)(\\d+)");
            PARAMETER = paramregex.getMatch(0) + "set=" + paramregex.getMatch(1);
        }
        /* only non /art/ requires packagename */
        // find and set username
        final String username = getUsername();
        if (username == null) {
            logger.warning("Plugin broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        // find and set page type
        String pagetype = "";
        if (PARAMETER.matches(TYPE_CATPATH_2)) {
            pagetype = new Regex(PARAMETER, "deviantart\\.com/gallery/\\?catpath=([a-z0-9]+)").getMatch(0);
            /* First letter = capital letter */
            pagetype = pagetype.substring(0, 1).toUpperCase() + pagetype.substring(1, pagetype.length());
        } else if (PARAMETER.contains("/favourites/")) {
            pagetype = "Favourites";
        } else if (PARAMETER.contains("/gallery/")) {
            pagetype = "Gallery";
        } else {
            pagetype = "Unknown";
        }
        // find and set pagename
        String pagename = br.getRegex("class=\"folder\\-title\">([^<>\"]*?)</span>").getMatch(0);
        if (pagename != null) {
            pagename = Encoding.htmlDecode(pagename.trim());
        }
        // set packagename
        String fpName = "";
        if (pagename != null) {
            fpName = username + " - " + pagetype + " - " + pagename;
        } else {
            fpName = username + " - " + pagetype;
        }

        int currentOffset = 0;
        int maxOffset = 0;
        final int offsetIncrease = 24;
        int counter = 1;
        if (PARAMETER.contains("offset=")) {
            final int offsetLink = Integer.parseInt(new Regex(PARAMETER, "(\\d+)$").getMatch(0));
            currentOffset = offsetLink;
            maxOffset = offsetLink;
        } else if (!PARAMETER.matches(TYPE_CATPATH_1)) {
            final String[] offsets = br.getRegex("data\\-offset=\"(\\d+)\" name=\"gmi\\-GPageButton\"").getColumn(0);
            if (offsets != null && offsets.length != 0) {
                for (final String offset : offsets) {
                    final int offs = Integer.parseInt(offset);
                    if (offs > maxOffset) {
                        maxOffset = offs;
                    }
                }
            }
        }
        FilePackage fp = null;
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.setProperty("ALLOW_MERGE", true);
        }
        do {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + PARAMETER);
                    return;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            logger.info("Decrypting offset " + currentOffset + " of " + maxOffset);
            if (PARAMETER.matches(TYPE_CATPATH_1) && !PARAMETER.contains("offset=")) {
                if (counter > 1) {
                    br.getPage(PARAMETER + "&offset=" + currentOffset);
                }
                // catpath links have an unknown end-offset
                final String nextOffset = br.getRegex("\\?catpath=[A-Za-z0-9%]+\\&amp;offset=(\\d+)\"><span>Next</span></a>").getMatch(0);
                if (nextOffset != null) {
                    maxOffset = Integer.parseInt(nextOffset);
                }
            } else if (counter > 1) {
                if (PARAMETER.contains("?")) {
                    br.getPage(PARAMETER + "&offset=" + currentOffset);
                } else {
                    br.getPage(PARAMETER + "?offset=" + currentOffset);
                }
            }
            final boolean fastcheck = SubConfiguration.getConfig("deviantart.com").getBooleanProperty(FASTLINKCHECK_2, false);
            final String grab = br.getRegex("<smoothie q=(.*?)(class=\"folderview-bottom\"></div>|div id=\"gallery_pager\")").getMatch(0);
            String[] artlinks = new Regex(grab, "\"(https?://[\\w\\.\\-]*?deviantart\\.com/art/[\\w\\-]+)\"").getColumn(0);
            if ((artlinks == null || artlinks.length == 0) && counter == 1) {
                logger.warning("Possible Plugin error, with finding /art/ links: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            } else if (artlinks == null || artlinks.length == 0) {
                /* We went too far - we should already have links */
                logger.info("Current offset contains no links --> Stopping");
                break;
            }
            if (artlinks != null && artlinks.length != 0) {
                for (final String artlink : artlinks) {
                    final DownloadLink fina = createDownloadlink(artlink);
                    if (fastcheck) {
                        fina.setAvailable(true);
                    }
                    /* No reason to hide their single links */
                    fina.setBrowserUrl(artlink);
                    if (fp != null) {
                        fp.add(fina);
                    }
                    try {
                        distribute(fina);
                    } catch (final Throwable e) {
                        // Not available in old 0.9.581 Stable
                    }
                    decryptedLinks.add(fina);
                }
            }

            currentOffset += offsetIncrease;
            counter++;
        } while (currentOffset <= maxOffset);
        if (fpName != null) {
            fp.addLinks(decryptedLinks);
        }
    }

    private String getUsername() {
        return br.getRegex("name=\"username\" value=\"([^<>\"]*?)\"").getMatch(0);
    }

    private void checkFeatureDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("featuredialog_Shown", Boolean.FALSE) == false) {
                if (config.getProperty("featuredialog_Shown2") == null) {
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
                config.setProperty("featuredialog_Shown", Boolean.TRUE);
                config.setProperty("featuredialog_Shown2", "shown");
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
                        title = "Deviantart.com Plugin";
                        final String lang = System.getProperty("user.language");
                        if ("de".equalsIgnoreCase(lang)) {
                            message += "Für deviantArt.com wurde die schnelle Linküberprüfung aktiviert.\r\n";
                            message += "Diese bewirkt, dass weder Dateiname noch -größe korrekt angezeigt\r\nund die Bilder somit erheblich schneller gesammelt werden.\r\n";
                            message += "Du kannst dieses Verhalten jederzeit unter\r\n";
                            message += "JD2 Beta: Einstellungen ->Plugins ->deviantArt.com\r\n";
                            message += "JD 0.9.581: Einstellungen ->Anbieter ->deviantart.com ->Einstellungen\r\n";
                            message += "deaktivieren.";
                        } else {
                            message += "For deviantArt.com the fast link check has been activated.\r\n";
                            message += "This causes the loss of the correct filename and filesize but will ensure a much faster grabbing of pictures.\r\n";
                            message += "You can deactivate this setting under\r\n";
                            message += "JD2 Beta: Settings ->Plugins ->deviantArt.com\r\n";
                            message += "JD 0.9.581: Settings ->Hoster ->deviantArt.com ->Settings\r\n";
                            message += "at any time.";
                        }
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}