package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision: 41147 $", interfaceVersion = 2, names = { "thingiverse.com" }, urls = { "https?://(www\\.)?thingiverse\\.com/.*" })
public class ThingiverseCom extends antiDDoSForDecrypt {
    public ThingiverseCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replaceAll("/about($|\\?.*)", "/designs");
        if (StringUtils.containsIgnoreCase(parameter, "/groups/")) {
            parameter = param.toString().replaceAll("/(forums|members|about)($|\\?.*)", "/designs");
        }
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>\\s*([^<]+?)\\s*-\\s*Thingiverse").getMatch(0);
        if (parameter.contains("thing:")) {
            final String fid = new Regex(parameter, "(\\d+).*").getMatch(0);
            final DownloadLink link = createDownloadlink("directhttp://https://www.thingiverse.com/thing:" + fid + "/zip");
            if (fpName != null) {
                fpName = Encoding.htmlOnlyDecode(fpName);
                link.setFinalFileName(fpName + ".zip");
            }
            decryptedLinks.add(link);
        } else if (parameter.contains("make:")) {
            String link = br.getRegex("href=\"(/\\w+:\\d+)\" class=\"card-img-holder\"").getMatch(0);
            link = br.getURL(Encoding.htmlOnlyDecode(link)).toString();
            decryptedLinks.add(createDownloadlink(link));
        } else if (br.containsHTML("<div class=\"items-page result-page")) {
            final String[] links = br.getRegex("href=\"(/\\w+:\\d+)\" class=\"card-img-holder\"").getColumn(0);
            if (links != null && links.length > 0) {
                for (String link : links) {
                    link = br.getURL(Encoding.htmlOnlyDecode(link)).toString();
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }
}