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

import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.PorntrexCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { PorntrexCom.class })
public class PorntrexComCrawler extends PluginForDecrypt {
    public PorntrexComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private static List<String[]> getPluginDomains() {
        return PorntrexCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(playlists|albums)/(\\d+)/([a-z0-9\\-]+)/");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArrayList<String> dupes = new ArrayList<String>();
        final String addedlink = param.getCryptedUrl();
        final Regex photoalbum = new Regex(addedlink, "(?i)^https?://[^/]+/albums/(\\d+)/([a-z0-9\\-]+)/?$");
        if (photoalbum.patternFind()) {
            /* Photo album */
            final String slug = photoalbum.getMatch(1);
            final String title = slug.replace("-", " ").trim();
            br.setFollowRedirects(true);
            br.getPage(addedlink);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(">\\s*This album is a private album uploaded by")) {
                logger.info("Private album -> Account required to access it");
                throw new AccountRequiredException();
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            final String[] urls = br.getRegex("href=\"(https?://[^\"]+)\" class=\"item\" rel=\"images\"").getColumn(0);
            if (urls == null || urls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String url : urls) {
                final DownloadLink image = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(url));
                final String name = new Regex(url, "/([^/]+)/$").getMatch(0);
                if (name != null) {
                    image.setName(name);
                }
                image.setAvailable(true);
                image._setFilePackage(fp);
                ret.add(image);
            }
        } else {
            /* Playlist */
            final String url_playlist_name = new Regex(addedlink, this.getSupportedLinks()).getMatch(2);
            String fpName = url_playlist_name.replace("-", " ");
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            final UrlQuery query = UrlQuery.parse("mode=async&function=get_block&block_id=playlist_view_playlist_view_dev&sort_by=added2fav_date&_=" + System.currentTimeMillis());
            int page = 1;
            int addedItems = 0;
            final int minItemsPerPage = 4;
            final String url_base = addedlink;
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
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
                        dl.setName(url_name.replace("-", " ").trim() + ".mp4");
                    }
                    dl._setFilePackage(fp);
                    distribute(dl);
                    ret.add(dl);
                    addedItems++;
                }
                page++;
                hasNextPage = br.containsHTML("from1:0*" + page);
            } while (addedItems >= minItemsPerPage && hasNextPage && !this.isAbort());
        }
        return ret;
    }
}
