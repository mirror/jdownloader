package jd.plugins.decrypter;

import java.net.URLDecoder;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.appwork.utils.encoding.Base64;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "folderwatch" }, urls = { "\\[folderwatch:\\d+\\][0-9a-zA-Z\\+\\/=]+(%3D){0,2}" })
public class FolderWatchURL extends PluginForDecrypt {
    public FolderWatchURL(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public CrawledLink convert(DownloadLink link) {
        final CrawledLink ret = super.convert(link);
        if (getCurrentLink().isCrawlDeep()) {
            ret.setCrawlDeep(true);
        }
        return ret;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        String base64 = new Regex(parameter.getCryptedUrl(), "\\[folderwatch:\\d+\\]([0-9a-zA-Z\\+\\/=]+(%3D){0,2})").getMatch(0);
        if (base64 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            if (base64.contains("%3D")) {
                base64 = URLDecoder.decode(base64, "UTF-8");
            }
            while (true) {
                if (base64.length() % 4 != 0) {
                    base64 += "=";
                } else {
                    break;
                }
            }
            final byte[] decoded = Base64.decode(base64);
            if (decoded != null) {
                String possibleURLs = new String(decoded, "UTF-8");
                if (HTMLParser.getProtocol(possibleURLs) == null) {
                    possibleURLs = URLDecoder.decode(possibleURLs, "UTF-8");
                }
                if (HTMLParser.getProtocol(possibleURLs) != null) {
                    ret.add(createDownloadlink(possibleURLs));
                }
            }
            return ret;
        }
    }
}
