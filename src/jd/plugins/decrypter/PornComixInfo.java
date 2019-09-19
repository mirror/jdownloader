package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porncomix.info " }, urls = { "https?://(?:www\\.)?(?:porncomix\\.info|(?:bestporncomix\\.com|porncomix\\.one)/gallery)/[a-zA-Z0-9\\-_]+/?" })
public class PornComixInfo extends PluginForDecrypt {
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        String postTitle = br.getRegex("class=\"post-title\"\\s*>\\s*(.*?)\\s*</").getMatch(0);
        if (postTitle == null || postTitle.length() == 0) {
            postTitle = br.getRegex("<h1\\s+class\\s*=\\s*\"post-title[^\"]*\"><a[^>]*>\\s*([^<]+)\\s*</a></h1>").getMatch(0);
        }
        if (postTitle != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(postTitle));
            String[] images = br.getRegex("class='gallery-icon\\s*(?:portrait|landscape)'\\s*>\\s*<a.*?href='.*?'\\s*>\\s*<img[^>]+src=\"(https?://(?:www\\.)?\\w+\\.\\w+/.*?(jpe?g|gif|png))\"").getColumn(0);
            if (images == null || images.length == 0) {
                images = br.getRegex("<img[^>]+data-jg-srcset\\s*=\\s*\"([^\",\\s]+)").getColumn(0);
            }
            if (images != null) {
                for (final String image : images) {
                    if (!isAbort()) {
                        final String url = Encoding.htmlDecode(image).replaceFirst("(-\\d+x\\d+)\\.(jpe?g|gif|png)$", ".$2");
                        final DownloadLink link = createDownloadlink(url);
                        link.setAvailable(true);
                        link.setContainerUrl(parameter.getCryptedUrl());
                        fp.add(link);
                        distribute(link);
                    }
                }
            }
        }
        return decryptedLinks;
    }
}
