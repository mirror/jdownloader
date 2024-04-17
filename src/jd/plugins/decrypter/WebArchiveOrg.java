package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class WebArchiveOrg extends PluginForDecrypt {
    private static final Pattern PATTERN_DIRECT = Pattern.compile("https?://web\\.archive\\.org/web/(\\d+)(if|im|oe)_/(https?.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_OTHER  = Pattern.compile("https?://web\\.archive\\.org/web/(\\d+)/(https?.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_FILE   = Pattern.compile("^https?://[^/]+/web/(\\d+)[^/]*/(.+)", Pattern.CASE_INSENSITIVE);

    public WebArchiveOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "web.archive.org" });
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
            ret.add("https?://" + buildHostsPatternPart(domains) + "/web/[0-9]+.+");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex regexDirect = new Regex(param.getCryptedUrl(), PATTERN_DIRECT);
        final Regex regexFile = new Regex(param.getCryptedUrl(), PATTERN_FILE);
        if (regexDirect.patternFind()) {
            /* Sure that we got a direct-URL */
            ret.add(createDownloadlink(DirectHTTP.createURLForThisPlugin(param.getCryptedUrl())));
        }
        final String originalURL = new Regex(param.getCryptedUrl(), PATTERN_OTHER).getMatch(1);
        final PluginForDecrypt yt = getNewPluginForDecryptInstance("youtube.com");
        final String youtubeVideoID = yt != null && yt.canHandle(originalURL) ? TbCmV2.getVideoIDFromUrl(param.getCryptedUrl()) : null;
        if (youtubeVideoID != null) {
            /* Look for direct-URLs */
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(false);
            brc.getPage(param.getCryptedUrl());
            brc.getPage("https://web.archive.org/web/2oe_/http://wayback-fakeurl.archive.org/yt/" + Encoding.urlEncode(youtubeVideoID));
            final String directurl = brc.getRedirectLocation();
            if (directurl != null) {
                ret.add(createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl)));
                return ret;
            }
        }

        if (regexFile.patternFind()) {
            /* Unsure if we got a direct-URL -> Check it */
            final String fileID = regexFile.getMatch(0);
            final String linkpart = regexFile.getMatch(1);
            final String url = "https://web.archive.org/web/" + fileID + "if_" + "/" + linkpart;
            /* First check if maybe the user has added a directURL. */
            final GetRequest getRequest = br.createGetRequest(url);
            final URLConnectionAdapter con = this.br.openRequestConnection(getRequest);
            try {
                if (this.looksLikeDownloadableContent(con)) {
                    final DownloadLink direct = getCrawler().createDirectHTTPDownloadLink(getRequest, con);
                    ret.add(direct);
                } else {
                    br.followConnection();
                    if (br.getHttpConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    /* E.g. embedded PDF */
                    final String directurl = br.getRegex("<iframe id=\"playback\"[^>]*src=\"(https?://[^\"]+)").getMatch(0);
                    if (directurl == null) {
                        logger.info("URL is not supported or content is offline");
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        ret.add(this.createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl)));
                    }
                }
            } finally {
                con.disconnect();
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return ret;
        }
    }
}
