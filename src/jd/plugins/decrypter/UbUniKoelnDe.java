package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision: 34675 $", interfaceVersion = 2, names = { "ub.uni-koeln.de" }, urls = { "https?://(www.)?ub\\.uni-koeln\\.de/cdm/[a-zA-Z0-9]+/collection/[a-zA-Z0-9\\-_]+/id/\\d+" })
public class UbUniKoelnDe extends PluginForDecrypt {

    public UbUniKoelnDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final int padLength(final int size) {
        if (size < 10) {
            return 1;
        } else if (size < 100) {
            return 2;
        } else if (size < 1000) {
            return 3;
        } else if (size < 10000) {
            return 4;
        } else if (size < 100000) {
            return 5;
        } else if (size < 1000000) {
            return 6;
        } else if (size < 10000000) {
            return 7;
        } else {
            return 8;// hello djmakinera
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setLoadLimit(1024 * 1024 * 8);
        br.getPage(parameter.getCryptedUrl());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String rights = br.getRegex("id=\"metadata_object_rechte\">\\s*(.*?)\\s*</").getMatch(0);
        if (StringUtils.equalsIgnoreCase("Rechte vorbehalten - freier Zugang", rights) || StringUtils.equalsIgnoreCase("Kein Urheberrechtsschutz", rights) || StringUtils.containsIgnoreCase(rights, "freier Zugang")) {
            final String title = br.getRegex("id=\"metadata_object_title\">\\s*(.*?)\\s*</").getMatch(0);
            final String[][] pages = br.getRegex("<a class=\"co-page-link.*?\" id=\"pageTitle-\\d+\".*?item_id=\"(\\d+)\" item_file=\"(.*?)\"\\s*>(.*?)</a").getMatches();
            final FilePackage fp = FilePackage.getInstance();
            if (title != null) {
                fp.setName(title);
            }
            final int padLength = padLength(pages.length);
            final String collection = new Regex(parameter.getCryptedUrl(), "collection/(.+)").getMatch(0);
            for (int index = 0; index < pages.length; index++) {
                if (!isAbort()) {
                    final String page[] = pages[index];
                    final String fileName = "Seite_" + String.format(Locale.US, "%0" + padLength + "d", index + 1) + ".jpg";
                    final String url = "http://www.ub.uni-koeln.de/utils/getdownloaditem/collection/" + collection + "/type/compoundobject/show/" + page[0] + "/cpdtype/monograph/filename/" + page[1];
                    final DownloadLink link = createDownloadlink("directhttp://" + url);
                    link.setProperty("requestType", "GET");
                    link.setAvailable(true);
                    link.setFinalFileName(fileName);
                    ret.add(link);
                    fp.add(link);
                    distribute(link);
                } else {
                    break;
                }
            }
        }
        return ret;
    }

}
