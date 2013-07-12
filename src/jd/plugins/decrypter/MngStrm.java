package jd.plugins.decrypter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangastream.com" }, urls = { "http://(www\\.)?(mangastream|readms)\\.com/read/([a-z0-9\\-_]+/){2}\\d+" }, flags = { 0 })
public class MngStrm extends PluginForDecrypt {

    public MngStrm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String url = parameter.toString().replace("readms.com/", "mangastream.com/");
        if (!parameter.equals(url)) parameter.setCryptedUrl(url);
        br.getPage(url + "/1");
        if (br.containsHTML(">Page Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String title = br.getRegex("<title>([^<>\"]*?) \\- Manga Stream</title>").getMatch(0);
        if (title == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String lastP = br.getRegex(">Last Page \\((\\d+)\\)<").getMatch(0);
        if (lastP == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final int lastPage = Integer.parseInt(lastP);
        NumberFormat formatter = new DecimalFormat("00");
        String urlPart = new Regex(url, "mangastream\\.com(/read/.+)").getMatch(0);
        for (int i = 1; i <= lastPage; i++) {
            DownloadLink link = createDownloadlink("mangastream://" + urlPart + "/" + i);
            link.setAvailableStatus(AvailableStatus.TRUE);
            link.setFinalFileName(title.trim() + " – page " + formatter.format(i) + ".png");
            decryptedLinks.add(link);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(title.trim());
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}