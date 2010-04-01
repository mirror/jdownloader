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
        String[] hits = br.getRegex("class=\"hd\".*?href=\"((http://.*?apple[^<>]*?|/[^<>]*?)_h?\\d+p\\.mov)\"").getColumn(0);
        if (hits.length == 0) {
            /* custom trailer page */
            br.getPage(parameter.toString() + "/hd/");
        }
        String customAgent[] = new String[] { "User-Agent", "QuickTime/7.6.2 (qtver=7.6.2;cpu=IA32;os=Mac 10.5.8)" };
        ArrayList<String[]> customHeaders = new ArrayList<String[]>();
        customHeaders.add(customAgent);
        hits = br.getRegex("class=\"hd\".*?href=\"((http://.*?apple[^<>]*?|/[^<>]*?)_h?\\d+p\\.mov)\"").getColumn(0);
        if (hits.length == 0) return decryptedLinks;
        String title = br.getRegex("var trailerTitle = '(.*?)';").getMatch(0);
        FilePackage fp = FilePackage.getInstance();
        if (title != null) fp.setName(title.trim() + " Trailers");
        for (String hit : hits) {
            /* correct url */
            String url = hit.replaceFirst("movies\\.", "www.");
            /* get format */
            String format = new Regex(url, "_h?(\\d+)p").getMatch(0);
            /* get filename */
            String file = new Regex(url, ".+/(.+)").getMatch(0);
            if (file == null || format == null) continue;
            /* get size */
            String size = br.getRegex("class=\"hd\".*?>.*?" + hit + ".*?" + format + "p \\((\\d+ ?MB)\\)").getMatch(0);
            /* fix url for download */
            url = url.replaceFirst("_h?" + format, "_h" + format);
            /* correct url if its relative */
            if (!url.startsWith("http")) url = "http://trailers.apple.com" + url;
            DownloadLink dlLink = createDownloadlink(url);
            if (size != null) dlLink.setDownloadSize(Regex.getSize(size));
            dlLink.setAvailable(true);
            dlLink.setProperty("customHeader", customHeaders);
            decryptedLinks.add(dlLink);
        }
        if (title != null) fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
