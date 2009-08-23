package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "crazytr.com" }, urls = { "http://[\\w\\.]*?crazytr\\.com/url/\\d+" }, flags = { 0 })
public class crzytrcm extends PluginForDecrypt {

    public crzytrcm(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());
        String dllink = null;
        dllink = br.getRegex("</script><a href=\"(.*?)\"><img src=\"http://crazytr.com/url/aa.png\"></a>").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        decryptedLinks.add(createDownloadlink(dllink));
        return decryptedLinks;
    }

}
