package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "emulatorgames.net" }, urls = { "https?://(?:www\\.)?emulatorgames\\.net/(?:(?:roms|download)/).+" })
public class EmulatorgamesNet extends antiDDoSForDecrypt {
    public EmulatorgamesNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String itemStub = new Regex(parameter, "(?:/roms/(?:[^/]+/)?|/download/\\?rom=)([^/&$]+)").getMatch(0);
        String fpName = null;
        if (StringUtils.isNotEmpty(itemStub)) {
            fpName = br.getRegex("([^<>]+)\\s+ROM\\s+-\\s+[^<]+\\s+-\\s+Emulator Games").getMatch(0);
            String romID = br.getRegex("<span[^>]+class\\s*=\\s*\"eg-view\"[^>]+data-type\\s*=\\s*\"rom\"[^>]+data-id\\s*=\\s*\"([^\"]+)\"").getMatch(0);
            if (StringUtils.isEmpty(romID)) {
                getLogger().warning("Could not retrieve ROM ID required for download steps!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            Browser br2 = br.cloneBrowser();
            final PostRequest downloadPagePost = new PostRequest(br2.getURL("/increment/"));
            downloadPagePost.addVariable("get_type", "rom");
            downloadPagePost.addVariable("get_id", romID);
            downloadPagePost.getHeaders().put("Referer", br.getURL());
            downloadPagePost.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            downloadPagePost.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
            String postResult = br2.getPage(downloadPagePost);
            getPage(br.getURL("/download/?rom=" + itemStub).toString());
            final PostRequest romTargetPost = new PostRequest(br2.getURL("/prompt/"));
            romTargetPost.addVariable("get_type", "rom");
            romTargetPost.addVariable("get_id", romID);
            downloadPagePost.getHeaders().put("Referer", br2.getURL());
            romTargetPost.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            romTargetPost.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
            postResult = br2.getPage(romTargetPost);
            String romURL = br2.getRegex("\\[\\s*\"([^\"]+)").getMatch(0);
            if (StringUtils.isNotEmpty(romURL)) {
                romURL = romURL.replaceAll("\\\\", "");
                decryptedLinks.add(createDownloadlink(romURL));
            }
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}