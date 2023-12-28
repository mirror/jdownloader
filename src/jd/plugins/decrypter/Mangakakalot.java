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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class Mangakakalot extends PluginForDecrypt {
    public Mangakakalot(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String TYPE_MANGA         = "(?i)^https?://[^/]+/manga-([a-z0-9\\-]+)/?$";
    private final String TYPE_MANGA_CHAPTER = "(?i)^https?://[^/]+/(?:manga-|chapter/)([a-z0-9\\-_]+)/chapter[\\-_](\\d+(\\.\\d+)?)$";

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mangakakalot.com", "manganelo.com", "manganato.com", "manganelo.com", "chapmanganato.com", "chapmanganato.to", "readmanganato.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:manga-|chapter)[^\\s$]+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class=\"panel-not-found\"")) {
            /* 404 error page without response code 404 for example: https://manganato.com/manga-pw1337 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        if (param.getCryptedUrl().matches(TYPE_MANGA)) {
            /* Find all chapters of a manga */
            final String[] chapters = br.getRegex("<a[^>]+class\\s*=\\s*\"chapter-name[^\"]*\"[^>]+href\\s*=\\s*\"([^\"]+)\"").getColumn(0);
            if (chapters == null || chapters.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String chapter : chapters) {
                final DownloadLink dd = createDownloadlink(Encoding.htmlDecode(chapter).trim());
                ret.add(dd);
            }
            fp.addLinks(ret);
        } else if (param.getCryptedUrl().matches(TYPE_MANGA_CHAPTER)) {
            /* Find all images of a chapter */
            final String chapterNumber = new Regex(param.getCryptedUrl(), TYPE_MANGA_CHAPTER).getMatch(1);
            String breadcrumb = br.getRegex("<div[^>]+class\\s*=\\s*\"(?:panel-)?breadcrumb[^\"]*\"[^>]*>\\s*([^§]+)<div[^>]+class\\s*=\\s*\"panel").getMatch(0);
            breadcrumb = breadcrumb != null ? breadcrumb.replaceAll("<div[^>]+class\\s*=\\s*\\\"panel[^§]+", "") : null;
            //
            // Extract manga title
            //
            String mangaTitle = null;
            if (StringUtils.isEmpty(mangaTitle)) {
                mangaTitle = new Regex(breadcrumb, "<a[^>]+/manga[\\-/][^/>]*title\\s*=\\s*\"([^\"]+)").getMatch(0);
                if (StringUtils.isEmpty(mangaTitle)) {
                    mangaTitle = br.getRegex("<title>\\s*([^<]+)\\s+(?:Ch\\.|Chapter)[^<]+\\s-\\s+").getMatch(0);
                    if (StringUtils.isEmpty(mangaTitle)) {
                        mangaTitle = new Regex(breadcrumb, "<a[^>]+title\\s*=\\s*\"([^\"]+)").getMatch(0);
                        if (StringUtils.isEmpty(mangaTitle)) {
                            mangaTitle = new Regex(param.getCryptedUrl(), TYPE_MANGA_CHAPTER).getMatch(0).replace("-", " ");
                        }
                    }
                }
            }
            if (StringUtils.isNotEmpty(mangaTitle)) {
                mangaTitle = Encoding.htmlDecode(mangaTitle).trim();
                if (chapterNumber != null) {
                    fp.setName(mangaTitle + " Chapter " + chapterNumber);
                } else {
                    fp.setName(mangaTitle);
                }
            }
            //
            // Extract chapter title
            //
            String chapterTitle = null;
            if (StringUtils.isEmpty(chapterTitle)) {
                chapterTitle = new Regex(breadcrumb, "<a[^>]+/chapter[\\-_][^/>]*title\\s*=\\s*\"([^\"]+)").getMatch(0);
                if (StringUtils.isEmpty(chapterTitle)) {
                    chapterTitle = new Regex(breadcrumb, "<a[^>]+/chapter[\\-_][^/>]*>\\s*<span[^>]*\"name\"[^>]*>\\s*(.*?)\\s*<").getMatch(0);
                }
                if (StringUtils.isEmpty(chapterTitle)) {
                    chapterTitle = "Chapter_" + chapterNumber;
                }
            }
            //
            String imgSrc = br.getRegex("<div class=\"container-chapter-reader\">\\s+(.*?)\n").getMatch(0);
            if (imgSrc == null) {
                /* Fallback */
                imgSrc = br.getRequest().getHtmlCode();
            }
            final HashSet<String> dups = new HashSet<String>();
            final String[] urls = new Regex(imgSrc, "img src=\"(https?://[^\"]+)").getColumn(0);
            final int padLength = StringUtils.getPadLength(urls.length);
            int pageNumber = 1;
            for (final String url : urls) {
                final String realURL;
                final UrlQuery query = UrlQuery.parse(url);
                final String urlProxyBase64 = query.get("url_img");
                if (urlProxyBase64 != null) {
                    realURL = Encoding.Base64Decode(urlProxyBase64);
                } else {
                    realURL = url;
                }
                if (!dups.add(realURL)) {
                    /* Skip links that we've already added. */
                    continue;
                }
                final DownloadLink link = createDownloadlink(realURL);
                final String ext = Plugin.getFileNameExtensionFromURL(realURL);
                if (chapterTitle != null && ext != null) {
                    link.setFinalFileName(mangaTitle + "_" + chapterTitle + "-Page_" + StringUtils.formatByPadLength(padLength, pageNumber) + ext);
                }
                link.setAvailable(true);
                link._setFilePackage(fp);
                ret.add(link);
                distribute(link);
                pageNumber++;
            }
        } else {
            /* Unsupported URL -> Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "unsupported: " + param.getCryptedUrl());
        }
        return ret;
    }
}