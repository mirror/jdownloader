package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wnacg.org" }, urls = { "https?://(?:www\\.)?wnacg\\.org/(download|photos)-index-aid-\\d+.html" })
public class WnacgOrg extends PluginForDecrypt {
    public WnacgOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl().replaceFirst("/photos-", "/download-"));
        final String fileName = br.getRegex("download_filename\">\\s*(.*?)\\s*</p").getMatch(0);
        final String url = br.getRegex("down_btn[^\"]*\"\\s*href\\s*=\\s*\"((https)?:?//.*?)\"").getMatch(0);
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final DownloadLink link = createDownloadlink("directhttp://" + br.getURL(url).toString());
        if (fileName != null) {
            final String finalFilename = Encoding.htmlOnlyDecode(fileName).trim();
            link.setFinalFileName(finalFilename);
            link.setProperty(DirectHTTP.FIXNAME, finalFilename);
        }
        ret.add(link);
        return ret;
    }
}
