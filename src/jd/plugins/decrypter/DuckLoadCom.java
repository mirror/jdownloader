package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class DuckLoadCom extends PluginForDecrypt {

    public DuckLoadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());

        String[] pages = br.getRegex("<option value=\"(\\d+)\" selected>\\d+</option>").getColumn(0);
        if (pages.length != 0) {
            for (String page : pages) {
                br.getPage(parameter.toString() + "/" + page);
                String[] links = br.getRegex("value=\"(http://.+?)\"").getColumn(0);
                if (links.length == 0) return null;
                for (String link : links) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
        } else
            return null;

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
