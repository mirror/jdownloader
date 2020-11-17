package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.StringUtils;

import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bestporncomix.com" }, urls = { "https?://(?:www\\.)?(?:porncomix\\.info|bestporncomix\\.com)/gallery/([a-z0-9\\-]+)/?" })
public class PornComixInfo extends PluginForDecrypt {
    /** 2020-11-16: bestporncomix.com got routing issues in germany. Use a US VPN to make the website load faster/load at all. */
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String addedurl = param.getCryptedUrl();
        /* 2020-11-13: Main domain has changed from porncomix.info --> bestporncomix.com */
        addedurl = addedurl.replace(Browser.getHost(addedurl) + "/", this.getHost() + "/");
        br.getPage(addedurl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        final String urltitle = new Regex(addedurl, this.getSupportedLinks()).getMatch(0);
        String postTitle = br.getRegex("<title>([^<>\"]+) - \\| 18\\+ Porn Comics</title>").getMatch(0);
        if (StringUtils.isEmpty(postTitle)) {
            /* Fallback */
            postTitle = urltitle.replace("-", " ");
        }
        String[] images = br.getRegex("<li><a href=\"(https?://[^/]+/content/[^<>\"]+)").getColumn(0);
        if (images != null) {
            for (final String imageurl : images) {
                final DownloadLink link = createDownloadlink(imageurl);
                link.setAvailable(true);
                link.setContainerUrl(addedurl);
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
