package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.DecrypterArrayList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class PornEmbedParser extends PluginForDecrypt {
    public PornEmbedParser(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    protected Browser prepareBrowser(final Browser br) {
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        decryptedLinks.addAll(this.preProcessCryptedLink(param));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        final String filename = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        decryptedLinks.addAll(findEmbedUrls(filename));
        return decryptedLinks;
    }
    // protected final String getFileTitle(final Browser br) {
    // return getFileTitle(null, br);
    // }

    protected String getFileTitle(final CryptedLink param, final Browser br) {
        return null;
    }

    /** Returns true if content is offline according to html code or http response. */
    protected boolean isOffline(final Browser br) {
        /* TODO: Make this abstract */
        return false;
    }

    /**
     * Override this if it is possible to recognize selfhosted content before looking for external URLs. </br>
     * Example plugin: boobinspector.com
     */
    protected boolean isSelfhosted(final Browser br) {
        return false;
    }

    /** Use thisd if you want to change the URL added by the user before processing it. */
    protected void correctCryptedLink(final CryptedLink param) {
    }

    protected ArrayList<DownloadLink> preProcessCryptedLink(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (returnRedirectToUnsupportedLinkAsResult()) {
            br.getPage(param.getCryptedUrl());
            int redirectCounter = 0;
            while (true) {
                if (br.getRedirectLocation() == null) {
                    break;
                }
                if (this.isOffline(br)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                redirectCounter++;
                if (redirectCounter >= 20) {
                    throw new IllegalStateException("Too many redirects!");
                } else if (!this.canHandle(br.getRedirectLocation())) {
                    /* Redirect to external website */
                    ret.add(createDownloadlink(br.getRedirectLocation()));
                    return ret;
                } else {
                    br.followRedirect();
                }
            }
        } else {
            br.setFollowRedirects(true);
            br.getPage(param.getCryptedUrl());
        }
        if (this.isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (isSelfhosted(br)) {
            ret.add(getDownloadLinkSelfhosted(param, br));
            return ret;
        }
        ret.addAll(this.findEmbedUrl(br, getFileTitle(param, br)));
        if (ret.isEmpty() && assumeSelfhostedContentOnNoResults()) {
            ret.add(getDownloadLinkSelfhosted(param, br));
        }
        return ret;
    }

    protected DownloadLink getDownloadLinkSelfhosted(final CryptedLink param, final Browser br) {
        final DownloadLink selfhosted = this.createDownloadlink(br.getURL());
        final String fileTitle = getFileTitle(param, br);
        if (fileTitle != null) {
            if (selfhostedContentSetFinalFilename()) {
                selfhosted.setFinalFileName(fileTitle + ".mp4");
            } else {
                selfhosted.setName(fileTitle + ".mp4");
            }
        }
        if (selfhostedContentSkipAvailablecheck()) {
            selfhosted.setAvailable(true);
        }
        return selfhosted;
    }

    /**
     * true = skip results with same domain of source URL. </br>
     * Warning: Disabling this may have unwanted side-effects as a lot of video sites will show recommendations/similar videos on single
     * video pages which would then also be grabbed!
     */
    protected boolean skipSamePluginResults() {
        return true;
    }

    /** Example where it makes sense to enable this: amateurmasturbations.com. */
    protected boolean returnRedirectToUnsupportedLinkAsResult() {
        return false;
    }

    /** Assumes that content is selfhosted if no external results are found. */
    protected boolean assumeSelfhostedContentOnNoResults() {
        return false;
    }

    /**
     * If set to true, AvailableStatus of DownloadLink for selfhosted content will be set to TRUE in crawler so it does not need to get
     * linkchecked by hosterplugin --> Appears in linkgrabber faster.
     */
    protected boolean selfhostedContentSkipAvailablecheck() {
        return true;
    }

    /** Enable this if DownloadLink.setFinalFilename should be used over DownloadLink.setName. */
    protected boolean selfhostedContentSetFinalFilename() {
        return false;
    }

    /**
     * find the first within findEmbedUrl method from default this.br browser.
     *
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrl() throws Exception {
        return findEmbedUrls(br, null, false);
    }

    /**
     * finds and returns only the first
     *
     * @param title
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrl(final String title) throws Exception {
        return findEmbedUrls(br, title, false);
    }

    /**
     * finds and returns only the first from imported browser
     *
     * @param ibr
     * @param title
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrl(final Browser ibr, final String title) throws Exception {
        return findEmbedUrls(ibr, title, false);
    }

    /**
     * finds all embed urls from this.br
     *
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrls() throws Exception {
        return findEmbedUrls(br, null, true);
    }

    /**
     * finds all embed urls from this.br, with provided title
     *
     * @param title
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrls(final String title) throws Exception {
        return findEmbedUrls(br, title, true);
    }

    /**
     * finds all embed urls from imported browser, with provided title
     *
     * @param ibr
     * @param title
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrls(final Browser ibr, final String title) throws Exception {
        return findEmbedUrls(ibr, title, true);
    }

    /**
     * porn_plugin
     *
     *
     * This method is designed to find porn website urls in html code.
     *
     * @param pluginBrowser
     *            : Browser containing the sourceurl with the embed urls/codes *
     *
     *
     * @param title
     *            : Title to be used in case a directhttp url is found. If the title is not given, directhttp urls will never be decrypted.
     * @throws Exception
     *
     *
     */
    public final ArrayList<DownloadLink> findEmbedUrls(final Browser br, String title, final boolean processAll) throws Exception {
        final DecrypterArrayList<DownloadLink> decryptedLinks = new DecrypterArrayList<DownloadLink>() {
            /**
             *
             */
            private static final long serialVersionUID = 4665325651021721965L;

            @Override
            public boolean add(final String link) {
                return add(link, br);
            }

            @Override
            public boolean add(String link, final Browser br) {
                if (link.startsWith("//")) {
                    link = "https:" + link;
                }
                final String url = Request.getLocation(link, br.getRequest());
                return add(createDownloadlink(url));
            }
        };
        /* Cleanup/Improve title */
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
        }
        logger.info("PornEmbedParser is being executed...");
        String externID = null;
        externID = br.getRegex("(https?://(?:www\\.)?camhub\\.(?:world|cc)/embed/\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = this.createDownloadlink(externID);
            /* Filename is good to have but not necessarily required, */
            if (title != null) {
                title += ".mp4";
                /* 2020-09-29: Special: Enforce this filename because host-plugin will not be able to find a meaningful filename! */
                dl.setForcedFileName(title);
            }
            decryptedLinks.add(dl);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        /************************************************************************************************************/
        // Now check for all existant URLs if they're supported by any plugin tagged as porn plugin
        /************************************************************************************************************/
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        final int before = decryptedLinks.size();
        for (final String url : urls) {
            final List<LazyCrawlerPlugin> nextLazyCrawlerPlugins = findNextLazyCrawlerPlugins(url, LazyPlugin.FEATURE.XXX);
            if (nextLazyCrawlerPlugins.size() > 0) {
                decryptedLinks.addAll(convert(br, title, url, nextLazyCrawlerPlugins));
            }
            final List<LazyHostPlugin> nextLazyHostPlugins = findNextLazyHostPlugins(url, LazyPlugin.FEATURE.XXX);
            if (nextLazyHostPlugins.size() > 0) {
                decryptedLinks.addAll(convert(br, title, url, nextLazyHostPlugins));
            }
        }
        final int results = decryptedLinks.size() - before;
        if (results > 0 && !processAll) {
            return decryptedLinks;
        }
        /************************************************************************************************************/
        // filename needed for all IDs below
        /************************************************************************************************************/
        if (title == null) {
            if (!processAll) {
                return decryptedLinks;
            }
        }
        return decryptedLinks;
    }

    protected List<DownloadLink> convert(final Browser br, final String title, final String url, List<? extends LazyPlugin> lazyPlugins) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (lazyPlugins.size() == 0) {
            return ret;
        }
        if (this.canHandle(url) && skipSamePluginResults()) {
            return ret;
        }
        final DownloadLink dl = createDownloadlink(Request.getLocation(url, br.getRequest()));
        if (lazyPlugins.size() == 1) {
            // TODO: better way for this
            if ("mydaddy.cc".equals(lazyPlugins.get(0).getDisplayName())) {
                if (title != null) {
                    dl.setProperty("decryptertitle", title);
                }
            }
        }
        ret.add(dl);
        return ret;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public DownloadLink createDownloadlink(String url) {
        if (url.startsWith("//")) {
            url = "https:" + url;
        }
        return super.createDownloadlink(url);
    }
}
