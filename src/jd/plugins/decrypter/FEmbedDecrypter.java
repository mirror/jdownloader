package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.FEmbedComConfig;
import org.jdownloader.plugins.components.config.FEmbedComConfig.Quality;
import org.jdownloader.plugins.components.config.FEmbedComConfig.QualitySelectionMode;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FEmbedDecrypter extends PluginForDecrypt {
    public FEmbedDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        /* Always add current domain to first position! */
        ret.add(new String[] { "fembed.com", "there.to", "gcloud.live", "plycdn.xyz", "hlsmp4.com", "svpri.xyz", "asianclub.nl", "asianclub.tv", "javcl.me", "javjav.top", "feurl.com", "zidiplay.com", "embed.media", "javideo.pw", "playvideo.best", "ffem.club", "dunbed.xyz", "embed.casa", "sexhd.co", "fileone.tv", "luxubu.review", "anime789.com", "femax20.com", "smartshare.tv", "cercafilm.net", "watchjavnow.xyz", "layarkacaxxi.icu", "mycloudzz.com", "purefiles.in", "rapidplay.org", "2tazhfx9vrx4jnvaxt87sknw5eqbd6as.club", "av-th.info", "embedsito.com", "vanfem.com" });
        /* 2021-11-16 */
        ret.add(new String[] { "watch-jav-english.live", "dutrag.com", "nekolink.site", "pornhole.club", "javip.pro", "javlove.club", "javguru.tk", "diasfem.com", "cloudrls.com", "embedaio.com", "fakyutube.com", "javhdfree.icu", "fembed-hd.com", "suzihaza.com", "mambast.tk", "javpoll.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:f|v)/([a-zA-Z0-9_-]+)(#javclName=[a-fA-F0-9]+)?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public void init() {
        super.init();
        /* 2021-09-06: Without this we'll run into their rate-limit very fast! */
        for (final String[] domainlist : getPluginDomains()) {
            for (final String domain : domainlist) {
                setRequestLimit(domain);
            }
        }
    }

    public static void setRequestLimit(final String domain) {
        Browser.setRequestIntervalLimitGlobal(domain, true, 8000);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final boolean isDownloadPlugin = !(Thread.currentThread() instanceof LinkCrawlerThread);
        final String file_id = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        String name = param.getDownloadLink() != null ? param.getDownloadLink().getStringProperty("javclName", null) : null;
        String fembedHost = null;
        Form dlform = null;
        if (name == null) {
            br.getPage(param.getCryptedUrl());
            dlform = br.getForm(1);
            fembedHost = br.getHost();
            name = br.getRegex("<title>\\s*([^<]*?)\\s*(-\\s*Free\\s*download)?\\s*</title>").getMatch(0);
            if (br.getURL().contains("/v/")) {
                final Browser brc = br.cloneBrowser();
                brc.getPage("/f/" + file_id);
                final String fileName = brc.getRegex("<title>\\s*([^<]*?)\\s*(-\\s*Free\\s*download)?\\s*</title>").getMatch(0);
                if (fileName != null) {
                    name = fileName;
                }
            }
        }
        if (fembedHost == null) {
            fembedHost = Browser.getHost(param.getCryptedUrl());
        }
        final List<Map<String, Object>> videos = new ArrayList<Map<String, Object>>();
        final String sources[] = br.getRegex("<source\\s*(src.*?)\\s*>").getColumn(0);
        for (String source : sources) {
            String src = new Regex(source, "src\\s*=\\s*([^ ]+)").getMatch(0);
            String type = new Regex(source, "type\\s*=\\s*video/([^ ]+)").getMatch(0);
            String label = new Regex(source, "title\\s*=\\s*\"(\\d+p)").getMatch(0);
            if (StringUtils.isAllNotEmpty(src, type)) {
                final Map<String, Object> video = new HashMap<String, Object>();
                video.put("file", br.getURL(src).toString());
                video.put("type", type);
                if (label == null) {
                    video.put("label", "1080p");
                } else {
                    video.put("label", label);
                }
                videos.add(video);
            }
        }
        Map<String, Object> response = null;
        if (videos.size() == 0 && !"fileone.tv".equals(fembedHost)) {
            final PostRequest postRequest = new PostRequest("https://" + fembedHost + "/api/source/" + file_id);
            br.getPage(postRequest);
            try {
                response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        if (response == null && dlform != null) {
            /* 2020-03-16: Fallback for sites which do not have an API e.g. for fileone.tv */
            br.setFollowRedirects(false);
            br.submitForm(dlform);
            final String dllink = br.getRedirectLocation();
            if (dllink == null) {
                return null;
            } else {
                // TODO: rewrite to use FEmbedCom to support link refresh
                final DownloadLink link = this.createDownloadlink("directhttp://" + dllink);
                if (name != null) {
                    link.setFinalFileName(name);
                }
                ret.add(link);
                return ret;
            }
        }
        if (response != null) {
            if (!Boolean.TRUE.equals(response.get("success"))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                if (response.get("data") instanceof String) {
                    videos.addAll((List<Map<String, Object>>) JSonStorage.restoreFromString((String) response.get("data"), TypeRef.OBJECT));
                } else {
                    videos.addAll((List<Map<String, Object>>) response.get("data"));
                }
            }
        }
        if (videos.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, DownloadLink> foundQualities = new HashMap<String, DownloadLink>();
        DownloadLink best = null;
        int bestQ = -1;
        for (final Map<String, Object> video : videos) {
            final String label = (String) video.get("label");
            final DownloadLink link = createDownloadlink(param.getCryptedUrl().replaceAll("https?://", "decryptedforFEmbedHosterPlugin://") + "#label=" + label);
            final String type = (String) video.get("type");
            String directurl = (String) video.get("file");
            if (directurl != null && directurl.startsWith("/")) {
                directurl = "https://www." + fembedHost + directurl;
            }
            link.setProperty("label", label);
            link.setProperty("fembedid", file_id);
            link.setProperty("fembedHost", fembedHost);
            if (directurl != null) {
                link.setProperty(jd.plugins.hoster.FEmbedCom.PROPERTY_DIRECTURL, directurl);
            }
            if (!StringUtils.isEmpty(name)) {
                link.setFinalFileName(name + "-" + label + "." + type);
            } else {
                /* Fallback */
                link.setName(file_id + "-" + label + "." + type);
            }
            link.setAvailable(true);
            final int quality = Integer.parseInt(label.replaceAll("[^\\d]", ""));
            if (best == null || quality > bestQ) {
                best = link;
                bestQ = quality;
            }
            foundQualities.put(label, link);
        }
        final QualitySelectionMode mode = isDownloadPlugin ? QualitySelectionMode.ALL : PluginJsonConfig.get(FEmbedComConfig.class).getQualitySelectionMode();
        final String preferredQualityString = getUserPreferredqualityStr();
        switch (mode) {
        case ALL:
            for (final Entry<String, DownloadLink> entry : foundQualities.entrySet()) {
                ret.add(entry.getValue());
            }
            break;
        case SELECTED_ONLY:
            if (foundQualities.containsKey(preferredQualityString)) {
                ret.add(foundQualities.get(preferredQualityString));
                break;
            } else {
                /* Fallback to BEST */
            }
        case BEST:
        default:
            /* BEST */
            ret.add(best);
        }
        if (ret.size() > 1 && !isDownloadPlugin) {
            final FilePackage filePackage = FilePackage.getInstance();
            final String title;
            if (!StringUtils.isEmpty(name)) {
                title = name;
            } else {
                title = file_id;
            }
            filePackage.setName(title);
            filePackage.addLinks(ret);
        }
        return ret;
    }

    private String getUserPreferredqualityStr() {
        final Quality quality = PluginJsonConfig.get(FEmbedComConfig.class).getPreferredStreamQuality();
        switch (quality) {
        case Q360:
            return "360p";
        case Q480:
            return "480p";
        case Q720:
            return "720p";
        case Q1080:
            return "1080p";
        default:
            /* E.g. BEST */
            return null;
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return FEmbedComConfig.class;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2021-09-06: They're using heavy rate-limiting. */
        return 1;
    }
}
