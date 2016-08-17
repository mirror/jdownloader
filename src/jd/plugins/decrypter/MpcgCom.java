package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 32094 $", interfaceVersion = 2, names = { "mpc-g.com" }, urls = { "https?://(www\\.)?mpc-g\\.com/[a-zA-Z0-9\\-]+" }) public class MpcgCom extends PluginForDecrypt {

    public MpcgCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String[] links = br.getRegex("name=\"downloadLink\"\\s*(type=\"hidden\")?\\s*value=\"((ftp|https?).*?)\"").getColumn(1);
        if (links != null) {
            for (final String link : links) {
                final DownloadLink downloadLink = createDownloadlink(link);
                decryptedLinks.add(downloadLink);
            }
        }
        return decryptedLinks;
    }

}
