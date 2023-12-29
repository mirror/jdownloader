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

import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gelbooru.com" }, urls = { "https?://(?:www\\.)?gelbooru\\.com/index\\.php\\?page=post\\&s=list\\&tags=.+" })
public class GelbooruComCrawler extends PluginForDecrypt {
    public GelbooruComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String contenturl = param.getCryptedUrl().replaceFirst("http://", "https://");
        br.setCookie(getHost(), "fringeBenefits", "yup");
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final UrlQuery query = UrlQuery.parse(contenturl);
        final String tags = query.get("tags");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(tags).trim());
        final String url_part = contenturl;
        int pageCounter = 1;
        int offset = 0;
        final int max_entries_per_page = 42;
        int entries_per_page_current = 0;
        int adPagesSkipped = 0;
        final ArrayList<String> dupes = new ArrayList<String>();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        do {
            int numberofNewItemsOnThisPage = 0;
            String[] contentIDs = br.getRegex("id=\"(?:s|p)(\\d+)\"").getColumn(0);
            if (contentIDs.length == 0) {
                /* Fallback */
                contentIDs = br.getRegex("page=post&[^\"]*id=(\\d+)\\&tags=").getColumn(0);
            }
            if (contentIDs == null || contentIDs.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            entries_per_page_current = contentIDs.length;
            for (final String contentID : contentIDs) {
                if (dupes.contains(contentID)) {
                    continue;
                }
                dupes.add(contentID);
                numberofNewItemsOnThisPage++;
                final String link = "https://" + this.getHost() + "/index.php?page=post&s=view&id=" + contentID;
                final DownloadLink dl = createDownloadlink(link);
                dl.setLinkID(contentID);
                dl.setAvailable(true);
                dl.setName(contentID + ".jpeg");
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
                offset++;
            }
            logger.info("Crawled page " + pageCounter + " | Found items on this page: " + numberofNewItemsOnThisPage + " | Total: " + ret.size());
            pageCounter++;
            if (br.containsHTML(">\\s*Unable to go this deep in pagination")) {
                logger.info("Stopping because: Account required to continue pagination");
                break;
            } else if (numberofNewItemsOnThisPage == 0) {
                /* Fail-safe */
                logger.info("Stoping because: Failed to find any items on current page");
                break;
            } else if (entries_per_page_current < max_entries_per_page) {
                logger.info("Stopping because: Reached end");
                break;
            } else {
                this.br.getPage(url_part + "&pid=" + offset);
                if (this.br.containsHTML("You are viewing an advertisement")) {
                    logger.info("Skipping ad " + adPagesSkipped);
                    this.br.getPage(url_part + "&pid=" + offset);
                    adPagesSkipped++;
                }
            }
        } while (!this.isAbort());
        return ret;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Danbooru;
    }
}
