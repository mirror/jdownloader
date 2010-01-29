package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "apple.com" }, urls = { "http://[\\w\\.]*?apple\\.com/trailers/[a-zA-Z0-9_/]+/" }, flags = { 0 })
public class AppleTrailer extends PluginForDecrypt {

    public AppleTrailer(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());
        String[] hits = br.getRegex("class=\"hd\".*?href=\"(http://.*?apple.*?_h?\\d+p\\.mov)\"").getColumn(0);
        if (hits.length == 0) {
            /* custom trailer page */
            br.getPage(parameter.toString() + "/hd/");
        }
        hits = br.getRegex("class=\"hd\".*?href=\"(http://.*?apple.*?_h?\\d+p\\.mov)\"").getColumn(0);
        if (hits.length == 0) return decryptedLinks;
        String title = br.getRegex("var trailerTitle = '(.*?)';").getMatch(0);
        FilePackage fp = FilePackage.getInstance();
        if (title != null) fp.setName(title.trim() + " Trailers");
        for (String hit : hits) {
            String url = hit.replaceFirst("http://.*?\\.com/", "http://www.apple.com/");
            String format = new Regex(url, "_h?(\\d+)p").getMatch(0);
            if (format == null) continue;
            String size = br.getRegex("class=\"hd\".*?>" + format + "p \\((\\d+ ?MB)\\)").getMatch(0);
            /* TODO: get correct size for custom trailer page */
            url = url.replaceFirst("_h?" + format, "_h" + format);
            DownloadLink dlLink = createDownloadlink(url);
            if (size != null) dlLink.setDownloadSize(Regex.getSize(size));
            dlLink.setAvailable(true);
            decryptedLinks.add(dlLink);
        }
        if (title != null) fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
