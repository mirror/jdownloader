package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploading.com" }, urls = { "http://[\\w\\.]*?uploading\\.com/linklists/\\w+" }, flags = { 0 })
public class PldingFldr extends PluginForDecrypt {

    public PldingFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setFollowRedirects(true);
        br.setCookie("http://www.uploading.com/", "lang", "1");
        br.setCookie("http://www.uploading.com/", "language", "1");
        br.setCookie("http://www.uploading.com/", "setlang", "en");
        br.setCookie("http://www.uploading.com/", "_lang", "en");
        br.getPage(parameter.toString());
        if (br.containsHTML("(We are sorry, this link list was removed either by its owner or due to the complaint received|/images/ico_big_file_error\\.gif\")")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String code = new Regex(parameter.toString(), "linklists/(\\w+)").getMatch(0);
        int page = 1;
        while (true) {
            br.postPage("http://uploading.com/linklists/main/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "action=get_files&code=" + code + "&pass=&page=" + page);
            String correctedHTML = br.toString().replace("\\", "");
            String founds[] = new Regex(correctedHTML, "(http://[\\w\\.]*?uploading\\.com/files/(get/)?\\w+)").getColumn(0);
            if (founds != null) {
                for (String found : founds) {
                    DownloadLink dLink = createDownloadlink(found);
                    decryptedLinks.add(dLink);
                }
            }
            page++;
            if (!new Regex(correctedHTML, "href=\"#\">" + page).matches()) {
                break;
            }
        }
        return decryptedLinks;
    }

}
