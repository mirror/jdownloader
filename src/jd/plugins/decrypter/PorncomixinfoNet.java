package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.controller.LazyPlugin;

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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PorncomixinfoNet extends PluginForDecrypt {
    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = new Browser();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:chapter|porncomic)/[a-z0-9\\-]+/[a-z0-9\\-]+/?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String addedurl = param.getCryptedUrl();
        br.getPage(addedurl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String title = br.getRegex("<title>([^<>\"]+) - Porn Comics</title>").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
        } else {
            /* Fallback */
            title = br._getURL().getPath();
        }
        /* Similar to hentairead.com */
        String imagesText = br.getRegex("chapter_preloaded_images = \\[(.*?)\\]").getMatch(0);
        if (imagesText != null) {
            /* Old */
            imagesText = PluginJSonUtils.unescape(imagesText);
            imagesText = imagesText.replace("\"", "");
            String[] images = imagesText.split(",");
            for (final String imageurl : images) {
                /* 2020-11-13: Not needed anymore */
                // imageurl = Encoding.htmlDecode(imageurl).replaceFirst("(-\\d+x\\d+)\\.(jpe?g|gif|png)$", ".$2");
                final DownloadLink link = createDownloadlink(DirectHTTP.createURLForThisPlugin(imageurl));
                link.setAvailable(true);
                ret.add(link);
            }
        } else {
            /* New 2023-10-30 */
            final String[] imageurls = br.getRegex("=\"image-\\d+\"\\s*src=\"\\s*(https?://[^\"]+)\"").getColumn(0);
            if (imageurls == null || imageurls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String imageurl : imageurls) {
                final DownloadLink link = createDownloadlink(DirectHTTP.createURLForThisPlugin(imageurl));
                link.setAvailable(true);
                ret.add(link);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        return ret;
    }
}
