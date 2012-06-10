package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision: 17707 $", interfaceVersion = 2, names = { "naughtyblog.org" }, urls = { "http://(www\\.)?naughtyblog\\.org/(?!category)[^/]+" }, flags = { 0 })
public class NaughtyBlgOrg extends PluginForDecrypt {

    public NaughtyBlgOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);

        String contentReleaseName = br.getRegex("<h2 class=\"post\\-title\">(.*?)</h2>").getMatch(0);
        if (contentReleaseName == null) return null;
        contentReleaseName = contentReleaseName.replaceAll("&#8217;", "");

        String contentReleaseLinks = br.getRegex("<em>Download:</em>(.*?)<span style=").getMatch(0);
        if (contentReleaseLinks == null) {
            logger.warning("contentReleaseLinks == null");
            return null;
        }
        String[] links = new Regex(contentReleaseLinks, "<a href=\"(http[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        for (String link : links) {
            decryptedLinks.add(createDownloadlink(link));
        }

        String[] imgs = br.getRegex("(http://([\\w\\.]+)?pixhost\\.org/show/[^\"]+)").getColumn(0);
        if (links != null && links.length != 0) {
            for (String img : imgs) {
                decryptedLinks.add(createDownloadlink(img));
            }
        }

        if (contentReleaseName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(contentReleaseName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}