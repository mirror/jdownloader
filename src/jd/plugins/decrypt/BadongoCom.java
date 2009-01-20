package jd.plugins.decrypt;

import java.util.ArrayList;
import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class BadongoCom extends PluginForDecrypt {
    static private String host = "badongo.com";

    public BadongoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.setCookiesExclusive(true);
        br.clearCookies(host);
        br.setCookie("http://www.badongo.com", "badongoL", "de");
        br.getPage(parameter);
        if (!br.containsHTML("Diese Datei wurde gesplittet")) {
            DownloadLink dlLink = createDownloadlink(Encoding.htmlDecode(parameter.replaceFirst("http", "httpviajd")));
            dlLink.setProperty("type", "single");
            decryptedLinks.add(dlLink);
        } else {
            String[] links = br.getRegex("<div class=\"m\">Download Teil(.*?)</div>").getColumn(0);
            progress.setRange(links.length);
            for (Integer part = 0; part < links.length; part++) {
                DownloadLink dlLink = createDownloadlink(Encoding.htmlDecode(parameter.replaceFirst("http", "httpviajd" + Integer.toString(part))));
                dlLink.setProperty("type", "split");
                dlLink.setProperty("part", part);
                dlLink.setProperty("parts", links.length);
                decryptedLinks.add(dlLink);
                progress.increase(1);
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 4227 $");
    }
}