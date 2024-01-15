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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rule34.paheal.net" }, urls = { "https?://(?:www\\.)?rule34\\.paheal\\.net/post/(list/[\\w\\-\\.%!]+|view)/\\d+" })
public class Rule34PahealNet extends PluginForDecrypt {
    public Rule34PahealNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*No Images Found\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(contenturl, "/post/list/(.*?)/\\d+").getMatch(0));
        final HashSet<String> loop = new HashSet<String>();
        final HashSet<String> dupes = new HashSet<String>();
        String next = null;
        do {
            String[] links = br.getRegex("<br><a href=('|\")(https?://.*?)\\1>").getColumn(1);
            if (links == null || links.length == 0) {
                links = br.getRegex("('|\")(https?://[^/]+\\.paheal\\.net/_images/[a-z0-9]+/.*?)\\1").getColumn(1);
                if (links == null || links.length == 0) {
                    links = br.getRegex("('|\")(https?://rule34-[a-zA-Z0-9\\-]*?\\.paheal\\.net/_images/[a-z0-9]+/.*?)\\1").getColumn(1);
                    if (links == null || links.length == 0) {
                        /* 2023-01-15 */
                        links = br.getRegex("(https://\\w+\\.paheal-cdn\\.net/[a-f0-9]{2}/[a-f0-9]{2}/[a-f0-9]{32})").getColumn(0);
                    }
                }
            }
            if (links == null || links.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int numberofNewItems = 0;
            for (final String singlelink : links) {
                if (dupes.add(singlelink)) {
                    final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(singlelink));
                    dl.setAvailable(true);
                    fp.add(dl);
                    ret.add(dl);
                    distribute(dl);
                    numberofNewItems++;
                }
            }
            next = br.getRegex("\"(/post/[^<>\"]*?)\">\\s*Next\\s*</a>").getMatch(0);
            if (numberofNewItems == 0) {
                logger.info("Stopping because: Failed to find any new items");
                break;
            } else if (next == null) {
                logger.info("Stopping because: Failed to find next page URL");
                break;
            } else if (!loop.add(next)) {
                logger.info("Stopping because: The page we were about to crawl as next page has already been crawled -> Reached end?");
                break;
            } else {
                /* Continue to next page */
                sleep(1000, param);
                br.getPage(next);
            }
        } while (!this.isAbort());
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Danbooru;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2023-01-15 */
        return 2;
    }
}