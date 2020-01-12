package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "manganelo.com" }, urls = { "https?://(www\\.)?manganelo\\.com/(?:chapter|manga)/.+" })
public class ManganeloCom extends antiDDoSForDecrypt {
    public ManganeloCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        getPage(br, parameter.getCryptedUrl());
        if (StringUtils.containsIgnoreCase(parameter.getCryptedUrl(), "/manga/")) {
            String[] chapters = br.getRegex("<a[^>]+class\\s*=\\s*\"chapter-name[^\"]*\"[^>]+href\\s*=\\s*\"([^\"]+)\"").getColumn(0);
            if (chapters != null && chapters.length > 0) {
                for (String chapter : chapters) {
                    ret.add(createDownloadlink(Encoding.htmlDecode(chapter)));
                }
            }
        } else {
            String manga = br.getRegex("(?i)title\\s*=\\s*\".*?\"\\s*>\\s*<span\\s*itemprop\\s*=\\s*\"title\"\\s*>.*?</span>.*?title\\s*=\\s*\".*?\"\\s*>\\s*<span\\s*itemprop\\s*=\\s*\"title\"\\s*>\\s*(.*?)\\s*</span>").getMatch(0);
            String chapter = br.getRegex("(?i)<span\\s*itemprop\\s*=\\s*\"title\"\\s*>\\s*Chapter\\s*(\\d+(\\.\\d+)?)\\s*</span>").getMatch(0);
            final String[] breadcrumbs = br.getRegex("<a[^>]+class\\s*=\\s*\"a-h\"[^>]+>\\s*([^<]+)\\s*").getColumn(0);
            if (breadcrumbs != null && breadcrumbs.length > 1) {
                if (StringUtils.isEmpty(manga)) {
                    manga = breadcrumbs[1];
                }
                if (StringUtils.isEmpty(chapter)) {
                    chapter = breadcrumbs[2];
                }
            }
            String title = manga + "-Chapter_" + chapter;
            if (StringUtils.isEmpty(title)) {
                title = br.getRegex("<meta[^>]+property\\s*=\\s*\"og:title\"[^>]+content\\s*=\\s*\"\\s*([^\"]+)\\s+-\\s+Manganelo\\s*\"").getMatch(0);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            final String images[][] = br.getRegex("(?i)img\\s*src\\s*=\\s*\"(https?://[^\"]*?/\\d+.(?:jpe?g|png))\"").getMatches();
            final HashSet<String> dups = new HashSet<String>();
            for (String image[] : images) {
                if (isAbort()) {
                    break;
                }
                if (dups.add(image[0])) {
                    final DownloadLink link = createDownloadlink("directhttp://" + image[0]);
                    final String page = new Regex(image[0], "(?i)/(\\d+)\\.(?:jpe?g|png)$").getMatch(0);
                    link.setAvailable(true);
                    if (title != null) {
                        link.setFinalFileName(title + "-Page_" + page + Plugin.getFileNameExtensionFromURL(image[0]));
                    }
                    fp.add(link);
                    distribute(link);
                }
            }
        }
        return ret;
    }
}
