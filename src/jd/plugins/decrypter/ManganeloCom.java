package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "manganelo.com" }, urls = { "https?://(www\\.)?manganelo\\.com/chapter/[^/]*/chapter_\\d+(\\.\\d+)?" })
public class ManganeloCom extends antiDDoSForDecrypt {
    public ManganeloCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        getPage(br, parameter.getCryptedUrl());
        final String manga = br.getRegex("(?i)title\\s*=\\s*\".*?\"\\s*>\\s*<span\\s*itemprop\\s*=\\s*\"title\"\\s*>.*?</span>.*?title\\s*=\\s*\".*?\"\\s*>\\s*<span\\s*itemprop\\s*=\\s*\"title\"\\s*>\\s*(.*?)\\s*</span>").getMatch(0);
        final String chapter = br.getRegex("(?i)<span\\s*itemprop\\s*=\\s*\"title\"\\s*>\\s*Chapter\\s*(\\d+(\\.\\d+)?)\\s*</span>").getMatch(0);
        final String title = manga + "-Chapter_" + chapter;
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
        return ret;
    }
}
