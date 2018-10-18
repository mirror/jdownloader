package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "indianpornvideos.com", "freesexyindians.com" }, urls = { "https?://(?:www\\.)?indianpornvideos\\.com/(video/)?[A-Za-z0-9\\-_]+(?:\\.html)?", "https?://(?:www\\.)?freesexyindians\\.com/porn-star/.+" })
public class IndianpornvideosCom extends PornEmbedParser {
    public IndianpornvideosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        /* First scan for any standard extern embedded URLs. */
        ret = this.findEmbedUrls(null);
        if (!ret.isEmpty()) {
            return ret;
        }
        ret.add(createDownloadlink(parameter.getCryptedUrl()));
        return ret;
    }
}
