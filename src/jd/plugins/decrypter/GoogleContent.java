package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "googleusercontent.com" }, urls = { "https?://(?!translate\\.)[a-z0-9]+\\.googleusercontent\\.com/[a-zA-Z0-9\\_\\-]+.+" })
public class GoogleContent extends PluginForDecrypt {
    public GoogleContent(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (!parameter.toString().matches("https?://translate\\.googleusercontent\\.com/.+")) {
            ret.add(createDownloadlink("directhttp://" + parameter.toString()));
        }
        return ret;
    }
}
