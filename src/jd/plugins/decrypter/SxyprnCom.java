package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourporn.sexy", "sxyprn.com" }, urls = { "https?://(?:www\\.)?yourporn\\.sexy/.+", "https?://(?:www\\.)?sxyprn\\.(?:com|net)/.+" })
public class SxyprnCom extends antiDDoSForDecrypt {
    public SxyprnCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        final boolean isSpecificPageGivenInURL = UrlQuery.parse(parameter.getCryptedUrl()).get("page") != null;
        if (account != null) {
            try {
                ((jd.plugins.hoster.SxyprnCom) plg).login(this.br, account, false);
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        getPage(parameter.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            ret.add(this.createOfflinelink(parameter.getCryptedUrl()));
            return ret;
        } else if (br.containsHTML("class='page_message'[^>]*>\\s*Post Not Found")) {
            /* 2020-11-19 */
            ret.add(this.createOfflinelink(parameter.getCryptedUrl()));
            return ret;
        }
        if (new Regex(parameter.getCryptedUrl(), plg.getSupportedLinks()).matches()) {
            final String packageName = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (packageName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(packageName);
                fp.addLinks(ret);
            }
            final String postText = br.getRegex("<textarea([^>]*class='PostEditTA'.*?)</textarea>").getMatch(0);
            if (postText != null) {
                logger.info("Found postText");
                final String[] urls = HTMLParser.getHttpLinks(postText, br.getURL());
                if (urls.length == 0) {
                    logger.info("Failed to find any external URLs in postText");
                } else {
                    logger.info("Found URLs in postText: " + urls.length);
                    for (final String url : urls) {
                        ret.add(this.createDownloadlink(url));
                    }
                }
            }
            /* This kind of URL also has a selfhosted video which will be handled by our host plugin */
            final DownloadLink main = this.createDownloadlink(parameter.getCryptedUrl());
            if (packageName != null) {
                main.setName(packageName + ".mp4");
            } else {
                main.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            }
            main.setAvailable(true);
            ret.add(main);
        } else {
            final String[] posts = br.getRegex("(<div class='post_el_small'>.*?</span>\\s*</div>\\s*</a>\\s*</div>)").getColumn(0);
            for (final String postHTML : posts) {
                if (!postHTML.contains("post_vid_thumb")) {
                    /* 2019-09-24: Try to skip non-video (e.g. text-only) content */
                    continue;
                }
                final String[][] hits = new Regex(postHTML, "href=(?:\"|')(/post/[a-fA-F0-9]{13}(?:\\.html)?)[^<>]*?title='(.*?)'").getMatches();
                for (final String[] hit : hits) {
                    final DownloadLink link = createDownloadlink(br.getURL(hit[0]).toString());
                    link.setName(hit[1].trim() + ".mp4");
                    link.setAvailable(true);
                    ret.add(link);
                }
            }
            if (!isSpecificPageGivenInURL) {
                /* Does the post have multiple pages? Add them so they will go into this crawler again. */
                final String pages[] = br.getRegex("<a href=(?:\"|')(/[^/]*?\\.html\\?page=\\d+)").getColumn(0);
                for (final String page : pages) {
                    final String url = br.getURL(page).toString();
                    if (!StringUtils.equals(parameter.getCryptedUrl(), url)) {
                        final DownloadLink link = createDownloadlink(url);
                        ret.add(link);
                    }
                }
            }
            final String packageName = new Regex(parameter.getCryptedUrl(), "/([^/]*?)\\.html").getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(packageName);
            fp.addLinks(ret);
        }
        return ret;
    }
}
