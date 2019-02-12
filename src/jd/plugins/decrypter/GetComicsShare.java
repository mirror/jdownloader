package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision: 39909 $", interfaceVersion = 3, names = { "getcomics.info" }, urls = { "https?://getcomics\\.info/share/uploads/\\d+/\\d+/[a-zA-Z0-9\\_\\-]+\\.txt" })
public class GetComicsShare extends PluginForDecrypt {
    public GetComicsShare(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // Load page
        br.setFollowRedirects(true);
        final Request request = br.createGetRequest(parameter);
        request.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String page = br.getPage(request).toString();
        String[][] regExMatches = new Regex(page, "(https?://.*?)(\\s|$)").getMatches();
        for (String[] regExMatch : regExMatches) {
            String matchedURL = Encoding.htmlDecode(regExMatch[0]);
            decryptedLinks.add(createDownloadlink(matchedURL));
        }
        return decryptedLinks;
    }
}