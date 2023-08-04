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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "animegalleries.net" }, urls = { "https?://(?:www\\.)?animegalleries\\.net/album/\\d+" })
public class AnimegalleriesNetCrawler extends PluginForDecrypt {
    public AnimegalleriesNetCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.IMAGE_HOST };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("\">([^<>\"]+)</h2>").getMatch(0);
        if (fpName == null) {
            /* Fallback */
            fpName = new Regex(parameter, "(\\d+)$").getMatch(0);
        }
        String next = null;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName).trim());
        fp.addLinks(ret);
        int page = 0;
        final HashSet<String> dupes = new HashSet<String>();
        do {
            page++;
            if (next != null) {
                br.getPage(next);
            }
            final String[] linkids = br.getRegex("\"/img/(\\d+)\"").getColumn(0);
            if (linkids == null || linkids.length == 0) {
                break;
            }
            int newItemsOnThisPage = 0;
            for (final String contentID : linkids) {
                if (!dupes.add(contentID)) {
                    continue;
                }
                final String singleLink = "http://www.animegalleries.net/img/" + contentID;
                final DownloadLink dl = createDownloadlink(singleLink);
                dl._setFilePackage(fp);
                dl.setAvailable(true);
                dl.setName(contentID + ".jpg");
                ret.add(dl);
                distribute(dl);
                newItemsOnThisPage++;
            }
            logger.info("Crawled page" + page + "| New items on this page: " + newItemsOnThisPage + " | Total so far: " + ret.size());
            next = this.br.getRegex("class=\"tableb_compact\".*?class=\"navmenu\"><a href=\"(/album/\\d+/page/\\d+)\"").getMatch(0);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                return ret;
            } else if (newItemsOnThisPage == 0) {
                logger.info("Stopping becaused: Failed to find any new items on current page: " + page);
                break;
            } else if (next == null) {
                logger.info("Stopping because: Failed to find next page -> Reached end?");
                break;
            }
        } while (next != null);
        return ret;
    }
}
