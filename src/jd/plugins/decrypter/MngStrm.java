package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangastream.com" }, urls = { "http://[\\w\\.]*?mangastream\\.com/read/.*?/\\d+" }, flags = { 0 })
public class MngStrm extends PluginForDecrypt {

    public MngStrm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String url = parameter.toString();
        br.getPage(url + "/1");
        if (br.containsHTML("404: Page Not Found<")) return null;
        String title = br.getRegex("<title>(.*?)- Read").getMatch(0);
        if (title == null) return null;
        String prefix = new Regex(url, "(/read.+)").getMatch(0);
        String pages[] = br.getRegex("\"(" + prefix + "/\\d+)\"").getColumn(0);
        if (pages == null) return null;
        ArrayList<String> done = new ArrayList<String>();
        for (String page : pages) {
            if (done.contains(page)) continue;
            done.add(page);
        }
        progress.setRange(done.size());
        for (String page : done) {
            br.getPage("http://mangastream.com" + page);
            String pageNr = new Regex(page, ".+/(\\d+)$").getMatch(0);
            String picUrl = br.getRegex("<img src=\"(http://.*?mangastream.com/manga/\\d+/\\d+/.*?\\.(jpg|png))\"").getMatch(0);
            String ext = br.getRegex("<img src=\"(http://.*?mangastream.com/manga/\\d+/\\d+/.*?\\.(jpg|png))\"").getMatch(1);
            if (picUrl != null) {
                DownloadLink link = createDownloadlink("directhttp://" + picUrl);
                link.setAvailable(true);
                link.setFinalFileName(title.trim() + "_Page_" + pageNr + "." + ext);
                decryptedLinks.add(link);
            }
            progress.increase(1);
        }
        return decryptedLinks;
    }
}
