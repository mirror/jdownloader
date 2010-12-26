package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesonic.com" }, urls = { "http://[\\w\\.]*?filesonic\\.com/.*?folder/[0-9a-z]+" }, flags = { 0 })
public class FlsncCm extends PluginForDecrypt {

    public FlsncCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String id = new Regex(parameter, "/(folder/[0-9a-z]+)").getMatch(0);
        if (id == null) return null;
        parameter = "http://www.filesonic.com/en/" + id;
        boolean failed = false;
        br.getPage(parameter);
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        if (br.containsHTML("(Folder do not exist<|>The requested folder do not exist or was deleted by the owner|>If you want, you can contact the owner of the referring site to tell him about this mistake)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] links = br.getRegex("<td width=\"70%\" align=\"left\" valign=\"top\">(.*?)</tr>").getColumn(0);
        if (links == null || links.length == 0) {
            failed = true;
            links = br.getRegex("<td><a href=\"(http://.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(http://filesonic\\.com/.*?file/\\d+/.*?)\"").getColumn(0);
        }
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String data : links) {
            if (failed) {
                if (!data.contains("/folder/")) decryptedLinks.add(createDownloadlink(data));
            } else {
                String filename = new Regex(data, "filesonic\\.com/.*?file/.*?/(.*?)\"").getMatch(0);
                String filesize = new Regex(data, "valign=\"top\">.*?\\|.*?\\|.*?\\|.*?\\|(.*?)\\(.*?</td>").getMatch(0);
                String dlink = new Regex(data, "href=\"(http.*?)\"").getMatch(0);
                if (dlink == null) return null;
                DownloadLink aLink = createDownloadlink(dlink);
                if (filename != null) aLink.setName(filename.trim());
                if (filesize != null) aLink.setDownloadSize(Regex.getSize(filesize.trim()));
                if (filename != null && filesize != null) aLink.setAvailable(true);
                if (!dlink.contains("/folder/")) decryptedLinks.add(createDownloadlink(dlink));
            }
            progress.increase(1);
        }
        return decryptedLinks;
    }

}
