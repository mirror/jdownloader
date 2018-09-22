package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wiemanga.com" }, urls = { "https?://(www\\.)?wiemanga\\.com/chapter/[^/]*(Kapitel|Prolog|Chapter)\\d+[^/]*/\\d+" })
public class WieMangaCom extends PluginForDecrypt {
    public WieMangaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(parameter.getCryptedUrl());
        final String manga = br.getRegex("href\\s*=\\s*\"https?://(?:www.)?wiemanga\\.com/manga/.*?\\.html\".*?>\\s*(.*?)\\s*<").getMatch(0);
        final String chapter = br.getRegex("href\\s*=\\s*\"https?://(?:www.)?wiemanga\\.com/chapter/.*?>[^<]*(?:Kapitel|Chapter) (\\d+)").getMatch(0);
        final String title = manga + "-Kapitel_" + chapter;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        final String id = new Regex(parameter.getCryptedUrl(), "/(\\d+)$").getMatch(0);
        final String sites[][] = br.getRegex("value\\s*=\\s*\"(https?://(?:www\\.)?wiemanga\\.com/chapter/[^/]*/" + id + "-\\d+.html)\"\\s*(selected)?\\s*>\\s*(.*?)\\s*<").getMatches();
        final HashSet<String> dups = new HashSet<String>();
        for (String site[] : sites) {
            if (isAbort()) {
                break;
            }
            if (!dups.add(site[2])) {
                continue;
            }
            if (!"selected".equals(site[1])) {
                br.getPage(site[0]);
            }
            final String image = br.getRegex("img\\s*id='comicpic'.*?src=\"(https?://[^'\"]*?\\.(?:jpe?g|png))\"").getMatch(0);
            if (image == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink link = createDownloadlink("directhttp://" + image);
            link.setAvailable(true);
            if (title != null) {
                link.setFinalFileName(title + "-Seite_" + site[2] + Plugin.getFileNameExtensionFromURL(image));
            }
            fp.add(link);
            distribute(link);
        }
        return ret;
    }
}
