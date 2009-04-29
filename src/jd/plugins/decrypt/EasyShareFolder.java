package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class EasyShareFolder extends PluginForDecrypt {

    public EasyShareFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookie("http://www.easy-share.com", "language", "en");
        br.getPage(parameter);
        String[] links = br.getRegex("class=\"last\"><a href=\"(.*?easy.*?html)\">").getColumn(0);
        if (links.length == 0) return null;
        for (String dl : links) {
            decryptedLinks.add(this.createDownloadlink(dl));
        }
        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
