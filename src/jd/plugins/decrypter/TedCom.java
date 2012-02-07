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

@DecrypterPlugin(revision = "$Revision: 15830 $", interfaceVersion = 2, names = { "ted.com" }, urls = { "http://(www\\.)?ted.com/talks/(lang/[a-zA-Z\\-]+/)?\\w+.html" }, flags = { 0 })
public class TedCom extends PluginForDecrypt {

    public TedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = parameter.toString();
        br.getPage(url);
        String[] info = br.getRegex("<meta property=\"og:video\" content=\"([^\"]+)/([^/\"\\-]+)(?:-320k)?\\.(\\w+)\"").getRow(0);
        if (info == null) return decryptedLinks;

        // Videos
        String baseUrl = info[0] + "/" + info[1];
        FilePackage fp = FilePackage.getInstance();
        fp.setName(info[1] + " (video)");
        fp.add(createDownloadlink(baseUrl + "-light." + info[2]));
        fp.add(createDownloadlink(baseUrl + "." + info[2]));
        fp.add(createDownloadlink(baseUrl + "-480p." + info[2]));
        decryptedLinks.addAll(fp.getChildren());

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
        }
        decryptedLinks.addAll(subP.getChildren());
        return decryptedLinks;
    }

}
