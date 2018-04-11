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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "weltderwunder.de" }, urls = { "https?://(?:www\\.|video\\.)?weltderwunder\\.de/.+" })
public class WeltderwunderDeDecrypter extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public WeltderwunderDeDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DOMAIN = "weltderwunder.de";

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister-host plugin */
        JDUtilities.getPluginForHost(DOMAIN);
        final PluginForDecrypt tele5hostPlugin = JDUtilities.getPluginForDecrypt("tele5.de");
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final String nicehost = new Regex(parameter, "https?://(?:www\\.|video\\.)?([^/]+)").getMatch(0);
        final String decryptedhost = "http://" + nicehost + "decrypted";
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final LinkedHashMap<String, String[]> formats = jd.plugins.hoster.WeltderwunderDe.formats;
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML("kaltura_player_")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String[] videosinfo = br.getRegex("kWidget\\.(?:thumb)?Embed\\(\\{(.*?)</script>").getColumn(0);
        if (videosinfo == null || videosinfo.length == 0) {
            videosinfo = br.getRegex("<script src=\"(https?://api\\.medianac\\.com/p/[^<>\"]+uiconf_id/[^<>\"]+entry_id[^<>\"]+)\"></script>").getColumn(0);
        }
        HashMap<String, DownloadLink> foundLinks_all = new HashMap<String, DownloadLink>();
        /* parse flash url */
        ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
        for (final String videosource : videosinfo) {
            HashMap<String, DownloadLink> foundLinks = ((jd.plugins.decrypter.TeleFiveDeDecrypter) tele5hostPlugin).getURLsFromMedianac(this.br, decryptedhost, videosource, formats);
            foundLinks_all.putAll(foundLinks);
            /* Only decrypt the first entry */
            break;
        }
        final Iterator<Entry<String, DownloadLink>> it = foundLinks_all.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, DownloadLink> next = it.next();
            final String qualityInfo = next.getKey();
            final DownloadLink dl = next.getValue();
            if (cfg.getBooleanProperty(qualityInfo, true)) {
                newRet.add(dl);
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(newRet);
        }
        decryptedLinks.addAll(newRet);
        return decryptedLinks;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KalturaVideoPlatform;
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