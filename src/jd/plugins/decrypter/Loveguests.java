package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "loveguests.com" }, urls = { "http://[\\w\\.]*?loveguests\\.com(/.+/.*)" }, flags = { 0 })
public class Loveguests extends PluginForDecrypt {
    public Loveguests(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);

        String content = br.getRegex("class=\"news\"(.*?)</td>").getMatch(0);

        String releaseName = new org.appwork.utils.Regex(content, "<div align=\"center\"><b>(.*?)</b>", 34).getMatch(0);
        String contentReleaseName = new org.appwork.utils.Regex(releaseName, "\"(.*?)\"", 34).getMatch(0);

        String packageName = "";

        if (contentReleaseName != null) {
            contentReleaseName = contentReleaseName.replaceAll("Ã¤", "ae");
            contentReleaseName = contentReleaseName.replaceAll("Ã", "Ue");
            contentReleaseName = contentReleaseName.replaceAll("Ã¼", "ue");

            contentReleaseName = contentReleaseName.replaceAll("ä", "ae");
            contentReleaseName = contentReleaseName.replaceAll("Ä", "Ae");
            contentReleaseName = contentReleaseName.replaceAll("ö", "oe");
            contentReleaseName = contentReleaseName.replaceAll("Ö", "Oe");
            contentReleaseName = contentReleaseName.replaceAll("ü", "ue");
            contentReleaseName = contentReleaseName.replaceAll("Ü", "Ue");
            contentReleaseName = contentReleaseName.replaceAll("ß", "ss");
            contentReleaseName = contentReleaseName.replaceAll(":", " -");
            contentReleaseName = contentReleaseName.replaceAll("'", "");
            contentReleaseName = contentReleaseName.replaceAll("\"", "");

            packageName = contentReleaseName.trim();
        }

        if (content == null) return null;

        String[] links = new org.appwork.utils.Regex(content, "<a href=\"(http://.*?)\"", 2).getColumn(0);

        if ((links == null) || (links.length == 0)) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));

        FilePackage filePackage = FilePackage.getInstance();
        filePackage.setName(packageName);

        for (String link : links) {
            if ((!new org.appwork.utils.Regex(link, getSupportedLinks()).matches()) && (DistributeData.hasPluginFor(link, true))) {
                DownloadLink dLink = createDownloadlink(link);
                decryptedLinks.add(dLink);
            }
        }

        String link = parameter;

        if ((!new org.appwork.utils.Regex(link, getSupportedLinks()).matches()) && (DistributeData.hasPluginFor(link, true))) {
            DownloadLink dLink = createDownloadlink(link);
            decryptedLinks.add(dLink);
        }

        filePackage.addLinks(decryptedLinks);

        return decryptedLinks;
    }
}