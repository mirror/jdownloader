package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 34675 $", interfaceVersion = 2, names = { "13dl.net" }, urls = { "https?://(www\\.)?13dl\\.(net|link)/wp/(link/.*?url=.+|zip/.*?url=.+)" })
public class ThirteenDlNet extends antiDDoSForDecrypt {
    public ThirteenDlNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        getPage(parameter.getCryptedUrl());
        int loop = 4;
        while (loop >= 0) {
            final String redirect = br.getRedirectLocation();
            if (redirect != null) {
                if (redirect.matches(getMatcher().pattern().pattern())) {
                    getPage(redirect);
                    loop--;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        final String redirect = br.getRedirectLocation();
        if (redirect != null) {
            if (!redirect.matches(getMatcher().pattern().pattern())) {
                final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                ret.add(createDownloadlink(redirect));
                return ret;
            }
        }
        return null;
    }
}
