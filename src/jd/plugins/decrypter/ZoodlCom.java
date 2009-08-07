package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "Zoodl.com" }, urls = { "http://[\\w\\.]*?zoodl\\.com/.+"}, flags = { 0 })


public class ZoodlCom extends PluginForDecrypt {

    public ZoodlCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String param = parameter.toString();
        String decLink;

        br.getPage(param);

        if (br.containsHTML("is password protected</td>")) {
            for (int retry = 1; retry <= 5; retry++) {
                Form form = br.getForm(1);
                String pass = UserIO.getInstance().requestInputDialog("Password");
                form.put("p", pass);
                br.submitForm(form);
                if (!br.containsHTML("Not valid password!")) break;
                logger.warning("Wrong password!");
            }
        }

        decLink = br.getRedirectLocation();
        if (decLink == null) decLink = br.getRegex("<FRAME src=\"(.*?)\">").getMatch(0);
        if (decLink == null) return null;

        decryptedLinks.add(createDownloadlink(decLink));
        return decryptedLinks;
    }

    
    

}
