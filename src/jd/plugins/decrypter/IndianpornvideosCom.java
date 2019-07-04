package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.hoster.IndianPornVideosCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "indianpornvideos.com", "freesexyindians.com" }, urls = { "https?://(?:www\\.)?indianpornvideos2?\\.com/(video/)?[A-Za-z0-9\\-_]+(?:\\.html)?", "https?://(?:www\\.)?freesexyindians\\.com/porn-star/.+" })
public class IndianpornvideosCom extends PornEmbedParser {
    public IndianpornvideosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        if (IndianPornVideosCom.findStream(br) != null) {
            ret.add(createDownloadlink(parameter.getCryptedUrl()));
            return ret;
        } else {
            /* First scan for any standard extern embedded URLs. */
            final ArrayList<DownloadLink> found = this.findEmbedUrls(null);
            if (!found.isEmpty()) {
                return found;
            } else {
                ret.add(createDownloadlink(parameter.getCryptedUrl()));
                return ret;
            }
        }
    }
}
