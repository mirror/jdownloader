package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vipergirls.to" }, urls = { "https?://(?:www\\.)?vipergirls\\.to/threads/\\d+" })
public class VipergirlsToBoard extends PluginForDecrypt {
    // WOO-945-41428
    public VipergirlsToBoard(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // The title is in the H2 tag spanning 3 lines
        final String title = br.getRegex("<h2[^>]*>[\\r\\n\\s]*(.*?)[\\r\\n\\s]*</h2>").getMatch(0);
        // Get all post content and then filter it for the href links
        final String postContent = br.getRegex("<h2 class=\"title icon\">\\s*(.*?)\\s*</blockquote>").getMatch(0);
        final String[] results = new Regex(postContent, "<a href=\"(https?://[^\"]+)").getColumn(0);
        for (final String result : results) {
            decryptedLinks.add(createDownloadlink(result));
        }
        if (title != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
