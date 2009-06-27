package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class RsdStore extends PluginForDecrypt {

    public RsdStore(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = parameter.toString();
        this.setBrowserExclusive();
        br.getPage(url);
        sleep(4000, parameter);
        String dlc = new Regex(url, "/(\\d+)\\.html").getMatch(0);
        File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
        br.getDownload(container, "http://rsd-store.com/" + dlc + ".dlc");
        decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
        return decryptedLinks;
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

}
