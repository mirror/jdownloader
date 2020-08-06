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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porntrex.com" }, urls = { "https?://(?:www\\.)?porntrex\\.com/playlists/(\\d+)/([a-z0-9\\-]+)/" })
public class PorntrexComCrawler extends PluginForDecrypt {
    public PorntrexComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> dupes = new ArrayList<String>();
        final String parameter = param.toString();
        final String url_playlist_name = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        String fpName = url_playlist_name.replace("-", " ");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        final UrlQuery query = new UrlQuery().parse("mode=async&function=get_block&block_id=playlist_view_playlist_view_dev&sort_by=added2fav_date&_=" + System.currentTimeMillis());
        int page = 1;
        int addedItems = 0;
        final int minItemsPerPage = 4;
        final String url_base = parameter;
        boolean hasNextPage = false;
        do {
            final UrlQuery thisQuery = query;
            thisQuery.add("from1", page + "");
            br.getPage(url_base + "?" + thisQuery.toString());
            if (br.getHttpConnection().getResponseCode() == 404) {
                if (page > 1) {
                    logger.info("404 response --> Probably reached last page");
                    break;
                } else {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
            }
            final String[] urls = br.getRegex("data-playlist-item=\"(https?://[^\"]*/video/\\d+/[a-z0-9\\-]+)\"").getColumn(0);
            for (final String url : urls) {
                if (dupes.contains(url)) {
                    continue;
                }
                dupes.add(url);
                final DownloadLink dl = this.createDownloadlink(url);
                final String url_name = new Regex(url, "([a-z0-9\\-]+)$").getMatch(0);
                dl.setAvailable(true);
                if (url_name != null) {
                    dl.setName(url_name.replace("-", " ") + ".mp4");
                }
                dl._setFilePackage(fp);
                distribute(dl);
                decryptedLinks.add(dl);
                addedItems++;
            }
            page++;
            hasNextPage = br.containsHTML("from1:0*" + page);
        } while (addedItems >= minItemsPerPage && hasNextPage && !this.isAbort());
        return decryptedLinks;
    }
}
