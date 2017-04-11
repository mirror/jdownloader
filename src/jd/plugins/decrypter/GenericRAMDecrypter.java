package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ram" }, urls = { "https?://.+/.*\\.ram($|\\?[^\\s<>\"']*)" })
public class GenericRAMDecrypter extends PluginForDecrypt {
    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public GenericRAMDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        final String videoURLs[] = br.getRegex("(https?://.*(rm|ra|rmvb))").getColumn(0);
        if (videoURLs != null && videoURLs.length > 0) {
            final HashSet<String> dups = new HashSet<String>();
            for (final String videoURL : videoURLs) {
                if (dups.add(videoURL)) {
                    final DownloadLink downloadLink = createDownloadlink(videoURL);
                    downloadLink.setProperty("Referer", param.getCryptedUrl());
                    ret.add(downloadLink);
                }
            }
        }
        if (ret.size() == 0) {
            return null;
        } else {
            return ret;
        }
    }
}
