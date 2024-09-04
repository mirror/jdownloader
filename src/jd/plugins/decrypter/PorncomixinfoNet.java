package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PorncomixinfoNet extends PluginForDecrypt {
    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "gedecomix.com", "porncomixinfo.com", "porncomixinfo.net" });
        return ret;
    }

    private List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("porncomixinfo.com"); // 2024-09-04
        return deadDomains;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:chapter|porncomic)/([a-z0-9\\-_]+)/?(([a-z0-9\\-_/]+)/?)?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        String contenturl = param.getCryptedUrl();
        for (final String deadDomain : getDeadDomains()) {
            contenturl = contenturl.replace(deadDomain, this.getHost());
        }
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String seriesSlug = urlinfo.getMatch(0);
        final String chapterSlug = urlinfo.getMatch(2);
        if (chapterSlug != null) {
            /* Find all images of a chapter */
            String title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
            if (title != null) {
                title = Encoding.htmlDecode(title).trim();
                title = title.replaceFirst("\\s*- Porn Comics$", "");
            } else {
                /* Fallback */
                title = br._getURL().getPath();
            }
            /* Similar to hentairead.com */
            final String imagesJsonArrayText = br.getRegex("chapter_preloaded_images = (\\[[^\\]]+\\])").getMatch(0);
            if (imagesJsonArrayText != null) {
                /* Old */
                final List<Map<String, Object>> imagesmaps = (List<Map<String, Object>>) restoreFromString(imagesJsonArrayText, TypeRef.OBJECT);
                for (final Map<String, Object> imagesmap : imagesmaps) {
                    final String imageurl = imagesmap.get("src").toString();
                    final DownloadLink link = createDownloadlink(DirectHTTP.createURLForThisPlugin(imageurl));
                    link.setAvailable(true);
                    ret.add(link);
                }
            } else {
                /* New 2023-10-30 */
                final String[] imageurls = br.getRegex("=\"image-\\d+\"\\s*src=\"\\s*(https?://[^\"]+)\"").getColumn(0);
                if (imageurls != null && imageurls.length > 0) {
                    for (String imageurl : imageurls) {
                        imageurl = Encoding.htmlOnlyDecode(imageurl);
                        final DownloadLink link = createDownloadlink(DirectHTTP.createURLForThisPlugin(imageurl));
                        ret.add(link);
                    }
                }
            }
            /* Add cover URLs */
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            final HashSet<String> webpurls = new HashSet<String>();
            for (final String url : urls) {
                if (StringUtils.endsWithCaseInsensitive(url, "cover.jpg") || StringUtils.endsWithCaseInsensitive(url, "cover.webp")) {
                    final DownloadLink link = createDownloadlink(DirectHTTP.createURLForThisPlugin(url));
                    ret.add(link);
                } else if (StringUtils.endsWithCaseInsensitive(url, ".webp")) {
                    webpurls.add(url);
                }
            }
            if (ret.isEmpty() && webpurls.size() > 0) {
                /* Final fallback for cover URLs */
                for (final String webpurl : webpurls) {
                    final DownloadLink link = createDownloadlink(DirectHTTP.createURLForThisPlugin(webpurl));
                    ret.add(link);
                }
            }
            if (ret.isEmpty()) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            /* Some chapters have the same names but they should go into separate packages. */
            fp.setPackageKey("porncomix://chapter_by_url_path/" + br._getURL().getPath());
            for (final DownloadLink result : ret) {
                result.setAvailable(true);
                result._setFilePackage(fp);
            }
        } else if (seriesSlug != null) {
            /* Find all chapters of series */
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            final HashSet<String> dupes = new HashSet<String>();
            for (final String url : urls) {
                final String thisChapterSlug = new Regex(url, this.getSupportedLinks()).getMatch(2);
                final boolean isValidChapterSlug = thisChapterSlug != null && thisChapterSlug.contains("-");
                if (url.contains(seriesSlug) && isValidChapterSlug && dupes.add(url)) {
                    ret.add(this.createDownloadlink(url));
                }
            }
            logger.info("Found chapters: " + ret.size());
            if (ret.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            /* Developer mistake: This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
