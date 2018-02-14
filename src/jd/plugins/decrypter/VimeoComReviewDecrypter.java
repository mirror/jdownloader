package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 38667 $", interfaceVersion = 3, names = { "vimeo.com" }, urls = { "https?://(?:www\\.)?vimeo\\.com/user\\d+/review/\\d+/[a-f0-9]+" })
public class VimeoComReviewDecrypter extends PluginForDecrypt {
    public VimeoComReviewDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String videoID = new Regex(param.getCryptedUrl(), "user\\d+/review/(\\d+)").getMatch(0);
        final DownloadLink link = createDownloadlink(VimeoComDecrypter.createPrivateVideoUrlWithReferer(videoID, param.getCryptedUrl()));
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ret.add(link);
        return ret;
    }
}