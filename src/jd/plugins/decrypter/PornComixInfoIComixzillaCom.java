package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;

import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PornComixInfoIComixzillaCom extends PluginForDecrypt {
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "comixzilla.com", "ilikecomix.io" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:[a-z]{2}/)?comic-g/([a-z0-9\\-]+)/?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String addedurl = param.getCryptedUrl();
        br.getPage(addedurl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        final String urltitle = new Regex(br.getURL(), "/([^/]+)/?$").getMatch(0);
        /* Allow to pickup quotes */
        String postTitle = br.getRegex("itemprop=\"headline\">([^<>\"]+)</h1>").getMatch(0);
        if (postTitle == null) {
            postTitle = br.getRegex("<title>([^<>\"]+) \\| Porn comics</title>").getMatch(0);
        }
        if (StringUtils.isEmpty(postTitle)) {
            /* Fallback */
            postTitle = urltitle.replace("-", " ");
        }
        String[] images = br.getRegex("<li><a href=\"(https?://[^/]+/img/[^<>\"]+)").getColumn(0);
        if (images != null) {
            for (final String imageurl : images) {
                /* 2020-11-13: Not needed anymore */
                // imageurl = Encoding.htmlDecode(imageurl).replaceFirst("(-\\d+x\\d+)\\.(jpe?g|gif|png)$", ".$2");
                final DownloadLink link = createDownloadlink(imageurl);
                link.setAvailable(true);
                link.setContainerUrl(param.getCryptedUrl());
                decryptedLinks.add(link);
            }
        }
        if (postTitle != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(postTitle));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
