package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "loveguests.com" }, urls = { "http://(\\w+\\.)?loveguests.com/(?!site\\-news).+/[\\w\\-]+\\.html" }, flags = { 0 })
public class LovGstCm extends PluginForDecrypt {
    public LovGstCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = null;

        String content = br.getRegex("class=\"news\"(.*?)</td>").getMatch(0);
        if (content != null) {
            fpName = new Regex(content, "alt=(\\'|\")(.*?)(\\'|\")").getMatch(1);
            if (fpName == null) {
                fpName = new Regex(content, "title=(\\'|\")(.*?)(\\'|\")").getMatch(1);
            }
            if (fpName != null) {
                fpName = fpName.replaceAll("Ã¤", "ae");
                fpName = fpName.replaceAll("Ã", "Ue");
                fpName = fpName.replaceAll("Ã¼", "ue");
                fpName = fpName.replaceAll("ä", "ae");
                fpName = fpName.replaceAll("Ä", "Ae");
                fpName = fpName.replaceAll("ö", "oe");
                fpName = fpName.replaceAll("Ö", "Oe");
                fpName = fpName.replaceAll("ü", "ue");
                fpName = fpName.replaceAll("Ü", "Ue");
                fpName = fpName.replaceAll("ß", "ss");
                fpName = fpName.replaceAll(":", " -");
                fpName = fpName.replaceAll("'", "");
                fpName = fpName.replaceAll("\"", "");
            }
        }
        String[] links = new Regex(content, "<a href=\"(http://.*?)\"").getColumn(0);

        if ((links == null) || (links.length == 0)) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));

        for (String link : links) {
            decryptedLinks.add(createDownloadlink(link));
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}