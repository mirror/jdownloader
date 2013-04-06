package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ted.com" }, urls = { "http://(www\\.)?ted.com/talks/(lang/[a-zA-Z\\-]+/)?\\w+\\.html" }, flags = { 0 })
public class TedCom extends PluginForDecrypt {

    public TedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = parameter.toString();
        br.getPage(url);
        String externalLink = br.getRegex("class=\"external\" href=\"(http://(www\\.)?youtube\\.com/[^<>\"]*?)\"").getMatch(0);
        if (externalLink != null) {
            decryptedLinks.add(createDownloadlink(externalLink));
            return decryptedLinks;
        }
        String talkInfo = br.getRegex(">var talkDetails = \\{(.*?)<div class=\"talk\\-wrapper\">").getMatch(0);
        talkInfo = Encoding.htmlDecode(talkInfo).replace("\\", "");
        if (talkInfo == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String plainfilename = br.getRegex("\"http://download\\.ted\\.com/talks/([^<>\"]*?)\\.mp4([^<>\"]+)?\">download the video</a>").getMatch(0);
        if (plainfilename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        plainfilename = Encoding.htmlDecode(plainfilename.trim());
        br.getPage("http://www.ted.com/download/links/slug/" + plainfilename + "/type/talks/ext/mp4");
        final String[] qualities = { "480p", "light" };
        for (final String quality : qualities) {
            final DownloadLink dl = createDownloadlink("http://download.ted.com/talks/" + plainfilename + "-" + quality + ".mp4?apikey=TEDDOWNLOAD");
            dl.setFinalFileName(plainfilename + "_" + quality + ".mp4");
            decryptedLinks.add(dl);
        }
        final String dlMP3 = br.getRegex("<dt><a href=\"(http://download\\.ted\\.com/talks/[^<>\"]*?)\">Download to desktop \\(MP3\\)<").getMatch(0);
        if (dlMP3 != null) {
            final DownloadLink dl = createDownloadlink(dlMP3);
            dl.setFinalFileName(plainfilename + ".mp3");
            decryptedLinks.add(dl);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(plainfilename);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
