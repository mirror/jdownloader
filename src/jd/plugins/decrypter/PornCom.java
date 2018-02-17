package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Files;
import org.appwork.utils.logging2.LogSource;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porn.com" }, urls = { "https?://(www\\.)?porn\\.com/videos/(embed/)?[a-z0-9\\-]*?\\-\\d+" })
public class PornCom extends PluginForDecrypt {
    public PornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            try {
                jd.plugins.hoster.PornCom.login(br, aa, false);
            } catch (final PluginException e) {
                LogSource.exception(logger, e);
            }
        }
        String url = parameter.getCryptedUrl();
        jd.plugins.hoster.PornHubCom.getPage(br, url.replace("/embed/", "/"));
        if (br.containsHTML("(id=\"error\"><h2>404|No such video|<title>PORN\\.COM</title>|/removed(_dmca|_deleted_single)?.png)") || this.br.getHttpConnection().getResponseCode() == 404) {
            links.add(this.createOfflinelink(parameter.getCryptedUrl()));
            return links;
        }
        String filename = jd.plugins.hoster.PornCom.getFilename(br);
        links = getLinks(br, url, filename);
        /* A little trick to download videos that are usually only available for registered users WITHOUT account :) */
        if (links.size() == 0) {
            final String fid = new Regex(url, "(\\d+)(?:\\.html)?$").getMatch(0);
            final Browser brc = br.cloneBrowser();
            /* This way we can access links which are usually only accessible for registered users */
            jd.plugins.hoster.PornHubCom.getPage(brc, "https://www.porn.com/videos/embed/" + fid);
            if (brc.containsHTML("<div id=\"player-removed\">")) {
                links.add(this.createOfflinelink(parameter.getCryptedUrl()));
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
        return links;
    }

    private ArrayList<DownloadLink> getLinks(final Browser br, final String origin, final String fileName) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final SubConfiguration cfg = getPluginConfig();
        boolean q240 = cfg.getBooleanProperty("240p", true);
        boolean q360 = cfg.getBooleanProperty("360p", true);
        boolean q480 = cfg.getBooleanProperty("480p", true);
        boolean q720 = cfg.getBooleanProperty("720p", true);
        if (q240 == q360 == q480 == q720 == false) {
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
