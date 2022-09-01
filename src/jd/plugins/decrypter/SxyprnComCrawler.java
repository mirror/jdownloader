package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.SxyprnCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourporn.sexy", "sxyprn.com" }, urls = { "https?://(?:www\\.)?yourporn\\.sexy/.+", "https?://(?:www\\.)?sxyprn\\.(?:com|net)/.+" })
public class SxyprnComCrawler extends antiDDoSForDecrypt {
    public SxyprnComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            ((jd.plugins.hoster.SxyprnCom) plg).login(this.br, account, false);
        }
        getPage(param.getCryptedUrl());
        if (SxyprnCom.isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (plg.canHandle(param.getCryptedUrl())) {
            /* Single post */
            final String packageName = SxyprnCom.regexTitle(br);
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
            final DownloadLink main = this.createDownloadlink(param.getCryptedUrl());
            if (packageName != null) {
                main.setName(packageName + ".mp4");
            } else {
                main.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            }
            main.setAvailable(true);
            ret.add(main);
        } else {
            /* Crawl all posts within a page */
            final String[] posts = br.getRegex("(<div class='post_el_small'>.*?</span>\\s*</div>\\s*</a>\\s*</div>)").getColumn(0);
            for (final String postHTML : posts) {
                if (!postHTML.contains("post_vid_thumb")) {
                    /* 2019-09-24: Try to skip non-video (e.g. text-only) content */
                    continue;
                }
                final String[][] hits = new Regex(postHTML, "href=(?:\"|')(/post/[a-fA-F0-9]{13}(?:\\.html)?)[^<>]*?title='(.*?)'").getMatches();
                for (final String[] hit : hits) {
                    final DownloadLink link = createDownloadlink(br.getURL(hit[0]).toString());
                    link.setName(Encoding.htmlDecode(hit[1]).trim() + ".mp4");
                    link.setAvailable(true);
                    ret.add(link);
                }
            }
            final boolean isSpecificPageGivenInURL = UrlQuery.parse(param.getCryptedUrl()).get("page") != null;
            if (!isSpecificPageGivenInURL) {
                /* Does the post have multiple pages? Add them so they will go into this crawler again. */
                final String pages[] = br.getRegex("<a href=(?:\"|')(/[^/]*?\\.html\\?page=\\d+)").getColumn(0);
                for (final String page : pages) {
                    final String url = br.getURL(page).toString();
                    if (!StringUtils.equals(param.getCryptedUrl(), url)) {
                        final DownloadLink link = createDownloadlink(url);
                        ret.add(link);
                    }
                }
            }
            final String packageName = new Regex(param.getCryptedUrl(), "/([^/]*?)\\.html").getMatch(0);
            if (packageName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(packageName);
                fp.addLinks(ret);
            }
        }
        return ret;
    }

    public static final String getContentURL(final String host, final String contentID) {
        return "https://" + host + "/post/" + contentID + ".html";
    }
}
