package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.appwork.utils.Files;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porn.com" }, urls = { "https?://(\\w+\\.)?porn\\.com/(?:videos/(embed/)?[a-z0-9\\-]*?\\-\\d+|out/[a-z]/[^/]+/[a-zA-Z0-9_/\\+\\=\\-%]+)" })
public class PornCom extends PluginForDecrypt {
    public PornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private static final String TYPE_REDIRECT_BASE64 = "(?i)https?://(?:\\w+\\.)?[^/]+/out/[a-z]/[^/]+/([a-zA-Z0-9_/\\+\\=\\-%]+)/.*";

    /* DEV NOTES */
    /* Porn_plugin */
    /* Similar websites: porn.com, fucktube.com */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String url = param.getCryptedUrl();
        if (url.matches(TYPE_REDIRECT_BASE64)) {
            /* These ones redirect to a single external URL and contain a base64 encoded String containing that URL. */
            String b64 = Encoding.htmlDecode(new Regex(url, "https?://(?:\\w+\\.)?[^/]+/out/[a-z]/[^/]+/([a-zA-Z0-9_/\\+\\=\\-%]+)/.*").getMatch(0));
            /* Correct b64 string as it can be invalid. */
            final String b64Remove = new Regex(b64, "(==|=)(.+)$").getMatch(0);
            if (b64Remove != null) {
                b64 = b64.replaceFirst(Pattern.quote(b64Remove), "");
            }
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
            final String fid = new Regex(url, "(\\d+)(?:\\.html)?$").getMatch(0);
            final Account aa = AccountController.getInstance().getValidAccount(getHost());
            if (aa != null) {
                try {
                    jd.plugins.hoster.PornCom.login(br, aa, false);
                } catch (final PluginException e) {
                    LogSource.exception(logger, e);
                }
            }
            jd.plugins.hoster.PornHubCom.getPage(br, url.replace("/embed/", "/"));
            if (br.containsHTML("(id=\"error\"><h2>404|No such video|<title>PORN\\.COM</title>|/removed(_dmca|_deleted_single)?.png)") || this.br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(fid)) {
                links.add(this.createOfflinelink(param.getCryptedUrl()));
                return links;
            }
            String filename = jd.plugins.hoster.PornCom.getFilename(br);
            links = getLinks(br, url, filename);
            /* A little trick to download videos that are usually only available for registered users WITHOUT account :) */
            if (links.size() == 0) {
                final Browser brc = br.cloneBrowser();
                /* This way we can access links which are usually only accessible for registered users */
                jd.plugins.hoster.PornHubCom.getPage(brc, "https://www.porn.com/videos/embed/" + fid);
                if (brc.containsHTML("<div id=\"player-removed\">") || br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(fid)) {
                    links.add(this.createOfflinelink(param.getCryptedUrl()));
                    return links;
                }
                links = getLinks(brc, url, filename);
            }
            if (links.size() == 0) {
                if (br.containsHTML(">Sorry, this video is only available to members")) {
                    logger.info("Sorry, this video is only available to members");
                    return new ArrayList<DownloadLink>(0);
                } else {
                    return null;
                }
            }
        }
        return links;
    }

    private ArrayList<DownloadLink> getLinks(final Browser br, final String origin, final String fileName) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final SubConfiguration cfg = getPluginConfig();
        boolean q240 = cfg.getBooleanProperty("240p", true);
        boolean q360 = cfg.getBooleanProperty("360p", true);
        boolean q480 = cfg.getBooleanProperty("480p", true);
        boolean q720 = cfg.getBooleanProperty("720p", true);
        if (!(q240 || q360 || q480 || q720)) {
            q240 = true;
            q360 = true;
            q480 = true;
            q720 = true;
        }
        final String originID = new Regex(origin, "(\\d+)(?:\\.html)?$").getMatch(0);
        final boolean best = cfg.getBooleanProperty("ALLOW_BEST", false);
        final HashMap<String, String> matches = getQualities(this.br);
        if (best) {
            if (q720 && matches.containsKey("720p")) {
                final String url = matches.get("720p");
                matches.clear();
                matches.put("720p", url);
            }
            if (q480 && matches.containsKey("480p")) {
                final String url = matches.get("480p");
                matches.clear();
                matches.put("480p", url);
            }
            if (q360 && matches.containsKey("360p")) {
                final String url = matches.get("360p");
                matches.clear();
                matches.put("360p", url);
            }
            if (q240 && matches.containsKey("240p")) {
                final String url = matches.get("240p");
                matches.clear();
                matches.put("240p", url);
            }
        }
        for (final Entry<String, String> match : matches.entrySet()) {
            final String url = match.getValue();
            final String q = match.getKey();
            final DownloadLink link = createDownloadlink(origin);
            final String ext = Files.getExtension(new Regex(url, "/(.*?)(\\?|$)").getMatch(0));
            if (ext != null) {
                link.setFinalFileName(fileName + "_" + q + "." + ext);
            } else {
                link.setFinalFileName(fileName + "_" + q + ".mp4");
            }
            link.setLinkID(getHost() + "_" + q + originID);
            link.setProperty("q", q);
            link.setAvailable(true);
            ret.add(link);
            if (best) {
                break;
            }
        }
        return ret;
    }

    public static HashMap<String, String> getQualities(final Browser br) {
        final String qualities[][] = br.getRegex("(\\d+p|low|med|hq|hd)\",url:\"(https?:.*?)\"").getMatches();
        HashMap<String, String> matches = new HashMap<String, String>();
        if (qualities != null && qualities.length > 0) {
            for (final String qualitiy[] : qualities) {
                String qualityname = qualitiy[0];
                /* Correct that */
                if (qualityname.equals("low")) {
                    qualityname = "240p";
                } else if (qualityname.equals("med")) {
                    qualityname = "360p";
                } else if (qualityname.equals("hq")) {
                    qualityname = "480p";
                } else if (qualityname.equals("hd")) {
                    qualityname = "720p";
                }
                matches.put(qualityname, qualitiy[1]);
            }
        }
        return matches;
    }
}
