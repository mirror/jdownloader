package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "javfree.sh" }, urls = { "https?://(?:www\\.)?javfree\\.sh/\\d+/[a-z0-9\\-_]+\\.html" })
public class JavfreeSh extends PluginForDecrypt {
    public JavfreeSh(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** 2021-02-18: Formerly known as: javqd.tv */
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String urlSlug = new Regex(br.getURL(), "([a-z0-9_\\-]+)\\.html$").getMatch(0);
        final String embedID = br.getRegex("player#([a-f0-9]+)\"").getMatch(0);
        if (embedID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("/stream/" + embedID);
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        for (final String url : urls) {
            ret.add(this.createDownloadlink(url));
        }
        if (urlSlug != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(urlSlug.replace(" ", " "));
            fp.addLinks(ret);
        }
        return ret;
    }
}
