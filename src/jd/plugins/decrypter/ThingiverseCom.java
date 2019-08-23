package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 41147 $", interfaceVersion = 2, names = { "thingiverse.com" }, urls = { "https?://(www\\.)?thingiverse\\.com/thing:\\d+" })
public class ThingiverseCom extends antiDDoSForDecrypt {
    public ThingiverseCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>\\s*([^<]+?)\\s*-\\s*Thingiverse").getMatch(0);
        final String fid = new Regex(parameter, "thing:(\\d+).*").getMatch(0);
        final DownloadLink link = createDownloadlink("directhttp://https://www.thingiverse.com/thing:" + fid + "/zip");
        if (fpName != null) {
            fpName = Encoding.htmlOnlyDecode(fpName);
            link.setFinalFileName(fpName + ".zip");
        }
        decryptedLinks.add(link);
        return decryptedLinks;
    }
}