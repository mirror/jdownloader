package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.Vector;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

public class FourSharedFolder extends PluginForDecrypt {

    public FourSharedFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String pages[] = br.getRegex("javascript:pagerShowFiles\\((\\d+)\\);").getColumn(0);
        String name = br.getRegex("hidden\" name=\"defaultZipName\" value=\"(.*?)\">").getMatch(0);
        String[] links = br.getRegex("<a href=\"(.*?4shared.com/file/.*?)\"").getColumn(0);
        FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        if (links.length == 0) return null;
        for (String dl : links) {
            decryptedLinks.add(this.createDownloadlink(dl));
        }
        for (String page:pages)
        {
            br.getPage(page);
            links = br.getRegex("<a href=\"(.*?4shared.com/file/.*?)\"").getColumn(0);
            if (links.length == 0) return null;
            for (String dl : links) {
                decryptedLinks.add(this.createDownloadlink(dl));
            }
        }
        fp.addAll(new Vector<DownloadLink>(decryptedLinks));
        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
