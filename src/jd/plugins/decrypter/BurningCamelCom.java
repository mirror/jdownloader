package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "burningcamel.com" }, urls = { "https?://(?:www\\.)?(?:burningcamel\\.com|camelstyle\\.net)/video/([a-z0-9\\-]+(/\\d+)?)" })
public class BurningCamelCom extends PornEmbedParser {
    public BurningCamelCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "burningcamel.com", "camelstyle.net" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/video/([a-z0-9\\-]+(/\\d+)?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String fid = new Regex(parameter.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.getPage(getContentURL(fid));
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* First scan for any standard extern embedded URLs. */
        ret = this.findEmbedUrls(null);
        if (!ret.isEmpty()) {
            return ret;
        }
        final String embedded = br.getRegex("class=\"inner-block embed-responsive\">\\s*<iframe\\s*[^<>]*src=\"(https?://.*?)\"").getMatch(0);
        if (embedded != null) {
            ret.add(createDownloadlink(embedded));
        }
        final Regex basicRegex = br.getRegex("createPlayer\\(\"(http://.*?)\",\"http://.*?\",\"(.*?)\"");
        if (basicRegex.getMatch(0) != null || br.getRegex("(https?://burningcamel.com/media/videos/.*?)(\\'|\")").getMatch(0) != null) {
            ret.add(createDownloadlink(parameter.getCryptedUrl()));
        }
        return ret;
    }

    public static final String getContentURL(final String fid) {
        return "https://" + getPluginDomains().get(0)[0] + "/video/" + fid;
    }
}
