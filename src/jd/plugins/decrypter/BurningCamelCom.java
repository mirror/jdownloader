package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "burningcamel.com" }, urls = { "https?://(?:www\\.)?(?:burningcamel\\.com|camelstyle\\.net)/video/[a-z0-9\\-]+(/\\d+)?" })
public class BurningCamelCom extends PornEmbedParser {
    public BurningCamelCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        if (br.getURL().equals("http://www.burningcamel.com/") || br.containsHTML("<title>Amateur Porn and Free Amateur Sex Videos \\| Burning Camel</title>|<title>404: File Not Found") || this.br.getHttpConnection().getResponseCode() == 404) {
            return ret;
        }
        /* First scan for any standard extern embedded URLs. */
        ret = this.findEmbedUrls(null);
        if (!ret.isEmpty()) {
            return ret;
        }
        final String embedded = br.getRegex("class=\"inner-block embed-responsive\">\\s*<iframe\\s*[^<>]*src=\"(https?://.*?)\"").getMatch(0);
        if (embedded != null) {
            ret.add(createDownloadlink(embedded));
        }
        final Regex basicRegex = br.getRegex("createPlayer\\(\"(http://.*?)\",\"http://.*?\",\"(.*?)\"");
        if (basicRegex.getMatch(0) != null || br.getRegex("(https?://burningcamel.com/media/videos/.*?)(\\'|\")").getMatch(0) != null) {
            ret.add(createDownloadlink(parameter.getCryptedUrl()));
        }
        return ret;
    }
}
