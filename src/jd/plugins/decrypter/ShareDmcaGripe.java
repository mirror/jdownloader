package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40452 $", interfaceVersion = 3, names = { "share.dmca.gripe" }, urls = { "https?://share\\.dmca\\.gripe/a/[a-zA-Z0-9\\._-]+" })
public class ShareDmcaGripe extends antiDDoSForDecrypt {
    public ShareDmcaGripe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        getPage(parameter.getCryptedUrl());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (!canHandle(br.getURL())) {
            return ret;
        } else {
            final String title = br.getRegex("class\\s*=\\s*\"title\"\\s*id\\s*=\\s*'title'[^>]*>\\s*(.*?)\\s*</").getMatch(0);
            final String images[] = br.getRegex("(https?://share\\.dmca\\.gripe/[a-zA-Z0-9\\._-]+\\.\\w+)").getColumn(0);
            if (images == null || images.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final HashSet<String> dups = new HashSet<String>();
            for (final String image : images) {
                if (dups.add(image)) {
                    final DownloadLink link = createDownloadlink(image);
                    link.setAvailable(true);
                    ret.add(link);
                }
            }
            if (ret.size() > 1 && title != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(ret);
            }
            return ret;
        }
    }
}
