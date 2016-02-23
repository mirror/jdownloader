package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision: 21898 $", interfaceVersion = 2, names = { "torx.pw" }, urls = { "https?://(www\\.)?torx\\.pw/\\?download=[a-zA-Z0-9]+" }, flags = { 0 })
public class TorXPW extends PluginForDecrypt {

    public TorXPW(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String title = br.getRegex("<center><h3><b>(.*?)</b").getMatch(0);
        final String[][] urls = br.getRegex("href=\"(https?://[a-f0-9]+\\.torx\\.pw/\\?file=.*?)\">(.*?)</a>\\s*-\\s*([0-9\\.]+)\\s*<b>\\s*([TGKMB]+)").getMatches();
        if (urls != null) {
            for (final String url[] : urls) {
                final DownloadLink downloadLink = createDownloadlink("directhttp://" + url[0]);
                downloadLink.setAvailable(true);
                downloadLink.setFinalFileName(url[1]);
                final String size = url[2] + " " + url[3];
                downloadLink.setDownloadSize(SizeFormatter.getSize(size));
                decryptedLinks.add(downloadLink);
            }
        }
        if (title != null && decryptedLinks.size() > 0) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
