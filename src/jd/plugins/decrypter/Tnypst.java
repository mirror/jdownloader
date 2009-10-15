package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tinypaste.com" }, urls = { "http://[\\w\\.]*?tinypaste\\.com/([0-9a-z]+$|.*?id=[0-9a-z]+)" }, flags = { 0 })
public class Tnypst extends PluginForDecrypt {

    private DownloadLink dl = null;

    public Tnypst(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String link = parameter.toString();
        String id = new Regex(link, "tinypaste\\.com/.*?id=([0-9a-z]+)").getMatch(0);
        if (id == null) id = new Regex(link, "tinypaste\\.com/([0-9a-z]+)").getMatch(0);
        if (id == null) return null;
        br.getPage("http://tinypaste.com/pre.php?id=" + id);
        String[] links = HTMLParser.getHttpLinks(br.toString(), null);
        ArrayList<String> pws = HTMLParser.findPasswords(br.toString());
        for (String element : links) {
            /* prevent recursion */
            if (element.contains("tinypaste.com")) continue;
            decryptedLinks.add(dl = createDownloadlink(element));
            dl.addSourcePluginPasswordList(pws);
        }
        return decryptedLinks;
    }
}
