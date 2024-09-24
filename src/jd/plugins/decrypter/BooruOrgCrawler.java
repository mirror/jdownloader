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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BooruOrgCrawler extends PluginForDecrypt {
    public BooruOrgCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "tbib.org" });
        ret.add(new String[] { "booru.org" });
        ret.add(new String[] { "safebooru.org" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/.*?index\\.php\\?page=post\\&s=list\\&tags=[A-Za-z0-9_\\-]+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final UrlQuery query = UrlQuery.parse(contenturl);
        final String fpName = query.get("tags");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName).trim());
        final String url_part = contenturl;
        int page_counter = 1;
        int offset = 0;
        final int max_entries_per_page = 20;
        int entries_per_page_current = 0;
        final Set<String> dupes = new HashSet<String>();
        pagination: do {
            if (page_counter > 1) {
                this.br.getPage(url_part + "&pid=" + offset);
                if (this.br.containsHTML("You are viewing an advertisement")) {
                    this.br.getPage(url_part + "&pid=" + offset);
                }
            }
            logger.info("Decrypting: " + this.br.getURL());
            final String[] linkids = br.getRegex("id=\"p(\\d+)\"").getColumn(0);
            if (linkids == null || linkids.length == 0) {
                logger.warning("Decrypter might be broken for link: " + contenturl);
                break;
            }
            entries_per_page_current = linkids.length;
            int numberofNewItemsThisPage = 0;
            for (final String linkid : linkids) {
                if (!dupes.add(linkid)) {
                    continue;
                }
                final String link = br.getURL("index.php?page=post&s=view&id=" + linkid).toExternalForm();
                final DownloadLink dl = createDownloadlink(link);
                dl.setLinkID(linkid);
                dl.setAvailable(true);
                dl.setName(linkid + ".jpeg");
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
                offset++;
                numberofNewItemsThisPage++;
            }
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                break pagination;
            } else if (numberofNewItemsThisPage == 0) {
                logger.info("Stopping because: Failed to find any new items on current page");
                break pagination;
            } else {
                /* Continue to next page */
                page_counter++;
            }
        } while (entries_per_page_current >= max_entries_per_page);
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Danbooru;
    }
}
