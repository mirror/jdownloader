package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.MagentaMusik360Config;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "magenta-musik-360.de" }, urls = { "https?://(?:www\\.)?magenta-musik-360\\.de/.+" })
public class MagentaMusik360 extends PluginForDecrypt {
    public MagentaMusik360(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends MagentaMusik360Config> getConfigInterface() {
        return MagentaMusik360Config.class;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        if (parameter.getCryptedUrl().matches("https?://(?:www\\.)?magenta-musik-360\\.de/(?:[^/]+/)?[a-zA-Z0-9\\-%_]+-\\d{10,}(?:-\\d+)?")) {
            /* Type1 */
            final String itemId = new Regex(parameter.getCryptedUrl(), "-(\\d+)(-\\d+)?$").getMatch(0);
            final String playerVersion = "58935";
            br.getPage("https://wcss.t-online.de/cvss/magentamusic/vodplayer/v3/player/" + playerVersion + "/" + itemId + "/Main%20Movie?referrer=https%3A%2F%2Fwcss.t-online.de%2Fcvss%2Fmagentamusic%2Fvodplayer%2Fv3%2Fdetails%2F" + playerVersion + "%2F" + itemId + "%3F%24whiteLabelId%3DMM3&%24whiteLabelId=MM3&_c=aHR0cHM6d3d3Lm1hZ2VudGEtbXVzaWstMzYwLmRl");
            if (br.getHttpConnection().getResponseCode() == 404) {
                ret.add(this.createOfflinelink(parameter.toString()));
                return ret;
            }
            final Map<String, Object> map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final Map<String, Object> content = (Map<String, Object>) map.get("content");
            final Map<String, Object> feature = (Map<String, Object>) content.get("feature");
            final Map<String, Object> metadata = (Map<String, Object>) feature.get("metadata");
            final String title = (String) metadata.get("title");
            final String originalTitle = (String) metadata.get("originalTitle");
            final List<Map<String, Object>> representations = (List<Map<String, Object>>) feature.get("representations");
            for (final Map<String, Object> representation : representations) {
                final String type = (String) representation.get("type");
                if (StringUtils.equalsIgnoreCase("HlsStreaming", type)) {
                    final String quality = (String) representation.get("quality");
                    if (StringUtils.equalsIgnoreCase("VR", quality) && !PluginJsonConfig.get(MagentaMusik360Config.class).isCrawlVR()) {
                        continue;
                    }
                    final String href = (String) JavaScriptEngineFactory.walkJson(representation, "contentPackages/{0}/media/href");
                    if (href == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final Browser br2 = br.cloneBrowser();
                    br2.getPage(href);
                    /* 2020-08-04: Their HLS master URLs can e.g. contain parameters like commonly "?yospace=true" */
                    final String src = Encoding.htmlOnlyDecode(br2.getRegex("src\\s*=\\s*\"(https?://.*?m3u8[^\"]*)\"").getMatch(0));
                    if (src == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br2.getPage(src);
                    final ArrayList<DownloadLink> found = GenericM3u8Decrypter.parseM3U8(this, href, br2, null, null, null, title + "-" + originalTitle + "_" + quality);
                    ret.addAll(found);
                }
            }
            if (ret.size() > 0) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(title + "-" + originalTitle);
                fp.addLinks(ret);
            }
        } else {
            /* Type2 */
            br.getPage(parameter.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                ret.add(this.createOfflinelink(parameter.toString()));
                return ret;
            }
            final String seriesID = br.getRegex("odplayer%2Fv3%2Fseriesdetails%2F(\\d+)%2F\\d+").getMatch(0);
            final String assetID = br.getRegex("class=\"dtag-web-player o-video__player\"[^>]*data-asset-id=\"(\\d+)\"").getMatch(0);
            if (seriesID == null || assetID == null) {
                ret.add(this.createOfflinelink(parameter.toString()));
                return ret;
            }
            br.getPage(String.format("https://wcps.t-online.de/cvss/magentamusic/vodplayer/v3/details/%s/%s", seriesID, assetID));
            final Map<String, Object> videoInfo = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final String playerURL = (String) JavaScriptEngineFactory.walkJson(videoInfo, "content/movie/features/{0}/player/href");
            if (StringUtils.isEmpty(playerURL)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(playerURL);
            final Map<String, Object> streamingInfo = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final String streamingXMLUrl = (String) JavaScriptEngineFactory.walkJson(streamingInfo, "content/feature/representations/{0}/contentPackages/{0}/media/href");
            if (StringUtils.isEmpty(streamingXMLUrl)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(streamingXMLUrl);
            final String hls_master = br.getRegex("\"(https?://[^<>\"]+\\.m3u8[^\"]*)\"").getMatch(0);
            if (hls_master == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ret.add(this.createDownloadlink(hls_master));
        }
        return ret;
    }
}
