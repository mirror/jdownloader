package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.MagentamusikDeConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "magentamusik.de" }, urls = { "https?://(?:www\\.)?(?:magenta-musik-360|magentamusik)\\.de/.+" })
public class MagentamusikDe extends PluginForDecrypt {
    public MagentamusikDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final Regex patternIDInsideURL = new Regex(param.getCryptedUrl(), "https?://[^/]+/(?:[^/]+/)?[a-zA-Z0-9\\-%_]+-(\\d{10,})(?:-\\d+)?$");
        final String playerVersion = "58935";
        String assetdetailsVersion = null;
        String itemID;
        if (patternIDInsideURL.matches()) {
            /* Required information is given inside URL */
            itemID = patternIDInsideURL.getMatch(0);
        } else {
            /* Required information is not given inside URL and needs to be crawled from html. */
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            assetdetailsVersion = br.getRegex("odplayer%2Fv3%2Fseriesdetails%2F(\\d+)%2F\\d+").getMatch(0);
            itemID = br.getRegex("class=\"dtag-web-player o-video__player\"[^>]*data-asset-id=\"(\\d+)\"").getMatch(0);
            if (itemID == null) {
                /* 2023-02-22 */
                itemID = br.getRegex("\"assetId\"\\s*:\\s*\"DMM_MOVIE_(\\d+)\"").getMatch(0);
            }
            if (itemID == null) {
                /* Unsupported/offline URL */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (assetdetailsVersion == null) {
            /* Fallback */
            assetdetailsVersion = "58938";
        }
        final String referer = "https://wcps.t-online.de/cvss/magentamusic/vodclient/v2/assetdetails/" + assetdetailsVersion + "/DMM_MOVIE_" + itemID;
        // Similar/old request: https://wcps.t-online.de/cvss/magentamusic/vodplayer/v3/details/<assetdetailsVersion>/<itemID>
        br.getPage("https://wcps.t-online.de/cvss/magentamusic/vodclient/v2/player/" + playerVersion + "/" + itemID + "/Main%20Movie?referrer=" + Encoding.urlEncode(referer));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> map = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final Map<String, Object> content = (Map<String, Object>) map.get("content");
        final Map<String, Object> feature = (Map<String, Object>) content.get("feature");
        final Map<String, Object> metadata = (Map<String, Object>) feature.get("metadata");
        final String title = (String) metadata.get("title");
        final String originalTitle = (String) metadata.get("originalTitle");
        final List<Map<String, Object>> representations = (List<Map<String, Object>>) feature.get("representations");
        int counter = 1;
        final List<Map<String, Object>> representationsVRSkipped = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> representationsSelected = new ArrayList<Map<String, Object>>();
        /* Collect what we want */
        for (final Map<String, Object> representation : representations) {
            logger.info("Crawling representation " + counter + "/" + representations.size());
            final String type = (String) representation.get("type");
            if (!StringUtils.equalsIgnoreCase("HlsStreaming", type)) {
                logger.info("Skipping invalid streaming type: " + type);
            }
            final String quality = (String) representation.get("quality");
            if (StringUtils.equalsIgnoreCase("VR", quality) && !PluginJsonConfig.get(MagentamusikDeConfig.class).isCrawlVR()) {
                representationsVRSkipped.add(representation);
                continue;
            } else {
                representationsSelected.add(representation);
            }
        }
        if (representationsSelected.isEmpty() && representationsVRSkipped.size() > 0) {
            logger.info("Fallback: Video is only available as VR version but user disabled VR crawling -> Crawling VR anyways");
            representationsSelected.addAll(representationsVRSkipped);
        }
        /* Process what we want */
        final String titleComplete = title + "-" + originalTitle;
        for (final Map<String, Object> representation : representationsSelected) {
            logger.info("Crawling url " + counter + "/" + representations.size());
            final String href = (String) JavaScriptEngineFactory.walkJson(representation, "contentPackages/{0}/media/href");
            if (href == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String quality = (String) representation.get("quality");
            final Browser br2 = br.cloneBrowser();
            br2.getPage(href);
            /* 2020-08-04: Their HLS master URLs can e.g. contain parameters like commonly "?yospace=true" */
            final String src = Encoding.htmlOnlyDecode(br2.getRegex("src\\s*=\\s*\"(https?://.*?m3u8[^\"]*)\"").getMatch(0));
            if (src == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br2.getPage(src);
            final ArrayList<DownloadLink> found = GenericM3u8Decrypter.parseM3U8(this, href, br2, null, null, titleComplete + "_" + quality);
            ret.addAll(found);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
            } else {
                counter++;
            }
        }
        if (ret.size() > 0) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(titleComplete);
            fp.addLinks(ret);
        }
        return ret;
    }

    @Override
    public Class<? extends MagentamusikDeConfig> getConfigInterface() {
        return MagentamusikDeConfig.class;
    }
}
