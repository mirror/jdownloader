package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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
        String[] info = br.getRegex("flashvars\\&quot; value=\\&quot;vu=(http://video\\.ted\\.com/talk/stream/([^\"]*?)/[^\"]*?)/([^/\"\\-]*?)(?:\\-320k)?\\.(\\w+)\\&amp;").getRow(0);
        if (info == null || info.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        // Videos
        final String plainfilename = info[2];
        final String ext = info[3];
        final String baseUrl = "http://download.ted.com/talks/" + plainfilename;
        FilePackage fp = FilePackage.getInstance();
        fp.setName(info[1] + " (video)");
        // Standard
        decryptedLinks.add(createDownloadlink(baseUrl + "." + ext));
        // 480p
        decryptedLinks.add(createDownloadlink(baseUrl + "-480p." + ext));
        // the link which is used in the player
        decryptedLinks.add(createDownloadlink(info[0] + "/" + plainfilename + "-320k." + ext));
        // light version (2 different links leading to the exact same file)
        decryptedLinks.add(createDownloadlink(baseUrl + "-light." + ext));
        decryptedLinks.add(createDownloadlink("http://video.ted.com/talk/podcast/" + info[1] + "/None/" + plainfilename + "-light." + ext));
        // mp3 file
        decryptedLinks.add(createDownloadlink(baseUrl + ".mp3"));
        fp.addLinks(decryptedLinks);

        // Subtitles: gets talkId and JSON array of languages from HTML
        String talkId = br.getRegex("var +talkID *= *(\\d+);").getMatch(0);
        String langParam = br.getRegex("languages:\"([^\"]+)\",").getMatch(0);
        if (talkId == null || langParam == null) return decryptedLinks;

        // Unescapes JSON array and finds existing subtitles
        langParam = Encoding.urlDecode(langParam, false);
        String[][] langArr = new Regex(langParam, "\"LanguageCode\":\"([a-zA-Z\\-]+)\"").getMatches();
        if (langArr == null) return decryptedLinks;

        // Adds each subtitle
        FilePackage subP = FilePackage.getInstance();
        subP.setName(info[1] + " (subtitles)");
        for (int i = 0; i < langArr.length; i++) {
            DownloadLink dl = createDownloadlink("directhttp://http://www.ted.com/talks/subtitles/id/" + talkId + "/lang/" + langArr[i][0] + "/format/srt");
            dl.setFinalFileName(info[1] + "." + langArr[i][0] + ".srt");
            subP.add(dl);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

}
