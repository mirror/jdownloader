package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 38918 $", interfaceVersion = 3, names = { "limitsizamca.org" }, urls = { "https?://(?:www\\.)?limitsizamca.org/koruma\\.php\\?id=\\d+" })
public class LimitsizamcaOrg extends antiDDoSForDecrypt {
    public LimitsizamcaOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        getPage(parameter.getCryptedUrl());
        final Form form = br.getForm(0);
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        submitForm(form);
        final String links[] = br.getRegex("<a\\s*href\\s*=\\s*\"(https?://.*?)\"\\s*target\\s*=\\s*\"_blank\"").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (links != null) {
            for (String link : links) {
                ret.add(createDownloadlink(link));
            }
        }
        return ret;
    }
}
