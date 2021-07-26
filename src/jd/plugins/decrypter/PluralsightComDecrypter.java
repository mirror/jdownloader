package jd.plugins.decrypter;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.components.config.PluralsightComConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.hoster.PluralsightCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 1, names = { "pluralsight.com" }, urls = { "https?://(?:app|www)?\\.pluralsight\\.com(\\/library)?\\/courses\\/[^/]+|https://app\\.pluralsight\\.com/course-player\\?clipId=[a-f0-9\\-]+" })
public class PluralsightComDecrypter extends antiDDoSForDecrypt {
    public PluralsightComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return PluralsightComConfig.class;
    }

    @Override
    public void sendRequest(Browser ibr, Request request) throws Exception {
        super.sendRequest(ibr, request);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final boolean useNewHandling = true;
        if (useNewHandling) {
            return newHandling(param);
        } else {
            return oldHandling(param);
        }
    }

    private ArrayList<DownloadLink> newHandling(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        if (account != null) {
            PluralsightCom.login(account, br, this, false);
        }
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final UrlQuery query = new UrlQuery().parse(param.getCryptedUrl());
        if (!query.containsKey("clipId")) {
            logger.info("Looking for clipIDs");
            final Set<String> dup = new HashSet<String>();
            final String clipPlayerURL[] = br.getRegex("(/course-player\\?clipId=[a-f0-9\\-]+)\"").getColumn(0);
            if (clipPlayerURL.length == 0) {
                /* Content offline or plugin broken */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            for (final String clipID : clipPlayerURL) {
                if (dup.add(clipID)) {
                    ret.add(createDownloadlink(URLHelper.parseLocation(new URL("https://app.pluralsight.com"), clipID).toString()));
                }
            }
            return ret;
        }
        final String jsonRoot = br.getRegex("type=\"application/json\">(\\{.*?)</script>").getMatch(0);
        final Map<String, Object> root = JSonStorage.restoreFromString(jsonRoot, TypeRef.HASHMAP);
        final Map<String, Object> course = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "props/pageProps/tableOfContents");
        final String courseTitle = (String) course.get("title");
        final List<Map<String, Object>> modules = (List<Map<String, Object>>) course.get("modules");
        int moduleIndex = 0;
        int totalNumberofClips = 0;
        for (final Map<String, Object> module : modules) {
            final String moduleTitle = PluralsightCom.correctFileName((String) module.get("title"));
            final List<Map<String, Object>> clips = (List<Map<String, Object>>) module.get("contentItems");
            int clipIndex = 0;
            for (final Map<String, Object> clip : clips) {
                totalNumberofClips++;
                if (!clip.get("type").toString().equalsIgnoreCase("clip")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown item type");
                }
                final String title = (String) clip.get("title");
                final String version = (String) clip.get("version");
                final DownloadLink link = new DownloadLink(null, null, this.getHost(), createContentURL(clip.get("id").toString()), true);
                link.setAvailable(true);
                link.setProperty(PluralsightCom.PROPERTY_CLIP_ID, clip.get("id").toString());
                if (version != null) {
                    link.setProperty(PluralsightCom.PROPERTY_CLIP_VERSION, version);
                }
                link.setProperty(PluralsightCom.PROPERTY_DURATION, clip.get("duration"));
                link.setProperty(PluralsightCom.PROPERTY_MODULE_ORDER_ID, moduleIndex + 1);
                String fullName = String.format("%02d", moduleIndex + 1) + "-" + String.format("%02d", clipIndex + 1) + " - " + moduleTitle + " -- " + title;
                fullName = PluralsightCom.correctFileName(fullName);
                link.setFinalFileName(fullName + ".mp4");
                link.setProperty(PluralsightCom.PROPERTY_TYPE, "mp4");
                link.setAvailable(true);
                ret.add(link);
                clipIndex++;
            }
            moduleIndex++;
        }
        logger.info("Total number of clips found: " + totalNumberofClips);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(PluralsightCom.correctFileName(courseTitle));
        fp.addLinks(ret);
        ret.addAll(ret);
        return ret;
    }

    /** 2021-07-20: Deprecated because the API used here doesn't return all clips - some in between are just randomly missing! */
    @Deprecated
    private ArrayList<DownloadLink> oldHandling(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final UrlQuery query = new UrlQuery().parse(param.getCryptedUrl());
        String courseSlug = query.get("course");
        if (StringUtils.isEmpty(courseSlug)) {
            /* Slug or ID (course-hash) is allowed. */
            courseSlug = new Regex(param.getCryptedUrl(), "/courses/([^/]+)").getMatch(0);
        }
        if (StringUtils.isEmpty(courseSlug)) {
            /* Unsupported URL or offline content or broken plugin */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Failed to find courseSlug");
        }
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        if (account != null) {
            PluralsightCom.login(account, br, this, false);
        }
        PluralsightCom.getClips(br, this, courseSlug);
        if (br.getHttpConnection().getResponseCode() != 200 || br.containsHTML("You have reached the end of the internet")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getRequest().getHttpConnection().getResponseCode() != 200) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Course or Data not found");
        }
        final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Map<String, Object> map = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "data/rpc/bootstrapPlayer");
        final Object courseO = map.get("course");
        if (courseO == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> course = (Map<String, Object>) courseO;
        final boolean supportsWideScreenVideoFormats = Boolean.TRUE.equals(course.get("supportsWideScreenVideoFormats"));
        final List<Map<String, Object>> modules = (List<Map<String, Object>>) course.get("modules");
        if (modules == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (modules.size() == 0) {
            return null;
        }
        final String courseTitle = (String) course.get("title");
        int moduleIndex = 0;
        int totalNumberofClips = 0;
        for (final Map<String, Object> module : modules) {
            final int moduleOrderID = moduleIndex++;
            final List<Map<String, Object>> clips = (List<Map<String, Object>>) module.get("clips");
            if (clips != null) {
                for (final Map<String, Object> clip : clips) {
                    totalNumberofClips += 1;
                    /*
                     * 2021-07-21: The code below would generate their old contentURLs. These are still working in browser but not used
                     * anymore.
                     */
                    // String playerUrl = (String) clip.get("playerUrl");
                    // if (StringUtils.isEmpty(playerUrl)) {
                    // final String id[] = new Regex((String) clip.get("id"), "(.*?):(.*?):(\\d+):(.+)").getRow(0);
                    // playerUrl = "https://app.pluralsight.com/player?name=" + id[1] + "&mode=live&clip=" + id[2] + "&course=" + id[0] +
                    // "&author=" + id[3];
                    // }
                    // final String url = br.getURL(playerUrl).toString();
                    final String clipId = (String) clip.get("clipId");
                    final DownloadLink link = new DownloadLink(null, null, this.getHost(), createContentURL(clipId), true);
                    link.setProperty(PluralsightCom.PROPERTY_DURATION, clip.get("duration").toString());
                    link.setProperty(PluralsightCom.PROPERTY_CLIP_ID, clipId);
                    link.setLinkID(this.getHost() + "://" + clipId);
                    final String title = (String) clip.get("title");
                    final String moduleTitle = (String) clip.get("moduleTitle");
                    Object ordering = clip.get("ordering");
                    if (ordering == null) {
                        ordering = clip.get("index");
                    }
                    link.setProperty(PluralsightCom.PROPERTY_ORDERING, ordering);
                    link.setProperty(PluralsightCom.PROPERTY_SUPPORTS_WIDESCREEN_FORMATS, supportsWideScreenVideoFormats);
                    link.setProperty(PluralsightCom.PROPERTY_MODULE_ORDER_ID, moduleOrderID);
                    if (StringUtils.isNotEmpty(title) && StringUtils.isNotEmpty(moduleTitle) && ordering != null) {
                        String fullName = String.format("%02d", moduleIndex) + "-" + String.format("%02d", Long.parseLong(ordering.toString()) + 1) + " - " + moduleTitle + " -- " + title;
                        fullName = PluralsightCom.correctFileName(fullName);
                        link.setFinalFileName(fullName + ".mp4");
                    }
                    link.setProperty(PluralsightCom.PROPERTY_TYPE, "mp4");
                    link.setAvailable(true);
                    ret.add(link);
                }
            }
        }
        logger.info("Total number of clips found: " + totalNumberofClips);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(PluralsightCom.correctFileName(courseTitle));
        fp.addLinks(ret);
        ret.addAll(ret);
        /** 2021-07-21: Removed this plugin setting - we want to save HTTP requests anyways */
        // if (!PluginJsonConfig.get(PluralsightComConfig.class).isFastLinkCheckEnabled()) {
        // final Browser brc = br.cloneBrowser();
        // String forced_resolution = null;
        // for (final DownloadLink clip : ret) {
        // /*
        // * Set found resolution for first clip on all videos so we can save API requests. Assume that all videos of one course are
        // * available in the same quality.
        // */
        // if (forced_resolution != null) {
        // clip.setProperty(PluralsightCom.PROPERTY_FORCED_RESOLUTION, forced_resolution);
        // }
        // if (clip.getKnownDownloadSize() < 0) {
        // final String streamURL = PluralsightCom.getStreamURL(br, this, clip, null);
        // if (streamURL != null) {
        // final Request checkStream = PluralsightCom.getRequest(brc, this, brc.createHeadRequest(streamURL));
        // final URLConnectionAdapter con = checkStream.getHttpConnection();
        // try {
        // if (con.getResponseCode() == 200 && !StringUtils.containsIgnoreCase(con.getContentType(), "text") &&
        // con.getCompleteContentLength() > 0) {
        // clip.setVerifiedFileSize(con.getCompleteContentLength());
        // clip.setAvailable(true);
        // } else {
        // clip.setAvailableStatus(AvailableStatus.UNCHECKED);
        // }
        // } finally {
        // con.disconnect();
        // }
        // } else {
        // clip.setAvailableStatus(AvailableStatus.UNCHECKED);
        // }
        // if (forced_resolution == null) {
        // forced_resolution = clip.getStringProperty(PluralsightCom.PROPERTY_FORCED_RESOLUTION);
        // }
        // }
        // distribute(clip);
        // if (this.isAbort()) {
        // break;
        // }
        // }
        // }
        // TODO: add subtitles here, for each video add additional DownloadLink that represents subtitle, eg
        // link.setProperty("type", "srt");
        return ret;
    }

    private static String createContentURL(final String clipID) {
        return "https://app.pluralsight.com/course-player?clipId=" + clipID;
    }
}
