package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "web.archive.org" }, urls = { "https?://web\\.archive\\.org/web/\\d+.+" })
public class WebArchiveOrg extends PluginForDecrypt {
    private static final Pattern PATTERN_DIRECT = Pattern.compile("https?://web\\.archive\\.org/web/\\d+(im|oe)_/(https?.+)", Pattern.CASE_INSENSITIVE);

    public WebArchiveOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (new Regex(param.getCryptedUrl(), PATTERN_DIRECT).patternFind()) {
            ret.add(createDownloadlink(DirectHTTP.createURLForThisPlugin(param.getCryptedUrl())));
            final String originalURL = new Regex(param.getCryptedUrl(), PATTERN_DIRECT).getMatch(0);
            ret.add(createDownloadlink(originalURL));
        } else {
            final String youtubeVideoID = TbCmV2.getVideoIDFromUrl(param.getCryptedUrl());
            specialyoutubehandling: if (youtubeVideoID != null) {
                /* Look for direct-URLs */
                br.setFollowRedirects(false);
                br.getPage(param.getCryptedUrl());
                br.getPage("https://web.archive.org/web/2oe_/http://wayback-fakeurl.archive.org/yt/" + Encoding.urlEncode(youtubeVideoID));
                final String directurl = br.getRedirectLocation();
                if (directurl == null) {
                    logger.info("Failed to extract YT directurl");
                    break specialyoutubehandling;
                }
                createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl));
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
