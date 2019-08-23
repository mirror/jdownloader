package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 41147 $", interfaceVersion = 2, names = { "thingiverse.com" }, urls = { "https?://(www\\.)?thingiverse\\.com/(thing:\\d+|make:\\d+|[^/]+/(about|designs|collections(/[^/]+)?|makes|likes))" })
public class ThingiverseCom extends antiDDoSForDecrypt {
    public ThingiverseCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        String fpName = br.getRegex("<title>\\s*([^<]+?)\\s*-\\s*Thingiverse").getMatch(0);
        if (new Regex(br.getURL(), "/[^/]+/(about|designs|collections(/[^/]+)?|makes|likes)").matches()) {
            // TODO add support for user-abouts/designs/collections/makes/likes, collections may have individual ones
            // requires post requests to ajax api (pagination)
            // please don't use wide/open pattern but try to make it as fine as possible
            // TODO: /groups/XY , /groups/XY/things , /groups/XY/about
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (StringUtils.containsIgnoreCase(br.getURL(), "/thing:")) {
            // a thing
            final String thingID = new Regex(br.getURL(), "thing:(\\d+).*").getMatch(0);
            final DownloadLink link = createDownloadlink("directhttp://https://www.thingiverse.com/thing:" + thingID + "/zip");
            if (fpName != null) {
                fpName = Encoding.htmlOnlyDecode(fpName);
                link.setFinalFileName(fpName + ".zip");
            }
            decryptedLinks.add(link);
        } else if (StringUtils.containsIgnoreCase(br.getURL(), "/make:")) {
            // a make
            final String thingID = br.getRegex("href=\"/thing:(\\d+)\"\\s*class=\"card-img-holder\"").getMatch(0);
            if (thingID != null) {
                final DownloadLink thing = createDownloadlink("https://www.thingiverse.com/thing:" + thingID);
                decryptedLinks.add(thing);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}