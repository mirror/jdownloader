package jd.plugins.decrypter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

/**
 * A plugin for downloading JPG galleries from plain HTML of configured sites. Single galleries are supported, but also all galleries for a
 * specific model. Each gallery will be put into an own folder with name like the "title" tag of the single gallery, with a best-effort
 * unique name. Image names will be like "image_01", "image_02" and so on.
 *
 * Please note: right now, if a model has multiple pages worth of galleries, paging must be done manually.
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@Deprecated
public class SimpleHtmlBasedGalleryPlugin extends PluginForDecrypt {
    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    protected static final String HTTPS_WWW_REGEX_PREFIX = "https?://(?:www\\.)?";

    protected static class SiteData {
        // these 2 are mandatory
        public final String[] host;
        private final String  galleryUrlRegexSuffix;
        // if only single galleries are supported for that host, leave those 2 as "null". otherwise both are mandatory
        private final String  galleriesUrlRegexSuffix;
        private final String  galleryHrefRegex;
        // access those only via their respective methods
        private Pattern       _galleryUrlPattern;
        private Pattern       _galleriesUrlPattern;

        @Deprecated
        protected SiteData(String host, String galleryUrlRegexSuffix, String galleriesRegexSuffix, String galleryHrefRegex) {
            this(new String[] { host }, galleryUrlRegexSuffix, galleriesRegexSuffix, galleryHrefRegex);
        }

        protected SiteData(String host[], String galleryUrlRegexSuffix, String galleriesRegexSuffix, String galleryHrefRegex) {
            this.host = host;
            this.galleryUrlRegexSuffix = galleryUrlRegexSuffix;
            this.galleriesUrlRegexSuffix = galleriesRegexSuffix;
            this.galleryHrefRegex = galleryHrefRegex;
        }

        private String getHostPattern() {
            if (host.length == 1) {
                return host[0].replace(".", "\\.");
            } else {
                final StringBuilder sb = new StringBuilder();
                sb.append("(");
                for (String entry : host) {
                    if (sb.length() > 1) {
                        sb.append("|");
                    }
                    sb.append(entry.replace(".", "\\."));
                }
                sb.append(")");
                return sb.toString();
            }
        }

        protected String getUrlRegex() {
            String uri = galleryUrlRegexSuffix;
            if (StringUtils.isNotEmpty(galleriesUrlRegexSuffix)) {
                uri = "(" + uri + "|" + galleriesUrlRegexSuffix + ")";
            }
            return HTTPS_WWW_REGEX_PREFIX + getHostPattern() + uri;
        }

        protected synchronized Pattern getGalleryUrlPattern() {
            if (_galleryUrlPattern == null) {
                _galleryUrlPattern = Pattern.compile(HTTPS_WWW_REGEX_PREFIX + getHostPattern() + galleryUrlRegexSuffix, Pattern.CASE_INSENSITIVE);
            }
            return _galleryUrlPattern;
        }

        protected synchronized Pattern getGalleriesUrlPattern() {
            if (StringUtils.isEmpty(galleriesUrlRegexSuffix)) {
                return null;
            }
            if (_galleriesUrlPattern == null) {
                _galleriesUrlPattern = Pattern.compile(HTTPS_WWW_REGEX_PREFIX + getHostPattern() + galleriesUrlRegexSuffix, Pattern.CASE_INSENSITIVE);
            }
            return _galleriesUrlPattern;
        }
    }

    private enum Type {
        GALLERY,
        GALLERIES
    }

    private static class SiteAndType {
        private final SiteData data;
        private final Type     type;

        private SiteAndType(SiteData data, Type type) {
            this.data = data;
            this.type = type;
        }
    }

    private static final List<SiteData> SITE_DATA = new ArrayList<SiteData>();
    static {
        // only single gallery
        SITE_DATA.add(new SiteData(new String[] { "coedcherry.com" }, "./*pics/[^/]+", null, null));
        SITE_DATA.add(new SiteData(new String[] { "erocurves.com" }, "/.+", null, null));
        SITE_DATA.add(new SiteData(new String[] { "xxxporn.pics" }, "/sex/(?!\\d+).+", null, null));
        SITE_DATA.add(new SiteData(new String[] { "fapcat.com" }, "/albums/\\d+/.+", null, null));
        // single gallery and all per model
        SITE_DATA.add(new SiteData(new String[] { "babesource.com" }, "/galleries/[^/]+-\\d+\\.html", "/pornstars/.+", "[^\"']+babesource\\.com/galleries[^\"']+"));
        SITE_DATA.add(new SiteData(new String[] { "sexygirlspics.com" }, "/pics/.+", "/\\?q=[^&]+&log-model=1", "[^\"']+sexygirlspics\\.com/pics[^\"']+"));
        SITE_DATA.add(new SiteData(new String[] { "pichunter.com" }, "/gallery/.+", "/models/.+", "/gallery/[^\"']+"));
        SITE_DATA.add(new SiteData(new String[] { "nastypornpics.com" }, "/pics/.+", "/\\?q=[^&]+&log-model=1", "[^\"']+nastypornpics\\.com/pics[^\"']+"));
        SITE_DATA.add(new SiteData(new String[] { "viewgals.com" }, "/pics/.+", "/\\?q=[^&]+&log-model=1", "[^\"']+viewgals\\.com/pics[^\"']+"));
        SITE_DATA.add(new SiteData(new String[] { "sexhd.pics" }, "/gallery/[^/]+/[^/]+/.+", "/gallery/[^/]+/?$", "/gallery/[^/\"']+/[^/\"']+/[^/\"']+/?"));
        SITE_DATA.add(new SiteData(new String[] { "hqsluts.com" }, "/[^/]+-\\d+", "/sluts/.+", "/[^/\"']+-\\d+/"));
    }

    public SimpleHtmlBasedGalleryPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static String[] getAnnotationNames() {
        final List<String> ret = new ArrayList<String>();
        for (SiteData siteData : SITE_DATA) {
            ret.add(siteData.host[0]);
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String[] siteSupportedNames() {
        final String host = getHost();
        for (SiteData siteData : SITE_DATA) {
            if (StringUtils.equals(host, siteData.host[0])) {
                return siteData.host;
            }
        }
        return new String[] { host };
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (SiteData siteData : SITE_DATA) {
            ret.add(siteData.getUrlRegex());
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> allImageLinks = new ArrayList<DownloadLink>();
        final String url = cryptedLink.toString();
        SiteAndType siteAndType = determineSiteAndType(url);
        if (siteAndType.type == Type.GALLERY) {
            crawlGallery(allImageLinks, url);
        } else if (siteAndType.type == Type.GALLERIES) {
            crawlGalleries(allImageLinks, url, siteAndType.data.galleryHrefRegex);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "unsupported site type " + siteAndType.type);
        }
        return allImageLinks;
    }

    private void crawlGallery(ArrayList<DownloadLink> allImageLinks, String url) throws PluginException, IOException {
        Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        /* First check for direct downloadable content */
        final GetRequest getRequest = br.createGetRequest(url);
        final URLConnectionAdapter con = brc.openRequestConnection(getRequest);
        if (this.looksLikeDownloadableContent(con)) {
            final DownloadLink direct = getCrawler().createDirectHTTPDownloadLink(getRequest, con);
            try {
                allImageLinks.add(direct.getDownloadLink());
            } finally {
                con.disconnect();
            }
            return;
        }
        brc.followConnection();
        if (brc.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> galleryImageLinks = new ArrayList<DownloadLink>();
        populateGalleryImageLinks(galleryImageLinks, brc);
        final String title = getFilePackageName(url, brc);
        if (title != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(galleryImageLinks);
        }
        allImageLinks.addAll(galleryImageLinks);
    }

    private void crawlGalleries(ArrayList<DownloadLink> allImageLinks, String url, String galleryHrefRegex) throws PluginException, IOException {
        br.setFollowRedirects(true);
        br.getPage(url);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String[] galleryUrls = getGalleryUrls(galleryHrefRegex);
        if (galleryUrls == null || galleryUrls.length == 0) {
            // do not throw PluginException(LinkStatus.ERROR_PLUGIN_DEFECT), see e.g. https://svn.jdownloader.org/issues/88913
            logger.warning("found 0 galleries for " + url);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        for (final String galleryUrl : galleryUrls) {
            if (isAbort()) {
                break;
            }
            crawlGallery(allImageLinks, galleryUrl);
        }
    }

    private String[] getGalleryUrls(String galleryHrefRegex) throws PluginException, IOException {
        if (StringUtils.isEmpty(galleryHrefRegex)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "no gallery href regex configured for " + br.getHost());
        }
        ArrayList<String> galleryUrls = new ArrayList<String>(getCurrentGalleryUrls(galleryHrefRegex));
        while (fetchMoreGalleries()) {
            if (isAbort()) {
                break;
            }
            galleryUrls.addAll(getCurrentGalleryUrls(galleryHrefRegex));
        }
        return galleryUrls.toArray(new String[0]);
    }

    // if there is another "page" of galleries available, fetch them, or navigate to that page and return true, so that
    // getCurrentGalleryUrls()
    // can parse them. otherwise, return false.
    protected boolean fetchMoreGalleries() throws IOException {
        // right now, no generic support for auto-paging of galleries
        return false;
    }

    protected ArrayList<String> getCurrentGalleryUrls(String galleryHrefRegex) throws PluginException {
        ArrayList<String> galleryUrls = new ArrayList<String>();
        String[][] matches = br.getRegex("href\\s*=\\s*(?:\"|')(" + galleryHrefRegex + ")(?:\"|')").getMatches();
        if (matches.length == 0) {
            return galleryUrls;
        }
        for (String[] match : matches) {
            try {
                String url = match[0];
                if (StringUtils.isEmpty(url)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "no gallery match found");
                }
                galleryUrls.add(br.getURL(url).toString());
            } catch (IOException e) {
                // TODO e.g. retry instead, in order to not loose the already found links
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
            }
        }
        return galleryUrls;
    }

    protected List<SiteData> getSiteData() {
        return SITE_DATA;
    }

    private SiteAndType determineSiteAndType(String url) throws PluginException {
        List<SiteData> siteData = getSiteData();
        for (SiteData sd : siteData) {
            final Pattern galleryUrlPattern = sd.getGalleryUrlPattern();
            if (galleryUrlPattern != null) {
                if (new Regex(url, galleryUrlPattern).matches()) {
                    return new SiteAndType(sd, Type.GALLERY);
                }
            }
            final Pattern galleriesUrlPattern = sd.getGalleriesUrlPattern();
            if (galleriesUrlPattern != null) {
                if (new Regex(url, galleriesUrlPattern).matches()) {
                    return new SiteAndType(sd, Type.GALLERIES);
                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "could not determine site data of " + url);
    }

    private void populateGalleryImageLinks(ArrayList<DownloadLink> imageLinks, Browser brc) throws PluginException, IOException {
        final String[] imageUrls = determineImageUrls(brc);
        if (imageUrls == null || imageUrls.length == 0) {
            // do not throw PluginException(LinkStatus.ERROR_PLUGIN_DEFECT), see e.g. https://svn.jdownloader.org/issues/88913
            logger.warning("found 0 images for " + brc.getURL());
            imageLinks.add(this.createOfflinelink(brc.getURL()));
        } else {
            final int padLength = (int) Math.log10(imageUrls.length) + 1;
            int index = 1;
            for (String imageUrl : imageUrls) {
                imageLinks.add(buildImageDownloadLink(padLength, index++, imageUrl));
            }
        }
    }

    private String[] determineImageUrls(Browser brc) throws PluginException {
        final String[] rawImageUrls = getRawImageUrls(brc);
        if (rawImageUrls == null || rawImageUrls.length == 0) {
            return rawImageUrls;
        } else {
            // in case the link is relative to the host, make it absolute
            final List<String> imageUrls = new ArrayList<String>();
            for (String rawImageUrl : rawImageUrls) {
                try {
                    final String url = brc.getURL(rawImageUrl).toString();
                    if (!imageUrls.contains(url)) {
                        imageUrls.add(url);
                    }
                } catch (IOException e) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
                }
            }
            return imageUrls.toArray(new String[0]);
        }
    }

    private String[] getRawImageUrls(Browser brc) {
        String[] rawlinks = brc.getRegex("href\\s*=\\s*(?:\"|')([^\"']+\\.jpg/?)(\"|')").getColumn(0);
        return rawlinks;
    }

    private DownloadLink buildImageDownloadLink(int padLength, int index, String imageUrl) throws IOException {
        final URL url = new URL(imageUrl);
        final DownloadLink dl;
        if (url.getPath().matches(".*\\.(jpg)$")) {
            dl = createDownloadlink(imageUrl);
        } else {
            dl = createDownloadlink("directhttp://" + imageUrl);
        }
        dl.setAvailable(true);
        dl.setFinalFileName(buildImageFileName(padLength, index));
        return dl;
    }

    private String buildImageFileName(int padLength, int index) {
        return "image_" + String.format(Locale.US, "%0" + padLength + "d", index) + ".jpg";
    }

    private String getFilePackageName(String url, Browser brc) {
        String title = brc.getRegex("<title>\\s*([^<>]+?)\\s*</title>").getMatch(0);
        if (StringUtils.isNotEmpty(title)) {
            String id = new Regex(url, "(\\d+)").getMatch(0);
            if (StringUtils.isNotEmpty(id) && !title.contains(id)) {
                title = title + " " + id;
            }
        }
        // TODO what if title is "null" or empty?
        return title != null ? Encoding.htmlDecode(title.trim()) : null;
    }
}
