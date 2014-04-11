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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "oboom.com" }, urls = { "https?://(www\\.)?oboom\\.com/(#share/[a-f0-9\\-]+|#folder/[A-Z0-9]+)" }, flags = { 0 })
public class OBoomCom extends PluginForDecrypt {

    private final String APPID  = "43340D9C23";
    private final String wwwURL = "https://www.oboom.com/1.0/";
    private final String apiURL = "https://api.oboom.com/1/";

    public OBoomCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(wwwURL + "guestsession?source=" + APPID);
        String guestSession = br.getRegex("200,.*?\"(.*?)\"").getMatch(0);
        if (guestSession == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String UID = new Regex(parameter.toString(), "#(share|folder)/([A-Z0-9\\-]+)").getMatch(1);
        if (UID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String name = null;
        if (parameter.toString().contains("#share")) {
            br.getPage(wwwURL + guestSession + "/share?share=" + UID);
            String files = br.getRegex("\"files\":\\[(.*?)\\]").getMatch(0);
            name = br.getRegex("\"name\":\"(.*?)\"").getMatch(0);
            if (name != null && "undefined".equals(name)) name = null;
            if (files != null) {
                String fileIDs[] = new Regex(files, "\"(.*?)\"").getColumn(0);
                for (String fileID : fileIDs) {
                    decryptedLinks.add(createDownloadlink("https://www.oboom.com/#" + fileID));
                }
            }
        } else if (parameter.toString().contains("#folder")) {
            br.getPage(apiURL + "ls?item=" + UID + "&token=" + guestSession);
            name = br.getRegex("\"name\":\"(.*?)\"").getMatch(0);
            String[] files = br.getRegex("\\{\"size.*?\\}").getColumn(-1);
            if (files != null && files.length != 0) {
                for (final String f : files) {
                    final String fname = getJson(f, "name");
                    final String fsize = getJson(f, "size");
                    final String fuid = getJson(f, "id");
                    final String type = getJson(f, "type");
                    final String state = getJson(f, "state");
                    if (fuid != null && "file".equalsIgnoreCase(type)) {
                        DownloadLink dl = createDownloadlink("https://www.oboom.com/#" + fuid);
                        if ("online".equalsIgnoreCase(state))
                            dl.setAvailable(true);
                        else
                            dl.setAvailable(false);
                        if (fname != null) dl.setName(fname);
                        if (fsize != null) dl.setDownloadSize(Long.parseLong(fsize));
                        try {
                            dl.setLinkID(fuid);
                        } catch (final Throwable e) {
                            // not in JD2
                        }
                        decryptedLinks.add(dl);
                    }
                    if (fuid != null && !"file".equalsIgnoreCase(type)) {
                        // sub folders maybe possible also ??
                        return null; // should get users reporting issue!
                    }
                }
            }
        }
        if (name != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(name);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String getJson(final String source, final String key) {
        if (source == null) return null;
        String result = new Regex(source, "\"" + key + "\":\"([^\"]+)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + key + "\":(\\d+|true|false)").getMatch(0);
        return result;
    }
}
