package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40452 $", interfaceVersion = 3, names = { "imgfrog.cf" }, urls = { "https?://imgfrog\\.(?:cf|pw)/a/[a-zA-Z0-9\\-_]+\\.[a-zA-Z0-9]{4,}" })
public class ImgFrog extends antiDDoSForDecrypt {
    public ImgFrog(PluginWrapper wrapper) {
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
            final String title = br.getRegex("<strong\\s*data-text\\s*=\\s*\"album-name\"\\s*>\\s*(.*?)\\s*</").getMatch(0);
            int page = 1;
            while (true) {
                final String images[][] = br.getRegex("<a\\s*href\\s*=\\s*\"\\s*(https?://imgfrog.(?:cf|pw)/i/[^\"]*)\"[^<>]*>\\s*([^<>]*?)\\s*</a>").getMatches();
                if (images == null || images.length == 0) {
                    if (page > 1) {
                        break;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                for (final String image[] : images) {
                    final DownloadLink link = createDownloadlink(image[0]);
                    link.setName(image[1] + ".jpg");
                    link.setAvailable(true);
                    ret.add(link);
                }
                getPage("?sort=date_desc&page=" + ++page);
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
