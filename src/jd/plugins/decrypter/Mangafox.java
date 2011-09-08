package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangafox.com" }, urls = { "http://[\\w\\.]*?mangafox\\.com/manga/.*?/(v\\d+/c\\d+/|c\\d+/)" }, flags = { 0 })
public class Mangafox extends PluginForDecrypt {

    public Mangafox(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        String url = parameter.toString();
        br.getPage(url + "/1.html");
        if (br.containsHTML("cannot be found|not available yet")) return null;

        // We get the title
        String title = br.getRegex("<title>(.+?) Page 1, Read").getMatch(0);
        if (title == null) return null;

        // We get the number of pages in the chapter
        int numberOfPages = Integer.parseInt(br.getRegex("of (\\d+)").getMatch(0));
        progress.setRange(numberOfPages);

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // We load each page and retrieve the URL of the picture
        FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        for (int i = 1; i <= numberOfPages; i++) {
            br.getPage(url + "/" + i + ".html");
            String[][] unformattedSource = br.getRegex("onclick=\"return enlarge\\(\\);\"><img src=\"(http://.+?(.[a-z]+))\" .+? id=\"image\"").getMatches();
            String source = unformattedSource[0][0];
            String extension = unformattedSource[0][1];

            DownloadLink link = createDownloadlink("directhttp://" + source);
            link.setFinalFileName(title + " – page " + i + extension);
            fp.add(link);
            try {
                distribute(link);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(link);

            progress.increase(1);
        }

        return decryptedLinks;
    }
}
