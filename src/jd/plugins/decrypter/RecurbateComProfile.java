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
import java.util.List;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RecurbateComProfile extends PluginForDecrypt {
    public RecurbateComProfile(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "recurbate.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/performer/([^/]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String username = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        fp.addLinks(decryptedLinks);
        int page = 0;
        do {
            page += 1;
            logger.info("Crawling page " + page);
            final String[] videoIDs = br.getRegex("/play\\.php\\?video=(\\d+)").getColumn(0);
            if (videoIDs == null || videoIDs.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String videoID : videoIDs) {
                final DownloadLink dl = createDownloadlink("https://" + this.getHost() + "/play.php?video=" + videoID);
                dl.setName(username + "_" + videoID + ".mp4");
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            final String nextpage = br.getRegex("(/performer/[^/]+/page/" + (page + 1) + ")").getMatch(0);
            if (this.isAbort()) {
                break;
            } else if (videoIDs.length == 0) {
                logger.warning("Stopping because: Failed to find any items on current page");
            } else if (nextpage == null) {
                logger.info("Stopping because: Looks like we've reached last page: " + page);
                break;
            } else {
                logger.info("Continuing to next page: " + nextpage);
                br.getPage(nextpage);
                continue;
            }
        } while (true);
        return decryptedLinks;
    }
}
