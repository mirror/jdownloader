package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "x-gay.ws" }, urls = { "https?://x-gay.ws/link/\\d+" })
public class XGayWs extends antiDDoSForDecrypt {
    public XGayWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setCurrentURL("http://x-gay.ws");
        getPage(parameter);
        final String finallink = br.getRegex("<meta http-equiv=\"refresh\".*?url=(https?://.*?)\"").getMatch(0);
        if (finallink == null) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
