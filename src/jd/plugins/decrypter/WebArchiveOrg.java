package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "web.archive.org" }, urls = { "https?://web\\.archive\\.org/web/\\d+im_/https?.+" })
public class WebArchiveOrg extends PluginForDecrypt {
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ret.add(createDownloadlink("directhttp://" + parameter.getCryptedUrl()));
        return ret;
    }
}
