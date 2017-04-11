package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "torx.pw", "premium.torx.pw" }, urls = { "https?://(www\\.)?torx\\.pw/\\?download=[a-zA-Z0-9]+", "https?://premium\\.torx\\.pw/\\?(file=[a-zA-Z0-9\\=]+&x=[a-zA-Z0-9\\=]+&username=.*?&password=.+|username=.*?&password=.+&download=[a-zA-Z0-9\\=]+)" }) public class TorXPW extends antiDDoSForDecrypt {

    public TorXPW(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if ("premium.torx.pw".equals(getHost())) {
            if (StringUtils.containsIgnoreCase(parameter, "torx.pw/?file")) {
                final DownloadLink downloadLink = createDownloadlink("directhttp://" + parameter);
                decryptedLinks.add(downloadLink);
            } else {
                br.setFollowRedirects(true);
                getPage(parameter);
                final String title = br.getRegex("<center><h3><b>(.*?)</b").getMatch(0);
                final String[][] urls = br.getRegex("href=\"(https?://premium\\.torx\\.pw/\\?file=.*?)\">(.*?)</a>\\s*-\\s*([0-9\\.]+)\\s*<b>\\s*([TGKMB]+)").getMatches();
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
            }
        } else {
            br.setFollowRedirects(true);
            getPage(parameter);
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
        }
        return decryptedLinks;
    }

}
