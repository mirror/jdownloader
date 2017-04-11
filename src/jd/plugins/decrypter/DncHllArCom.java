package jd.plugins.decrypter;

import java.util.ArrayList;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dancehallarena.com" }, urls = { "https?://(\\w*\\.)?dancehallarena\\.com/[a-zA-Z0-9\\-/]+" }) public class DncHllArCom extends antiDDoSForDecrypt {

    public DncHllArCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        getPage(parameter);
        // invalid url
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            return decryptedLinks;
        }
        // all external links pass via there own tracking url
        String[] links = br.getRegex("xurl=(http[^\"]+|://[^\"]+|%3A%2F%2F[^\"]+)").getColumn(0);
        if (links != null) {
            for (String link : links) {
                link = validate(link);
                if (link != null) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
        }
        // audiomac provided via iframe
        links = br.getRegex("<iframe[^>]* src=(\"|')(.*?)\\1").getColumn(1);
        if (links != null) {
            for (String link : links) {
                link = validate(link);
                if (link != null) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
        }
        return decryptedLinks;
    }

    /**
     * Validates input URL and corrects protocol if missing prefixs, and also ignores some patterns to prevent false positives
     *
     * @param link
     * @return
     */
    private final String validate(String link) {
        if (link == null) {
            return null;
        }
        final String protocol = new Regex(br.getURL(), "^(https?)").getMatch(-1);
        if (link.startsWith("%3A%2F%2F")) {
            link = Encoding.urlDecode(link, true);
            link = protocol + link;
        } else if (link.startsWith("://")) {
            link = protocol + link;
        } else if (link.startsWith("//")) {
            link = protocol + ":" + link;
        }
        if (new Regex(link, "facebook.com(/|%2F)plugins(/|%2F)|twitter.com(/|%2F)").matches()) {
            return null;
        }
        return link;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
