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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornhub.com" }, urls = { "https?://(?:www\\.|[a-z]{2}\\.)?pornhub\\.com/(?:.*\\?viewkey=[a-z0-9]+|embed/[a-z0-9]+|embed_player\\.php\\?id=\\d+|users/[^/]+/videos/public)|https?://(?:[a-z]+\\.)?pornhubpremium\\.com/(?:view_video\\.php\\?viewkey=|embed/)[a-z0-9]+" })
public class PornHubCom extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public PornHubCom(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 333);
    }

    final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    private String                parameter      = null;
    private Account               aa             = null;
    private static final String   DOMAIN         = "pornhub.com";
    private static final String   BEST_ONLY      = jd.plugins.hoster.PornHubCom.BEST_ONLY;
    private static final String   FAST_LINKCHECK = jd.plugins.hoster.PornHubCom.FAST_LINKCHECK;

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister-host plugin */
        final PluginForHost pornhubHosterPlugin = JDUtilities.getPluginForHost(DOMAIN);
        parameter = jd.plugins.hoster.PornHubCom.correctAddedURL(param.toString());
        br.setFollowRedirects(true);
        jd.plugins.hoster.PornHubCom.prepBr(br);
        aa = AccountController.getInstance().getValidAccount(pornhubHosterPlugin);
        if (aa != null) {
            jd.plugins.hoster.PornHubCom.login(br, aa, false);
        }
        if (parameter.matches(".+/users/.+")) {
            decryptAllVideosOfAUser();
        } else {
            decryptSingleVideo();
        }
        return decryptedLinks;
    }

    private void decryptAllVideosOfAUser() throws Exception {
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return;
        }
        final String username = new Regex(parameter, "users/([^/]+)/").getMatch(0);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int page = 1;
        final int max_entries_per_page = 50;
        int last_count_entries_per_page;
        do {
            if (this.isAbort()) {
                return;
            }
            if (page > 1) {
                jd.plugins.hoster.PornHubCom.getPage(br, "/users/" + username + "/videos/public/ajax?o=mr&page=" + page);
            }
            final String[] viewkeys = br.getRegex("_vkey=\"([^<>\"]+)\"").getColumn(0);
            for (final String viewkey : viewkeys) {
                final DownloadLink dl = this.createDownloadlink("http://www." + this.getHost() + "/view_video.php?viewkey=" + viewkey);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            last_count_entries_per_page = viewkeys.length;
            page++;
        } while (last_count_entries_per_page >= max_entries_per_page);
    }

    private void decryptSingleVideo() throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final boolean bestonly = cfg.getBooleanProperty(BEST_ONLY, false);
        final boolean fastlinkcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);
        /* Convert embed links to normal links */
        if (parameter.matches(".+/embed_player\\.php\\?id=\\d+")) {
            if (br.containsHTML("No htmlCode read") || br.containsHTML("flash/novideo\\.flv")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return;
            }
            final String newLink = br.getRegex("<link_url>(https?://(?:www\\.)?pornhub\\.com/view_video\\.php\\?viewkey=[a-z0-9]+)</link_url>").getMatch(0);
            if (newLink == null) {
                throw new DecrypterException("Decrypter broken");
            }
            parameter = newLink;
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
        }
        final String viewkey = jd.plugins.hoster.PornHubCom.getViewkeyFromURL(parameter);
        jd.plugins.hoster.PornHubCom.getPage(br, jd.plugins.hoster.PornHubCom.createPornhubVideolink(viewkey, aa));
        final String fpName = jd.plugins.hoster.PornHubCom.getSiteTitle(this, br);
        if (isOffline(br)) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName("viewkey=" + viewkey);
            decryptedLinks.add(dl);
            return;
        }
        if (br.containsHTML(jd.plugins.hoster.PornHubCom.html_privatevideo)) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName("This_video_is_private_" + fpName + ".mp4");
            decryptedLinks.add(dl);
            return;
        }
        if (br.containsHTML(jd.plugins.hoster.PornHubCom.html_premium_only)) {
            decryptedLinks.add(createOfflinelink(parameter, fpName + ".mp4", "Private_video_Premium_required"));
            return;
        }
        final LinkedHashMap<String, String> foundLinks_all = jd.plugins.hoster.PornHubCom.getVideoLinksFree(br);
        if (foundLinks_all == null) {
            throw new DecrypterException("Decrypter broken");
        }
        final Iterator<Entry<String, String>> it = foundLinks_all.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, String> next = it.next();
            final String qualityInfo = next.getKey();
            final String finallink = next.getValue();
            if (StringUtils.isEmpty(finallink)) {
                continue;
            }
            if (cfg.getBooleanProperty(qualityInfo, true) || bestonly) {
                final String final_filename = fpName + "_" + qualityInfo + "p.mp4";
                final DownloadLink dl = getDecryptDownloadlink();
                dl.setProperty("directlink", finallink);
                dl.setProperty("quality", qualityInfo);
                dl.setProperty("decryptedfilename", final_filename);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("viewkey", viewkey);
                dl.setLinkID(getHost() + "://" + viewkey + qualityInfo);
                dl.setFinalFileName(final_filename);
                dl.setContentUrl(parameter);
                if (fastlinkcheck) {
                    dl.setAvailable(true);
                }
                decryptedLinks.add(dl);
                if (bestonly) {
                    /* Our LinkedHashMap is already in the right order so best = first entry --> Step out of the loop */
                    logger.info("User wants best-only");
                    break;
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
    }

    public static boolean isOffline(final Browser br) {
        return br.getURL().equals("http://www.pornhub.com/") || !br.containsHTML("\\'embedSWF\\'") || br.getHttpConnection().getResponseCode() == 404;
    }

    private DownloadLink getDecryptDownloadlink() {
        return this.createDownloadlink("http://pornhubdecrypted" + new Random().nextInt(1000000000));
    }

    public int getMaxConcurrentProcessingInstances() {
        return 2;// seems they try to block crawling
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