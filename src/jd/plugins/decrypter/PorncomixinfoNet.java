package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.StringUtils;

import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porncomixinfo.net" }, urls = { "https?(?:www\\.)?://porncomixinfo\\.net/chapter/[a-z0-9\\-]+/[a-z0-9\\-]+/?" })
public class PorncomixinfoNet extends PluginForDecrypt {
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
        String postTitle = br.getRegex("\"description\":\"(.*?)\",\"").getMatch(0);
        if (StringUtils.isEmpty(postTitle)) {
            /* Fallback */
            postTitle = urltitle;
        }
        String imagesText = br.getRegex("chapter_preloaded_images = \\[(.*?)\\]").getMatch(0);
        imagesText = PluginJSonUtils.unescape(imagesText);
        imagesText = imagesText.replace("\"", "");
        String[] images = imagesText.split(",");
        for (final String imageurl : images) {
            /* 2020-11-13: Not needed anymore */
            // imageurl = Encoding.htmlDecode(imageurl).replaceFirst("(-\\d+x\\d+)\\.(jpe?g|gif|png)$", ".$2");
            final DownloadLink link = createDownloadlink(imageurl);
            link.setAvailable(true);
            link.setContainerUrl(param.getCryptedUrl());
            decryptedLinks.add(link);
        }
        if (postTitle != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(postTitle));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
