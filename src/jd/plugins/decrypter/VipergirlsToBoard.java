package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vipergirls.to" }, urls = { "https?://(?:www\\.)?vipergirls\\.to/threads/\\d+(.+)?" })
public class VipergirlsToBoard extends PluginForDecrypt {
    public VipergirlsToBoard(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String threadID = new Regex(parameter, "threads/(\\d+)").getMatch(0);
        String postID = new Regex(parameter, "p(?:=|post)(\\d+)").getMatch(0);
        if (postID == null) {
            postID = "";
        }
        final String[] posts = br.getRegex("<li[^>]*id\\s*=\\s*\"post_" + postID + "[^>]*>(.*?)</li>\\s*<(li[^>]*id\\s*=\\s*\"post|/ol)").getColumn(0);
        for (final String post : posts) {
            final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            String title = new Regex(post, "<div style\\s*=\\s*\"text-align: center;\">\\s*<i>\\s*<b>\\s*<font color\\s*=\\s*\\s*\"red\"\\s*>\\s*<font[^>]*>(.*?)</font>\\s*</font>\\s*</b>\\s*</i>\\s*<br />").getMatch(0);
            if (title == null) {
                // The title is in the H2 tag spanning 3 lines
                title = new Regex(post, "<h2[^>]*>[\\r\\n\\s]*(.*?)[\\r\\n\\s]*</h2>").getMatch(0);
                if (title == null) {
                    final String postNumber = new Regex(post, "name\\s*=\\s*\"post(\\d+)").getMatch(0);
                    title = threadID + "_" + postNumber;
                }
            }
            // Get all post content and then filter it for the href links
            String postContent = new Regex(post, "<h2 class=\"title icon\">\\s*(.*?)\\s*<div\\s*class\\s*=\\s*\"(after_content|postfoot)\"").getMatch(0);
            if (postContent == null) {
                postContent = new Regex(post, "<div\\s*class\\s*=\\s*\"content\"\\s*>\\s*(.*?)\\s*<div\\s*class\\s*=\\s*\"(after_content|postfoot)\"").getMatch(0);
            }
            final String[] results = new Regex(postContent, "<a href=\"(https?://[^\"]+)").getColumn(0);
            for (final String result : results) {
                decryptedLinks.add(createDownloadlink(result));
            }
            if (title != null) {
                title = title.replaceAll("(<img.*>)", "");
                title = Encoding.htmlDecode(title).trim();
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
