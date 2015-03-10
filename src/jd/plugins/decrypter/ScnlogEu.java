//    jDownloader - Downloadmanager

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

/**
 * @author ohmygod
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision: 24808 $", interfaceVersion = 1, names = { "scnlog.eu" }, urls = { "https?://(?:www\\.)?scnlog\\.eu/(?:[a-z0-9_\\-]+/){2}" }, flags = { 0 })
public class ScnlogEu extends antiDDoSForDecrypt {

    /**
     * @author ohgod
     * */
    public ScnlogEu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        br.setFollowRedirects(true);
        getPage(parameter);

        final String content = br.getRegex("<div class=\"download\">(.*?)<div class=\"clear\">").getMatch(0);

        if (content == null) {
            return null;
        }

        final String packageName = br.getRegex("<strong>Release:</strong>(.*?)<br/>").getMatch(0);

        final String[] links = new Regex(content, "<a href=\"(https?://.*?)\"").getColumn(0);

        if (links == null || links.length == 0) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        for (final String link : links) {
            decryptedLinks.add(createDownloadlink(link));
        }

        if (packageName != null) {
            FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(packageName);
            filePackage.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}