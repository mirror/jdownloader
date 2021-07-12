package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangadex.org" }, urls = { "https?://(?:www\\.)?mangadex\\.org/title/\\d+/[a-z0-9\\-]+/covers/?|https?://(?:www\\.)?mangadex\\.org/chapter/[a-f0-9\\-]+" })
public class MangadexOrg extends antiDDoSForDecrypt {
    public MangadexOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_CHAPTER = "https?://(?:www\\.)?mangadex\\.org/chapter/([a-f0-9\\-]+)";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl().replace("mangadex.cc", "mangadex.org"));
        final String apiBase = "https://api.mangadex.org/";
        String urlTitle = new Regex(param.getCryptedUrl(), "/title/\\d+/([^/]+)").getMatch(0);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (StringUtils.containsIgnoreCase(param.getCryptedUrl(), "/covers")) {
            String[] covers = br.getRegex("<a[^>]+href\\s*=\\s*[\"']([^\"']*/covers/[^\"'/]*)").getColumn(0);
            if (covers != null && covers.length > 0) {
                for (String cover : covers) {
                    cover = cover.trim().replaceAll("^//", "https://");
                    if (cover.startsWith("/") || !cover.startsWith("http")) {
                        cover = br.getURL(cover).toString();
                    }
                    final DownloadLink dl = createDownloadlink(cover);
                    dl.setAvailable(true);
                    ret.add(dl);
                }
                if (urlTitle != null) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(urlTitle);
                    fp.addLinks(ret);
                }
            }
        } else if (StringUtils.containsIgnoreCase(param.getCryptedUrl(), "/title/")) {
            String[] chapters = br.getRegex("<a[^>]+href\\s*=\\s*[\"']([^\"']*/chapter/[^\"'/]*)").getColumn(0);
            if (chapters != null && chapters.length > 0) {
                for (String chapter : chapters) {
                    chapter = chapter.trim().replaceAll("^//", "https://");
                    if (chapter.startsWith("/") || !chapter.startsWith("http")) {
                        chapter = br.getURL(chapter).toString();
                    }
                    ret.add(createDownloadlink(chapter));
                }
            }
        } else if (param.getCryptedUrl().matches(TYPE_CHAPTER)) {
            final String chapterID = new Regex(param.getCryptedUrl(), TYPE_CHAPTER).getMatch(0);
            getPage(apiBase + "chapter/" + chapterID + "?includes[]=scanlation_group&includes[]=manga&includes[]=user");
            if (br.getHttpConnection().getResponseCode() == 404) {
                ret.add(this.createOfflinelink(param.getCryptedUrl()));
                return ret;
            }
            Map<String, Object> map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            map = (Map<String, Object>) map.get("data");
            map = (Map<String, Object>) map.get("attributes");
            final String hash = (String) map.get("hash");
            /* 2021-07-12: Hardcoded */
            final String server = "https://uploads.mangadex.org/data/";
            final String status = (String) map.get("status");
            if ("unavailable".equals(status)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (StringUtils.isEmpty(hash)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // final String mangaID = map.get("mangaId") != null ? String.valueOf(map.get("mangaId")) : null;
            final StringBuilder sb = new StringBuilder();
            final String mangaTitle = (String) map.get("mangaTitle");
            final String volume = (String) map.get("volume");
            final String chapter = (String) map.get("chapter");
            final String title = (String) map.get("title");
            final FilePackage fp = FilePackage.getInstance();
            if (StringUtils.isNotEmpty(mangaTitle)) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
                sb.append(mangaTitle);
            }
            if (StringUtils.isNotEmpty(volume)) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
                sb.append("Vol_");
                sb.append(volume);
            }
            if (StringUtils.isNotEmpty(chapter)) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
                sb.append("Ch_");
                sb.append(chapter);
            }
            if (!StringUtils.isEmpty(title)) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
                sb.append(title);
            }
            fp.setName(sb.toString());
            final String titleForFilename;
            if (!StringUtils.isEmpty(title)) {
                titleForFilename = title;
            } else {
                titleForFilename = mangaTitle;
            }
            final List<String> pages = (List<String>) map.get("data");
            if (pages == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int pageIndex = 0;
            for (final String page : pages) {
                pageIndex++;
                if (page == null) {
                    continue;
                } else if (page instanceof String) {
                    final String url = br.getURL(server + hash + "/" + page).toString();
                    final DownloadLink link = createDownloadlink("directhttp://" + url);
                    link.setProperty("refURL", param.getCryptedUrl());
                    link.setAvailable(true);
                    if (titleForFilename != null) {
                        link.setFinalFileName(titleForFilename + "-Page_" + pageIndex + Plugin.getFileNameExtensionFromURL(url));
                    }
                    link.setContentUrl(param.getCryptedUrl() + "/" + pageIndex);
                    link.setLinkID(getHost() + "://" + chapterID + "/" + page);
                    fp.add(link);
                    distribute(link);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        return ret;
    }
}
