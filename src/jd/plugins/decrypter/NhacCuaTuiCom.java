package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

/**
 * @author noone2407
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nhaccuatui.com" }, urls = { "http://(www\\.)?nhaccuatui\\.com/playlist/\\S+" }) 
public class NhacCuaTuiCom extends PluginForDecrypt {

    public NhacCuaTuiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String url = parameter.toString();
        br.setFollowRedirects(true);
        br.getPage(url);
        // package name
        String title = br.getRegex("<h1 itemprop=\"name\">(.*?)<\\/h1>").getMatch(0);
        // get all songs
        final String[] allMatches = br.getRegex("href=\"(http://(www\\.)?nhaccuatui\\.com/bai-hat/\\S+)\"\\sl=\"off\"").getColumn(0);
        if (allMatches != null) {
            for (final String s : allMatches) {
                decryptedLinks.add(createDownloadlink(s));
            }
        }
        if (title != null) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(title);
            filePackage.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
