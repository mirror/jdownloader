package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class GazUpCom extends PluginForDecrypt {

    public GazUpCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());
        String[] finalLinks = br.getRegex("<td class=\"downloadonline\"><a .+?>(.*?)</a></td>").getColumn(0);
        progress.setRange(finalLinks.length);
        for (String data : finalLinks) {
            br.setFollowRedirects(false);
            br.getPage(data);
            DownloadLink link = createDownloadlink(br.getRedirectLocation());
            decryptedLinks.add(link);
            progress.increase(1);
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
