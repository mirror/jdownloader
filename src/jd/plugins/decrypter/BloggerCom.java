package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 36558 $", interfaceVersion = 2, names = { "blogger.com" }, urls = { "https?://([a-z0-9\\-]+\\.)?blogger\\.com/video\\.g\\?token=[a-zA-Z0-9\\-_]+" })
public class BloggerCom extends PluginForDecrypt {
    public BloggerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String iframe_id = br.getRegex("\"iframe_id\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        final String[] play_urls = br.getRegex("\"play_url\"\\s*:\\s*\"(https?://.*?)\"").getColumn(0);
        if (play_urls != null) {
            for (final String play_url : play_urls) {
                final DownloadLink downloadLink = createDownloadlink(Encoding.unicodeDecode(play_url));
                if (iframe_id != null) {
                    downloadLink.setName(iframe_id);
                }
                ret.add(downloadLink);
            }
        }
        return ret;
    }
}
