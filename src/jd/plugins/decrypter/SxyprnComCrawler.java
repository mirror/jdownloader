package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
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
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
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
            String title = SxyprnCom.regexTitle(br);
            if (title != null) {
                title = SxyprnCom.cleanupTitle(this, br, title);
            }
            final String postText = br.getRegex("<textarea([^>]*class='PostEditTA'.*?)</textarea>").getMatch(0);
            if (postText != null) {
                ret.addAll(crawlURLsGeneric(postText));
            }
            /* This kind of URL also has a selfhosted video which will be handled by our host plugin */
            final DownloadLink main = this.createDownloadlink(param.getCryptedUrl());
            final String videoExtDefault = ".mp4";
            if (title != null) {
                main.setName(title + videoExtDefault);
            } else {
                main.setName(br._getURL().getPath() + videoExtDefault);
            }
            main.setAvailable(true);
            ret.add(main);
            final FilePackage fp = FilePackage.getInstance();
            if (title != null) {
                fp.setName(title);
            } else {
                /* Fallback */
                fp.setName(br._getURL().getPath());
            }
            fp.addLinks(ret);
        } else if (br.getURL().matches("https?://[^/]+/blog/[a-fA-F0-9]{13}/\\d+\\.html")) {
            /* Crawl all [video-] posts within a blog | first page only */
            final String[] posts = br.getRegex("(<div class='post_el_small'>.*?</div></div>)").getColumn(0);
            for (final String postHTML : posts) {
                if (!postHTML.contains("post_vid_thumb")) {
                    /* 2019-09-24: Try to skip non-video (e.g. text-only) content */
                    continue;
                }
                final String postURL = new Regex(postHTML, "href='(/post/[a-fA-F0-9]{13}(?:\\.html)?)").getMatch(0);
                String postText = new Regex(postHTML, "data-title='([^\\']+)' ").getMatch(0);
                if (postURL != null && postText != null) {
                    final DownloadLink link = createDownloadlink(br.getURL(postURL).toString());
                    final String title = SxyprnCom.cleanupTitle(this, br, postText);
                    link.setName(title + ".mp4");
                    link.setAvailable(true);
                    ret.add(link);
                    postText = Encoding.htmlDecode(postText).trim();
                    ret.addAll(crawlURLsGeneric(postText));
                }
            }
            final String packageName = new Regex(param.getCryptedUrl(), "/([^/]*?)\\.html").getMatch(0);
            if (packageName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(packageName);
                fp.addLinks(ret);
            }
        } else {
            /* Crawl all [video-] posts within a generic page or category e.g. https://sxyprn.com/hardcore.html?sm=trending | all pages */
            final String[] posts = br.getRegex("(<div class='post_el_small'>.*?</span>\\s*</div>\\s*</a>\\s*</div>)").getColumn(0);
            for (final String postHTML : posts) {
                if (!postHTML.contains("post_vid_thumb")) {
                    /* 2019-09-24: Try to skip non-video (e.g. text-only) content */
                    continue;
                }
                final String[][] hits = new Regex(postHTML, "href=(?:\"|')(/post/[a-fA-F0-9]{13}(?:\\.html)?)[^<>]*?title='(.*?)'").getMatches();
                for (final String[] hit : hits) {
                    final DownloadLink link = createDownloadlink(br.getURL(hit[0]).toString());
                    final String postText = Encoding.htmlDecode(hit[1]).trim();
                    final String title = SxyprnCom.cleanupTitle(this, br, postText);
                    link.setName(title + ".mp4");
                    link.setAvailable(true);
                    ret.add(link);
                    ret.addAll(crawlURLsGeneric(postText));
                }
            }
            final boolean isSpecificPageGivenInURL = UrlQuery.parse(param.getCryptedUrl()).get("page") != null;
            /* Only look for more pages if we're currently on page 1 and also found some results on that page. */
            if (!isSpecificPageGivenInURL && !ret.isEmpty()) {
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

    private ArrayList<DownloadLink> crawlURLsGeneric(final String text) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String[] urls = HTMLParser.getHttpLinks(text, br.getURL());
        for (final String url : urls) {
            ret.add(this.createDownloadlink(url));
        }
        return ret;
    }

    public static final String getContentURL(final String host, final String contentID) {
        return "https://" + host + "/post/" + contentID + ".html";
    }
}
