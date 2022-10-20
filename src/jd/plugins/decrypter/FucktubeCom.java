package jd.plugins.decrypter;

import java.util.ArrayList;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fucktube.com" }, urls = { "https?://(\\w+\\.)?fucktube\\.com/out/[a-z]/[^/]+/[a-zA-Z0-9_/\\+\\=\\-%]+" })
public class FucktubeCom extends PluginForDecrypt {
    public FucktubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private static final String TYPE_REDIRECT_BASE64 = "https?://(?:\\w+\\.)?[^/]+/out/[a-z]/[^/]+/([a-zA-Z0-9_/\\+\\=\\-%]+)/.*";

    /* DEV NOTES */
    /* Porn_plugin */
    /* Similar websites: porn.com, fucktube.com */
    /**
     * 2022-10-20: Website seems to have changed and is now linking to paid websites povr.com and wankzvr.com but old URLs will continue to
     * work as this is an "offline crawler" which will only extract information out of base64 strings contained in added URLs.
     */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(TYPE_REDIRECT_BASE64)) {
            /* These ones redirect to a single external URL and contain a base64 encoded String containing that URL. */
            final String b64 = Encoding.htmlDecode(new Regex(param.getCryptedUrl(), "https?://(?:\\w+\\.)?[^/]+/out/[a-z]/[^/]+/([a-zA-Z0-9_/\\+\\=\\-%]+)/.*").getMatch(0));
            final String decoded = Encoding.Base64Decode(b64);
            final String[] urls = HTMLParser.getHttpLinks(decoded, br.getURL());
            if (urls.length == 0) {
                /* Allow this to happen */
                logger.info("Found no results");
            } else {
                /* Usually we will get exactly 1 result. */
                for (final String thisurl : urls) {
                    links.add(this.createDownloadlink(thisurl));
                }
            }
        } else {
            /* Unsupported URL -> Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return links;
    }
}
