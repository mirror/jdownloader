package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangadex.org" }, urls = { "https?://(?:www\\.)?mangadex\\.(?:org|cc)/chapter/[a-f0-9\\-]+" })
public class MangadexOrg extends antiDDoSForDecrypt {
    public MangadexOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_CHAPTER = "^https?://[^/]+/chapter/([a-f0-9\\-]+)$";
    private static final String TYPE_LEGACY  = "^https?://[^/]+/chapter/(\\d+)$";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(TYPE_LEGACY)) {
            /* Handling for older URLs */
            getPage(param.getCryptedUrl());
            if (br.getURL().matches(TYPE_CHAPTER)) {
                logger.info("Old URL: " + param.getCryptedUrl() + " | New URL: " + br.getURL());
                param.setCryptedUrl(br.getURL());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final String apiBase = "https://api.mangadex.org/";
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String chapterID = new Regex(param.getCryptedUrl(), TYPE_CHAPTER).getMatch(0);
        getPage(apiBase + "chapter/" + chapterID + "?includes[]=scanlation_group&includes[]=manga&includes[]=user");
        if (br.getHttpConnection().getResponseCode() == 404) {
            ret.add(this.createOfflinelink(param.getCryptedUrl()));
            return ret;
        }
        final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Map<String, Object> data = (Map<String, Object>) root.get("data");
        final Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");
        final StringBuilder sb = new StringBuilder();
        String mangaTitle = (String) attributes.get("mangaTitle");
        final String volume = (String) attributes.get("volume");
        final String chapter = (String) attributes.get("chapter");
        String title = (String) attributes.get("title");
        String description = null;
        /* Find title (fallback) and description */
        final List<Map<String, Object>> relationships = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(root, "data/relationships");
        for (final Map<String, Object> relationship : relationships) {
            final String type = (String) relationship.get("type");
            if (type.equals("manga")) {
                final Map<String, Object> mangaAttributes = (Map<String, Object>) relationship.get("attributes");
                if (StringUtils.isEmpty(mangaTitle)) {
                    mangaTitle = (String) JavaScriptEngineFactory.walkJson(mangaAttributes, "title/en");
                }
                description = (String) JavaScriptEngineFactory.walkJson(mangaAttributes, "description/en");
                final String status = (String) mangaAttributes.get("status");
                if ("unavailable".equals(status)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                break;
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        if (StringUtils.isNotEmpty(mangaTitle)) {
            if (sb.length() > 0) {
                sb.append("-");
            }
            sb.append(mangaTitle);
            if (!StringUtils.isEmpty(description)) {
                fp.setComment(description);
            }
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
        this.getPage("/at-home/server/" + chapterID + "?forcePort443=false");
        final Map<String, Object> root2 = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Map<String, Object> chapter2 = (Map<String, Object>) root2.get("chapter");
        final String baseUrl = (String) root2.get("baseUrl");
        final String hash = (String) chapter2.get("hash");
        final List<String> pages = (List<String>) chapter2.get("data");
        if (pages == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int pageIndex = 0;
        for (String url : pages) {
            pageIndex++;
            if (url == null) {
                continue;
            } else {
                url = baseUrl + "/data/" + hash + "/" + url;
                final DownloadLink link = createDownloadlink("directhttp://" + url);
                link.setProperty("refURL", param.getCryptedUrl());
                link.setAvailable(true);
                if (titleForFilename != null) {
                    link.setFinalFileName(titleForFilename + "-Page_" + pageIndex + Plugin.getFileNameExtensionFromURL(url));
                }
                link.setContentUrl(param.getCryptedUrl() + "/" + pageIndex);
                link.setLinkID(getHost() + "://" + chapterID + "/" + url);
                fp.add(link);
                distribute(link);
            }
        }
        return ret;
    }
}
