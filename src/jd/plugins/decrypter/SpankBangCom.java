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
import java.util.concurrent.atomic.AtomicBoolean;

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
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spankbang.com" }, urls = { "http://(www\\.)?([a-z]{2}\\.)?spankbang\\.com/[a-z0-9]+/video/" }, flags = { 0 })
public class SpankBangCom extends PluginForDecrypt {

    public SpankBangCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String           DOMAIN         = "spankbang.com";

    private LinkedHashMap<String, String> FOUNDQUALITIES = new LinkedHashMap<String, String>();
    private String                        PARAMETER      = null;

    /** Settings stuff */
    private static final String           FASTLINKCHECK  = "FASTLINKCHECK";
    private static final String           ALLOW_BEST     = "ALLOW_BEST";
    private static final String           ALLOW_240p     = "ALLOW_240p";
    private static final String           ALLOW_480p     = "ALLOW_480p";
    private static final String           ALLOW_720p     = "ALLOW_720p";

    private static Object                 ctrlLock       = new Object();
    private static AtomicBoolean          pluginLoaded   = new AtomicBoolean(false);

    @Override
    protected DownloadLink createDownloadlink(String link) {
        DownloadLink ret = super.createDownloadlink(link);
        try {
            ret.setUrlProtection(org.jdownloader.controlling.UrlProtection.PROTECTED_INTERNAL_URL);
        } catch (Throwable e) {
            // jd09
        }
        return ret;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final boolean fastcheck = cfg.getBooleanProperty(FASTLINKCHECK, false);
        PARAMETER = param.toString().replaceAll("http://(www\\.)?([a-z]{2}\\.)?spankbang\\.com/", "http://spankbang.com/");
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Language", "en");
        synchronized (ctrlLock) {
            if (!pluginLoaded.get()) {
                // load plugin!
                JDUtilities.getPluginForHost(DOMAIN);
                pluginLoaded.set(true);
            }
            br.getPage(PARAMETER);
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">this video is no longer available.<")) {
                final DownloadLink dl = createDownloadlink("directhttp://" + PARAMETER);
                dl.setProperty("offline", true);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            /* Decrypt start */
            final FilePackage fp = FilePackage.getInstance();

            /* Decrypt qualities START */
            String title = br.getRegex("<title>([^<>\"]*?) \\- SpankBang</title>").getMatch(0);
            final String fid = new Regex(PARAMETER, "spankbang\\.com/([a-z0-9]+)/video/").getMatch(0);
            final String streamkey = br.getRegex("var stream_key  = \\'([^<>\"]*?)\\'").getMatch(0);
            final String[] qualities = br.getRegex("class=\"ft\\-button ft\\-light-blue tt q_(\\d+p)\"").getColumn(0);
            if (qualities == null || qualities.length == 0 || streamkey == null || title == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                return null;
            }
            title = Encoding.htmlDecode(title.trim());
            fp.setName(title);
            for (final String quality : qualities) {
                final String directlink = "http://spankbang.com/_" + fid + "/" + streamkey + "/title/" + quality + "__mp4";
                FOUNDQUALITIES.put(quality, directlink);
            }

            if (FOUNDQUALITIES == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                return null;
            }
            /* Decrypt qualities END */
            /* Decrypt qualities, selected by the user */
            final ArrayList<String> selectedQualities = new ArrayList<String>();
            /* User selected nothing -> Decrypt everything */
            if (cfg.getBooleanProperty(ALLOW_BEST, false)) {
                final String[] allqualities = { "720p", "480p", "240p" };
                for (final String quality : allqualities) {
                    if (FOUNDQUALITIES.get(quality) != null) {
                        selectedQualities.add(quality);
                        break;
                    }
                }
            } else {
                boolean q240p = cfg.getBooleanProperty(ALLOW_240p, false);
                boolean q480p = cfg.getBooleanProperty(ALLOW_480p, false);
                boolean q720p = cfg.getBooleanProperty(ALLOW_720p, false);
                if (q240p == false && q480p == false && q720p == false) {
                    q240p = true;
                    q480p = true;
                    q720p = true;
                }
                if (q240p) {
                    selectedQualities.add("240p");
                }
                if (q480p) {
                    selectedQualities.add("480p");
                }
                if (q720p) {
                    selectedQualities.add("720p");
                }
            }
            for (final String selectedQualityValue : selectedQualities) {
                final String directlink = FOUNDQUALITIES.get(selectedQualityValue);
                if (directlink != null) {
                    final String finalname = title + "_" + selectedQualityValue + ".mp4";
                    final DownloadLink dl = createDownloadlink("http://spankbangdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                    dl.setFinalFileName(finalname);
                    if (fastcheck) {
                        dl.setAvailable(true);
                    }
                    dl.setProperty("LINKDUPEID", "spankbangcom_" + fid + "_" + selectedQualityValue);
                    dl.setProperty("plain_filename", finalname);
                    dl.setProperty("plain_directlink", directlink);
                    dl.setProperty("mainlink", PARAMETER);
                    fp.add(dl);
                    decryptedLinks.add(dl);
                }
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info(DOMAIN + ": None of the selected qualities were found, decrypting done...");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}