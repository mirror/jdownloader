package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 36642 $", interfaceVersion = 3, names = { "wnacg.org" }, urls = { "https?://(?:www\\.)?wnacg\\.org/(download|photos)-index-aid-\\d+.html" })
public class WnacgOrg extends PluginForDecrypt {
    public WnacgOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(parameter.getCryptedUrl().replaceFirst("org/photos-", "org/download-"));
        final String fileName = br.getRegex("download_filename\">(.*?)</p").getMatch(0);
        final String url = br.getRegex("down_btn\"\\s*href=\"(https?://.*?)\"").getMatch(0);
        if (url != null) {
            final DownloadLink link = createDownloadlink("directhttp://" + url);
            if (fileName != null) {
                link.setFinalFileName(Encoding.htmlOnlyDecode(fileName));
            }
            ret.add(link);
        }
        if (ret.size() > 0) {
            return ret;
        } else {
            return null;
        }
    }
}
