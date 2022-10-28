package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangadex.org" }, urls = { "https?://(?:www\\.)?mangadex\\.(?:org|cc)/(chapter/[a-f0-9\\-]+|title/[a-f0-9\\-]+)(/[^/]*\\?tab=(art|chapters))?" })
public class MangadexOrg extends antiDDoSForDecrypt {
    public MangadexOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_CHAPTER = "^https?://[^/]+/chapter/([a-f0-9\\-]+).*";
    private static final String TYPE_LEGACY  = "^https?://[^/]+/chapter/(\\d+)$";
    private static final String TYPE_TITLE   = "^https?://[^/]+/title/([a-f0-9\\-]+).*";
    private final String        apiBase      = "https://api.mangadex.org/";

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 2;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (param.getCryptedUrl().matches(TYPE_LEGACY)) {
            /* Handling for older URLs */
            getPage(param.getCryptedUrl());
            if (br.getURL().matches(TYPE_CHAPTER)) {
                logger.info("Old URL: " + param.getCryptedUrl() + " | New URL: " + br.getURL());
                param.setCryptedUrl(br.getURL());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else if (param.getCryptedUrl().matches(TYPE_TITLE)) {
            final boolean artTab = StringUtils.containsIgnoreCase(param.getCryptedUrl(), "tab=art");
            final boolean chapterTab = StringUtils.containsIgnoreCase(param.getCryptedUrl(), "tab=chapters");
            final String mangaID = new Regex(param.getCryptedUrl(), TYPE_TITLE).getMatch(0);
            getPage(apiBase + "manga/" + mangaID + "/?includes[]=artist&includes[]=author&includes[]=cover_art");
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            if (!"ok".equals(root.get("result"))) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String title = (String) JavaScriptEngineFactory.walkJson(root, "data/attributes/title/en");
            final FilePackage fp;
            if (title != null) {
                fp = FilePackage.getInstance();
                fp.setName(title + " Covers");
            } else {
                fp = null;
            }
            if (artTab || !chapterTab) {
                // add cover art
                final HashSet<String> dups = new HashSet<String>();
                int offset = 0;
                int limit = 32;
                while (!isAbort()) {
                    getPage(apiBase + "cover?order[volume]=asc&manga[]=" + mangaID + "&limit=" + limit + "&offset=" + offset);
                    root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    if (!"ok".equals(root.get("result"))) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final List<Map<String, Object>> data = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(root, "data");
                    int count = 0;
                    for (final Map<String, Object> dataEntry : data) {
                        if ("cover_art".equals(dataEntry.get("type"))) {
                            final String volume = StringUtils.valueOfOrNull(JavaScriptEngineFactory.walkJson(dataEntry, "attributes/volume"));
                            final String fileName = StringUtils.valueOfOrNull(JavaScriptEngineFactory.walkJson(dataEntry, "attributes/fileName"));
                            if (fileName == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            } else if (dups.add(fileName)) {
                                final String url = "https://uploads.mangadex.org/covers/" + mangaID + "/" + fileName;
                                final DownloadLink link = createDownloadlink("directhttp://" + url);
                                link.setReferrerUrl(param.getCryptedUrl());
                                link.setAvailable(true);
                                final String volumeString;
                                if (volume == null) {
                                    volumeString = String.format(Locale.US, "-V%0" + StringUtils.getPadLength(data.size()) + "d", count);
                                } else if (volume.matches("\\d+")) {
                                    volumeString = String.format(Locale.US, "-V%0" + StringUtils.getPadLength(data.size()) + "d", Integer.parseInt(volume));
                                } else {
                                    volumeString = volume;
                                }
                                link.setFinalFileName(title + "-V" + volumeString + Plugin.getFileNameExtensionFromURL(url));
                                link.setContentUrl(param.getCryptedUrl() + "?tab=art");
                                ret.add(link);
                                fp.add(link);
                                distribute(link);
                                count++;
                            }
                        }
                    }
                    if (count != limit) {
                        break;
                    } else {
                        offset += data.size();
                    }
                }
            }
            if (chapterTab || !artTab) {
                // add chapter
                final HashSet<String> dups = new HashSet<String>();
                int offset = 0;
                int limit = 32;
                while (!isAbort()) {
                    getPage(apiBase + "manga/" + mangaID + "/feed?limit=" + limit + "&includes[]=scanlation_group&includes[]=user&order[volume]=desc&order[chapter]=desc&offset=" + offset + "&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&contentRating[]=pornographic");
                    root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    if (!"ok".equals(root.get("result"))) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final List<Map<String, Object>> data = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(root, "data");
                    int count = 0;
                    for (final Map<String, Object> dataEntry : data) {
                        if ("chapter".equals(dataEntry.get("type"))) {
                            final String id = (String) dataEntry.get("id");
                            if (dups.add(id)) {
                                final DownloadLink link = createDownloadlink("https://mangadex.org/chapter/" + id);
                                ret.add(link);
                                distribute(link);
                                count++;
                            }
                        }
                    }
                    if (count != limit) {
                        break;
                    } else {
                        offset += data.size();
                    }
                }
            }
            return ret;
        } else {
            final String chapterID = new Regex(param.getCryptedUrl(), TYPE_CHAPTER).getMatch(0);
            getPage(apiBase + "chapter/" + chapterID + "?includes[]=scanlation_group&includes[]=manga&includes[]=user");
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final Map<String, Object> data = (Map<String, Object>) root.get("data");
            final Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");
            final StringBuilder sb = new StringBuilder();
            String mangaTitle = (String) attributes.get("mangaTitle");
            final String volume = (String) attributes.get("volume");
            final String chapter = (String) attributes.get("chapter");
            final String translatedLanguage = (String) attributes.get("translatedLanguage");
            String title = (String) attributes.get("title");
            /* Find title (fallback) and description */
            final List<Map<String, Object>> relationships = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(root, "data/relationships");
            for (final Map<String, Object> relationship : relationships) {
                final String type = (String) relationship.get("type");
                if (type.equals("manga")) {
                    final Map<String, Object> mangaAttributes = (Map<String, Object>) relationship.get("attributes");
                    if (StringUtils.isEmpty(mangaTitle)) {
                        final Map<String, String> titles = (Map<String, String>) mangaAttributes.get("title");
                        if (!titles.isEmpty()) {
                            /* Prefer english titles */
                            if (titles.containsKey("en")) {
                                mangaTitle = titles.get("en");
                            } else {
                                /* Auto-use first language in that map */
                                mangaTitle = titles.entrySet().iterator().next().getValue();
                            }
                        }
                    }
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
            if (!StringUtils.isEmpty(translatedLanguage)) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
                sb.append(translatedLanguage);
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
                    link.setReferrerUrl(param.getCryptedUrl());
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
        }
        return ret;
    }
}
