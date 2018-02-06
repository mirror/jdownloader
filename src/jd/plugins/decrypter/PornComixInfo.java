package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porncomix.info " }, urls = { "https?://(?:www\\.)?porncomix\\.info/[a-zA-Z0-9\\-]+/" })
public class PornComixInfo extends PluginForDecrypt {
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String postTitle = br.getRegex("class=\"post-title\"\\s*>\\s*(.*?)\\s*</").getMatch(0);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (postTitle != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(postTitle));
            final String[] images = br.getRegex("class='gallery-icon\\s*portrait'\\s*>\\s*<a.*?href='.*?'\\s*>\\s*<img\\s*src=\".*?\"\\s*data-lazy-src=\"(https?://(?:www\\.)?porncomix\\.info/.*?(jpe?g|gif|png))\"").getColumn(0);
            if (images != null) {
                for (final String image : images) {
                    if (!isAbort()) {
                        final String url = image.replaceFirst("(-\\d+x\\d+)\\.(jpe?g|gif|png)$", ".$2");
                        final DownloadLink link = createDownloadlink(url);
                        link.setAvailable(true);
                        fp.add(link);
                        distribute(link);
                    }
                }
            }
        }
        return decryptedLinks;
    }
}
