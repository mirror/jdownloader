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

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornhub.com" }, urls = { "https?://(www\\.|[a-z]{2}\\.)?pornhub\\.com/(view_video\\.php\\?viewkey=[a-z0-9]+|embed/[a-z0-9]+|embed_player\\.php\\?id=\\d+)" }, flags = { 0 })
public class PornHubCom extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public PornHubCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DOMAIN = "pornhub.com";

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister-host plugin */
        JDUtilities.getPluginForHost(DOMAIN);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = jd.plugins.hoster.PornHubCom.correctAddedURL(param.toString());
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final LinkedHashMap<String, String[]> formats = jd.plugins.hoster.PornHubCom.formats;
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final Throwable e) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // Convert embed links to normal links
        if (parameter.matches("http://(www\\.)?pornhub\\.com/embed_player\\.php\\?id=\\d+")) {
            if (br.containsHTML("No htmlCode read") || br.containsHTML("flash/novideo\\.flv")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String newLink = br.getRegex("<link_url>(http://(www\\.)?pornhub\\.com/view_video\\.php\\?viewkey=[a-f0-9]+)</link_url>").getMatch(0);
            if (newLink == null) {
                return null;
            }
            parameter = newLink;
            br.getPage(parameter);
        }
        final String fpName = jd.plugins.hoster.PornHubCom.getSiteTitle(this.br);
        if (br.getURL().equals("http://www.pornhub.com/") || !br.containsHTML("\\'embedSWF\\'") || this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        if (this.br.containsHTML(jd.plugins.hoster.PornHubCom.html_privatevideo)) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName("This_video_is_private_" + fpName + ".mp4");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        final LinkedHashMap<String, String> foundLinks_all = jd.plugins.hoster.PornHubCom.getVideoLinksFree(this.br);

        final Iterator<Entry<String, String>> it = foundLinks_all.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, String> next = it.next();
            final String qualityInfo = next.getKey();
            final String finallink = next.getValue();
            if (cfg.getBooleanProperty(qualityInfo, true)) {
                final String final_filename = fpName + "_" + qualityInfo + "p.mp4";
                final DownloadLink dl = getDecryptDownloadlink();
                dl.setProperty("directlink", finallink);
                dl.setProperty("quality", qualityInfo);
                dl.setProperty("decryptedfilename", final_filename);
                dl.setProperty("mainlink", parameter);
                dl.setFinalFileName(final_filename);
                dl.setContentUrl(parameter);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private DownloadLink getDecryptDownloadlink() {
        return this.createDownloadlink("http://pornhubdecrypted" + new Random().nextInt(1000000000));
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