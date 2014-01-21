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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "playvid.com" }, urls = { "http://(www\\.)?playvid.com/watch\\?v=[A-Za-z0-9\\-]+" }, flags = { 0 })
public class PlayVidComDecrypter extends PluginForDecrypt {

    public PlayVidComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private LinkedHashMap<String, String> FOUNDQUALITIES = new LinkedHashMap<String, String>();
    private String                        FILENAME       = null;
    private String                        PARAMETER      = null;

    /** Settings stuff */
    private static final String           FASTLINKCHECK  = "FASTLINKCHECK";
    private static final String           ALLOW_BEST     = "ALLOW_BEST";
    private static final String           ALLOW_360P     = "ALLOW_360P";
    private static final String           ALLOW_480P     = "ALLOW_480P";
    private static final String           ALLOW_720P     = "ALLOW_720";

    @SuppressWarnings("static-access")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        PARAMETER = param.toString();
        br.setFollowRedirects(true);
        // Log in if possible to get 720p quality
        getUserLogin(false);
        br.getPage(PARAMETER);
        if (br.containsHTML("Video not found<|class=\"play\\-error\"")) {
            final DownloadLink dl = createDownloadlink("http://playviddecrypted.com/" + System.currentTimeMillis());
            dl.setFinalFileName(new Regex(PARAMETER, "\\?v=([A-Za-z0-9\\-]+)").getMatch(0));
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        /** Decrypt start */
        FILENAME = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (FILENAME == null) {
            FILENAME = br.getRegex("<title>([^<>\"]*?)\\- PlayVid</title>").getMatch(0);
        }
        if (FILENAME == null) {
            logger.warning("Playvid.com decrypter failed..." + PARAMETER);
            return null;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(FILENAME);

        /** Decrypt qualities START */
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("playvid.com");
        FOUNDQUALITIES = ((jd.plugins.hoster.PlayVidCom) hostPlugin).getQualities(this.br);
        if (FOUNDQUALITIES == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            return null;
        }
        /** Decrypt qualities END */
        /** Decrypt qualities, selected by the user */
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        final SubConfiguration cfg = SubConfiguration.getConfig("playvid.com");
        if (cfg.getBooleanProperty(ALLOW_BEST, false)) {
            ArrayList<String> list = new ArrayList<String>(FOUNDQUALITIES.keySet());
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
            if (q360p) selectedQualities.add("360p");
            if (q480p) selectedQualities.add("480p");
            if (q720) selectedQualities.add("720p");
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

    private DownloadLink getVideoDownloadlink(final String qualityValue) {
        String directlink = FOUNDQUALITIES.get(qualityValue);
        if (directlink != null) {
            directlink = Encoding.htmlDecode(directlink);
            final String fname = FILENAME + "_" + qualityValue + ".mp4";
            final DownloadLink dl = createDownloadlink("http://playviddecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            dl.setProperty("directlink", directlink);
            dl.setProperty("qualityvalue", qualityValue);
            dl.setProperty("mainlink", PARAMETER);
            dl.setProperty("directname", fname);
            dl.setFinalFileName(fname);
            if (SubConfiguration.getConfig("playvid.com").getBooleanProperty(FASTLINKCHECK, false)) dl.setAvailable(true);
            return dl;
        } else {
            return null;
        }
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("playvid.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available...");
            return false;
        }
        try {
            ((jd.plugins.hoster.PlayVidCom) hostPlugin).login(this.br, aa, force);
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