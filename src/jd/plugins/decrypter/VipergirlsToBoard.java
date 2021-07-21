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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vipergirls.to" }, urls = { "https?://(?:www\\.)?vipergirls\\.to/threads/\\d+(.+)?" })
public class VipergirlsToBoard extends PluginForDecrypt {
    // WOO-945-41428
    public VipergirlsToBoard(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String postID = new Regex(parameter, "p(?:=|post)(\\d+)").getMatch(0);
        if (postID == null) {
            postID = "";
        }
        final String[] posts = br.getRegex("<li[^>]*id\\s*=\\s*\"post_" + postID + "[^>]*>(.*?)</li>\\s*<(li[^>]*id\\s*=\\s*\"post|/ol)").getColumn(0);
        for (final String post : posts) {
            final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            // The title is in the H2 tag spanning 3 lines
            String title = new Regex(post, "<h2[^>]*>[\\r\\n\\s]*(.*?)[\\r\\n\\s]*</h2>").getMatch(0);
            // Get all post content and then filter it for the href links
            final String postContent = new Regex(post, "<h2 class=\"title icon\">\\s*(.*?)\\s*</blockquote>").getMatch(0);
            final String[] results = new Regex(postContent, "<a href=\"(https?://[^\"]+)").getColumn(0);
            for (final String result : results) {
                decryptedLinks.add(createDownloadlink(result));
            }
            if (title != null) {
                title = title.replaceAll("(<img.*>)", "").trim();
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(decryptedLinks);
            }
            ret.addAll(decryptedLinks);
        }
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
