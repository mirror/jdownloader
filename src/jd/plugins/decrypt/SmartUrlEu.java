package jd.plugins.decrypt;

import java.util.ArrayList;
import jd.PluginWrapper;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class SmartUrlEu extends PluginForDecrypt {

    public SmartUrlEu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.getPage(param.toString());        
        decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 3393 $");
    }
}