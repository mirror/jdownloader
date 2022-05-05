package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "manganato.com" }, urls = { "https?://(?:www\\.)?(?:manganelo|readmanganato|manganato)\\.com/manga-[a-z0-9\\-]+(?:/chapter-\\d+(\\.\\d+)?)?" })
public class ManganatoCom extends antiDDoSForDecrypt {
    public ManganatoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String TYPE_MANGA         = "^https?://[^/]+/manga-([a-z0-9\\-]+)$";
    private final String TYPE_MANGA_CHAPTER = "^https?://[^/]+/manga-([a-z0-9\\-]+)/chapter-(\\d+(\\.\\d+)?)$";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        getPage(br, param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (param.getCryptedUrl().matches(TYPE_MANGA)) {
            String[] chapters = br.getRegex("<a[^>]+class\\s*=\\s*\"chapter-name[^\"]*\"[^>]+href\\s*=\\s*\"([^\"]+)\"").getColumn(0);
            if (chapters != null && chapters.length > 0) {
                for (String chapter : chapters) {
                    ret.add(createDownloadlink(Encoding.htmlDecode(chapter)));
                }
            }
        } else if (param.getCryptedUrl().matches(TYPE_MANGA_CHAPTER)) {
            final String chapterNumber = new Regex(param.getCryptedUrl(), TYPE_MANGA_CHAPTER).getMatch(1);
            String mangaTitle = br.getRegex("<title>([^<>\"]+)- Manganelo</title>").getMatch(0);
            String title;
            if (mangaTitle != null) {
                title = Encoding.htmlDecode(mangaTitle).trim().replaceFirst(" Chapter ", "-Ch_");
                // title = Encoding.htmlDecode(mangaTitle).trim() + "-Ch" + chapterNumber + "";
            } else {
                /* Fallback */
                mangaTitle = new Regex(param.getCryptedUrl(), TYPE_MANGA_CHAPTER).getMatch(0).replace("-", " ");
                title = mangaTitle + "-Chapter_" + chapterNumber;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            String imgSrc = br.getRegex("<div class=\"container-chapter-reader\">(.+)").getMatch(0);
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
                    continue;
                }
                final DownloadLink link = createDownloadlink(realURL);
                final String ext = Plugin.getFileNameExtensionFromURL(realURL);
                if (title != null && ext != null) {
                    link.setFinalFileName(title + "-Page_" + String.format(Locale.US, "%0" + padLength + "d", pageNumber) + ext);
                }
                link.setAvailable(true);
                link._setFilePackage(fp);
                ret.add(link);
                distribute(link);
                pageNumber++;
            }
            // String images[][] = br.getRegex("(?i)img\\s*src\\s*=\\s*\"(https?://[^\"]*?/(\\d+)\\.(jpe?g|png))\"").getMatches();
            // if (images == null || images.length == 0) {
            // images = br.getRegex("(?i)img\\s*src\\s*=\\s*\"(https?://[^\"]*?/img/[^\"]*/(\\d+)[^/]*\\.(jpe?g|png))\"").getMatches();
            // }
            // if (images == null || images.length == 0) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // for (final String image[] : images) {
            // if (dups.add(image[0])) {
            // final DownloadLink link = createDownloadlink("directhttp://" + image[0]);
            // if (title != null) {
            // link.setFinalFileName(title + "-Page_" + String.format(Locale.US, "%0" + padLength + "d", pageNumber) + "." + image[2]);
            // }
            // link.setAvailable(true);
            // link._setFilePackage(fp);
            // ret.add(link);
            // distribute(link);
            // }
            // }
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
