package jd.plugins.decrypter;

import java.util.ArrayList;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/**
 * category not designed to do spanning page support!
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dancehallarena.com" }, urls = { "https?://(?:\\w*\\.)?dancehallarena\\.com/(?:[a-zA-Z0-9\\-/]+|category/(?:(?:dancehall|reggae)/(?:singles/|dancehall-albums/|instrumental-dancehall/)?|soca/|mixtapes/(?:dancehall-mixtapes/|reggae-mixtapes/|hiphoprb/)?|videos/(?:music-videos/|viral-videos/)?|efx/)(?:page/\\d+)?)" })
public class DncHllArCom extends antiDDoSForDecrypt {
    public DncHllArCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.setFollowRedirects(true);
        getPage(parameter);
        // invalid url
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<h2>No posts")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (parameter.contains("/category/")) {
            String[] results = br.getRegex("(https?://dancehallarena\\.com/[\\w\\-]+/)").getColumn(0);
            if (results == null || results.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String result : results) {
                ret.add(createDownloadlink(result));
            }
            return ret;
        } else {
            // all external links pass via there own tracking url
            // String[] links = br.getRegex("xurl=(http[^\"]+|://[^\"]+|%3A%2F%2F[^\"]+)").getColumn(0);
            String[] links = br.getRegex("xurl=(%3A%2F%2F[^\"]+)\" target=").getColumn(0);
            if (links != null) {
                for (String link : links) {
                    link = validate(link);
                    if (link != null) {
                        ret.add(createDownloadlink(link));
                    }
                }
            }
            // audiomac provided via iframe
            links = br.getRegex("<iframe[^>]* src=(\"|')(.*?)\\1").getColumn(1);
            if (links != null) {
                for (String link : links) {
                    link = validate(link);
                    if (link != null) {
                        ret.add(createDownloadlink(link));
                    }
                }
            }
            /* 2023-02-03 */
            links = br.getRegex("href=\"(https?://[^\"]+)\" target=\"_blank\" rel=\"noopener nofollow").getColumn(0);
            if (links != null) {
                for (String link : links) {
                    link = validate(link);
                    if (link != null) {
                        ret.add(createDownloadlink(link));
                    }
                }
            }
            if (ret.isEmpty()) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
            }
            return ret;
        }
    }

    /**
     * Validates input URL and corrects protocol if missing prefixs, and also ignores some patterns to prevent false positives
     *
     * @param link
     * @return
     */
    private final String validate(String link) {
        if (link == null) {
            return null;
        }
        final String protocol = new Regex(br.getURL(), "^(https?)").getMatch(-1);
        if (link.startsWith("%3A%2F%2F")) {
            link = Encoding.urlDecode(link, true);
            link = protocol + link;
        } else if (link.startsWith("://")) {
            link = protocol + link;
        } else if (link.startsWith("//")) {
            link = protocol + ":" + link;
        }
        if (new Regex(link, "facebook.com(/|%2F)plugins(/|%2F)|(x|twitter).com(/|%2F)").matches()) {
            return null;
        }
        return link;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
