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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangakakalot.com" }, urls = { "https?://(www\\.)?(manganelo|readmanganato|manganato|mangakakalot|chapmanganato)\\.com/(?:manga-|chapter)[^\\s$]+" })
public class Mangakakalot extends antiDDoSForDecrypt {
    public Mangakakalot(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String TYPE_MANGA         = "^https?://[^/]+/manga-([a-z0-9\\-]+)$";
    private final String TYPE_MANGA_CHAPTER = "^https?://[^/]+/(?:manga-|chapter/)([a-z0-9\\-]+)/chapter[\\-_](\\d+(\\.\\d+)?)$";

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        getPage(br, param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        if (param.getCryptedUrl().matches(TYPE_MANGA)) {
            String[] chapters = br.getRegex("<a[^>]+class\\s*=\\s*\"chapter-name[^\"]*\"[^>]+href\\s*=\\s*\"([^\"]+)\"").getColumn(0);
            if (chapters != null && chapters.length > 0) {
                for (String chapter : chapters) {
                    final DownloadLink dd = createDownloadlink(Encoding.htmlDecode(chapter).trim());
                    ret.add(dd);
                }
                String fpName = br.getRegex("<title>\\s*([^<]+)\\s+Manga\\s+Online").getMatch(0);
                if (StringUtils.isNotEmpty(fpName)) {
                    fp.setAllowInheritance(true);
                    fp.setName(Encoding.htmlDecode(fpName).trim());
                }
            }
            fp.addLinks(ret);
        } else if (param.getCryptedUrl().matches(TYPE_MANGA_CHAPTER)) {
            final String chapterNumber = new Regex(param.getCryptedUrl(), TYPE_MANGA_CHAPTER).getMatch(1);
            //
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
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "unsupported:" + param.getCryptedUrl());
        }
        return ret;
    }
}