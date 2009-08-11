package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "http://www.flashget.com" }, urls = { "flashget://.+&?" }, flags = { 0 })
public class FlashGet extends PluginForDecrypt {

    public FlashGet(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = Encoding.Base64Decode(new Regex(parameter.toString(), "flashget://(.*?)&|$").getMatch(0));
        if (url == null) return null;
        url = url.replaceAll("(\\[FLASHGET\\])", "");
        decryptedLinks.add(this.createDownloadlink(url));
        return decryptedLinks;
    }
}
