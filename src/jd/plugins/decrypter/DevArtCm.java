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

import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/((gallery|favourites)/\\d+(\\?offset=\\d+)?|(gallery|favourites)/(\\?offset=\\d+|\\?catpath(=(/|%2F|[a-z0-9]+)(\\&offset=\\d+)?)?)?)" }, flags = { 0 })
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

    private static Object       LOCK            = new Object();

    private static final String FASTLINKCHECK_2 = "FASTLINKCHECK_2";
    private static final String TYPE_CATPATH    = "https?://[\\w\\.\\-]*?deviantart\\.com/(gallery|favourites)/\\?catpath(=(/|%2F|[a-z0-9]+)(\\&offset=\\d+)?)?";
    private static final String TYPE_CATPATH_2  = "https?://[\\w\\.\\-]*?deviantart\\.com/(gallery|favourites)/\\?catpath=[a-z0-9]+(\\&offset=\\d+)?";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        synchronized (LOCK) {
            // checkFeatureDialog();
            checkFeatureDialog();
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        if (br.containsHTML("The page you were looking for doesn\\'t exist\\.")) {
            logger.warning("Invalid URL: " + parameter);
            return decryptedLinks;
        }

        // only non /art/ requires packagename
        if (parameter.contains("/gallery/") || parameter.contains("/favourites/")) {
            // find and set username
            final String username = br.getRegex("name=\"username\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (username == null) {
                logger.warning("Plugin broken for link: " + parameter);
                return null;
            }
            // find and set page type
            String pagetype = "";
            String catpath_addition = null;
            if (parameter.matches(TYPE_CATPATH_2)) {
                catpath_addition = new Regex(parameter, "deviantart\\.com/gallery/\\?catpath=([a-z0-9]+)").getMatch(0);
            }
            if (parameter.contains("/favourites/"))
                pagetype = "Favourites";
            else if (parameter.contains("/gallery/"))
                pagetype = "Gallery";
            else
                pagetype = "Unknown";
            // find and set pagename
            String pagename = br.getRegex("class=\"folder\\-title\">([^<>\"]*?)</span>").getMatch(0);
            if (pagename != null) pagename = Encoding.htmlDecode(pagename.trim());
            // set packagename
            String fpName = "";
            if (pagename != null && catpath_addition != null) {
                fpName = username + " - " + pagetype + " - " + catpath_addition + " - " + pagename;
            } else if (pagename != null) {
                fpName = username + " - " + pagetype + " - " + pagename;
            } else {
                fpName = username + " - " + pagetype;
            }

            int currentOffset = 0;
            int maxOffset = 0;
            final int offsetIncrease = 24;
            int counter = 1;
            if (parameter.contains("offset=")) {
                final int offsetLink = Integer.parseInt(new Regex(parameter, "(\\d+)$").getMatch(0));
                currentOffset = offsetLink;
                maxOffset = offsetLink;
            } else if (!parameter.matches(TYPE_CATPATH)) {
                final String[] offsets = br.getRegex("data\\-offset=\"(\\d+)\" name=\"gmi\\-GPageButton\"").getColumn(0);
                if (offsets != null && offsets.length != 0) {
                    for (final String offset : offsets) {
                        final int offs = Integer.parseInt(offset);
                        if (offs > maxOffset) maxOffset = offs;
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
                        logger.info("Decryption aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                logger.info("Decrypting offset " + currentOffset + " of " + maxOffset);
                if (parameter.matches(TYPE_CATPATH) && !parameter.contains("offset=")) {
                    if (counter > 1) br.getPage(parameter + "&offset=" + currentOffset);
                    // catpath links have an unknown end-offset
                    final String nextOffset = br.getRegex("\\?catpath=[A-Za-z0-9%]+\\&amp;offset=(\\d+)\"><span>Next</span></a>").getMatch(0);
                    if (nextOffset != null) maxOffset = Integer.parseInt(nextOffset);
                } else if (counter > 1) {
                    br.getPage(parameter + "?offset=" + currentOffset);
                }
                final boolean fastcheck = SubConfiguration.getConfig("deviantart.com").getBooleanProperty(FASTLINKCHECK_2, false);
                final String grab = br.getRegex("<smoothie q=(.*?)(class=\"folderview-bottom\"></div>|div id=\"gallery_pager\")").getMatch(0);
                String[] artlinks = new Regex(grab, "\"(https?://[\\w\\.\\-]*?deviantart\\.com/art/[\\w\\-]+)\"").getColumn(0);
                if (artlinks == null || artlinks.length == 0) {
                    logger.warning("Possible Plugin error, with finding /art/ links: " + parameter);
                    return null;
                }
                if (artlinks != null && artlinks.length != 0) {
                    for (final String al : artlinks) {
                        final DownloadLink fina = createDownloadlink(al);
                        if (fastcheck) fina.setAvailable(true);
                        if (fp != null) fina._setFilePackage(fp);
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
        return decryptedLinks;
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