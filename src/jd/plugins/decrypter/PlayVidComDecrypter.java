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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "playvid.com" }, urls = { "https?://(www\\.)?playvid.com/(?:watch(?:\\?v=|/)|embed/|v/)[A-Za-z0-9\\-_]+|https?://(?:www\\.)?playvids\\.com/(?:[a-z]{2}/)?v/[A-Za-z0-9\\-_]+" })
public class PlayVidComDecrypter extends PluginForDecrypt {

    public PlayVidComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
    private String                        filename       = null;
    private String                        parameter      = null;

    /** Settings stuff */
    private static final String           FASTLINKCHECK  = "FASTLINKCHECK";
    private static final String           ALLOW_BEST     = "ALLOW_BEST";
    private static final String           ALLOW_360P     = "ALLOW_360P";
    private static final String           ALLOW_480P     = "ALLOW_480P";
    private static final String           ALLOW_720P     = "ALLOW_720";

    @SuppressWarnings({ "static-access", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.toString().contains("playvid.com/")) {
            parameter = new Regex(param.toString(), "https?://").getMatch(-1) + "www.playvid.com/watch/" + new Regex(param.toString(), "([A-Za-z0-9\\-_]+)$").getMatch(0);
        } else {
            parameter = param.toString();
        }
        br.setFollowRedirects(true);
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("playvid.com");
        }
        ((jd.plugins.hoster.PlayVidCom) plugin).prepBrowser(br);
        // Log in if possible to get 720p quality
        getUserLogin(false);
        br.getPage(parameter);
        if (jd.plugins.hoster.PlayVidCom.isOffline(this.br)) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Decrypt start */
        filename = br.getRegex("data\\-title=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("data\\-callback=\"pv_hideshowTitle\">([^<>\"]*?)<").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("itemprop=\"name\" content=\"([^<>\"]+)\"").getMatch(0);
                if (filename == null) {
                    filename = new Regex(parameter, "/([^/]+)$").getMatch(0);
                    if (filename == null) {
                        logger.warning("Playvid.com decrypter failed..." + parameter);
                        return null;
                    }
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(filename);

        /** Decrypt qualities START */

        foundQualities = ((jd.plugins.hoster.PlayVidCom) plugin).getQualities(this.br);
        if (foundQualities == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /** Decrypt qualities END */
        /** Decrypt qualities, selected by the user */
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        final SubConfiguration cfg = SubConfiguration.getConfig("playvid.com");
        if (cfg.getBooleanProperty(ALLOW_BEST, false)) {
            ArrayList<String> list = new ArrayList<String>(foundQualities.keySet());
            final String highestAvailableQualityValue = list.get(0);
            selectedQualities.add(highestAvailableQualityValue);
        } else {
            /** User selected nothing -> Decrypt everything */
            boolean q360p = cfg.getBooleanProperty(ALLOW_360P, false);
            boolean q480p = cfg.getBooleanProperty(ALLOW_480P, false);
            boolean q720 = cfg.getBooleanProperty(ALLOW_720P, false);
            if (q360p == false && q480p == false && q720 == false) {
                q360p = true;
                q480p = true;
                q720 = true;
            }
            if (q360p) {
                selectedQualities.add("360p");
            }
            if (q480p) {
                selectedQualities.add("480p");
            }
            if (q720) {
                selectedQualities.add("720p");
            }
        }
        for (final String selectedQualityValue : selectedQualities) {
            final DownloadLink dl = getVideoDownloadlink(selectedQualityValue);
            if (dl != null) {
                fp.add(dl);
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info("None of the selected qualities were found, decrypting done...");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    @SuppressWarnings("deprecation")
    private DownloadLink getVideoDownloadlink(final String qualityValue) {
        String directlink = foundQualities.get(qualityValue);
        if (directlink != null) {
            directlink = Encoding.htmlDecode(directlink);
            final String fname = filename + "_" + qualityValue + ".mp4";
            final DownloadLink dl = createDownloadlink("http://playviddecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            dl.setProperty("directlink", directlink);
            dl.setProperty("qualityvalue", qualityValue);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("directname", fname);
            dl.setLinkID(fname);
            dl.setFinalFileName(fname);
            dl.setContentUrl(parameter);
            if (SubConfiguration.getConfig("playvid.com").getBooleanProperty(FASTLINKCHECK, false)) {
                dl.setAvailable(true);
            }
            return dl;
        } else {
            return null;
        }
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private PluginForHost plugin = null;

    private boolean getUserLogin(final boolean force) throws Exception {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("playvid.com");
        }
        final Account aa = AccountController.getInstance().getValidAccount(plugin);
        if (aa == null) {
            logger.warning("There is no account available...");
            return false;
        }
        try {
            ((jd.plugins.hoster.PlayVidCom) plugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}