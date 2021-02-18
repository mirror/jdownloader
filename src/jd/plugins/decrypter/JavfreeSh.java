package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "javfree.sh" }, urls = { "https?://(?:www\\.)?javfree\\.sh/\\d+/[a-z0-9\\-_]+\\.html" })
public class JavfreeSh extends PluginForDecrypt {
    public JavfreeSh(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** 2021-02-18: Formerly known as: javqd.tv */
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(parameter.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            ret.add(this.createOfflinelink(parameter.getCryptedUrl()));
            return ret;
        }
        final String embedID = br.getRegex("player\\.[^/]+/embed\\.html#([a-f0-9]+)\"").getMatch(0);
        if (embedID != null) {
            br.getPage("https://player.javfree.sh/stream/" + embedID);
            final String[] urls = HTMLParser.getHttpLinks(br.toString(), br.getURL());
            for (final String url : urls) {
                ret.add(this.createDownloadlink(url));
            }
        }
        return ret;
    }
}
