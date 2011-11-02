package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 14904 $", interfaceVersion = 2, names = { "unixmanga.com" }, urls = { "http://(www\\.)?unixmanga\\.com/onlinereading/[^\\?]*?/.*?(c|ch)\\d+.*" }, flags = { 0 })
public class UnixMangaCom extends PluginForDecrypt {

    public UnixMangaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        String url = parameter.toString();
        br.getPage(url);

        // We get the title
        String title = br.getRegex(".*?/(.+?)/.*?").getMatch(0);
        if (title == null) { return null; }

        // We get the number of pages in the chapter
        String[][] hrefArray = br.getRegex("<A[^>]*?\"td2\"[^>]*?HREF[^>]*?\"([^>]*?)\">[^>]*?\\d+[^>]*?\\.(png|jpg|jpeg|gif)[^>]*?</A").getMatches();
        int numberOfPages = hrefArray.length;
        String format = "%02d";
        if (numberOfPages > 0) {
            format = String.format("%%0%dd", (int) Math.log10(numberOfPages) + 1);
        }

        progress.setRange(numberOfPages);

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // We load each page and retrieve the URL of the picture
        FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        for (int i = 0; i < numberOfPages; i++) {
            br.getPage(hrefArray[i][0]);
            String[][] unformattedSource = br.getRegex("<IMG.*?SRC.*?\"(.*?)\\.(png|jpg|jpeg|gif)\"").getMatches();
            String source = unformattedSource[0][0];
            String extension = unformattedSource[0][1];
            DownloadLink link = createDownloadlink("directhttp://" + source + "." + extension);
            String pageNumber = String.format(format, (i + 1));
            link.setFinalFileName(title + "–page_" + pageNumber + "." + extension);
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
