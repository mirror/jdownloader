package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 21356 $", interfaceVersion = 2, names = { "oboom.com" }, urls = { "https?://(www\\.)?oboom\\.com/#share/[a-f0-9\\-]+" }, flags = { 0 })
public class OBoomCom extends PluginForDecrypt {
    private final String APPID = "43340D9C23";
    
    public OBoomCom(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.getPage("https://www.oboom.com/1.0/guestsession?source=" + APPID);
        String guestSession = br.getRegex("200,.*?\"(.*?)\"").getMatch(0);
        if (guestSession == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String shareID = new Regex(parameter.toString(), "#share/([a-f0-9\\-]+)").getMatch(0);
        br.getPage("https://www.oboom.com/1.0/" + guestSession + "/share?share=" + shareID);
        String files = br.getRegex("\"files\":\\[(.*?)\\]").getMatch(0);
        String name = br.getRegex("\"name\":\"(.*?)\"").getMatch(0);
        if (name != null && "undefined".equals(name)) name = null;
        if (files != null) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            String fileIDs[] = new Regex(files, "\"(.*?)\"").getColumn(0);
            for (String fileID : fileIDs) {
                decryptedLinks.add(createDownloadlink("https://www.oboom.com/#" + fileID));
            }
            if (name != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(name);
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }
        return null;
    }
}
